#!/usr/bin/env bash
set -euo pipefail

workflow=".github/workflows/deploy.yml"
ci=".github/workflows/ci.yml"
[[ -f "$workflow" ]] || { echo "missing: $workflow" >&2; exit 1; }

require() {
  local pattern=$1 file=${2:-$workflow}
  grep -Eq "$pattern" "$file" || { echo "required pattern '$pattern' missing from $file" >&2; exit 1; }
}

reject() {
  local pattern=$1
  if grep -Eqi "$pattern" "$workflow"; then
    echo "forbidden pattern '$pattern' in $workflow" >&2
    exit 1
  fi
}

require 'workflow_call:' "$ci"
require 'id-token: write'
require 'contents: read'
require 'environment: production'
require 'needs: verify'
require 'github\.sha'
require 'amazon-ecr-login'
require 'deploy/scripts/send-command\.sh'
require 'ssm send-command' 'deploy/scripts/send-command.sh'
require 'get-command-invocation'
reject 'AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|echo.*secret|echo.*token'

echo "deployment workflow verification passed"
