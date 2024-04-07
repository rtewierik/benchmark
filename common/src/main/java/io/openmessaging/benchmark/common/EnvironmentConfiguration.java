/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openmessaging.benchmark.common;


import lombok.Getter;

public class EnvironmentConfiguration {

    @Getter private static final boolean produceWithAllWorkers;
    @Getter private static final boolean skipReadinessCheck;
    @Getter private static final String monitoringSqsUri;
    @Getter private static final boolean isCloudMonitoringEnabled;
    @Getter private static final String region;

    static {
        produceWithAllWorkers = Boolean.parseBoolean(System.getenv("PRODUCE_WITH_ALL_WORKERS"));
        skipReadinessCheck = Boolean.parseBoolean(System.getenv("SKIP_READINESS_CHECK"));
        monitoringSqsUri = System.getenv("MONITORING_SQS_URI");
        isCloudMonitoringEnabled = Boolean.parseBoolean(System.getenv("IS_CLOUD_MONITORING_ENABLED"));
        region = System.getenv("REGION");
    }
}
