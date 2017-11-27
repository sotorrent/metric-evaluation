package de.unitrier.st.soposthistory.metricscomparison;

import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

/**
 * Evaluation of one similarity metric using a sample of SO posts.
 */
public class MetricEvaluationPerSample extends LinkedList<MetricEvaluationPerPost> {
    private static Logger logger;

    static {
        // configure logger
        try {
            logger = getClassLogger(MetricEvaluationPerSample.class);
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

    private int maxFailuresText;
    private int maxFailuresCode;

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

        // check if GT and post version list contain the same posts with the same post blocks types in the same positions
        for (int postId : postVersionLists.keySet()) {
            PostGroundTruth gt = postGroundTruths.get(postId);

            if (gt == null) {
                return false;
            } else {
                PostVersionList list = postVersionLists.get(postId);

                Set<PostBlockConnection> connectionsList = list.getConnections();
                Set<PostBlockConnection> connectionsGT = gt.getConnections();

                if (!PostBlockConnection.matches(connectionsList, connectionsGT)) {
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

        if (aggregatedResultText.getPostCount() != aggregatedResultCode.getPostCount()) {
            throw new IllegalStateException("Post count of aggregated results does not match.");
        }

        if (aggregatedResultText.getPostVersionCount() != aggregatedResultCode.getPostVersionCount()) {
            throw new IllegalStateException("Post version count of aggregated results does not match.");
        }

        // "MetricType", "Metric", "Threshold",
        // "YoudensJText", "RuntimeText", "YoudensJCode", "RuntimeCode",
        // "PostCount", "PostVersionCount", "PostBlockVersionCount", "PossibleConnections",
        // "TextBlockVersionCount", "PossibleConnectionsText",
        // "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "FailuresText",
        // "PrecisionText", "RecallText", "SensitivityText", "SpecificityText", "FailureRateText",
        // "CodeBlockVersionCount", "PossibleConnectionsCode",
        // "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode", "FailuresCode",
        // "PrecisionCode", "RecallCode", "SensitivityCode", "SpecificityCode", "FailureRateCode"
        csvPrinterSample.printRecord(
                similarityMetric.getType(),
                similarityMetric.getName(),
                similarityMetric.getThreshold(),

                aggregatedResultText.getYoudensJ(),
                aggregatedResultText.getRuntime(),
                aggregatedResultCode.getYoudensJ(),
                aggregatedResultCode.getRuntime(),

                aggregatedResultText.getPostCount(),
                aggregatedResultText.getPostVersionCount(),
                aggregatedResultText.getPostBlockVersionCount() + aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleConnections() + aggregatedResultCode.getPossibleConnections(),

                aggregatedResultText.getPostBlockVersionCount(),
                aggregatedResultText.getPossibleConnections(),

                aggregatedResultText.getTruePositives(),
                aggregatedResultText.getTrueNegatives(),
                aggregatedResultText.getFalsePositives(),
                aggregatedResultText.getFalseNegatives(),
                aggregatedResultText.getFailedPredecessorComparisons(),

                aggregatedResultText.getPrecision(),
                aggregatedResultText.getRecall(),
                aggregatedResultText.getSensitivity(),
                aggregatedResultText.getSpecificity(),
                aggregatedResultText.getFailureRate(maxFailuresText),

                aggregatedResultCode.getPostBlockVersionCount(),
                aggregatedResultCode.getPossibleConnections(),

                aggregatedResultCode.getTruePositives(),
                aggregatedResultCode.getTrueNegatives(),
                aggregatedResultCode.getFalsePositives(),
                aggregatedResultCode.getFalseNegatives(),
                aggregatedResultCode.getFailedPredecessorComparisons(),

                aggregatedResultCode.getPrecision(),
                aggregatedResultCode.getRecall(),
                aggregatedResultCode.getSensitivity(),
                aggregatedResultCode.getSpecificity(),
                aggregatedResultCode.getFailureRate(maxFailuresCode)
        );
    }

    MetricResult getResultAggregatedBySampleText() {
        // aggregate by sample
        if (aggregatedResultText == null) {
            aggregatedResultText = new MetricResult(similarityMetric);
            for (MetricEvaluationPerPost evaluationPerPost : this) {
                MetricResult resultText = evaluationPerPost.getResultAggregatedByPostText();
                maxFailuresText = Math.max(maxFailuresText, resultText.getFailedPredecessorComparisons());
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
                maxFailuresCode = Math.max(maxFailuresCode, resultCode.getFailedPredecessorComparisons());
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
