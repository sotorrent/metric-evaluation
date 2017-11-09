package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.util.Util;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class Main {

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Metrics Comparison)\n");

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

                        new Thread(manager).start();
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
