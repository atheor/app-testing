# Debugging Guide

## Debugging Strategy by Layer

The platform has three distinct runtime layers. Identify which layer the issue is in before diving into logs:

| Symptom | Most likely layer |
|---------|-------------------|
| HTTP 400/500 from `curl` | Inbound Lambda or API Gateway |
| SOAP response received but no DB record | Processor Lambda or SQS |
| DB record exists but `status != PUBLISHED` | SNS publish step |
| Request times out / no response | API Gateway config or Lambda timeout |

---

## Local Debugging (LocalStack)

### 1. Lambda logs via CloudWatch (LocalStack)

```bash
alias awslocal='aws --endpoint-url=http://localhost:4566 --profile localstack'

# List log groups
awslocal logs describe-log-groups --query 'logGroups[*].logGroupName'

# Get the latest log stream name for Inbound Lambda
STREAM=$(awslocal logs describe-log-streams \
  --log-group-name /aws/lambda/local-inbound-lambda \
  --order-by LastEventTime \
  --descending \
  --query 'logStreams[0].logStreamName' \
  --output text)

# Tail recent log events
awslocal logs get-log-events \
  --log-group-name /aws/lambda/local-inbound-lambda \
  --log-stream-name "${STREAM}" \
  --query 'events[*].message' \
  --output text

# Same for Processor Lambda
awslocal logs get-log-events \
  --log-group-name /aws/lambda/local-processor-lambda \
  --log-stream-name "${STREAM2}"
```

### 2. Docker container logs

```bash
# Live-follow LocalStack output (shows Lambda execution output)
docker compose -f app-testing-terraform/docker-compose.yml logs -f localstack

# PostgreSQL logs
docker compose -f app-testing-terraform/docker-compose.yml logs -f postgres
```

### 3. SQS queue inspection

```bash
# Messages waiting in the main queue (should be 0 after processing)
awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/schedule-events-queue \
  --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible

# Read a message from the DLQ without deleting it
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/schedule-events-dlq \
  --max-number-of-messages 10 \
  --visibility-timeout 0
```

### 4. Invoke Lambda directly (bypass API Gateway)

Create a minimal API Gateway proxy event:
```bash
cat > /tmp/apigw-event.json <<'EOF'
{
  "httpMethod": "POST",
  "path": "/soap/schedule",
  "headers": {
    "Content-Type": "text/xml; charset=utf-8",
    "SOAPAction": "http://atheor.com/schedule/createSchedule"
  },
  "body": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://atheor.com/schedule\"><soap:Body><tns:CreateScheduleRequest><tns:scheduleId>DEBUG-001</tns:scheduleId><tns:scheduleName>Debug Test</tns:scheduleName><tns:startDate>2026-05-01</tns:startDate><tns:endDate>2026-05-31</tns:endDate></tns:CreateScheduleRequest></soap:Body></soap:Envelope>",
  "isBase64Encoded": false
}
EOF

awslocal lambda invoke \
  --function-name local-inbound-lambda \
  --payload file:///tmp/apigw-event.json \
  /tmp/lambda-response.json

cat /tmp/lambda-response.json | python3 -m json.tool
```

Invoke Processor Lambda with a raw SQS event:
```bash
cat > /tmp/sqs-event.json <<'EOF'
{
  "Records": [
    {
      "messageId": "debug-msg-001",
      "receiptHandle": "debug-receipt",
      "body": "{\"correlationId\":\"abc-123\",\"scheduleId\":\"DEBUG-001\",\"scheduleName\":\"Debug Test\",\"startDate\":\"2026-05-01\",\"endDate\":\"2026-05-31\",\"status\":\"RECEIVED\",\"createdAt\":\"2026-04-21T10:00:00Z\",\"operations\":[]}",
      "attributes": {},
      "messageAttributes": {},
      "md5OfBody": "test",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:us-east-1:000000000000:schedule-events-queue",
      "awsRegion": "us-east-1"
    }
  ]
}
EOF

awslocal lambda invoke \
  --function-name local-processor-lambda \
  --payload file:///tmp/sqs-event.json \
  /tmp/processor-response.json

cat /tmp/processor-response.json
```

### 5. Database debugging

```bash
PGPASSWORD=atheor123 psql -h localhost -U atheor -d atheor

-- Check schedule status
SELECT schedule_id, status, correlation_id, created_at, updated_at
FROM schedules
ORDER BY created_at DESC;

-- Check all operations
SELECT s.schedule_id, o.operation_id, o.operation_name, o.operation_type
FROM operations o
JOIN schedules s ON s.schedule_id = o.schedule_id;

-- Check for failed schedules
SELECT * FROM schedules WHERE status = 'FAILED';

-- Watch for new inserts in real time (run while sending requests)
\watch 2
SELECT schedule_id, status, created_at FROM schedules ORDER BY created_at DESC LIMIT 5;
```

---

## Debugging in AWS

### CloudWatch Logs

Each Lambda function writes to a dedicated log group:
- `/aws/lambda/<env>-inbound-lambda`
- `/aws/lambda/<env>-processor-lambda`

AWS Console path: **CloudWatch → Log groups → `/aws/lambda/prod-inbound-lambda`**

