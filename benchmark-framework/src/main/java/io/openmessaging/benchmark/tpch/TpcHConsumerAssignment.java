package io.openmessaging.benchmark.tpch;

import io.openmessaging.benchmark.driver.TpcHQuery;

public class TpcHConsumerAssignment {
    public TpcHQuery query;
    public String batchId;
    public String sourceDataS3Uri;
}
