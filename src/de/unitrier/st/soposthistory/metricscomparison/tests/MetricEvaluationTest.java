package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.Config;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.metricscomparison.evaluation.MetricEvaluationManager;
import de.unitrier.st.soposthistory.metricscomparison.evaluation.MetricEvaluationPerPost;
import de.unitrier.st.soposthistory.metricscomparison.evaluation.MetricResult;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricEvaluationTest {
    private static Logger logger = null;

    static Path pathToPostIdList = Paths.get("testdata", "gt_test", "post_ids.csv");
    static Path pathToPostHistory = Paths.get("testdata", "gt_test", "files");
    static Path pathToGroundTruth = Paths.get("testdata", "gt_test", "gt");
    static Path testOutputDir = Paths.get("testdata", "output");
    private static Path pathToSamplesComparisonTestDir = Paths.get("testdata", "samples_comparison_test");

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(MetricEvaluationTest.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMetricEvaluationManager() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestMetricEvaluationManager")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withOutputDirPath(testOutputDir)
                .withDefaultSimilarityMetrics(false)
                .initialize();

        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));

        manager.addSimilarityMetric(
                MetricEvaluationManager.getDefaultSimilarityMetric("fourGramOverlap", 0.6)
        );

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();
            assertTrue(manager.isFinished()); // assert that execution of manager successfully finished

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

    @Test
    void testAggregatedResultsManagers() {
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        List<MetricEvaluationManager> managers = MetricEvaluationManager.createManagersFromSampleDirectories(
                pathToSamplesComparisonTestDir, testOutputDir, false
        );
        for (MetricEvaluationManager manager : managers) {
            manager.addSimilarityMetric(
                    MetricEvaluationManager.getDefaultSimilarityMetric("winnowingTwoGramOverlap", 0.3)
            );
            manager.addSimilarityMetric(
                    MetricEvaluationManager.getDefaultSimilarityMetric("tokenJaccard", 0.6)
            );
            manager.addSimilarityMetric(
                    MetricEvaluationManager.getDefaultSimilarityMetric("twoGramJaccard", 0.9)
            );
            // the following metric should produce failed comparisons
            manager.addSimilarityMetric(
                    MetricEvaluationManager.getDefaultSimilarityMetric("twoShingleOverlap", 0.6)
            );

            threadPool.execute(new Thread(manager));
        }

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, TimeUnit.DAYS);

            for (MetricEvaluationManager manager : managers) {
                assertTrue(manager.isFinished()); // assert that execution of manager successfully finished
            }

            // output file aggregated over all samples
            File outputFileAggregated= Paths.get(testOutputDir.toString(), "MetricComparison_aggregated.csv").toFile();
            if (outputFileAggregated.exists()) {
                if (!outputFileAggregated.delete()) {
                    throw new IllegalStateException("Error while deleting output file: " + outputFileAggregated);
                }
            }

            MetricEvaluationManager.aggregateAndWriteSampleResults(managers, outputFileAggregated);
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            e.printStackTrace();
        }
    }

    @Test
    void testFailedPredecessorComparisonsText() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestFailedPredecessorComparisonsText")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withOutputDirPath(testOutputDir)
                .withDefaultSimilarityMetrics(false)
                .initialize();

        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));

        manager.addSimilarityMetric(
                MetricEvaluationManager.getDefaultSimilarityMetric("threeShingleOverlap", 0.6)
        );

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();
            assertTrue(manager.isFinished()); // assert that execution of manager successfully finished

            List<Integer> postHistoryIds_3758880 = manager.getPostGroundTruths().get(3758880).getPostHistoryIds();
            MetricEvaluationPerPost evaluation_a_3758880 = manager.getMetricEvaluation(3758880, "threeShingleOverlap", 0.6);

            int postHistoryId_version2 = postHistoryIds_3758880.get(1);

            MetricResult resultsText = evaluation_a_3758880.getResultsText(postHistoryId_version2);
            assertEquals(1, resultsText.getTruePositives());
            assertEquals(0, resultsText.getFalsePositives());
            assertEquals(5, resultsText.getTrueNegatives());
            assertEquals(0, resultsText.getFalseNegatives());
            assertEquals(4, resultsText.getFailedPredecessorComparisons());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testFailedPredecessorComparisonsCode() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestFailedPredecessorComparisonsCode")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withOutputDirPath(testOutputDir)
                .withDefaultSimilarityMetrics(false)
                .initialize();

        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));

        manager.addSimilarityMetric(
                MetricEvaluationManager.getDefaultSimilarityMetric("threeShingleOverlap", 0.6)
        );

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();
            assertTrue(manager.isFinished()); // assert that execution of manager successfully finished

            List<Integer> postHistoryIds_2096370 = manager.getPostGroundTruths().get(2096370).getPostHistoryIds();
            MetricEvaluationPerPost evaluation_a_2096370 = manager.getMetricEvaluation(2096370, "threeShingleOverlap", 0.6);

            int postHistoryId_version2 = postHistoryIds_2096370.get(1);

            MetricResult resultsCode = evaluation_a_2096370.getResultsCode(postHistoryId_version2);
            assertEquals(1, resultsCode.getTruePositives());
            assertEquals(0, resultsCode.getFalsePositives());
            assertEquals(2, resultsCode.getTrueNegatives());
            assertEquals(0, resultsCode.getFalseNegatives());
            assertEquals(1, resultsCode.getFailedPredecessorComparisons());


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void validationTest() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("ValidationTestSample")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withValidate(false)
                .initialize();
        assertTrue(manager.validate());
    }

    @Test
    void equalsTestQuestion10381975() {
        int postId = 10381975;
        PostVersionList q_10381975 = PostVersionList.readFromCSV(pathToPostHistory, postId, 1, false);
        q_10381975.processVersionHistory(Config.DEFAULT
                .withTextSimilarityMetric(de.unitrier.st.stringsimilarity.equal.Variants::equal)
                .withTextBackupSimilarityMetric(null)
                .withTextSimilarityThreshold(1.0)
                .withCodeSimilarityMetric(de.unitrier.st.stringsimilarity.equal.Variants::equal)
                .withCodeBackupSimilarityMetric(null)
                .withCodeSimilarityThreshold(1.0)
        );
        PostGroundTruth q_10381975_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        // check if the post version list does not contain more connections than the ground truth (which should not
        // happen when using equality-based metrics)
        validateEqualMetricConnections(q_10381975, q_10381975_gt);

        // check if manager produces false positives or failed comparisons
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("EqualTestSample")
                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                .withDefaultSimilarityMetrics(false)
                .initialize();

        manager.addSimilarityMetric(
                MetricEvaluationManager.getDefaultSimilarityMetric("equal", 1.0)
        );

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();
            assertTrue(manager.isFinished()); // assert that execution of manager successfully finished
            // assert that equality-based metric did not produce false positives or failed comparisons
            validateEqualMetricResults(manager, postId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void validateEqualMetricConnections(PostVersionList postVersionList, PostGroundTruth postGroundTruth) {
        // check if the post version list does not contain more connections than the ground truth (which should not
        // happen when using equality-based metrics)

        // text
        Set<PostBlockConnection> connectionsList = postVersionList.getConnections(TextBlockVersion.getPostBlockTypeIdFilter());
        Set<PostBlockConnection> connectionsGT = postGroundTruth.getConnections(TextBlockVersion.getPostBlockTypeIdFilter());
        assertTrue(PostBlockConnection.difference(connectionsList, connectionsGT).size() == 0);

        // code
        connectionsList = postVersionList.getConnections(CodeBlockVersion.getPostBlockTypeIdFilter());
        connectionsGT = postGroundTruth.getConnections(CodeBlockVersion.getPostBlockTypeIdFilter());
        assertTrue(PostBlockConnection.difference(connectionsList, connectionsGT).size() == 0);
    }

    static void validateEqualMetricResults(MetricEvaluationManager manager, int postId) {
        // assert that equality-based metric did not produce false positives or failed comparisons
        MetricEvaluationPerPost evaluation = manager.getMetricEvaluation(postId, "equal", 1.0);
        for (int postHistoryId : evaluation.getPostHistoryIds()) {
            MetricResult resultsCode = evaluation.getResultsCode(postHistoryId);
            if (resultsCode.getFalsePositives() > 0) {
                logger.warning("False positives for postId " + postId + ", postHistoryId " + postHistoryId);
            }
            assertEquals(0, resultsCode.getFalsePositives());
            assertEquals(0, resultsCode.getFailedPredecessorComparisons());

            MetricResult resultsText = evaluation.getResultsText(postHistoryId);
            if (resultsText.getFalsePositives() > 0) {
                logger.warning("False positives for postId " + postId + ", postHistoryId " + postHistoryId);
            }
            assertEquals(0, resultsText.getFalsePositives());
            assertEquals(0, resultsText.getFailedPredecessorComparisons());
        }
    }
}
