package io.openmessaging.benchmark.tpch;

public class TpcHCommand {
    public String sourceDataS3FolderUri;
    public int numberOfChunks;
    public int numberOfReducers;
    public int queryId;

    public int getNumberOfMapResults(int index) {
        int actualIndex = index % numberOfReducers;
        int defaultNumberOfIntermediateResults = this.getDefaultNumberOfMapResults();
        if (actualIndex < numberOfReducers - 1) {
            return defaultNumberOfIntermediateResults;
        }
        return numberOfChunks - (numberOfReducers - 1) * defaultNumberOfIntermediateResults;
    }

    private int getDefaultNumberOfMapResults() {
        return (int)Math.ceil((double)this.numberOfChunks / this.numberOfReducers);
    }
}
