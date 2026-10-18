# Runbook

## What "healthy" means

| Signal | Healthy | Where |
|---|---|---|
| Availability SLO | ≥ 99% of `/api/*` requests non-5xx (5m) | `task_manager:http_error_ratio:rate5m`, Grafana "5xx error ratio" |
| Latency SLO | p95 of `/api/*` < 500ms | `task_manager:http_latency_p95:rate5m` |
| Ready replicas | ≥ 1 (normally ≥ 2) | `kubectl -n task-manager get deploy task-manager` |
| User journey | `scripts/smoke-test.sh` passes | create → get → update → validate → delete |

## High error rate
Alert `TaskManagerHighErrorRate`.
1. `kubectl -n task-manager logs deploy/task-manager | grep '"log.level":"ERROR"'` — note `traceId`.
2. Open the trace in Jaeger to see whether the failing span is the DB call.
3. If it started with a release: `helm -n task-manager history task-manager` → `helm -n task-manager rollback task-manager <prev>`.
4. Verify: error ratio back under 1%, smoke test passes.

## High latency
Alert `TaskManagerHighLatency`.
1. Grafana: DB pool panel — pending connections > 0 means the pool/DB is the bottleneck.
2. Jaeger: compare HTTP span vs JDBC span durations.
3. Check CPU throttling / HPA: `kubectl -n task-manager get hpa`, `kubectl top pods -n task-manager`.

## No ready pods
Alert `TaskManagerNotReady`.
1. `kubectl -n task-manager get pods`, `kubectl -n task-manager describe pod <pod>`.
2. Readiness failing but liveness OK → dependency problem: `kubectl -n task-manager get pod task-manager-db-0`, `kubectl -n task-manager logs task-manager-db-0`.
3. CrashLoopBackOff → app/config problem → logs of previous container (`--previous`), roll back.

## Fault-injection drill (DB outage)
```bash
kubectl -n task-manager scale statefulset task-manager-db --replicas=0   # symptom: 503s, pods 0/1 Ready, no restarts
kubectl -n task-manager get pods -w
kubectl -n task-manager scale statefulset task-manager-db --replicas=1   # recovery
scripts/smoke-test.sh http://localhost:8080                              # verification (data persisted on the PVC)
```
