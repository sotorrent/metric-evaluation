package de.unitrier.st.soposthistory.metricscomparison;

public class RuntimePost extends Runtime {
    String similarityMetricName;
    double similarityThreshold;

    RuntimePost(String similarityMetricName, double similarityThreshold, Long runtimeTotal, Long runtimeUser) {
        super(runtimeTotal, runtimeUser);
        this.similarityMetricName = similarityMetricName;
        this.similarityThreshold = similarityThreshold;
    }

    String getSimilarityMetricName() {
        return similarityMetricName;
    }

    void setSimilarityMetricName(String similarityMetricName) {
        this.similarityMetricName = similarityMetricName;
    }

    double getSimilarityThreshold() {
        return similarityThreshold;
    }

    void setSimilarityThreshold(int similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}
