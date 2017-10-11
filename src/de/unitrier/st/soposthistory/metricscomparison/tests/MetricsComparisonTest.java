package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.GroundTruthExtractionOfCSVs;
import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.metricscomparison.metricsComparison.MetricsComparator;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfTwoVersions;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.soposthistory.gt.util.anchorsURLs.AnchorTextAndUrlHandler;
import de.unitrier.st.stringsimilarity.profile.Variants;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement.pattern_groundTruth;
import static de.unitrier.st.soposthistory.gt.GroundTruthApp.GroundTruthCreator.normalizeURLsInTextBlocksOfAllVersions;

// TODO: get rid of dependency to soposthistory.gt

public class MetricsComparisonTest {

    // TODO: make paths relative or configurable
    // TODO: do not start time-consuming test cases automatically

    private static String pathToCSVs = Paths.get("testdata", "representative CSVs").toString();
    private static String pathToFewCompletedFiles = Paths.get("testdata", "fewCompletedFiles").toString();
    private static LinkedList<String> pathToAllDirectories = new LinkedList<>();

    @BeforeAll
    public static void init() {
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_1\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_2\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_3\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_4\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_5\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_6\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_7\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_8\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_9\\files");
        pathToAllDirectories.add("C:\\Users\\Lorik\\Desktop\\5. Semester\\Master-Arbeit\\PostVersionLists\\PostId_VersionCount_SO_17-06_sample_10000_10\\files");
    }


    @Test
    public void testExtractionOfOnePost() {

        int postId = 3758880;

        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(pathToCSVs);
        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);

        TextBlockVersion.similarityMetric = Variants::manhattanThreeGramNormalized;
        CodeBlockVersion.similarityMetric = Variants::manhattanThreeGramNormalized;

        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory();

