package de.unitrier.st.soposthistory.metricscomparison.samplesConvertor;

import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {

        MetricThresholdAggregatedList metricThresholdAggregatedList = new MetricThresholdAggregatedList();

        metricThresholdAggregatedList.convertAggregatedSampleToMetricThresholdAggregated(
                Paths.get("output", "PostId_VersionCount_SO_17-06_sample_100_aggregated.csv"),
                Paths.get("output"),
                true);
    }
}
