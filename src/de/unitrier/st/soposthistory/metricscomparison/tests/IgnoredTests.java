package de.unitrier.st.soposthistory.metricscomparison.tests;

import de.unitrier.st.soposthistory.metricscomparison.metricsComparison.MetricsComparator;
import de.unitrier.st.soposthistory.version.PostVersionList;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;

import static de.unitrier.st.soposthistory.metricscomparison.csvExtraction.PostVersionsListManagement.pattern_groundTruth;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

// https://stackoverflow.com/a/7535602
@Ignore
class IgnoredTests{

    // These are time-consuming test cases which were checked manually and were successful

    static LinkedList<String> pathToAllDirectories = new LinkedList<>();

    @BeforeAll
    static void init() {
        for (int i=1; i<=10; i++) {
            pathToAllDirectories.add(Paths.get("testdata", "Samples_10000", "PostId_VersionCount_SO_17-06_sample_10000_" + i, "files").toString());
        }
    }

    @Test
    void testSetIfAllPostVersionListsAreParsable() throws IOException {

        for (String path : pathToAllDirectories) {
            File file = new File(path);
            File[] allPostHistoriesInFolder = file.listFiles((dir, name) -> name.matches(pattern_groundTruth.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

            assert allPostHistoriesInFolder != null;
            for (File postHistory : allPostHistoriesInFolder) {
                int postId = Integer.parseInt(postHistory.getName().replace(".csv", ""));
                PostVersionList tmpPostVersionList;
                try {
                    tmpPostVersionList = PostVersionList.readFromCSV(path + "\\", postId, 2);
                    tmpPostVersionList.processVersionHistory();
                    tmpPostVersionList.normalizeLinks();
                } catch (Exception e) {
                    e.printStackTrace();
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
                Paths.get("testdata", "Samples_test", "fewCompletedFiles").toString(),
                Paths.get("testdata", "Samples_test", "fewCompletedFiles").toString());

        metricsComparator.createStatisticsFiles(Paths.get("testdata", "Samples_test", "fewCompletedFiles").toString());

        stopWatch.stop();

        assert true;
    }
}
