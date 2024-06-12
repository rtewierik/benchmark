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
package io.openmessaging.tpch.client;


import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

@Slf4j
public class AlwaysRetryCondition implements RetryCondition {

    private final RetryCondition defaultRetryCondition;

    public AlwaysRetryCondition() {
        defaultRetryCondition = RetryCondition.defaultRetryCondition();
    }

    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        String exceptionMessage = context.exception().getMessage();
        Throwable cause = context.exception().getCause();
        log.debug(
                "S3 retry: shouldRetry retryCount="
                        + context.retriesAttempted()
                        + " defaultRetryCondition="
                        + defaultRetryCondition.shouldRetry(context)
                        + " httpstatus="
                        + context.httpStatusCode()
                        + " "
                        + context.exception().getClass().getSimpleName()
                        + (cause != null ? " cause=" + cause.getClass().getSimpleName() : "")
                        + " message="
                        + exceptionMessage);

        return true;
    }

    @Override
    public void requestWillNotBeRetried(RetryPolicyContext context) {
        log.debug("S3 retry: requestWillNotBeRetried retryCount=" + context.retriesAttempted());
    }

    @Override
    public void requestSucceeded(RetryPolicyContext context) {
        if (context.retriesAttempted() > 0) {
            log.debug("S3 retry: requestSucceeded retryCount=" + context.retriesAttempted());
        }
    }
}
