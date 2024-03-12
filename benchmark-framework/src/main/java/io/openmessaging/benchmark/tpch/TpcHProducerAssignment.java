package io.openmessaging.benchmark.tpch;

import io.openmessaging.benchmark.driver.TpcHQuery;

public class TpcHProducerAssignment {
    public String queryId;
    public TpcHQuery query;
    public String sourceDataS3FolderUri;
    public int batchSize;
    public int offset;

    public TpcHProducerAssignment(TpcHCommand command, int offset) {
        this.queryId = command.queryId;
        this.query = command.query;
        this.sourceDataS3FolderUri = command.sourceDataS3FolderUri;
        this.batchSize = command.getNumberOfMapResults(offset);
        this.offset = offset;
    }
}