        ConnectionsOfAllVersions connectionsOfAllVersionsGroundTruth_text = groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(postId);
        ConnectionsOfAllVersions connectionsOfAllVersionsGroundTruth_code = groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_code(postId);
        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);
        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_code = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(postId);


        System.out.println("Ground Truth: ");
        System.out.println("All text blocks:");
        for (int i = 0; i < connectionsOfAllVersionsGroundTruth_text.size(); i++) {
            System.out.println(connectionsOfAllVersionsGroundTruth_text.get(i));
        }
        System.out.println("\nAll code blocks:");
        for (int i = 0; i < connectionsOfAllVersionsGroundTruth_code.size(); i++) {
            System.out.println(connectionsOfAllVersionsGroundTruth_code.get(i));
        }


        System.out.println("\n\nComputed Metric: ");
        System.out.println("All text blocks:");
        for (int i = 0; i < connectionsOfAllVersionsComputedMetric_text.size(); i++) {
            System.out.println(connectionsOfAllVersionsComputedMetric_text.get(i));
        }
        System.out.println("\n\nAll code blocks:");
        for (int i = 0; i < connectionsOfAllVersionsComputedMetric_code.size(); i++) {
            System.out.println(connectionsOfAllVersionsComputedMetric_code.get(i));
        }
    }


    @Test
    public void testSetIfAllPostVersionListsAreParsable() throws IOException {

        for (String path : pathToAllDirectories) {
            File file = new File(path);
            File[] allPostHistoriesInFolder = file.listFiles((dir, name) -> name.matches(pattern_groundTruth.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

            assert allPostHistoriesInFolder != null;
            for (File postHistory : allPostHistoriesInFolder) {
                try {
                    PostVersionList tmpPostVersionList = new PostVersionList();
                    int postId = Integer.valueOf(postHistory.getName().substring(0, postHistory.getName().length() - 4));
                    tmpPostVersionList.readFromCSV(path + "\\", postId, 2);
                    tmpPostVersionList.processVersionHistory();

                    AnchorTextAndUrlHandler anchorTextAndUrlHandler = new AnchorTextAndUrlHandler();
                    normalizeURLsInTextBlocksOfAllVersions(tmpPostVersionList, anchorTextAndUrlHandler);
                } catch (Exception e) {

                    System.out.println("Failed to parse " + postHistory.getPath());
                }
            }

            System.out.println("Finished: " + path);
        }
    }


    @Test
    public void testMetricsComparism() throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();

        MetricsComparator metricsComparator = new MetricsComparator(
                pathToFewCompletedFiles,
                pathToFewCompletedFiles);

        metricsComparator.createStatisticsFiles(pathToFewCompletedFiles);

        stopWatch.stop();
        System.out.println(stopWatch.getTime() + " milliseconds overall");
    }


    @Test
    public void testNumberOfPredecessorsOfOnePost() {
        int postId = 3758880;
        TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceVariant;

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory(PostVersionList.PostBlockTypeFilter.TEXT);


        List<TextBlockVersion> textBlocks = postVersionsListManagement.getPostVersionListWithID(postId).get(postVersionsListManagement.getPostVersionListWithID(postId).size() - 1).getTextBlocks();
        for (int i = 0; i < textBlocks.size(); i++) {
            Integer predId = null;
            if (textBlocks.get(i).getPred() != null)
                predId = textBlocks.get(i).getPred().getLocalId();
            System.out.println(textBlocks.get(i).getLocalId() + " has pred " + predId);
        }
        System.out.println();
    }

    @Test
    public void testNumberOfPredecessorsComputedMetric() {

        TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceVariant;

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);

        for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
            postVersionList.processVersionHistory();

            for (PostVersion postVersion : postVersionList) {
                List<PostBlockVersion> postBlocks = postVersion.getPostBlocks();

                for (int i = 0; i < postBlocks.size(); i++) {
                    if (postBlocks.get(i).getPred() == null)
                        continue;

                    for (int j = i + 1; j < postBlocks.size(); j++) {
                        if (postBlocks.get(j).getPred() == null || postBlocks.get(i) instanceof TextBlockVersion != postBlocks.get(j) instanceof TextBlockVersion)
                            continue;

                        if (Objects.equals(postBlocks.get(i).getPred().getLocalId(), postBlocks.get(j).getPred().getLocalId())) {
                            System.err.println(
                                    "Error: multiple predecessors are set in post with id " + postVersion.getPostId() + "."
                            );
                        }
                    }
                }

            }
        }
    }

    @Test
    public void testNumberOfPredecessorsGroundTruth() {

        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs("C:\\Users\\Lorik\\Documents\\GitHub\\so-posthistory-gt\\postVersionLists");

        for (ConnectionsOfAllVersions connectionsOfAllVersions : groundTruthExtractionOfCSVs.getGroundTruth()) {
            for (ConnectionsOfTwoVersions connectionsOfTwoVersions : connectionsOfAllVersions) {

                for (int i = 0; i < connectionsOfTwoVersions.size(); i++) {
                    if (connectionsOfTwoVersions.get(i).getLeftLocalId() == null)
                        continue;

                    for (int j = i + 1; j < connectionsOfTwoVersions.size(); j++) {
                        if (connectionsOfTwoVersions.get(j).getLeftLocalId() == null || connectionsOfTwoVersions.get(i).getPostBlockTypeId() != connectionsOfTwoVersions.get(j).getPostBlockTypeId())
                            continue;

                        if (Objects.equals(connectionsOfTwoVersions.get(i).getLeftLocalId(), connectionsOfTwoVersions.get(j).getLeftLocalId())) {
                            System.err.println(
                                    "Error: multiple predecessors are set in post with id " + connectionsOfAllVersions.getPostId() + "."
                            );
                        }
                    }
                }

            }
        }
    }

    @Test
    public void checkWhetherPostVersionListConnectionsWillBeResetRight() {
        int postId = 3758880;
        //TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTokenDiceVariant;

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory(PostVersionList.PostBlockTypeFilter.TEXT);


        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);

        System.out.println("\n\nComputed Metric after processVersionHistory: ");
        System.out.println("All text blocks:");
        for (int i = 0; i < connectionsOfAllVersionsComputedMetric_text.size(); i++) {
            System.out.println(connectionsOfAllVersionsComputedMetric_text.get(i));
        }


        postVersionsListManagement = new PostVersionsListManagement(pathToCSVs.toString());
        connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);

        System.out.println("\n\nComputed Metric after resetting of links: ");
        System.out.println("All text blocks:");
        for (int i = 0; i < connectionsOfAllVersionsComputedMetric_text.size(); i++) {
            System.out.println(connectionsOfAllVersionsComputedMetric_text.get(i));
        }
    }
}
