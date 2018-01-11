package de.unitrier.st.soposthistory.metricscomparison.evaluation;

import de.unitrier.st.soposthistory.Config;

import java.util.function.BiFunction;

public class SimilarityMetric {
    public enum MetricType {NONE, EQUAL, EDIT, FINGERPRINT, PROFILE, SET, DEFAULT}

    final private String nameText;
    final private MetricType typeText;
    final private String backupNameText;
    final private MetricType backupTypeText;

    final private String nameCode;
    final private MetricType typeCode;
    final private String backupNameCode;
    final private MetricType backupTypeCode;

    final private Config config;

    public SimilarityMetric(String name, BiFunction<String, String, Double> metric, MetricType type, double threshold) {
        this(
                name, type, "none", MetricType.NONE,
                name, type, "none", MetricType.NONE,
                Config.METRICS_COMPARISON
                    .withCodeSimilarityMetric(metric)
                    .withCodeSimilarityThreshold(threshold)
                    .withTextSimilarityMetric(metric)
                    .withTextSimilarityThreshold(threshold)
        );
    }

    SimilarityMetric(String nameText, MetricType typeText, String backupNameText, MetricType backupTypeText,
                            String nameCode, MetricType typeCode, String backupNameCode, MetricType backupTypeCode,
                            Config config) {
        this.nameText = nameText;
        this.typeText = typeText;
        this.backupNameText = backupNameText;
        this.backupTypeText = backupTypeText;

        this.nameCode = nameCode;
        this.typeCode = typeCode;
        this.backupNameCode = backupNameCode;
        this.backupTypeCode = backupTypeCode;

        this.config = config;
    }

    public String getNameText() {
        return nameText;
    }

    public MetricType getTypeText() {
        return typeText;
    }

    public String getBackupNameText() {
        return backupNameText;
    }

    public MetricType getBackupTypeText() {
        return backupTypeText;
    }

    public String getNameCode() {
        return nameCode;
    }

    public MetricType getTypeCode() {
        return typeCode;
    }

    public String getBackupNameCode() {
        return backupNameCode;
    }

    public MetricType getBackupTypeCode() {
        return backupTypeCode;
    }

    public Config getConfig() {
        return config;
    }

    public SimilarityMetric withConfig(Config config) {
        return new SimilarityMetric(
                nameText, typeText, backupNameText, backupTypeText,
                nameCode, typeCode, backupNameCode, backupTypeCode,
                config
        );
    }

    @Override
    public String toString() {
        return "(" + nameText + "; " + config.getTextSimilarityThreshold() + "; "
                + nameCode + "; " + config.getCodeSimilarityThreshold() + ")";
    }
}
