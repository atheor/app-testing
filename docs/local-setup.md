# Running Locally

The local environment uses **LocalStack** to emulate AWS services (API Gateway, Lambda, SQS, SNS, IAM) and **PostgreSQL** running in Docker.

---

## Prerequisites

Ensure all tools are installed (see [Developer Guide](developer-guide.md#prerequisites)).

Verify:
```bash
java -version          # must be 21
mvn -version           # must be ≥ 3.9
docker info            # Docker daemon must be running
terraform -version     # must be ≥ 1.6
```

---

## Quick Start (One Command)

From the project root:
```bash
./deploy-local.sh
```

This script runs four steps in sequence:
1. Starts LocalStack + PostgreSQL via Docker Compose
2. Builds both Lambda fat JARs with Maven
3. Runs `terraform init`
4. Runs `terraform apply -auto-approve`

At the end it prints the API Gateway invoke URL.

---

## Step-by-Step Manual Setup

If you prefer more control over each step:

### Step 1 — Start infrastructure containers

```bash
cd app-testing-terraform
docker compose up -d --wait
```

Verify both are healthy:
```bash
docker compose ps
# localstack   Up (healthy)
# postgres     Up (healthy)
```

Check LocalStack services:
```bash
curl -s http://localhost:4566/_localstack/health | python3 -m json.tool
```

### Step 2 — Build Lambda JARs

```bash
# From project root
mvn package -pl app-testing-inbound,app-testing-lambda -am -DskipTests --no-transfer-progress
```

Output JARs:
- `app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar`
- `app-testing-lambda/target/app-testing-lambda-1.0.0-SNAPSHOT.jar`

### Step 3 — Apply Terraform

```bash
cd app-testing-terraform
terraform init      # only needed once or after provider version changes
terraform apply -auto-approve
```

### Step 4 — Get the endpoint

```bash
terraform output
# api_gateway_endpoint = "http://localhost:4566/restapis/XXXXXXXX/local/_user_request_/soap/schedule"
```

---

## Sending a Test SOAP Request

```bash
ENDPOINT=$(cd app-testing-terraform && terraform output -raw api_gateway_endpoint)

curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d @app-testing-terraform/scripts/sample-request.xml
```

Expected SOAP response:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <CreateScheduleResponse xmlns="http://atheor.com/schedule">
      <scheduleId>SCH-2026-001</scheduleId>
      <status>RECEIVED</status>
      <correlationId>550e8400-e29b-41d4-a716-446655440000</correlationId>
      <message>Schedule created successfully</message>
    </CreateScheduleResponse>
  </soap:Body>
</soap:Envelope>
```

---

## Inspecting Resources with the AWS CLI

Configure a local profile once:
```bash
aws configure --profile localstack
# AWS Access Key ID: test
# AWS Secret Access Key: test
# Default region: us-east-1
# Default output format: json
```

Add this alias for convenience:
```bash
alias awslocal='aws --endpoint-url=http://localhost:4566 --profile localstack'
```

Useful commands:
```bash
# List SQS queues
awslocal sqs list-queues

# Inspect messages on the DLQ (after a failed processing)
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/schedule-events-dlq \
  --max-number-of-messages 10

# List Lambda functions
awslocal lambda list-functions

# Invoke Inbound Lambda directly (bypass API Gateway)
awslocal lambda invoke \
  --function-name local-inbound-lambda \
  --payload file://app-testing-terraform/scripts/sample-apigw-event.json \
  /tmp/response.json && cat /tmp/response.json

# List SNS topics
awslocal sns list-topics

# List API Gateway APIs
awslocal apigateway get-rest-apis
```

---

## Inspecting the Database

```bash
PGPASSWORD=atheor123 psql -h localhost -p 5432 -U atheor -d atheor
```

Useful queries:
```sql
-- See all schedules and their current status
SELECT schedule_id, schedule_name, status, correlation_id, created_at
FROM schedules
ORDER BY created_at DESC;

-- See all operations for a schedule
SELECT o.*
FROM operations o
JOIN schedules s ON s.schedule_id = o.schedule_id
WHERE s.schedule_id = 'SCH-2026-001';

-- See Flyway migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

## Tearing Down

```bash
cd app-testing-terraform
terraform destroy -auto-approve          # remove LocalStack resources
docker compose down -v                   # stop containers + remove volumes (clears all data)
```

To keep container data between sessions (do not pass `-v`):
```bash
docker compose down
```

---

## Environment Variable Reference (Local)

These are automatically injected by Terraform into each Lambda's configuration. For local development outside Lambda (e.g., running a test main), export them manually:

```bash
# Inbound Lambda
export AWS_REGION=us-east-1
export SQS_QUEUE_URL=http://localhost:4566/000000000000/schedule-events-queue
export SQS_ENDPOINT=http://localhost:4566

# Processor Lambda
export AWS_REGION=us-east-1
export SNS_TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:schedule-events-topic
export SNS_ENDPOINT=http://localhost:4566
export DB_URL=jdbc:postgresql://localhost:5432/atheor
export DB_USERNAME=atheor
export DB_PASSWORD=atheor123
```

---

## Changing Terraform Variables

Edit `app-testing-terraform/terraform.tfvars` for persistent changes, or pass overrides inline:

```bash
terraform apply -auto-approve \
  -var="lambda_memory_mb=1024" \
  -var="lambda_timeout_seconds=60"
```

---

## Troubleshooting

### LocalStack not starting
```bash
docker compose logs localstack
# Check if port 4566 is already in use
lsof -i :4566
```

### PostgreSQL connection refused
```bash
docker compose logs postgres
# Verify it's healthy
docker compose ps postgres
```

### Lambda deployment fails — JAR not found
The Terraform `terraform.tfvars` references relative JAR paths. Always run Terraform from inside `app-testing-terraform/`:
```bash
cd app-testing-terraform && terraform apply
# NOT: terraform -chdir=app-testing-terraform apply  (relative paths break)
```

### SOAP request returns 502
Check Lambda logs in LocalStack:
```bash
awslocal logs describe-log-groups
awslocal logs get-log-events \
  --log-group-name /aws/lambda/local-inbound-lambda \
  --log-stream-name $(awslocal logs describe-log-streams \
      --log-group-name /aws/lambda/local-inbound-lambda \
      --query 'logStreams[-1].logStreamName' --output text)
```

### Spring context fails to start in Processor Lambda
Usually a DB connectivity issue. Verify `DB_URL` uses `host.docker.internal` (not `localhost`) when Lambda runs inside Docker:
```
DB_URL=jdbc:postgresql://host.docker.internal:5432/atheor
```
