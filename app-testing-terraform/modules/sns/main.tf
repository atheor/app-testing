# ------------------------------------------------------------------
# SNS Topic — receives processed ScheduleEvents
# ------------------------------------------------------------------
resource "aws_sns_topic" "schedule_events" {
  name = "${var.environment}-${var.topic_name}"

  tags = {
    Environment = var.environment
    Module      = "sns"
    Purpose     = "schedule-events-notification"
  }
}
