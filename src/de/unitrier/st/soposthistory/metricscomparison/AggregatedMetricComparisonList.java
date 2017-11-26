package de.unitrier.st.soposthistory.metricscomparison;

import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

class AggregatedMetricComparisonList extends LinkedList<AggregatedMetricComparison> {
    private int maxFailuresText = 0;
    private int maxFailuresCode = 0;
    private boolean maxValuesRetrieved = false;

    void addMetricComparisons(List<MetricComparison> metricComparisons) {
        for (MetricComparison metricComparison : metricComparisons) {
            boolean inList = false;
            for (AggregatedMetricComparison aggregatedMetricComparison : this) {
                boolean nameEquals = aggregatedMetricComparison.getSimilarityMetricName().equals(metricComparison.getSimilarityMetricName());
                boolean thresholdEquals = aggregatedMetricComparison.getSimilarityThreshold() == metricComparison.getSimilarityThreshold();
                boolean typeEquals = aggregatedMetricComparison.getSimilarityMetricType() == metricComparison.getSimilarityMetricType();

                if (nameEquals && thresholdEquals && typeEquals) {
                    // aggregate values
                    aggregatedMetricComparison.add(metricComparison);
                    inList = true;
                    break;
                }
            }
            if (!inList) {
                add(AggregatedMetricComparison.fromMetricComparison(metricComparison));
            }
        }
    }

    private void retrieveMaxValues() {
        for (AggregatedMetricComparison aggregatedMetricComparison : this) {
            MetricResult aggregatedResultsText = aggregatedMetricComparison.getAggregatedResultsText();
            MetricResult aggregatedResultsCode= aggregatedMetricComparison.getAggregatedResultsCode();
            maxFailuresText = Math.max(maxFailuresText, aggregatedResultsText.getFailedPredecessorComparisons());
            maxFailuresCode = Math.max(maxFailuresCode, aggregatedResultsCode.getFailedPredecessorComparisons());
        }
        maxValuesRetrieved = true;
    }

    int getMaxFailuresText() {
        if (!maxValuesRetrieved) {
            retrieveMaxValues();
        }
        return maxFailuresText;
    }

    int getMaxFailuresCode() {
        if (!maxValuesRetrieved) {
            retrieveMaxValues();
        }
        return maxFailuresCode;
    }

    void writeToCSV(CSVPrinter csvPrinterAggregated) throws IOException {
        if (this.size() < 1) {
            return;
        }

        AggregatedMetricComparison first = this.getFirst();

        int postCountFirst = first.getPostCount();
        int postVersionCountFirst = first.getPostVersionCount();
        int textBlockVersionCountFirst = first.getAggregatedResultsText().getPostBlockVersionCount();
        int codeBlockVersionCountFirst = first.getAggregatedResultsCode().getPostBlockVersionCount();
        int textPossibleConnectionsFirst = first.getAggregatedResultsText().getPossibleConnections();
        int codePossibleConnectionsFirst = first.getAggregatedResultsCode().getPossibleConnections();

        for (AggregatedMetricComparison aggregatedMetricComparison : this) {
            int postCount = aggregatedMetricComparison.getPostCount();
            int postVersionCount = aggregatedMetricComparison.getPostVersionCount();
            int textBlockVersionCount = aggregatedMetricComparison.getAggregatedResultsText().getPostBlockVersionCount();
            int codeBlockVersionCount = aggregatedMetricComparison.getAggregatedResultsCode().getPostBlockVersionCount();
            int textPossibleConnections = aggregatedMetricComparison.getAggregatedResultsText().getPossibleConnections();
            int codePossibleConnections = aggregatedMetricComparison.getAggregatedResultsCode().getPossibleConnections();

            if (postCount != postCountFirst || postVersionCount != postVersionCountFirst
                    || textBlockVersionCount != textBlockVersionCountFirst || codeBlockVersionCount != codeBlockVersionCountFirst
                    || textPossibleConnections != textPossibleConnectionsFirst || codePossibleConnections != codePossibleConnectionsFirst) {
                throw new IllegalStateException("Values differ between aggregated metric comparisons.");
            }

            aggregatedMetricComparison.writeToCSV(csvPrinterAggregated, getMaxFailuresText(), getMaxFailuresCode());
        }
    }
}
