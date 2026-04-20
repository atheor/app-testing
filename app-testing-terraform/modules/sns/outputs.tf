output "topic_arn" {
  description = "ARN of the SNS schedule-events topic"
  value       = aws_sns_topic.schedule_events.arn
}

output "topic_name" {
  description = "Name of the SNS topic"
  value       = aws_sns_topic.schedule_events.name
}
