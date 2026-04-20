# ScheduleService SOAP Platform — Documentation

This documentation covers the end-to-end SOAP platform built on AWS serverless infrastructure.

---

## Contents

| Document | Description |
|----------|-------------|
| [Architecture](architecture.md) | System design, request flow diagram, module breakdown, data model, status lifecycle, technology stack, security |
| [Developer Guide](developer-guide.md) | Prerequisites, project structure, build commands, module responsibilities, code conventions, how to add a new SOAP operation |
| [Local Setup](local-setup.md) | Running with LocalStack and Docker, quick start, step-by-step setup, AWS CLI inspection, database queries, environment variables, troubleshooting |
| [AWS Deployment](aws-deployment.md) | Real AWS deployment steps, IAM requirements, RDS setup, Secrets Manager, Terraform configuration, CI/CD outline, cost estimate |
| [Testing Guide](testing-guide.md) | Test layers, running unit and e2e tests, SOAP testing with curl/Postman/SoapUI, end-to-end pipeline verification, error scenario tests |
| [Debugging Guide](debugging-guide.md) | Lambda logs, direct Lambda invocation, database inspection, common issues and fixes, logging reference |

---

## Platform at a Glance

```
SOAP Client ──▶ API Gateway ──▶ Inbound Lambda ──▶ SQS ──▶ Processor Lambda ──▶ SNS
                                                                    │
                                                                    ▼
                                                               PostgreSQL
```

**Stack:** Java 21 · Spring Boot 3.2 · AWS Lambda · SQS · SNS · PostgreSQL · Flyway · MapStruct · JAXB 4 · Terraform · LocalStack

---

## Quick Reference

### Start local environment
```bash
./deploy-local.sh
```

### Send a SOAP request
```bash
ENDPOINT=$(cd app-testing-terraform && terraform output -raw api_gateway_endpoint)
curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d @app-testing-terraform/scripts/sample-request.xml
```

### Run e2e tests
```bash
mvn test -pl app-testing-e2e -Dadms.endpoint="${ENDPOINT}"
```

### Stop local environment
```bash
cd app-testing-terraform && terraform destroy -auto-approve && docker compose down -v
```
