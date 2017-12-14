package de.unitrier.st.soposthistory.metricscomparison.evaluation;

import com.google.common.base.Stopwatch;
import de.unitrier.st.soposthistory.Config;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Evaluation of one similarity metric using one SO post.
 */
public class MetricEvaluationPerPost {
    private static Logger logger;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(MetricEvaluationPerPost.class);
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
            String msg = "PostHistoryIds in postVersionList and postGroundTruth differ.";
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
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
                String msg = "Repetition count does not match (expected: " + currentRepetition + "; actual: " + this.currentRepetition;
                logger.warning(msg);
                throw new IllegalArgumentException(msg);
            }

            //logger.info("Evaluating metric " + similarityMetric + " on post " + postId);

            // alternate the order in which the post history is processed and evaluated
            evaluatePostBlockVersions(config);
        }
    }

    private void evaluatePostBlockVersions(Config config) {
        // process version history and measure runtime
        stopWatch.start();
        try {
            postVersionList.processVersionHistory(config);
        } finally {
            stopWatch.stop();
        }

        // save runtime value
        runtime = stopWatch.elapsed().getNano();

        // save and validate results (text)
        setResultAndRuntime(resultsText, TextBlockVersion.getPostBlockTypeIdFilter());
        validateResultsText();

        // save and validate results (code)
        setResultAndRuntime(resultsCode, CodeBlockVersion.getPostBlockTypeIdFilter());
        validateResultsCode();

        // reset flag inputTooShort, stopWatch, and runtime variables
        this.reset();
        // reset post block version history
        postVersionList.resetPostBlockVersionHistory();
    }

    private void validateResultsText() {
        int textBlockVersionCount = 0;
        int textPossibleComparisons = 0;

        for (int postHistoryId : postHistoryIds) {
            textBlockVersionCount += resultsText.get(postHistoryId).getPostBlockVersionCount();
            textPossibleComparisons += resultsText.get(postHistoryId).getPossibleComparisons();
        }

        if (textBlockVersionCount != postVersionList.getTextBlockVersionCount()) {
            String msg = "TextBlockVersionCount does not match.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (textPossibleComparisons != postVersionList.getPossibleComparisons(TextBlockVersion.getPostBlockTypeIdFilter())) {
            String msg = "PossibleComparisons for text blocks do not match.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
    }

    private void validateResultsCode() {
        int codeBlockVersionCount = 0;
        int codePossibleComparisons = 0;

        for (int postHistoryId : postHistoryIds) {
            codeBlockVersionCount += resultsCode.get(postHistoryId).getPostBlockVersionCount();
            codePossibleComparisons += resultsCode.get(postHistoryId).getPossibleComparisons();
        }

        if (codeBlockVersionCount != postVersionList.getCodeBlockVersionCount()) {
            String msg = "CodeBlockVersionCount does not match.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (codePossibleComparisons != postVersionList.getPossibleComparisons(CodeBlockVersion.getPostBlockTypeIdFilter())) {
            String msg = "PossibleComparisons for code blocks do not match.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
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
                boolean possibleComparisonsEqual = resultInMap.getPossibleComparisons() == newResult.getPossibleComparisons();
                boolean truePositivesEqual = resultInMap.getTruePositives() == newResult.getTruePositives();
                boolean falsePositivesEqual = resultInMap.getFalsePositives() == newResult.getFalsePositives();
                boolean trueNegativesEqual = resultInMap.getTrueNegatives() == newResult.getTrueNegatives();
                boolean falseNegativesEqual = resultInMap.getFalseNegatives() == newResult.getFalseNegatives();
                boolean failedPredecessorComparisonsEqual = resultInMap.getFailedPredecessorComparisons() == newResult.getFailedPredecessorComparisons();

                if (!postBlockVersionCountEqual || ! possibleComparisonsEqual
                        || !truePositivesEqual || !falsePositivesEqual || !trueNegativesEqual || !falseNegativesEqual
                        || !failedPredecessorComparisonsEqual) {
                    String msg = "Metric results changed from repetition " + (currentRepetition - 1) + " to " + currentRepetition;
                    logger.warning(msg);
                    throw new IllegalStateException(msg);
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

        // possible comparisons
        newResult.setPossibleComparisons(
                postVersionList.getPostVersion(postHistoryId).getPossibleComparisons(postBlockTypeFilter)
        );

        // results
        int failedPredecessorComparisons = postVersionList.getPostVersion(postHistoryId).getFailedPredecessorComparisons(postBlockTypeFilter);
        int possibleComparisonsGT = postGroundTruth.getPossibleComparisons(postHistoryId, postBlockTypeFilter);
        if (possibleComparisonsGT != newResult.getPossibleComparisons()) {
            String msg = "Invalid result (expected: " + possibleComparisonsGT + "; actual: " + newResult.getPossibleComparisons() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
        Set<PostBlockConnection> postBlockConnections = postVersionList.getPostVersion(postHistoryId).getConnections(postBlockTypeFilter);
        Set<PostBlockConnection> postBlockConnectionsGT = postGroundTruth.getConnections(postHistoryId, postBlockTypeFilter);

        int truePositivesCount = PostBlockConnection.getTruePositives(postBlockConnections, postBlockConnectionsGT).size();
        int falsePositivesCount = PostBlockConnection.getFalsePositives(postBlockConnections, postBlockConnectionsGT).size();

        int trueNegativesCount = PostBlockConnection.getTrueNegatives(postBlockConnections, postBlockConnectionsGT, possibleComparisonsGT);
        int falseNegativesCount = PostBlockConnection.getFalseNegatives(postBlockConnections, postBlockConnectionsGT).size();

        int allConnectionsCount = truePositivesCount + falsePositivesCount + trueNegativesCount + falseNegativesCount;
        if (possibleComparisonsGT != allConnectionsCount) {
            String msg = "Invalid result (expected: " + possibleComparisonsGT + "; actual: " + allConnectionsCount + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
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

        // validate results
        MetricResult.validate(aggregatedResultText, aggregatedResultCode);

        // "MetricType", "Metric", "Threshold", "PostId", "Runtime"
        // "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons",
        // "TextBlockVersionCount", "PossibleComparisonsText",
        // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText",
        // "CodeBlockVersionCount", "PossibleComparisonsCode",
        // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode"
        csvPrinterPost.printRecord(
                similarityMetric.getType(),
                similarityMetric.getName(),
                similarityMetric.getThreshold(),
                postId,
                aggregatedResultText.getRuntime(),
                postVersionList.size(),
                aggregatedResultText.getPostBlockVersionCount() + aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleComparisons() + aggregatedResultCode.getPossibleComparisons(),
                aggregatedResultText.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleComparisons(),
                aggregatedResultText.getTruePositives(),
                aggregatedResultText.getTrueNegatives(),
                aggregatedResultText.getFalsePositives(),
                aggregatedResultText.getFalseNegatives(),
                aggregatedResultText.getFailedPredecessorComparisons(),
                aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultCode.getPossibleComparisons(),
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

            // validate results
            MetricResult.validate(resultText, resultCode);

            // "Sample", "MetricType", "Metric", "Threshold", "PostId", "PostHistoryId", "Runtime", "PossibleComparisons",
            // "TextBlockCount", "PossibleComparisonsText",
            // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailedPredecessorComparisonsText",
            // "CodeBlockCount", "PossibleComparisonsCode",
            // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailedPredecessorComparisonsCode"
            csvPrinterVersion.printRecord(
                    similarityMetric.getType(),
                    similarityMetric.getName(),
                    similarityMetric.getThreshold(),
                    postId,
                    postHistoryId,
                    resultText.getRuntime(),
                    resultText.getPossibleComparisons() + resultCode.getPossibleComparisons(),
                    resultText.getPostBlockVersionCount(),
                    resultText.getPossibleComparisons(),
                    resultText.getTruePositives(),
                    resultText.getTrueNegatives(),
                    resultText.getFalsePositives(),
                    resultText.getFalseNegatives(),
                    resultText.getFailedPredecessorComparisons(),
                    resultCode.getPostBlockVersionCount(),
                    resultCode.getPossibleComparisons(),
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

    public List<Integer> getPostHistoryIds() {
        return postHistoryIds;
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
