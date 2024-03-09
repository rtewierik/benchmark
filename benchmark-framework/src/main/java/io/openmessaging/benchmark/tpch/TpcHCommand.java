package io.openmessaging.benchmark.tpch;

public class TpcHCommand {
    public String sourceDataS3FolderUri;
    public int numberOfChunks;
    public int batchSize;
    public int queryId;

    public int getNumberOfReduceOperations() {
        return (int)Math.ceil((double)this.numberOfChunks / this.batchSize);
    }
}
