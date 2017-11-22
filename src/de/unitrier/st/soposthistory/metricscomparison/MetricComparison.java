package de.unitrier.st.soposthistory.metricscomparison;

import com.google.common.base.Stopwatch;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.stringsimilarity.util.InputTooShortException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.logger;

// TODO: move to metrics comparison project
public class MetricComparison {
    public enum MetricType {EDIT, FINGERPRINT, PROFILE, SET}

    final private int postId;
    final private List<Integer> postHistoryIds;
    final private PostVersionList postVersionList;
    final private PostGroundTruth postGroundTruth;
    final private BiFunction<String, String, Double> similarityMetric;
    final private String similarityMetricName;
    final private MetricType similarityMetricType;
    final private double similarityThreshold;
    private int numberOfRepetitions;
    private int currentRepetition;
    private Stopwatch stopWatch;

    // the following variable is used to temporarily store the runtime
    private long runtimeTotal;

    // text
    private MetricRuntime metricRuntimeText;
    // PostHistoryId -> metric results for text blocks
    private Map<Integer, MetricResult> resultsText;

    // code
    private MetricRuntime metricRuntimeCode;
    // PostHistoryId -> metric results for code blocks
    private Map<Integer, MetricResult> resultsCode;

    public MetricComparison(int postId,
                            PostVersionList postVersionList,
                            PostGroundTruth postGroundTruth,
                            BiFunction<String, String, Double> similarityMetric,
                            String similarityMetricName,
                            MetricType similarityMetricType,
                            double similarityThreshold,
                            int numberOfRepetitions) {
        this.postId = postId;
        this.postVersionList = postVersionList;
        // normalize links so that post version list and ground truth are comparable
        postVersionList.normalizeLinks();
        this.postGroundTruth = postGroundTruth;
        this.postHistoryIds = postVersionList.getPostHistoryIds();

        if (!this.postGroundTruth.getPostHistoryIds().equals(this.postHistoryIds)) {
            throw new IllegalArgumentException("PostHistoryIds in postVersionList and postGroundTruth differ.");
        }

        this.similarityMetric = similarityMetric;
        this.similarityMetricName = similarityMetricName;
        this.similarityMetricType = similarityMetricType;
        this.similarityThreshold = similarityThreshold;

        this.runtimeTotal = 0;

        this.metricRuntimeText = new MetricRuntime();
        this.metricRuntimeCode = new MetricRuntime();
        this.resultsText = new HashMap<>();
        this.resultsCode = new HashMap<>();

        this.numberOfRepetitions = numberOfRepetitions;
        this.currentRepetition = 0;

        this.stopWatch = Stopwatch.createUnstarted();
    }

    private void reset() {
        this.runtimeTotal = 0;
        this.stopWatch.reset();
    }

    public void start(int currentRepetition) {
        Config config = Config.METRICS_COMPARISON
                .withTextSimilarityMetric(similarityMetric)
                .withTextSimilarityThreshold(similarityThreshold)
                .withCodeSimilarityMetric(similarityMetric)
                .withCodeSimilarityThreshold(similarityThreshold);

        // the post version list is shared by all metric comparisons conducted for the corresponding post
        synchronized (postVersionList) {
            this.currentRepetition++;

            if (this.currentRepetition != currentRepetition) {
                throw new IllegalStateException("Repetition count does not match (expected: " + currentRepetition
                        + "; actual: " + this.currentRepetition);
            }

            logger.info("Current metric: " + similarityMetricName + ", current threshold: " + similarityThreshold);

            // alternate the order in which the post history is processed and evaluated
            if (currentRepetition % 2 == 0) {
                evaluate(config, TextBlockVersion.getPostBlockTypeIdFilter());
                evaluate(config, CodeBlockVersion.getPostBlockTypeIdFilter());
            } else {
                evaluate(config, CodeBlockVersion.getPostBlockTypeIdFilter());
                evaluate(config, TextBlockVersion.getPostBlockTypeIdFilter());
            }
        }
    }

