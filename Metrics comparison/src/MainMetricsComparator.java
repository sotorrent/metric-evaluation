import metricsComparism.MetricsComparator;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;

public class MainMetricsComparator {


    public static void main(String[] args) throws IOException {

        // TODO: for all samples

        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();

        String pathToAllCompletedCSVs = System.getProperty("user.dir") + "\\data\\Completed_PostId_VersionCount_SO_17-06_sample_100_2_files\\Completed_PostId_VersionCount_SO_17-06_sample_100_2_files\\Completed_PostId_VersionCount_SO_17-06_sample_100_2_files";    // https://stackoverflow.com/a/13011927
        String pathToPostHistories = System.getProperty("user.dir") + "\\data\\PostId_VersionCount_SO_17-06_sample_100_2_files\\PostId_VersionCount_SO_17-06_sample_100_2_files\\PostId_VersionCount_SO_17-06_sample_100_2_files";                           // https://stackoverflow.com/a/13011927

        MetricsComparator metricsComparator = new MetricsComparator(
                pathToPostHistories,
                pathToAllCompletedCSVs);

        metricsComparator.createStatisticsFiles();

        stopWatch.stop();
        System.out.println(stopWatch.getTime() + " milliseconds overall");
    }
}

