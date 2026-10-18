#!/usr/bin/env bash
# Creates the DB credential Secret out-of-band with a random password.
# Idempotent: an existing Secret is left untouched so the DB password never drifts from the PVC data.
set -euo pipefail
NS="${NAMESPACE:-task-manager}"
kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS"
if kubectl -n "$NS" get secret task-manager-db >/dev/null 2>&1; then
  echo "Secret task-manager-db already exists in $NS"
  exit 0
fi
kubectl -n "$NS" create secret generic task-manager-db \
  --from-literal=username=tasks \
  --from-literal=password="$(openssl rand -base64 24 | tr -d '\n')"
echo "Created secret task-manager-db in $NS"
