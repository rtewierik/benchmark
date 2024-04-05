package io.openmessaging.benchmark.driver.monitoring;

public class MonitoredReceivedMessage {
    public int payloadLength;
    public long endToEndLatencyMicros;
    public String experimentId;
    public String messageId;
    public boolean isTpcH;

    public MonitoredReceivedMessage(
            int payloadLength,
            long endToEndLatencyMicros,
            String experimentId,
            String messageId,
            boolean isTpcH
    ) {
        this.payloadLength = payloadLength;
        this.endToEndLatencyMicros = endToEndLatencyMicros;
        this.experimentId = experimentId;
        this.messageId = messageId;
        this.isTpcH = isTpcH;
    }
}
