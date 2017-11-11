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

            long startUserTimeNano, endUserTimeNano;

            // process version history of text blocks
            stopWatch.start();
            startUserTimeNano = threadMXBean.getCurrentThreadUserTime();
            try {
                postVersionList.processVersionHistory(config, TextBlockVersion.getPostBlockTypeIdFilter());
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
            setResultText();

            // reset flag inputTooShort, stopWatch, and runtime variables
            this.reset();
            // reset post block version history
            postVersionList.resetPostBlockVersionHistory();

            // process version history of code blocks
            stopWatch.start();
            startUserTimeNano = threadMXBean.getCurrentThreadUserTime();
            try {
                postVersionList.processVersionHistory(config, CodeBlockVersion.getPostBlockTypeIdFilter());
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

            // set results for code blocks
            runtimeUser = endUserTimeNano-startUserTimeNano;
            runtimeTotal = stopWatch.elapsed().getNano();
            setResultCode();

            // reset flag inputTooShort, stopWatch, and runtime variables
            this.reset();
            // reset post block version history
            postVersionList.resetPostBlockVersionHistory();
        }
    }

    private void setResultText() {
        setResult(resultsText, runtimeText, TextBlockVersion.getPostBlockTypeIdFilter());
    }

    private void setResultCode() {
        setResult(resultsCode, runtimeCode, CodeBlockVersion.getPostBlockTypeIdFilter());
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
                boolean truePositivesEqual = (resultInMap.truePositives == null && newResult.truePositives == null)
                        || (resultInMap.truePositives != null && newResult.truePositives != null
                        && resultInMap.truePositives.equals(newResult.truePositives));
                boolean falsePositivesEqual = (resultInMap.falsePositives == null && newResult.falsePositives == null)
                        || (resultInMap.falsePositives != null && newResult.falsePositives != null
                        && resultInMap.falsePositives.equals(newResult.falsePositives));
                boolean trueNegativesEqual = (resultInMap.trueNegatives == null && newResult.trueNegatives == null)
                        || (resultInMap.trueNegatives != null && newResult.trueNegatives != null
                        && resultInMap.trueNegatives.equals(newResult.trueNegatives));
                boolean falseNegativesEqual = (resultInMap.falseNegatives == null && newResult.falseNegatives == null)
                        || (resultInMap.falseNegatives != null && newResult.falseNegatives != null
                        && resultInMap.falseNegatives.equals(newResult.falseNegatives));
                boolean postBlockCountEqual = (resultInMap.postBlockCount == null && newResult.postBlockCount == null)
                        || (resultInMap.postBlockCount != null && newResult.postBlockCount != null
                        && resultInMap.postBlockCount.equals(newResult.postBlockCount));

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
            runtime.runtimeTotal = runtimeTotal;
            runtime.runtimeUser = runtimeUser;
        } else if (currentRepetition < numberOfRepetitions) {
            runtime.runtimeTotal = runtime.runtimeTotal + runtimeTotal;
            runtime.runtimeUser = runtime.runtimeUser + runtimeUser;
        } else {
            runtime.runtimeTotal = Math.round((double)(runtime.runtimeTotal + runtimeTotal) / numberOfRepetitions);
            runtime.runtimeUser = Math.round((double)(runtime.runtimeUser + runtimeUser) / numberOfRepetitions);
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

            result.truePositives = truePositivesCount;
            result.falsePositives = falsePositivesCount;
            result.trueNegatives = trueNegativesCount;
            result.falseNegatives = falseNegativesCount;

            result.postBlockCount = 0;
            if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId))
                result.postBlockCount += postVersionList.getPostVersion(postHistoryId).getTextBlocks().size();
            if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId))
                result.postBlockCount += postVersionList.getPostVersion(postHistoryId).getCodeBlocks().size();
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

    public class Runtime {
        private Long runtimeTotal = null;
        private Long runtimeUser = null;

        public Long getRuntimeTotal() {
            return runtimeTotal;
        }

        public Long getRuntimeUser() {
            return runtimeUser;
        }
    }

    public class MetricResult {
        private Integer postBlockCount = null;
        private Integer truePositives = null;
        private Integer falsePositives = null;
        private Integer trueNegatives = null;
        private Integer falseNegatives = null;

        public Integer getPostBlockCount() {
            return postBlockCount;
        }

        public Integer getTruePositives() {
            return truePositives;
        }

        public Integer getFalsePositives() {
            return falsePositives;
        }

        public Integer getTrueNegatives() {
            return trueNegatives;
        }

        public Integer getFalseNegatives() {
            return falseNegatives;
        }
    }
}
