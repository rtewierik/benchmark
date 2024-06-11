WITH data_range AS (
    SELECT 
        MIN(CAST(Item.endToEndLatencyMicros.N AS BIGINT)) AS min_value, 
        MAX(CAST(Item.endToEndLatencyMicros.N AS BIGINT)) AS max_value
    FROM {{ table_name }}
    WHERE LOWER(Item.experimentId.S) LIKE '%{{ short_experiment_id }}%'
),
buckets AS (
    SELECT
        CAST(Item.endToEndLatencyMicros.N AS BIGINT) AS endToEndLatencyMicros,
        width_bucket(
            CAST(Item.endToEndLatencyMicros.N AS BIGINT), 
            (SELECT min_value FROM data_range), 
            (SELECT max_value FROM data_range), 
            10  -- Number of buckets
        ) AS bucket
    FROM {{ table_name }}
    WHERE LOWER(Item.experimentId.S) LIKE '%{{ short_experiment_id }}%'
)
SELECT
    bucket,
    COUNT(*) AS bucket_size,
    data_range.min_value + (bucket - 1) * (data_range.max_value - data_range.min_value) / 10 AS bucket_min_value,
    data_range.min_value + bucket * (data_range.max_value - data_range.min_value) / 10 AS bucket_max_value
FROM buckets, data_range
GROUP BY bucket, data_range.min_value, data_range.max_value
ORDER BY bucket;