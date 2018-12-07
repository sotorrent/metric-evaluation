# metrics-comparison
Comparision of different string similarity metrics for reconstructing the history Stack Overflow posts.

To test all metrics (see `MetricEvaluationManager.addAllSimilarityMetrics()`), run:
 
    java -jar metric-evaluation-2.2.0-jar-with-dependencies.jar -s testdata/samples_comparison -o output -t 1

To test only the selected metrics, run:

    java -jar metric-evaluation-2.2.0-jar-with-dependencies.jar -sm selected_metrics -s testdata/samples_comparison -o output -t 1

To test only the configured combined metrics (see `MetricEvaluationManager.createCombinedSimilarityMetrics()`), run:

    java -jar metric-evaluation-2.2.0-jar-with-dependencies.jar -cm -s testdata/samples_comparison -o output -t 1

To test only the default metric, run:

    java -jar metric-evaluation-2.2.1-jar-with-dependencies.jar -dm -s testdata/samples_comparison -o output -t 1

[![DOI](https://zenodo.org/badge/103541441.svg)](https://zenodo.org/badge/latestdoi/103541441)
