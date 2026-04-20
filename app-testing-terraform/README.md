# app-testing-terraform

Local AWS infrastructure simulation using **LocalStack** and **Terraform**.

## Architecture

```
SOAP Client
    │  POST /soap/schedule
    │  text/xml (SOAP 1.1 envelope)
    ▼
API Gateway (REST)
    │  AWS_PROXY integration
    ▼
Lambda: InboundLambdaHandler          (app-testing-inbound)
    │  1. Parse SOAP → CreateScheduleRequest (JAXB, XXE-safe)
    │  2. Map to ScheduleEvent (MapStruct)
    │  3. Serialize to JSON → SQS message
    ▼
SQS Queue: schedule-events-queue
    │  Event source mapping (batch 5, window 10s)
    ▼
Lambda: SqsConsumerLambdaHandler      (app-testing-lambda)
    │  1. Deserialize JSON → ScheduleEvent
    │  2. Persist to PostgreSQL (Spring Data JPA + Flyway)
    │  3. Publish JSON to SNS
    ▼
SNS Topic: schedule-events-topic
    ▼
(Subscribers — email, SQS fan-out, HTTP, etc.)
```

Dead messages are routed to `schedule-events-dlq` after **3 failed attempts**.

---

## Prerequisites

| Tool             | Version  |
|------------------|----------|
| Docker           | ≥ 24     |
| Docker Compose   | V2       |
| Java             | 21       |
| Maven            | ≥ 3.9    |
| Terraform        | ≥ 1.6    |
| AWS CLI          | any (optional) |

---

## Quick Start — LocalStack

```bash
# 1. Start LocalStack + PostgreSQL
docker compose up -d --wait

# 2. Build Lambda JARs (from project root)
cd ..
mvn package -pl app-testing-inbound,app-testing-lambda -am -DskipTests

# 3. Deploy infrastructure
cd app-testing-terraform
terraform init
terraform apply -auto-approve

# 4. Run a sample SOAP request
ENDPOINT=$(terraform output -raw api_gateway_endpoint)
curl -s -X POST "${ENDPOINT}" \
     -H "Content-Type: text/xml" \
     -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
     -d @scripts/sample-request.xml

# Or use the deploy script (run from the project root) which combines steps 1-4
./deploy-local.sh
```

---

## Module Layout

```
app-testing-terraform/
├── docker-compose.yml          # LocalStack + PostgreSQL
├── main.tf                     # Root module — wires sub-modules together
├── variables.tf                # All input variables with defaults
├── outputs.tf                  # Useful outputs (URLs, ARNs)
├── terraform.tfvars            # Local overrides (not for production)
├── modules/
│   ├── api-gateway/            # REST API → /soap/schedule → Lambda proxy
│   ├── lambda/                 # IAM role, Inbound Lambda, Processor Lambda, SQS ESM
│   ├── sqs/                    # Main queue + DLQ with redrive policy
│   └── sns/                    # Notification topic
└── scripts/
    ├── deploy-local.sh         # One-shot build + deploy script
    ├── init-db.sh              # Wait for PG + verify schema
    └── sample-request.xml      # Example SOAP request for manual testing
```

---

## Environment Variables Injected into Lambdas

### Inbound Lambda (`app-testing-inbound`)
| Variable        | Description                      |
|-----------------|----------------------------------|
| `AWS_REGION`    | AWS region                       |
| `SQS_QUEUE_URL` | URL of the SQS schedule-events queue |
| `SQS_ENDPOINT`  | LocalStack endpoint override     |

### SQS Consumer Lambda (`app-testing-lambda`)
| Variable        | Description                      |
|-----------------|----------------------------------|
| `AWS_REGION`    | AWS region                       |
| `SNS_TOPIC_ARN` | ARN of the SNS notification topic |
| `SNS_ENDPOINT`  | LocalStack endpoint override     |
| `DB_URL`        | JDBC URL for PostgreSQL          |
| `DB_USERNAME`   | PostgreSQL username              |
| `DB_PASSWORD`   | PostgreSQL password (sensitive)  |

---

## Teardown

```bash
terraform destroy -auto-approve
docker compose down -v   # removes volumes (data)
```
