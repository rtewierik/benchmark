WITH data_range AS (
    SELECT 
        MIN(CAST(Item.processTimestamp.N AS BIGINT)) AS min_value, 
        MAX(CAST(Item.processTimestamp.N AS BIGINT)) AS max_value
    FROM final_tpc_h
    WHERE LOWER(Item.experimentId.S) LIKE '%{{ short_experiment_id}}%' AND Item.endToEndLatencyMicros.N > 0 AND Item.payloadLength.N >= 459 AND Item.payloadLength.N < 512
),
buckets AS (
    SELECT
        CAST(Item.processTimestamp.N AS BIGINT) AS processTimestamp,
        width_bucket(
            CAST(Item.processTimestamp.N AS BIGINT), 
            (SELECT min_value FROM data_range), 
            (SELECT max_value FROM data_range), 
            20  -- Number of buckets
        ) AS bucket
    FROM final_tpc_h
    WHERE LOWER(Item.experimentId.S) LIKE '%{{ short_experiment_id }}%' AND Item.endToEndLatencyMicros.N > 0 AND Item.payloadLength.N >= 459 AND Item.payloadLength.N < 512
)
SELECT
    bucket,
    COUNT(*) AS bucket_size,
    (bucket - 1) * (data_range.max_value - data_range.min_value) / 20 AS bucket_min_value,
    bucket * (data_range.max_value - data_range.min_value) / 20 AS bucket_max_value
FROM buckets, data_range
GROUP BY bucket, data_range.min_value, data_range.max_value
ORDER BY bucket;