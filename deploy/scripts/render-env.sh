#!/usr/bin/env bash
set -euo pipefail

runtime_dir="${DEPLOY_ROOT:-/opt/classfit}/runtime"
parameter_prefix="${SSM_PARAMETER_PREFIX:-/classfit/prod}"
aws_region="${AWS_REGION:?AWS_REGION is required}"
destination="$runtime_dir/app.env"

required_parameters=(
  DB_URL
  DB_USER
  DB_PASSWORD
  KAKAO_CLIENT_ID
  KAKAO_CLIENT_SECRET
  OPENAI_API_KEY
  CLASSFIT_FRONTEND_ORIGIN
)

mkdir -p "$runtime_dir"
umask 077
temporary_file=$(mktemp "$runtime_dir/app.env.tmp.XXXXXX")
trap 'rm -f "$temporary_file"' EXIT

for parameter_name in "${required_parameters[@]}"; do
  parameter_path="$parameter_prefix/$parameter_name"
  if ! parameter_value=$(aws ssm get-parameter \
      --region "$aws_region" \
      --name "$parameter_path" \
      --with-decryption \
      --query 'Parameter.Value' \
      --output text 2>/dev/null); then
    echo "missing required SSM parameter: $parameter_path" >&2
    exit 1
  fi

  if [[ -z "$parameter_value" || "$parameter_value" == "None" || "$parameter_value" == *$'\n'* || "$parameter_value" == *$'\r'* ]]; then
    echo "invalid required SSM parameter: $parameter_path" >&2
    exit 1
  fi

  printf '%s=%s\n' "$parameter_name" "$parameter_value" >> "$temporary_file"
done

printf '%s\n' 'PUBLIC_DATA_SYNC_ENABLED=false' >> "$temporary_file"
chmod 600 "$temporary_file"
mv "$temporary_file" "$destination"
trap - EXIT
