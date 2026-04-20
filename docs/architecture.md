# Architecture

## Overview

The **ScheduleService SOAP Platform** is a serverless, event-driven application hosted on AWS. It exposes a SOAP 1.1 HTTP endpoint via API Gateway and processes schedule creation requests asynchronously through Lambda functions, SQS, and SNS before persisting data to PostgreSQL.

---

## End-to-End Request Flow

```
┌─────────────┐   SOAP 1.1 (text/xml)   ┌─────────────────────┐
│ SOAP Client │ ──── POST /soap/schedule ──▶ API Gateway (REST) │
└─────────────┘                          └──────────┬──────────┘
                                                     │ AWS_PROXY
                                                     ▼
                                         ┌─────────────────────────┐
                                         │  Inbound Lambda          │
                                         │  (app-testing-inbound)   │
                                         │                          │
                                         │  1. Parse SOAP → JAXB    │
                                         │  2. Map to ScheduleEvent │
                                         │  3. Publish JSON to SQS  │
                                         │  4. Return SOAP response │
                                         └───────────┬─────────────┘
                                                     │ JSON message
                                                     ▼
                                         ┌─────────────────────────┐
                                         │  SQS Queue               │
                                         │  schedule-events-queue   │
                                         │  (DLQ after 3 failures)  │
                                         └───────────┬─────────────┘
                                                     │ batch=5, window=10s
                                                     ▼
                                         ┌─────────────────────────┐
                                         │  Processor Lambda        │
                                         │  (app-testing-lambda)    │
                                         │                          │
                                         │  1. Deserialise JSON     │
                                         │  2. Persist to Postgres  │
                                         │  3. Publish to SNS       │
                                         │  4. Update status        │
                                         └──────┬──────────────────┘
                                                │
                              ┌─────────────────┴───────────────────┐
                              │                                       │
                              ▼                                       ▼
               ┌───────────────────────┐         ┌───────────────────────────┐
               │  PostgreSQL Database  │         │  SNS Topic                │
               │  (schedules +         │         │  schedule-events-topic    │
               │   operations tables)  │         │  → subscribers (email,    │
               └───────────────────────┘         │    SQS fan-out, HTTP...)  │
                                                  └───────────────────────────┘
```

---

## Module Breakdown

| Module | Artifact | Role |
|--------|----------|------|
| `app-testing-common` | Library JAR | Shared models (`ScheduleEvent`, `OperationEvent`, `EventStatus`), `JsonUtil`, `AppConstants` |
| `app-testing-wsdl` | Library JAR | WSDL 1.1 contract, XSD schema, JAXB-annotated request/response classes |
| `app-testing-soap` | Library JAR | XXE-safe SOAP parser, SOAP response/fault builder, shared `JAXBContext` |
| `app-testing-mapping` | Library JAR | MapStruct mappers: SOAP↔Event↔Entity |
| `app-testing-backend` | Library JAR | JPA entities (`ScheduleEntity`, `OperationEntity`), Spring Data repositories, Flyway migrations, `BackendService` |
| `app-testing-inbound` | Fat JAR (Lambda) | API Gateway proxy handler — parses SOAP, publishes to SQS |
| `app-testing-lambda` | Fat JAR (Lambda) | SQS consumer — persists to PostgreSQL, publishes to SNS |
| `app-testing-openapi` | Resources only | OpenAPI 3.0.3 specification for the HTTP facade |
| `app-testing-terraform` | Terraform IaC | LocalStack simulation + AWS infrastructure (SQS, SNS, Lambda, API Gateway) |

---

## Module Dependency Graph

```
app-testing-common
    ├─▶ app-testing-wsdl
    │       └─▶ app-testing-soap
    │               └─▶ app-testing-inbound (Lambda 1)
    ├─▶ app-testing-mapping
    │       ├─▶ app-testing-inbound (Lambda 1)
    │       └─▶ app-testing-lambda (Lambda 2)
    └─▶ app-testing-backend
            └─▶ app-testing-lambda (Lambda 2)
```

---

## Data Model

### ScheduleEvent (in-flight, JSON on SQS/SNS)

