variable "environment"            { type = string }
variable "aws_region"             { type = string }
variable "localstack_endpoint"    { type = string }
variable "lambda_aws_endpoint"    { type = string }
variable "lambda_runtime"         { type = string }
variable "lambda_memory_mb"       { type = number }
variable "lambda_timeout_seconds" { type = number }
variable "inbound_lambda_jar"     { type = string }
variable "processor_lambda_jar"   { type = string }
variable "sqs_queue_url"          { type = string }
variable "sqs_queue_arn"          { type = string }
variable "sns_topic_arn"          { type = string }
variable "db_url"                 { type = string }
variable "db_username" {
  type      = string
  sensitive = true
}
variable "db_password" {
  type      = string
  sensitive = true
}
