package de.unitrier.st.soposthistory.metricscomparison;

class MetricRuntime {
    private Long runtimeTotal;

    MetricRuntime() {
        this.runtimeTotal = null;
    }

    MetricRuntime(Long runtimeTotal, Long runtimeUser, Long runtimeCPU) {
        this.runtimeTotal = runtimeTotal;
    }

    Long getRuntime() {
        return runtimeTotal;
    }
    void setRuntimeTotal(Long runtimeTotal) {
        this.runtimeTotal = runtimeTotal;
    }
}