```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "scheduleId": "SCH-2026-001",
  "scheduleName": "Daily Batch Run",
  "startDate": "2026-05-01",
  "endDate": "2026-05-31",
  "status": "RECEIVED",
  "createdAt": "2026-04-21T10:00:00Z",
  "operations": [
    {
      "operationId": "OP-001",
      "operationName": "Extract Orders",
      "operationType": "EXTRACT",
      "payload": "{\"source\":\"db1\"}"
    }
  ]
}
```

### Database Schema

```sql
-- schedules (one row per CreateSchedule request)
id             UUID    PK   gen_random_uuid()
schedule_id    VARCHAR UNIQUE
schedule_name  VARCHAR
start_date     VARCHAR
end_date       VARCHAR
status         VARCHAR  -- RECEIVED → PROCESSING → PROCESSED → PUBLISHED | FAILED
correlation_id VARCHAR
created_at     TIMESTAMPTZ
updated_at     TIMESTAMPTZ  -- auto-updated by trigger fn_set_updated_at()

-- operations (many per schedule)
id             UUID    PK
schedule_id    VARCHAR FK → schedules(schedule_id) ON DELETE CASCADE
operation_id   VARCHAR
operation_name VARCHAR
operation_type VARCHAR
payload        TEXT    (nullable)
created_at     TIMESTAMPTZ
```

---

## Status Lifecycle

```
RECEIVED  ──▶  PROCESSING  ──▶  PUBLISHED
   │                │
   └────────────────┴──▶  FAILED
```

| Status | Set by | Meaning |
|--------|--------|---------|
| `RECEIVED` | `SoapToEventMapper` | Event created from SOAP request |
| `PROCESSING` | `EventProcessorService` | DB persist started |
| `PUBLISHED` | `EventProcessorService` | SNS publish succeeded |
| `FAILED` | Error handler | Processing exception — DLQ re-drive |

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| SOAP contract | WSDL 1.1 + XSD | — |
| JAXB runtime | Jakarta JAXB | 4.0.2 / 4.0.5 |
| Object mapping | MapStruct | 1.5.5.Final |
| Persistence | Spring Data JPA + Hibernate | Spring Boot 3.2.5 |
| Database | PostgreSQL | 42.7.3 (driver), 16 (server) |
| Schema migration | Flyway | 10.11.0 |
| Connection pool | HikariCP | 5.1.0 |
| Lambda runtime | AWS Lambda Java | core 1.2.3 / events 3.11.4 |
| AWS SDK | AWS SDK v2 BOM | 2.25.40 |
| Serialisation | Jackson | 2.16.2 |
| Build tool | Maven | ≥ 3.9 |
| Fat JAR | Maven Shade | 3.5.2 |
| Infrastructure | Terraform | ≥ 1.6 |
| Local AWS | LocalStack | 3.4 |

---

## Security Considerations

| Concern | Mitigation |
|---------|-----------|
| **XXE (CWE-611)** | `SoapMessageParser` disables DOCTYPE declarations and all external entity/parameter references via `DocumentBuilderFactory` feature flags |
| **XML Injection** | `SoapResponseBuilder.buildFaultResponse()` sanitises fault messages by stripping `<`, `>`, `&`, `"`, `'` characters before embedding in XML |
| **Credentials** | Database password and AWS credentials never hardcoded — injected via Lambda environment variables; local `terraform.tfvars` must not be committed with real secrets |
| **IAM** | Lambda execution role is least-privilege: `sqs:SendMessage` + `sqs:ReceiveMessage` + `sqs:DeleteMessage` + `sns:Publish` + CloudWatch Logs only |
| **DLQ** | Poison messages are quarantined after 3 failures, preventing infinite retry loops |
| **Content-Type enforcement** | API Gateway request validator rejects any `POST /soap/schedule` missing a `Content-Type` header (HTTP 400 returned as SOAP fault); Lambda additionally validates the value contains `text/xml` and returns HTTP 415 for mismatches |
| **SOAP fault for infra errors** | `DEFAULT_4XX` and `DEFAULT_5XX` API Gateway gateway responses are overridden to return SOAP fault envelopes (`Content-Type: text/xml; charset=utf-8`) instead of the default JSON `{"message":"..."}`, ensuring SOAP clients always receive well-formed XML |
