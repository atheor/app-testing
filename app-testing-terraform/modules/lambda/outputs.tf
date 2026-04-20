output "inbound_lambda_arn" {
  description = "ARN of the Inbound Lambda function"
  value       = aws_lambda_function.inbound.arn
}

output "inbound_lambda_invoke_arn" {
  description = "Invoke ARN of the Inbound Lambda (used by API Gateway)"
  value       = aws_lambda_function.inbound.invoke_arn
}

output "processor_lambda_arn" {
  description = "ARN of the SQS Consumer Lambda function"
  value       = aws_lambda_function.processor.arn
}
