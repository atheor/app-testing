package com.atheor.inbound.config;

import com.atheor.common.constant.AppConstants;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

/**
 * Builds AWS SDK v2 clients.
 *
 * <p>Supports LocalStack via the {@code SQS_ENDPOINT} environment variable.
 * When running on real AWS, the environment variable is absent and the SDK
 * uses the standard endpoint resolution.
 */
public final class AwsClientConfig {

    private AwsClientConfig() {
    }

    public static SqsClient sqsClient() {
        String region      = System.getenv().getOrDefault(AppConstants.ENV_AWS_REGION, AppConstants.DEFAULT_REGION);
        String sqsEndpoint = System.getenv(AppConstants.ENV_SQS_ENDPOINT);

        var builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create());

        if (sqsEndpoint != null && !sqsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(sqsEndpoint));
        }

        return builder.build();
    }
}
