variable "environment"  { type = string }
variable "queue_name"   { type = string }
variable "dlq_name"     { type = string }

# ------------------------------------------------------------------
# Dead-letter queue (DLQ)
# ------------------------------------------------------------------
resource "aws_sqs_queue" "dlq" {
  name                      = "${var.environment}-${var.dlq_name}"
  message_retention_seconds = 1209600  # 14 days

  tags = {
    Environment = var.environment
    Module      = "sqs"
    Purpose     = "dead-letter-queue"
  }
}

# ------------------------------------------------------------------
# Main queue (receives inbound SOAP events)
# ------------------------------------------------------------------
resource "aws_sqs_queue" "main" {
  name                       = "${var.environment}-${var.queue_name}"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 86400  # 1 day

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 3
  })

  tags = {
    Environment = var.environment
    Module      = "sqs"
    Purpose     = "schedule-events"
  }
}
