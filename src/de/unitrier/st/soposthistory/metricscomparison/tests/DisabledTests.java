package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.*;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
class DisabledTests {
    private static Logger logger;

    private static Path pathToOldMetricComparisonResults = Paths.get(
            "testdata", "metrics_comparison", "results_metric_comparison_old.csv"
    );

    static {
        try {
            logger = getClassLogger(de.unitrier.st.soposthistory.metricscomparison.tests.DisabledTests.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCompareMetricEvaluationManagerWithOldProject() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestSample")
                .withInputPaths(MetricsEvaluationTest.pathToPostIdList, MetricsEvaluationTest.pathToPostHistory,
                        MetricsEvaluationTest.pathToGroundTruth)
                .withOutputDirPath(MetricsEvaluationTest.outputDir)
                .initialize();

        List<Integer> postHistoryIds_3758880 = manager.getPostVersionLists().get(3758880).getPostHistoryIds();
        List<Integer> postHistoryIds_22037280 = manager.getPostVersionLists().get(22037280).getPostHistoryIds();

        Set<String> excludedVariants = new HashSet<>();
        excludedVariants.add("Kondrak05");

        CSVParser csvParser;

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();

            csvParser = CSVParser.parse(
                    pathToOldMetricComparisonResults.toFile(),
                    StandardCharsets.UTF_8,
                    MetricEvaluationManager.csvFormatMetricEvaluationPerVersion.withFirstRecordAsHeader()
            );

            csvParser.getHeaderMap();
            List<CSVRecord> records = csvParser.getRecords();
            for (CSVRecord record : records) {
                String metric = record.get("metric");

                boolean skipRecord = false;
                for(String excludedVariant : excludedVariants) {
                    if (metric.contains(excludedVariant)) {
                        skipRecord = true;
                        break;
                    }
                }

                if(skipRecord) {
                    continue;
                }

                Double threshold = Double.valueOf(record.get("threshold"));
                // comparison manager computes only thresholds mod 0.10 by now so unequal thresholds will be skipped
                if ((int) (threshold * 100) % 10 != 0) {
                    continue;
                }

                Integer postId = Integer.valueOf(record.get("postid"));

                Integer postHistoryId = null;

                Integer truePositivesText = null;
                Integer trueNegativesText = null;
                Integer falsePositivesText = null;
                Integer falseNegativesText = null;

                Integer truePositivesCode = null;
                Integer trueNegativesCode = null;
                Integer falsePositivesCode = null;
                Integer falseNegativesCode = null;

                try {
                    postHistoryId = Integer.valueOf(record.get("posthistoryid"));

                    truePositivesText = Integer.valueOf(record.get("#truepositivestext"));
                    trueNegativesText = Integer.valueOf(record.get("#truenegativestext"));
                    falsePositivesText = Integer.valueOf(record.get("#falsepositivestext"));
                    falseNegativesText = Integer.valueOf(record.get("#falsenegativestext"));

                    truePositivesCode = Integer.valueOf(record.get("#truepositivescode"));
                    trueNegativesCode = Integer.valueOf(record.get("#truenegativescode"));
                    falsePositivesCode = Integer.valueOf(record.get("#falsepositivescode"));
                    falseNegativesCode = Integer.valueOf(record.get("#falsenegativescode"));
                } catch (NumberFormatException ignored) {
                }

                MetricEvaluationPerPost tmpMetricEvaluationPerPost = manager.getMetricEvaluation(postId, metric, threshold);

                if (postHistoryId == null) {
                    List<Integer> postHistoryIds;
                    if (postId == 3758880) {
                        postHistoryIds = postHistoryIds_3758880;
                    } else if (postId == 22037280) {
                        postHistoryIds = postHistoryIds_22037280;
                    } else {
                        throw new IllegalArgumentException("Post with id " + postId + " has not been listed in test set");
                    }

                    assertNotNull(postHistoryIds);
                    for (Integer tmpPostHistoryId : postHistoryIds) {
                        MetricResult resultsText = tmpMetricEvaluationPerPost.getResultsText(tmpPostHistoryId);
                        MetricResult resultsCode = tmpMetricEvaluationPerPost.getResultsCode(tmpPostHistoryId);

                        // in previous versions, the results were set to null in case one comparison failed
                        boolean resultsTextNull = resultsText.getFailedPredecessorComparisons() > 0;
                        boolean resultsCodeNull = resultsCode.getFailedPredecessorComparisons() > 0;

                        assertTrue(resultsTextNull || resultsCodeNull);
                    }
                } else {
                    MetricResult resultsText = tmpMetricEvaluationPerPost.getResultsText(postHistoryId);
                    assertEquals(truePositivesText, new Integer(resultsText.getTruePositives()));
                    assertEquals(trueNegativesText, new Integer(resultsText.getPostBlockVersionCount() - resultsText.getTruePositives() - resultsText.getFalsePositives() - resultsText.getFalseNegatives()));
                    assertEquals(trueNegativesText, new Integer(resultsText.getPostBlockVersionCount() - resultsText.getTruePositives() - resultsText.getFalseNegatives()));
                    assertEquals(falsePositivesText, new Integer(resultsText.getFalsePositives()));
                    assertEquals(falseNegativesText, new Integer(resultsText.getFalseNegatives()));

                    MetricResult resultsCode = tmpMetricEvaluationPerPost.getResultsCode(postHistoryId);
                    assertEquals(truePositivesCode, new Integer(resultsCode.getTruePositives()));
                    assertEquals(trueNegativesCode, new Integer(resultsCode.getPostBlockVersionCount() - resultsCode.getTruePositives() - resultsCode.getFalsePositives() - resultsCode.getFalseNegatives()));
                    assertEquals(trueNegativesCode, new Integer(resultsCode.getPostBlockVersionCount() - resultsCode.getTruePositives() - resultsCode.getFalseNegatives()));
                    assertEquals(falsePositivesCode, new Integer(resultsCode.getFalsePositives()));
                    assertEquals(falseNegativesCode, new Integer(resultsCode.getFalseNegatives()));
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGTSamplesParsable() {
        testSamples(Statistics.pathsToGTSamples);
    }

    @Test
    void testTestSamplesParsable() {
        testSamples(Statistics.pathsToTestSamples);
    }

    private void testSamples(List<Path> samplePaths) {
        for (Path currentSamplePath : samplePaths) {
            Path currentSampleFiles = Paths.get(currentSamplePath.toString(), "files");

            File[] postHistoryFiles = currentSampleFiles.toFile().listFiles(
                    (dir, name) -> name.matches(PostVersionList.fileNamePattern.pattern())
            );

            assertNotNull(postHistoryFiles);

            for (File postHistoryFile : postHistoryFiles) {
                Matcher fileNameMatcher = PostVersionList.fileNamePattern.matcher(postHistoryFile.getName());
                if (fileNameMatcher.find()) {
                    int postId = Integer.parseInt(fileNameMatcher.group(1));
                    // no exception should be thrown for the following two lines
                    PostVersionList postVersionList = PostVersionList.readFromCSV(currentSampleFiles, postId, -1);
                    postVersionList.normalizeLinks();
                    assertTrue(postVersionList.size() > 0);
                }
            }
        }
    }

    @Test
    void testComparePossibleMultipleConnectionsWithOldProject() {
        // This test case "fails" because the extraction of post blocks has been changed since the creation of the old file.

        File oldFile = Paths.get(Statistics.pathToMultipleConnectionsDir.toString(),
                "multiple_possible_connections_old.csv").toFile();
        File newFile = Statistics.pathToMultipleConnectionsFile.toFile();

        CSVParser csvParserOld, csvParserNew;
        try {
            // parse old records
            csvParserOld = CSVParser.parse(
                    oldFile,
                    StandardCharsets.UTF_8,
                    Statistics.csvFormatMultipleConnections.withFirstRecordAsHeader()
                            .withHeader("postId", "postHistoryId", "localId", "blockTypeId",
                                    "possiblePredOrSuccLocalIds", "numberOfPossibleSuccessorsOrPredecessors")
            );
            List<CSVRecord> oldRecords = csvParserOld.getRecords();
            List<MultipleConnectionsResultOld> oldResults = new ArrayList<>(oldRecords.size());

            for (CSVRecord record : oldRecords) {
                int postId = Integer.parseInt(record.get("postId"));
                int postHistoryId = Integer.parseInt(record.get("postHistoryId"));
                int localId = Integer.parseInt(record.get("localId"));
                int postBlockTypeId = Integer.parseInt(record.get("blockTypeId"));
                String possiblePredOrSuccLocalIds = record.get("possiblePredOrSuccLocalIds");
                int numberOfPossibleSuccessorsOrPredecessors = Integer.parseInt(record.get("numberOfPossibleSuccessorsOrPredecessors"));

                oldResults.add(new MultipleConnectionsResultOld(postId, postHistoryId, localId, postBlockTypeId,
                        possiblePredOrSuccLocalIds, numberOfPossibleSuccessorsOrPredecessors));
            }

            // parse new records
            csvParserNew = CSVParser.parse(
                    newFile,
                    StandardCharsets.UTF_8,
                    Statistics.csvFormatMultipleConnections.withFirstRecordAsHeader()
            );

            List<CSVRecord> newRecords = csvParserNew.getRecords();
            List<MultipleConnectionsResultNew> newResults = new ArrayList<>(newRecords.size());

            for (CSVRecord record : newRecords) {
                int postId = Integer.parseInt(record.get("PostId"));
                int postHistoryId = Integer.parseInt(record.get("PostHistoryId"));
                int localId = Integer.parseInt(record.get("LocalId"));
                int postBlockTypeId = Integer.parseInt(record.get("PostBlockTypeId"));
                int possiblePredecessorsCount = Integer.parseInt(record.get("PossiblePredecessorsCount"));
                int possibleSuccessorsCount = Integer.parseInt(record.get("PossibleSuccessorsCount"));
                String possiblePredecessorLocalIds = record.get("PossiblePredecessorLocalIds");
                String possibleSuccessorLocalIds = record.get("PossibleSuccessorLocalIds");

                newResults.add(new MultipleConnectionsResultNew(postId, postHistoryId, localId, postBlockTypeId,
                        possiblePredecessorsCount, possibleSuccessorsCount,
                        possiblePredecessorLocalIds, possibleSuccessorLocalIds));
            }

            // compare old and new results
            for (MultipleConnectionsResultNew multipleConnectionsResultNew : newResults) {
                int newPostId = multipleConnectionsResultNew.postId;
                int newPostHistoryId = multipleConnectionsResultNew.postHistoryId;
                int newLocalId = multipleConnectionsResultNew.localId;

                int newPostBlockTypeId = multipleConnectionsResultNew.postBlockTypeId;
                int newPossiblePredecessorsCount = multipleConnectionsResultNew.possiblePredecessorsCount;
                int newPossibleSuccessorsCount = multipleConnectionsResultNew.possibleSuccessorsCount;
                String newPossiblePredecessorLocalIds = multipleConnectionsResultNew.possiblePredecessorLocalIds;
                String newPossibleSuccessorLocalIds = multipleConnectionsResultNew.possibleSuccessorLocalIds;

                for (MultipleConnectionsResultOld multipleConnectionsResultOld : oldResults) {
                    int oldPostId = multipleConnectionsResultOld.postId;
                    int oldPostHistoryId = multipleConnectionsResultOld.postHistoryId;
                    int oldLocalId = multipleConnectionsResultOld.localId;

                    int oldPostBlockTypeId = multipleConnectionsResultOld.postBlockTypeId;
                    int oldNumberOfPossibleSuccessorsOrPredecessors = multipleConnectionsResultOld.numberOfPossibleSuccessorsOrPredecessors;
                    String oldPossiblePredOrSuccLocalIds = multipleConnectionsResultOld.possiblePredOrSuccLocalIds;

                    if (newPostId == oldPostId
                            && newPostHistoryId == oldPostHistoryId
                            && newLocalId == oldLocalId) {

                        assertEquals(newPostBlockTypeId, oldPostBlockTypeId);

                        if (oldPossiblePredOrSuccLocalIds.equals(newPossiblePredecessorLocalIds)) {
                            assertEquals(oldNumberOfPossibleSuccessorsOrPredecessors, newPossiblePredecessorsCount);
                        } else if (oldPossiblePredOrSuccLocalIds.equals(newPossibleSuccessorLocalIds)) {
                            assertEquals(oldNumberOfPossibleSuccessorsOrPredecessors, newPossibleSuccessorsCount);

                        } else {
                            logger.warning("Entry (" + newPostId + "," + newPostHistoryId + "," + newLocalId
                                    + ") in new file differs from old file with multiple possible connections.");
                        }

                        break;
                    }
                }
            }

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


    @Test
    void sampleValidationTest() {
        for (Path samplePath : Statistics.pathsToGTSamples) {
            String sampleName = samplePath.toFile().getName();
            Path pathToPostIdList = Paths.get(samplePath.toString(), sampleName + ".csv");
            Path pathToFiles = Paths.get(samplePath.toString(), "files");
            Path pathToGTs = Paths.get(samplePath.toString(), "completed");

            MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                    .withName("TestSample")
                    .withInputPaths(pathToPostIdList, pathToFiles, pathToGTs)
                    .withValidate(false)
                    .initialize();

            assertTrue(manager.validate());
        }
    }


    @Test
    void testMetricEvaluationManagerForEqualMetrics() {
        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                .withName("TestSampleEquals")
                .withInputPaths(
                        Paths.get("testdata", "samples_gt", "PostId_VersionCount_17_06_sample_edited_gt", "PostId_VersionCount_17_06_sample_edited_gt.csv"),
                        Paths.get("testdata", "samples_gt", "PostId_VersionCount_17_06_sample_edited_gt", "files"),
                        Paths.get("testdata", "samples_gt", "PostId_VersionCount_17_06_sample_edited_gt", "completed"))
                .withOutputDirPath(Paths.get("testdata", "samples_gt", "PostId_VersionCount_17_06_sample_edited_gt", "output"))
                .withAddDefaultMetricsAndThresholds(false)
                .initialize();
        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruths().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruths().keySet()));

        List<Double> similarityThresholds = Arrays.asList(0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9);

        for (double similarityThreshold : similarityThresholds) {
            manager.addSimilarityMetric(
                    new SimilarityMetric(
                            "equals",
                            de.unitrier.st.stringsimilarity.edit.Variants::equals,
                            SimilarityMetric.MetricType.EDIT,
                            similarityThreshold
                    )
            );
            manager.addSimilarityMetric(
                    new SimilarityMetric(
                            "equalsNormalized",
                            de.unitrier.st.stringsimilarity.edit.Variants::equalsNormalized,
                            SimilarityMetric.MetricType.EDIT,
                            similarityThreshold
                    )
            );
            manager.addSimilarityMetric(
                    new SimilarityMetric(
                            "tokenEquals",
                            de.unitrier.st.stringsimilarity.set.Variants::tokenEquals,
                            SimilarityMetric.MetricType.SET,
                            similarityThreshold
                    )
            );
            manager.addSimilarityMetric(
                    new SimilarityMetric(
                            "tokenEqualsNormalized",
                            de.unitrier.st.stringsimilarity.set.Variants::tokenEqualsNormalized,
                            SimilarityMetric.MetricType.SET,
                            similarityThreshold
                    )
            );
        }

        Thread managerThread = new Thread(manager);
        managerThread.start();
        try {
            managerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PostVersionList a_19612096 = manager.getPostVersionLists().get(19612096);
        a_19612096.processVersionHistory(
                Config.DEFAULT
                        .withTextSimilarityMetric(de.unitrier.st.stringsimilarity.edit.Variants::equals)
                        .withTextBackupSimilarityMetric(null)
                        .withCodeSimilarityMetric(de.unitrier.st.stringsimilarity.edit.Variants::equals)
                        .withCodeBackupSimilarityMetric(null));

        assertEquals(13, a_19612096.getPostVersion(50536699).getPostBlocks().get(12).getPred().getLocalId().intValue());
        assertEquals(9, a_19612096.getPostVersion(50536699).getPostBlocks().get(8).getPred().getLocalId().intValue());
    }
}
