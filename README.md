# app-testing

Serverless SOAP scheduling platform with a full local simulation and E2E test suite.

The system accepts `CreateSchedule` SOAP requests through an API Gateway endpoint, routes them through two Lambda functions, persists data to PostgreSQL, and publishes notifications via SNS. The local environment runs entirely on [LocalStack](https://localstack.cloud) and Docker.

---

## Architecture

```
SOAP Client
    │  POST /soap/schedule  (text/xml, SOAP 1.1)
    ▼
API Gateway  (REST, AWS_PROXY)
    ▼
Lambda 1 — InboundLambdaHandler      [app-testing-inbound]
    │  1. Parse SOAP → JAXB (XXE-safe)
    │  2. Map to ScheduleEvent (MapStruct)
    │  3. Publish JSON to SQS
    │  4. Return synchronous SOAP response
    ▼
SQS — schedule-events-queue
    │  (DLQ after 3 failed attempts)
    ▼
Lambda 2 — SqsConsumerLambdaHandler  [app-testing-lambda]
    │  1. Deserialise JSON → ScheduleEvent
    │  2. Persist to PostgreSQL (JPA + Flyway)
    │  3. Publish to SNS
    ▼
SNS — schedule-events-topic
    └▶  Subscribers (email, SQS fan-out, HTTP webhooks, …)
```

---

## Modules

| Module | Role |
|---|---|
| `app-testing-common` | Shared models (`ScheduleEvent`, `OperationEvent`, `EventStatus`), `JsonUtil`, `AppConstants` |
| `app-testing-wsdl` | WSDL 1.1 contract, XSD schema, JAXB-annotated request/response classes |
| `app-testing-soap` | XXE-safe SOAP parser, SOAP response/fault builder, shared `JAXBContext` |
| `app-testing-mapping` | MapStruct mappers: SOAP ↔ Event ↔ Entity |
| `app-testing-backend` | JPA entities, Spring Data repositories, Flyway migrations, `BackendService` |
| `app-testing-inbound` | **Lambda 1** fat JAR — API Gateway proxy handler |
| `app-testing-lambda` | **Lambda 2** fat JAR — SQS consumer |
| `app-testing-openapi` | OpenAPI 3.0.3 specification for the HTTP facade |
| `app-testing-terraform` | Terraform IaC + Docker Compose for LocalStack simulation |
| `app-testing-framework` | Test utilities: `ConfigManager`, `SoapClient`, `XmlValueExtractor`, `DriverFactory` |
| `app-testing-e2e` | End-to-end tests — SOAP integration tests and Selenium UI tests |

---

## Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 21 |
| Maven | ≥ 3.9 |
| Docker Desktop | ≥ 4.x |
| Terraform | ≥ 1.6 |
| AWS CLI | any (optional) |

Install on macOS:
```bash
brew install --cask temurin@21
brew install maven terraform awscli
```

---

## Quick Start

From the project root, one script does everything:

```bash
./deploy-local.sh
```

This runs four steps:
1. Starts **LocalStack** (AWS simulation) and **PostgreSQL** via Docker Compose
2. Builds both Lambda fat JARs with Maven
3. Runs `terraform init`
4. Runs `terraform apply -auto-approve` and prints the endpoint URL

### Manual step-by-step

```bash
# 1. Start containers
cd app-testing-terraform
docker compose up -d --wait

# 2. Build Lambda JARs (from project root)
cd ..
mvn package -pl app-testing-inbound,app-testing-lambda -am -DskipTests

# 3. Deploy infrastructure
cd app-testing-terraform
terraform init
terraform apply -auto-approve

# 4. Get the endpoint
export ENDPOINT=$(terraform output -raw api_gateway_endpoint)
```

---

## Test the Endpoint

Send a sample SOAP request:

```bash
export ENDPOINT=$(cd app-testing-terraform && terraform output -raw api_gateway_endpoint)

curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=UTF-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:tns="http://atheor.com/schedule">
  <soap:Body>
    <tns:CreateScheduleRequest>
      <tns:scheduleId>SCH-2026-001</tns:scheduleId>
      <tns:scheduleName>Daily Batch Run</tns:scheduleName>
      <tns:startDate>2026-05-01</tns:startDate>
      <tns:endDate>2026-05-31</tns:endDate>
    </tns:CreateScheduleRequest>
  </soap:Body>
</soap:Envelope>'
```

Expected response:
```xml
<CreateScheduleResponse xmlns="http://atheor.com/schedule">
  <scheduleId>SCH-2026-001</scheduleId>
  <status>CREATED</status>
  <message>Schedule received and queued for processing</message>
  <correlationId>...</correlationId>
</CreateScheduleResponse>
```

---

## Running Tests

### E2E SOAP test (requires running environment)

```bash
export ENDPOINT=$(cd app-testing-terraform && terraform output -raw api_gateway_endpoint)

mvn test -pl app-testing-e2e \
  -Dtest=InboundCreateScheduleTest \
  -Dinbound.endpoint="${ENDPOINT}"
```

The test is **skipped automatically** when `inbound.endpoint` is not set, so CI stays green on machines without a deployed environment.

### Selenium UI test

```bash
mvn test -pl app-testing-e2e -Dtest=SauceDemoLoginTest

# Headless:
mvn test -pl app-testing-e2e -Dtest=SauceDemoLoginTest -Dbrowser.headless=true
```

### All tests

```bash
mvn verify
```

### Skip tests during build

```bash
mvn package -DskipTests
```

See [docs/testing-guide.md](docs/testing-guide.md) for the full testing reference.

---

## Project Structure

```
app-testing/
├── pom.xml                          ← parent POM (Java 21, dependency management)
├── deploy-local.sh                  ← one-shot local deployment script
├── app-testing-common/
├── app-testing-wsdl/
├── app-testing-soap/
├── app-testing-mapping/
├── app-testing-backend/
├── app-testing-inbound/             ← Lambda 1 fat JAR
├── app-testing-lambda/              ← Lambda 2 fat JAR
├── app-testing-openapi/
├── app-testing-terraform/           ← Terraform IaC
│   ├── docker-compose.yml           ← LocalStack + PostgreSQL
│   ├── main.tf / variables.tf / outputs.tf / terraform.tfvars
│   └── modules/
│       ├── api-gateway/
│       ├── lambda/
│       ├── sqs/
│       └── sns/
├── app-testing-framework/           ← shared test utilities
├── app-testing-e2e/                 ← end-to-end tests
└── docs/                            ← extended documentation
```

---

## Documentation

| Document | Description |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Detailed component and flow diagrams |
| [docs/local-setup.md](docs/local-setup.md) | Step-by-step local environment setup |
| [docs/testing-guide.md](docs/testing-guide.md) | Running tests, manual curl/Postman/SoapUI testing |
| [docs/developer-guide.md](docs/developer-guide.md) | Build commands, project conventions |
| [docs/aws-deployment.md](docs/aws-deployment.md) | Deploying to real AWS |
| [docs/debugging-guide.md](docs/debugging-guide.md) | Lambda logs, DLQ inspection, common failures |
| [app-testing-terraform/README.md](app-testing-terraform/README.md) | Terraform module reference |
