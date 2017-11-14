package de.unitrier.st.soposthistory.metricscomparison;

class MetricRuntime {
    private Long runtimeTotal;
    private Long runtimeCPU;
    private Long runtimeUser;

    MetricRuntime() {
        this.runtimeTotal = null;
        this.runtimeCPU = null;
        this.runtimeUser = null;
    }

    MetricRuntime(Long runtimeTotal, Long runtimeUser, Long runtimeCPU) {
        this.runtimeTotal = runtimeTotal;
        this.runtimeCPU = runtimeCPU;
        this.runtimeUser = runtimeUser;
    }

    Long getRuntimeTotal() {
        return runtimeTotal;
    }

    Long getRuntimeCPU() {
        return runtimeCPU;
    }

    Long getRuntimeUser() {
        return runtimeUser;
    }

    void setRuntimeTotal(Long runtimeTotal) {
        this.runtimeTotal = runtimeTotal;
    }

    void setRuntimeCPU(Long runtimeCPU) {
        this.runtimeCPU = runtimeCPU;
    }

    void setRuntimeUser(Long runtimeUser) {
        this.runtimeUser = runtimeUser;
    }
}
