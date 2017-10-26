package de.unitrier.st.soposthistory.metricscomparison.metricsComparison;

import de.unitrier.st.soposthistory.metricscomparison.util.ConnectedBlocks;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MetricResult {

    StopWatch stopWatch = new StopWatch();
    public ConnectionsOfAllVersions connectionsOfAllVersions_text;
    public ConnectionsOfAllVersions connectionsOfAllVersions_code;

    long totalTimeMeasured_text = 0;
    long totalTimeMeasured_code = 0;

    List<MeasuredData> measuredDataList = new ArrayList<>();


    public MetricResult() {
    }

    public void updateMetricResult_text() {
        this.totalTimeMeasured_text += this.stopWatch.getTime();
    }

    public void updateMetricResult_code() {
        this.totalTimeMeasured_code += this.stopWatch.getTime();
    }


    void compareMetricResults_text(ConnectionsOfAllVersions groundTruth) {

        for (int i = 0; i < groundTruth.size(); i++) {

            MeasuredData measuredData = new MeasuredData();

            for (int j = 0; j < groundTruth.get(i).size(); j++) {
                ConnectedBlocks connectedBlocksGroundTruth = groundTruth.get(i).get(j);
                ConnectedBlocks connectedBlocksComputedMetric = connectionsOfAllVersions_text.get(i).get(j);

                if (Objects.equals(connectedBlocksGroundTruth.getLeftLocalId(), connectedBlocksComputedMetric.getLeftLocalId())) {
                    if (connectedBlocksGroundTruth.getLeftLocalId() != null) {
                        measuredData.truePositives_text++;
                    } else {
                        measuredData.trueNegatives_text++;
                    }
                } else {
                    if (connectedBlocksGroundTruth.getLeftLocalId() == null) {
                        measuredData.falsePositives_text++;
                    } else {
                        measuredData.falseNegatives_text++;
                    }
                }
            }

            measuredDataList.add(measuredData);
        }
    }


    void compareMetricResults_code(ConnectionsOfAllVersions groundTruth) {

        for (int i = 0; i < groundTruth.size(); i++) {

            MeasuredData measuredData = new MeasuredData();

            for (int j = 0; j < groundTruth.get(i).size(); j++) {
                ConnectedBlocks connectedBlocksGroundTruth = groundTruth.get(i).get(j);
                ConnectedBlocks connectedBlocksComputedMetric = connectionsOfAllVersions_code.get(i).get(j);

                if (Objects.equals(connectedBlocksGroundTruth.getLeftLocalId(), connectedBlocksComputedMetric.getLeftLocalId())) {
                    if (connectedBlocksGroundTruth.getLeftLocalId() != null) {
                        measuredData.truePositives_code++;
                    } else {
                        measuredData.trueNegatives_code++;
                    }
                } else {
                    if (connectedBlocksGroundTruth.getLeftLocalId() == null) {
                        measuredData.falsePositives_code++;
                    } else {
                        measuredData.falseNegatives_code++;
                    }
                }
            }

            measuredDataList.add(measuredData);
        }
    }
}
