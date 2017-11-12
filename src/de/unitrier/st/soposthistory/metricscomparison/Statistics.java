package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.util.Util;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.csvFormatMetricComparisonPost;
import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.csvFormatMetricComparisonVersion;
import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.csvFormatPostIds;
import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// TODO: move to metrics comparison project
public class Statistics {
    private static Logger logger = null;

    private static final Path rootPathToGTSamples = Paths.get("testdata", "samples_gt");
    public static final List<Path> pathsToGTSamples = getGTSamples();

    private static final Path rootPathToTestSamples = Paths.get("testdata", "samples_test");
    public static final List<Path> pathsToTestSamples = getTestSamples();

    public static final Path pathToMultipleConnectionsDir = Paths.get("testdata", "multiple_connections");
    public static final Path pathToMultipleConnectionsFile = Paths.get(pathToMultipleConnectionsDir.toString(), "multiple_possible_connections.csv");
    private static final Path pathToMultipleConnectionsPostsFile = Paths.get(pathToMultipleConnectionsDir.toString(), "multiple_possible_connections_posts.csv");

    private static final Path outputDir = Paths.get("output");

    public static final CSVFormat csvFormatMultipleConnections;

    static {
        // configure logger
        try {
            logger = getClassLogger(Statistics.class, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format
        csvFormatMultipleConnections = CSVFormat.DEFAULT
                .withHeader("PostId", "PostHistoryId", "LocalId", "PostBlockTypeId", "PossiblePredecessorsCount", "PossibleSuccessorsCount", "PossiblePredecessorLocalIds", "PossibleSuccessorLocalIds")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\');
    }

    public static void main(String[] args) {
        Statistics statistics = new Statistics();
//        statistics.getMultiplePossibleConnections();
//        statistics.copyPostsWithPossibleMultipleConnectionsIntoDirectory();
        statistics.getDifferencesOfRuntimesBetweenMetricComparisons();
    }

    private static List<Path> getGTSamples() {
        ArrayList<Path> pathsToGTSamples = new ArrayList<>(6);
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_1"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_2"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_1+"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_2+"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_Java_17-06_sample_100_1"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_Java_17-06_sample_100_2"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17_06_sample_unclear_matching"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_multiple_possible_links"));
        return pathsToGTSamples;
    }

