package de.unitrier.st.soposthistory.metricscomparison;

public class MetricResult {
    private SimilarityMetric similarityMetric;
    private int postCount;
    private int postVersionCount;
    private int postBlockVersionCount;
    private int possibleConnections;
    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;
    private int failedPredecessorComparisons;
    private long runtime;

    MetricResult(SimilarityMetric similarityMetric) {
        this.similarityMetric = similarityMetric;
        this.postCount = 0;
        this.postVersionCount = 0;
        this.postBlockVersionCount = 0;
        this.possibleConnections = 0;
        this.truePositives = 0;
        this.falsePositives = 0;
        this.trueNegatives = 0;
        this.falseNegatives = 0;
        this.failedPredecessorComparisons = 0;
        this.runtime = 0;
    }

    SimilarityMetric getSimilarityMetric() {
        return similarityMetric;
    }

    int getPostCount() {
        return postCount;
    }

    int getPostVersionCount() {
        return postVersionCount;
    }

    public int getPostBlockVersionCount() {
        return postBlockVersionCount;
    }

    int getPossibleConnections() {
        return possibleConnections;
    }

    public int getTruePositives() {
        return truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    public int getFailedPredecessorComparisons() {
        return failedPredecessorComparisons;
    }

    long getRuntime() {
        return runtime;
    }

    void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    void setPostVersionCount(int postVersionCount) {
        this.postVersionCount = postVersionCount;
    }

    void setPostBlockVersionCount(int postBlockVersionCount) {
        this.postBlockVersionCount = postBlockVersionCount;
    }

    void setPossibleConnections(int possibleConnections) {
        this.possibleConnections = possibleConnections;
    }

    void setTruePositives(int truePositives) {
        this.truePositives = truePositives;
    }

    void setFalsePositives(int falsePositives) {
        this.falsePositives = falsePositives;
    }

    void setTrueNegatives(int trueNegatives) {
        this.trueNegatives = trueNegatives;
    }

    void setFalseNegatives(int falseNegatives) {
        this.falseNegatives = falseNegatives;
    }

    void setFailedPredecessorComparisons(int failedPredecessorComparisons) {
        this.failedPredecessorComparisons = failedPredecessorComparisons;
    }

    void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    void add(MetricResult result) {
        postCount += result.getPostCount();
        postVersionCount += result.getPostVersionCount();
        postBlockVersionCount += result.getPostBlockVersionCount();
        possibleConnections += result.getPossibleConnections();
        truePositives += result.getTruePositives();
        falsePositives += result.getFalsePositives();
        trueNegatives += result.getTrueNegatives();
        falseNegatives += result.getFalseNegatives();
        failedPredecessorComparisons += result.getFailedPredecessorComparisons();
        runtime += result.getRuntime();
    }

    double getPrecision() {
        return ((double) truePositives) / (truePositives + falsePositives);
    }

    double getRecall() {
        return ((double) truePositives) / (truePositives + falseNegatives);
    }

    double getSensitivity() {
        return getRecall();
    }

    double getSpecificity() {
        return ((double) trueNegatives) / (trueNegatives + falsePositives);
    }

    double getYoudensJ() {
        // https://en.wikipedia.org/wiki/Youden%27s_J_statistic
        return getSensitivity() + getSpecificity() - 1;
    }

    double getFailureRate(int maxFailures) {
        return maxFailures == 0 ? 0.0 : ((double) failedPredecessorComparisons) / maxFailures;
    }
}
