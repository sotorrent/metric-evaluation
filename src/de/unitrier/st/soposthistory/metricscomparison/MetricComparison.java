package de.unitrier.st.soposthistory.metricscomparison;

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

    // text
    private double runtimeText;
    // PostHistoryId -> metric results for text blocks
    private Map<Integer, MetricResult> resultsText;

    // code
    private double runtimeCode;
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
        this.resultsText = new HashMap<>();
        this.resultsCode = new HashMap<>();

        this.numberOfRepetitions = numberOfRepetitions;
        this.currentRepetition = 0;

        this.threadMXBean = threadMXBean;
    }

    private void reset() {
        this.inputTooShort = false;
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
            startUserTimeNano = threadMXBean.getCurrentThreadUserTime();
            try {
                postVersionList.processVersionHistory(config, TextBlockVersion.getPostBlockTypeIdFilter());
            } catch (InputTooShortException e) {
                inputTooShort = true;
            } finally {
                endUserTimeNano = threadMXBean.getCurrentThreadUserTime() - startUserTimeNano;
                setResultsText(endUserTimeNano);
            }

            // reset flag inputTooShort
            this.reset();
            // reset post block version history
            postVersionList.resetPostBlockVersionHistory();

            // process version history of code blocks
            startUserTimeNano = threadMXBean.getCurrentThreadUserTime();
            try {
                postVersionList.processVersionHistory(config, CodeBlockVersion.getPostBlockTypeIdFilter());
            } catch (InputTooShortException e) {
                inputTooShort = true;
            } finally {
                endUserTimeNano = threadMXBean.getCurrentThreadUserTime() - startUserTimeNano;
                setResultsCode(endUserTimeNano);
            }

            // reset flag inputTooShort
            this.reset();
            // reset post block version history
            postVersionList.resetPostBlockVersionHistory();
        }
    }

    private void setResultsText(long userTimeNano) {
        runtimeText = setResults(resultsText, runtimeText, userTimeNano, TextBlockVersion.getPostBlockTypeIdFilter());
    }

    private void setResultsCode(long userTimeNano) {
        runtimeCode = setResults(resultsCode, runtimeCode, userTimeNano, CodeBlockVersion.getPostBlockTypeIdFilter());
    }

    private double setResults(Map<Integer, MetricResult> results, double runtimeOld, long runtimeNew,
                              Set<Integer> postBlockTypeFilter) {
        if (currentRepetition == 1) {
            // set initial values after first run, return runtime
            for (int postHistoryId : postHistoryIds) {
                MetricResult result = getResults(postHistoryId, postBlockTypeFilter);
                results.put(postHistoryId, result);
            }
            return runtimeNew;
        } else {
            // compare result values in later runs
            for (int postHistoryId : postHistoryIds) {
                MetricResult resultInMap = results.get(postHistoryId);
                MetricResult newResult = getResults(postHistoryId, postBlockTypeFilter);
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

            if (currentRepetition < numberOfRepetitions) {
                // return sum of runtimes
                return runtimeOld + runtimeNew;
            } else {
                // calculate and return mean runtime after last run
                return (runtimeOld + runtimeNew) / (double) numberOfRepetitions;
            }
        }
    }

    private MetricResult getResults(int postHistoryId, Set<Integer> postBlockTypeFilter) {
        MetricResult result = new MetricResult();

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

    public double getRuntimeText() {
        return runtimeText;
    }

    public MetricResult getResultsText(int postHistoryId) {
        return resultsText.get(postHistoryId);
    }

    public double getRuntimeCode() {
        return runtimeCode;
    }

    public MetricResult getResultsCode(int postHistoryId) {
        return resultsCode.get(postHistoryId);
    }

    public PostVersionList getPostVersionList() {
        return postVersionList;
    }

    public class MetricResult {
        Integer truePositives = null;
        Integer falsePositives = null;
        Integer trueNegatives = null;
        Integer falseNegatives = null;
        Integer postBlockCount = null; // needed to compute true negatives in test cases

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

        public Integer getPostBlockCount() {
            return postBlockCount;
        }
    }
}
