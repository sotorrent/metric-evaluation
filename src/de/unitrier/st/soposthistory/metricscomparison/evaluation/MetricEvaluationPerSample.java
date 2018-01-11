package de.unitrier.st.soposthistory.metricscomparison.evaluation;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Evaluation of one similarity metric using a sample of SO posts.
 */
public class MetricEvaluationPerSample extends LinkedList<MetricEvaluationPerPost> {
    private static Logger logger;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(MetricEvaluationPerSample.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String sampleName;
    private SimilarityMetric similarityMetric;

    private Set<Integer> postIds;
    private Map<Integer, PostGroundTruth> postGroundTruths; // postId -> PostGroundTruth
    private Map<Integer, PostVersionList> postVersionLists; // postId -> PostVersionList

    private int numberOfRepetitions;
    private boolean randomizeOrder;

    private MetricResult aggregatedResultText;
    private MetricResult aggregatedResultCode;

    MetricEvaluationPerSample(String sampleName,
                              SimilarityMetric similarityMetric,
                              Set<Integer> postIds,
                              Map<Integer, PostVersionList> postVersionLists,
                              Map<Integer, PostGroundTruth> postGroundTruths,
                              int numberOfRepetitions,
                              boolean randomizeOrder) {
        this.sampleName = sampleName;
        this.similarityMetric = similarityMetric;
        this.postIds = postIds;
        this.postGroundTruths = postGroundTruths;
        this.postVersionLists = postVersionLists;
        this.numberOfRepetitions = numberOfRepetitions;
        this.randomizeOrder = randomizeOrder;
    }

    boolean validate() {
        if (postGroundTruths.size() != postVersionLists.size())
            return false;

        // check if GT and post version list contain the same posts with the same number of possible comparisons
        for (int postId : postVersionLists.keySet()) {
            PostGroundTruth gt = postGroundTruths.get(postId);

            if (gt == null) {
                return false;
            } else {
                // text
                PostVersionList list = postVersionLists.get(postId);
                int possibleComparisonsList = list.getPossibleComparisons(TextBlockVersion.getPostBlockTypeIdFilter());
                int possibleCompariosnsGT = gt.getPossibleComparisons(TextBlockVersion.getPostBlockTypeIdFilter());
                if (possibleComparisonsList != possibleCompariosnsGT) {
                    return false;
                }
                // code
                possibleComparisonsList = list.getPossibleComparisons(CodeBlockVersion.getPostBlockTypeIdFilter());
                possibleCompariosnsGT = gt.getPossibleComparisons(CodeBlockVersion.getPostBlockTypeIdFilter());
                if (possibleComparisonsList != possibleCompariosnsGT) {
                    return false;
                }
                // both
                possibleComparisonsList = list.getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
                possibleCompariosnsGT = gt.getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
                if (possibleComparisonsList != possibleCompariosnsGT) {
                    return false;
                }
            }
        }

        return true;
    }

    private void randomizeOrder() {
        Collections.shuffle(this, new Random());
    }

    void prepareEvaluation() {
        for (int postId : postIds) {
            this.add(
                    new MetricEvaluationPerPost(
                            similarityMetric,
                            postId,
                            postVersionLists.get(postId),
                            postGroundTruths.get(postId),
                            numberOfRepetitions
                    )
            );
        }
    }

    void startEvaluation(int currentRepetition) {
        if (randomizeOrder) {
            logger.info("Randomizing order of posts in sample " + sampleName + " for metric " + similarityMetric + "...");
            randomizeOrder();
        }

        logger.info("Starting evaluation run " + currentRepetition + " for metric " + similarityMetric + " on sample " + sampleName + "...");

        for (MetricEvaluationPerPost evaluation : this) {
            evaluation.startEvaluation(currentRepetition);
        }
    }

    void writeToCSV(CSVPrinter csvPrinterSample) throws IOException {

        // write results aggregated per sample
        MetricResult aggregatedResultText = getResultAggregatedBySampleText();
        MetricResult aggregatedResultCode = getResultAggregatedBySampleCode();

        // validate results
        MetricResult.validate(aggregatedResultText, aggregatedResultCode);

        // "MetricTypeText", "MetricText", "ThresholdText",
        // "MetricTypeTextBackup", "MetricTextBackup", "ThresholdTextBackup",
        // "MetricTypeCode", "MetricCode", "ThresholdCode",
        // "MetricTypeCodeBackup", "MetricCodeBackup", "ThresholdCodeBackup",
        // "Runtime",
        // "InformednessText", "MarkednessText", "MatthewsCorrelationText", "FScoreText",,
        // "InformednessCode", "MarkednessCode", "MatthewsCorrelationCode", "FScoreCode",
        // "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleComparisons",
        // "TextBlockVersionCount", "PossibleComparisonsText",
        // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText",
        // "PrecisionText", "RecallText", "InversePrecisionText", "InverseRecallText", "FailureRateText",
        // "CodeBlockVersionCount", "PossibleComparisonsCode",
        // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode",
        // "PrecisionCode", "RecallCode", "InversePrecisionCode", "InverseRecallCode", "FailureRateCode"
        csvPrinterSample.printRecord(
                similarityMetric.getTypeText(),
                similarityMetric.getNameText(),
                similarityMetric.getConfig().getTextSimilarityThreshold(),

                similarityMetric.getBackupTypeText(),
                similarityMetric.getBackupNameText(),
                similarityMetric.getConfig().getTextBackupSimilarityThreshold(),

                similarityMetric.getTypeCode(),
                similarityMetric.getNameCode(),
                similarityMetric.getConfig().getCodeSimilarityThreshold(),

                similarityMetric.getBackupTypeCode(),
                similarityMetric.getBackupNameCode(),
                similarityMetric.getConfig().getCodeBackupSimilarityThreshold(),

                aggregatedResultText.getRuntime(),

                aggregatedResultText.getInformedness(),
                aggregatedResultText.getMarkedness(),
                aggregatedResultText.getMatthewsCorrelation(),
                aggregatedResultText.getFScore(),

                aggregatedResultCode.getInformedness(),
                aggregatedResultCode.getMarkedness(),
                aggregatedResultCode.getMatthewsCorrelation(),
                aggregatedResultCode.getFScore(),

                aggregatedResultText.getPostCount(),
                aggregatedResultText.getPostVersionCount(),
                aggregatedResultText.getPostBlockVersionCount() + aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleComparisons() + aggregatedResultCode.getPossibleComparisons(),

                aggregatedResultText.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleComparisons(),

                aggregatedResultText.getTruePositives(),
                aggregatedResultText.getTrueNegatives(),
                aggregatedResultText.getFalsePositives(),
                aggregatedResultText.getFalseNegatives(),
                aggregatedResultText.getFailedPredecessorComparisons(),

                aggregatedResultText.getPrecision(),
                aggregatedResultText.getRecall(),
                aggregatedResultText.getInversePrecision(),
                aggregatedResultText.getInverseRecall(),
                aggregatedResultText.getFailureRate(),

                aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultCode.getPossibleComparisons(),

                aggregatedResultCode.getTruePositives(),
                aggregatedResultCode.getTrueNegatives(),
                aggregatedResultCode.getFalsePositives(),
                aggregatedResultCode.getFalseNegatives(),
                aggregatedResultCode.getFailedPredecessorComparisons(),

                aggregatedResultCode.getPrecision(),
                aggregatedResultCode.getRecall(),
                aggregatedResultCode.getInversePrecision(),
                aggregatedResultCode.getInverseRecall(),
                aggregatedResultCode.getFailureRate()
        );
    }

    MetricResult getResultAggregatedBySampleText() {
        // aggregate by sample
        if (aggregatedResultText == null) {
            aggregatedResultText = new MetricResult(similarityMetric);
            for (MetricEvaluationPerPost evaluationPerPost : this) {
                MetricResult resultText = evaluationPerPost.getResultAggregatedByPostText();
                aggregatedResultText.add(resultText);
            }
        }
        return aggregatedResultText;
    }

    MetricResult getResultAggregatedBySampleCode() {
        // aggregate by sample
        if (aggregatedResultCode == null) {
            aggregatedResultCode = new MetricResult(similarityMetric);
            for (MetricEvaluationPerPost evaluationPerPost : this) {
                MetricResult resultCode = evaluationPerPost.getResultAggregatedByPostCode();
                aggregatedResultCode.add(resultCode);
            }
        }
        return aggregatedResultCode;
    }

    SimilarityMetric getSimilarityMetric() {
        return similarityMetric;
    }

    @Override
    public String toString() {
        return similarityMetric + " on " + sampleName;
    }
}
