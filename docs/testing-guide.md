# Testing Guide

## Test Layers

The project has three distinct test layers:

| Layer | Module | Technology | Scope |
|-------|--------|-----------|-------|
| Unit | Any module | JUnit 5 | Single class, no I/O |
| Integration | `app-testing-e2e` | JUnit 5, REST Assured, SOAP client | Full stack against running environment |
| UI / E2E | `app-testing-e2e` | Selenium 4, WebDriverManager | Browser-level flows |

---

## Running Tests

### All unit tests

```bash
mvn test
```

### Full build + tests (includes integration tests if environment is configured)

```bash
mvn verify
```

### Skip tests during build

```bash
mvn package -DskipTests
```

### Run a specific test class

```bash
mvn test -pl app-testing-e2e -Dtest=InboundCreateScheduleCreateOperationTest
```

### Run a specific test method

```bash
mvn test -pl app-testing-e2e \
  -Dtest="InboundCreateScheduleCreateOperationTest#shouldCreateScheduleThenCreateOperation"
```

---

## E2E Tests — `app-testing-e2e`

### Test configuration

Test properties live in `app-testing-e2e/src/test/resources/test.properties`. Values can be overridden via `-D` system properties:

```bash
mvn test -pl app-testing-e2e \
  -Dadms.endpoint=http://localhost:4566/restapis/XXXXXXXX/local/_user_request_/soap/schedule \
  -Dadms.username=test \
  -Dadms.password=test
```

All configurable properties:

| Property | Default | Description |
|----------|---------|-------------|
| `browser` | `chrome` | Browser for Selenium tests (`chrome` or `edge`) |
| `browser.headless` | `false` | Run browser headless |
| `wait.timeout.seconds` | `10` | Explicit wait timeout |
| `saucedemo.base.url` | `https://www.saucedemo.com` | SauceDemo URL |
| `saucedemo.username` | `standard_user` | SauceDemo login |
| `saucedemo.password` | `secret_sauce` | SauceDemo password |
| `adms.endpoint` | _(blank)_ | SOAP endpoint URL — required for SOAP tests |
| `adms.username` | _(blank)_ | SOAP auth username |
| `adms.password` | _(blank)_ | SOAP auth password |

> Tests with missing or placeholder `adms.endpoint` are **skipped** automatically via `Assumptions.assumeTrue(...)`, not failed.

---

### SOAP E2E Test — `InboundCreateScheduleCreateOperationTest`

**Purpose:** Sends a real `CreateSchedule` SOAP request end-to-end and asserts the response contains a non-blank `scheduleId` and `operationId`.

**Requires:** The full local stack running (LocalStack + Terraform applied). See [Local Setup](local-setup.md).

**Run against LocalStack:**

```bash
# Get endpoint from Terraform
cd app-testing-terraform
ENDPOINT=$(terraform output -raw api_gateway_endpoint)
cd ..

mvn test -pl app-testing-e2e \
  -Dtest=InboundCreateScheduleCreateOperationTest \
  -Dadms.endpoint="${ENDPOINT}"
```

**Run against AWS:**

```bash
mvn test -pl app-testing-e2e \
  -Dtest=InboundCreateScheduleCreateOperationTest \
  -Dadms.endpoint="https://xxxxxxxxxx.execute-api.us-east-1.amazonaws.com/v1/soap/schedule"
```

**Flow exercised:**
1. `InboundSoapServiceClient` sends SOAP `CreateScheduleRequest` to the endpoint
2. `CreateScheduleOperationWorkflow` parses the SOAP response
3. Assertions confirm `scheduleId` and `operationId` are non-blank

---

### UI E2E Test — `SauceDemoLoginTest`

**Purpose:** Browser-level smoke test using Selenium. Logs into SauceDemo, adds an item to cart, and starts checkout.

**Requirements:** Chrome browser installed (or Edge if configured).

```bash
mvn test -pl app-testing-e2e -Dtest=SauceDemoLoginTest
```

Run headless:
```bash
mvn test -pl app-testing-e2e -Dtest=SauceDemoLoginTest -Dbrowser.headless=true
```

---

## Testing the SOAP Layer Manually

### Using `curl`

