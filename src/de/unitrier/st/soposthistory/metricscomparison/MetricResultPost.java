package de.unitrier.st.soposthistory.metricscomparison;

public class MetricResultPost extends MetricResult {
    String similarityMetricName;
    double similarityThreshold;

    public MetricResultPost(String similarityMetricName, double similarityThreshold,
                            Integer postBlockCount,
                            Integer truePositives, Integer falsePositives,
                            Integer trueNegatives, Integer falseNegatives) {
        super(postBlockCount, truePositives, falsePositives, trueNegatives, falseNegatives);
        this.similarityMetricName = similarityMetricName;
        this.similarityThreshold = similarityThreshold;
    }

    public String getSimilarityMetricName() {
        return similarityMetricName;
    }

    public void setSimilarityMetricName(String similarityMetricName) {
        this.similarityMetricName = similarityMetricName;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(int similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}
