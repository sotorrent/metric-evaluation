package de.unitrier.st.soposthistory.metricscomparison;

import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

class Main {

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Metrics Comparison)\n");

        Options options = new Options();

        Option gtDir = new Option("gt", "gt-dir", true, "path to directory with GT files");
        gtDir.setRequired(true);
        options.addOption(gtDir);

        Option soDir = new Option("so", "so-dir", true, "path to directory with SO post history files");
        soDir.setRequired(true);
        options.addOption(soDir);

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

        Path gtDirPath = Paths.get(commandLine.getOptionValue("gt-dir"));
        Path soDirPath = Paths.get(commandLine.getOptionValue("so-dir"));
        Path outputDirPath = Paths.get(commandLine.getOptionValue("output-dir"));

//        List<GroundTruth> gtList = GroundTruth.readFromDirectory(gtDirPath);
//
//        for (GroundTruth gt : gtList) {
//            System.out.println(gt);
//        }
//
//        List<PostVersionList> postVersionList = PostVersionList.readFromDirectory(soDirPath);
//
//        for (PostVersionList so : postVersionList) {
//            System.out.println(so);
//        }

    }
}
