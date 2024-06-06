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
package io.openmessaging.benchmark.driver.s3;


import io.openmessaging.benchmark.common.EnvironmentConfiguration;

import java.util.ArrayList;
import java.util.List;

public class S3BenchmarkConfiguration {

    public static final String region;
    public static final String accountId;
    public static final boolean isTpcH;
    public static final List<String> s3Uris;

    static {
        region = System.getenv("REGION");
        accountId = System.getenv("ACCOUNT_ID");
        isTpcH = Boolean.parseBoolean(System.getenv("IS_TPC_H"));
        s3Uris = S3BenchmarkConfiguration.getS3UrisFromEnvironment();
    }

    private static List<String> getS3UrisFromEnvironment() {
        ArrayList<String> s3Uris = new ArrayList<>();
        if (isTpcH) {
            s3Uris.add(getS3Uri("result", 333, false));
            s3Uris.add(getS3Uri("map", 666, false));
            for (int i = 0; i < EnvironmentConfiguration.getNumberOfConsumers(); i++) {
                String id = String.format("reduce%s", i);
                s3Uris.add(getS3Uri(id, i, true));
            }
        } else {
            for (int i = 0; i < EnvironmentConfiguration.getNumberOfConsumers(); i++) {
                String id = String.format("default%s", i);
                s3Uris.add(getS3Uri(id, i, true));
            }
        }
        return s3Uris;
    }

    private static String getS3Uri(String id, Integer index, boolean iterative) {
        int bucketIndex = iterative ? 1 + (index / 50) : 0;
        return String.format("s3://benchmarking-events%d/%d-s3-consumer-lambda-%s", bucketIndex, index, id);
    }
}
