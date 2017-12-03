package de.unitrier.st.soposthistory.metricscomparison.evaluation;

import de.unitrier.st.util.Util;

import java.io.IOException;
import java.util.logging.Logger;

public class MetricResult {
    private static Logger logger;

    private SimilarityMetric similarityMetric;
    private int postCount;
    private int postVersionCount;
    private int postBlockVersionCount;
    private int possibleConnections;
    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;
    private int failedPredecessorComparisons;
    private long runtime;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(MetricResult.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MetricResult(SimilarityMetric similarityMetric) {
        this.similarityMetric = similarityMetric;
        this.postCount = 0;
        this.postVersionCount = 0;
        this.postBlockVersionCount = 0;
        this.possibleConnections = 0;
        this.truePositives = 0;
        this.falsePositives = 0;
        this.trueNegatives = 0;
        this.falseNegatives = 0;
        this.failedPredecessorComparisons = 0;
        this.runtime = 0;
    }

    SimilarityMetric getSimilarityMetric() {
        return similarityMetric;
    }

    int getPostCount() {
        return postCount;
    }

    int getPostVersionCount() {
        return postVersionCount;
    }

    public int getPostBlockVersionCount() {
        return postBlockVersionCount;
    }

    int getPossibleConnections() {
        return possibleConnections;
    }

    public int getTruePositives() {
        return truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    public int getFailedPredecessorComparisons() {
        return failedPredecessorComparisons;
    }

    long getRuntime() {
        return runtime;
    }

    void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    void setPostVersionCount(int postVersionCount) {
        this.postVersionCount = postVersionCount;
    }

    void setPostBlockVersionCount(int postBlockVersionCount) {
        this.postBlockVersionCount = postBlockVersionCount;
    }

    void setPossibleConnections(int possibleConnections) {
        this.possibleConnections = possibleConnections;
    }

    void setTruePositives(int truePositives) {
        this.truePositives = truePositives;
    }

    void setFalsePositives(int falsePositives) {
        this.falsePositives = falsePositives;
    }

    void setTrueNegatives(int trueNegatives) {
        this.trueNegatives = trueNegatives;
    }

    void setFalseNegatives(int falseNegatives) {
        this.falseNegatives = falseNegatives;
    }

    void setFailedPredecessorComparisons(int failedPredecessorComparisons) {
        this.failedPredecessorComparisons = failedPredecessorComparisons;
    }

    void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    void add(MetricResult result) {
        postCount += result.getPostCount();
        postVersionCount += result.getPostVersionCount();
        postBlockVersionCount += result.getPostBlockVersionCount();
        possibleConnections += result.getPossibleConnections();
        truePositives += result.getTruePositives();
        falsePositives += result.getFalsePositives();
        trueNegatives += result.getTrueNegatives();
        falseNegatives += result.getFalseNegatives();
        failedPredecessorComparisons += result.getFailedPredecessorComparisons();
        runtime += result.getRuntime();
    }

    double getPrecision() {
        return ((double) truePositives) / ((double) truePositives + (double) falsePositives);
    }

    double getInversePrecision() {
        // see Powers11
        return ((double) trueNegatives) / ((double) trueNegatives + (double) falseNegatives);
    }

    double getRecall() {
        return ((double) truePositives) / ((double) truePositives + (double) falseNegatives);
    }

    double getInverseRecall() {
        // see Powers11
        return ((double) trueNegatives) / ((double) trueNegatives + (double) falsePositives);
    }

    double getMarkedness() {
        // see Powers11
        // see https://en.wikipedia.org/wiki/Evaluation_of_binary_classifiers
        return getPrecision() + getInversePrecision() - 1;
    }


    double getInformedness() {
        // see Powers11
        // see https://en.wikipedia.org/wiki/Youden%27s_J_statistic
        // see https://en.wikipedia.org/wiki/Evaluation_of_binary_classifiers
        return getRecall() + getInverseRecall() - 1;
    }

    double getMatthewsCorrelation() {
        // see Powers11, Matthews75
        // see https://en.wikipedia.org/wiki/Matthews_correlation_coefficient
        // see https://lettier.github.io/posts/2016-08-05-matthews-correlation-coefficient.html

        // conversion to double to prevent integer overflow
        double truePositives = getTruePositives();
        double trueNegatives = getTrueNegatives();
        double falsePositives = getFalsePositives();
        double falseNegatives = getFalseNegatives();

        double numerator = (truePositives * trueNegatives) - (falsePositives * falseNegatives);
        double denominatorSum = (truePositives + falsePositives) * (truePositives + falseNegatives) * (trueNegatives + falsePositives) * (trueNegatives + falseNegatives);
        // "If any of the four sums in the denominator is zero, the denominator can be arbitrarily set to one; this results in a Matthews correlation coefficient of zero, which can be shown to be the correct limiting value."
        double denominator = (denominatorSum == 0 ? 1 : Math.sqrt(denominatorSum));

        double matthewsCorrelationCoefficient = numerator / denominator;

        if (Util.lessThan(matthewsCorrelationCoefficient, 0.0) || Util.greaterThan(matthewsCorrelationCoefficient, 1.0)) {
            String msg = "Matthews correlation coefficient must be in range [0.0, 1.0], but was " + matthewsCorrelationCoefficient;
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
        }

        return matthewsCorrelationCoefficient;
    }

    double getFScore() {
        // see https://en.wikipedia.org/wiki/F1_score
        double precision = getPrecision();
        double recall = getRecall();
        return 2 * (precision * recall) / (precision + recall);
    }

    double getFailureRate() {
        double failureRate = possibleConnections == 0 ? 0.0 : (double)failedPredecessorComparisons / possibleConnections;

        if (Util.lessThan(failureRate, 0.0) || Util.greaterThan(failureRate, 1.0)) {
            String msg = similarityMetric + ": Failure rate must be in range [0.0, 1.0], but was " + failureRate;
            logger.warning(msg);
            throw new IllegalArgumentException(msg);
        }

        return failureRate;
    }

    static void validate(MetricResult resultText, MetricResult resultCode) throws IllegalStateException {
        if (resultText.getFailedPredecessorComparisons() > resultText.getPossibleConnections()) {
            String msg = resultText.getSimilarityMetric() + ": Number of failed comparisons is greater than number of possible connections (text).";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
        if (resultCode.getFailedPredecessorComparisons() > resultCode.getPossibleConnections()) {
            String msg = resultCode.getSimilarityMetric() + ": Number of failed comparisons is greater than number of possible connections (code).";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
        if (resultText.getPostCount() != resultCode.getPostCount()) {
            String msg = "Post count does not match.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (resultText.getPostVersionCount() != resultCode.getPostVersionCount()) {
            String msg = "Post version count does not match.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
    }
}
