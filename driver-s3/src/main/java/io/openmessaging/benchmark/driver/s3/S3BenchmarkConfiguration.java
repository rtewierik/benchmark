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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

public class S3BenchmarkConfiguration {

    @Getter private static final List<String> s3Uris;
    @Getter private static final String region;
    @Getter private static final boolean isTpcH;

    static {
        s3Uris = S3BenchmarkConfiguration.getS3UrisFromEnvironment();
        region = System.getenv("REGION");
        isTpcH = Boolean.parseBoolean(System.getenv("IS_TPC_H"));
    }

    private static List<String> getS3UrisFromEnvironment() {
        String s3Uris = System.getenv("S3_URIS");
        return (s3Uris != null) ? Arrays.asList(s3Uris.split(",")) : Collections.emptyList();
    }
}
