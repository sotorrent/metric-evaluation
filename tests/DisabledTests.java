import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sotorrent.metricevaluation.evaluation.MetricEvaluationManager;
import org.sotorrent.posthistoryextractor.history.PostHistory;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.version.PostVersionList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DisabledTests {

    @Test
    void testMetricEvaluationManagerTestData() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestData")
                .withInputPaths(MetricEvaluationTest.pathToPostIdList, MetricEvaluationTest.pathToPostHistory,
                        MetricEvaluationTest.pathToGroundTruth)
                .withOutputDirPath(MetricEvaluationTest.testOutputDir)
                .withNumberOfRepetitions(1)
                .initialize();

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();
            assertTrue(manager.isFinished()); // assert that execution of manager successfully finished
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testComparisonSamplesParsable() {
        try (Stream<Path> paths = Files.list(MetricEvaluationTest.pathToComparisonSamplesDir)) {
            paths.forEach(
                    path -> {
                        String sampleName = path.getFileName().toString();
                        Path currentSampleFiles = Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "files");

                        File[] postHistoryFiles = currentSampleFiles.toFile().listFiles(
                                (dir, name) -> name.matches(PostHistory.fileNamePattern.pattern())
                        );

                        assertNotNull(postHistoryFiles);

                        for (File postHistoryFile : postHistoryFiles) {
                            Matcher fileNameMatcher = PostHistory.fileNamePattern.matcher(postHistoryFile.getName());
                            if (fileNameMatcher.find()) {
                                int postId = Integer.parseInt(fileNameMatcher.group(1));
                                // no exception should be thrown for the following two lines
                                PostVersionList postVersionList = PostVersionList.readFromCSV(currentSampleFiles, postId, Posts.UNKNOWN_ID);
                                postVersionList.normalizeLinks();
                                assertTrue(postVersionList.size() > 0);
                            }
                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MultipleConnectionsResultOld {
        int postId;
        int postHistoryId;
        int localId;
        int postBlockTypeId;
        String possiblePredOrSuccLocalIds;
        int numberOfPossibleSuccessorsOrPredecessors;

        MultipleConnectionsResultOld(int postId, int postHistoryId, int localId, int postBlockTypeId,
                                     String possiblePredOrSuccLocalIds,
                                     int numberOfPossibleSuccessorsOrPredecessors) {
            this.postId = postId;
            this.postHistoryId = postHistoryId;
            this.localId = localId;
            this.postBlockTypeId = postBlockTypeId;
            this.possiblePredOrSuccLocalIds = possiblePredOrSuccLocalIds;
            this.numberOfPossibleSuccessorsOrPredecessors = numberOfPossibleSuccessorsOrPredecessors;
        }
    }

    private class MultipleConnectionsResultNew {
        int postId;
        int postHistoryId;
        int localId;
        int postBlockTypeId;
        int possiblePredecessorsCount;
        int possibleSuccessorsCount;
        String possiblePredecessorLocalIds;
        String possibleSuccessorLocalIds;

        MultipleConnectionsResultNew(int postId, int postHistoryId, int localId, int postBlockTypeId,
                                     int possiblePredecessorsCount, int possibleSuccessorsCount,
                                     String possiblePredecessorLocalIds, String possibleSuccessorLocalIds) {
            this.postId = postId;
            this.postHistoryId = postHistoryId;
            this.localId = localId;
            this.postBlockTypeId = postBlockTypeId;
            this.possiblePredecessorsCount = possiblePredecessorsCount;
            this.possibleSuccessorsCount = possibleSuccessorsCount;
            this.possiblePredecessorLocalIds = possiblePredecessorLocalIds;
            this.possibleSuccessorLocalIds = possibleSuccessorLocalIds;
        }
    }

    private void joinAndValidateEqualMetricResults(MetricEvaluationManager manager, Thread managerThread) throws InterruptedException {
        managerThread.join();
        assertTrue(manager.isFinished()); // assert that execution of manager successfully finished
        for (int postId : manager.getPostIds()) {
            // assert that equality-based metric did not produce false positives or failed comparisons
            MetricEvaluationTest.validateEqualMetricResults(manager, postId);
        }
    }

    @Test
    void testMetricEvaluationManagerValidationWithComparisonSamples() {
        try (Stream<Path> paths = Files.list(MetricEvaluationTest.pathToComparisonSamplesDir)) {
            paths.forEach(
                    path -> {
                        String sampleName = path.getFileName().toString();
                        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                                .withName("TestComparisonSamples")
                                .withInputPaths(
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, sampleName + ".csv"),
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "files"),
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "completed"))
                                .withOutputDirPath(MetricEvaluationTest.testOutputDir)
                                .withAllSimilarityMetrics(false)
                                .withNumberOfRepetitions(1)
                                .withValidate(false)
                                .initialize();

                        assertTrue(manager.validate());
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMetricEvaluationManagerInitializationWithComparisonSamples() {
        try (Stream<Path> paths = Files.list(MetricEvaluationTest.pathToComparisonSamplesDir)) {
            paths.forEach(
                    path -> {
                        String sampleName = path.getFileName().toString();
                        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                                .withName("TestComparisonSamples")
                                .withInputPaths(
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, sampleName + ".csv"),
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "files"),
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "completed"))
                                .withOutputDirPath(MetricEvaluationTest.testOutputDir)
                                .withAllSimilarityMetrics(false)
                                .withNumberOfRepetitions(1)
                                .initialize();

                        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
                        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMetricEvaluationManagerWithEqualityMetrics() {
        try (Stream<Path> paths = Files.list(MetricEvaluationTest.pathToComparisonSamplesDir)) {
            paths.forEach(
                    path -> {
                        String sampleName = path.getFileName().toString();
                        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                                .withName("TestComparisonSamplesEqual")
                                .withInputPaths(
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, sampleName + ".csv"),
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "files"),
                                        Paths.get(MetricEvaluationTest.pathToComparisonSamplesDir.toString(), sampleName, "completed"))
                                .withOutputDirPath(MetricEvaluationTest.testOutputDir)
                                .withAllSimilarityMetrics(false)
                                .initialize();

                        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
                        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));

                        List<Double> similarityThresholds = Arrays.asList(0.0, 0.5, 1.0);

                        for (double threshold : similarityThresholds) {
                            manager.addSimilarityMetric(
                                    MetricEvaluationManager.getSimilarityMetric("equal", threshold)
                            );
                            manager.addSimilarityMetric(
                                    MetricEvaluationManager.getSimilarityMetric("equalNormalized", threshold)
                            );
                            manager.addSimilarityMetric(
                                    MetricEvaluationManager.getSimilarityMetric("tokenEqual", threshold)
                            );
                            manager.addSimilarityMetric(
                                    MetricEvaluationManager.getSimilarityMetric("tokenEqualNormalized", threshold)
                            );
                        }

                        Thread managerThread = new Thread(manager);
                        managerThread.start();
                        try {
                            joinAndValidateEqualMetricResults(manager, managerThread);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
