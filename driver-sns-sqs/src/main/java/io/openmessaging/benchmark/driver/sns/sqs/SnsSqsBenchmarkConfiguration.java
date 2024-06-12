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
package io.openmessaging.benchmark.driver.sns.sqs;


import io.openmessaging.benchmark.common.EnvironmentConfiguration;
import java.util.ArrayList;
import java.util.List;

public class SnsSqsBenchmarkConfiguration {

    public static final String sqsUri;
    public static final String region;
    public static final String accountId;
    public static final boolean isTpcH;
    public static final List<String> snsUris;

    static {
        sqsUri = System.getenv("SQS_URI");
        region = System.getenv("REGION");
        accountId = System.getenv("ACCOUNT_ID");
        Boolean isTpcHFromEnv;
        try {
            isTpcHFromEnv = Boolean.parseBoolean(System.getenv("IS_TPC_H"));
        } catch (Throwable ignored) {
            isTpcHFromEnv = false;
        }
        isTpcH = isTpcHFromEnv;
        snsUris = SnsSqsBenchmarkConfiguration.getSnsUrisFromEnvironment();
    }

    private static List<String> getSnsUrisFromEnvironment() {
        ArrayList<String> snsUris = new ArrayList<>();
        if (isTpcH) {
            snsUris.add(getSnsUri("result"));
            snsUris.add(getSnsUri("map"));
            for (int i = 0; i < EnvironmentConfiguration.getNumberOfConsumers(); i++) {
                String id = String.format("reduce%s", i);
                snsUris.add(getSnsUri(id));
            }
        } else {
            for (int i = 0; i < EnvironmentConfiguration.getNumberOfConsumers(); i++) {
                String id = String.format("default%s", i);
                snsUris.add(getSnsUri(id));
            }
        }
        return snsUris;
    }

    private static String getSnsUri(String id) {
        return String.format("arn:aws:sns:%s:%s:sns-sqs-consumer-lambda-%s", region, accountId, id);
    }
}
