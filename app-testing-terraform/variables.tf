variable "aws_region" {
  description = "AWS region (use us-east-1 for LocalStack)"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (local | dev | prod)"
  type        = string
  default     = "local"
}

variable "localstack_endpoint" {
  description = "LocalStack unified endpoint URL (host-side, used by Terraform provider)"
  type        = string
  default     = "http://localhost:4566"
}

variable "lambda_aws_endpoint" {
  description = "LocalStack endpoint URL as seen from inside a Lambda Docker container"
  type        = string
  default     = "http://host.docker.internal:4566"
}

# ------------------------------------------------------------------
# Lambda
# ------------------------------------------------------------------
variable "inbound_lambda_jar" {
  description = "Path to the fat JAR for the Inbound Lambda (app-testing-inbound)"
  type        = string
  default     = "../app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar"
}

variable "processor_lambda_jar" {
  description = "Path to the fat JAR for the SQS Consumer Lambda (app-testing-lambda)"
  type        = string
  default     = "../app-testing-lambda/target/app-testing-lambda-1.0.0-SNAPSHOT.jar"
}

variable "lambda_runtime" {
  description = "Lambda runtime identifier"
  type        = string
  default     = "java21"
}

variable "lambda_memory_mb" {
  description = "Memory (MB) allocated to each Lambda function"
  type        = number
  default     = 512
}

variable "lambda_timeout_seconds" {
  description = "Maximum Lambda execution duration in seconds"
  type        = number
  default     = 30
}

# ------------------------------------------------------------------
# Database
# ------------------------------------------------------------------
variable "db_url" {
  description = "JDBC connection URL for PostgreSQL"
  type        = string
  default     = "jdbc:postgresql://host.docker.internal:5432/atheor"
  sensitive   = false
}

variable "db_username" {
  description = "PostgreSQL username"
  type        = string
  default     = "atheor"
  sensitive   = true
}

variable "db_password" {
  description = "PostgreSQL password"
  type        = string
  default     = "atheor123"
  sensitive   = true
}

# ------------------------------------------------------------------
# SQS / SNS names
# ------------------------------------------------------------------
variable "sqs_queue_name" {
  description = "Name of the SQS queue that receives inbound SOAP events"
  type        = string
  default     = "schedule-events-queue"
}

variable "sqs_dlq_name" {
  description = "Name of the dead-letter queue for failed messages"
  type        = string
  default     = "schedule-events-dlq"
}

variable "sns_topic_name" {
  description = "Name of the SNS topic that receives processed events"
  type        = string
  default     = "schedule-events-topic"
}

# ------------------------------------------------------------------
# API Gateway
# ------------------------------------------------------------------
variable "api_name" {
  description = "Name of the REST API in API Gateway"
  type        = string
  default     = "schedule-service-api"
}

variable "api_stage" {
  description = "API Gateway deployment stage"
  type        = string
  default     = "v1"
}
