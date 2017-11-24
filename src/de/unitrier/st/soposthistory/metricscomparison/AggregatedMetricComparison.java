package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;

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

    void writeToCSV(CSVPrinter csvPrinterAggregated, int maxFailedPredecessorComparisonsText, int maxFailedPredecessorComparisonsCode) throws IOException {
        double relativeFailedPredecessorComparisonsText = ((double)aggregatedResultsText.getFailedPredecessorComparisons()) / maxFailedPredecessorComparisonsText;
        double relativeFailedPredecessorComparisonsCode = ((double)aggregatedResultsCode.getFailedPredecessorComparisons()) / maxFailedPredecessorComparisonsCode;

        // "MetricType", "Metric", "Threshold", "QualityText", "QualityCode", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",
        // "RuntimeText", "TextBlockVersionCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText",
        // "FailedPredecessorComparisonsText", "PrecisionText", "RecallText", "RelativeFailedPredecessorComparisonsText",
        // "RuntimeCode", "CodeBlockVersionCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode",
        // "FailedPredecessorComparisonsCode", "PrecisionCode", "RecallCode", "RelativeFailedPredecessorComparisonsCode"
        csvPrinterAggregated.printRecord(
                getSimilarityMetricType(),
                getSimilarityMetricName(),
                getSimilarityThreshold(),
                MetricComparisonManager.calculateQualityMeasure(
                        getPrecisionText(),
                        getRecallText(),
                        relativeFailedPredecessorComparisonsText
                ),
                MetricComparisonManager.calculateQualityMeasure(
                        getPrecisionCode(),
                        getRecallCode(),
                        relativeFailedPredecessorComparisonsCode
                ),
                postVersionList.size(),
                postVersionList.getPostBlockVersionCount(),
                aggregatedResultsText.getPossibleConnections() + aggregatedResultsCode.getPossibleConnections(),
                aggregatedRuntimeText.getTotalRuntime(),
                aggregatedResultsText.getPostBlockVersionCount(),
                aggregatedResultsText.getPossibleConnections(),
                aggregatedResultsText.getTruePositives(),
                aggregatedResultsText.getTrueNegatives(),
                aggregatedResultsText.getFalsePositives(),
                aggregatedResultsText.getFalseNegatives(),
                aggregatedResultsText.getFailedPredecessorComparisons(),
                getPrecisionText(),
                getRecallText(),
                relativeFailedPredecessorComparisonsText,
                aggregatedRuntimeCode.getTotalRuntime(),
                aggregatedResultsCode.getPostBlockVersionCount(),
                aggregatedResultsCode.getPossibleConnections(),
                aggregatedResultsCode.getTruePositives(),
                aggregatedResultsCode.getTrueNegatives(),
                aggregatedResultsCode.getFalsePositives(),
                aggregatedResultsCode.getFalseNegatives(),
                aggregatedResultsCode.getFailedPredecessorComparisons(),
                getPrecisionCode(),
                getRecallCode(),
                relativeFailedPredecessorComparisonsCode
        );
    }
}
