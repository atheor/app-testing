package com.atheor.lambda.config;

import com.atheor.common.constant.AppConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

/**
 * Spring configuration that provides AWS SDK v2 clients as Spring beans.
 *
 * <p>Both real AWS and LocalStack are supported: set the {@code SNS_ENDPOINT}
 * environment variable to the LocalStack endpoint (e.g. {@code http://localhost:4566})
 * to redirect traffic.
 */
@Configuration
public class AwsClientConfig {

    @Bean
    public SnsClient snsClient() {
        String region      = System.getenv().getOrDefault(AppConstants.ENV_AWS_REGION, AppConstants.DEFAULT_REGION);
        String snsEndpoint = System.getenv(AppConstants.ENV_SNS_ENDPOINT);

        SnsClient.Builder builder = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create());

        if (snsEndpoint != null && !snsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(snsEndpoint));
        }

        return builder.build();
    }
}
