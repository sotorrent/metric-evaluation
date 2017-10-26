package de.unitrier.st.soposthistory.metricscomparison.metricsComparison;

import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.GroundTruthExtractionOfCSVs;
import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

public class MetricsComparator {

    private String pathToDirectoryOfAllPostHistories;
    private String pathToDirectoryOfAllCompletedCSVs;

    private PostVersionsListManagement postVersionsListManagement;
    private GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs;

    private LinkedList<ConnectionsOfAllVersions> groundTruth = null;
    private LinkedList<ConnectionsOfAllVersions> groundTruth_text = null;
    private LinkedList<ConnectionsOfAllVersions> groundTruth_code = null;

    // constructor and helping methods
    public MetricsComparator(String pathToDirectoryOfPostHistories, String pathToDirectoryOfCompletedCSVs) {

        this.pathToDirectoryOfAllPostHistories = pathToDirectoryOfPostHistories;
        this.pathToDirectoryOfAllCompletedCSVs = pathToDirectoryOfCompletedCSVs;

        postVersionsListManagement = new PostVersionsListManagement(pathToDirectoryOfPostHistories);
        groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(pathToDirectoryOfCompletedCSVs);

        this.groundTruth = groundTruthExtractionOfCSVs.getGroundTruth();
        this.groundTruth_text = groundTruthExtractionOfCSVs.getGroundTruthText();
        this.groundTruth_code = groundTruthExtractionOfCSVs.getGroundTruthCode();

        checkWhetherSetOfCompletedPostsIsSameAsSetOfPostHistories();

        checkWhetherNumberOfBlocksIsSame();
    }

    private void checkWhetherSetOfCompletedPostsIsSameAsSetOfPostHistories() {
        LinkedList<Integer> postIdsOfGroundTruth = new LinkedList<>();
        LinkedList<Integer> postIdsOfPostHistories = new LinkedList<>();
        for (int i = 0; i < groundTruth.size(); i++) {
            postIdsOfGroundTruth.add(groundTruth.get(i).getPostId());
        }

        for (int i = 0; i < postVersionsListManagement.postVersionLists.size(); i++) {
            postIdsOfPostHistories.add(postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId());
        }

        Collections.sort(postIdsOfGroundTruth);
        Collections.sort(postIdsOfPostHistories);

        if (!postIdsOfGroundTruth.equals(postIdsOfPostHistories)) {
            LinkedList<Integer> invalidPosts = new LinkedList<>();
            for (int i = 0; i < postIdsOfGroundTruth.size(); i++) {
                if (!postIdsOfPostHistories.contains(postIdsOfGroundTruth.get(i))) {
                    invalidPosts.add(postIdsOfGroundTruth.get(i));
                }
            }
            for (int i = 0; i < postIdsOfPostHistories.size(); i++) {
                if (!postIdsOfGroundTruth.contains(postIdsOfPostHistories.get(i))) {
                    invalidPosts.add(postIdsOfPostHistories.get(i));
                }
            }

            System.err.println("Every post version list must have a corresponding completed csv, but doesn't.");
            System.err.println("Check the following post(s): " + invalidPosts);
            System.exit(0);
        }
    }

