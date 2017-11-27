package de.unitrier.st.soposthistory.metricscomparison;

import com.google.common.base.Stopwatch;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

/**
 * Evaluation of one similarity metric using one SO post.
 */
public class MetricEvaluationPerPost {
    private static Logger logger;

    static {
        // configure logger
        try {
            logger = getClassLogger(MetricEvaluationPerPost.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final SimilarityMetric similarityMetric;

    final private int postId;
    final private List<Integer> postHistoryIds;
    final private PostVersionList postVersionList;
    final private PostGroundTruth postGroundTruth;

    private int numberOfRepetitions;
    private int currentRepetition;
    private Stopwatch stopWatch;

    // the following variable is used to temporarily store the runtime
    private long runtime;

    // text
    // PostHistoryId -> metric results for text blocks
    private Map<Integer, MetricResult> resultsText;
    private MetricResult aggregatedResultText;

    // code
    // PostHistoryId -> metric results for code blocks
    private Map<Integer, MetricResult> resultsCode;
    private MetricResult aggregatedResultCode;


    MetricEvaluationPerPost(SimilarityMetric similarityMetric,
                            int postId,
                            PostVersionList postVersionList,
                            PostGroundTruth postGroundTruth,
                            int numberOfRepetitions) {

        this.similarityMetric = similarityMetric;

        this.postId = postId;
        this.postVersionList = postVersionList;
        postVersionList.normalizeLinks(); // normalize links so that post version list and ground truth are comparable
        this.postGroundTruth = postGroundTruth;
        this.postHistoryIds = postVersionList.getPostHistoryIds();

        if (!this.postGroundTruth.getPostHistoryIds().equals(this.postHistoryIds)) {
            throw new IllegalArgumentException("PostHistoryIds in postVersionList and postGroundTruth differ.");
        }

        this.runtime = 0;

        this.resultsText = new HashMap<>();
        this.resultsCode = new HashMap<>();

        this.numberOfRepetitions = numberOfRepetitions;
        this.currentRepetition = 0;

        this.stopWatch = Stopwatch.createUnstarted();
    }

    private void reset() {
        this.runtime = 0;
        this.stopWatch.reset();
    }

    void startEvaluation(int currentRepetition) {
        Config config = Config.METRICS_COMPARISON
                .withTextSimilarityMetric(similarityMetric.getMetric())
                .withTextSimilarityThreshold(similarityMetric.getThreshold())
                .withCodeSimilarityMetric(similarityMetric.getMetric())
                .withCodeSimilarityThreshold(similarityMetric.getThreshold());

        // the post version list is shared by all metric evaluations conducted for the corresponding post
        synchronized (postVersionList) {
            this.currentRepetition++;

            if (this.currentRepetition != currentRepetition) {
                throw new IllegalStateException("Repetition count does not match (expected: " + currentRepetition
                        + "; actual: " + this.currentRepetition);
            }

            //logger.info("Evaluating metric " + similarityMetric + " on post " + postId);

            // alternate the order in which the post history is processed and evaluated
            if (currentRepetition % 2 == 0) {
                evaluatePostBlockVersions(config, TextBlockVersion.getPostBlockTypeIdFilter());
                evaluatePostBlockVersions(config, CodeBlockVersion.getPostBlockTypeIdFilter());
            } else {
                evaluatePostBlockVersions(config, CodeBlockVersion.getPostBlockTypeIdFilter());
                evaluatePostBlockVersions(config, TextBlockVersion.getPostBlockTypeIdFilter());
            }
        }
    }

    private void evaluatePostBlockVersions(Config config, Set<Integer> postBlockTypeFilter) {
        // process version history and measure runtime
        stopWatch.start();
        try {
            postVersionList.processVersionHistory(config, postBlockTypeFilter);
        } finally {
            stopWatch.stop();
        }

        // save runtime value
        runtime = stopWatch.elapsed().getNano();

        // save and validate results
        if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
            setResultAndRuntime(resultsText, postBlockTypeFilter);
            validateResultsText();
        }
        if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
            setResultAndRuntime(resultsCode, postBlockTypeFilter);
            validateResultsCode();
        }

        // reset flag inputTooShort, stopWatch, and runtime variables
        this.reset();
        // reset post block version history
        postVersionList.resetPostBlockVersionHistory();
    }

    private void validateResultsText() {
        int textBlockVersionCount = 0;
        int textPossibleConnections = 0;

        for (int postHistoryId : postHistoryIds) {
            textBlockVersionCount += resultsText.get(postHistoryId).getPostBlockVersionCount();
            textPossibleConnections += resultsText.get(postHistoryId).getPossibleConnections();
        }

        if (textBlockVersionCount != postVersionList.getTextBlockVersionCount()) {
            throw new IllegalStateException("TextBlockVersionCount does not match.");
        }

        if (textPossibleConnections != postVersionList.getPossibleConnections(TextBlockVersion.getPostBlockTypeIdFilter())) {
            throw new IllegalStateException("PossibleConnections for text blocks do not match.");
        }
    }

    private void validateResultsCode() {
        int codeBlockVersionCount = 0;
        int codePossibleConnections = 0;

        for (int postHistoryId : postHistoryIds) {
            codeBlockVersionCount += resultsCode.get(postHistoryId).getPostBlockVersionCount();
            codePossibleConnections += resultsCode.get(postHistoryId).getPossibleConnections();
        }

        if (codeBlockVersionCount != postVersionList.getCodeBlockVersionCount()) {
            throw new IllegalStateException("CodeBlockVersionCount does not match.");
        }

        if (codePossibleConnections != postVersionList.getPossibleConnections(CodeBlockVersion.getPostBlockTypeIdFilter())) {
            throw new IllegalStateException("PossibleConnections for code blocks do not match.");
        }
    }

    private void setResultAndRuntime(Map<Integer, MetricResult> results, Set<Integer> postBlockTypeFilter) {
        if (currentRepetition == 1) {
            // set initial values after first run, return runtimeUser
            for (int postHistoryId : postHistoryIds) {
                MetricResult result = getResultAndSetRuntime(postHistoryId, new MetricResult(similarityMetric), postBlockTypeFilter);
                results.put(postHistoryId, result);
            }
        } else {
            // compare result values in later runs
            for (int postHistoryId : postHistoryIds) {
                MetricResult resultInMap = results.get(postHistoryId);
                MetricResult newResult = getResultAndSetRuntime(postHistoryId, resultInMap, postBlockTypeFilter);

                boolean postBlockVersionCountEqual = resultInMap.getPostBlockVersionCount() == newResult.getPostBlockVersionCount();
                boolean possibleConnectionsEqual = resultInMap.getPossibleConnections() == newResult.getPossibleConnections();
                boolean truePositivesEqual = resultInMap.getTruePositives() == newResult.getTruePositives();
                boolean falsePositivesEqual = resultInMap.getFalsePositives() == newResult.getFalsePositives();
                boolean trueNegativesEqual = resultInMap.getTrueNegatives() == newResult.getTrueNegatives();
                boolean falseNegativesEqual = resultInMap.getFalseNegatives() == newResult.getFalseNegatives();
                boolean failedPredecessorComparisonsEqual = resultInMap.getFailedPredecessorComparisons() == newResult.getFailedPredecessorComparisons();

                if (!postBlockVersionCountEqual || ! possibleConnectionsEqual
                        || !truePositivesEqual || !falsePositivesEqual || !trueNegativesEqual || !falseNegativesEqual
                        || !failedPredecessorComparisonsEqual) {
                    throw new IllegalStateException("Metric results changed from repetition "
                            + (currentRepetition - 1) + " to " + currentRepetition);
                }
            }
        }
    }

    private MetricResult getResultAndSetRuntime(int postHistoryId, MetricResult oldResult, Set<Integer> postBlockTypeFilter) {
        MetricResult newResult = new MetricResult(similarityMetric);

        // runtime
        if (currentRepetition < numberOfRepetitions) {
            // sum up runtime of all repetitions...
            newResult.setRuntime(runtime + oldResult.getRuntime()); // oldResult.getRuntime() is 0 in first repetition
        } else {
            // ... and calculate arithmetic mean of runtime in last repetition
            newResult.setRuntime(Math.round(((double) runtime + oldResult.getRuntime()) / numberOfRepetitions));
        }

        // post count and post version count are always one for non-aggregated results
        newResult.setPostCount(1);
        newResult.setPostVersionCount(1);

        // post block count
        newResult.setPostBlockVersionCount(
                postVersionList.getPostVersion(postHistoryId).getPostBlocks(postBlockTypeFilter).size()
        );

        // possible connections
        newResult.setPossibleConnections(
                postVersionList.getPostVersion(postHistoryId).getPossibleConnections(postBlockTypeFilter)
        );

        // results
        int possibleConnectionsGT = postGroundTruth.getPossibleConnections(postHistoryId, postBlockTypeFilter);
        if (possibleConnectionsGT != newResult.getPossibleConnections()) {
            throw new IllegalStateException("Invalid result (expected: " + possibleConnectionsGT
                    + "; actual: " + newResult.getPossibleConnections() + ")");
        }
        Set<PostBlockConnection> postBlockConnections = postVersionList.getPostVersion(postHistoryId).getConnections(postBlockTypeFilter);
        Set<PostBlockConnection> postBlockConnectionsGT = postGroundTruth.getConnections(postHistoryId, postBlockTypeFilter);

        int truePositivesCount = PostBlockConnection.intersection(postBlockConnectionsGT, postBlockConnections).size();
        int falsePositivesCount = PostBlockConnection.difference(postBlockConnections, postBlockConnectionsGT).size();

        int trueNegativesCount = possibleConnectionsGT - (PostBlockConnection.union(postBlockConnectionsGT, postBlockConnections).size());
        int falseNegativesCount = PostBlockConnection.difference(postBlockConnectionsGT, postBlockConnections).size();

        int failedPredecessorComparisons = postVersionList.getFailedPredecessorComparisons(postBlockTypeFilter);

        int allConnectionsCount = truePositivesCount + falsePositivesCount + trueNegativesCount + falseNegativesCount;
        if (possibleConnectionsGT != allConnectionsCount) {
            throw new IllegalStateException("Invalid result (expected: " + possibleConnectionsGT
                    + "; actual: " + allConnectionsCount + ")");
        }

        newResult.setTruePositives(truePositivesCount);
        newResult.setFalsePositives(falsePositivesCount);
        newResult.setTrueNegatives(trueNegativesCount);
        newResult.setFalseNegatives(falseNegativesCount);
        newResult.setFailedPredecessorComparisons(failedPredecessorComparisons);

        return newResult;
    }

    void writeToCSV(CSVPrinter csvPrinterPost, CSVPrinter csvPrinterVersion) throws IOException {

        // write result per post
        MetricResult aggregatedResultText = getResultAggregatedByPostText();
        MetricResult aggregatedResultCode = getResultAggregatedByPostCode();

        if (aggregatedResultText.getPostVersionCount() != aggregatedResultCode.getPostVersionCount()) {
            throw new IllegalStateException("Post version count of aggregated results does not match.");
        }

        // "MetricType", "Metric", "Threshold", "PostId",
        // "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",
        // "RuntimeText", "TextBlockVersionCount", "PossibleConnectionsText",
        // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText",
        // "RuntimeCode", "CodeBlockVersionCount", "PossibleConnectionsCode",
        // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode"
        csvPrinterPost.printRecord(
                similarityMetric.getType(),
                similarityMetric.getName(),
                similarityMetric.getThreshold(),
                postId,
                postVersionList.size(),
                aggregatedResultText.getPostBlockVersionCount() + aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleConnections() + aggregatedResultCode.getPossibleConnections(),
                aggregatedResultText.getRuntime(),
                aggregatedResultText.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleConnections(),
                aggregatedResultText.getTruePositives(),
                aggregatedResultText.getTrueNegatives(),
                aggregatedResultText.getFalsePositives(),
                aggregatedResultText.getFalseNegatives(),
                aggregatedResultText.getFailedPredecessorComparisons(),
                aggregatedResultCode.getRuntime(),
                aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultCode.getPossibleConnections(),
                aggregatedResultCode.getTruePositives(),
                aggregatedResultCode.getTrueNegatives(),
                aggregatedResultCode.getFalsePositives(),
                aggregatedResultCode.getFalseNegatives(),
                aggregatedResultCode.getFailedPredecessorComparisons()
        );

        // write result per version
        for (int postHistoryId : postHistoryIds) {
            MetricResult resultText = resultsText.get(postHistoryId);
            MetricResult resultCode = resultsCode.get(postHistoryId);

            // "Sample", "MetricType", "Metric", "Threshold", "PostId", "PostHistoryId", "PossibleConnections",
            // "RuntimeText", "TextBlockCount", "PossibleConnectionsText",
            // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText",
            // "RuntimeCode", "CodeBlockCount", "PossibleConnectionsCode",
            // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode"
            csvPrinterVersion.printRecord(
                    similarityMetric.getType(),
                    similarityMetric.getName(),
                    similarityMetric.getThreshold(),
                    postId,
                    postHistoryId,
                    resultText.getPossibleConnections() + resultCode.getPossibleConnections(),
                    resultText.getRuntime(),
                    resultText.getPostBlockVersionCount(),
                    resultText.getPossibleConnections(),
                    resultText.getTruePositives(),
                    resultText.getTrueNegatives(),
                    resultText.getFalsePositives(),
                    resultText.getFalseNegatives(),
                    resultText.getFailedPredecessorComparisons(),
                    resultCode.getRuntime(),
                    resultCode.getPostBlockVersionCount(),
                    resultCode.getPossibleConnections(),
                    resultCode.getTruePositives(),
                    resultCode.getTrueNegatives(),
                    resultCode.getFalsePositives(),
                    resultCode.getFalseNegatives(),
                    resultCode.getFailedPredecessorComparisons()
            );
        }
    }

    private MetricResult aggregateResultsPerPost(Collection<MetricResult> results) {
        MetricResult aggregatedResult = new MetricResult(similarityMetric);
        for (MetricResult currentResult : results) {
            aggregatedResult.add(currentResult);
        }
        aggregatedResult.setPostCount(1);
        return aggregatedResult;
    }

    MetricResult getResultAggregatedByPostText() {
        // aggregate by post
        if (aggregatedResultText == null) {
            aggregatedResultText = aggregateResultsPerPost(resultsText.values());
        }
        return aggregatedResultText;
    }

    MetricResult getResultAggregatedByPostCode() {
        // aggregate by post
        if (aggregatedResultCode == null) {
            aggregatedResultCode = aggregateResultsPerPost(resultsCode.values());
        }
        return aggregatedResultCode;
    }

    int getPostId() {
        return postId;
    }

    public MetricResult getResultsText(int postHistoryId) {
        return resultsText.get(postHistoryId);
    }

    public MetricResult getResultsCode(int postHistoryId) {
        return resultsCode.get(postHistoryId);
    }
}
