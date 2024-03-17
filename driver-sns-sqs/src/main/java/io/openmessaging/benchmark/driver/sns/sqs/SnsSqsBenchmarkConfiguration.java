package io.openmessaging.benchmark.driver.sns.sqs;

import lombok.Getter;

public class SnsSqsBenchmarkConfiguration {

    @Getter
    private static final String sqsUri;
    @Getter
    private static final String region;

    static {
        sqsUri = System.getenv("SQS_URI");
        region = System.getenv("AWS_REGION");
    }
}
