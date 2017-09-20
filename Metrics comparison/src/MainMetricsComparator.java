import metricsComparism.MetricsComparator;
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
        String sampleOneRandom = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_1", "files").toString();
        String sampleOneRandomCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_1", "completed").toString();

        String sampleTwoRandom = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_2", "files").toString();
        String sampleTwoRandomCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_2", "completed").toString();

        String sampleOneJava = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_Java_17-06_sample_100_1", "files").toString();
        String sampleOneJavaCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_Java_17-06_sample_100_1", "completed").toString();

        String sampleTwoJava = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_Java_17-06_sample_100_2", "files").toString();
        String sampleTwoJavaCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_Java_17-06_sample_100_2", "completed").toString();

        String sampleOnePlusRandom = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_1+", "files").toString();
        String sampleOnePlusRandomCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_1+", "completed").toString();

        String sampleTwoPlusRandom = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_2+", "files").toString();
        String sampleTwoPlusRandomCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_2+", "completed").toString();

        String sampleMostVersions = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_most_versions", "files").toString();
        String sampleMostVersionsCompleted = System.getProperty("user.dir") + FileSystems.getDefault().getPath("data", "PostId_VersionCount_SO_17-06_sample_100_most_versions", "completed").toString();


        List<String> pathToPostHistories = new ArrayList<>();
        pathToPostHistories.add(sampleOneRandom);
        pathToPostHistories.add(sampleTwoRandom);
        pathToPostHistories.add(sampleOneJava);
        pathToPostHistories.add(sampleTwoJava);
        pathToPostHistories.add(sampleOnePlusRandom);
        pathToPostHistories.add(sampleTwoPlusRandom);
        pathToPostHistories.add(sampleMostVersions);


        List<String> pathToAllCompletedCSVs = new ArrayList<>();
        pathToPostHistories.add(sampleOneRandomCompleted);
        pathToPostHistories.add(sampleTwoRandomCompleted);
        pathToPostHistories.add(sampleOneJavaCompleted);
        pathToPostHistories.add(sampleTwoJavaCompleted);
        pathToPostHistories.add(sampleOnePlusRandomCompleted);
        pathToPostHistories.add(sampleTwoPlusRandomCompleted);
        pathToPostHistories.add(sampleMostVersionsCompleted);


        for(int i=0; i<pathToPostHistories.size(); i++) {
            MetricsComparator metricsComparator = new MetricsComparator(
                    pathToPostHistories.get(i),
                    pathToAllCompletedCSVs.get(i));

            metricsComparator.createStatisticsFiles(pathToPostHistories.get(i));

        }

        stopWatch.stop();
        System.out.println(stopWatch.getTime() + " milliseconds overall");
    }
}

