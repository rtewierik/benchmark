### Locations to configure memory in case of changing instance size

* `bkenv.sh` requires modification of allocated environment variables (`common.sh` also applies memory configuration, but only as defaults)
* `config.properties` requires modification of `pravegaservice.cache.size.max`. Note that this memory is always fully allocated even if not used, and the Pravega Segment Store is running on the Bookkeeper EC2 instance along with another service. **The sum of allocated memory for both services should be less than the total memory available for the EC2 instance type**.
* `pravega-segmentstore.service` requires modification of `Environment`
* `deploy.yaml`requires modification of the `Configure memory` task