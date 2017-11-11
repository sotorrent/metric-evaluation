package de.unitrier.st.soposthistory.metricscomparison;

import com.google.common.base.Stopwatch;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.stringsimilarity.util.InputTooShortException;

import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.logger;

// TODO: move to metrics comparison project
public class MetricComparison {
    final private int postId;
    final private List<Integer> postHistoryIds;
    final private PostVersionList postVersionList;
    final private PostGroundTruth postGroundTruth;
    final private BiFunction<String, String, Double> similarityMetric;
    final private String similarityMetricName;
    final private double similarityThreshold;
    private boolean inputTooShort;
    private int numberOfRepetitions;
    private int currentRepetition;
    private ThreadMXBean threadMXBean;
    private Stopwatch stopWatch;
    private long runtimeUser;
    private long runtimeTotal;

    // text
    private Runtime runtimeText;
    // PostHistoryId -> metric results for text blocks
    private Map<Integer, MetricResult> resultsText;

    // code
    private Runtime runtimeCode;
    // PostHistoryId -> metric results for code blocks
    private Map<Integer, MetricResult> resultsCode;

    public MetricComparison(int postId,
                            PostVersionList postVersionList,
                            PostGroundTruth postGroundTruth,
                            BiFunction<String, String, Double> similarityMetric,
                            String similarityMetricName,
                            double similarityThreshold,
                            int numberOfRepetitions,
                            ThreadMXBean threadMXBean) {
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
        this.similarityThreshold = similarityThreshold;
        this.inputTooShort = false;
        this.runtimeUser = 0;
        this.runtimeTotal = 0;

        this.runtimeText = new Runtime();
        this.runtimeCode = new Runtime();
        this.resultsText = new HashMap<>();
        this.resultsCode = new HashMap<>();

        this.numberOfRepetitions = numberOfRepetitions;
        this.currentRepetition = 0;

        this.threadMXBean = threadMXBean;
        this.stopWatch = Stopwatch.createUnstarted();
    }

