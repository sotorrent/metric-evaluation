package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.version.PostVersionList;

class AggregatedMetricComparison {
    private String similarityMetricName;
    private MetricComparison.MetricType similarityMetricType;
    private double similarityThreshold;

    private MetricRuntime aggregatedRuntimeText;
    private MetricResult aggregatedResultsText;

    private MetricRuntime aggregatedRuntimeCode;
    private MetricResult aggregatedResultsCode;

    private PostVersionList postVersionList;

    private AggregatedMetricComparison(String similarityMetricName, MetricComparison.MetricType similarityMetricType,
                               double similarityThreshold,
                               MetricRuntime aggregatedRuntimeText, MetricResult aggregatedResultsText,
                               MetricRuntime aggregatedRuntimeCode, MetricResult aggregatedResultsCode,
                               PostVersionList postVersionList) {
        this.similarityMetricName = similarityMetricName;
        this.similarityMetricType = similarityMetricType;
        this.similarityThreshold = similarityThreshold;
        this.aggregatedRuntimeText = aggregatedRuntimeText;
        this.aggregatedResultsText = aggregatedResultsText;
        this.aggregatedRuntimeCode = aggregatedRuntimeCode;
        this.aggregatedResultsCode = aggregatedResultsCode;
        this.postVersionList = postVersionList;
    }

    static AggregatedMetricComparison fromMetricComparison(MetricComparison metricComparison) {
        return new AggregatedMetricComparison(
                metricComparison.getSimilarityMetricName(),
                metricComparison.getSimilarityMetricType(),
                metricComparison.getSimilarityThreshold(),
                metricComparison.getRuntimeText(),
                metricComparison.getAggregatedResultsText(),
                metricComparison.getRuntimeCode(),
                metricComparison.getAggregatedResultsCode(),
                metricComparison.getPostVersionList()
        );
    }

    void add(MetricComparison metricComparison) {
        aggregatedRuntimeText.add(metricComparison.getRuntimeText());
        aggregatedResultsText.add(metricComparison.getAggregatedResultsText());

        aggregatedRuntimeCode.add(metricComparison.getRuntimeCode());
        aggregatedResultsCode.add(metricComparison.getAggregatedResultsCode());

        postVersionList.addAll(metricComparison.getPostVersionList());
    }

    String getSimilarityMetricName() {
        return similarityMetricName;
    }

    MetricComparison.MetricType getSimilarityMetricType() {
        return similarityMetricType;
    }

    double getSimilarityThreshold() {
        return similarityThreshold;
    }

    MetricRuntime getAggregatedRuntimeText() {
        return aggregatedRuntimeText;
    }

    MetricResult getAggregatedResultsText() {
        return aggregatedResultsText;
    }

    MetricRuntime getAggregatedRuntimeCode() {
        return aggregatedRuntimeCode;
    }

    MetricResult getAggregatedResultsCode() {
        return aggregatedResultsCode;
    }

    PostVersionList getPostVersionList() {
        return postVersionList;
    }

    double getPrecisionText() {
        return ((double) aggregatedResultsText.getTruePositives())
                / (aggregatedResultsText.getTruePositives() + aggregatedResultsText.getFalsePositives());
    }

    double getRecallText() {
        return ((double) aggregatedResultsText.getTruePositives())
                / (aggregatedResultsText.getTruePositives() + aggregatedResultsText.getFalseNegatives());
    }

    double getPrecisionCode() {
        return ((double) aggregatedResultsCode.getTruePositives())
                / (aggregatedResultsCode.getTruePositives() + aggregatedResultsCode.getFalsePositives());
    }

    double getRecallCode() {
        return ((double) aggregatedResultsCode.getTruePositives())
                / (aggregatedResultsCode.getTruePositives() + aggregatedResultsCode.getFalseNegatives());
    }
}
