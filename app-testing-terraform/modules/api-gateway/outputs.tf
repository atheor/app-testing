output "invoke_url" {
  description = "Base invoke URL for the SOAP endpoint — append /soap/schedule to call the service"
  value       = "${aws_api_gateway_stage.stage.invoke_url}/soap/schedule"
}

output "rest_api_id" {
  description = "REST API ID"
  value       = aws_api_gateway_rest_api.soap_api.id
}
