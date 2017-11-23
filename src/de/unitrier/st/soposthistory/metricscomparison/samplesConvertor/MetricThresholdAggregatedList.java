package de.unitrier.st.soposthistory.metricscomparison.samplesConvertor;

import org.apache.commons.csv.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.unitrier.st.soposthistory.metricscomparison.MetricComparisonManager.csvFormatMetricComparisonPost;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricThresholdAggregatedList extends ArrayList<MetricThresholdAggregated> {

    private int maxFalsePositivesText = 0;
    private int maxFalseNegativesText = 0;
    private int maxFailedPredecessorComparisonsText = 0;

    private int commonDenominatorText = 0;

    private int maxFalsePositivesCode = 0;
    private int maxFalseNegativesCode = 0;
    private int maxFailedPredecessorComparisonsCode = 0;

    private int commonDenominatorCode = 0;


    MetricThresholdAggregatedList() {}


    private MetricThresholdAggregatedList parseAggregatedSample(Path pathToFile_perPostAggregated, boolean divideBySamples) {


        MetricThresholdAggregatedList aggregatedMetrics = new MetricThresholdAggregatedList();


        try (CSVParser csvParser = new CSVParser( new FileReader(pathToFile_perPostAggregated.toString()), csvFormatMetricComparisonPost.withHeader())) {

            for (CSVRecord currentRecord : csvParser) {

                String sample = currentRecord.get("Sample");
                String metricType = currentRecord.get("MetricType");
                String metric = currentRecord.get("Metric");
                double threshold = Double.parseDouble(currentRecord.get("Threshold"));

                int postVersionCount = Integer.parseInt(currentRecord.get("PostVersionCount"));


                long runtimeText = Long.parseLong(currentRecord.get("RuntimeText"));

                Integer textBlockVersionCount = Integer.parseInt(currentRecord.get("TextBlockVersionCount"));
                Integer possibleConnectionsText = Integer.parseInt(currentRecord.get("PossibleConnectionsText"));

                Integer truePositivesText = Integer.parseInt(currentRecord.get("TruePositivesText"));
                Integer trueNegativesText = Integer.parseInt(currentRecord.get("TrueNegativesText"));
                Integer falsePositivesText = Integer.parseInt(currentRecord.get("FalsePositivesText"));
                Integer falseNegativesText = Integer.parseInt(currentRecord.get("FalseNegativesText"));

                Integer failedPredecessorComparisonsText = Integer.parseInt(currentRecord.get("FailedPredecessorComparisonsText"));


                long runtimeCode = Long.parseLong(currentRecord.get("RuntimeCode"));

                Integer codeBlockVersionCount = Integer.parseInt(currentRecord.get("CodeBlockVersionCount"));
                Integer possibleConnectionsCode = Integer.parseInt(currentRecord.get("PossibleConnectionsCode"));

                Integer truePositivesCode = Integer.parseInt(currentRecord.get("TruePositivesCode"));
                Integer trueNegativesCode = Integer.parseInt(currentRecord.get("TrueNegativesCode"));
                Integer falsePositivesCode = Integer.parseInt(currentRecord.get("FalsePositivesCode"));
                Integer falseNegativesCode = Integer.parseInt(currentRecord.get("FalseNegativesCode"));

                Integer failedPredecessorComparisonsCode = Integer.parseInt(currentRecord.get("FailedPredecessorComparisonsCode"));


                MetricThresholdAggregated tmpMetricThresholdAggregated = new MetricThresholdAggregated(
                        sample, metricType, metric, threshold,
                        postVersionCount,

                        runtimeText,
                        textBlockVersionCount, possibleConnectionsText,
                        truePositivesText, trueNegativesText, falsePositivesText, falseNegativesText,
                        failedPredecessorComparisonsText,

                        runtimeCode,
                        codeBlockVersionCount, possibleConnectionsCode,
                        truePositivesCode, trueNegativesCode, falsePositivesCode, falseNegativesCode,
                        failedPredecessorComparisonsCode);

                integrateInList(aggregatedMetrics, tmpMetricThresholdAggregated, divideBySamples);


                maxFalsePositivesText = Math.max(maxFalsePositivesText, falsePositivesText);
                maxFalseNegativesText = Math.max(maxFalseNegativesText, falseNegativesText);
                maxFailedPredecessorComparisonsText = Math.max(maxFailedPredecessorComparisonsText, failedPredecessorComparisonsText);

                maxFalsePositivesCode = Math.max(maxFalsePositivesCode, falsePositivesCode);
                maxFalseNegativesCode = Math.max(maxFalseNegativesCode, falseNegativesCode);
                maxFailedPredecessorComparisonsCode = Math.max(maxFailedPredecessorComparisonsCode, failedPredecessorComparisonsCode);


                int postBlockVersionCount = Integer.parseInt(currentRecord.get("PostBlockVersionCount"));
                int possibleConnections = Integer.parseInt(currentRecord.get("PossibleConnections"));

                assertEquals(postBlockVersionCount, textBlockVersionCount + codeBlockVersionCount);
                assertEquals(possibleConnections, possibleConnectionsText + possibleConnectionsCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        aggregatedMetrics.commonDenominatorText = maxFalsePositivesText + maxFalseNegativesText + maxFailedPredecessorComparisonsText;
        aggregatedMetrics.commonDenominatorCode = maxFalsePositivesCode + maxFalseNegativesCode + maxFailedPredecessorComparisonsCode;

        return aggregatedMetrics;
    }

    private void integrateInList(
            MetricThresholdAggregatedList metricThresholdAggregateds,
            MetricThresholdAggregated newMetricThresholdAggregated,
            boolean divideBySamples){
        for (MetricThresholdAggregated tmpMetricThresholdAggregated : metricThresholdAggregateds) {
            if (newMetricThresholdAggregated.definesSameType(tmpMetricThresholdAggregated, divideBySamples)) {

                tmpMetricThresholdAggregated.postVersionCount += newMetricThresholdAggregated.postVersionCount;


                tmpMetricThresholdAggregated.runtimeText += newMetricThresholdAggregated.runtimeText; // TODO: check this

                tmpMetricThresholdAggregated.textBlockVersionCount += newMetricThresholdAggregated.textBlockVersionCount;
                tmpMetricThresholdAggregated.possibleConnectionsText += newMetricThresholdAggregated.possibleConnectionsText;

                tmpMetricThresholdAggregated.truePositivesText += newMetricThresholdAggregated.truePositivesText;
                tmpMetricThresholdAggregated.trueNegativesText += newMetricThresholdAggregated.trueNegativesText;
                tmpMetricThresholdAggregated.falsePositivesText += newMetricThresholdAggregated.falsePositivesText;
                tmpMetricThresholdAggregated.falseNegativesText += newMetricThresholdAggregated.falseNegativesText;

                tmpMetricThresholdAggregated.failedPredecessorComparisonsText = newMetricThresholdAggregated.failedPredecessorComparisonsText;


                tmpMetricThresholdAggregated.runtimeCode += newMetricThresholdAggregated.runtimeCode; // TODO: check this

                tmpMetricThresholdAggregated.codeBlockVersionCount += newMetricThresholdAggregated.codeBlockVersionCount;
                tmpMetricThresholdAggregated.possibleConnectionsCode += newMetricThresholdAggregated.possibleConnectionsCode;

                tmpMetricThresholdAggregated.truePositivesCode += newMetricThresholdAggregated.truePositivesCode;
                tmpMetricThresholdAggregated.trueNegativesCode += newMetricThresholdAggregated.trueNegativesCode;
                tmpMetricThresholdAggregated.falsePositivesCode += newMetricThresholdAggregated.falsePositivesCode;
                tmpMetricThresholdAggregated.falseNegativesCode += newMetricThresholdAggregated.falseNegativesCode;

                tmpMetricThresholdAggregated.failedPredecessorComparisonsCode = newMetricThresholdAggregated.failedPredecessorComparisonsCode;


                return;
            }
        }

        metricThresholdAggregateds.add(newMetricThresholdAggregated);
    }


    private void writeCsvFile(MetricThresholdAggregatedList aggregatedMetrics, Path pathToOutputDirectory, boolean divideBySamples){

        String[] header = {
                "sample", "metricType", "metric", "threshold",
                "postVersionCount",

                "runtimeText",
                "textBlockVersionCount", "possibleConnectionsText",
                "truePositivesText", "trueNegativesText", "falsePositivesText", "falseNegativesText",
                "failedPredecessorComparisonsText",

                "relativeFalsePositivesText",
                "relativeFalseNegativesText",
                "relativeFailedPredecessorComparisonsText",

                "runtimeCode",
                "codeBlockVersionCount", "possibleConnectionsCode",
                "truePositivesCode", "trueNegativesCode", "falsePositivesCode", "falseNegativesCode",
                "failedPredecessorComparisonsCode",

                "relativeFalsePositivesCode",
                "relativeFalseNegativesCode",
                "relativeFailedPredecessorComparisonsCode"
        };


        CSVFormat csvFormat = divideBySamples ?
                CSVFormat.DEFAULT.withHeader(header) :
                CSVFormat.DEFAULT.withHeader(Arrays.copyOfRange(header, 1, header.length)); // https://stackoverflow.com/a/4439612

        // print csv file
        try (CSVPrinter csvPrinter = new CSVPrinter(
                new FileWriter(
                        pathToOutputDirectory.toString() + "\\PostId_VersionCount_SO_17-06_sample_100_per_metricThreshold" + (divideBySamples ? "Sample" : "") + ".csv"),
                csvFormat
                        .withDelimiter(';')
                        .withQuote('"')
                        .withQuoteMode(QuoteMode.MINIMAL) // TODO: Adjust with right quote mode
                        .withEscape('\\')
                        .withNullString("null"))) {

            for (MetricThresholdAggregated metricThresholdAggregated : aggregatedMetrics) {

                List<Object> columnEntries = new ArrayList<>();

                if (divideBySamples) {
                    columnEntries.add(metricThresholdAggregated.sample);
                }


                columnEntries.add(metricThresholdAggregated.metric);
                columnEntries.add(metricThresholdAggregated.metricType);
                columnEntries.add(metricThresholdAggregated.threshold);

                columnEntries.add(metricThresholdAggregated.postVersionCount);


                columnEntries.add(metricThresholdAggregated.runtimeText);
                columnEntries.add(metricThresholdAggregated.textBlockVersionCount);
                columnEntries.add(metricThresholdAggregated.possibleConnectionsText);

                columnEntries.add(metricThresholdAggregated.truePositivesText);
                columnEntries.add(metricThresholdAggregated.trueNegativesText);
                columnEntries.add(metricThresholdAggregated.falsePositivesText);
                columnEntries.add(metricThresholdAggregated.falseNegativesText);

                columnEntries.add(metricThresholdAggregated.failedPredecessorComparisonsText);

                columnEntries.add(metricThresholdAggregated.falsePositivesText / (double)aggregatedMetrics.commonDenominatorText);
                columnEntries.add(metricThresholdAggregated.falseNegativesText / (double)aggregatedMetrics.commonDenominatorText);
                columnEntries.add(metricThresholdAggregated.failedPredecessorComparisonsText / (double)aggregatedMetrics.commonDenominatorText);


                columnEntries.add(metricThresholdAggregated.runtimeCode);
                columnEntries.add(metricThresholdAggregated.codeBlockVersionCount);
                columnEntries.add(metricThresholdAggregated.possibleConnectionsCode);

                columnEntries.add(metricThresholdAggregated.truePositivesCode);
                columnEntries.add(metricThresholdAggregated.trueNegativesCode);
                columnEntries.add(metricThresholdAggregated.falsePositivesCode);
                columnEntries.add(metricThresholdAggregated.falseNegativesCode);

                columnEntries.add(metricThresholdAggregated.failedPredecessorComparisonsCode);

                columnEntries.add(metricThresholdAggregated.falsePositivesCode / (double)aggregatedMetrics.commonDenominatorCode);
                columnEntries.add(metricThresholdAggregated.falseNegativesCode / (double)aggregatedMetrics.commonDenominatorCode);
                columnEntries.add(metricThresholdAggregated.failedPredecessorComparisonsCode / (double)aggregatedMetrics.commonDenominatorCode);


                csvPrinter.printRecord(columnEntries);
            }

            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    void convertAggregatedSampleToMetricThresholdAggregated(
            Path pathToFile_perPostAggregated,
            Path pathToOutputDirectory,
            boolean divideBySamples) {

        MetricThresholdAggregatedList aggregatedList = parseAggregatedSample(pathToFile_perPostAggregated, divideBySamples);
        writeCsvFile(aggregatedList, pathToOutputDirectory, divideBySamples);
    }
}