```bash
curl -s -X POST "http://localhost:4566/restapis/XXXXXXXX/local/_user_request_/soap/schedule" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:tns="http://atheor.com/schedule">
  <soap:Body>
    <tns:CreateScheduleRequest>
      <tns:scheduleId>SCH-TEST-001</tns:scheduleId>
      <tns:scheduleName>Test Schedule</tns:scheduleName>
      <tns:startDate>2026-05-01</tns:startDate>
      <tns:endDate>2026-05-31</tns:endDate>
      <tns:operations>
        <tns:operation>
          <tns:operationId>OP-001</tns:operationId>
          <tns:operationName>Extract</tns:operationName>
          <tns:operationType>EXTRACT</tns:operationType>
        </tns:operation>
      </tns:operations>
    </tns:CreateScheduleRequest>
  </soap:Body>
</soap:Envelope>'
```

### Using Postman

1. Create a new **POST** request
2. URL: `<ENDPOINT>/soap/schedule`
3. Headers:
   - `Content-Type`: `text/xml; charset=utf-8`
   - `SOAPAction`: `http://atheor.com/schedule/createSchedule`
4. Body → **raw** → **XML**
5. Paste the SOAP envelope from `app-testing-terraform/scripts/sample-request.xml`

### Using SoapUI

1. New SOAP project → WSDL URL → point to `app-testing-wsdl/src/main/resources/wsdl/schedule-service.wsdl`
2. SoapUI auto-generates a sample request
3. Set the endpoint override to `<ENDPOINT>/soap/schedule`
4. Click ▶ Send

---

## Testing the SQS → PostgreSQL → SNS Flow

After a successful SOAP request, verify the full asynchronous pipeline:

### 1. Confirm the SQS message was consumed

```bash
alias awslocal='aws --endpoint-url=http://localhost:4566 --profile localstack'

# DLQ should be empty (messages processed successfully)
awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/schedule-events-dlq \
  --attribute-names ApproximateNumberOfMessages
```

### 2. Confirm the record in PostgreSQL

```bash
PGPASSWORD=atheor123 psql -h localhost -U atheor -d atheor \
  -c "SELECT schedule_id, status, correlation_id FROM schedules ORDER BY created_at DESC LIMIT 5;"
```

Expected `status = PUBLISHED`.

### 3. Confirm SNS received the message

```bash
# Subscribe a local SQS queue to the SNS topic, then check the queue
awslocal sqs create-queue --queue-name sns-test-sink

SNS_ARN=$(awslocal sns list-topics --query 'Topics[0].TopicArn' --output text)
SINK_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/sns-test-sink \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

awslocal sns subscribe \
  --topic-arn "${SNS_ARN}" \
  --protocol sqs \
  --notification-endpoint "${SINK_ARN}"

# Re-send the SOAP request, then check the sink queue
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/sns-test-sink \
  --max-number-of-messages 5
```

---

## Testing Error / Fault Scenarios

### Malformed SOAP envelope

```bash
curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d '<not-a-soap-envelope/>'
```

Expected: HTTP 400 + SOAP fault response with `soap:Client` fault code.

### Empty body

```bash
curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule"
```

Expected: HTTP 400 + SOAP fault `"Request body is empty"`.

### Missing required fields

Send a request without `scheduleId`:
```xml
<tns:CreateScheduleRequest>
  <tns:scheduleName>No ID</tns:scheduleName>
  <tns:startDate>2026-05-01</tns:startDate>
  <tns:endDate>2026-05-31</tns:endDate>
</tns:CreateScheduleRequest>
```

Expected: HTTP 500 + SOAP fault — JAXB unmarshalling will set `scheduleId` to `null`; downstream validation should produce a meaningful fault.

### Simulate Processor Lambda failure (DLQ test)

1. Temporarily stop PostgreSQL: `docker compose stop postgres`
2. Send a valid SOAP request
3. SQS will retry the Processor Lambda 3 times (watch Lambda logs)
4. After 3 failures the message routes to the DLQ
5. Restart Postgres: `docker compose start postgres`
6. Check DLQ: `awslocal sqs receive-message --queue-url .../schedule-events-dlq`

---

## Test Reports

Surefire XML reports are generated in:
```
<module>/target/surefire-reports/
```

To generate an HTML summary:
```bash
mvn surefire-report:report -DshowSuccess=true
# Open target/site/surefire-report.html
```

Or use the Allure plugin if added to the project:
```bash
mvn allure:serve
```