    private void evaluate(Config config, Set<Integer> postBlockTypeFilter) {

        // process version history and measure runtime
        stopWatch.start();
        try {
            postVersionList.processVersionHistory(config, postBlockTypeFilter);
        } finally {
            stopWatch.stop();
        }

        // save runtime value
        runtimeTotal = stopWatch.elapsed().getNano();

        // save results
        if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
            setResultAndRuntime(resultsText, metricRuntimeText, postBlockTypeFilter);
        }
        if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
            setResultAndRuntime(resultsCode, metricRuntimeCode, postBlockTypeFilter);
        }

        // reset flag inputTooShort, stopWatch, and runtime variables
        this.reset();
        // reset post block version history
        postVersionList.resetPostBlockVersionHistory();
    }

    private void setResultAndRuntime(Map<Integer, MetricResult> results, MetricRuntime metricRuntime,
                                     Set<Integer> postBlockTypeFilter) {
        if (currentRepetition == 1) {
            // set initial values after first run, return runtimeUser
            for (int postHistoryId : postHistoryIds) {
                MetricResult result = getResultAndSetRuntime(postHistoryId, metricRuntime, postBlockTypeFilter);
                results.put(postHistoryId, result);
            }
        } else {
            // compare result values in later runs
            for (int postHistoryId : postHistoryIds) {
                MetricResult resultInMap = results.get(postHistoryId);
                MetricResult newResult = getResultAndSetRuntime(postHistoryId, metricRuntime, postBlockTypeFilter);

                boolean truePositivesEqual = resultInMap.getTruePositives() == newResult.getTruePositives();
                boolean falsePositivesEqual = resultInMap.getFalsePositives() == newResult.getFalsePositives();
                boolean trueNegativesEqual = resultInMap.getTrueNegatives() == newResult.getTrueNegatives();
                boolean falseNegativesEqual = resultInMap.getFalseNegatives() == newResult.getFalseNegatives();
                boolean postBlockVersionCountEqual = resultInMap.getPostBlockVersionCount() == newResult.getPostBlockVersionCount();
                boolean failedPredecessorComparisonsEqual = resultInMap.getFailedPredecessorComparisons() == newResult.getFailedPredecessorComparisons();

                if (!truePositivesEqual || !falsePositivesEqual || !trueNegativesEqual || !falseNegativesEqual
                        || !postBlockVersionCountEqual || !failedPredecessorComparisonsEqual) {
                    throw new IllegalStateException("Metric results changed from repetition "
                            + (currentRepetition - 1) + " to " + currentRepetition);
                }
            }
        }
    }

    private MetricResult getResultAndSetRuntime(int postHistoryId, MetricRuntime metricRuntime, Set<Integer> postBlockTypeFilter) {
        MetricResult result = new MetricResult();

        // metric runtime
        if (currentRepetition == 1) {
            metricRuntime.setRuntimeTotal(runtimeTotal);
        } else if (currentRepetition < numberOfRepetitions) {
            // add up runtime values
            metricRuntime.setRuntimeTotal(metricRuntime.getRuntime() + runtimeTotal);
        } else {
            // calculate mean in last run
            metricRuntime.setRuntimeTotal(Math.round((double)(metricRuntime.getRuntime() + runtimeTotal) / numberOfRepetitions));
        }

        // post block count
        result.setPostBlockVersionCount(0);
        if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId))
            result.setPostBlockVersionCount(postVersionList.getPostVersion(postHistoryId).getTextBlocks().size());
        if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId))
            result.setPostBlockVersionCount(postVersionList.getPostVersion(postHistoryId).getCodeBlocks().size());

        // results
        int possibleConnections = postGroundTruth.getPossibleConnections(postHistoryId, postBlockTypeFilter);
        Set<PostBlockConnection> postBlockConnections = postVersionList.getPostVersion(postHistoryId).getConnections(postBlockTypeFilter);
        Set<PostBlockConnection> postBlockConnectionsGT = postGroundTruth.getConnections(postHistoryId, postBlockTypeFilter);

        int truePositivesCount = PostBlockConnection.intersection(postBlockConnectionsGT, postBlockConnections).size();
        int falsePositivesCount = PostBlockConnection.difference(postBlockConnections, postBlockConnectionsGT).size();

        int trueNegativesCount = possibleConnections - (PostBlockConnection.union(postBlockConnectionsGT, postBlockConnections).size());
        int falseNegativesCount = PostBlockConnection.difference(postBlockConnectionsGT, postBlockConnections).size();

        int failedPredecessorComparisons = postVersionList.getFailedPredecessorComparisons();

        int allConnectionsCount = truePositivesCount + falsePositivesCount + trueNegativesCount + falseNegativesCount;
        if (possibleConnections != allConnectionsCount) {
            throw new IllegalStateException("Invalid result (expected: " + possibleConnections
                    + "; actual: " + allConnectionsCount + ")");
        }

        result.setTruePositives(truePositivesCount);
        result.setFalsePositives(falsePositivesCount);
        result.setTrueNegatives(trueNegativesCount);
        result.setFalseNegatives(falseNegativesCount);
        result.setFailedPredecessorComparisons(failedPredecessorComparisons);

        return result;
    }

    public BiFunction<String, String, Double> getSimilarityMetric() {
        return similarityMetric;
    }

    public String getSimilarityMetricName() {
        return similarityMetricName;
    }

    public MetricType getSimilarityMetricType() {
        return similarityMetricType;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getPostId() {
        return postId;
    }

    public PostVersionList getPostVersionList() {
        return postVersionList;
    }

    public MetricRuntime getMetricRuntimeText() {
        return metricRuntimeText;
    }

    public MetricRuntime getMetricRuntimeCode() {
        return metricRuntimeCode;
    }

    public MetricResult getResultText(int postHistoryId) {
        return resultsText.get(postHistoryId);
    }

    public MetricResult getResultCode(int postHistoryId) {
        return resultsCode.get(postHistoryId);
    }

    public MetricResult getAggregatedResultText() {
        MetricResult result = new MetricResult();
        for (MetricResult currentResult : resultsText.values()) {
            result.add(currentResult);
        }
        return result;
    }

    public MetricResult getAggregatedResultCode() {
        MetricResult result = new MetricResult();
        for (MetricResult currentResult : resultsCode.values()) {
            result.add(currentResult);
        }
        return result;
    }

}
