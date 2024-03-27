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

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class S3BenchmarkConfiguration {

    @Getter
    private static final String sqsUri;
    @Getter
    private static final List<String> snsUris;
    @Getter
    private static final String region;
    @Getter
    private static final boolean isTpcH;

    static {
        sqsUri = System.getenv("SQS_URI");
        snsUris = S3BenchmarkConfiguration.getSnsUrisFromEnvironment();
        region = System.getenv("REGION");
        isTpcH = Boolean.parseBoolean(System.getenv("IS_TPC_H"));
    }

    private static List<String> getSnsUrisFromEnvironment() {
        String snsUris = System.getenv("SNS_URIS");
        return (snsUris != null) ? Arrays.asList(snsUris.split(",")) : Collections.emptyList();
    }
}
