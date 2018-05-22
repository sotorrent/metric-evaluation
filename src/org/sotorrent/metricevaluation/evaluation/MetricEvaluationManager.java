package org.sotorrent.metricevaluation.evaluation;

import org.apache.commons.csv.*;
import org.sotorrent.posthistoryextractor.Config;
import org.sotorrent.posthistoryextractor.gt.PostGroundTruth;
import org.sotorrent.posthistoryextractor.version.PostVersionList;
import org.sotorrent.util.FileUtils;
import org.sotorrent.util.LogUtils;

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
    private static final List<SimilarityMetric> allSimilarityMetrics = new LinkedList<>();
    private static final List<SimilarityMetric> selectedSimilarityMetrics = new LinkedList<>();
    private static final List<SimilarityMetric> combinedSimilarityMetrics = new LinkedList<>();
    private static final SimilarityMetric defaultSimilarityMetric = new SimilarityMetric(
            "default", SimilarityMetric.MetricType.DEFAULT,
            "default", SimilarityMetric.MetricType.DEFAULT,
            "default", SimilarityMetric.MetricType.DEFAULT,
            "default", SimilarityMetric.MetricType.DEFAULT,
            Config.DEFAULT
    );

    private int threadId;
    private String sampleName;
    private boolean addAllSimilarityMetrics;
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
            logger = LogUtils.getClassLogger(MetricEvaluationManager.class);
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
                .withHeader("MetricTypeText", "MetricText", "ThresholdText", "MetricTypeTextBackup", "MetricTextBackup", "ThresholdTextBackup", "MetricTypeCode", "MetricCode", "ThresholdCode", "MetricTypeCodeBackup", "MetricCodeBackup", "ThresholdCodeBackup", "PostId", "Runtime", "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons", "TextBlockVersionCount", "PossibleComparisonsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "CodeBlockVersionCount", "PossibleComparisonsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for metric comparison results (per version, i.e., per PostHistoryId)
        csvFormatMetricEvaluationPerVersion = CSVFormat.DEFAULT
                .withHeader("MetricTypeText", "MetricText", "ThresholdText", "MetricTypeTextBackup", "MetricTextBackup", "ThresholdTextBackup", "MetricTypeCode", "MetricCode", "ThresholdCode", "MetricTypeCodeBackup", "MetricCodeBackup", "ThresholdCodeBackup", "PostId", "PostHistoryId", "Runtime", "PossibleComparisons", "TextBlockCount", "PossibleComparisonsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "CodeBlockCount", "PossibleComparisonsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for aggregated metric comparison results (per (metric, threshold) combination)
        csvFormatMetricEvaluationPerSample = CSVFormat.DEFAULT
                .withHeader("MetricTypeText", "MetricText", "ThresholdText", "MetricTypeTextBackup", "MetricTextBackup", "ThresholdTextBackup", "MetricTypeCode", "MetricCode", "ThresholdCode", "MetricTypeCodeBackup", "MetricCodeBackup", "ThresholdCodeBackup", "Runtime", "InformednessText", "MarkednessText", "MatthewsCorrelationText", "FScoreText", "InformednessCode", "MarkednessCode", "MatthewsCorrelationCode", "FScoreCode", "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons", "TextBlockVersionCount", "PossibleComparisonsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText", "PrecisionText", "RecallText", "InversePrecisionText", "InverseRecallText", "FailureRateText", "CodeBlockVersionCount", "PossibleComparisonsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode", "PrecisionCode", "RecallCode", "InversePrecisionCode", "InverseRecallCode", "FailureRateCode")
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
        createAllSimilarityMetrics();
    }

    private MetricEvaluationManager(String sampleName, Path postIdPath,
                                    Path postHistoryPath, Path groundTruthPath, Path outputDirPath,
                                    boolean validate, boolean addAllSimilarityMetrics, boolean randomizeOrder,
                                    int numberOfRepetitions, int threadCount) {

        this.sampleName = sampleName;

        this.postIdPath = postIdPath;
        this.postHistoryPath = postHistoryPath;
        this.groundTruthPath = groundTruthPath;
        this.outputDirPath = outputDirPath;

        this.validate = validate;
        this.addAllSimilarityMetrics = addAllSimilarityMetrics;
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
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withInputPaths(Path postIdPath, Path postHistoryPath, Path groundTruthPath) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withOutputDirPath(Path outputDirPath) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withValidate(boolean validate) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withAllSimilarityMetrics(boolean addAllSimilarityMetrics) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withRandomizeOrder(boolean randomizeOrder) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withNumberOfRepetitions(int numberOfRepetitions) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withThreadCount(int threadCount) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addAllSimilarityMetrics, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager initialize() {
        this.threadId = threadIdCounter.incrementAndGet();

        if (addAllSimilarityMetrics) {
            addAllSimilarityMetrics();
        }

        // ensure that input file exists (directories are tested in read methods)
        FileUtils.checkIfFileExists(postIdPath);

        logger.info("Thread " + threadId + ": Creating new MetricEvaluationManager for sample " + sampleName + " ...");

        try (CSVParser csvParser = new CSVParser(new FileReader(postIdPath.toFile()), csvFormatPostIds.withFirstRecordAsHeader())) {

            logger.info("Thread " + threadId + ": Reading PostIds from CSV file " + postIdPath.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postId = Integer.parseInt(currentRecord.get("PostId"));
                byte postTypeId = Byte.parseByte(currentRecord.get("PostTypeId"));
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
                    String msg = "Thread " + threadId + ": Number of possible comparisons in ground truth is different "
                            + "from number of possible comparisons in post history for post id " + postId + ".";
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

    private void addAllSimilarityMetrics() {
        similarityMetrics.addAll(allSimilarityMetrics);
    }

    public void addSelectedSimilarityMetrics() {
        similarityMetrics.addAll(selectedSimilarityMetrics);
    }

    public void addCombinedSimilarityMetrics() {
        similarityMetrics.addAll(combinedSimilarityMetrics);
    }

    public void addDefaultSimilarityMetric() {
        similarityMetrics.add(defaultSimilarityMetric);
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
            FileUtils.createDirectory(outputDirPath);

            // output file by version
            Path outputFilePerVersion = Paths.get(this.outputDirPath.toString(), sampleName + "_per_version.csv");
            FileUtils.deleteFileIfExists(outputFilePerVersion);

            // output file aggregated by post
            Path outputFilePerPost = Paths.get(this.outputDirPath.toString(), sampleName + "_per_post.csv");
            FileUtils.deleteFileIfExists(outputFilePerPost);

            // output file aggregated by sample
            Path outputFilePerSample = Paths.get(this.outputDirPath.toString(), sampleName + "_per_sample.csv");
            FileUtils.deleteFileIfExists(outputFilePerSample);

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
            SimilarityMetric currentMetric = evaluationPerSample.getSimilarityMetric();
            if (!currentMetric.getNameCode().equals(metricName)
                    || !currentMetric.getNameText().equals(metricName)
                    || currentMetric.getConfig().getCodeSimilarityThreshold() != threshold
                    || currentMetric.getConfig().getTextSimilarityThreshold() != threshold) {
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
            FileUtils.ensureEmptyDirectoryExists(outputDir);
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
                                .withAllSimilarityMetrics(addDefaultMetricsAndThresholds)
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

                // "MetricTypeText", "MetricText", "ThresholdText",
                // "MetricTypeTextBackup", "MetricTextBackup", "ThresholdTextBackup",
                // "MetricTypeCode", "MetricCode", "ThresholdCode",
                // "MetricTypeCodeBackup", "MetricCodeBackup", "ThresholdCodeBackup",
                // "Runtime"
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
                        similarityMetric.getTypeText(),
                        similarityMetric.getNameText(),
                        similarityMetric.getConfig().getTextSimilarityThreshold(),

                        similarityMetric.getBackupTypeText(),
                        similarityMetric.getBackupNameText(),
                        similarityMetric.getConfig().getTextBackupSimilarityThreshold(),

                        similarityMetric.getTypeCode(),
                        similarityMetric.getNameCode(),
                        similarityMetric.getConfig().getCodeSimilarityThreshold(),

                        similarityMetric.getBackupTypeCode(),
                        similarityMetric.getBackupNameCode(),
                        similarityMetric.getConfig().getCodeBackupSimilarityThreshold(),

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

    public static SimilarityMetric getSimilarityMetric(String name, double threshold) {
        for (SimilarityMetric metric : allSimilarityMetrics) {
            if (metric.getNameText().equals(name)
                    && metric.getNameCode().equals(name)
                    && metric.getConfig().getTextSimilarityThreshold() == threshold
                    && metric.getConfig().getCodeSimilarityThreshold() == threshold) {
                return metric;
            }
        }
        String msg = "No default similarity metric with name " + name + " and threshold " + threshold + " found.";
        logger.warning(msg);
        throw new IllegalArgumentException(msg);
    }

    /*
     * Add metrics selected after evaluation, with additional thresholds and baseline metric (equal).
     */
    public static void createSelectedSimilarityMetrics(Path selectedMetricsDir) {
        try {
            FileUtils.ensureDirectoryExists(selectedMetricsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        FileUtils.checkIfFileExists(pathToSelectedMetrics);
        Path pathToBackupMetrics = Paths.get(selectedMetricsDir.toString(), "backup_metrics.csv");
        FileUtils.checkIfFileExists(pathToBackupMetrics);

        logger.info("Reading selected and backup metrics from directory " + selectedMetricsDir + " ...");

        try (CSVParser csvParserSelectedMetrics = new CSVParser(new FileReader(pathToSelectedMetrics.toFile()), csvFormatSelectedMetrics.withFirstRecordAsHeader());
             CSVParser csvParserBackupMetrics = new CSVParser(new FileReader(pathToBackupMetrics.toFile()), csvFormatSelectedMetrics.withFirstRecordAsHeader())) {

            logger.info("Reading selected metrics from CSV file " + pathToSelectedMetrics.toFile().toString() + " ...");
            for (CSVRecord currentRecord : csvParserSelectedMetrics) {
                String metric = currentRecord.get("Metric").trim();
                if (metric.length() > 0) {
                    metricNames.add(metric);
                }
            }

            logger.info("Reading backup metrics from CSV file " + pathToBackupMetrics.toFile().toString() + " ...");
            for (CSVRecord currentRecord : csvParserBackupMetrics) {
                String metric = currentRecord.get("Metric").trim();
                if (metric.length() > 0) {
                    metricNames.add(metric);
                }
            }

            logger.info(metricNames.size() + " metric names read.");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // retrieve default metrics
        for (String metricName : metricNames) {
            SimilarityMetric metric = getSimilarityMetric(metricName, 0.5);
            defaultSimilarityMetrics.add(metric);
        }

        // do not use for loop with += 0.01 --> leads to rounding errors
        for (double threshold : thresholds) {
            // baseline metric
            selectedSimilarityMetrics.add(new SimilarityMetric(
                    "equal",
                    org.sotorrent.stringsimilarity.equal.Variants::equal,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold
            ));

            // add selected and backup metrics
            for (SimilarityMetric defaultMetric : defaultSimilarityMetrics) {
                Config config = defaultMetric.getConfig();
                selectedSimilarityMetrics.add(defaultMetric.withConfig(config
                        .withTextSimilarityThreshold(threshold)
                        .withCodeSimilarityThreshold(threshold))
                );
            }
        }

        logger.info(selectedSimilarityMetrics.size() + " metrics added.");
    }

    /*
     * Add combined metrics selected after evaluation, with baseline metric (equal).
     */
    public static void createCombinedSimilarityMetrics() {

        List<SimilarityMetric> metricsText = new LinkedList<>();
        metricsText.add(
            new SimilarityMetric(
                    "manhattanFourGramNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    0.16)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanFourGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.17)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanFourGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.18)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanThreeGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.20)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanThreeGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.21)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanThreeGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.22)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanThreeGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.23)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanThreeGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.24)
        );
        metricsText.add(
                new SimilarityMetric(
                        "manhattanThreeGramNormalized",
                        org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                        SimilarityMetric.MetricType.PROFILE,
                        0.25)
        );
        metricsText.add(
                new SimilarityMetric(
                        "threeGramDice",
                        org.sotorrent.stringsimilarity.set.Variants::threeGramDice,
                        SimilarityMetric.MetricType.SET,
                        0.30)
        );
        metricsText.add(
                new SimilarityMetric(
                        "fourGramDice",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDice,
                        SimilarityMetric.MetricType.SET,
                        0.22)
        );
        metricsText.add(
                new SimilarityMetric(
                        "fourGramDice",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDice,
                        SimilarityMetric.MetricType.SET,
                        0.23)
        );
        metricsText.add(
                new SimilarityMetric(
                        "fourGramJaccard",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramJaccard,
                        SimilarityMetric.MetricType.SET,
                        0.13)
        );
        logger.info(metricsText.size() + " text metrics added.");

        List<SimilarityMetric> metricsTextBackup = new LinkedList<>();
        metricsTextBackup.add(
                new SimilarityMetric(
                        "cosineTokenNormalizedTermFrequency",
                        org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency,
                        SimilarityMetric.MetricType.PROFILE,
                        0.36)
        );
        metricsTextBackup.add(
                new SimilarityMetric(
                        "cosineTokenNormalizedTermFrequency",
                        org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency,
                        SimilarityMetric.MetricType.PROFILE,
                        0.37)
        );
        metricsTextBackup.add(
                new SimilarityMetric(
                        "tokenJaccardNormalized",
                        org.sotorrent.stringsimilarity.set.Variants::tokenJaccardNormalized,
                        SimilarityMetric.MetricType.SET,
                        0.37)
        );
        logger.info(metricsTextBackup.size() + " text backup metrics added.");

        List<SimilarityMetric> metricsCode = new LinkedList<>();
        metricsCode.add(
                new SimilarityMetric(
                        "winnowingFourGramDiceNormalized",
                        org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                        SimilarityMetric.MetricType.FINGERPRINT,
                        0.20)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "winnowingFourGramDiceNormalized",
                        org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                        SimilarityMetric.MetricType.FINGERPRINT,
                        0.23)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "winnowingFourGramDiceNormalized",
                        org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                        SimilarityMetric.MetricType.FINGERPRINT,
                        0.24)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "winnowingFourGramDiceNormalized",
                        org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                        SimilarityMetric.MetricType.FINGERPRINT,
                        0.25)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "winnowingFourGramDiceNormalized",
                        org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                        SimilarityMetric.MetricType.FINGERPRINT,
                        0.26)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramDiceNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.26)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramDiceNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.27)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramDiceNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.28)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramDiceNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.29)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramDiceNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.30)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramDiceNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.31)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramJaccardNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.15)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramJaccardNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.16)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "fourGramJaccardNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.17)
        );
        metricsCode.add(
                new SimilarityMetric(
                        "threeGramJaccardNormalizedPadding",
                        org.sotorrent.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding,
                        SimilarityMetric.MetricType.SET,
                        0.20)
        );
        logger.info(metricsCode.size() + " code metrics added.");

        List<SimilarityMetric> metricsCodeBackup = new LinkedList<>();
        metricsCodeBackup.add(
                new SimilarityMetric(
                        "cosineTokenNormalizedNormalizedTermFrequency",
                        org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency,
                        SimilarityMetric.MetricType.PROFILE,
                        0.26)
        );
        metricsCodeBackup.add(
                new SimilarityMetric(
                        "cosineTokenNormalizedNormalizedTermFrequency",
                        org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency,
                        SimilarityMetric.MetricType.PROFILE,
                        0.27)
        );
        logger.info(metricsCodeBackup.size() + " code backup metrics added.");

        for (SimilarityMetric metricText : metricsText) {
            for (SimilarityMetric metricTextBackup : metricsTextBackup) {
                for (SimilarityMetric metricCode : metricsCode) {
                    for (SimilarityMetric metricCodeBackup : metricsCodeBackup) {
                        combinedSimilarityMetrics.add(new SimilarityMetric(
                                metricText.getNameText(), metricText.getTypeText(), metricTextBackup.getNameText(), metricTextBackup.getTypeText(),
                                metricCode.getNameCode(), metricCode.getTypeCode(), metricCodeBackup.getNameCode(), metricCodeBackup.getTypeCode(),
                                Config.METRICS_COMPARISON
                                        .withTextSimilarityMetric(metricText.getConfig().getTextSimilarityMetric())
                                        .withTextSimilarityThreshold(metricText.getConfig().getTextSimilarityThreshold())
                                        .withTextBackupSimilarityMetric(metricTextBackup.getConfig().getTextSimilarityMetric())
                                        .withTextBackupSimilarityThreshold(metricTextBackup.getConfig().getTextSimilarityThreshold())
                                        .withCodeSimilarityMetric(metricCode.getConfig().getCodeSimilarityMetric())
                                        .withCodeSimilarityThreshold(metricCode.getConfig().getCodeSimilarityThreshold())
                                        .withCodeBackupSimilarityMetric(metricCodeBackup.getConfig().getCodeSimilarityMetric())
                                        .withCodeBackupSimilarityThreshold(metricCodeBackup.getConfig().getCodeSimilarityThreshold())
                        ));
                    }
                }
            }
        }

        logger.info(combinedSimilarityMetrics.size() + " metrics added.");
    }

    /**
     * Add all available metrics.
     */
    private static void createAllSimilarityMetrics() {
        List<Double> thresholds = Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);

        // do not use for loop with += 0.1 --> leads to rounding errors
        for (double threshold : thresholds) {

            // ****** Equality based *****

            allSimilarityMetrics.add(new SimilarityMetric(
                    "equal",
                    org.sotorrent.stringsimilarity.equal.Variants::equal,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "equalNormalized",
                    org.sotorrent.stringsimilarity.equal.Variants::equalNormalized,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenEqual",
                    org.sotorrent.stringsimilarity.equal.Variants::tokenEqual,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenEqualNormalized",
                    org.sotorrent.stringsimilarity.equal.Variants::tokenEqualNormalized,
                    SimilarityMetric.MetricType.EQUAL,
                    threshold)
            );


            // ****** Edit based *****

            allSimilarityMetrics.add(new SimilarityMetric(
                    "levenshtein",
                    org.sotorrent.stringsimilarity.edit.Variants::levenshtein,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "levenshteinNormalized",
                    org.sotorrent.stringsimilarity.edit.Variants::levenshteinNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "damerauLevenshtein",
                    org.sotorrent.stringsimilarity.edit.Variants::damerauLevenshtein,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "damerauLevenshteinNormalized",
                    org.sotorrent.stringsimilarity.edit.Variants::damerauLevenshteinNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "optimalAlignment",
                    org.sotorrent.stringsimilarity.edit.Variants::optimalAlignment,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "optimalAlignmentNormalized",
                    org.sotorrent.stringsimilarity.edit.Variants::optimalAlignmentNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "longestCommonSubsequence",
                    org.sotorrent.stringsimilarity.edit.Variants::longestCommonSubsequence,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "longestCommonSubsequenceNormalized",
                    org.sotorrent.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    threshold)
            );


            // ****** Fingerprint based *****

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramJaccard",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramJaccard",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramJaccard",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramJaccard",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramDice",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramDice",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramDice",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramDice",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramDiceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramDiceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramDiceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramDiceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOverlap",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOverlap",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOverlap",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOverlap",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramLongestCommonSubsequence",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramLongestCommonSubsequence",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramLongestCommonSubsequence",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramLongestCommonSubsequence",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );


            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramLongestCommonSubsequenceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramLongestCommonSubsequenceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramLongestCommonSubsequenceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramLongestCommonSubsequenceNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOptimalAlignment",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOptimalAlignment",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOptimalAlignment",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOptimalAlignment",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOptimalAlignmentNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOptimalAlignmentNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOptimalAlignmentNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOptimalAlignmentNormalized",
                    org.sotorrent.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    threshold)
            );


            // ****** Profile based *****

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedBool",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedNormalizedTermFrequency",
                    org.sotorrent.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanTokenNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanTokenNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanTwoGramNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanTwoGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanThreeGramNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanFourGramNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanFiveGramNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanFiveGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanTwoShingleNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "manhattanThreeShingleNormalized",
                    org.sotorrent.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    threshold)
            );



            // ****** Set based *****

            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::tokenJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::tokenJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccardNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccardNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccardNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccardNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::twoShingleJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleJaccard",
                    org.sotorrent.stringsimilarity.set.Variants::threeShingleJaccard,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::twoShingleJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleJaccardNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::threeShingleJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenDice",
                    org.sotorrent.stringsimilarity.set.Variants::tokenDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::tokenDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramDice",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramDice",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramDice",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramDice",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramDiceNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramDiceNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramDiceNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramDiceNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleDice",
                    org.sotorrent.stringsimilarity.set.Variants::twoShingleDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleDice",
                    org.sotorrent.stringsimilarity.set.Variants::threeShingleDice,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::twoShingleDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleDiceNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::threeShingleDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::tokenOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "tokenOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::tokenOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlapNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlapNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlapNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlapNormalizedPadding",
                    org.sotorrent.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::twoShingleOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleOverlap",
                    org.sotorrent.stringsimilarity.set.Variants::threeShingleOverlap,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );

            allSimilarityMetrics.add(new SimilarityMetric(
                    "twoShingleOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::twoShingleOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
            allSimilarityMetrics.add(new SimilarityMetric(
                    "threeShingleOverlapNormalized",
                    org.sotorrent.stringsimilarity.set.Variants::threeShingleOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    threshold)
            );
        }
    }
}
