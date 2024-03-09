package io.openmessaging.benchmark.tpch;

public class TpcHProducerAssignment {
    public TpcHQuery query;
    public String sourceDataS3FolderUri;
    public int batchSize;
    public int offset;
}
