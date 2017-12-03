package de.unitrier.st.soposthistory.metricscomparison.evaluation;

import java.util.function.BiFunction;

public class SimilarityMetric {
    public enum MetricType {EQUAL, EDIT, FINGERPRINT, PROFILE, SET}

    final private String name;
    final private BiFunction<String, String, Double> metric;
    final private MetricType type;
    final private double threshold;

    public SimilarityMetric(String name, BiFunction<String, String, Double> metric,
                     MetricType type, double threshold) {
        this.name = name;
        this.metric = metric;
        this.type = type;
        this.threshold = threshold;
    }

    public String getName() {
        return name;
    }

    BiFunction<String, String, Double> getMetric() {
        return metric;
    }

    MetricType getType() {
        return type;
    }

    double getThreshold() {
        return threshold;
    }

    boolean equals(SimilarityMetric other) {
        return name.equals(other.getName())
                && metric.equals(other.getMetric())
                && type == other.getType()
                && threshold == other.getThreshold();
    }

    @Override
    public String toString() {
        return "(" + name + "; " + threshold + ")";
    }
}
