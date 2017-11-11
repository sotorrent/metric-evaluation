package de.unitrier.st.soposthistory.metricscomparison;

public class MetricResult {
    Integer postBlockCount;
    Integer truePositives;
    Integer falsePositives;
    Integer trueNegatives;
    Integer falseNegatives;

    MetricResult() {
        this.postBlockCount = null;
        this.truePositives = null;
        this.falsePositives = null;
        this.trueNegatives = null;
        this.falseNegatives = null;
    }

    MetricResult(Integer postBlockCount, Integer truePositives, Integer falsePositives, Integer trueNegatives, Integer falseNegatives) {
        this.postBlockCount = postBlockCount;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;
    }

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

    void setPostBlockCount(Integer postBlockCount) {
        this.postBlockCount = postBlockCount;
    }

    void setTruePositives(Integer truePositives) {
        this.truePositives = truePositives;
    }

    void setFalsePositives(Integer falsePositives) {
        this.falsePositives = falsePositives;
    }

    void setTrueNegatives(Integer trueNegatives) {
        this.trueNegatives = trueNegatives;
    }

    void setFalseNegatives(Integer falseNegatives) {
        this.falseNegatives = falseNegatives;
    }
}
