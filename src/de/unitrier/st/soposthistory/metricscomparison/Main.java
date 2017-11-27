package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.util.Util;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

class Main {

    private static Logger logger;

    static {
        // configure logger
        try {
            logger = getClassLogger(Main.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Metrics Comparison)");

        Options options = new Options();

        Option gtDirOption = new Option("s", "samples-dir", true, "path to directory with samples");
        gtDirOption.setRequired(true);
        options.addOption(gtDirOption);

        Option outputDirOption = new Option("o", "output-dir", true, "path to output directory");
        outputDirOption.setRequired(true);
        options.addOption(outputDirOption);

        Option threadCountOption = new Option("t", "thread-count", true, "maximum number of threads used for the comparison");
        threadCountOption.setRequired(true);
        options.addOption(threadCountOption);

        CommandLineParser commandLineParser = new DefaultParser();
        HelpFormatter commandLineFormatter = new HelpFormatter();
        CommandLine commandLine;

        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            commandLineFormatter.printHelp("SOPostHistory (Metrics Comparison)", options);
            System.exit(1);
            return;
        }

        Path samplesDir = Paths.get(commandLine.getOptionValue("samples-dir"));
        Path outputDir = Paths.get(commandLine.getOptionValue("output-dir"));
        int threadCount = Integer.parseInt(commandLine.getOptionValue("thread-count"));

        logger.info("Creating thread pool with at most " + threadCount + " threads...");
        // it is recommended to process only one sample at a time to prevent a bias in the runtime measurements
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

        logger.info("Creating MetricEvaluationManagers for samples...");
        List<MetricEvaluationManager> managers = new LinkedList<>();

        try (Stream<Path> paths = Files.list(samplesDir)) {

            Util.ensureEmptyDirectoryExists(outputDir);

            paths.forEach(
                    path -> {
                        String name = path.toFile().getName();
                        Path pathToPostIdList = Paths.get(path.toString(), name + ".csv");
                        Path pathToPostHistory = Paths.get(path.toString(), "files");
                        Path pathToGroundTruth = Paths.get(path.toString(), "completed");

                        MetricEvaluationManager manager = MetricEvaluationManager.DEFAULT
                                .withName(name)
                                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                                .withOutputDirPath(outputDir)
                                .initialize();

                        managers.add(manager);

                        logger.info("Adding manager for sample " + manager.getSampleName() + " to thread pool...");
                        threadPool.execute(new Thread(manager));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Waiting for termination of thread pool...");
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, TimeUnit.DAYS);
            logger.info("Thread pool terminated, all samples evaluated.");
            logger.info("Saving aggregated results over all samples...");

            // output file aggregated over all samples
            File outputFileAggregated= Paths.get(outputDir.toString(), "MetricComparison_aggregated.csv").toFile();
            if (outputFileAggregated.exists()) {
                if (!outputFileAggregated.delete()) {
                    throw new IllegalStateException("Error while deleting output file: " + outputFileAggregated);
                }
            }

            MetricEvaluationManager.aggregateAndWriteSampleResults(managers, outputFileAggregated);

        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            e.printStackTrace();
        }
    }
}
