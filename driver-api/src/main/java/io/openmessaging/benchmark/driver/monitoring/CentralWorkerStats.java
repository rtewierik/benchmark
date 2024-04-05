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
package io.openmessaging.benchmark.driver.monitoring;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import io.openmessaging.benchmark.driver.EnvironmentConfiguration;

public class CentralWorkerStats implements WorkerStats {

    private static final AmazonSQS sqsClient =
            AmazonSQSClientBuilder.standard()
                    .withRegion(EnvironmentConfiguration.getMonitoringSqsUri())
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();

    @Override
    public void recordMessageReceived(
            long payloadLength, long endToEndLatencyMicros, String experimentId, String messageId, boolean isTpcH) {
        // TO DO: Send event to SQS.
    }

    @Override
    public void recordMessageProduced(
            long payloadLength,
            long intendedSendTimeNs,
            long sendTimeNs,
            long nowNs,
            String experimentId,
            String messageId,
            boolean isTpcH,
            boolean isError
    ) {
        // TO DO: Send event to SQS.
    }
}
