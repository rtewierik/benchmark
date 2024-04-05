package io.openmessaging.benchmark.driver.monitoring;

public class MonitoredProducedMessage {
    public long payloadLength;
    public long intendedSendTimeNs;
    public long sendTimeNs;
    public long nowNs;
    public String experimentId;
    public String messageId;
    public boolean isTpcH;
    public boolean isError;

    public MonitoredProducedMessage(
            long payloadLength,
            long intendedSendTimeNs,
            long sendTimeNs,
            long nowNs,
            String experimentId,
            String messageId,
            boolean isTpcH,
            boolean isError
    ) {
        this.payloadLength = payloadLength;
        this.intendedSendTimeNs = intendedSendTimeNs;
        this.sendTimeNs = sendTimeNs;
        this.nowNs = nowNs;
        this.experimentId = experimentId;
        this.messageId = messageId;
        this.isTpcH = isTpcH;
        this.isError = isError;
    }
}
