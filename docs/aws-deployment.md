# Deploying to AWS

This guide covers deploying the ScheduleService SOAP Platform to a real AWS account.

---

## Prerequisites

- AWS account with sufficient IAM permissions (Lambda, API Gateway, SQS, SNS, IAM, CloudWatch Logs)
- AWS CLI configured with a non-`test` profile
- Terraform ≥ 1.6
- S3 bucket for Lambda JAR storage (recommended for production)

---

## AWS IAM Permissions Required

The IAM user or role running Terraform must have at minimum:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": ["lambda:*"], "Resource": "*" },
    { "Effect": "Allow", "Action": ["apigateway:*"], "Resource": "*" },
    { "Effect": "Allow", "Action": ["sqs:*"], "Resource": "*" },
    { "Effect": "Allow", "Action": ["sns:*"], "Resource": "*" },
    { "Effect": "Allow", "Action": ["iam:CreateRole","iam:AttachRolePolicy","iam:PutRolePolicy",
        "iam:GetRole","iam:PassRole","iam:DeleteRole","iam:DeleteRolePolicy"], "Resource": "*" },
    { "Effect": "Allow", "Action": ["logs:*"], "Resource": "*" }
  ]
}
```

---

## Step 1 — Build Lambda JARs

```bash
# From the project root
mvn package -pl app-testing-inbound,app-testing-lambda -am -DskipTests
```

Output artifacts:
- `app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar`
- `app-testing-lambda/target/app-testing-lambda-1.0.0-SNAPSHOT.jar`

> For production, upload JARs to an S3 bucket and reference them via `s3_key` in the Terraform Lambda resource instead of `filename`. This avoids the 50 MB direct upload limit.

---

## Step 2 — Create a Production Terraform Configuration

Create a new file `app-testing-terraform/terraform-aws.tfvars` (do **not** overwrite `terraform.tfvars`):

```hcl
environment         = "prod"
aws_region          = "us-east-1"
localstack_endpoint = ""    # leave empty — not used for real AWS

sqs_queue_name = "schedule-events-queue"
sqs_dlq_name   = "schedule-events-dlq"
sns_topic_name = "schedule-events-topic"

api_name  = "schedule-service-api"
api_stage = "v1"

lambda_runtime         = "java21"
lambda_memory_mb       = 512
lambda_timeout_seconds = 30

inbound_lambda_jar   = "../app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar"
processor_lambda_jar = "../app-testing-lambda/target/app-testing-lambda-1.0.0-SNAPSHOT.jar"

db_url      = "jdbc:postgresql://<RDS_ENDPOINT>:5432/atheor"
db_username = "atheor"
db_password = "<STRONG_PASSWORD>"   # use a secret manager in production (see below)
```

> **Security:** Do not commit `terraform-aws.tfvars` to version control. Add it to `.gitignore`.

---

## Step 3 — Configure Terraform for Real AWS

The current `main.tf` configures the provider for LocalStack. For AWS, create an override file:

```hcl
# app-testing-terraform/provider-aws-override.tf  (do NOT commit with secrets)
provider "aws" {
  region  = var.aws_region
  profile = "your-aws-profile"   # or rely on environment variables / instance role

  # Remove all LocalStack-specific overrides
}
```

Alternatively, comment out the `endpoints {}` block and `skip_*` settings in `main.tf` for the production apply.

---

## Step 4 — Provision the Database (PostgreSQL on RDS)

The Terraform in this project does not provision RDS. Provision it separately:

**AWS Console (recommended for first time):**
1. RDS → Create database → PostgreSQL 16
2. DB identifier: `atheor-schedule-db`
3. DB name: `atheor`, username: `atheor`, password: strong random value
4. VPC: same VPC the Lambda functions will run in
5. Security group: allow inbound `5432` from the Lambda security group

**Or via AWS CLI:**
```bash
aws rds create-db-instance \
  --db-instance-identifier atheor-schedule-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16.3 \
  --master-username atheor \
  --master-user-password '<PASSWORD>' \
  --db-name atheor \
  --allocated-storage 20 \
  --no-publicly-accessible \
  --region us-east-1
```

After the instance is available:
```bash
aws rds describe-db-instances \
  --db-instance-identifier atheor-schedule-db \
  --query 'DBInstances[0].Endpoint.Address'
```

---

## Step 5 — Store Secrets in AWS Secrets Manager (Recommended)

Rather than injecting raw passwords as Lambda environment variables, store them in Secrets Manager:

```bash
aws secretsmanager create-secret \
  --name /atheor/schedule-service/db \
  --secret-string '{"username":"atheor","password":"<STRONG_PASSWORD>","url":"jdbc:postgresql://<RDS_ENDPOINT>:5432/atheor"}'
