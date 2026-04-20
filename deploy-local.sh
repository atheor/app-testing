#!/usr/bin/env bash
# =============================================================
# deploy-local.sh — Build JARs and apply Terraform against LocalStack
# =============================================================
# Prerequisites:
#   - Docker + Docker Compose
#   - Java 21, Maven
#   - Terraform >= 1.6
#   - awscli (optional, for manual verification)
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}"
TF_DIR="${SCRIPT_DIR}/app-testing-terraform"

echo "===> [1/4] Starting LocalStack and PostgreSQL via Docker Compose..."
docker compose -f "${TF_DIR}/docker-compose.yml" up -d --wait

echo "===> [2/4] Building Lambda JARs..."
cd "${ROOT_DIR}"
mvn package -pl app-testing-inbound,app-testing-lambda \
    -am \
    -DskipTests \
    --no-transfer-progress

echo "===> [3/4] Initialising Terraform..."
cd "${TF_DIR}"
terraform init -input=false

echo "===> [4/4] Applying Terraform plan..."
terraform apply -auto-approve -input=false

echo ""
echo "======================================================"
echo " Deployment complete!"
echo ""
terraform output
echo "======================================================"
echo ""
echo " Test your SOAP endpoint:"
echo "   ENDPOINT=\$(terraform output -raw api_gateway_endpoint)"
echo "   curl -s -X POST \"\${ENDPOINT}\" \\"
echo "        -H 'Content-Type: text/xml' \\"
echo "        -H 'SOAPAction: http://atheor.com/schedule/createSchedule' \\"
echo "        -d @${TF_DIR}/scripts/sample-request.xml"
echo "======================================================"
