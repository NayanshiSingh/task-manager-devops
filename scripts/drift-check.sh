#!/usr/bin/env bash
# Detects drift between the live cluster and what the chart renders for the currently released version.
# Exit code 0 = no drift, 1 = drift found.
set -euo pipefail
cd "$(dirname "$0")/.."
NS="${NAMESPACE:-task-manager}"
VALUES="${VALUES:-helm/task-manager/values-local.yaml}"
TAG=$(helm -n "$NS" get values task-manager -o json | python3 -c 'import json,sys;print(json.load(sys.stdin)["image"]["tag"])')
REPO=$(helm -n "$NS" get values task-manager -o json | python3 -c 'import json,sys;print(json.load(sys.stdin)["image"]["repository"])')
echo "Released image: $REPO:$TAG"
helm template task-manager helm/task-manager -n "$NS" -f "$VALUES" \
  --set image.repository="$REPO" --set image.tag="$TAG" | kubectl diff -n "$NS" -f - && echo "No drift"
