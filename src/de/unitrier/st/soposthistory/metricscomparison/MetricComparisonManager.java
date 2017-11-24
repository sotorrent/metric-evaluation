package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
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
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

// TODO: move to metrics comparison project
public class MetricComparisonManager implements Runnable {
    private static int threadIdCounter = 0;

    static Logger logger = null;
    static final CSVFormat csvFormatPostIds;
    public static final CSVFormat csvFormatMetricComparisonPost;
    public static final CSVFormat csvFormatMetricComparisonVersion;
    public static final CSVFormat csvFormatMetricComparisonAggregated;

    private static final Path DEFAULT_OUTPUT_DIR = Paths.get("output");

    private int threadId;
    private String name;
    private boolean addDefaultMetricsAndThresholds;
    private boolean randomizeOrder;
    private boolean validate;
    private int numberOfRepetitions;

    private Path postIdPath;
    private Path postHistoryPath;
    private Path groundTruthPath;
    private Path outputDirPath;

    private Set<Integer> postIds;
    private Map<Integer, List<Integer>> postHistoryIds;
    private Map<Integer, PostGroundTruth> postGroundTruth; // postId -> PostGroundTruth
    private Map<Integer, PostVersionList> postVersionLists; // postId -> PostVersionList

    private List<BiFunction<String, String, Double>> similarityMetrics;
    private List<String> similarityMetricsNames;
    private List<MetricComparison.MetricType> similarityMetricsTypes;
    private List<Double> similarityThresholds;

    private List<MetricComparison> metricComparisons;

    private boolean initialized;

