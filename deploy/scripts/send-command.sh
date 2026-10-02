#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! "$1" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "usage: send-command.sh IMAGE_SHA" >&2
  exit 2
fi

: "${AWS_REGION:?AWS_REGION is required}"
: "${ECR_REGISTRY:?ECR_REGISTRY is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${EC2_INSTANCE_ID:?EC2_INSTANCE_ID is required}"
: "${CLASSFIT_DOMAIN:?CLASSFIT_DOMAIN is required}"

account_id=${ECR_REGISTRY%%.*}
if [[ ! "$account_id" =~ ^[0-9]{12}$ || "$ECR_REGISTRY" != "${account_id}.dkr.ecr.${AWS_REGION}.amazonaws.com" ]]; then
  echo "invalid ECR registry" >&2
  exit 2
fi

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd "$script_dir/../.." && pwd)
deploy_bundle=$(tar -C "$repo_root" -czf - deploy | base64 | tr -d '\n')

parameters=$(jq -cn \
  --arg bundle "$deploy_bundle" \
  --arg sha "$1" \
  --arg account "$account_id" \
  --arg region "$AWS_REGION" \
  --arg repository "$ECR_REPOSITORY" \
  --arg domain "$CLASSFIT_DOMAIN" \
  '{commands:[
    "set -euo pipefail",
    "mkdir -p /opt/classfit/runtime",
    ("printf %s " + ($bundle | @sh) + " | base64 -d | tar -xzf - -C /opt/classfit --strip-components=1"),
    "chmod 0755 /opt/classfit/scripts/deploy.sh /opt/classfit/scripts/render-env.sh",
    ("env AWS_ACCOUNT_ID=" + ($account | @sh) + " AWS_REGION=" + ($region | @sh) +
      " ECR_REPOSITORY=" + ($repository | @sh) + " CLASSFIT_DOMAIN=" + ($domain | @sh) +
      " /opt/classfit/scripts/deploy.sh " + ($sha | @sh))
  ]}')

aws ssm send-command \
  --region "$AWS_REGION" \
  --instance-ids "$EC2_INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "ClassFit $1" \
  --parameters "$parameters" \
  --query 'Command.CommandId' \
  --output text
