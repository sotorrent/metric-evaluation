package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

public class MetricEvaluationManager implements Runnable {
    private static int threadIdCounter = 0;

    private static Logger logger = null;
    static final CSVFormat csvFormatPostIds;
    static final CSVFormat csvFormatMetricEvaluationPerPost;
    public static final CSVFormat csvFormatMetricEvaluationPerVersion;
    private static final CSVFormat csvFormatMetricEvaluationPerSample;
    private static final Path DEFAULT_OUTPUT_DIR = Paths.get("output");

    private int threadId;
    private String sampleName;
    private boolean addDefaultMetricsAndThresholds;
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

    static {
        // configure logger
        try {
            logger = getClassLogger(MetricEvaluationManager.class);
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
                .withHeader("MetricType", "Metric", "Threshold", "PostId", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections", "RuntimeText", "TextBlockVersionCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "RuntimeCode", "CodeBlockVersionCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for metric comparison results (per version, i.e., per PostHistoryId)
        csvFormatMetricEvaluationPerVersion = CSVFormat.DEFAULT
                .withHeader("MetricType", "Metric", "Threshold", "PostId", "PostHistoryId", "PossibleConnections", "RuntimeText", "TextBlockCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "RuntimeCode", "CodeBlockCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for aggregated metric comparison results (per (metric, threshold) combination)
        csvFormatMetricEvaluationPerSample = CSVFormat.DEFAULT
                .withHeader("MetricType", "Metric", "Threshold", "YoudensJText", "RuntimeText", "YoudensJCode", "RuntimeCode", "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections", "TextBlockVersionCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText", "PrecisionText", "RecallText", "SensitivityText", "SpecificityText", "FailureRateText", "CodeBlockVersionCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode", "PrecisionCode", "RecallCode", "SensitivityCode", "SpecificityCode", "FailureRateCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");
    }

    private MetricEvaluationManager(String sampleName, Path postIdPath,
                                    Path postHistoryPath, Path groundTruthPath, Path outputDirPath,
                                    boolean validate, boolean addDefaultMetricsAndThresholds, boolean randomizeOrder,
                                    int numberOfRepetitions, int threadCount) {
        this.threadId = -1;
        this.sampleName = sampleName;

        this.postIdPath = postIdPath;
        this.postHistoryPath = postHistoryPath;
        this.groundTruthPath = groundTruthPath;
        this.outputDirPath = outputDirPath;

        this.validate = validate;
        this.addDefaultMetricsAndThresholds = addDefaultMetricsAndThresholds;
        this.randomizeOrder = randomizeOrder;
        this.numberOfRepetitions = numberOfRepetitions;
        this.threadCount = threadCount;

        this.postIds = new HashSet<>();
        this.postGroundTruths = new HashMap<>();
        this.postVersionLists = new HashMap<>();

        this.similarityMetrics = new LinkedList<>();
        this.metricEvaluationsPerSample = new LinkedList<>();

        this.initialized = false;
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
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withInputPaths(Path postIdPath, Path postHistoryPath, Path groundTruthPath) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withOutputDirPath(Path outputDirPath) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withValidate(boolean validate) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withAddDefaultMetricsAndThresholds(boolean addDefaultMetricsAndThresholds) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withRandomizeOrder(boolean randomizeOrder) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withNumberOfRepetitions(int numberOfRepetitions) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager withThreadCount(int threadCount) {
        return new MetricEvaluationManager(sampleName, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions, threadCount
        );
    }

    public MetricEvaluationManager initialize() {
        if (addDefaultMetricsAndThresholds) {
            addDefaultSimilarityMetricsAndThresholds();
        }

        // ensure that input file exists (directories are tested in read methods)
        if (!Files.exists(postIdPath) || Files.isDirectory(postIdPath)) {
            throw new IllegalArgumentException("File not found: " + postIdPath);
        }

        logger.info("Creating new MetricEvaluationManager for sample " + sampleName + " ...");

        try (CSVParser csvParser = new CSVParser(new FileReader(postIdPath.toFile()), csvFormatPostIds.withFirstRecordAsHeader())) {

            logger.info("Reading PostIds from CSV file " + postIdPath.toFile().toString() + " ...");

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
                    String msg = "Version count expected to be " + versionCount + ", but was " + newPostVersionList.size();
                    logger.warning(msg);
                    throw new IllegalArgumentException(msg);
                }

                postVersionLists.put(postId, newPostVersionList);

                // read ground truth
                PostGroundTruth newPostGroundTruth = PostGroundTruth.readFromCSV(groundTruthPath, postId);

                if (newPostGroundTruth.getPossibleConnections() != newPostVersionList.getPossibleConnections()) {
                    String msg = "Number of possible connections in ground truth is different " + "from number of possible connections in post history.";
                    logger.warning(msg);
                    throw new IllegalArgumentException(msg);
                }

                postGroundTruths.put(postId, newPostGroundTruth);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (validate && ! validate()) {
            String msg = "Post ground truth files and post version history files do not match.";
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
        }

        initialized = true;

        return this;
    }

    public boolean validate() {
        for (MetricEvaluationPerSample sample : metricEvaluationsPerSample) {
            if (!sample.validate()) {
                return false;
            }
        }
        return true;
    }

    private void prepareEvaluation() {
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
        threadId = ++threadIdCounter;
        logger.info("Thread " + threadId + " started for sample " + sampleName + "...");

        if (!initialized) {
            initialize();
        }

        prepareEvaluation();

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

        logger.info("Saving results for sample " + sampleName + "...");
        writeToCSV();
        logger.info("Results saved.");
    }

    private void writeToCSV() {
        // create output directory if it does not exist
        try {
            Files.createDirectories(outputDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // output file by version
        File outputFilePerVersion = Paths.get(this.outputDirPath.toString(), sampleName + "_per_version.csv").toFile();
        if (outputFilePerVersion.exists()) {
            if (!outputFilePerVersion.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFilePerVersion);
            }
        }

        // output file aggregated by post
        File outputFilePerPost = Paths.get(this.outputDirPath.toString(), sampleName + "_per_post.csv").toFile();
        if (outputFilePerPost.exists()) {
            if (!outputFilePerPost.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFilePerPost);
            }
        }

        // output file aggregated by sample
        File outputFilePerSample = Paths.get(this.outputDirPath.toString(), sampleName + "_per_sample.csv").toFile();
        if (outputFilePerSample.exists()) {
            if (!outputFilePerSample.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFilePerSample);
            }
        }

        logger.info("Writing metric evaluation results per version to CSV file " + outputFilePerVersion.getName() + " ...");
        logger.info("Writing metric evaluation results per post to CSV file " + outputFilePerPost.getName() + " ...");
        logger.info("Writing metric evaluation results per sample to CSV file " + outputFilePerSample.getName() + " ...");
        try (CSVPrinter csvPrinterVersion = new CSVPrinter(new FileWriter(outputFilePerVersion), csvFormatMetricEvaluationPerVersion);
             CSVPrinter csvPrinterPost = new CSVPrinter(new FileWriter(outputFilePerPost), csvFormatMetricEvaluationPerPost);
             CSVPrinter csvPrinterSample = new CSVPrinter(new FileWriter(outputFilePerSample), csvFormatMetricEvaluationPerSample)) {

            // header is automatically written

            // write results per sample, per post, and per version
            for (MetricEvaluationPerSample evaluationPerSample : metricEvaluationsPerSample) {
                evaluationPerSample.writeToCSV(csvPrinterSample);
                for (MetricEvaluationPerPost evaluationPerPost : evaluationPerSample) {
                    evaluationPerPost.writeToCSV(csvPrinterPost, csvPrinterVersion);
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

    public void addSimilarityMetric(SimilarityMetric metric) {
        similarityMetrics.add(metric);
    }

    public MetricEvaluationPerPost getMetricEvaluation(int postId, String metricName, double threshold) {
        for (MetricEvaluationPerSample evaluationPerSample : metricEvaluationsPerSample) {
            if (evaluationPerSample.getSimilarityMetric().getName().equals(metricName)
                    && evaluationPerSample.getSimilarityMetric().getThreshold() == threshold) {
                for (MetricEvaluationPerPost evaluationPerPost : evaluationPerSample) {
                    if (evaluationPerPost.getPostId() == postId) {
                        return evaluationPerPost;
                    }
                }
            }
        }

        throw new IllegalStateException("Similarity metric " + metricName + " not found in evaluation samples.");
    }

    String getSampleName() {
        return sampleName;
    }

    static void aggregateAndWriteSampleResults(List<MetricEvaluationManager> managers, File outputFile) {
        // aggregate results over all samples
        Map<SimilarityMetric, MetricResult> aggregatedMetricResultsText = new HashMap<>();
        Map<SimilarityMetric, MetricResult> aggregatedMetricResultsCode = new HashMap<>();
        int maxFailuresText = 0;
        int maxFailuresCode = 0;

        for (int i=0; i<managers.size(); i++) {
            MetricEvaluationManager manager = managers.get(i);
            if (i==0) {
                for (MetricEvaluationPerSample evaluation : manager.metricEvaluationsPerSample) {
                    MetricResult resultText = evaluation.getResultAggregatedBySampleText();
                    maxFailuresText = resultText.getFailedPredecessorComparisons();
                    aggregatedMetricResultsText.put(evaluation.getSimilarityMetric(), resultText);

                    MetricResult resultCode = evaluation.getResultAggregatedBySampleCode();
                    maxFailuresCode = resultCode.getFailedPredecessorComparisons();
                    aggregatedMetricResultsCode.put(evaluation.getSimilarityMetric(), resultCode);
                }
            } else {
                for (MetricEvaluationPerSample evaluation : manager.metricEvaluationsPerSample) {
                    MetricResult newResultText = evaluation.getResultAggregatedBySampleText();
                    maxFailuresText = Math.max(maxFailuresText, newResultText.getFailedPredecessorComparisons());
                    MetricResult resultText = aggregatedMetricResultsText.get(newResultText.getSimilarityMetric());
                    resultText.add(newResultText);

                    MetricResult newResultCode = evaluation.getResultAggregatedBySampleCode();
                    maxFailuresCode = Math.max(maxFailuresCode, newResultCode.getFailedPredecessorComparisons());
                    MetricResult resultCode = aggregatedMetricResultsCode.get(newResultCode.getSimilarityMetric());
                    resultCode.add(newResultCode);
                }
            }
        }

        try (CSVPrinter csvPrinterAggregated = new CSVPrinter(new FileWriter(outputFile), csvFormatMetricEvaluationPerSample)) {
            for (SimilarityMetric similarityMetric : aggregatedMetricResultsText.keySet()) {
                MetricResult aggregatedResultText = aggregatedMetricResultsText.get(similarityMetric);
                MetricResult aggregatedResultCode = aggregatedMetricResultsCode.get(similarityMetric);

                // "MetricType", "Metric", "Threshold",
                // "YoudensJText", "RuntimeText", "YoudensJCode", "RuntimeCode",
                // "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",
                // "TextBlockVersionCount", "PossibleConnectionsText",
                // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText",
                // "PrecisionText", "RecallText", "SensitivityText", "SpecificityText", "FailureRateText",
                // "CodeBlockVersionCount", "PossibleConnectionsCode",
                // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode",
                // "PrecisionCode", "RecallCode", "SensitivityCode", "SpecificityCode", "FailureRateCode"
                csvPrinterAggregated.printRecord(
                        similarityMetric.getType(),
                        similarityMetric.getName(),
                        similarityMetric.getThreshold(),

                        aggregatedResultText.getYoudensJ(),
                        aggregatedResultText.getRuntime(),
                        aggregatedResultCode.getYoudensJ(),
                        aggregatedResultCode.getRuntime(),

                        aggregatedResultText.getPostCount(),
                        aggregatedResultText.getPostVersionCount(),
                        aggregatedResultText.getPostBlockVersionCount() + aggregatedResultCode.getPostBlockVersionCount(),
                        aggregatedResultText.getPossibleConnections() + aggregatedResultCode.getPossibleConnections(),

                        aggregatedResultText.getPostBlockVersionCount(),
                        aggregatedResultText.getPossibleConnections(),

                        aggregatedResultText.getTruePositives(),
                        aggregatedResultText.getTrueNegatives(),
                        aggregatedResultText.getFalsePositives(),
                        aggregatedResultText.getFalseNegatives(),
                        aggregatedResultText.getFailedPredecessorComparisons(),

                        aggregatedResultText.getPrecision(),
                        aggregatedResultText.getRecall(),
                        aggregatedResultText.getSensitivity(),
                        aggregatedResultText.getSpecificity(),
                        aggregatedResultText.getFailureRate(maxFailuresText),

                        aggregatedResultCode.getPostBlockVersionCount(),
                        aggregatedResultCode.getPossibleConnections(),

                        aggregatedResultCode.getTruePositives(),
                        aggregatedResultCode.getTrueNegatives(),
                        aggregatedResultCode.getFalsePositives(),
                        aggregatedResultCode.getFalseNegatives(),
                        aggregatedResultCode.getFailedPredecessorComparisons(),

                        aggregatedResultCode.getPrecision(),
                        aggregatedResultCode.getRecall(),
                        aggregatedResultCode.getSensitivity(),
                        aggregatedResultCode.getSpecificity(),
                        aggregatedResultCode.getFailureRate(maxFailuresCode)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Aggregated results over all samples saved.");
    }

    private void addDefaultSimilarityMetricsAndThresholds() {
        List<Double> similarityThresholds = Arrays.asList(0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9); // TODO: add also 0.35, 0.45, 0.55, 0.65, 0.75, 0.85

        for (double similarityThreshold : similarityThresholds) {

            // ****** Equality based *****

            similarityMetrics.add(new SimilarityMetric(
                    "equals",
                    de.unitrier.st.stringsimilarity.equal.Variants::equal,
                    SimilarityMetric.MetricType.EQUAL,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "equalsNormalized",
                    de.unitrier.st.stringsimilarity.equal.Variants::equalNormalized,
                    SimilarityMetric.MetricType.EQUAL,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "tokenEquals",
                    de.unitrier.st.stringsimilarity.equal.Variants::tokenEqual,
                    SimilarityMetric.MetricType.EQUAL,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "tokenEqualsNormalized",
                    de.unitrier.st.stringsimilarity.equal.Variants::tokenEqualNormalized,
                    SimilarityMetric.MetricType.EQUAL,
                    similarityThreshold)
            );


            // ****** Edit based *****

            similarityMetrics.add(new SimilarityMetric(
                    "levenshtein",
                    de.unitrier.st.stringsimilarity.edit.Variants::levenshtein,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "levenshteinNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::levenshteinNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "damerauLevenshtein",
                    de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshtein,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "damerauLevenshteinNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshteinNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "optimalAlignment",
                    de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignment,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "optimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignmentNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "longestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequence,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "longestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.EDIT,
                    similarityThreshold)
            );


            // ****** Fingerprint based *****

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramJaccard",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramDice",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOverlap",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramLongestCommonSubsequence",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );


            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramLongestCommonSubsequenceNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOptimalAlignment",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "winnowingTwoGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingThreeGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFourGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "winnowingFiveGramOptimalAlignmentNormalized",
                    de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized,
                    SimilarityMetric.MetricType.FINGERPRINT,
                    similarityThreshold)
            );


            // ****** Profile based *****

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineTokenNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTwoGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineThreeGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineFourGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineFiveGramNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedBool",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "cosineTwoShingleNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "cosineThreeShingleNormalizedNormalizedTermFrequency",
                    de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "manhattanTokenNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanTokenNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "manhattanTwoGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "manhattanThreeGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "manhattanFourGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "manhattanFiveGramNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanFiveGramNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "manhattanTwoShingleNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "manhattanThreeShingleNormalized",
                    de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized,
                    SimilarityMetric.MetricType.PROFILE,
                    similarityThreshold)
            );



            // ****** Set based *****

            similarityMetrics.add(new SimilarityMetric(
                    "tokenJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "tokenJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramJaccardNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoShingleJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeShingleJaccard",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccard,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoShingleJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeShingleJaccardNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccardNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "tokenDice",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "tokenDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramDice",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramDiceNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoShingleDice",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeShingleDice",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleDice,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoShingleDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeShingleDiceNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleDiceNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "tokenOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "tokenOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::tokenOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fourGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "fiveGramOverlapNormalizedPadding",
                    de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoShingleOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeShingleOverlap",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlap,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );

            similarityMetrics.add(new SimilarityMetric(
                    "twoShingleOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
            similarityMetrics.add(new SimilarityMetric(
                    "threeShingleOverlapNormalized",
                    de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlapNormalized,
                    SimilarityMetric.MetricType.SET,
                    similarityThreshold)
            );
        }
    }
}
