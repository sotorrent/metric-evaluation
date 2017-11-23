package de.unitrier.st.soposthistory.metricscomparison.samplesConvertor;

import java.util.Objects;

class MetricThresholdAggregated {

    String sample;
    String metricType;
    String metric;
    double threshold;

    int postVersionCount;


    long runtimeText;

    int textBlockVersionCount;
    int possibleConnectionsText;

    int truePositivesText;
    int trueNegativesText;
    int falsePositivesText;
    int falseNegativesText;

    int failedPredecessorComparisonsText;


    long runtimeCode;

    int codeBlockVersionCount;
    int possibleConnectionsCode;

    int truePositivesCode;
    int trueNegativesCode;
    int falsePositivesCode;
    int falseNegativesCode;

    int failedPredecessorComparisonsCode;



    MetricThresholdAggregated(
            String sample, String metricType, String metric, double threshold,
            int postVersionCount,

            long runtimeText,
            int textBlockVersionCount, int possibleConnectionsText,
            int truePositivesText, int trueNegativesText, int falsePositivesText, int falseNegativesText,
            int failedPredecessorComparisonsText,

            long runtimeCode,
            int codeBlockVersionCount, int possibleConnectionsCode,
            int truePositivesCode, int trueNegativesCode, int falsePositivesCode, int falseNegativesCode,
            int failedPredecessorComparisonsCode){


        this.sample = sample;
        this.metricType = metricType;
        this.metric = metric;
        this.threshold = threshold;

        this.postVersionCount = postVersionCount;


        this.runtimeText = runtimeText;

        this.textBlockVersionCount = textBlockVersionCount;
        this.possibleConnectionsText = possibleConnectionsText;

        this.truePositivesText = truePositivesText;
        this.trueNegativesText = trueNegativesText;
        this.falsePositivesText = falsePositivesText;
        this.falseNegativesText = falseNegativesText;

        this.failedPredecessorComparisonsText = failedPredecessorComparisonsText;


        this.runtimeCode = runtimeCode;

        this.codeBlockVersionCount = codeBlockVersionCount;
        this.possibleConnectionsCode = possibleConnectionsCode;

        this.truePositivesCode = truePositivesCode;
        this.trueNegativesCode = trueNegativesCode;
        this.falsePositivesCode = falsePositivesCode;
        this.falseNegativesCode = falseNegativesCode;

        this.failedPredecessorComparisonsCode = failedPredecessorComparisonsCode;
    }


    boolean definesSameType(Object other, boolean divideBySamples) {
        return other instanceof MetricThresholdAggregated
                && (!divideBySamples
                || (Objects.equals(this.sample, ((MetricThresholdAggregated) other).sample)))
                && (Objects.equals(this.metric, ((MetricThresholdAggregated) other).metric))
                && this.threshold == ((MetricThresholdAggregated) other).threshold;
    }

}