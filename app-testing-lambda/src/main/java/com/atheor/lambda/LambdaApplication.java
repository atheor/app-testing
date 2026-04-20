package com.atheor.lambda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot bootstrap for the SQS Consumer Lambda.
 *
 * <p>The web application type is set to {@link WebApplicationType#NONE} so that
 * no embedded servlet container is started inside the Lambda runtime.
 * The Spring context is initialised once and reused across warm invocations.
 */
@SpringBootApplication(scanBasePackages = {
        "com.atheor.lambda",
        "com.atheor.backend"
})
public class LambdaApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(LambdaApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
