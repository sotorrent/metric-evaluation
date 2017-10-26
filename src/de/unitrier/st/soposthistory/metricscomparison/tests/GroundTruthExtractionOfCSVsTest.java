package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.csvExtraction.GroundTruthExtractionOfCSVs;
import de.unitrier.st.soposthistory.metricscomparison.util.ConnectionsOfAllVersions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GroundTruthExtractionOfCSVsTest {

    @Test
    void testGroundTruthExtractionOfCSV(){
        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(Paths.get("testdata", "Samples_test", "fewCompletedFiles").toString());

        // testing text
        assertNull(groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(4711));    // post with id 4711 is not listed in ground truth
        assertEquals(2, groundTruthExtractionOfCSVs.getGroundTruthText().size());

        ConnectionsOfAllVersions connectionsOfAllVersions_text = groundTruthExtractionOfCSVs.getGroundTruthText().getFirst();
        assertEquals(connectionsOfAllVersions_text, groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(3758880));
        assertEquals(10, connectionsOfAllVersions_text.size());       // post version list has 11 versions and therefore 10 comparisons for adjacent versions

        assertEquals(new Integer(1), connectionsOfAllVersions_text.get(0).get(0).getLeftLocalId());
        assertEquals(new Integer(1), connectionsOfAllVersions_text.get(0).get(0).getRightLocalId());
        assertEquals(1, connectionsOfAllVersions_text.get(0).get(0).getPostBlockTypeId());
        assertNull(connectionsOfAllVersions_text.get(0).get(1).getLeftLocalId());
        assertEquals(new Integer(3),connectionsOfAllVersions_text.get(0).get(1).getRightLocalId());

        assertEquals(new Integer(3), connectionsOfAllVersions_text.get(7).get(1).getLeftLocalId());
        assertEquals(new Integer(3), connectionsOfAllVersions_text.get(7).get(1).getRightLocalId());


        // testing code
        ConnectionsOfAllVersions connectionsOfAllVersions_code = groundTruthExtractionOfCSVs.getGroundTruthCode().getFirst();
        assertEquals(10, connectionsOfAllVersions_code.size());       // post version list has 11 versions and therefore 10 comparisons for adjacent versions

        assertEquals(new Integer(6), connectionsOfAllVersions_code.get(0).get(1).getLeftLocalId()); // compares always from right to left
        assertEquals(new Integer(4), connectionsOfAllVersions_code.get(0).get(1).getRightLocalId());

        assertEquals(2, connectionsOfAllVersions_code.get(2).get(1).getPostBlockTypeId());
        assertEquals(new Integer(4),connectionsOfAllVersions_code.get(2).get(1).getLeftLocalId());
        assertEquals(new Integer(4),connectionsOfAllVersions_code.get(2).get(1).getRightLocalId());
    }
}
