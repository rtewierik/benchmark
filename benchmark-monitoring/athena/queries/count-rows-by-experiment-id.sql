SELECT
  {{ table_name }}.Item.experimentId.S AS experimentId,
  COUNT(*) AS group_size
FROM
  {{ table_name }}
GROUP BY
  {{ table_name }}.Item.experimentId.S;
