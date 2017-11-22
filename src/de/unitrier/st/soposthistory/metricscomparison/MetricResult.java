package de.unitrier.st.soposthistory.metricscomparison;

public class MetricResult {
    private int postBlockVersionCount;
    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;
    private int failedPredecessorComparisons;

    MetricResult() {
        this.postBlockVersionCount = 0;
        this.truePositives = 0;
        this.falsePositives = 0;
        this.trueNegatives = 0;
        this.falseNegatives = 0;
        this.failedPredecessorComparisons = 0;
    }

    MetricResult(int postBlockVersionCount,
                 int truePositives, int falsePositives, int trueNegatives, int falseNegatives,
                 int failedPredecessorComparisons) {
        this.postBlockVersionCount = postBlockVersionCount;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;
        this.failedPredecessorComparisons = failedPredecessorComparisons;
    }

    public int getPostBlockVersionCount() {
        return postBlockVersionCount;
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

    void setPostBlockVersionCount(int postBlockVersionCount) {
        this.postBlockVersionCount = postBlockVersionCount;
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

    public void setFailedPredecessorComparisons(int failedPredecessorComparisons) {
        this.failedPredecessorComparisons = failedPredecessorComparisons;
    }

    void add(MetricResult result) {
        setPostBlockVersionCount(getPostBlockVersionCount() + result.getPostBlockVersionCount());
        setTruePositives(getTruePositives() + result.getTruePositives());
        setFalsePositives(getFalsePositives() + result.getFalsePositives());
        setTrueNegatives(getTrueNegatives() + result.getTrueNegatives());
        setFalseNegatives(getFalseNegatives() + result.getFalseNegatives());
        setFailedPredecessorComparisons(getFailedPredecessorComparisons() + result.getFailedPredecessorComparisons());
    }
}
