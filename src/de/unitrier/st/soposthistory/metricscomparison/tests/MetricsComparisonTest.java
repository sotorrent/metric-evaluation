package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.MetricComparison;
import de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager;
import de.unitrier.st.soposthistory.metricscomparison.MetricResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsComparisonTest {
    static Path pathToPostIdList = Paths.get("testdata", "gt_test", "post_ids.csv");
    static Path pathToPostHistory = Paths.get("testdata", "gt_test", "files");
    static Path pathToGroundTruth = Paths.get("testdata", "gt_test", "gt");
    static Path outputDir = Paths.get("testdata", "metrics_comparison");

    @Test
    void testMetricComparisonManager() {
        MetricComparisonManager manager = MetricComparisonManager.DEFAULT
                .withName("TestManager")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withOutputDirPath(outputDir)
                .withAddDefaultMetricsAndThresholds(false)
                .initialize();

        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruth().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruth().keySet()));

        manager.addSimilarityMetric(
                "fourGramOverlap",
                MetricComparison.MetricType.SET,
                de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap
        );
        manager.addSimilarityThreshold(0.6);

        manager.compareMetrics();

        List<Integer> postHistoryIds_3758880 = manager.getPostGroundTruth().get(3758880).getPostHistoryIds();
        MetricComparison comparison_a_3758880 = manager.getMetricComparison(3758880, "fourGramOverlap", 0.6);

        /* compare a 3758880 */
        // first version has never predecessors
        int postHistoryId = postHistoryIds_3758880.get(0);

        MetricResult resultsText = comparison_a_3758880.getResultText(postHistoryId);
        assertEquals(new Integer(0), resultsText.getTruePositives());
        assertEquals(new Integer(0), resultsText.getFalsePositives());
        assertEquals(new Integer(0), resultsText.getTrueNegatives());
        assertEquals(new Integer(0), resultsText.getFalseNegatives());

        MetricResult resultsCode = comparison_a_3758880.getResultCode(postHistoryId);
        assertEquals(new Integer(0), resultsCode.getTruePositives());
        assertEquals(new Integer(0), resultsCode.getFalsePositives());
        assertEquals(new Integer(0), resultsCode.getTrueNegatives());
        assertEquals(new Integer(0), resultsCode.getFalseNegatives());

        // second version
        postHistoryId = postHistoryIds_3758880.get(1);

        resultsText = comparison_a_3758880.getResultText(postHistoryId);
        assertEquals(new Integer(1), resultsText.getTruePositives());
        assertEquals(new Integer(0), resultsText.getFalsePositives());
        assertEquals(new Integer(5), resultsText.getTrueNegatives());
        assertEquals(new Integer(0), resultsText.getFalseNegatives());

        resultsCode = comparison_a_3758880.getResultCode(postHistoryId);
        assertEquals(new Integer(2), resultsCode.getTruePositives());
        assertEquals(new Integer(0), resultsCode.getFalsePositives());
        assertEquals(new Integer(4), resultsCode.getTrueNegatives());
        assertEquals(new Integer(0), resultsCode.getFalseNegatives());

        // version 3 to 10 only for text blocks (they don't differ)
        for (int i = 2; i < 10; i++) {
            postHistoryId = postHistoryIds_3758880.get(i);

            resultsText = comparison_a_3758880.getResultText(postHistoryId);
            assertEquals(new Integer(2), resultsText.getTruePositives());
            assertEquals(new Integer(0), resultsText.getFalsePositives());
            assertEquals(new Integer(2), resultsText.getTrueNegatives());
            assertEquals(new Integer(0), resultsText.getFalseNegatives());
        }

        postHistoryId = postHistoryIds_3758880.get(10);
        resultsText = comparison_a_3758880.getResultText(postHistoryId);
        assertEquals(new Integer(2), resultsText.getTruePositives());
        assertEquals(new Integer(0), resultsText.getFalsePositives());
        assertEquals(new Integer(4), resultsText.getTrueNegatives());
        assertEquals(new Integer(0), resultsText.getFalseNegatives());

        // version 3 and 6 for code
        List<Integer> versions = Arrays.asList(2, 5);
        for (Integer version_number : versions) {
            postHistoryId = postHistoryIds_3758880.get(version_number);

            resultsCode = comparison_a_3758880.getResultCode(postHistoryId);
            assertEquals(new Integer(1), resultsCode.getTruePositives());
            assertEquals(new Integer(0), resultsCode.getFalsePositives());
            assertEquals(new Integer(2), resultsCode.getTrueNegatives());
            assertEquals(new Integer(1), resultsCode.getFalseNegatives());
        }

        // version 4,5,7,8,9,10,11 for code
        versions = Arrays.asList(3, 4, 6, 7, 8, 9, 10);
        for (Integer version_number : versions) {
            postHistoryId = postHistoryIds_3758880.get(version_number);

            resultsCode = comparison_a_3758880.getResultCode(postHistoryId);
            assertEquals(new Integer(2), resultsCode.getTruePositives());
            assertEquals(new Integer(0), resultsCode.getFalsePositives());
            assertEquals(new Integer(2), resultsCode.getTrueNegatives());
            assertEquals(new Integer(0), resultsCode.getFalseNegatives());
        }

        /* compare a 22037280 */
        List<Integer> postHistoryIds_22037280 = manager.getPostGroundTruth().get(22037280).getPostHistoryIds();
        MetricComparison comparison_a_22037280 = manager.getMetricComparison(22037280, "fourGramOverlap", 0.6);

        postHistoryId = postHistoryIds_22037280.get(0);

        resultsText = comparison_a_22037280.getResultText(postHistoryId);
        assertEquals(new Integer(0), resultsText.getTruePositives());
        assertEquals(new Integer(0), resultsText.getFalsePositives());
        assertEquals(new Integer(0), resultsText.getTrueNegatives());
        assertEquals(new Integer(0), resultsText.getFalseNegatives());

        resultsCode = comparison_a_22037280.getResultCode(postHistoryId);
        assertEquals(new Integer(0), resultsCode.getTruePositives());
        assertEquals(new Integer(0), resultsCode.getFalsePositives());
        assertEquals(new Integer(0), resultsCode.getTrueNegatives());
        assertEquals(new Integer(0), resultsCode.getFalseNegatives());

        for (int i = 1; i < postHistoryIds_22037280.size(); i++) {
            postHistoryId = postHistoryIds_22037280.get(i);

            resultsText = comparison_a_22037280.getResultText(postHistoryId);
            assertEquals(new Integer(3), resultsText.getTruePositives());
            assertEquals(new Integer(0), resultsText.getFalsePositives());
            assertEquals(new Integer(6), resultsText.getTrueNegatives());
            assertEquals(new Integer(0), resultsText.getFalseNegatives());

            resultsCode = comparison_a_22037280.getResultCode(postHistoryId);
            assertEquals(new Integer(2), resultsCode.getTruePositives());
            assertEquals(new Integer(0), resultsCode.getFalsePositives());
            assertEquals(new Integer(2), resultsCode.getTrueNegatives());
            assertEquals(new Integer(0), resultsCode.getFalseNegatives());
        }

        manager.writeToCSV();
    }
}
