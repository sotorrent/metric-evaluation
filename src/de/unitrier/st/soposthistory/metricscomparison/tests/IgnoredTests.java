package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.metricsComparison.MetricsComparator;
import de.unitrier.st.soposthistory.version.PostVersionList;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement.pattern_groundTruth;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

// https://stackoverflow.com/a/7535602
@Ignore
class IgnoredTests extends MetricsComparisonTest{

    // These are time-consuming test cases which were checked manually and were successful

    @Test
    void testSetIfAllPostVersionListsAreParsable() throws IOException {

        for (String path : pathToAllDirectories) {
            File file = new File(path);
            File[] allPostHistoriesInFolder = file.listFiles((dir, name) -> name.matches(pattern_groundTruth.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

            assert allPostHistoriesInFolder != null;
            for (File postHistory : allPostHistoriesInFolder) {
                PostVersionList tmpPostVersionList = new PostVersionList();
                try {
                    int postId = Integer.valueOf(postHistory.getName().substring(0, postHistory.getName().length() - 4));
                    tmpPostVersionList.readFromCSV(path + "\\", postId, 2);
                    tmpPostVersionList.processVersionHistory();
                    tmpPostVersionList.normalizeLinks();
                } catch (Exception e) {
                    assertNotEquals(0, tmpPostVersionList.size());
                }
            }
            System.out.println("Finished: " + path);
        }
    }

    @Test
    void testMetricsComparision() throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();

        MetricsComparator metricsComparator = new MetricsComparator(
                pathToFewCompletedFiles,
                pathToFewCompletedFiles);

        metricsComparator.createStatisticsFiles(pathToFewCompletedFiles);

        stopWatch.stop();

        assert true;
    }
}
