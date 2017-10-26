package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.GroundTruthExtractionOfCSVs;
import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectedBlocks;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfTwoVersions;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.*;

public class MetricsComparisonTest {

    private static String pathToCSVs = Paths.get("testdata", "Samples_test", "representative CSVs").toString();
    static String pathToFewCompletedFiles = Paths.get("testdata", "Samples_test", "fewCompletedFiles").toString();
    static LinkedList<String> pathToAllDirectories = new LinkedList<>();


    @Test
    void testExtractionsForOnePost() {

        int postId = 22037280;

        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(pathToFewCompletedFiles);
        ConnectionsOfAllVersions connectionsOfAllVersionsGroundTruth_text = groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(postId);
        ConnectionsOfAllVersions connectionsOfAllVersionsGroundTruth_code = groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_code(postId);

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToFewCompletedFiles);
        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory();
        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);
        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_code = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(postId);


        assertEquals(6, connectionsOfAllVersionsGroundTruth_text.size());

        assertThat(connectionsOfAllVersionsComputedMetric_text, is(connectionsOfAllVersionsComputedMetric_text));
        assertThat(connectionsOfAllVersionsComputedMetric_code, is(connectionsOfAllVersionsComputedMetric_code));

    }


    @Test
    void testNumberOfPredecessorsOfOnePost() {
        int postId = 3758880;

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory(
                PostVersionList.PostBlockTypeFilter.TEXT,
                Config.DEFAULT.withTextSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice));

        List<TextBlockVersion> textBlocks = postVersionsListManagement.getPostVersionListWithID(postId).get(postVersionsListManagement.getPostVersionListWithID(postId).size() - 1).getTextBlocks();
        assertEquals(new Integer(1), textBlocks.get(0).getPred().getLocalId());
        assertEquals(new Integer(1), textBlocks.get(0).getLocalId());

        assertEquals(new Integer(3), textBlocks.get(1).getPred().getLocalId());
        assertEquals(new Integer(3), textBlocks.get(1).getLocalId());

        assertEquals(null, textBlocks.get(2).getPred());
        assertEquals(new Integer(5), textBlocks.get(2).getLocalId());
    }

    @Test
    void testNumberOfPredecessorsComputedMetric() {

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);

        for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
            postVersionList.processVersionHistory(Config.DEFAULT.withTextSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice));

            for (PostVersion postVersion : postVersionList) {
                List<PostBlockVersion> postBlocks = postVersion.getPostBlocks();

                for (int i = 0; i < postBlocks.size(); i++) {
                    if (postBlocks.get(i).getPred() == null)
                        continue;

                    for (int j = i + 1; j < postBlocks.size(); j++) {
                        if (postBlocks.get(j).getPred() == null || postBlocks.get(i) instanceof TextBlockVersion != postBlocks.get(j) instanceof TextBlockVersion)
                            continue;

                        assertNotEquals(postBlocks.get(i).getPred().getLocalId(), postBlocks.get(j).getPred().getLocalId());
                    }
                }

            }
        }
    }

    @Test
    void testNumberOfPredecessorsGroundTruth() {

        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(Paths.get("testdata","Samples_test",  "representative CSVs").toString());

        for (ConnectionsOfAllVersions connectionsOfAllVersions : groundTruthExtractionOfCSVs.getGroundTruth()) {
            for (ConnectionsOfTwoVersions connectionsOfTwoVersions : connectionsOfAllVersions) {

                for (int i = 0; i < connectionsOfTwoVersions.size(); i++) {
                    if (connectionsOfTwoVersions.get(i).getLeftLocalId() == null)
                        continue;

                    for (int j = i + 1; j < connectionsOfTwoVersions.size(); j++) {
                        if (connectionsOfTwoVersions.get(j).getLeftLocalId() == null ||
                                connectionsOfTwoVersions.get(i).getPostBlockTypeId() != connectionsOfTwoVersions.get(j).getPostBlockTypeId())
                            continue;

                        assertNotEquals(connectionsOfTwoVersions.get(i).getLeftLocalId(), connectionsOfTwoVersions.get(j).getLeftLocalId());
                    }
                }

            }
        }
    }

    @Test
    void checkWhetherPostVersionListConnectionsWillBeResetRight() {
        int postId = 3758880;
        //TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTokenDiceVariant;

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory(PostVersionList.PostBlockTypeFilter.TEXT);


        // This sets predecessors
        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);

        // This resets the predecessors again
        postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
        connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);
        for (ConnectionsOfTwoVersions connections : connectionsOfAllVersionsComputedMetric_text) {
            for (ConnectedBlocks connection : connections) {
                assertNull(connection.getLeftLocalId());
                assertNotNull(connection.getRightLocalId());
            }
        }
    }
}
