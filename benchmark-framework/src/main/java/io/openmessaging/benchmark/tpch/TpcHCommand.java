package io.openmessaging.benchmark.tpch;

public class TpcHCommand {
    public String sourceDataS3FolderUri;
    public int numberOfChunks;
    public int numberOfIntermediateResultsPartitions;
    public int chunkSize;
    public int queryId;
}
