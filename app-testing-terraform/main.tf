terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
  }
}

# =============================================================
# Provider — LocalStack (test credentials, custom endpoints)
# =============================================================
provider "aws" {
  region = var.aws_region

  # LocalStack test credentials — never use real credentials here
  access_key = "test"
  secret_key = "test"

  # Skip validations that require a real AWS account
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    apigateway = var.localstack_endpoint
    lambda     = var.localstack_endpoint
    sqs        = var.localstack_endpoint
    sns        = var.localstack_endpoint
    iam        = var.localstack_endpoint
    sts        = var.localstack_endpoint
    cloudwatch = var.localstack_endpoint
    logs       = var.localstack_endpoint
  }
}

# =============================================================
# Modules
# =============================================================

module "sqs" {
  source        = "./modules/sqs"
  environment   = var.environment
  queue_name    = var.sqs_queue_name
  dlq_name      = var.sqs_dlq_name
}

module "sns" {
  source      = "./modules/sns"
  environment = var.environment
  topic_name  = var.sns_topic_name
}

module "lambda" {
  source = "./modules/lambda"

  environment            = var.environment
  aws_region             = var.aws_region
  localstack_endpoint    = var.localstack_endpoint
  lambda_aws_endpoint    = var.lambda_aws_endpoint
  lambda_runtime         = var.lambda_runtime
  lambda_memory_mb       = var.lambda_memory_mb
  lambda_timeout_seconds = var.lambda_timeout_seconds

  inbound_lambda_jar   = var.inbound_lambda_jar
  processor_lambda_jar = var.processor_lambda_jar

  sqs_queue_url = module.sqs.queue_url
  sqs_queue_arn = module.sqs.queue_arn
  sns_topic_arn = module.sns.topic_arn

  db_url      = var.db_url
  db_username = var.db_username
  db_password = var.db_password
}

module "api_gateway" {
  source = "./modules/api-gateway"

  environment              = var.environment
  api_name                 = var.api_name
  api_stage                = var.api_stage
  inbound_lambda_invoke_arn = module.lambda.inbound_lambda_invoke_arn
  inbound_lambda_arn        = module.lambda.inbound_lambda_arn
}
