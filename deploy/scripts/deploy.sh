#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: deploy.sh NEW_IMAGE_SHA" >&2
  exit 2
fi

new_image_sha=$1
if [[ ! "$new_image_sha" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "invalid image SHA" >&2
  exit 2
fi

: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${CLASSFIT_DOMAIN:?CLASSFIT_DOMAIN is required}"

script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
deploy_dir=$(cd "$script_dir/.." && pwd)
runtime_dir="${DEPLOY_ROOT:-/opt/classfit}/runtime"
compose_file="${COMPOSE_FILE:-$deploy_dir/compose.yaml}"
current_image_file="$runtime_dir/current-image"
health_retries="${HEALTH_RETRIES:-12}"
health_interval_seconds="${HEALTH_INTERVAL_SECONDS:-5}"
proxy_health_retries="${PROXY_HEALTH_RETRIES:-18}"
proxy_health_interval_seconds="${PROXY_HEALTH_INTERVAL_SECONDS:-5}"
app_container_name="${APP_CONTAINER_NAME:-classfit-app-1}"
registry="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

mkdir -p "$runtime_dir"
"$script_dir/render-env.sh"

aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$registry" >/dev/null

previous_image_sha=""
if [[ -f "$current_image_file" ]]; then
  previous_image_sha=$(tr -d '\r\n' < "$current_image_file")
fi

export AWS_ACCOUNT_ID AWS_REGION ECR_REPOSITORY CLASSFIT_DOMAIN
export IMAGE_TAG="$new_image_sha"

compose() {
  docker compose --project-name classfit --env-file "$runtime_dir/app.env" -f "$compose_file" "$@"
}

compose pull app
compose stop app >/dev/null 2>&1 || true
compose up -d --no-deps app

healthy=false
for ((attempt = 1; attempt <= health_retries; attempt++)); do
  status=$(docker inspect --format '{{.State.Health.Status}}' "$app_container_name" 2>/dev/null || true)
  if [[ "$status" == "healthy" ]]; then
    healthy=true
    break
  fi
  sleep "$health_interval_seconds"
done

proxy_healthy=false
if [[ "$healthy" == "true" ]] && compose up -d caddy; then
  for ((attempt = 1; attempt <= proxy_health_retries; attempt++)); do
    health_response=$(curl --fail --silent --show-error --max-time 10 \
      --resolve "api.${CLASSFIT_DOMAIN}:443:127.0.0.1" \
      "https://api.${CLASSFIT_DOMAIN}/actuator/health" 2>/dev/null) || health_response=""
    if [[ "$health_response" == *'"status":"UP"'* ]]; then
      proxy_healthy=true
      break
    fi
    sleep "$proxy_health_interval_seconds"
  done
fi

if [[ "$proxy_healthy" == "true" ]]; then
  temporary_current=$(mktemp "$runtime_dir/current-image.tmp.XXXXXX")
  printf '%s\n' "$new_image_sha" > "$temporary_current"
  chmod 600 "$temporary_current"
  mv "$temporary_current" "$current_image_file"
  echo "deployment healthy: $new_image_sha"
  exit 0
fi

echo "deployment failed health or proxy startup; rolling back" >&2
compose stop app >/dev/null 2>&1 || true
if [[ -n "$previous_image_sha" ]]; then
  export IMAGE_TAG="$previous_image_sha"
  compose up -d --no-deps app
  echo "rollback started: $previous_image_sha" >&2
else
  echo "no previous image is available for rollback" >&2
fi
exit 1
