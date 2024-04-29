CREATE EXTERNAL TABLE IF NOT EXISTS cpu_test (
  instanceId STRING,
  tags array<struct<Key: STRING, Value: STRING>>,
  values array<double>
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES (
  'serialization.format' = '1'
)
LOCATION 's3://rtw-monitoring-exports/ec2-metrics/cpu';

SELECT 
    instanceId, 
    tags, 
    value, 
    ROW_NUMBER() OVER (PARTITION BY instanceId ORDER BY value_index) AS value_index
FROM cpu_test
CROSS JOIN UNNEST("values") WITH ORDINALITY AS t(value, value_index);

WITH dataset AS (
  SELECT 
    instanceId, 
    tags, 
    value, 
    ROW_NUMBER() OVER (PARTITION BY instanceId ORDER BY value_index) AS value_index
  FROM cpu_test
  CROSS JOIN UNNEST("values") WITH ORDINALITY AS t(value, value_index)
),
SELECT 
  instanceId, 
  tag.Key AS tag_key,
  tag.Value AS tag_value,
  value,
  value_index
FROM dataset
CROSS JOIN UNNEST(tags) AS t(tag);

WITH dataset AS (
  SELECT 
    instanceId, 
    tags, 
    value, 
    ROW_NUMBER() OVER (PARTITION BY instanceId ORDER BY value_index) AS value_index
  FROM cpu_test
  CROSS JOIN UNNEST("values") WITH ORDINALITY AS t(value, value_index)
)
SELECT 
  instanceId, 
  tag.Key AS tag_key,
  tag.Value AS tag_value,
  value,
  value_index
FROM dataset
CROSS JOIN UNNEST(tags) AS t(tag);