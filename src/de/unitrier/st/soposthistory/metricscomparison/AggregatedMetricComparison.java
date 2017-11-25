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

    private int postCount;
    private int postVersionCount;

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
        this.postCount = 1;
        this.postVersionCount = postVersionList.size();
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

        postCount += 1;
        postVersionCount += metricComparison.getPostVersionList().size();
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

    public int getPostCount() {
        return postCount;
    }

    int getPostVersionCount() {
        return postVersionCount;
    }

    private double precision(int truePositives, int falsePositives) {
        return ((double) truePositives) / (truePositives + falsePositives);
    }

    private double recall(int truePositives, int falseNegatives) {
        return ((double) truePositives) / (truePositives + falseNegatives);
    }

    private double sensitivity(int truePositives, int falseNegatives) {
        return ((double) truePositives) / (truePositives + falseNegatives);
    }

    private double youdensJ(double sensitivity, double specificity) {
        // https://en.wikipedia.org/wiki/Youden%27s_J_statistic
        return sensitivity + specificity - 1;
    }

    private double specificity(int trueNegatives, int falsePositives) {
        return ((double) trueNegatives) / (trueNegatives + falsePositives);
    }

    private double failureRate(int failures, int maxFailures) {
        return maxFailures == 0 ? 0.0 : ((double) failures) / maxFailures;
    }

    void writeToCSV(CSVPrinter csvPrinterAggregated, int maxFailuresText, int maxFailuresCode) throws IOException {
        // "MetricType", "Metric", "Threshold",
        // "YoudensJText", "RuntimeText", "YoudensJCode", "RuntimeCode",
        // "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",
        // "TextBlockVersionCount", "PossibleConnectionsText",
        // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText",
        // "PrecisionText", "RecallText", "SensitivityText", "SpecificityText", "FailureRateText",
        // "CodeBlockVersionCount", "PossibleConnectionsCode",
        // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode",
        // "PrecisionCode", "RecallCode", "SensitivityCode", "SpecificityCode", "FailureRateCode"
        csvPrinterAggregated.printRecord(
                getSimilarityMetricType(),
                getSimilarityMetricName(),
                getSimilarityThreshold(),

                youdensJ(sensitivity(aggregatedResultsText.getTruePositives(), aggregatedResultsText.getFalseNegatives()),
                        specificity(aggregatedResultsText.getTrueNegatives(), aggregatedResultsText.getFalsePositives())),
                aggregatedRuntimeText.getTotalRuntime(),
                youdensJ(sensitivity(aggregatedResultsCode.getTruePositives(), aggregatedResultsCode.getFalseNegatives()),
                        specificity(aggregatedResultsCode.getTrueNegatives(), aggregatedResultsCode.getFalsePositives())),
                aggregatedRuntimeCode.getTotalRuntime(),

                getPostCount(),
                getPostVersionCount(),
                aggregatedResultsText.getPostBlockVersionCount() + aggregatedResultsCode.getPostBlockVersionCount(),
                aggregatedResultsText.getPossibleConnections() + aggregatedResultsCode.getPossibleConnections(),

                aggregatedResultsText.getPostBlockVersionCount(),
                aggregatedResultsText.getPossibleConnections(),

                aggregatedResultsText.getTruePositives(),
                aggregatedResultsText.getTrueNegatives(),
                aggregatedResultsText.getFalsePositives(),
                aggregatedResultsText.getFalseNegatives(),
                aggregatedResultsText.getFailedPredecessorComparisons(),

                precision(aggregatedResultsText.getTruePositives(), aggregatedResultsText.getFalsePositives()),
                recall(aggregatedResultsText.getTruePositives(), aggregatedResultsText.getFalseNegatives()),
                sensitivity(aggregatedResultsText.getTruePositives(), aggregatedResultsText.getFalseNegatives()),
                specificity(aggregatedResultsText.getTrueNegatives(), aggregatedResultsText.getFalsePositives()),
                failureRate(aggregatedResultsText.getFailedPredecessorComparisons(), maxFailuresText),

                aggregatedResultsCode.getPostBlockVersionCount(),
                aggregatedResultsCode.getPossibleConnections(),

                aggregatedResultsCode.getTruePositives(),
                aggregatedResultsCode.getTrueNegatives(),
                aggregatedResultsCode.getFalsePositives(),
                aggregatedResultsCode.getFalseNegatives(),
                aggregatedResultsCode.getFailedPredecessorComparisons(),

                precision(aggregatedResultsCode.getTruePositives(), aggregatedResultsCode.getFalsePositives()),
                recall(aggregatedResultsCode.getTruePositives(), aggregatedResultsCode.getFalseNegatives()),
                sensitivity(aggregatedResultsCode.getTruePositives(), aggregatedResultsCode.getFalseNegatives()),
                specificity(aggregatedResultsCode.getTrueNegatives(), aggregatedResultsCode.getFalsePositives()),
                failureRate(aggregatedResultsCode.getFailedPredecessorComparisons(), maxFailuresCode)
        );
    }
}
