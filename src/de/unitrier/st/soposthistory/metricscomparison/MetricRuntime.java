package de.unitrier.st.soposthistory.metricscomparison;

class MetricRuntime {
    private long runtimeTotal;

    MetricRuntime() {
        this.runtimeTotal = 0;
    }

    long getRuntime() {
        return runtimeTotal;
    }

    void setRuntimeTotal(long runtimeTotal) {
        this.runtimeTotal = runtimeTotal;
    }
}
