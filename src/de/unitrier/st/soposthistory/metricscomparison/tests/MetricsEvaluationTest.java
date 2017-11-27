package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.MetricEvaluationManager;
import de.unitrier.st.soposthistory.metricscomparison.MetricEvaluationPerPost;
import de.unitrier.st.soposthistory.metricscomparison.MetricResult;
import de.unitrier.st.soposthistory.metricscomparison.SimilarityMetric;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsEvaluationTest {
    static Path pathToPostIdList = Paths.get("testdata", "gt_test", "post_ids.csv");
    static Path pathToPostHistory = Paths.get("testdata", "gt_test", "files");
    static Path pathToGroundTruth = Paths.get("testdata", "gt_test", "gt");
    static Path outputDir = Paths.get("testdata", "metrics_comparison");

    @Test
    void testMetricEvaluationManager() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestSample")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withOutputDirPath(outputDir)
                .withAddDefaultMetricsAndThresholds(false)
                .initialize();

        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));

        manager.addSimilarityMetric(
                new SimilarityMetric(
                "fourGramOverlap",
                de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap,
                SimilarityMetric.MetricType.SET,
                0.6
            )
        );

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();

            List<Integer> postHistoryIds_3758880 = manager.getPostGroundTruths().get(3758880).getPostHistoryIds();
            MetricEvaluationPerPost evaluation_a_3758880 = manager.getMetricEvaluation(3758880, "fourGramOverlap", 0.6);

            /* compare a 3758880 */
            // first version has never predecessors
            int postHistoryId = postHistoryIds_3758880.get(0);

            MetricResult resultsText = evaluation_a_3758880.getResultsText(postHistoryId);
            assertEquals(0, resultsText.getTruePositives());
            assertEquals(0, resultsText.getFalsePositives());
            assertEquals(0, resultsText.getTrueNegatives());
            assertEquals(0, resultsText.getFalseNegatives());

            MetricResult resultsCode = evaluation_a_3758880.getResultsCode(postHistoryId);
            assertEquals(0, resultsCode.getTruePositives());
            assertEquals(0, resultsCode.getFalsePositives());
            assertEquals(0, resultsCode.getTrueNegatives());
            assertEquals(0, resultsCode.getFalseNegatives());

            // second version
            postHistoryId = postHistoryIds_3758880.get(1);

            resultsText = evaluation_a_3758880.getResultsText(postHistoryId);
            assertEquals(1, resultsText.getTruePositives());
            assertEquals(0, resultsText.getFalsePositives());
            assertEquals(5, resultsText.getTrueNegatives());
            assertEquals(0, resultsText.getFalseNegatives());

            resultsCode = evaluation_a_3758880.getResultsCode(postHistoryId);
            assertEquals(2, resultsCode.getTruePositives());
            assertEquals(0, resultsCode.getFalsePositives());
            assertEquals(4, resultsCode.getTrueNegatives());
            assertEquals(0, resultsCode.getFalseNegatives());

            // version 3 to 10 only for text blocks (they don't differ)
            for (int i = 2; i < 10; i++) {
                postHistoryId = postHistoryIds_3758880.get(i);

                resultsText = evaluation_a_3758880.getResultsText(postHistoryId);
                assertEquals(2, resultsText.getTruePositives());
                assertEquals(0, resultsText.getFalsePositives());
                assertEquals(2, resultsText.getTrueNegatives());
                assertEquals(0, resultsText.getFalseNegatives());
            }

            postHistoryId = postHistoryIds_3758880.get(10);
            resultsText = evaluation_a_3758880.getResultsText(postHistoryId);
            assertEquals(2, resultsText.getTruePositives());
            assertEquals(0, resultsText.getFalsePositives());
            assertEquals(4, resultsText.getTrueNegatives());
            assertEquals(0, resultsText.getFalseNegatives());

            // version 3 and 6 for code
            List<Integer> versions = Arrays.asList(2, 5);
            for (Integer version_number : versions) {
                postHistoryId = postHistoryIds_3758880.get(version_number);

                resultsCode = evaluation_a_3758880.getResultsCode(postHistoryId);
                assertEquals(1, resultsCode.getTruePositives());
                assertEquals(0, resultsCode.getFalsePositives());
                assertEquals(2, resultsCode.getTrueNegatives());
                assertEquals(1, resultsCode.getFalseNegatives());
            }

            // version 4,5,7,8,9,10,11 for code
            versions = Arrays.asList(3, 4, 6, 7, 8, 9, 10);
            for (Integer version_number : versions) {
                postHistoryId = postHistoryIds_3758880.get(version_number);

                resultsCode = evaluation_a_3758880.getResultsCode(postHistoryId);
                assertEquals(2, resultsCode.getTruePositives());
                assertEquals(0, resultsCode.getFalsePositives());
                assertEquals(2, resultsCode.getTrueNegatives());
                assertEquals(0, resultsCode.getFalseNegatives());
            }

            /* compare a 22037280 */
            List<Integer> postHistoryIds_22037280 = manager.getPostGroundTruths().get(22037280).getPostHistoryIds();
            MetricEvaluationPerPost evaluation_a_22037280 = manager.getMetricEvaluation(22037280, "fourGramOverlap", 0.6);

            postHistoryId = postHistoryIds_22037280.get(0);

            resultsText = evaluation_a_22037280.getResultsText(postHistoryId);
            assertEquals(0, resultsText.getTruePositives());
            assertEquals(0, resultsText.getFalsePositives());
            assertEquals(0, resultsText.getTrueNegatives());
            assertEquals(0, resultsText.getFalseNegatives());

            resultsCode = evaluation_a_22037280.getResultsCode(postHistoryId);
            assertEquals(0, resultsCode.getTruePositives());
            assertEquals(0, resultsCode.getFalsePositives());
            assertEquals(0, resultsCode.getTrueNegatives());
            assertEquals(0, resultsCode.getFalseNegatives());

            for (int i = 1; i < postHistoryIds_22037280.size(); i++) {
                postHistoryId = postHistoryIds_22037280.get(i);

                resultsText = evaluation_a_22037280.getResultsText(postHistoryId);
                assertEquals(3, resultsText.getTruePositives());
                assertEquals(0, resultsText.getFalsePositives());
                assertEquals(6, resultsText.getTrueNegatives());
                assertEquals(0, resultsText.getFalseNegatives());

                resultsCode = evaluation_a_22037280.getResultsCode(postHistoryId);
                assertEquals(2, resultsCode.getTruePositives());
                assertEquals(0, resultsCode.getFalsePositives());
                assertEquals(2, resultsCode.getTrueNegatives());
                assertEquals(0, resultsCode.getFalseNegatives());
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
