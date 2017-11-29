package de.unitrier.st.soposthistory.metricscomparison;

import org.apache.commons.cli.*;

import de.unitrier.st.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

class Main {

    private static Logger logger;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(Main.class);
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

        List<MetricEvaluationManager> managers = MetricEvaluationManager.createManagersFromSampleDirectories(
                samplesDir, outputDir, true
        );

        for (MetricEvaluationManager manager : managers) {
            logger.info("Adding manager for sample " + manager.getSampleName() + " to thread pool...");
            threadPool.execute(new Thread(manager));
        }

        logger.info("Waiting for termination of thread pool...");
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, TimeUnit.DAYS);
            logger.info("Thread pool terminated, all samples evaluated.");
            logger.info("Saving aggregated results over all samples...");

            // output file aggregated over all samples
            Path outputFileAggregated= Paths.get(outputDir.toString(), "MetricComparison_aggregated.csv");
            Util.deleteFileIfExists(outputFileAggregated);

            MetricEvaluationManager.aggregateAndWriteSampleResults(managers, outputFileAggregated.toFile());

        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
