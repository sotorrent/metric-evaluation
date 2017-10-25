package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PostVersionListManagementTest {

    @Test
    void testPostVersionListManagement(){

        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(Paths.get("testdata", "fewCompletedFiles").toString());
        postVersionsListManagement.postVersionLists.get(0).processVersionHistory();

        // testing text
        assertNull(postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(4711));    // post with id 4711 is not listed
        assertEquals(2, postVersionsListManagement.postVersionLists.size());

        ConnectionsOfAllVersions connectionsOfAllVersions_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(3758880);
        assertEquals(10, connectionsOfAllVersions_text.size());       // post version list has 11 versions and therefore 10 comparisons for adjacent versions

        assertEquals(new Integer(1), connectionsOfAllVersions_text.get(0).get(0).getLeftLocalId());
        assertEquals(new Integer(1), connectionsOfAllVersions_text.get(0).get(0).getRightLocalId());
        assertEquals(1, connectionsOfAllVersions_text.get(0).get(0).getPostBlockTypeId());
        assertNull(connectionsOfAllVersions_text.get(0).get(1).getLeftLocalId());
        assertEquals(new Integer(3),connectionsOfAllVersions_text.get(0).get(1).getRightLocalId());

        assertEquals(new Integer(3), connectionsOfAllVersions_text.get(7).get(1).getLeftLocalId());
        assertEquals(new Integer(3), connectionsOfAllVersions_text.get(7).get(1).getRightLocalId());


        // testing code
        ConnectionsOfAllVersions connectionsOfAllVersions_code = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(3758880);
        assertEquals(10, connectionsOfAllVersions_code.size());       // post version list has 11 versions and therefore 10 comparisons for adjacent versions

        assertEquals(new Integer(6), connectionsOfAllVersions_code.get(0).get(1).getLeftLocalId()); // compares always from right to left
        assertEquals(new Integer(4), connectionsOfAllVersions_code.get(0).get(1).getRightLocalId());

        assertEquals(2, connectionsOfAllVersions_code.get(2).get(1).getPostBlockTypeId());
        assertEquals(new Integer(4),connectionsOfAllVersions_code.get(2).get(1).getLeftLocalId());
        assertEquals(new Integer(4),connectionsOfAllVersions_code.get(2).get(1).getRightLocalId());
    }
}
