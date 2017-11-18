package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.util.Util;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.*;
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
//        statistics.getDifferencesOfRuntimesBetweenMetricComparisons();

        statistics.createPostIdVersionCount_perMetricThreshold(
                Paths.get("output", "PostId_VersionCount_SO_17-06_sample_100_aggregated.csv"),
                Paths.get("output"),
                true,
                false);
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

    // This method compares different directories of results of metric comparisons and creates a csv file with some differences
    private void getDifferencesOfRuntimesBetweenMetricComparisons() {

        // Add all paths of computed comparisons
        List<Path> pathsToOutputDirectories = new ArrayList<>();
        pathsToOutputDirectories.add(Paths.get("output", "2017-11-14_sample_comparison_sebastian-4")); // base directory is first element
        pathsToOutputDirectories.add(Paths.get("output", "2017-11-14_sample_comparison_lorik-3"));

        List<File[]> directoryFiles = new ArrayList<>();
        for (Path path : pathsToOutputDirectories) {
            File file = new File(path.toString());
            directoryFiles.add(file.listFiles((dir, name) -> name.matches(".*per_post\\.csv"))); // https://stackoverflow.com/a/13515268
        }


        // check whether all files are available
        for (int i = 1; i < directoryFiles.size(); i++) {
            for (int j = 0; j < directoryFiles.get(i).length; j++) {
                if (!directoryFiles.get(0)[j].getName().toLowerCase().equals(directoryFiles.get(i)[j].getName().toLowerCase())) { // Appearing difference between samples
                    throw new IllegalArgumentException(
                            "Files need to be over same samples but there was a difference:"
                                    + "\n" + directoryFiles.get(0)[j].getName()
                                    + "\n" + directoryFiles.get(i)[j].getName()
                    );
                }
            }
        }

        // parse measured data for comparison
        List<List<MeasuredRuntimes>> listOfListsOfMeasuredRuntimesFromDifferentComparisons = new ArrayList<>();
        for (File[] files : directoryFiles) {
            List<MeasuredRuntimes> measuredRuntimesList = parseMeasuredTimes(files);
            measuredRuntimesList.sort((o1, o2) -> measuredRuntimesList.get(0).compare(o1, o2));
            listOfListsOfMeasuredRuntimesFromDifferentComparisons.add(measuredRuntimesList);   // sorting lists reduces runtime from n*n to n log n for later comparison
        }

        // print csv file
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(Paths.get("output", "differences_between_runtimes_metric_comparisons.csv").toString()), CSVFormat.DEFAULT
                .withHeader(
                        "sample", "metric", "threshold",

                        "runtimeTotalTextMinimum", // "runtimeUserTextMinimum", "runtimeCPUTextMinimum",
                        "runtimeTextTotalMaxDifference", // "runtimeTextUserMaxDifference", "runtimeTextCPUMaxDifference",
                        "runtimeTextTotalDeviationWithConstantBaseAndAverage", // "runtimeTextUserDeviationWithConstantBaseAndAverage",
                        "runtimeTextTotalDeviationWithMinAndMax", // "runtimeTextUserDeviationWithMinAndMax",

                        "runtimeTotalCodeMinimum", // "runtimeUserCodeMinimum", "runtimeCPUCodeMinimum",
                        "runtimeCodeTotalMaxDifference", // "runtimeCodeUserMaxDifference", "runtimeCodeUserMaxDifference",
                        "runtimeCodeTotalDeviationWithConstantBaseAndAverage", // "runtimeCodeUserDeviationWithConstantBaseAndAverage",
                        "runtimeCodeTotalDeviationWithMinAndMax" // "runtimeCodeUserDeviationWithMinAndMax"
                        )
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL) // TODO: Adjust with right quote mode
                .withEscape('\\')
                .withNullString("null"))) {


            List<MeasuredRuntimes> baseComparison = listOfListsOfMeasuredRuntimesFromDifferentComparisons.get(0);
            for (int j = 0; j < baseComparison.size(); j++) {

                long minRuntimeTextTotal = baseComparison.get(j).runtimeTextTotal;
                long minRuntimeCodeTotal = baseComparison.get(j).runtimeCodeTotal;

                long maxRuntimeTextTotal = baseComparison.get(j).runtimeTextTotal;
                long maxRuntimeCodeTotal = baseComparison.get(j).runtimeCodeTotal;

                double averageRuntimeTextTotal = 0;
                double averageRuntimeCodeTotal = 0;

                for (int i = 1; i < listOfListsOfMeasuredRuntimesFromDifferentComparisons.size(); i++) {

                    List<MeasuredRuntimes> currentComparison = listOfListsOfMeasuredRuntimesFromDifferentComparisons.get(i);

                    // validate records
                    assertEquals(baseComparison.get(j).postId, currentComparison.get(j).postId);
                    assertEquals(baseComparison.get(j).metricName, currentComparison.get(j).metricName);
                    assertEquals(baseComparison.get(j).threshold, currentComparison.get(j).threshold);

                    minRuntimeTextTotal = Math.min(minRuntimeTextTotal, currentComparison.get(i).runtimeTextTotal);
                    minRuntimeCodeTotal = Math.min(minRuntimeCodeTotal, currentComparison.get(i).runtimeCodeTotal);

                    maxRuntimeTextTotal = Math.max(maxRuntimeTextTotal, currentComparison.get(i).runtimeTextTotal);
                    maxRuntimeCodeTotal = Math.max(maxRuntimeCodeTotal, currentComparison.get(i).runtimeCodeTotal);

                    averageRuntimeTextTotal += currentComparison.get(j).runtimeTextTotal;
                    averageRuntimeCodeTotal += currentComparison.get(j).runtimeCodeTotal;
                }

                averageRuntimeTextTotal /= (listOfListsOfMeasuredRuntimesFromDifferentComparisons.size()-1);
                averageRuntimeCodeTotal /= (listOfListsOfMeasuredRuntimesFromDifferentComparisons.size()-1);

                /*
                        "metric", "threshold",

                        "runtimeTotalTextMinimum",
                        "runtimeTextTotalMaxDifference",
                        "runtimeTextTotalDeviationWithConstantBaseAndAverage",
                        "runtimeTextTotalDeviationWithMinAndMax",

                        "runtimeTotalCodeMinimum",
                        "runtimeCodeTotalMaxDifference",
                        "runtimeCodeTotalDeviationWithConstantBaseAndAverage",
                        "runtimeCodeTotalDeviationWithMinAndMax",
                 */
                csvPrinter.printRecord(
                        baseComparison.get(j).sample,
                        baseComparison.get(j).metricName,
                        baseComparison.get(j).threshold,

                        minRuntimeTextTotal,
                        maxRuntimeTextTotal - minRuntimeTextTotal,
                        averageRuntimeTextTotal / (double)baseComparison.get(j).runtimeTextTotal, // TODO: handle division by 0
                        maxRuntimeTextTotal != 0 ? ((double)minRuntimeTextTotal / maxRuntimeTextTotal) : 1,

                        minRuntimeCodeTotal,
                        maxRuntimeCodeTotal - minRuntimeCodeTotal,
                        (double)baseComparison.get(j).runtimeCodeTotal / averageRuntimeCodeTotal, // TODO: handle division by 0
                        maxRuntimeCodeTotal != 0 ? ((double)minRuntimeCodeTotal / maxRuntimeCodeTotal) : 1
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method parses an aggregated file from metric comparison results and converts it so that every metric and threshold have a row entry
    private void createPostIdVersionCount_perMetricThreshold(
            Path pathToFile_perPostAggregated,
            Path pathToOutputDirectory,
            boolean divideBySamples,
            boolean relativeToNumberOfPosts){

        List<MetricThresholdAggregated> metricThresholdAggregateds = new ArrayList<>();

        try (CSVParser csvParser = new CSVParser(
                new FileReader(pathToFile_perPostAggregated.toString()),
                /*
                CSVFormat.DEFAULT.withHeader(
                "Sample", "Metric", "Threshold",
                "PostId", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",

                "RuntimeTextTotal", "RuntimeTextUser",
                "TextBlockVersionCount", "PossibleConnectionsText",
                "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText",

                "RuntimeCodeTotal", "RuntimeCodeUser",
                "CodeBlockVersionCount", "PossibleConnectionsCode",
                "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode"))) {
                */
                csvFormatMetricComparisonPost.withHeader())) {

            for (CSVRecord currentRecord : csvParser) {

                Set<Integer> postBlockTypeIdFilter = new HashSet<>();
                postBlockTypeIdFilter.add(TextBlockVersion.postBlockTypeId);
                postBlockTypeIdFilter.add(CodeBlockVersion.postBlockTypeId);

                String sample = currentRecord.get("Sample");
                String metric = currentRecord.get("Metric");
                double threshold = Double.parseDouble(currentRecord.get("Threshold"));

                long runtimeTextTotal = Long.parseLong(currentRecord.get("RuntimeTextTotal"));

                Integer truePositivesText = 0;
                Integer trueNegativesText = 0;
                Integer falsePositivesText = 0;
                Integer falseNegativesText = 0;
                try {
                    truePositivesText = Integer.parseInt(currentRecord.get("TruePositivesText"));
                    trueNegativesText = Integer.parseInt(currentRecord.get("TrueNegativesText"));
                    falsePositivesText = Integer.parseInt(currentRecord.get("FalsePositivesText"));
                    falseNegativesText = Integer.parseInt(currentRecord.get("FalseNegativesText"));
                } catch (NumberFormatException e){
                    postBlockTypeIdFilter.remove(TextBlockVersion.postBlockTypeId);
                }

                long runtimeCodeTotal = Long.parseLong(currentRecord.get("RuntimeCodeTotal"));

                Integer truePositivesCode = 0;
                Integer trueNegativesCode = 0;
                Integer falsePositivesCode = 0;
                Integer falseNegativesCode = 0;
                try {
                    truePositivesCode = Integer.parseInt(currentRecord.get("TruePositivesCode"));
                    trueNegativesCode = Integer.parseInt(currentRecord.get("TrueNegativesCode"));
                    falsePositivesCode = Integer.parseInt(currentRecord.get("FalsePositivesCode"));
                    falseNegativesCode = Integer.parseInt(currentRecord.get("FalseNegativesCode"));
                } catch (Exception e){
                    postBlockTypeIdFilter.remove(CodeBlockVersion.postBlockTypeId);
                }


                MetricThresholdAggregated tmpMetricThresholdAggregated = new MetricThresholdAggregated(
                        sample,
                        metric,
                        threshold,

                        runtimeTextTotal,

                        truePositivesText,
                        trueNegativesText,
                        falsePositivesText,
                        falseNegativesText,

                        runtimeCodeTotal,

                        truePositivesCode,
                        trueNegativesCode,
                        falsePositivesCode,
                        falseNegativesCode);

                integrateInList(metricThresholdAggregateds, tmpMetricThresholdAggregated, postBlockTypeIdFilter, divideBySamples);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] header = {
                "sample", "metric", "threshold",
                "numberOfTextPostsWithThisMetric",
                "runtimeTextTotal",
                "truePositivesText", "trueNegativesText", "falsePositivesText","falseNegativesText",
                "numberOfCodePostsWithThisMetric",
                "runtimeCodeTotal",
                "truePositivesCode", "trueNegativesCode", "falsePositivesCode", "falseNegativesCode"};

        if (relativeToNumberOfPosts) {
            for (int i=4; i<header.length; i++) {
                if (i != 9) {
                    header[i] += "Relative";
                }
            }
        }

        CSVFormat csvFormat = divideBySamples ?
                CSVFormat.DEFAULT.withHeader(header) :
                CSVFormat.DEFAULT.withHeader(Arrays.copyOfRange(header, 1, header.length)); // https://stackoverflow.com/a/4439612

        // print csv file
        try (CSVPrinter csvPrinter = new CSVPrinter(
                new FileWriter(
                        pathToOutputDirectory.toString() + "\\PostId_VersionCount_SO_17-06_sample_100_per_metricThreshold" + (divideBySamples ? "Sample" : "") + (relativeToNumberOfPosts ? "_relativeValues" : "") + ".csv"),
                csvFormat
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL) // TODO: Adjust with right quote mode
                .withEscape('\\')
                .withNullString("null"))) {

            for (MetricThresholdAggregated metricThresholdAggregated : metricThresholdAggregateds) {

                List<Object> columnEntries = new ArrayList<>();

                if (divideBySamples) {
                    columnEntries.add(metricThresholdAggregated.sample);
                }

                columnEntries.add(metricThresholdAggregated.metric);
                columnEntries.add(metricThresholdAggregated.threshold);
                columnEntries.add(metricThresholdAggregated.numberOfPostsText);


                if (relativeToNumberOfPosts) {
                    columnEntries.add(((double) metricThresholdAggregated.runtimeTextTotal) / metricThresholdAggregated.numberOfPostsText);
                    columnEntries.add(((double) metricThresholdAggregated.truePositivesText) / metricThresholdAggregated.numberOfPostsText);
                    columnEntries.add(((double) metricThresholdAggregated.trueNegativesText) / metricThresholdAggregated.numberOfPostsText);
                    columnEntries.add(((double) metricThresholdAggregated.falsePositivesText) / metricThresholdAggregated.numberOfPostsText);
                    columnEntries.add(((double) metricThresholdAggregated.falseNegativesText) / metricThresholdAggregated.numberOfPostsText);
                } else {
                    columnEntries.add(metricThresholdAggregated.runtimeTextTotal);
                    columnEntries.add(metricThresholdAggregated.truePositivesText);
                    columnEntries.add(metricThresholdAggregated.trueNegativesText);
                    columnEntries.add(metricThresholdAggregated.falsePositivesText);
                    columnEntries.add(metricThresholdAggregated.falseNegativesText);
                }

                columnEntries.add(metricThresholdAggregated.numberOfPostsCode);

                if (relativeToNumberOfPosts) {
                    columnEntries.add(((double)metricThresholdAggregated.runtimeCodeTotal) / metricThresholdAggregated.numberOfPostsCode);
                    columnEntries.add(((double)metricThresholdAggregated.truePositivesCode) / metricThresholdAggregated.numberOfPostsCode);
                    columnEntries.add(((double)metricThresholdAggregated.trueNegativesCode) / metricThresholdAggregated.numberOfPostsCode);
                    columnEntries.add(((double)metricThresholdAggregated.falsePositivesCode) / metricThresholdAggregated.numberOfPostsCode);
                    columnEntries.add(((double)metricThresholdAggregated.falseNegativesCode) / metricThresholdAggregated.numberOfPostsCode);
                } else {
                    columnEntries.add(metricThresholdAggregated.runtimeCodeTotal);
                    columnEntries.add(metricThresholdAggregated.truePositivesCode);
                    columnEntries.add(metricThresholdAggregated.trueNegativesCode);
                    columnEntries.add(metricThresholdAggregated.falsePositivesCode);
                    columnEntries.add(metricThresholdAggregated.falseNegativesCode);
                }

                csvPrinter.printRecord(columnEntries);
            }

            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    // --------------------------------------------------------------------------------------
    // helper methods and helper classes from here

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

    private class MeasuredRuntimes implements Comparator{
        String sample;
        int postId;
        String metricName;
        double threshold;
        long runtimeTextTotal;
        long runtimeCodeTotal;

        MeasuredRuntimes(String sample, int postId, String metricName, double threshold, long runtimeTextTotal, long runtimeCodeTotal) {
            this.sample = sample;
            this.postId = postId;
            this.metricName = metricName;
            this.threshold = threshold;
            this.runtimeTextTotal = runtimeTextTotal;
            this.runtimeCodeTotal = runtimeCodeTotal;
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
                    String sample = currentRecord.get("Sample");
                    int postId = Integer.parseInt(currentRecord.get("PostId"));
                    String metric = currentRecord.get("Metric");
                    double threshold = Double.parseDouble(currentRecord.get("Threshold"));
                    long runtimeTextTotal = Long.parseLong(currentRecord.get("RuntimeTextTotal"));
                    long runtimeTextUser = Long.parseLong(currentRecord.get("RuntimeTextUser"));
                    long runtimeCodeTotal = Long.parseLong(currentRecord.get("RuntimeCodeTotal"));
                    long runtimeCodeUser = Long.parseLong(currentRecord.get("RuntimeCodeUser"));

                    measuredRuntimes.add(
                            new MeasuredRuntimes(sample, postId, metric, threshold, runtimeTextTotal, runtimeCodeTotal)
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return measuredRuntimes;
    }

    private static double round(double value, int decimalDigits) {

        int tmp = 1;
        while (decimalDigits > 0) {
            tmp *= 10;
            decimalDigits--;
        }

        return ((double)((int)(value * tmp))) / tmp;
    }

    private class MetricThresholdAggregated {

        String sample;
        String metric;
        double threshold;

        int numberOfPostsText = 0;

        long runtimeTextTotal;

        Integer truePositivesText;
        Integer trueNegativesText;
        Integer falsePositivesText;
        Integer falseNegativesText;


        int numberOfPostsCode = 0;

        long runtimeCodeTotal;

        Integer truePositivesCode;
        Integer trueNegativesCode;
        Integer falsePositivesCode;
        Integer falseNegativesCode;

        MetricThresholdAggregated(
                String sample,
                String metric,
                double threshold,

                long runtimeTextTotal,

                Integer truePositivesText,
                Integer trueNegativesText,
                Integer falsePositivesText,
                Integer falseNegativesText,

                long runtimeCodeTotal,

                Integer truePositivesCode,
                Integer trueNegativesCode,
                Integer falsePositivesCode,
                Integer falseNegativesCode){

            this.sample = sample;
            this.metric = metric;
            this.threshold = threshold;

            this.numberOfPostsText = numberOfPostsText;

            this.runtimeTextTotal = runtimeTextTotal;

            this.truePositivesText = truePositivesText;
            this.trueNegativesText = trueNegativesText;
            this.falsePositivesText = falsePositivesText;
            this.falseNegativesText = falseNegativesText;

            this.numberOfPostsCode = numberOfPostsCode;

            this.runtimeCodeTotal = runtimeCodeTotal;

            this.truePositivesCode = truePositivesCode;
            this.trueNegativesCode = trueNegativesCode;
            this.falsePositivesCode = falsePositivesCode;
            this.falseNegativesCode = falseNegativesCode;
        }

        private boolean definesSameType(Object other, boolean divideBySamples) {
            return other instanceof MetricThresholdAggregated
                    && (!divideBySamples
                        || (Objects.equals(this.sample, ((MetricThresholdAggregated) other).sample)))
                    && (Objects.equals(this.metric, ((MetricThresholdAggregated) other).metric))
                    && this.threshold == ((MetricThresholdAggregated) other).threshold;
        }
    }

    private static void integrateInList(
            List<MetricThresholdAggregated> metricThresholdAggregateds,
            MetricThresholdAggregated newMetricThresholdAggregated,
            Set<Integer> postBlockTypeId,
            boolean divideBySamples){
        for (MetricThresholdAggregated tmpMetricThresholdAggregated : metricThresholdAggregateds) {
            if (newMetricThresholdAggregated.definesSameType(tmpMetricThresholdAggregated, divideBySamples)) {

                if (postBlockTypeId.contains(TextBlockVersion.postBlockTypeId)) {
                    tmpMetricThresholdAggregated.runtimeTextTotal += newMetricThresholdAggregated.runtimeTextTotal;

                    tmpMetricThresholdAggregated.truePositivesText += newMetricThresholdAggregated.truePositivesText;
                    tmpMetricThresholdAggregated.trueNegativesText += newMetricThresholdAggregated.trueNegativesText;
                    tmpMetricThresholdAggregated.falsePositivesText += newMetricThresholdAggregated.falsePositivesText;
                    tmpMetricThresholdAggregated.falseNegativesText += newMetricThresholdAggregated.falseNegativesText;

                    tmpMetricThresholdAggregated.numberOfPostsText++;
                }

                if (postBlockTypeId.contains(CodeBlockVersion.postBlockTypeId)) {
                    tmpMetricThresholdAggregated.runtimeCodeTotal += newMetricThresholdAggregated.runtimeCodeTotal;

                    tmpMetricThresholdAggregated.truePositivesCode += newMetricThresholdAggregated.truePositivesCode;
                    tmpMetricThresholdAggregated.trueNegativesCode += newMetricThresholdAggregated.trueNegativesCode;
                    tmpMetricThresholdAggregated.falsePositivesCode += newMetricThresholdAggregated.falsePositivesCode;
                    tmpMetricThresholdAggregated.falseNegativesCode += newMetricThresholdAggregated.falseNegativesCode;

                    tmpMetricThresholdAggregated.numberOfPostsCode++;
                }

                return;
            }
        }

        metricThresholdAggregateds.add(newMetricThresholdAggregated);
    }
}
