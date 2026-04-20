output "sqs_queue_url" {
  description = "URL of the SQS schedule-events queue"
  value       = module.sqs.queue_url
}

output "sqs_queue_arn" {
  description = "ARN of the SQS schedule-events queue"
  value       = module.sqs.queue_arn
}

output "sqs_dlq_url" {
  description = "URL of the SQS dead-letter queue"
  value       = module.sqs.dlq_url
}

output "sns_topic_arn" {
  description = "ARN of the SNS schedule-events topic"
  value       = module.sns.topic_arn
}

output "api_gateway_endpoint" {
  description = "Invoke URL for the SOAP endpoint on API Gateway"
  value       = module.api_gateway.invoke_url
}

output "inbound_lambda_arn" {
  description = "ARN of the Inbound Lambda function"
  value       = module.lambda.inbound_lambda_arn
}

output "processor_lambda_arn" {
  description = "ARN of the SQS Consumer Lambda function"
  value       = module.lambda.processor_lambda_arn
}
