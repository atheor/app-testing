package com.atheor.common.constant;

public final class AppConstants {

    private AppConstants() {
    }

    // SOAP namespaces
    public static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SCHEDULE_NAMESPACE = "http://atheor.com/schedule";

    // SOAP actions
    public static final String CREATE_SCHEDULE_ACTION = SCHEDULE_NAMESPACE + "/createSchedule";

    // Content types
    public static final String CONTENT_TYPE_SOAP = "text/xml; charset=utf-8";
    public static final String CONTENT_TYPE_JSON = "application/json";

    // Environment variable keys
    public static final String ENV_SQS_QUEUE_URL       = "SQS_QUEUE_URL";
    public static final String ENV_SNS_TOPIC_ARN        = "SNS_TOPIC_ARN";
    public static final String ENV_AWS_REGION           = "AWS_REGION";
    public static final String ENV_SQS_ENDPOINT         = "SQS_ENDPOINT";
    public static final String ENV_SNS_ENDPOINT         = "SNS_ENDPOINT";
    public static final String ENV_DB_URL               = "DB_URL";
    public static final String ENV_DB_USERNAME          = "DB_USERNAME";
    public static final String ENV_DB_PASSWORD          = "DB_PASSWORD";

    // Default values
    public static final String DEFAULT_REGION           = "us-east-1";
    public static final String DEFAULT_STATUS_CREATED   = "CREATED";
    public static final String DEFAULT_STATUS_FAILED    = "FAILED";

    // HTTP headers
    public static final String HEADER_CONTENT_TYPE      = "Content-Type";
    public static final String HEADER_SOAP_ACTION       = "SOAPAction";
    public static final String HEADER_CORRELATION_ID    = "X-Correlation-Id";
}
