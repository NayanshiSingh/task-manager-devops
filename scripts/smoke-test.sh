#!/usr/bin/env bash
# End-to-end user journey against a running deployment. Fails on the first unexpected response.
# Usage: scripts/smoke-test.sh [base-url] [expected-sha]
set -euo pipefail
BASE="${1:-http://localhost:8080}"
EXPECTED_SHA="${2:-}"

check() { # name expected actual
  if [[ "$2" != "$3" ]]; then echo "FAIL $1: expected $2 got $3"; exit 1; fi
  echo "PASS $1 ($3)"
}

INFO=$(curl -fsS "$BASE/actuator/info")
echo "info: $INFO"
if [[ -n "$EXPECTED_SHA" ]]; then
  echo "$INFO" | grep -q "\"commit\":\"$EXPECTED_SHA\"" || { echo "FAIL running version != $EXPECTED_SHA"; exit 1; }
  echo "PASS running version is $EXPECTED_SHA"
fi
check readiness 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health/readiness")"

BODY=$(curl -fsS -X POST "$BASE/api/tasks" -H 'Content-Type: application/json' \
  -d '{"title":"smoke-test","description":"created by smoke test"}')
ID=$(echo "$BODY" | sed -E 's/.*"id":([0-9]+).*/\1/')
echo "created task id=$ID"
check get 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/tasks/$ID")"
check update 200 "$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/api/tasks/$ID" \
  -H 'Content-Type: application/json' -d '{"title":"smoke-test","completed":true}')"
check validation 400 "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/tasks" \
  -H 'Content-Type: application/json' -d '{"title":""}')"
check delete 204 "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/api/tasks/$ID")"
check gone 404 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/tasks/$ID")"
echo "Smoke test passed"
