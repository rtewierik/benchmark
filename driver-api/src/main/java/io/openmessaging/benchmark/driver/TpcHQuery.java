package io.openmessaging.benchmark.driver;

public enum TpcHQuery {
    PricingSummaryReport(1),
    ForecastingRevenueChange(6);

    private final int id;

    private TpcHQuery(int id) {
        this.id = id;
    }
}
