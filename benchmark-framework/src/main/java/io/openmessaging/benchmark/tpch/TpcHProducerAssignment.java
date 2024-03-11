package io.openmessaging.benchmark.tpch;

import io.openmessaging.benchmark.driver.TpcHQuery;

public class TpcHProducerAssignment {
    public TpcHQuery query;
    public String sourceDataS3FolderUri;
    public int batchSize;
    public int offset;
}