    private void checkWhetherNumberOfBlocksIsSame() {

        if (groundTruth_text.size() != postVersionsListManagement.postVersionLists.size()
                || groundTruth_code.size() != postVersionsListManagement.postVersionLists.size()) {
            System.err.println("number of ground truths and number of post version lists are different");
        }

        for (int i = 0; i < groundTruth_text.size(); i++) {

            int currentPostId = groundTruth.get(i).getPostId();

            int numberOfTextBlocksOverallInGroundTruth = 0;
            for (int j = 0; j < groundTruth_text.get(i).size(); j++) {
                numberOfTextBlocksOverallInGroundTruth += groundTruth_text.get(i).get(j).size();
            }

            int numberOfTextBlocksOverallInComputedMetric = 0;
            for (int j = 0; j < postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(currentPostId).size(); j++) {
                numberOfTextBlocksOverallInComputedMetric += postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(currentPostId).get(j).size();
            }

            if (numberOfTextBlocksOverallInGroundTruth != numberOfTextBlocksOverallInComputedMetric) {
                System.err.println(
                        "Number of text blocks that will be compared must be the same but are different in post with id "
                                + postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId() + ": "
                                + numberOfTextBlocksOverallInGroundTruth + " text blocks in ground truth and "
                                + numberOfTextBlocksOverallInComputedMetric + " text blocks in computed metric");
                System.err.println("ground truth:");
                System.err.println(groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId()));
                System.err.println("computed metric:");
                System.err.println(postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId()));
                System.exit(0);
            }
        }

        for (int i = 0; i < groundTruth_code.size(); i++) {

            int currentPostId = groundTruth.get(i).getPostId();

            int numberOfCodeBlocksOverallInGroundTruth = 0;
            for (int j = 0; j < groundTruth_code.get(i).size(); j++) {
                numberOfCodeBlocksOverallInGroundTruth += groundTruth_code.get(i).get(j).size();
            }

            int numberOfCodeBlocksOverallInComputedMetric = 0;
            for (int j = 0; j < postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(currentPostId).size(); j++) {
                numberOfCodeBlocksOverallInComputedMetric += postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(currentPostId).get(j).size();
            }

            if (numberOfCodeBlocksOverallInGroundTruth != numberOfCodeBlocksOverallInComputedMetric) {
                System.err.println(
                        "Number of code blocks that will be compared must be the same but are different in post with id "
                                + postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId() + ": "
                                + numberOfCodeBlocksOverallInGroundTruth + " code blocks in ground truth and "
                                + numberOfCodeBlocksOverallInComputedMetric + " code blocks in computed metric");
                System.err.println("ground truth:");
                System.err.println(groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_code(postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId()));
                System.err.println("computed metric:");
                System.err.println(postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(postVersionsListManagement.postVersionLists.get(i).getFirst().getPostId()));
                System.exit(0);
            }
        }
    }


    // computing similarity
    public MetricResult computeSimilarity_writeInResult_text(int postVersionListID, BiFunction<String, String, Double> metric, double threshold) {

        MetricResult metricResult = new MetricResult();

        metricResult.stopWatch.reset();
        metricResult.stopWatch.start();
        postVersionsListManagement
                .getPostVersionListWithID(postVersionListID)
                .processVersionHistory(
                        PostVersionList.PostBlockTypeFilter.TEXT,
                        Config.DEFAULT
                                .withTextSimilarityMetric(metric)
                                .withTextBackupSimilarityMetric(null)
                                .withTextSimilarityThreshold(threshold)

                );
        metricResult.stopWatch.stop();
        metricResult.stopWatch.getNanoTime();

        metricResult.connectionsOfAllVersions_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postVersionListID);
        metricResult.compareMetricResults_text(groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(postVersionListID));

        metricResult.updateMetricResult_text();

        return metricResult;
    }

    public MetricResult computeSimilarity_writeInResult_code(int postVersionListID, BiFunction<String, String, Double> metric, double threshold) {

        MetricResult metricResult = new MetricResult();

        metricResult.stopWatch.reset();
        metricResult.stopWatch.start();
        postVersionsListManagement
                .getPostVersionListWithID(postVersionListID)
                .processVersionHistory(
                        PostVersionList.PostBlockTypeFilter.CODE,
                        Config.DEFAULT
                                .withCodeSimilarityMetric(metric)
                                .withCodeBackupSimilarityMetric(null)
                                .withCodeSimilarityThreshold(threshold)
                );
        metricResult.stopWatch.stop();
        metricResult.stopWatch.getNanoTime();

        metricResult.connectionsOfAllVersions_code = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(postVersionListID);
        metricResult.compareMetricResults_code(groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_code(postVersionListID));

        metricResult.updateMetricResult_code();

        return metricResult;
    }

    public void createStatisticsFiles(String pathToSample) throws IOException {

        Metric.Type[] metrics = Metric.Type.values().clone();

        for (int i = 0; i < metrics.length; i++) {
            Metric.Type tmp = metrics[i];
            int rnd = (int) (Math.random() * metrics.length);
            metrics[i] = metrics[rnd];
            metrics[rnd] = tmp;
        }


        PrintWriter printWriter = new PrintWriter(new File(pathToSample + ".csv"));

        printWriter.write("sample; ");
        printWriter.write("metric; ");
        printWriter.write("threshold; ");

        printWriter.write("postid; ");
        printWriter.write("posthistoryid; ");

        printWriter.write("runtimetext; ");
        printWriter.write("#truepositivestext; ");
        printWriter.write("#truenegativestext; ");
        printWriter.write("#falsepositivestext; ");
        printWriter.write("#falsenegativestext; ");

        printWriter.write("#runtimecode; ");
        printWriter.write("#truepositivescode; ");
        printWriter.write("#truenegativescode; ");
        printWriter.write("#falsepositivescode; ");
        printWriter.write("#falsenegativescode; ");
        printWriter.write("\n");


        List<Double> thresholds = Arrays.asList(0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9);

        for (Double threshold : thresholds) {

            for (Metric.Type metric : metrics) {

                if (Metric.getBiFunctionMetric(metric) == null) {
                    System.err.println("FAILED " + metric);
                    continue;
                }

                this.postVersionsListManagement = new PostVersionsListManagement(pathToDirectoryOfAllPostHistories);

                for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
                    try {
                        MetricResult tmpMetricResult_text
                                = this.computeSimilarity_writeInResult_text(
                                postVersionList.getFirst().getPostId(),
                                Metric.getBiFunctionMetric(metric),
                                threshold);

                        MetricResult tmpMetricResult_code
                                = this.computeSimilarity_writeInResult_code(
                                postVersionList.getFirst().getPostId(),
                                Metric.getBiFunctionMetric(metric),
                                threshold);


                        for (int l = 0; l < postVersionList.size() - 1; l++) {
                            printWriter.write(pathToSample + "; ");
                            printWriter.write(metric + "; ");
                            printWriter.write(threshold + "; ");

                            printWriter.write(postVersionList.get(l).getPostId() + "; ");
                            printWriter.write(postVersionList.get(l).getPostHistoryId() + "; ");

                            printWriter.write(tmpMetricResult_text.totalTimeMeasured_text + "; ");
                            printWriter.write(tmpMetricResult_text.measuredDataList.get(l).truePositives_text + "; ");
                            printWriter.write(tmpMetricResult_text.measuredDataList.get(l).trueNegatives_text + "; ");
                            printWriter.write(tmpMetricResult_text.measuredDataList.get(l).falsePositives_text + "; ");
                            printWriter.write(tmpMetricResult_text.measuredDataList.get(l).falseNegatives_text + "; ");

                            printWriter.write(tmpMetricResult_code.totalTimeMeasured_code + "; ");
                            printWriter.write(tmpMetricResult_code.measuredDataList.get(l).truePositives_code + "; ");
                            printWriter.write(tmpMetricResult_code.measuredDataList.get(l).trueNegatives_code + "; ");
                            printWriter.write(tmpMetricResult_code.measuredDataList.get(l).falsePositives_code + "; ");
                            printWriter.write(tmpMetricResult_code.measuredDataList.get(l).falseNegatives_code + "\n");
                        }

                    } catch (IllegalArgumentException e) {
                        printWriter.write(pathToSample + "; ");
                        printWriter.write(metric + "; ");
                        printWriter.write(threshold + "; ");

                        printWriter.write(postVersionList.getFirst().getPostId() + "; ");

                        for(int i=0; i<10; i++)
                            printWriter.write("null" + "; ");
                        printWriter.write("null" + "\n");
                    }
                }
                printWriter.flush();

                // System.out.println("metric " + metric + " with threshold " + threshold + " completed (" + count++ + " of " + metrics.length + ")");
            }

            System.out.println("completed threshold " + threshold + " in sample " + pathToSample);
        }

        printWriter.close();

        System.out.println("completed sample: " + pathToSample);
    }
}
