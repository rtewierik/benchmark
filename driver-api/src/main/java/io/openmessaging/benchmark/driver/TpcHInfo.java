package io.openmessaging.benchmark.driver;

public class TpcHInfo {
    public TpcHQuery query;
    public TpcHConsumer consumer;
    public Integer numberOfMapResults;
    public Integer numberOfReduceResults;

    public TpcHInfo(
        TpcHQuery query,
        TpcHConsumer consumer,
        Integer numberOfMapResults,
        Integer numberOfReduceResults
    ) {
        this.query = query;
        this.consumer = consumer;
        this.numberOfMapResults = numberOfMapResults;
        this.numberOfReduceResults = numberOfReduceResults;
    }

    @Override
    public String toString() {
        return "TpcHInfo{" +
                "consumer=" + consumer +
                ", numberOfMapResults=" + numberOfMapResults +
                ", numberOfReduceResults=" + numberOfReduceResults +
                '}';
    }
}
