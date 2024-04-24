## How to interpret the monitoring results using Athena

The idea would be to re-create fresh benchmark monitoring infrastructure after each experiment, the reason being that a full DynamoDB export to S3 should be done after each experiment after which the original DynamoDB table can be removed, which would allow the relevant data to sit in S3 (expected lower infrastructure costs) separated by experiment for easier querying.

### Creating additional infrastructure required for interpretation using Athena

* At the start of the benchmarking phase, the idea would be to create one S3 bucket to store the exported DynamoDB tables. These exports should happen in DymanoDB JSON format.
* Separately, a bucket needs to be created for Athena to store the query results in. This bucket should have lifecycle policies to automatically empty files in that bucket after 1 day to keep storage costs low.

### Creating an Athena table after an experiment

Once the DynamoDB table is exported to S3, the full S3 URI for the `data` folder of that export needs to be identified and substituted in the Athena query to create a new table.

### Querying the created Athena table

Many different queries are possible. The most common ones are stored as templates in `queries/*`.