AWS CLI:
```bash
# Tail logs in real time (requires awslogs or CloudWatch Live Tail in console)
aws logs tail /aws/lambda/prod-inbound-lambda --follow

# Filter for errors only
aws logs filter-log-events \
  --log-group-name /aws/lambda/prod-inbound-lambda \
  --filter-pattern "ERROR"
```

### X-Ray Tracing (Optional)

To enable tracing:
1. Add to the Lambda Terraform resource:
   ```hcl
   tracing_config {
     mode = "Active"
   }
   ```
2. Add IAM permission `xray:PutTraceSegments`, `xray:PutTelemetryRecords` to the Lambda role
3. View traces in **AWS Console → X-Ray → Traces**

---

## Common Issues and Fixes

### `ClassNotFoundException` or `NoClassDefFoundError` in Lambda

**Cause:** Dependency not included in the fat JAR.

**Fix:** Check the Maven Shade plugin configuration in the failing Lambda's `pom.xml`. Run:
```bash
# List all classes in the JAR
jar tf app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar | grep -i "missing-class"
```

Ensure the missing module is listed as a `<dependency>` in the Lambda's `pom.xml`.

---

### `JAXBException: class not known to this context`

**Cause:** The JAXB class is not registered in `JAXBContext.newInstance(...)`.

**Fix:** In `SoapMessageParser` and `JaxbConfig`, ensure the class is in the `newInstance(...)` call:
```java
JAXBContext.newInstance(CreateScheduleRequest.class, CreateScheduleResponse.class);
```

---

### `SAXParseException: DOCTYPE is disallowed`

**Cause:** A SOAP request body contains a `<!DOCTYPE ...>` declaration. This is blocked intentionally to prevent XXE attacks.

**Fix:** Remove the DOCTYPE declaration from the SOAP envelope being sent.

---

### Spring context fails to start — `Could not obtain connection`

**Cause:** Processor Lambda cannot reach PostgreSQL.

**Fix checklist:**
1. Is PostgreSQL running? `docker compose ps postgres`
2. Is `DB_URL` using `host.docker.internal` (not `localhost`) for Lambda in Docker?
3. Is the DB password correct? Check Lambda env var `DB_PASSWORD`
4. Is the VPC/security group allowing the Lambda to reach RDS (AWS only)?

---

### SQS messages stuck in DLQ

**Cause:** Processor Lambda failing after 3 attempts.

**Steps:**
1. Read the DLQ message body:
   ```bash
   awslocal sqs receive-message \
     --queue-url http://localhost:4566/000000000000/schedule-events-dlq
   ```
2. Check Processor Lambda logs for the error
3. Fix the root cause
4. Redrive from DLQ back to the main queue:
   ```bash
   # Move all DLQ messages back to the main queue for reprocessing
   awslocal sqs start-message-move-task \
     --source-arn arn:aws:sqs:us-east-1:000000000000:schedule-events-dlq \
     --destination-arn arn:aws:sqs:us-east-1:000000000000:schedule-events-queue
   ```

---

### Lambda cold start taking > 15 seconds

**Cause:** Spring context initialisation is slow due to eager bean loading or large classpath scan.

**Mitigations already in place:**
- `spring.main.lazy-initialization=true`
- `WebApplicationType.NONE`
- HikariCP pool size = 3

**Additional options:**
- Use [AWS Lambda SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html) (Java 21 supported) — takes a snapshot after init phase
- Enable SnapStart in Terraform: add `snap_start { apply_on = "PublishedVersions" }` to the processor Lambda resource
- Increase `lambda_memory_mb` (CPU scales with memory; more memory = faster JVM startup)

---

### `MapStruct` mapper returns `null` for a field

**Cause:** Source field name doesn't match target, or the `@Mapping` annotation is missing.

**Fix:** Add an explicit mapping:
```java
@Mapping(target = "targetField", source = "sourceField")
```

Rebuild with `-am` to trigger annotation processing:
```bash
mvn compile -pl app-testing-mapping -am
```

Check `target/generated-sources/annotations/` for the generated mapper class to inspect what MapStruct produced.

---

## Logging Reference

All loggers use SLF4J + Logback. Log level is configured in `app-testing-framework/src/main/resources/logback.xml`.

Key log messages to look for:

| Module | Log message | Meaning |
|--------|-------------|---------|
| `InboundLambdaHandler` | `Inbound SOAP request received [requestId=...]` | Lambda invoked successfully |
| `SoapMessageParser` | `Parsing SOAP CreateScheduleRequest` | Parsing started |
| `SqsEventPublisher` | `Published event to SQS [messageId=..., correlationId=...]` | SQS publish succeeded |
| `SqsConsumerLambdaHandler` | `SQS Consumer Lambda invoked [messageCount=...]` | Processor Lambda invoked |
| `EventProcessorService` | `Processing ScheduleEvent [scheduleId=..., correlationId=...]` | Processing started |
| `BackendService` | `Persisting schedule [scheduleId=...]` | DB write started |
| `SnsPublisherService` | `Published event to SNS [messageId=..., scheduleId=...]` | SNS publish succeeded |

To increase log verbosity for debugging, set the log level in `logback.xml`:
```xml
<logger name="com.atheor" level="DEBUG"/>
```

Or pass it as a system property:
```bash
mvn test -Dlogging.level.com.atheor=DEBUG
```
