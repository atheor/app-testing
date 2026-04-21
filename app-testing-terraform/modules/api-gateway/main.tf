# ------------------------------------------------------------------
# REST API
# ------------------------------------------------------------------
resource "aws_api_gateway_rest_api" "soap_api" {
  name        = "${var.environment}-${var.api_name}"
  description = "SOAP ScheduleService facade — routes POST /soap/schedule to the Inbound Lambda"

  tags = { Environment = var.environment }
}

# /soap resource
resource "aws_api_gateway_resource" "soap" {
  rest_api_id = aws_api_gateway_rest_api.soap_api.id
  parent_id   = aws_api_gateway_rest_api.soap_api.root_resource_id
  path_part   = "soap"
}

# /soap/schedule resource
resource "aws_api_gateway_resource" "schedule" {
  rest_api_id = aws_api_gateway_rest_api.soap_api.id
  parent_id   = aws_api_gateway_resource.soap.id
  path_part   = "schedule"
}

# ------------------------------------------------------------------
# Request validator — enforces required headers before Lambda invocation
# ------------------------------------------------------------------
resource "aws_api_gateway_request_validator" "headers_validator" {
  rest_api_id                 = aws_api_gateway_rest_api.soap_api.id
  name                        = "${var.environment}-soap-headers-validator"
  validate_request_parameters = true
  validate_request_body       = false
}

# POST method on /soap/schedule
resource "aws_api_gateway_method" "post_schedule" {
  rest_api_id          = aws_api_gateway_rest_api.soap_api.id
  resource_id          = aws_api_gateway_resource.schedule.id
  http_method          = "POST"
  authorization        = "NONE"
  request_validator_id = aws_api_gateway_request_validator.headers_validator.id

  # Content-Type: text/xml is mandatory per the SOAP/HTTP binding spec.
  # SOAPAction is optional (declared for documentation; validated in the Lambda).
  request_parameters = {
    "method.request.header.Content-Type" = true
    "method.request.header.SOAPAction"   = false
  }
}

# Lambda proxy integration
resource "aws_api_gateway_integration" "lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.soap_api.id
  resource_id             = aws_api_gateway_resource.schedule.id
  http_method             = aws_api_gateway_method.post_schedule.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.inbound_lambda_invoke_arn
}

# API Gateway → Lambda permission
resource "aws_lambda_permission" "apigw_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = var.inbound_lambda_arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.soap_api.execution_arn}/*/*"
}

# ------------------------------------------------------------------
# Gateway-level error responses — return SOAP fault envelopes instead
# of API Gateway's default JSON {"message":"..."} payloads.
# Triggered by throttling, missing routes, validator rejections, etc.
# ------------------------------------------------------------------
resource "aws_api_gateway_gateway_response" "soap_4xx" {
  rest_api_id   = aws_api_gateway_rest_api.soap_api.id
  response_type = "DEFAULT_4XX"

  response_parameters = {
    "gatewayresponse.header.Content-Type" = "'text/xml; charset=utf-8'"
  }

  response_templates = {
    "text/xml" = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>$context.error.message</faultstring></soap:Fault></soap:Body></soap:Envelope>"
  }
}

resource "aws_api_gateway_gateway_response" "soap_5xx" {
  rest_api_id   = aws_api_gateway_rest_api.soap_api.id
  response_type = "DEFAULT_5XX"

  response_parameters = {
    "gatewayresponse.header.Content-Type" = "'text/xml; charset=utf-8'"
  }

  response_templates = {
    "text/xml" = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault><faultcode>soap:Server</faultcode><faultstring>$context.error.message</faultstring></soap:Fault></soap:Body></soap:Envelope>"
  }
}

# Deployment
resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = aws_api_gateway_rest_api.soap_api.id

  depends_on = [
    aws_api_gateway_integration.lambda_integration,
    aws_api_gateway_gateway_response.soap_4xx,
    aws_api_gateway_gateway_response.soap_5xx,
  ]

  lifecycle {
    create_before_destroy = true
  }
}

# Stage
resource "aws_api_gateway_stage" "stage" {
  deployment_id = aws_api_gateway_deployment.deployment.id
  rest_api_id   = aws_api_gateway_rest_api.soap_api.id
  stage_name    = var.api_stage

  tags = { Environment = var.environment }
}
