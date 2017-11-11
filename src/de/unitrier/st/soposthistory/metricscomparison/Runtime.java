package de.unitrier.st.soposthistory.metricscomparison;

public class Runtime {
    Long runtimeTotal;
    Long runtimeUser;

    Runtime() {
        this.runtimeTotal = null;
        this.runtimeUser = null;
    }

    Runtime(Long runtimeTotal, Long runtimeUser) {
        this.runtimeTotal = runtimeTotal;
        this.runtimeUser = runtimeUser;
    }

    public Long getRuntimeTotal() {
        return runtimeTotal;
    }

    public Long getRuntimeUser() {
        return runtimeUser;
    }

    void setRuntimeTotal(Long runtimeTotal) {
        this.runtimeTotal = runtimeTotal;
    }

    void setRuntimeUser(Long runtimeUser) {
        this.runtimeUser = runtimeUser;
    }
}