    static {
        // configure logger
        try {
            logger = getClassLogger(MetricComparisonManager.class);
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
        csvFormatMetricComparisonPost = CSVFormat.DEFAULT
                .withHeader("Sample", "MetricType", "Metric", "Threshold", "PostId", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections", "RuntimeText", "TextBlockVersionCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "RuntimeCode", "CodeBlockVersionCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for metric comparison results (per version, i.e., per PostHistoryId)
        csvFormatMetricComparisonVersion = CSVFormat.DEFAULT
                .withHeader("Sample", "MetricType", "Metric", "Threshold", "PostId", "PostHistoryId", "PossibleConnections", "RuntimeText", "TextBlockCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "RuntimeCode", "CodeBlockCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");

        // configure CSV format for aggregated metric comparison results (per (metric, threshold) combination)
        csvFormatMetricComparisonAggregated = CSVFormat.DEFAULT
                .withHeader("MetricType", "Metric", "Threshold", "QualityText", "QualityCode", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections", "RuntimeText", "TextBlockVersionCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText", "PrecisionText", "RecallText", "RelativeFailedPredecessorComparisonsText", "RuntimeCode", "CodeBlockVersionCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode", "PrecisionCode", "RecallCode", "RelativeFailedPredecessorComparisonsCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");
    }

    private MetricComparisonManager(String name, Path postIdPath,
                                    Path postHistoryPath, Path groundTruthPath, Path outputDirPath,
                                    boolean validate, boolean addDefaultMetricsAndThresholds, boolean randomizeOrder,
                                    int numberOfRepetitions) {
        this.threadId = -1;
        this.name = name;

        this.postIdPath = postIdPath;
        this.postHistoryPath = postHistoryPath;
        this.groundTruthPath = groundTruthPath;
        this.outputDirPath = outputDirPath;

        this.validate = validate;
        this.addDefaultMetricsAndThresholds = addDefaultMetricsAndThresholds;
        this.randomizeOrder = randomizeOrder;
        this.numberOfRepetitions = numberOfRepetitions;

        this.postIds = new HashSet<>();
        this.postHistoryIds = new HashMap<>();
        this.postGroundTruth = new HashMap<>();
        this.postVersionLists = new HashMap<>();

        this.similarityMetrics = new LinkedList<>();
        this.similarityMetricsNames = new LinkedList<>();
        this.similarityMetricsTypes = new LinkedList<>();
        this.similarityThresholds = new LinkedList<>();

        this.metricComparisons = new LinkedList<>();

        this.initialized = false;
    }

    public static final MetricComparisonManager DEFAULT = new MetricComparisonManager(
            "MetricComparisonManager",
            null,
            null,
            null,
            DEFAULT_OUTPUT_DIR,
            true,
            true,
            true,
            4
    );

    public MetricComparisonManager withName(String name) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager withInputPaths(Path postIdPath, Path postHistoryPath, Path groundTruthPath) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager withOutputDirPath(Path outputDirPath) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager withValidate(boolean validate) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager withAddDefaultMetricsAndThresholds(boolean addDefaultMetricsAndThresholds) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager withRandomizeOrder(boolean randomizeOrder) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager withNumberOfRepetitions(int numberOfRepetitions) {
        return new MetricComparisonManager(name, postIdPath, postHistoryPath, groundTruthPath, outputDirPath,
                validate, addDefaultMetricsAndThresholds, randomizeOrder, numberOfRepetitions
        );
    }

    public MetricComparisonManager initialize() {
        if (addDefaultMetricsAndThresholds) {
            addDefaultSimilarityMetrics();
            addDefaultSimilarityThresholds();
        }

        // ensure that input file exists (directories are tested in read methods)
        if (!Files.exists(postIdPath) || Files.isDirectory(postIdPath)) {
            throw new IllegalArgumentException("File not found: " + postIdPath);
        }

        logger.info("Creating new MetricComparisonManager " + name + " ...");

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
                    throw new IllegalArgumentException("Version count expected to be " + versionCount
                            + ", but was " + newPostVersionList.size()
                    );
                }

                postVersionLists.put(postId, newPostVersionList);
                postHistoryIds.put(postId, newPostVersionList.getPostHistoryIds());

                // read ground truth
                PostGroundTruth newPostGroundTruth = PostGroundTruth.readFromCSV(groundTruthPath, postId);

                if (newPostGroundTruth.getPossibleConnections() != newPostVersionList.getPossibleConnections()) {
                    throw new IllegalArgumentException("Number of possible connections in ground truth is different " +
                            "from number of possible connections in post history.");
                }

                postGroundTruth.put(postId, newPostGroundTruth);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (validate && !validate()) {
            throw new IllegalArgumentException("Post ground truth files and post version history files do not match.");
        }

        initialized = true;

        return this;
    }

    public boolean validate() {
        if (postGroundTruth.size() != postVersionLists.size())
            return false;

        // check if GT and post version list contain the same posts with the same post blocks types in the same positions
        for (int postId : postVersionLists.keySet()) {
            PostGroundTruth gt = postGroundTruth.get(postId);

            if (gt == null) {
                return false;
            } else {
                PostVersionList list = postVersionLists.get(postId);

                Set<PostBlockConnection> connectionsList = list.getConnections();
                Set<PostBlockConnection> connectionsGT = gt.getConnections();

                if (!PostBlockConnection.matches(connectionsList, connectionsGT)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void prepareComparison() {
        for (int postId : postIds) {
            for (double similarityThreshold : similarityThresholds) {
                for (int i = 0; i < similarityMetrics.size(); i++) {
                    BiFunction<String, String, Double> similarityMetric = similarityMetrics.get(i);
                    String similarityMetricName = similarityMetricsNames.get(i);
                    MetricComparison.MetricType similarityMetricType = similarityMetricsTypes.get(i);
                    MetricComparison metricComparison = new MetricComparison(
                            postId,
                            postVersionLists.get(postId),
                            postGroundTruth.get(postId),
                            similarityMetric,
                            similarityMetricName,
                            similarityMetricType,
                            similarityThreshold,
                            numberOfRepetitions
                    );
                    metricComparisons.add(metricComparison);
                }
            }
        }
    }

    private void randomizeOrder() {
        Collections.shuffle(metricComparisons, new Random());
    }

    public void compareMetrics() {
        prepareComparison();

        for (int currentRepetition = 1; currentRepetition <= numberOfRepetitions; currentRepetition++) {
            if (randomizeOrder) {
                logger.info("Thread " + threadId + ": Randomizing order...");
                randomizeOrder();
            }

            logger.info("Thread " + threadId + ": Starting comparison run " + currentRepetition + "...");
            int size = metricComparisons.size();
            for (int i = 0; i < metricComparisons.size(); i++) {
                MetricComparison currentMetricComparison = metricComparisons.get(i);
                // Locale.ROOT -> force '.' as decimal separator
                String progress = String.format(Locale.ROOT, "%.2f%%", (((double)(i+1))/size*100));
                logger.info("Thread " + threadId + ": Current post: " + currentMetricComparison.getPostId() + " ("
                        + currentMetricComparison.getPostVersionList().size() + " versions)");
                logger.info("Thread " + threadId + ": MetricComparison " + (i+1) + " of " + size + " (" + progress + "), " +
                        "repetition " + currentRepetition + " of " + numberOfRepetitions);
                currentMetricComparison.start(currentRepetition);
            }
        }
    }

    public void writeToCSV() {
        // create output directory if it does not exist
        try {
            Files.createDirectories(outputDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // output file per version
        File outputFilePerVersion = Paths.get(this.outputDirPath.toString(), name + "_per_version.csv").toFile();
        if (outputFilePerVersion.exists()) {
            if (!outputFilePerVersion.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFilePerVersion);
            }
        }

        // output file per post
        File outputFilePerPost = Paths.get(this.outputDirPath.toString(), name + "_per_post.csv").toFile();
        if (outputFilePerPost.exists()) {
            if (!outputFilePerPost.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFilePerPost);
            }
        }

        // output file aggregated
        File outputFileAggregated = Paths.get(this.outputDirPath.toString(), name + "_aggregated.csv").toFile();
        if (outputFileAggregated.exists()) {
            if (!outputFileAggregated.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFileAggregated);
            }
        }

        logger.info("Thread " + threadId + ": Writing metric comparison results per version to CSV file " + outputFilePerVersion.getName() + " ...");
        logger.info("Thread " + threadId + ": Writing metric comparison results per post to CSV file " + outputFilePerPost.getName() + " ...");
        try (CSVPrinter csvPrinterVersion = new CSVPrinter(new FileWriter(outputFilePerVersion), csvFormatMetricComparisonVersion);
             CSVPrinter csvPrinterPost = new CSVPrinter(new FileWriter(outputFilePerPost), csvFormatMetricComparisonPost);
             CSVPrinter csvPrinterAggregated = new CSVPrinter(new FileWriter(outputFileAggregated), csvFormatMetricComparisonAggregated)) {

            // header is automatically written
            for (MetricComparison metricComparison : metricComparisons) {
                // write metric comparison results per postHistoryId and per postVersion
                int postId = metricComparison.getPostId();
                PostVersionList postVersionList = metricComparison.getPostVersionList();
                List<Integer> postHistoryIdsForPost = postHistoryIds.get(postId);

                MetricRuntime metricRuntimeText = metricComparison.getRuntimeText();
                MetricRuntime metricRuntimeCode = metricComparison.getRuntimeCode();
                MetricResult aggregatedResultText = metricComparison.getAggregatedResultsText();
                MetricResult aggregatedResultCode = metricComparison.getAggregatedResultsCode();

                if (aggregatedResultText.getPostBlockVersionCount() != postVersionList.getTextBlockVersionCount()
                        || aggregatedResultCode.getPostBlockVersionCount() != postVersionList.getCodeBlockVersionCount()) {
                    throw new IllegalStateException("Version count does not match.");
                }

                if (aggregatedResultText.getPossibleConnections() != postVersionList.getPossibleConnections(TextBlockVersion.getPostBlockTypeIdFilter())
                        || aggregatedResultCode.getPossibleConnections() != postVersionList.getPossibleConnections(CodeBlockVersion.getPostBlockTypeIdFilter())) {
                    throw new IllegalStateException("Possible connection count does not match.");
                }

                // write result per post
                //
                // "Sample", "MetricType", "Metric", "Threshold", "PostId",
                // "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",
                // "RuntimeText", "TextBlockVersionCount", "PossibleConnectionsText",
                // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText",
                // "RuntimeCode", "CodeBlockVersionCount", "PossibleConnectionsCode",
                // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode"
                csvPrinterPost.printRecord(
                        name,
                        metricComparison.getSimilarityMetricType(),
                        metricComparison.getSimilarityMetricName(),
                        metricComparison.getSimilarityThreshold(),
                        postId,
                        postVersionList.size(),
                        postVersionList.getPostBlockVersionCount(),
                        postVersionList.getPossibleConnections(),
                        metricRuntimeText.getTotalRuntime(),
                        aggregatedResultText.getPostBlockVersionCount(),
                        postVersionList.getPossibleConnections(TextBlockVersion.getPostBlockTypeIdFilter()),
                        aggregatedResultText.getTruePositives(),
                        aggregatedResultText.getTrueNegatives(),
                        aggregatedResultText.getFalsePositives(),
                        aggregatedResultText.getFalseNegatives(),
                        aggregatedResultText.getFailedPredecessorComparisons(),
                        metricRuntimeCode.getTotalRuntime(),
                        aggregatedResultCode.getPostBlockVersionCount(),
                        postVersionList.getPossibleConnections(CodeBlockVersion.getPostBlockTypeIdFilter()),
                        aggregatedResultCode.getTruePositives(),
                        aggregatedResultCode.getTrueNegatives(),
                        aggregatedResultCode.getFalsePositives(),
                        aggregatedResultCode.getFalseNegatives(),
                        aggregatedResultCode.getFailedPredecessorComparisons()
                );

                // write result per version
                for (int postHistoryId : postHistoryIdsForPost) {
                    MetricResult resultText = metricComparison.getResultText(postHistoryId);
                    MetricResult resultCode = metricComparison.getResultCode(postHistoryId);

                    // "Sample", "MetricType", "Metric", "Threshold", "PostId", "PostHistoryId", "PossibleConnections",
                    // "RuntimeText", "TextBlockCount", "PossibleConnectionsText",
                    // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText",
                    // "RuntimeCode", "CodeBlockCount", "PossibleConnectionsCode",
                    // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode"
                    csvPrinterVersion.printRecord(
                            name,
                            metricComparison.getSimilarityMetricType(),
                            metricComparison.getSimilarityMetricName(),
                            metricComparison.getSimilarityThreshold(),
                            postId,
                            postHistoryId,
                            metricComparison.getPostVersionList().getPostVersion(postHistoryId).getPossibleConnections(),
                            metricRuntimeText.getTotalRuntime(),
                            resultText.getPostBlockVersionCount(),
                            metricComparison.getPostVersionList().getPostVersion(postHistoryId).getPossibleConnections(TextBlockVersion.getPostBlockTypeIdFilter()),
                            resultText.getTruePositives(),
                            resultText.getTrueNegatives(),
                            resultText.getFalsePositives(),
                            resultText.getFalseNegatives(),
                            resultText.getFailedPredecessorComparisons(),
                            metricRuntimeCode.getTotalRuntime(),
                            resultCode.getPostBlockVersionCount(),
                            metricComparison.getPostVersionList().getPostVersion(postHistoryId).getPossibleConnections(CodeBlockVersion.getPostBlockTypeIdFilter()),
                            resultCode.getTruePositives(),
                            resultCode.getTrueNegatives(),
                            resultCode.getFalsePositives(),
                            resultCode.getFalseNegatives(),
                            resultCode.getFailedPredecessorComparisons()
                    );
                }
            }

            // write aggregated results
            AggregatedMetricComparisonList aggregatedMetricComparisons = AggregatedMetricComparisonList.fromMetricComparisons(metricComparisons);
            aggregatedMetricComparisons.writeToCSV(csvPrinterAggregated);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, PostGroundTruth> getPostGroundTruth() {
        return postGroundTruth;
    }

    public Map<Integer, PostVersionList> getPostVersionLists() {
        return postVersionLists;
    }

    public void addSimilarityThreshold(double threshold) {
        similarityThresholds.add(threshold);
    }

    private void addDefaultSimilarityThresholds() {
        similarityThresholds.addAll(Arrays.asList(0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)); // TODO: add also 0.35, 0.45, 0.55, 0.65, 0.75, 0.85
    }

    public void addSimilarityMetric(String name, MetricComparison.MetricType type, BiFunction<String, String, Double> metric) {
        similarityMetricsNames.add(name);
        similarityMetricsTypes.add(type);
        similarityMetrics.add(metric);
    }

    public MetricComparison getMetricComparison(int postId, String similarityMetricName, double similarityThreshold) {
        for (MetricComparison metricComparison : metricComparisons) {
            if (metricComparison.getPostId() == postId
                    && metricComparison.getSimilarityThreshold() == similarityThreshold
                    && metricComparison.getSimilarityMetricName().equals(similarityMetricName)) {
                return metricComparison;
            }
        }

        return null;
    }

    public List<MetricComparison> getMetricComparisons() {
        return metricComparisons;
    }

    @Override
    public void run() {
        synchronized (MetricComparisonManager.class) {
            threadId = ++threadIdCounter;
            logger.info("Thread " + threadId + " started...");
        }
        if (!initialized) {
            initialize();
        }
        compareMetrics();
        writeToCSV();
    }

    public String getName() {
        return name;
    }

    public static double calculateQualityMeasure(double precision, double recall, double relativeFailedPredecessorComparisons) {
        return (precision + recall + (1 - relativeFailedPredecessorComparisons)) / 3.0;
    }

    private void addDefaultSimilarityMetrics() {

        // ****** Edit based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::equals);
        similarityMetricsNames.add("equals");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::equalsNormalized);
        similarityMetricsNames.add("equalsNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::levenshtein);
        similarityMetricsNames.add("levenshtein");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::levenshteinNormalized);
        similarityMetricsNames.add("levenshteinNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshtein);
        similarityMetricsNames.add("damerauLevenshtein");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshteinNormalized);
        similarityMetricsNames.add("damerauLevenshteinNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignment);
        similarityMetricsNames.add("optimalAlignment");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignmentNormalized);
        similarityMetricsNames.add("optimalAlignmentNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequence);
        similarityMetricsNames.add("longestCommonSubsequence");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized);
        similarityMetricsNames.add("longestCommonSubsequenceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.EDIT);

        // ****** Fingerprint based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard);
        similarityMetricsNames.add("winnowingTwoGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard);
        similarityMetricsNames.add("winnowingThreeGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard);
        similarityMetricsNames.add("winnowingFourGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard);
        similarityMetricsNames.add("winnowingFiveGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized);
        similarityMetricsNames.add("winnowingTwoGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized);
        similarityMetricsNames.add("winnowingThreeGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized);
        similarityMetricsNames.add("winnowingFourGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized);
        similarityMetricsNames.add("winnowingFiveGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice);
        similarityMetricsNames.add("winnowingTwoGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice);
        similarityMetricsNames.add("winnowingThreeGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDice);
        similarityMetricsNames.add("winnowingFourGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice);
        similarityMetricsNames.add("winnowingFiveGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized);
        similarityMetricsNames.add("winnowingTwoGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized);
        similarityMetricsNames.add("winnowingThreeGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized);
        similarityMetricsNames.add("winnowingFourGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized);
        similarityMetricsNames.add("winnowingFiveGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap);
        similarityMetricsNames.add("winnowingTwoGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap);
        similarityMetricsNames.add("winnowingThreeGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap);
        similarityMetricsNames.add("winnowingFourGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap);
        similarityMetricsNames.add("winnowingFiveGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized);
        similarityMetricsNames.add("winnowingTwoGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized);
        similarityMetricsNames.add("winnowingThreeGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized);
        similarityMetricsNames.add("winnowingFourGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized);
        similarityMetricsNames.add("winnowingFiveGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingTwoGramLongestCommonSubsequence");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingThreeGramLongestCommonSubsequence");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingFourGramLongestCommonSubsequence");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingFiveGramLongestCommonSubsequence");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingTwoGramLongestCommonSubsequenceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingThreeGramLongestCommonSubsequenceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingFourGramLongestCommonSubsequenceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingFiveGramLongestCommonSubsequenceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment);
        similarityMetricsNames.add("winnowingTwoGramOptimalAlignment");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment);
        similarityMetricsNames.add("winnowingThreeGramOptimalAlignment");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment);
        similarityMetricsNames.add("winnowingFourGramOptimalAlignment");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment);
        similarityMetricsNames.add("winnowingFiveGramOptimalAlignment");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingTwoGramOptimalAlignmentNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingThreeGramOptimalAlignmentNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingFourGramOptimalAlignmentNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingFiveGramOptimalAlignmentNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.FINGERPRINT);

        // ****** Profile based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedBool);
        similarityMetricsNames.add("cosineTokenNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTokenNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTokenNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool);
        similarityMetricsNames.add("cosineTwoGramNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool);
        similarityMetricsNames.add("cosineThreeGramNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool);
        similarityMetricsNames.add("cosineFourGramNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool);
        similarityMetricsNames.add("cosineFiveGramNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoGramNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeGramNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFourGramNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFiveGramNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoGramNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeGramNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFourGramNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFiveGramNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool);
        similarityMetricsNames.add("cosineTwoShingleNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool);
        similarityMetricsNames.add("cosineThreeShingleNormalizedBool");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoShingleNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeShingleNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoShingleNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeShingleNormalizedNormalizedTermFrequency");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTokenNormalized);
        similarityMetricsNames.add("manhattanTokenNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoGramNormalized);
        similarityMetricsNames.add("manhattanTwoGramNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeGramNormalized);
        similarityMetricsNames.add("manhattanThreeGramNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized);
        similarityMetricsNames.add("manhattanFourGramNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanFiveGramNormalized);
        similarityMetricsNames.add("manhattanFiveGramNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized);
        similarityMetricsNames.add("manhattanTwoShingleNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized);
        similarityMetricsNames.add("manhattanThreeShingleNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.PROFILE);

        // ****** Set based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenEquals);
        similarityMetricsNames.add("tokenEquals");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenEqualsNormalized);
        similarityMetricsNames.add("tokenEqualsNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenJaccard);
        similarityMetricsNames.add("tokenJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenJaccardNormalized);
        similarityMetricsNames.add("tokenJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccard);
        similarityMetricsNames.add("twoGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccard);
        similarityMetricsNames.add("threeGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccard);
        similarityMetricsNames.add("fourGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccard);
        similarityMetricsNames.add("fiveGramJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalized);
        similarityMetricsNames.add("twoGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalized);
        similarityMetricsNames.add("threeGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalized);
        similarityMetricsNames.add("fourGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalized);
        similarityMetricsNames.add("fiveGramJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding);
        similarityMetricsNames.add("twoGramJaccardNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding);
        similarityMetricsNames.add("threeGramJaccardNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding);
        similarityMetricsNames.add("fourGramJaccardNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding);
        similarityMetricsNames.add("fiveGramJaccardNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccard);
        similarityMetricsNames.add("twoShingleJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccard);
        similarityMetricsNames.add("threeShingleJaccard");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccardNormalized);
        similarityMetricsNames.add("twoShingleJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccardNormalized);
        similarityMetricsNames.add("threeShingleJaccardNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenDice);
        similarityMetricsNames.add("tokenDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenDiceNormalized);
        similarityMetricsNames.add("tokenDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice);
        similarityMetricsNames.add("twoGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDice);
        similarityMetricsNames.add("threeGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDice);
        similarityMetricsNames.add("fourGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDice);
        similarityMetricsNames.add("fiveGramDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalized);
        similarityMetricsNames.add("twoGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalized);
        similarityMetricsNames.add("threeGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalized);
        similarityMetricsNames.add("fourGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalized);
        similarityMetricsNames.add("fiveGramDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding);
        similarityMetricsNames.add("twoGramDiceNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding);
        similarityMetricsNames.add("threeGramDiceNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding);
        similarityMetricsNames.add("fourGramDiceNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding);
        similarityMetricsNames.add("fiveGramDiceNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleDice);
        similarityMetricsNames.add("twoShingleDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleDice);
        similarityMetricsNames.add("threeShingleDice");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleDiceNormalized);
        similarityMetricsNames.add("twoShingleDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleDiceNormalized);
        similarityMetricsNames.add("threeShingleDiceNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenOverlap);
        similarityMetricsNames.add("tokenOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenOverlapNormalized);
        similarityMetricsNames.add("tokenOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlap);
        similarityMetricsNames.add("twoGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlap);
        similarityMetricsNames.add("threeGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap);
        similarityMetricsNames.add("fourGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlap);
        similarityMetricsNames.add("fiveGramOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalized);
        similarityMetricsNames.add("twoGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalized);
        similarityMetricsNames.add("threeGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalized);
        similarityMetricsNames.add("fourGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalized);
        similarityMetricsNames.add("fiveGramOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding);
        similarityMetricsNames.add("twoGramOverlapNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding);
        similarityMetricsNames.add("threeGramOverlapNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding);
        similarityMetricsNames.add("fourGramOverlapNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding);
        similarityMetricsNames.add("fiveGramOverlapNormalizedPadding");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlap);
        similarityMetricsNames.add("twoShingleOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlap);
        similarityMetricsNames.add("threeShingleOverlap");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlapNormalized);
        similarityMetricsNames.add("twoShingleOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlapNormalized);
        similarityMetricsNames.add("threeShingleOverlapNormalized");
        similarityMetricsTypes.add(MetricComparison.MetricType.SET);

        // much slower than other nGram based variants -> excluded after test run
//        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramSimilarityKondrak05);
//        similarityMetricsNames.add("twoGramSimilarityKondrak05");
//        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
//        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramSimilarityKondrak05);
//        similarityMetricsNames.add("threeGramSimilarityKondrak05");
//        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
//        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramSimilarityKondrak05);
//        similarityMetricsNames.add("fourGramSimilarityKondrak05");
//        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
//        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramSimilarityKondrak05);
//        similarityMetricsNames.add("fiveGramSimilarityKondrak05");
//        similarityMetricsTypes.add(MetricComparison.MetricType.SET);
    }
}