    private static List<Path> getTestSamples() {
        ArrayList<Path> pathsToTestSamples = new ArrayList<>(12);
        for (int i = 1; i <= 10; i++) {
            // test data
            pathsToTestSamples.add(Paths.get(rootPathToTestSamples.toString(), "PostId_VersionCount_SO_17-06_sample_10000_" + i));
        }
        // sample with many versions (n=100)
        pathsToTestSamples.add(Paths.get(rootPathToTestSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_most_versions"));
        // sample with multiple possible connections (n=498)
        pathsToTestSamples.add(Paths.get(rootPathToTestSamples.toString(), "PostId_VersionCount_SO_17-06_sample_multiple_possible_links"));
        return pathsToTestSamples;
    }

    private void getMultiplePossibleConnections() {

        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(
                pathToMultipleConnectionsFile.toFile()),
                csvFormatMultipleConnections);
             CSVPrinter csvPrinterPosts = new CSVPrinter(new FileWriter(
                     pathToMultipleConnectionsPostsFile.toFile()),
                     csvFormatPostIds
             )) {

            logger.info("Starting extraction of possible connections...");

            Set<PostVersionList> selectedPostVersionLists = new HashSet<>();

            // header is automatically written
            for (Path currentSample : pathsToTestSamples.subList(0, 10)) { // only consider large samples here
                logger.info("Processing sample: " + currentSample);

                Path postVersionListDir = Paths.get(currentSample.toString(), "files");
                List<PostVersionList> postVersionLists = PostVersionList.readFromDirectory(postVersionListDir);

                for (PostVersionList postVersionList : postVersionLists) {
                    for (int i = 0; i < postVersionList.size(); i++) {
                        PostVersion currentVersion = postVersionList.get(i);
                        PostVersion previousVersion = null;
                        if (i > 0) {
                            previousVersion = postVersionList.get(i - 1);
                        }
                        PostVersion nextVersion = null;
                        if (i < postVersionList.size() - 1) {
                            nextVersion = postVersionList.get(i + 1);
                        }

                        for (PostBlockVersion currentVersionPostBlock : currentVersion.getPostBlocks()) {
                            LinkedList<PostBlockVersion> possiblePredecessors = new LinkedList<>();
                            LinkedList<PostBlockVersion> possibleSuccessors = new LinkedList<>();

                            if (previousVersion != null) {
                                // get possible predecessors
                                for (PostBlockVersion previousVersionPostBlock : previousVersion.getPostBlocks()) {
                                    if (currentVersionPostBlock.getContent().equals(previousVersionPostBlock.getContent())) {
                                        possiblePredecessors.add(previousVersionPostBlock);
                                    }
                                }
                            }

                            if (nextVersion != null) {
                                // get possible successors
                                for (PostBlockVersion nextVersionPostBlock : nextVersion.getPostBlocks()) {
                                    if (currentVersionPostBlock.getContent().equals(nextVersionPostBlock.getContent())) {
                                        possibleSuccessors.add(nextVersionPostBlock);
                                    }
                                }
                            }

                            // write version data to CSV file
                            if (possiblePredecessors.size() > 1 || possibleSuccessors.size() > 1) {
                                selectedPostVersionLists.add(postVersionList);

                                csvPrinter.printRecord(
                                        currentVersion.getPostId(),
                                        currentVersion.getPostHistoryId(),
                                        currentVersionPostBlock.getLocalId(),
                                        currentVersionPostBlock.getPostBlockTypeId(),
                                        possiblePredecessors.size(),
                                        possibleSuccessors.size(),
                                        Arrays.toString(possiblePredecessors.stream()
                                                    .map(PostBlockVersion::getLocalId)
                                                    .collect(Collectors.toList())
                                                    .toArray()),
                                        Arrays.toString(possibleSuccessors.stream()
                                                .map(PostBlockVersion::getLocalId)
                                                .collect(Collectors.toList())
                                                .toArray())
                                );
                            }
                        }
                    }
                }
                logger.info("Processed sample: " + currentSample);
            }

            logger.info("Writing list with selected posts to CSV file: " + pathToMultipleConnectionsPostsFile.toFile().getName());

            for (PostVersionList postVersionList : selectedPostVersionLists) {
                csvPrinterPosts.printRecord(
                        postVersionList.getPostId(),
                        postVersionList.getPostTypeId(),
                        postVersionList.size()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method has been used to create the sample "PostId_VersionCount_SO_17-06_sample_100_multiple_possible_links",
    // which contains 100 randomly selected posts with multiple possible connections together with a manually created
    // ground truth
    private void copyPostsWithPossibleMultipleConnectionsIntoDirectory() {
        // get all postIds from file multiple_possible_connections.csv
        Set<Integer> postIds = new HashSet<>();
        try (CSVParser csvParser = CSVParser.parse(
                pathToMultipleConnectionsFile.toFile(),
                StandardCharsets.UTF_8,
                csvFormatMetricComparisonVersion.withFirstRecordAsHeader()
            )) {

            Util.ensureEmptyDirectoryExists(outputDir);

            for (CSVRecord record : csvParser) {
                postIds.add(Integer.valueOf(record.get("PostId")));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Path pathToSample : pathsToTestSamples.subList(0, 10)) { // only consider large samples here

            File file = Paths.get(pathToSample.toString(), "files").toFile();
            File[] postVersionListFilesInFolder = file.listFiles(
                    (dir, name) -> name.matches(PostVersionList.fileNamePattern.pattern())
            );

            assertNotNull(postVersionListFilesInFolder);

            for (File postVersionListFile : postVersionListFilesInFolder) {
                Matcher matcher = PostVersionList.fileNamePattern.matcher(postVersionListFile.getName());
                if (matcher.find()) {
                    int postId = Integer.parseInt(matcher.group(1));
                    if (postIds.contains(postId)) {
                        try {
                            Files.copy(
                                    postVersionListFile.toPath(),
                                    Paths.get(outputDir.toString(), postId + ".csv")
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    private void getDifferencesOfRuntimesBetweenMetricComparisons(){

        // https://stackoverflow.com/a/13515268
        File file_sebastian = new File(Paths.get("output", "2017-11-12_sample_comparison_sebastian").toString());
        File[] files_sebastian = file_sebastian.listFiles((dir1, name) -> name.matches(".*per_post\\.csv"));

        // https://stackoverflow.com/a/13515268
        File file_lorik = new File(Paths.get("output", "2017-11-12_sample_comparison_lorik").toString());
        File[] files_lorik = file_lorik.listFiles((dir, name) -> name.matches(".*per_post\\.csv"));

        for (int i=0; i<files_sebastian.length; i++) {
            if (!files_sebastian[i].getName().toLowerCase().equals(files_lorik[i].getName().toLowerCase())) { // Appearing difference between samples
                throw new IllegalArgumentException(
                        "Files need to be over same samples but there was a difference:"
                                + "\n" + files_sebastian[i].getName()
                                + "\n" + files_lorik[i].getName());
            }
        }

        List<MeasuredRuntimes> measuredRuntimes_sebastian = parseMeasuredTimes(files_sebastian);
        List<MeasuredRuntimes> measuredRuntimes_lorik = parseMeasuredTimes(files_lorik);

        // TODO
        // sorting lists reduces runtime from n*n to n log n
        measuredRuntimes_sebastian.sort((o1, o2) -> measuredRuntimes_sebastian.get(0).compare(o1,o2));
        measuredRuntimes_lorik.sort((o1, o2) -> measuredRuntimes_lorik.get(0).compare(o1,o2));

        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(Paths.get("output", "differences.csv").toString()), CSVFormat.DEFAULT
                .withHeader("postId", "metric", "threshold", "runtimeTextTotalDifference", "runtimeTextUserDifference", "runtimeCodeTotalDifference", "runtimeCodeUserDifference")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL) // TODO: Adjust with right quote mode
                .withEscape('\\')
                .withNullString("null"))) {

            for (int i=0; i<measuredRuntimes_sebastian.size(); i++) {
                assertEquals(measuredRuntimes_sebastian.get(i).postId, measuredRuntimes_lorik.get(i).postId);
                assertEquals(measuredRuntimes_sebastian.get(i).metricName, measuredRuntimes_lorik.get(i).metricName);
                assertEquals(measuredRuntimes_sebastian.get(i).threshold, measuredRuntimes_lorik.get(i).threshold);

                csvPrinter.printRecord(
                        measuredRuntimes_sebastian.get(i).postId,
                        measuredRuntimes_sebastian.get(i).metricName,
                        measuredRuntimes_sebastian.get(i).threshold,
                        Math.abs(measuredRuntimes_sebastian.get(i).runtimeTextTotal - measuredRuntimes_lorik.get(i).runtimeTextTotal),
                        Math.abs(measuredRuntimes_sebastian.get(i).runtimeTextUser - measuredRuntimes_lorik.get(i).runtimeTextUser),
                        Math.abs(measuredRuntimes_sebastian.get(i).runtimeCodeTotal - measuredRuntimes_lorik.get(i).runtimeCodeTotal),
                        Math.abs(measuredRuntimes_sebastian.get(i).runtimeCodeUser - measuredRuntimes_lorik.get(i).runtimeCodeUser)
                );
            }

            csvPrinter.flush();
            // csvPrinter.close();

            } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class MeasuredRuntimes implements Comparator{
        int postId;
        String metricName;
        double threshold;
        long runtimeTextTotal;
        long runtimeTextUser;
        long runtimeCodeTotal;
        long runtimeCodeUser;

        MeasuredRuntimes(int postId, String metricName, double threshold, long runtimeTextTotal, long runtimeTextUser, long runtimeCodeTotal, long runtimeCodeUser) {
            this.postId = postId;
            this.metricName = metricName;
            this.threshold = threshold;
            this.runtimeTextTotal = runtimeTextTotal;
            this.runtimeTextUser = runtimeTextUser;
            this.runtimeCodeTotal = runtimeCodeTotal;
            this.runtimeCodeUser = runtimeCodeUser;
        }


        @Override
        public int compare(Object o1, Object o2) {
            if (((MeasuredRuntimes)o1).postId < ((MeasuredRuntimes)o2).postId) {
                return -1;
            } else if (((MeasuredRuntimes)o1).postId > ((MeasuredRuntimes)o2).postId) {
                return 1;
            } else if (((MeasuredRuntimes)o1).metricName.compareTo(((MeasuredRuntimes)o2).metricName) < 0) {
                return -1;
            } else if (((MeasuredRuntimes)o1).metricName.compareTo(((MeasuredRuntimes)o2).metricName) > 0) {
                return 1;
            } else return Double.compare(((MeasuredRuntimes)o1).threshold, ((MeasuredRuntimes)o2).threshold);
        }
    }

    private List<MeasuredRuntimes> parseMeasuredTimes(File[] files){
        List<MeasuredRuntimes> measuredRuntimes = new ArrayList<>();
        for (File file : files) {
            try (CSVParser csvParser = new CSVParser(
                    new FileReader(file),
                    //CSVFormat.DEFAULT.withHeader("Sample", "Metric", "Threshold", "PostId", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections", "RuntimeTextTotal", "RuntimeTextUser", "TextBlockVersionCount", "PossibleConnectionsText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "RuntimeCodeTotal", "RuntimeCodeUser", "CodeBlockVersionCount", "PossibleConnectionsCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode"))) {
                    csvFormatMetricComparisonPost.withHeader())) {

                for (CSVRecord currentRecord : csvParser) {
                    int postId = Integer.parseInt(currentRecord.get("PostId"));
                    String metric = currentRecord.get("Metric");
                    double threshold = Double.parseDouble(currentRecord.get("Threshold"));
                    long runtimeTextTotal = Long.parseLong(currentRecord.get("RuntimeTextTotal"));
                    long runtimeTextUser = Long.parseLong(currentRecord.get("RuntimeTextUser"));
                    long runtimeCodeTotal = Long.parseLong(currentRecord.get("RuntimeCodeTotal"));
                    long runtimeCodeUser = Long.parseLong(currentRecord.get("RuntimeCodeUser"));

                    measuredRuntimes.add(
                            new MeasuredRuntimes(postId, metric, threshold, runtimeTextTotal, runtimeTextUser, runtimeCodeTotal, runtimeCodeUser)
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return measuredRuntimes;
    }

}
