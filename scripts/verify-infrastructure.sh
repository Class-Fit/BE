#!/usr/bin/env bash
set -euo pipefail

infrastructure_template="infra/cloudformation/classfit-infrastructure.yaml"
oidc_template="infra/cloudformation/classfit-github-oidc.yaml"
parameters="infra/parameters/prod.example.json"

for file in "$infrastructure_template" "$oidc_template" "$parameters"; do
  [[ -f "$file" ]] || { echo "missing: $file" >&2; exit 1; }
done

reject() {
  local pattern=$1 file=$2
  if grep -Eq "$pattern" "$file"; then
    echo "forbidden pattern '$pattern' in $file" >&2
    exit 1
  fi
}

require() {
  local pattern=$1 file=$2
  grep -Eq "$pattern" "$file" || { echo "required pattern '$pattern' missing from $file" >&2; exit 1; }
}

reject 'AWS::EC2::NatGateway|AWS::ElasticLoadBalancing|FromPort: 22|CidrIp:.*0\.0\.0\.0/0.*22' "$infrastructure_template"
require 'PubliclyAccessible: false' "$infrastructure_template"
require 'SourceSecurityGroupId:.*!Ref Ec2SecurityGroup|SourceSecurityGroupId:' "$infrastructure_template"
require 'LifecyclePolicy:' "$infrastructure_template"
require 'AmazonSSMManagedInstanceCore' "$infrastructure_template"
require 'Threshold: 10' "$infrastructure_template"
require 'Threshold: 30' "$infrastructure_template"
require 'Threshold: 70' "$infrastructure_template"
require 'token.actions.githubusercontent.com' "$oidc_template"
require 'repo:Class-Fit/BE:environment:production' "$oidc_template"
reject 'AKIA[0-9A-Z]{16}|aws_secret_access_key|BEGIN (RSA |EC )?PRIVATE KEY' "$parameters"
require 'REPLACE_IN_CONSOLE' "$parameters"

echo "infrastructure static verification passed"