```

Then update the Lambda functions to retrieve the secret at cold start using the AWS SDK:

```java
// In AwsClientConfig (app-testing-lambda), fetch from Secrets Manager
SecretsManagerClient sm = SecretsManagerClient.builder()
    .region(Region.of(System.getenv("AWS_REGION")))
    .build();
String secret = sm.getSecretValue(r -> r.secretId("/atheor/schedule-service/db"))
    .secretString();
// Parse JSON and set datasource properties
```

Add the `secretsmanager:GetSecretValue` IAM permission to the Lambda execution role.

---

## Step 6 — Apply Terraform

```bash
cd app-testing-terraform

# Initialise (downloads AWS provider)
terraform init

# Preview
terraform plan -var-file=terraform-aws.tfvars

# Apply
terraform apply -var-file=terraform-aws.tfvars
```

Terraform will output:
```
api_gateway_endpoint = "https://xxxxxxxxxx.execute-api.us-east-1.amazonaws.com/v1/soap/schedule"
```

---

## Step 7 — Database Schema Migration

Flyway runs automatically on the first Lambda cold start. To trigger it manually:

```bash
ENDPOINT=$(terraform output -raw api_gateway_endpoint)

curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d @scripts/sample-request.xml
```

Check migration status:
```bash
PGPASSWORD='<PASSWORD>' psql -h <RDS_ENDPOINT> -U atheor -d atheor \
  -c "SELECT version, description, success FROM flyway_schema_history;"
```

---

## Step 8 — Verify End-to-End

```bash
ENDPOINT="https://xxxxxxxxxx.execute-api.us-east-1.amazonaws.com/v1/soap/schedule"

curl -s -X POST "${ENDPOINT}" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: http://atheor.com/schedule/createSchedule" \
  -d @app-testing-terraform/scripts/sample-request.xml
```

Verify in AWS Console:
- **SQS** — `schedule-events-queue` message count briefly rises then drops to 0 (consumed by Processor Lambda)
- **Lambda** → `prod-processor-lambda` → Monitor → Recent invocations (should show success)
- **RDS** — connect and run `SELECT * FROM schedules ORDER BY created_at DESC LIMIT 5;`
- **SNS** — `schedule-events-topic` → subscriptions receive the notification

---

## Updating Lambda Code

After a code change:

```bash
# Rebuild
mvn package -pl app-testing-inbound,app-testing-lambda -am -DskipTests

# Re-deploy (Terraform detects JAR hash change and updates the function)
cd app-testing-terraform
terraform apply -var-file=terraform-aws.tfvars -auto-approve
```

Or deploy directly without Terraform:
```bash
aws lambda update-function-code \
  --function-name prod-inbound-lambda \
  --zip-file fileb://app-testing-inbound/target/app-testing-inbound-1.0.0-SNAPSHOT.jar

aws lambda update-function-code \
  --function-name prod-processor-lambda \
  --zip-file fileb://app-testing-lambda/target/app-testing-lambda-1.0.0-SNAPSHOT.jar
```

---

## CI/CD Pipeline Outline

```
┌──────────┐    push     ┌───────────────────────────────┐
│  GitHub  │ ──────────▶ │ GitHub Actions / Jenkins       │
└──────────┘             │                               │
                         │  1. mvn verify                │
                         │  2. mvn package -DskipTests   │
                         │  3. terraform plan            │
                         │  4. terraform apply           │
                         └───────────────────────────────┘
```

A minimal GitHub Actions workflow file would:
1. Use `actions/setup-java@v4` with `java-version: '21'`
2. Run `mvn verify` (build + tests)
3. Run `mvn package -DskipTests` for the Lambda JARs
4. Use `hashicorp/setup-terraform@v3`
5. Run `terraform apply` with secrets injected from GitHub Secrets

---

## Teardown

```bash
cd app-testing-terraform
terraform destroy -var-file=terraform-aws.tfvars
```

> RDS is not managed by this Terraform configuration — delete it separately via the AWS Console or CLI to avoid ongoing costs.

---

## Cost Estimate (us-east-1, light usage)

| Service | Notes | Approximate monthly cost |
|---------|-------|--------------------------|
| Lambda | 1M requests, 512 MB, 1s avg | ~$0.20 |
| API Gateway | 1M REST API calls | ~$3.50 |
| SQS | 1M messages | ~$0.40 |
| SNS | 1M notifications | ~$0.50 |
| RDS PostgreSQL (db.t3.micro) | 730 hrs/month | ~$13.00 |
| CloudWatch Logs | 1 GB ingested | ~$0.50 |
| **Total** | | **~$18/month** |

Costs scale linearly with request volume; Lambda and SQS are essentially free at low volumes.
