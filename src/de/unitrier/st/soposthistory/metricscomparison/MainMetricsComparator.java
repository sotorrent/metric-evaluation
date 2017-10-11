package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.metricscomparison.metricsComparison.MetricsComparator;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

public class MainMetricsComparator {


    public static void main(String[] args) throws IOException {

        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();


        // https://stackoverflow.com/a/13011927
        final String sampleOneRandom = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "Metrics comparison", "testdata", "PostId_VersionCount_SO_17-06_sample_100_1").toString();
        final String sampleTwoRandom = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "Metrics comparison", "testdata", "PostId_VersionCount_SO_17-06_sample_100_2").toString();
        final String sampleOneJava = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "Metrics comparison", "testdata", "PostId_VersionCount_SO_Java_17-06_sample_100_1").toString();
        final String sampleTwoJava = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "Metrics comparison", "testdata", "PostId_VersionCount_SO_Java_17-06_sample_100_2").toString();
        final String sampleOnePlusRandom = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "Metrics comparison", "testdata", "PostId_VersionCount_SO_17-06_sample_100_1+").toString();
        final String sampleTwoPlusRandom = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "Metrics comparison", "testdata", "PostId_VersionCount_SO_17-06_sample_100_2+").toString();


        List<String> pathToAllSamples = new ArrayList<>();
        pathToAllSamples.add(sampleOneRandom);
        pathToAllSamples.add(sampleTwoRandom);
        pathToAllSamples.add(sampleOneJava);
        pathToAllSamples.add(sampleTwoJava);
        pathToAllSamples.add(sampleOnePlusRandom);
        pathToAllSamples.add(sampleTwoPlusRandom);


        for(int i=0; i<pathToAllSamples.size(); i++) {
            MetricsComparator metricsComparator = new MetricsComparator(
                    pathToAllSamples.get(i),
                    pathToAllSamples.get(i));

            metricsComparator.createStatisticsFiles(pathToAllSamples.get(i));
        }

        stopWatch.stop();
        System.out.println(stopWatch.getTime() + " milliseconds overall");
    }
}
