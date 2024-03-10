package io.openmessaging.benchmark.driver;

public class TpcHInfo {
    public TpcHConsumer consumer;
    public Integer numberOfMapResults;
    public Integer numberOfReduceResults;

    public TpcHInfo(TpcHConsumer consumer, Integer numberOfMapResults, Integer numberOfReduceResults) {
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
