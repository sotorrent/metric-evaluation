package de.unitrier.st.soposthistory.metricscomparison;

import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

class AggregatedMetricComparisonList extends LinkedList<AggregatedMetricComparison> {
    private int maxFailedPredecessorComparisonsText = 0;
    private int maxFailedPredecessorComparisonsCode = 0;
    private boolean maxValuesRetrieved = false;

    static AggregatedMetricComparisonList fromMetricComparisons(List<MetricComparison> metricComparisons) {
        AggregatedMetricComparisonList list = new AggregatedMetricComparisonList();
        list.addMetricComparisons(metricComparisons);
        return list;
    }

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

    int getMaxFailedPredecessorComparisonsText() {
        if (!maxValuesRetrieved) {
            retrieveMaxValues();
        }
        return maxFailedPredecessorComparisonsText;
    }

    int getMaxFailedPredecessorComparisonsCode() {
        if (!maxValuesRetrieved) {
            retrieveMaxValues();
        }
        return maxFailedPredecessorComparisonsCode;
    }

    void writeToCSV(CSVPrinter csvPrinterAggregated) throws IOException {
        for (AggregatedMetricComparison aggregatedMetricComparison : this) {
            aggregatedMetricComparison.writeToCSV(csvPrinterAggregated, maxFailedPredecessorComparisonsText, maxFailedPredecessorComparisonsCode);
        }
    }
}
