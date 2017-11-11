package de.unitrier.st.soposthistory.metricscomparison;

public class MetricResult {
    Integer postBlockVersionCount;
    Integer truePositives;
    Integer falsePositives;
    Integer trueNegatives;
    Integer falseNegatives;

    MetricResult() {
        this.postBlockVersionCount = null;
        this.truePositives = null;
        this.falsePositives = null;
        this.trueNegatives = null;
        this.falseNegatives = null;
    }

    MetricResult(Integer postBlockVersionCount, Integer truePositives, Integer falsePositives, Integer trueNegatives, Integer falseNegatives) {
        this.postBlockVersionCount = postBlockVersionCount;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;
    }

    public Integer getPostBlockVersionCount() {
        return postBlockVersionCount;
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

    void setPostBlockVersionCount(Integer postBlockVersionCount) {
        this.postBlockVersionCount = postBlockVersionCount;
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

    void add(MetricResult result) {
        if (getPostBlockVersionCount() != null) {
            if (result.getPostBlockVersionCount() != null) {
                setPostBlockVersionCount(getPostBlockVersionCount() + result.getPostBlockVersionCount());
            }
        } else {
            setPostBlockVersionCount(result.getPostBlockVersionCount());
        }

        if (getTruePositives() != null) {
            if (result.getTruePositives() != null) {
                setTruePositives(getTruePositives() + result.getTruePositives());
            }
        } else {
            setTruePositives(result.getTruePositives());
        }

        if (getFalsePositives() != null) {
            if (result.getFalsePositives() != null) {
                setFalsePositives(getFalsePositives() + result.getFalsePositives());
            }
        } else {
            setFalsePositives(result.getFalsePositives());
        }

        if (getTrueNegatives() != null) {
            if (result.getTrueNegatives() != null) {
                setTrueNegatives(getTrueNegatives() + result.getTrueNegatives());
            }
        } else {
            setTrueNegatives(result.getTrueNegatives());
        }

        if (getFalseNegatives() != null) {
            if (result.getFalseNegatives() != null) {
                setFalseNegatives(getFalseNegatives() + result.getFalseNegatives());
            }
        } else {
            setFalseNegatives(result.getFalseNegatives());
        }
    }
}
