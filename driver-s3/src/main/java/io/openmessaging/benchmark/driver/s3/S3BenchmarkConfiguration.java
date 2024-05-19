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


import java.util.ArrayList;
import java.util.List;

public class S3BenchmarkConfiguration {

    public static final String region;
    public static final String accountId;
    public static final Integer numberOfConsumers;
    public static final boolean isTpcH;
    public static final List<String> s3Uris;

    static {
        region = System.getenv("REGION");
        accountId = System.getenv("ACCOUNT_ID");
        numberOfConsumers = Integer.parseInt(System.getenv("NUMBER_OF_CONSUMERS"));
        isTpcH = Boolean.parseBoolean(System.getenv("IS_TPC_H"));
        s3Uris = S3BenchmarkConfiguration.getS3UrisFromEnvironment();
    }

    private static List<String> getS3UrisFromEnvironment() {
        ArrayList<String> s3Uris = new ArrayList<>();
        if (isTpcH) {
            s3Uris.add(getS3Uri("Map"));
            s3Uris.add(getS3Uri("Result"));
            for (int i = 0; i < numberOfConsumers; i++) {
                String id = String.format("Reduce%s", i);
                s3Uris.add(getS3Uri(id));
            }
        } else {
            for (int i = 0; i < numberOfConsumers; i++) {
                String id = String.format("Default%s", i);
                s3Uris.add(getS3Uri(id));
            }
        }
        return s3Uris;
    }

    private static String getS3Uri(String id) {
        return String.format("s3://benchmarking-events/s3-consumer-lambda-%s", id);
    }
}