    private void reset() {
        this.inputTooShort = false;
        this.runtimeTotal = 0;
        this.runtimeUser = 0;
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
        long startUserTimeNano, endUserTimeNano;

        // process version history of text blocks
        stopWatch.start();
        startUserTimeNano = threadMXBean.getCurrentThreadUserTime();
        try {
            postVersionList.processVersionHistory(config, postBlockTypeFilter);
        } catch (InputTooShortException e) {
            inputTooShort = true;
        } finally {
            endUserTimeNano = threadMXBean.getCurrentThreadUserTime();
            stopWatch.stop();
        }

        // calculate runtimeUser and set results
        if (startUserTimeNano < 0 || endUserTimeNano < 0) {
            throw new IllegalArgumentException("User time has not been calculated correctly.");
        }

        // set results for text blocks
        runtimeUser = endUserTimeNano-startUserTimeNano;
        runtimeTotal = stopWatch.elapsed().getNano();

        if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
            setResult(resultsText, runtimeText, postBlockTypeFilter);
        } else if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
            setResult(resultsCode, runtimeCode, postBlockTypeFilter);
        } else {
            throw new IllegalArgumentException("Invalid PostBlockTypeFilter: " + postBlockTypeFilter);
        }

        // reset flag inputTooShort, stopWatch, and runtime variables
        this.reset();
        // reset post block version history
        postVersionList.resetPostBlockVersionHistory();
    }

    private void setResult(Map<Integer, MetricResult> results, Runtime runtime,
                           Set<Integer> postBlockTypeFilter) {
        if (currentRepetition == 1) {
            // set initial values after first run, return runtimeUser
            for (int postHistoryId : postHistoryIds) {
                MetricResult result = getResultsAndSetRuntime(postHistoryId, runtime, postBlockTypeFilter);
                results.put(postHistoryId, result);
            }
        } else {
            // compare result values in later runs
            for (int postHistoryId : postHistoryIds) {
                MetricResult resultInMap = results.get(postHistoryId);
                MetricResult newResult = getResultsAndSetRuntime(postHistoryId, runtime, postBlockTypeFilter);
                boolean truePositivesEqual = (resultInMap.getTruePositives() == null && newResult.getTruePositives() == null)
                        || (resultInMap.getTruePositives() != null && newResult.getTruePositives() != null
                        && resultInMap.getTruePositives().equals(newResult.getTruePositives()));
                boolean falsePositivesEqual = (resultInMap.getFalsePositives() == null && newResult.getFalsePositives() == null)
                        || (resultInMap.getFalsePositives() != null && newResult.getFalsePositives() != null
                        && resultInMap.getFalsePositives().equals(newResult.getFalsePositives()));
                boolean trueNegativesEqual = (resultInMap.getTrueNegatives() == null && newResult.getTrueNegatives() == null)
                        || (resultInMap.getTrueNegatives() != null && newResult.getTrueNegatives() != null
                        && resultInMap.getTrueNegatives().equals(newResult.getTrueNegatives()));
                boolean falseNegativesEqual = (resultInMap.getFalseNegatives() == null && newResult.getFalseNegatives() == null)
                        || (resultInMap.getFalseNegatives() != null && newResult.getFalseNegatives() != null
                        && resultInMap.getFalseNegatives().equals(newResult.getFalseNegatives()));
                boolean postBlockCountEqual = (resultInMap.getPostBlockCount() == null && newResult.getPostBlockCount() == null)
                        || (resultInMap.getPostBlockCount() != null && newResult.getPostBlockCount() != null
                        && resultInMap.getPostBlockCount().equals(newResult.getPostBlockCount()));

                if (!truePositivesEqual || !falsePositivesEqual || !trueNegativesEqual || !falseNegativesEqual
                        || !postBlockCountEqual) {
                    throw new IllegalStateException("Metric results changed from repetition "
                            + (currentRepetition - 1) + " to " + currentRepetition);
                }
            }
        }
    }

    private MetricResult getResultsAndSetRuntime(int postHistoryId, Runtime runtime, Set<Integer> postBlockTypeFilter) {
        MetricResult result = new MetricResult();

        if (currentRepetition == 1) {
            runtime.setRuntimeTotal(runtimeTotal);
            runtime.setRuntimeUser(runtimeUser);
        } else if (currentRepetition < numberOfRepetitions) {
            runtime.setRuntimeTotal(runtime.getRuntimeTotal() + runtimeTotal);
            runtime.setRuntimeUser(runtime.getRuntimeUser() + runtimeUser);
        } else {
            runtime.setRuntimeTotal(Math.round((double)(runtime.getRuntimeTotal() + runtimeTotal) / numberOfRepetitions));
            runtime.setRuntimeUser(Math.round((double)(runtime.getRuntimeUser() + runtimeUser) / numberOfRepetitions));
        }

        if (!inputTooShort) {
            int possibleConnections = postGroundTruth.getPossibleConnections(postHistoryId, postBlockTypeFilter);
            Set<PostBlockConnection> postBlockConnections = postVersionList.getPostVersion(postHistoryId).getConnections(postBlockTypeFilter);
            Set<PostBlockConnection> postBlockConnectionsGT = postGroundTruth.getConnections(postHistoryId, postBlockTypeFilter);

            int truePositivesCount = PostBlockConnection.intersection(postBlockConnectionsGT, postBlockConnections).size();
            int falsePositivesCount = PostBlockConnection.difference(postBlockConnections, postBlockConnectionsGT).size();

            int trueNegativesCount = possibleConnections - (PostBlockConnection.union(postBlockConnectionsGT, postBlockConnections).size());
            int falseNegativesCount = PostBlockConnection.difference(postBlockConnectionsGT, postBlockConnections).size();

            int allConnectionsCount = truePositivesCount + falsePositivesCount + trueNegativesCount + falseNegativesCount;
            if (possibleConnections != allConnectionsCount) {
                throw new IllegalStateException("Invalid result (expected: " + possibleConnections
                        + "; actual: " + allConnectionsCount + ")");
            }

            result.setTruePositives(truePositivesCount);
            result.setFalsePositives(falsePositivesCount);
            result.setTrueNegatives(trueNegativesCount);
            result.setFalseNegatives(falseNegativesCount);

            result.setPostBlockCount(0);
            if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId))
                result.setPostBlockCount(result.getPostBlockCount() + postVersionList.getPostVersion(postHistoryId).getTextBlocks().size());
            if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId))
                result.setPostBlockCount(result.getPostBlockCount() + postVersionList.getPostVersion(postHistoryId).getCodeBlocks().size());
        }

        return result;
    }

    public BiFunction<String, String, Double> getSimilarityMetric() {
        return similarityMetric;
    }

    public String getSimilarityMetricName() {
        return similarityMetricName;
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

    public Runtime getRuntimeText() {
        return runtimeText;
    }

    public Runtime getRuntimeCode() {
        return runtimeCode;
    }

    public MetricResult getResultText(int postHistoryId) {
        return resultsText.get(postHistoryId);
    }

    public MetricResult getResultCode(int postHistoryId) {
        return resultsCode.get(postHistoryId);
    }
}
