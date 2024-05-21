package io.openmessaging.benchmark.common.monitoring;

public class PeriodicMonitoring {

    public String message = "PERIODIC_MONITORING";
    public PeriodStats periodStats;
    public CumulativeLatencies cumulativeLatencies;

    public PeriodicMonitoring(PeriodStats periodStats, CumulativeLatencies cumulativeLatencies) {
        this.periodStats = periodStats;
        this.cumulativeLatencies = cumulativeLatencies;
    }
}
