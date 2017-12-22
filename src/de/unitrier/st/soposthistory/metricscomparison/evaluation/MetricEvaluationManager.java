package de.unitrier.st.soposthistory.metricscomparison.evaluation;

import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MetricEvaluationManager implements Runnable {
    private static AtomicInteger threadIdCounter = new AtomicInteger(0);

    private static Logger logger = null;
    public static final CSVFormat csvFormatPostIds;
    public static final CSVFormat csvFormatMetricEvaluationPerPost;
    public static final CSVFormat csvFormatMetricEvaluationPerVersion;
    private static final CSVFormat csvFormatMetricEvaluationPerSample;
    private static final CSVFormat csvFormatSelectedMetrics;
    private static final Path DEFAULT_OUTPUT_DIR = Paths.get("output");
    private static final List<SimilarityMetric> defaultSimilarityMetrics = new LinkedList<>();

    private int threadId;
    private String sampleName;
    private boolean addDefaultSimilarityMetrics;
    private boolean randomizeOrder;
    private boolean validate;
    private int numberOfRepetitions;
    private int threadCount;

    private Path postIdPath;
    private Path postHistoryPath;
    private Path groundTruthPath;
    private Path outputDirPath;

    private Set<Integer> postIds;
    private Map<Integer, PostGroundTruth> postGroundTruths; // postId -> PostGroundTruth
    private Map<Integer, PostVersionList> postVersionLists; // postId -> PostVersionList

    private List<SimilarityMetric> similarityMetrics;
    private List<MetricEvaluationPerSample> metricEvaluationsPerSample;

    private boolean initialized;
    private boolean evaluationPrepared; // flag used to check if the metrics and samples have been added
    private boolean finished; // flag used to check if end of run method was reached

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(MetricEvaluationManager.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for list of PostIds
        csvFormatPostIds = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId", "VersionCount")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\');

        // configure CSV format for metric comparison results (per post, i.e., per PostVersionList)
        csvFormatMetricEvaluationPerPost = CSVFormat.DEFAULT
                .withHeader("MetricType", "Metric", "Threshold", "PostId", "Runtime", "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons", "TextBlockVersionCount", "PossibleComparisonsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "CodeBlockVersionCount", "PossibleComparisonsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for metric comparison results (per version, i.e., per PostHistoryId)
        csvFormatMetricEvaluationPerVersion = CSVFormat.DEFAULT
                .withHeader("MetricType", "Metric", "Threshold", "PostId", "PostHistoryId", "Runtime", "PossibleComparisons", "TextBlockCount", "PossibleComparisonsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "CodeBlockCount", "PossibleComparisonsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for aggregated metric comparison results (per (metric, threshold) combination)
        csvFormatMetricEvaluationPerSample = CSVFormat.DEFAULT
                .withHeader("MetricType", "Metric", "Threshold", "Runtime", "InformednessText", "MarkednessText", "MatthewsCorrelationText", "FScoreText", "InformednessCode", "MarkednessCode", "MatthewsCorrelationCode", "FScoreCode", "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons", "TextBlockVersionCount", "PossibleComparisonsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText", "PrecisionText", "RecallText", "InversePrecisionText", "InverseRecallText", "FailureRateText", "CodeBlockVersionCount", "PossibleComparisonsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode", "PrecisionCode", "RecallCode", "InversePrecisionCode", "InverseRecallCode", "FailureRateCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for selected metrics
        csvFormatSelectedMetrics = CSVFormat.DEFAULT
                .withHeader("Metric")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // add default similarity metrics
        createDefaultSimilarityMetrics();
    }

    private MetricEvaluationManager(String sampleName, Path postIdPath,
                                    Path postHistoryPath, Path groundTruthPath, Path outputDirPath,
                                    boolean validate, boolean addDefaultSimilarityMetrics, boolean randomizeOrder,
                                    int numberOfRepetitions, int threadCount) {

        this.sampleName = sampleName;

        this.postIdPath = postIdPath;
        this.postHistoryPath = postHistoryPath;
        this.groundTruthPath = groundTruthPath;
        this.outputDirPath = outputDirPath;

        this.validate = validate;
        this.addDefaultSimilarityMetrics = addDefaultSimilarityMetrics;
        this.randomizeOrder = randomizeOrder;
        this.numberOfRepetitions = numberOfRepetitions;
        this.threadCount = threadCount;

        this.postIds = new HashSet<>();
        this.postGroundTruths = new HashMap<>();
        this.postVersionLists = new HashMap<>();

        this.similarityMetrics = new LinkedList<>();
        this.metricEvaluationsPerSample = new LinkedList<>();

        this.initialized = false;
        this.evaluationPrepared = false;
        this.finished = false;
    }

    public static final MetricEvaluationManager DEFAULT = new MetricEvaluationManager(
            "SampleName",
            null,
            null,
            null,
            DEFAULT_OUTPUT_DIR,
            true,
            true,
            true,
            4,
            1
    );

    public MetricEvaluationManager withName(String name) {
        return new MetricEvaluationManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withInputPaths(Path postIdPath, Path postHistoryPath, Path groundTruthPath) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withOutputDirPath(Path outputDirPath) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withValidate(boolean validate) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withDefaultSimilarityMetrics(boolean addDefaultSimilarityMetrics) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withRandomizeOrder(boolean randomizeOrder) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withNumberOfRepetitions(int numberOfRepetitions) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withThreadCount(int threadCount) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager initialize() {
        this.threadId = threadIdCounter.incrementAndGet();

        if (addDefaultSimilarityMetrics) {
            addDefaultSimilarityMetrics();
        }

        // ensure that input file exists (directories are tested in read methods)
        Util.ensureFileExists(postIdPath);

        logger.info("Thread " + threadId + ": Creating new MetricEvaluationManager for sample " + sampleName + " ...");

        try (CSVParser csvParser = new CSVParser(new FileReader(postIdPath.toFile()), csvFormatPostIds.withFirstRecordAsHeader())) {

            logger.info("Thread " + threadId + ": Reading PostIds from CSV file " + postIdPath.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postId = Integer.parseInt(currentRecord.get("PostId"));
                int postTypeId = Integer.parseInt(currentRecord.get("PostTypeId"));
                int versionCount = Integer.parseInt(currentRecord.get("VersionCount"));

                // add post id to set
                postIds.add(postId);

                // read post version list
                PostVersionList newPostVersionList = PostVersionList.readFromCSV(
                        postHistoryPath, postId, postTypeId, false
                );
                newPostVersionList.normalizeLinks();

                if (newPostVersionList.size() != versionCount) {
                    String msg = "Thread " + threadId + ": Version count expected to be " + versionCount + ", but was " + newPostVersionList.size();
                    logger.warning(msg);
                    throw new IllegalArgumentException(msg);
                }

                postVersionLists.put(postId, newPostVersionList);

                // read ground truth
                PostGroundTruth newPostGroundTruth = PostGroundTruth.readFromCSV(groundTruthPath, postId);

                if (newPostGroundTruth.getPossibleComparisons() != newPostVersionList.getPossibleComparisons()) {
                    String msg = "Thread " + threadId + ": Number of possible comparisons in ground truth is different " + "from number of possible comparisons in post history.";
                    logger.warning(msg);
                    throw new IllegalArgumentException(msg);
                }

                postGroundTruths.put(postId, newPostGroundTruth);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (validate && !validate()) {
            String msg = "Thread " + threadId + ": Post ground truth files and post version history files do not match.";
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
        }

        initialized = true;

        return this;
    }

    private void addDefaultSimilarityMetrics() {
        similarityMetrics.addAll(defaultSimilarityMetrics);
    }

    public boolean validate() {
        if (!evaluationPrepared) {
            prepareEvaluation();
        }
        for (MetricEvaluationPerSample sample : metricEvaluationsPerSample) {
            if (!sample.validate()) {
                return false;
            }
        }
        return true;
    }

    private void prepareEvaluation() {
        metricEvaluationsPerSample.clear();
        for (SimilarityMetric similarityMetric : similarityMetrics) {
            MetricEvaluationPerSample evaluationPerSample = new MetricEvaluationPerSample(
                    sampleName,
                    similarityMetric,
                    postIds,
                    postVersionLists,
                    postGroundTruths,
                    numberOfRepetitions,
                    randomizeOrder
            );
            evaluationPerSample.prepareEvaluation();
            metricEvaluationsPerSample.add(evaluationPerSample);
        }
    }

    private void randomizeOrder() {
        Collections.shuffle(metricEvaluationsPerSample, new Random());
    }

    @Override
    public void run() {
        logger.info("Thread " + threadId + " started for sample " + sampleName + "...");

        if (!initialized) {
            initialize();
        }

        if (!evaluationPrepared) {
            prepareEvaluation();
        }

        for (int currentRepetition = 1; currentRepetition <= numberOfRepetitions; currentRepetition++) {
            if (randomizeOrder) {
                logger.info( "Thread " + threadId + ": Randomizing order of similarity metrics for sample " + sampleName + "...");
                randomizeOrder();
            }

            int size = metricEvaluationsPerSample.size();
            for (int i = 0; i < size; i++) {
                MetricEvaluationPerSample evaluationPerSample = metricEvaluationsPerSample.get(i);

                // Locale.ROOT -> force '.' as decimal separator
                String progress = String.format(Locale.ROOT, "%.2f%%", (((double)(i+1))/size*100));
                logger.info( "Thread " + threadId + ": Starting evaluation " + (i+1) + " of " + size + " (" + progress + "), "
                        + "repetition " + currentRepetition + " of " + numberOfRepetitions + "...");

                synchronized (MetricEvaluationManager.class) {
                     evaluationPerSample.startEvaluation(currentRepetition);
                }
            }
        }

        logger.info("Thread " + threadId + ": Saving results for sample " + sampleName + "...");
        writeToCSV();
        logger.info("Thread " + threadId + ": Results saved.");

        this.finished = true;
        logger.info("Thread " + threadId + ": Finished.");
    }

    private void writeToCSV() {
        try {
            // create output directory if it does not exist
            Util.createDirectory(outputDirPath);

            // output file by version
            Path outputFilePerVersion = Paths.get(this.outputDirPath.toString(), sampleName + "_per_version.csv");
            Util.deleteFileIfExists(outputFilePerVersion);

            // output file aggregated by post
            Path outputFilePerPost = Paths.get(this.outputDirPath.toString(), sampleName + "_per_post.csv");
            Util.deleteFileIfExists(outputFilePerPost);

            // output file aggregated by sample
            Path outputFilePerSample = Paths.get(this.outputDirPath.toString(), sampleName + "_per_sample.csv");
            Util.deleteFileIfExists(outputFilePerSample);

            logger.info("Thread " + threadId + ": Writing metric evaluation results per version to CSV file " + outputFilePerVersion.toFile().getName() + " ...");
            logger.info("Thread " + threadId + ": Writing metric evaluation results per post to CSV file " + outputFilePerPost.toFile().getName() + " ...");
            logger.info("Thread " + threadId + ": Writing metric evaluation results per sample to CSV file " + outputFilePerSample.toFile().getName() + " ...");
            try (CSVPrinter csvPrinterVersion = new CSVPrinter(new FileWriter(outputFilePerVersion.toFile()), csvFormatMetricEvaluationPerVersion);
                 CSVPrinter csvPrinterPost = new CSVPrinter(new FileWriter(outputFilePerPost.toFile()), csvFormatMetricEvaluationPerPost);
                 CSVPrinter csvPrinterSample = new CSVPrinter(new FileWriter(outputFilePerSample.toFile()), csvFormatMetricEvaluationPerSample)) {

                // header is automatically written

                // write results per per post and per version
                for (MetricEvaluationPerSample evaluationPerSample : metricEvaluationsPerSample) {
                    for (MetricEvaluationPerPost evaluationPerPost : evaluationPerSample) {
                        evaluationPerPost.writeToCSV(csvPrinterPost, csvPrinterVersion);
                    }
                }

                // write aggregated results per sample
                for (MetricEvaluationPerSample evaluationPerSample : metricEvaluationsPerSample) {
                    evaluationPerSample.writeToCSV(csvPrinterSample);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, PostGroundTruth> getPostGroundTruths() {
        return postGroundTruths;
    }

    public Map<Integer, PostVersionList> getPostVersionLists() {
        return postVersionLists;
    }

    public Set<Integer> getPostIds() {
        return postVersionLists.keySet();
    }

    public void addSimilarityMetric(SimilarityMetric metric) {
        similarityMetrics.add(metric);
        evaluationPrepared = false;
    }

    public MetricEvaluationPerPost getMetricEvaluation(int postId, String metricName, double threshold) {
        for (MetricEvaluationPerSample evaluationPerSample : metricEvaluationsPerSample) {
            if (!evaluationPerSample.getSimilarityMetric().getName().equals(metricName)
                    || evaluationPerSample.getSimilarityMetric().getThreshold() != threshold) {
                continue;
            }
            // correct samples found
            for (MetricEvaluationPerPost evaluationPerPost : evaluationPerSample) {
                if (evaluationPerPost.getPostId() == postId) {
                    // correct post found
                    return evaluationPerPost;
                }
            }
        }

        String msg = "Thread " + threadId + ": Similarity metric " + metricName + " not found in evaluation samples.";
        logger.warning(msg);
        throw new IllegalArgumentException(msg);
    }

    public String getSampleName() {
        return sampleName;
    }

    public boolean isFinished() {
        return finished;
    }

    /*
     * Create managers for all samples in a directory.
     * A sample directory must contain:
     *   * one CSV file with a list of all post ids in the sample
     *   * a directory named "files" with the post version lists as CSV files
     *   * a directory named "completed" with the post ground truths as CSV files
     */
    public static List<MetricEvaluationManager> createManagersFromSampleDirectories(
            Path samplesDir,
            Path outputDir,
            boolean addDefaultMetricsAndThresholds) {

        try {
            Util.ensureEmptyDirectoryExists(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Creating MetricEvaluationManagers for samples in directory " + samplesDir + "...");
        List<MetricEvaluationManager> managers = new LinkedList<>();

        try (Stream<Path> paths = Files.list(samplesDir)) {
            paths.forEach(
                    path -> {
                        // only consider directories (ignore, e.g., .DS_Store files on macOS)
                        if (!Files.isDirectory(path)) {
                            return;
                        }
                        String name = path.toFile().getName();
                        Path pathToPostIdList = Paths.get(path.toString(), name + ".csv");
                        Path pathToPostHistory = Paths.get(path.toString(), "files");
                        Path pathToGroundTruth = Paths.get(path.toString(), "completed");

                        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                                .withName(name)
                                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                                .withOutputDirPath(outputDir)
                                .withDefaultSimilarityMetrics(addDefaultMetricsAndThresholds)
                                .initialize();

                        managers.add(manager);
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return managers;
    }

    public static void aggregateAndWriteSampleResults(List<MetricEvaluationManager> managers, File outputFile) {
        // aggregate results over all samples
        Map<SimilarityMetric, MetricResult> aggregatedMetricResultsText = new HashMap<>();
        Map<SimilarityMetric, MetricResult> aggregatedMetricResultsCode = new HashMap<>();
        for (int i=0; i<managers.size(); i++) {
            MetricEvaluationManager manager = managers.get(i);
            if (i==0) {
                for (MetricEvaluationPerSample evaluation : manager.metricEvaluationsPerSample) {
                    MetricResult resultText = evaluation.getResultAggregatedBySampleText();
                    aggregatedMetricResultsText.put(evaluation.getSimilarityMetric(), resultText);

                    MetricResult resultCode = evaluation.getResultAggregatedBySampleCode();
                    aggregatedMetricResultsCode.put(evaluation.getSimilarityMetric(), resultCode);
                }
            } else {
                for (MetricEvaluationPerSample evaluation : manager.metricEvaluationsPerSample) {
                    MetricResult newResultText = evaluation.getResultAggregatedBySampleText();
                    MetricResult resultText = aggregatedMetricResultsText.get(newResultText.getSimilarityMetric());
                    resultText.add(newResultText);

                    MetricResult newResultCode = evaluation.getResultAggregatedBySampleCode();
                    MetricResult resultCode = aggregatedMetricResultsCode.get(newResultCode.getSimilarityMetric());
                    resultCode.add(newResultCode);
                }
            }
        }

        // get max. failures
        int maxFailuresText = 0;
        int maxFailuresCode = 0;
        for (MetricResult resultText : aggregatedMetricResultsText.values()) {
            maxFailuresText = Math.max(maxFailuresText, resultText.getFailedPredecessorComparisons());
        }
        for (MetricResult resultCode : aggregatedMetricResultsCode.values()) {
            maxFailuresCode = Math.max(maxFailuresCode, resultCode.getFailedPredecessorComparisons());
        }

        // write aggregated results
        try (CSVPrinter csvPrinterAggregated = new CSVPrinter(new FileWriter(outputFile), csvFormatMetricEvaluationPerSample)) {
            for (SimilarityMetric similarityMetric : aggregatedMetricResultsText.keySet()) {
                MetricResult aggregatedResultText = aggregatedMetricResultsText.get(similarityMetric);
                MetricResult aggregatedResultCode = aggregatedMetricResultsCode.get(similarityMetric);

                // validate results
                MetricResult.validate(aggregatedResultText, aggregatedResultCode);

                // "MetricType", "Metric", "Threshold", "Runtime"
                // "InformednessText", "MarkednessText", "MatthewsCorrelationText", "FScoreText",
                // "InformednessCode", "MarkednessCode", "MatthewsCorrelationCode", "FScoreCode",
                // "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons",
                // "TextBlockVersionCount", "PossibleComparisonsText",
                // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText",
                // "PrecisionText", "RecallText", "InversePrecisionText", "InverseRecallText", "FailureRateText",
                // "CodeBlockVersionCount", "PossibleComparisonsCode",
                // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode",
                // "PrecisionCode", "RecallCode", "InversePrecisionCode", "InverseRecallCode", "FailureRateCode"
                csvPrinterAggregated.printRecord(
                        similarityMetric.getType(),
                        similarityMetric.getName(),
                        similarityMetric.getThreshold(),
                        aggregatedResultText.getRuntime(),

                        aggregatedResultText.getInformedness(),
                        aggregatedResultText.getMarkedness(),
                        aggregatedResultText.getMatthewsCorrelation(),
                        aggregatedResultText.getFScore(),

                        aggregatedResultCode.getInformedness(),
                        aggregatedResultCode.getMarkedness(),
                        aggregatedResultCode.getMatthewsCorrelation(),
                        aggregatedResultCode.getFScore(),

                        aggregatedResultText.getPostCount(),
                        aggregatedResultText.getPostVersionCount(),
                        aggregatedResultText.getPostBlockVersionCount() + aggregatedResultCode.getPostBlockVersionCount(),
                        aggregatedResultText.getPossibleComparisons() + aggregatedResultCode.getPossibleComparisons(),

                        aggregatedResultText.getPostBlockVersionCount(),
                        aggregatedResultText.getPossibleComparisons(),

                        aggregatedResultText.getTruePositives(),
                        aggregatedResultText.getTrueNegatives(),
                        aggregatedResultText.getFalsePositives(),
                        aggregatedResultText.getFalseNegatives(),
                        aggregatedResultText.getFailedPredecessorComparisons(),

                        aggregatedResultText.getPrecision(),
                        aggregatedResultText.getRecall(),
                        aggregatedResultText.getInversePrecision(),
                        aggregatedResultText.getInverseRecall(),
                        aggregatedResultText.getFailureRate(),

                        aggregatedResultCode.getPostBlockVersionCount(),
                        aggregatedResultCode.getPossibleComparisons(),

                        aggregatedResultCode.getTruePositives(),
                        aggregatedResultCode.getTrueNegatives(),
                        aggregatedResultCode.getFalsePositives(),
                        aggregatedResultCode.getFalseNegatives(),
                        aggregatedResultCode.getFailedPredecessorComparisons(),

                        aggregatedResultCode.getPrecision(),
                        aggregatedResultCode.getRecall(),
                        aggregatedResultCode.getInversePrecision(),
                        aggregatedResultCode.getInverseRecall(),
                        aggregatedResultCode.getFailureRate()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Aggregated results over all samples saved.");
    }

    public static SimilarityMetric getDefaultSimilarityMetric(String name, double threshold) {
        for (SimilarityMetric metric : defaultSimilarityMetrics) {
            if (metric.getName().equals(name) && metric.getThreshold() == threshold) {
                return metric;
            }
        }
        String msg = "No default similarity metric with name " + name + " and threshold " + threshold + " found.";
        logger.warning(msg);
        throw new IllegalArgumentException(msg);
    }

    /**
     * Add metrics selected after evaluation, with additional thresholds and baseline metric (equal).
     */
    public void addSelectedSimilarityMetrics(Path selectedMetricsDir) {
        Util.ensureDirectoryExists(selectedMetricsDir);

        Set<String> metricNames = new HashSet<>();
        List<SimilarityMetric> defaultSimilarityMetrics = new LinkedList<>();
        List<Double> thresholds = Arrays.asList(
                0.0, 0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1,
                     0.11, 0.12, 0.13, 0.14, 0.15, 0.16, 0.17, 0.18, 0.19, 0.2,
                     0.21, 0.22, 0.23, 0.24, 0.25, 0.26, 0.27, 0.28, 0.29, 0.3,
                     0.31, 0.32, 0.33, 0.34, 0.35, 0.36, 0.37, 0.38, 0.39, 0.4,
                     0.41, 0.42, 0.43, 0.44, 0.45, 0.46, 0.47, 0.48, 0.49, 0.5,
                     0.51, 0.52, 0.53, 0.54, 0.55, 0.56, 0.57, 0.58, 0.59, 0.6,
                     0.61, 0.62, 0.63, 0.64, 0.65, 0.66, 0.67, 0.68, 0.69, 0.7,
                     0.71, 0.72, 0.73, 0.74, 0.75, 0.76, 0.77, 0.78, 0.79, 0.8,
                     0.81, 0.82, 0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.9,
                     0.91, 0.92, 0.93, 0.94, 0.95, 0.96, 0.97, 0.98, 0.99, 1.0
        );

        Path pathToSelectedMetrics = Paths.get(selectedMetricsDir.toString(), "selected_metrics.csv");
        Util.ensureFileExists(pathToSelectedMetrics);
        Path pathToBackupMetrics = Paths.get(selectedMetricsDir.toString(), "backup_metrics.csv");
        Util.ensureFileExists(pathToBackupMetrics);

        logger.info("Thread " + threadId + ": Reading selected and backup metrics from directory " + selectedMetricsDir + " ...");

        try (CSVParser csvParserSelectedMetrics = new CSVParser(new FileReader(pathToSelectedMetrics.toFile()), csvFormatSelectedMetrics.withFirstRecordAsHeader());
             CSVParser csvParserBackupMetrics = new CSVParser(new FileReader(pathToBackupMetrics.toFile()), csvFormatSelectedMetrics.withFirstRecordAsHeader())) {

            logger.info("Thread " + threadId + ": Reading selected metrics from CSV file " + pathToSelectedMetrics.toFile().toString() + " ...");
            for (CSVRecord currentRecord : csvParserSelectedMetrics) {
                String metric = currentRecord.get("Metric").trim();
                if (metric.length() > 0) {
                    metricNames.add(metric);
                }
            }

            logger.info("Thread " + threadId + ": Reading backup metrics from CSV file " + pathToBackupMetrics.toFile().toString() + " ...");
            for (CSVRecord currentRecord : csvParserBackupMetrics) {
                String metric = currentRecord.get("Metric").trim();
                if (metric.length() > 0) {
                    metricNames.add(metric);
                }
            }

            logger.info("Thread " + threadId + ": " + metricNames.size() + " metric names read.");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // retrieve default metrics
        for (String metricName : metricNames) {
            SimilarityMetric metric = getDefaultSimilarityMetric(metricName, 0.5);
            defaultSimilarityMetrics.add(metric);
        }

        // do not use for loop with += 0.01 --> leads to rounding errors
        for (double threshold : thresholds) {
            // baseline metric
            addSimilarityMetric(new SimilarityMetric(
                    "equal",
                    de.unitrier.st.stringsimilarity.equal.Variants::equal,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold
            ));

            // add selected and backup metrics
            for (SimilarityMetric defaultMetric : defaultSimilarityMetrics) {
                addSimilarityMetric(defaultMetric.createCopyWithNewThreshold(threshold));
            }
        }

        logger.info("Thread " + threadId + ": " + similarityMetrics.size() + " metrics added.");
    }

    /**
     * Add default metrics.
     */
    private static void createDefaultSimilarityMetrics() {
        List<Double> thresholds = Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);

        // do not use for loop with += 0.1 --> leads to rounding errors
        for (double threshold : thresholds) {

            // ****** Equality based *****

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "equal",
                    de.unitrier.st.stringsimilarity.equal.Variants::equal,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "equalNormalized",
                    de.unitrier.st.stringsimilarity.equal.Variants::equalNormalized,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenEqual",
                    de.unitrier.st.stringsimilarity.equal.Variants::tokenEqual,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenEqualNormalized",
                    de.unitrier.st.stringsimilarity.equal.Variants::tokenEqualNormalized,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );


            // ****** Edit based *****

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "levenshtein",
                    de.unitrier.st.stringsimilarity.edit.Variants::levenshtein,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "levenshteinNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::levenshteinNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "damerauLevenshtein",
                    de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshtein,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "damerauLevenshteinNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshteinNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "optimalAlignment",
                    de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignment,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "optimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignmentNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "longestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequence,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "longestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );


            // ****** Fingerprint based *****

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );


            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );


            // ****** Profile based *****

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanTokenNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanTokenNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanTwoGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanThreeGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanFourGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanFiveGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanFiveGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanTwoShingleNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanThreeShingleNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );



            // ****** Set based *****

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenDice",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleDice",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleDice",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "tokenOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            defaultSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
        }
    }
}
