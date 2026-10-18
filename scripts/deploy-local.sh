#!/usr/bin/env bash
# Build (or pull) an image tagged with the git SHA and release it with Helm.
# Usage: scripts/deploy-local.sh [git-sha]   (defaults to HEAD)
#   PULL=1   pull ghcr.io image built by CI instead of building locally
#   VALUES=helm/task-manager/values-local.yaml (default)
set -euo pipefail
cd "$(dirname "$0")/.."
NS="${NAMESPACE:-task-manager}"
SHA="${1:-$(git rev-parse HEAD)}"
REPO="${IMAGE_REPO:-ghcr.io/nayanshisingh/task-manager-devops}"
VALUES="${VALUES:-helm/task-manager/values-local.yaml}"

if [[ "${PULL:-0}" == "1" ]]; then
  docker pull "$REPO:$SHA"
elif ! docker image inspect "$REPO:$SHA" >/dev/null 2>&1; then
  docker build --build-arg GIT_SHA="$SHA" --build-arg IMAGE_TAG="$SHA" -t "$REPO:$SHA" .
fi
# kind needs images side-loaded; OrbStack/Docker Desktop share the Docker image store.
if kubectl config current-context | grep -q '^kind-'; then
  kind load docker-image "$REPO:$SHA" --name "${KIND_CLUSTER:-kind}"
fi

NAMESPACE="$NS" scripts/create-secret.sh
# --atomic: if the new pods never become Ready, Helm rolls back to the previous revision automatically.
helm upgrade --install task-manager helm/task-manager -n "$NS" \
  -f "$VALUES" --set image.repository="$REPO" --set image.tag="$SHA" \
  --atomic --wait --timeout 5m
kubectl -n "$NS" rollout status deploy/task-manager
helm -n "$NS" history task-manager --max 5
