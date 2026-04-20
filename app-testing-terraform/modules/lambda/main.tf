variable "environment"            { type = string }
variable "aws_region"             { type = string }
variable "localstack_endpoint"    { type = string }
variable "lambda_runtime"         { type = string }
variable "lambda_memory_mb"       { type = number }
variable "lambda_timeout_seconds" { type = number }
variable "inbound_lambda_jar"     { type = string }
variable "processor_lambda_jar"   { type = string }
variable "sqs_queue_url"          { type = string }
variable "sqs_queue_arn"          { type = string }
variable "sns_topic_arn"          { type = string }
variable "db_url"                 { type = string }
variable "db_username"            { type = string; sensitive = true }
variable "db_password"            { type = string; sensitive = true }

# ------------------------------------------------------------------
# IAM — shared execution role for both Lambda functions
# ------------------------------------------------------------------
resource "aws_iam_role" "lambda_exec" {
  name = "${var.environment}-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = { Environment = var.environment }
}

resource "aws_iam_role_policy" "lambda_permissions" {
  name = "${var.environment}-lambda-permissions"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "AllowSqsSend"
        Effect   = "Allow"
        Action   = ["sqs:SendMessage", "sqs:GetQueueAttributes"]
        Resource = var.sqs_queue_arn
      },
      {
        Sid      = "AllowSqsReceive"
        Effect   = "Allow"
        Action   = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = var.sqs_queue_arn
      },
      {
        Sid      = "AllowSnsPublish"
        Effect   = "Allow"
        Action   = "sns:Publish"
        Resource = var.sns_topic_arn
      },
      {
        Sid      = "AllowCloudWatchLogs"
        Effect   = "Allow"
        Action   = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# ------------------------------------------------------------------
# Lambda 1 — Inbound (API Gateway → SQS)
# ------------------------------------------------------------------
resource "aws_lambda_function" "inbound" {
  function_name = "${var.environment}-inbound-lambda"
  filename      = var.inbound_lambda_jar
  handler       = "com.atheor.inbound.handler.InboundLambdaHandler::handleRequest"
  runtime       = var.lambda_runtime
  memory_size   = var.lambda_memory_mb
  timeout       = var.lambda_timeout_seconds
  role          = aws_iam_role.lambda_exec.arn

  source_code_hash = filebase64sha256(var.inbound_lambda_jar)

  environment {
    variables = {
      AWS_REGION      = var.aws_region
      SQS_QUEUE_URL   = var.sqs_queue_url
      SQS_ENDPOINT    = var.localstack_endpoint
    }
  }

  tags = {
    Environment = var.environment
    Module      = "lambda"
    Purpose     = "soap-inbound"
  }
}

# ------------------------------------------------------------------
# Lambda 2 — SQS Consumer (SQS → PostgreSQL → SNS)
# ------------------------------------------------------------------
resource "aws_lambda_function" "processor" {
  function_name = "${var.environment}-processor-lambda"
  filename      = var.processor_lambda_jar
  handler       = "com.atheor.lambda.handler.SqsConsumerLambdaHandler::handleRequest"
  runtime       = var.lambda_runtime
  memory_size   = var.lambda_memory_mb
  timeout       = var.lambda_timeout_seconds
  role          = aws_iam_role.lambda_exec.arn

  source_code_hash = filebase64sha256(var.processor_lambda_jar)

  environment {
    variables = {
      AWS_REGION    = var.aws_region
      SNS_TOPIC_ARN = var.sns_topic_arn
      SNS_ENDPOINT  = var.localstack_endpoint
      DB_URL        = var.db_url
      DB_USERNAME   = var.db_username
      DB_PASSWORD   = var.db_password
    }
  }

  tags = {
    Environment = var.environment
    Module      = "lambda"
    Purpose     = "sqs-processor"
  }
}

# ------------------------------------------------------------------
# SQS → Lambda 2 event source mapping
# ------------------------------------------------------------------
resource "aws_lambda_event_source_mapping" "sqs_to_processor" {
  event_source_arn                   = var.sqs_queue_arn
  function_name                      = aws_lambda_function.processor.arn
  batch_size                         = 5
  maximum_batching_window_in_seconds = 10
  enabled                            = true
}
