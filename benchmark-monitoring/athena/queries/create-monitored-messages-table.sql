CREATE EXTERNAL TABLE IF NOT EXISTS {{ table_name }} (
  Item STRUCT<
    messageId: STRUCT<S: STRING>,
    isTpcH: STRUCT<BOOL: BOOLEAN>,
    isError: STRUCT<BOOL: BOOLEAN>,
    payloadLength: STRUCT<N: BIGINT>,
    intendedSendTimeNs: STRUCT<N: BIGINT>,
    experimentId: STRUCT<S: STRING>,
    nowNs: STRUCT<N: BIGINT>,
    sendTimeNs: STRUCT<N: BIGINT>
  >
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES (
  'serialization.format' = '1'
)
LOCATION '{{ s3_uri }}';
