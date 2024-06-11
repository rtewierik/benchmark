CREATE EXTERNAL TABLE IF NOT EXISTS {{ table_name }} (
  Item STRUCT<
    transactionId: STRUCT<S: STRING>,
    messageId: STRUCT<S: STRING>,
    experimentId: STRUCT<S: STRING>,
    payloadLength: STRUCT<N: BIGINT>,
    intendedSendTimeNs: STRUCT<N: BIGINT>,
    sendTimeNs: STRUCT<N: BIGINT>,
    nowNs: STRUCT<N: BIGINT>,
    endToEndLatencyMicros: STRUCT<N: BIGINT>,
    timestamp: STRUCT<N: BIGINT>,
    publishTimestamp: STRUCT<N: BIGINT>,
    processTimestamp: STRUCT<N: BIGINT>,
    isTpcH: STRUCT<BOOL: BOOLEAN>,
    isError: STRUCT<BOOL: BOOLEAN>
  >
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES (
  'serialization.format' = '1'
)
LOCATION '{{ s3_uri }}';
