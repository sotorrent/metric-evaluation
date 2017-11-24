package de.unitrier.st.soposthistory.metricscomparison;

import java.util.LinkedList;
import java.util.List;

public class AggregatedMetricComparisonList extends LinkedList<AggregatedMetricComparison> {
    private int maxFailedPredecessorComparisonsText = 0;
    private int maxFailedPredecessorComparisonsCode = 0;
    private boolean maxValuesRetrieved = false;

    void addMetricComparisons(List<MetricComparison> metricComparisons) {
        if (this.size() == 0){
            for (MetricComparison metricComparison : metricComparisons) {
                this.add(AggregatedMetricComparison.fromMetricComparison(metricComparison));
            }
        } else {
            for (AggregatedMetricComparison aggregatedMetricComparison : this) {
                for (MetricComparison metricComparison : metricComparisons) {
                    boolean nameEquals = aggregatedMetricComparison.getSimilarityMetricName().equals(metricComparison.getSimilarityMetricName());
                    boolean thresholdEquals = aggregatedMetricComparison.getSimilarityThreshold() == metricComparison.getSimilarityThreshold();
                    boolean typeEquals = aggregatedMetricComparison.getSimilarityMetricType() == metricComparison.getSimilarityMetricType();

                    if (nameEquals && thresholdEquals && typeEquals) {
                        // aggregate values
                        aggregatedMetricComparison.add(metricComparison);
                        break;
                    }
                }
            }
        }
    }

    private void retrieveMaxValues() {
        for (AggregatedMetricComparison aggregatedMetricComparison : this) {
            MetricResult aggregatedResultsText = aggregatedMetricComparison.getAggregatedResultsText();
            MetricResult aggregatedResultsCode= aggregatedMetricComparison.getAggregatedResultsCode();
            maxFailedPredecessorComparisonsText = Math.max(maxFailedPredecessorComparisonsText, aggregatedResultsText.getFailedPredecessorComparisons());
            maxFailedPredecessorComparisonsCode = Math.max(maxFailedPredecessorComparisonsCode, aggregatedResultsCode.getFailedPredecessorComparisons());
        }
        maxValuesRetrieved = true;
    }

    public int getMaxFailedPredecessorComparisonsText() {
        if (!maxValuesRetrieved) {
            retrieveMaxValues();
        }
        return maxFailedPredecessorComparisonsText;
    }

    public int getMaxFailedPredecessorComparisonsCode() {
        if (!maxValuesRetrieved) {
            retrieveMaxValues();
        }
        return maxFailedPredecessorComparisonsCode;
    }
}
