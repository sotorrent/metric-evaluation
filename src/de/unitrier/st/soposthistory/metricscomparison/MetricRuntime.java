package de.unitrier.st.soposthistory.metricscomparison;

class MetricRuntime {
    private long totalRuntime;

    MetricRuntime() {
        this.totalRuntime = 0;
    }

    long getTotalRuntime() {
        return totalRuntime;
    }

    void setTotalRuntime(long totalRuntime) {
        this.totalRuntime = totalRuntime;
    }

    void add(MetricRuntime metricRuntime) {
        totalRuntime += metricRuntime.getTotalRuntime();
    }
}
