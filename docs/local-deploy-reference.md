# Local Deploy Reference

---

## What `deploy-local.sh` Creates

The script runs four steps in sequence. Here is a full breakdown of every container, file, and AWS resource it produces.

---

### Step 1 — Docker Containers

Starts 2 containers via `app-testing-terraform/docker-compose.yml` and waits until both are healthy:

| Container | Image | Port | Purpose |
|---|---|---|---|
| `localstack` | `localstack/localstack:3.4` | `4566` | Emulates AWS services (API Gateway, Lambda, SQS, SNS, IAM, STS, CloudWatch) |
| `postgres` | `postgres:16-alpine` | `5432` | Persistent backend DB (`atheor` database, user `atheor`) |

LocalStack mounts `/var/run/docker.sock` so it can spin up Lambda execution containers on demand.

**Docker volumes created:**

| Volume | Mounted at | Contains |
|---|---|---|
| `app-testing-terraform_localstack-data` | `/var/lib/localstack` | LocalStack runtime state |
| `app-testing-terraform_postgres-data` | `/var/lib/postgresql/data` | PostgreSQL database files |

---

### Step 2 — Maven Build (JAR files)

Builds only the two Lambda modules (plus their transitive dependencies via `-am`):

| Module | Output JAR | Purpose |
|---|---|---|
| `app-testing-inbound` | `app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar` | Handles incoming SOAP HTTP requests from API Gateway and pushes events to SQS |
| `app-testing-lambda` | `app-testing-lambda/target/app-testing-lambda-1.0.0-SNAPSHOT.jar` | Consumes SQS messages, writes to PostgreSQL, publishes to SNS |

---

### Steps 3 & 4 — Terraform (AWS resources inside LocalStack)

`terraform init` downloads the AWS provider, then `terraform apply` creates the following resources inside LocalStack:

**IAM**
- Role `local-lambda-exec-role` with an inline policy granting:
  - SQS `SendMessage`, `ReceiveMessage`, `DeleteMessage`, `GetQueueAttributes`
  - SNS `Publish`
  - CloudWatch Logs `CreateLogGroup`, `CreateLogStream`, `PutLogEvents`

**SQS**
- Main queue: `local-schedule-events` — receives inbound SOAP events, 1-day retention, max 3 retries before moving to DLQ
- DLQ: `local-schedule-events-dlq` — catches failed messages, 14-day retention

**SNS**
- Topic: `local-schedule-events` — receives notifications after a schedule is successfully processed

**Lambda**
- `local-inbound-lambda` — handler `InboundLambdaHandler::handleRequest`; env vars: `SQS_QUEUE_URL`, `SQS_ENDPOINT=http://host.docker.internal:4566`
- `local-processor-lambda` — handler `SqsConsumerLambdaHandler::handleRequest`; env vars: `SNS_TOPIC_ARN`, `SNS_ENDPOINT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

**API Gateway**
- REST API with route `POST /soap/schedule`
- Request validator enforcing `Content-Type: text/xml` header
- AWS Lambda Proxy integration → `local-inbound-lambda`
- Deployed to stage `v1`

---

### End State — Request Flow

```
curl POST /soap/schedule
    → API Gateway (LocalStack :4566)
        → local-inbound-lambda (JAR)
            → SQS local-schedule-events
                → local-processor-lambda (JAR)
                    → PostgreSQL (postgres:5432)
                    → SNS local-schedule-events
```

The script prints `api_gateway_endpoint` at the end — that is the URL used for SOAP calls.

---

## Removing or Resetting the Local Deployment

### Full teardown (destroy everything)

```bash
# 1. Destroy all Terraform-managed AWS resources inside LocalStack
cd app-testing-terraform
terraform destroy -auto-approve

# 2. Stop containers AND delete volumes (wipes PostgreSQL data + LocalStack state)
docker compose down -v
```

---

### Partial reset options

**Keep containers running, just re-apply Terraform** (e.g. after changing `.tf` files):
```bash
cd app-testing-terraform
terraform destroy -auto-approve
terraform apply -auto-approve
```

**Stop containers but keep data** (volumes are preserved; containers restart cleanly next time):
```bash
cd app-testing-terraform
docker compose stop
```

**Wipe only the database** (keep LocalStack resources intact):
```bash
cd app-testing-terraform
docker compose stop postgres
docker volume rm app-testing-terraform_postgres-data
docker compose up -d postgres --wait
```

**Wipe only LocalStack state** (re-provision all AWS resources from scratch):
```bash
cd app-testing-terraform
docker compose stop localstack
docker volume rm app-testing-terraform_localstack-data
docker compose up -d localstack --wait
terraform apply -auto-approve
```

---

### Re-deploy from scratch (clean run equivalent to `deploy-local.sh`)

```bash
cd app-testing-terraform
terraform destroy -auto-approve
docker compose down -v
cd ..
./deploy-local.sh
```

---

> **Note on volume names:** Docker volume names are prefixed with the Compose project name (the directory name). Confirm the exact names on your machine with:
> ```bash
> docker volume ls | grep app-testing
> ```
