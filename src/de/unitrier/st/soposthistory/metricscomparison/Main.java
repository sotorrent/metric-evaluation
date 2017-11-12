package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.util.Util;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        Option gtDir = new Option("s", "samples-dir", true, "path to directory with samples");
        gtDir.setRequired(true);
        options.addOption(gtDir);

        Option outputDir = new Option("o", "output-dir", true, "path to output directory");
        outputDir.setRequired(true);
        options.addOption(outputDir);

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

        Path samplesDirPath = Paths.get(commandLine.getOptionValue("samples-dir"));
        Path outputDirPath = Paths.get(commandLine.getOptionValue("output-dir"));

        // execute at most two thread at a time (not more because of runtime measurement)
        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        logger.info("Creating MetricComparisonManagers from samples...");

        try (Stream<Path> paths = Files.list(samplesDirPath)) {

            Util.ensureEmptyDirectoryExists(outputDirPath);

            paths.forEach(
                    path -> {
                        String name = path.toFile().getName();
                        Path pathToPostIdList = Paths.get(path.toString(), name + ".csv");
                        Path pathToPostHistory = Paths.get(path.toString(), "files");
                        Path pathToGroundTruth = Paths.get(path.toString(), "completed");

                        MetricComparisonManager manager = MetricComparisonManager.DEFAULT
                                .withName(name)
                                .withInputPaths(pathToPostIdList, pathToPostHistory, pathToGroundTruth)
                                .withOutputDirPath(outputDirPath)
                                .initialize();

                        logger.info("Adding manager " + manager.getName() + " to thread pool...");
                        threadPool.execute(new Thread(manager));
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Shutting down thread pool...");
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, TimeUnit.DAYS);
            logger.info("Thread pool terminated.");
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            e.printStackTrace();
        }
    }
}
