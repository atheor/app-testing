# Developer Guide

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java JDK | 21 | `brew install --cask temurin@21` or [Adoptium](https://adoptium.net) |
| Maven | ≥ 3.9 | `brew install maven` |
| Docker Desktop | ≥ 4.x | [docker.com](https://www.docker.com/products/docker-desktop/) |
| Terraform | ≥ 1.6 | `brew install terraform` |
| AWS CLI | any | `brew install awscli` (optional, for manual verification) |
| psql | any | `brew install libpq` (optional, for DB inspection) |

---

## Project Structure

```
app-testing/                         ← project root
├── pom.xml                          ← parent POM (aggregator)
├── deploy-local.sh                  ← one-shot local deployment script
├── app-testing-common/              ← shared models & utilities
├── app-testing-wsdl/                ← WSDL + XSD + JAXB classes
├── app-testing-soap/                ← SOAP parser & response builder
├── app-testing-mapping/             ← MapStruct mappers
├── app-testing-backend/             ← JPA entities, repos, Flyway, BackendService
├── app-testing-inbound/             ← Lambda 1: API Gateway → SQS
├── app-testing-lambda/              ← Lambda 2: SQS → PostgreSQL → SNS
├── app-testing-openapi/             ← OpenAPI 3.0.3 spec
├── app-testing-terraform/           ← Terraform IaC + Docker Compose
│   ├── docker-compose.yml
│   ├── main.tf / variables.tf / outputs.tf / terraform.tfvars
│   └── modules/
│       ├── sqs/
│       ├── sns/
│       ├── lambda/
│       └── api-gateway/
├── app-testing-e2e/                 ← end-to-end tests
├── app-testing-framework/           ← test framework utilities
└── docs/                            ← this documentation
```

---

## Building the Project

### Full build (all modules, skip tests)
```bash
mvn package -DskipTests
```

### Build only the Lambda JARs
```bash
mvn package -pl app-testing-inbound,app-testing-lambda -am -DskipTests
```

### Build with tests
```bash
mvn verify
```

### Build a single module
```bash
mvn package -pl app-testing-backend -am -DskipTests
```

> `-am` also builds all upstream modules that the target depends on.

---

## Module Responsibilities

### `app-testing-common`
Zero-dependency library. Contains:
- `ScheduleEvent` / `OperationEvent` — the canonical in-flight event model
- `EventStatus` enum: `RECEIVED → PROCESSING → PROCESSED → PUBLISHED | FAILED`
- `JsonUtil` — static `ObjectMapper` singleton with `JavaTimeModule` registered
- `AppConstants` — centralised names for all environment variable keys, namespaces, headers

**Key rule:** No other platform module may define its own `ObjectMapper`. Always use `JsonUtil.toJson()` / `JsonUtil.fromJson()`.

---

### `app-testing-wsdl`
Defines the SOAP contract:
- `schedule-service.wsdl` — WSDL 1.1 document/literal binding, operation `createSchedule`
- `schedule-service.xsd` — types: `CreateScheduleRequestType`, `OperationType`, `OperationListType`, `CreateScheduleResponseType`
- JAXB-annotated Java classes (hand-written, no code generation plugin):
  - `CreateScheduleRequest` / `CreateScheduleResponse` — root elements
  - `Operation` / `OperationList` — complex types
  - `package-info.java` — `@XmlSchema(namespace="http://atheor.com/schedule", elementFormDefault=QUALIFIED)`
  - `ObjectFactory` — `@XmlRegistry` factory methods

**Adding a new SOAP operation:**
1. Add the new type to `schedule-service.xsd`
2. Add the operation to `schedule-service.wsdl`
3. Create corresponding JAXB classes in `app-testing-wsdl`
4. Add a parser method to `SoapMessageParser`
5. Add a mapper in `SoapToEventMapper`

---

### `app-testing-soap`
Shared parsing and response-building library. All XML work lives here.
- `SoapMessageParser.parseCreateScheduleRequest(String soapXml)` — DOM-parses the envelope, locates `soap:Body`, unmarshals the body child via JAXB
- `SoapResponseBuilder.buildCreateScheduleResponse(...)` — wraps a `CreateScheduleResponse` in a SOAP envelope and marshals to String
- `SoapResponseBuilder.buildFaultResponse(statusCode, faultCode, message)` — returns a SOAP fault; sanitises `message` to prevent XML injection
- `JaxbConfig` — singleton `JAXBContext` shared by both parser and builder

**XXE protection** (CWE-611) — `SoapMessageParser` sets these `DocumentBuilderFactory` features before any parsing:
```java
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
```

---

### `app-testing-mapping`
MapStruct code-generated mappers — zero runtime reflection.

| Mapper | From → To | Notable mappings |
|--------|-----------|-----------------|
| `SoapToEventMapper` | `CreateScheduleRequest` → `ScheduleEvent` | Generates `correlationId = UUID.randomUUID()`, sets `status = RECEIVED`, `createdAt = Instant.now()` |
| `EventToEntityMapper` | `ScheduleEvent` → `ScheduleEntity` | Ignores `id`, `createdAt`, `updatedAt` (DB-generated) |
| `EventToEntityMapper` | `OperationEvent` → `OperationEntity` | Ignores `id`, `createdAt`, bidirectional `schedule` ref |

**Annotation processor order** matters — MapStruct must run before Lombok:
```xml
<annotationProcessorPaths>
    <path>org.mapstruct:mapstruct-processor</path>
    <path>org.projectlombok:lombok</path>
    <path>org.projectlombok:lombok-mapstruct-binding</path>
</annotationProcessorPaths>
```

---

### `app-testing-backend`
Spring Data JPA + Flyway persistence layer. Used **exclusively** by `app-testing-lambda`.

- `ScheduleEntity` — `@Entity`, UUID PK, `schedule_id UNIQUE`, `@OneToMany(cascade=ALL)` to operations
- `OperationEntity` — `@Entity`, UUID PK, `@ManyToOne` FK to schedule
- `ScheduleRepository` — `findByScheduleId`, `existsByScheduleId`
- `OperationRepository` — `findBySchedule_ScheduleId`
- `BackendService` — transactional `saveSchedule`, `findByScheduleId`, `updateStatus`
- Flyway migration `V1__create_schedule_schema.sql` — creates both tables, indexes, `updated_at` trigger

**Connection pool** (HikariCP): pool size capped at 3 to be Lambda-friendly.

---

### `app-testing-inbound`
Lambda 1 — stateless, no Spring context (minimises cold start).

Entry point: `InboundLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`

Static instances (reused across warm invocations):
```java
private static final SoapMessageParser   SOAP_PARSER   = new SoapMessageParser();
private static final SoapResponseBuilder SOAP_BUILDER  = new SoapResponseBuilder();
private static final SqsEventPublisher   SQS_PUBLISHER = new SqsEventPublisher(AwsClientConfig.sqsClient());
```

`AwsClientConfig.sqsClient()` — checks for `SQS_ENDPOINT` environment variable; if present, overrides the endpoint URI (used for LocalStack).

`SqsEventPublisher` — sends a JSON `ScheduleEvent` message with a `X-Correlation-Id` message attribute.

Packaged as fat JAR by Maven Shade Plugin (excludes signature files to avoid `SecurityException`).

---

### `app-testing-lambda`
Lambda 2 — uses a Spring Boot context (`WebApplicationType.NONE`) for dependency injection and JPA.

Entry point: `SqsConsumerLambdaHandler implements RequestHandler<SQSEvent, String>`

**Cold start optimisation:**
- `spring.main.lazy-initialization=true` — beans loaded on first use
- `WebApplicationType.NONE` — no servlet container started
- Double-checked locking for `ApplicationContext` initialisation (initialised once per container lifetime)

Processing flow per SQS message (`EventProcessorService.process()`):
1. `JsonUtil.fromJson(body, ScheduleEvent.class)`
2. `event.setStatus(PROCESSING)`
3. `EventToEntityMapper → ScheduleEntity + OperationEntities`
4. `BackendService.saveSchedule(schedule)`
5. `SnsPublisherService.publish(event)`
6. `BackendService.updateStatus(scheduleId, PUBLISHED)`

If any step throws, the exception propagates to the Lambda handler which re-throws it, causing SQS to retry up to `maxReceiveCount=3` times before routing to the DLQ.

---

### `app-testing-openapi`
Specification-only module — contains `src/main/resources/openapi/schedule-service.yaml`.

View it with any OpenAPI tool:
```bash
# Swagger UI via Docker
docker run -p 8080:8080 \
  -e SWAGGER_JSON=/spec/schedule-service.yaml \
  -v $(pwd)/app-testing-openapi/src/main/resources/openapi:/spec \
  swaggerapi/swagger-ui
# Then open http://localhost:8080
```

---

## Code Conventions

- **Package root:** `com.atheor`
- **SOAP namespace:** `http://atheor.com/schedule`
- **Environment variable keys:** always reference `AppConstants.ENV_*` constants — never hardcode strings
- **JSON:** always use `JsonUtil.toJson()` / `JsonUtil.fromJson()` — never instantiate `ObjectMapper` directly
- **Logging:** use SLF4J `@Slf4j` — always include `scheduleId` and/or `correlationId` in log messages for traceability
- **Transactions:** `@Transactional(readOnly = true)` on read methods in `BackendService`
- **Mapper singletons:** access MapStruct mappers via `.INSTANCE` — never `Mappers.getMapper()` at call sites

---

## Adding a New SOAP Operation

1. Add type definitions to `app-testing-wsdl/src/main/resources/schedule-service.xsd`
2. Add `<wsdl:operation>` to `schedule-service.wsdl`
3. Create JAXB request/response classes under `com.atheor.wsdl`
4. Add a `parseXxx(String soapXml)` method to `SoapMessageParser`
5. Add a `buildXxxResponse(...)` method to `SoapResponseBuilder`
6. Add a mapper in `SoapToEventMapper`
7. Add handler logic in `InboundLambdaHandler`
8. Add e2e test in `app-testing-e2e`
