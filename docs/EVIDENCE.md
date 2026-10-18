# Operational evidence

Captured on 18 Oct 2026 from a local OrbStack Kubernetes cluster (v1.36.5, arm64).
Tested version: commit `74bae013a12eb156532bf304b68eeefa8e53fb31`
(image `ghcr.io/nayanshisingh/task-manager-devops:74bae013a12eb156532bf304b68eeefa8e53fb31`, Helm revision 1).
The CI run for this PR (kind deploy + E2E + ZAP) is linked from the PR checks tab.

## 1. Source → build
- `mvn -B clean checkstyle:check verify` → Checkstyle clean, **13 tests, 0 failures** (service tests on H2 via Flyway, API tests incl. validation 400, probes, `/actuator/prometheus`).
- Found during this work: the baseline `checkstyle.xml` was invalid (`LineLength` under `TreeWalker`) and had never run because CI used `continue-on-error`. Fixed and made blocking.

## 2. Delivery → runtime (`scripts/deploy-local.sh`)
```
namespace/task-manager created
secret/task-manager-db created            # random password, generated in-cluster, not in git
Release "task-manager" does not exist. Installing it now.
STATUS: deployed   REVISION: 1
deployment "task-manager" successfully rolled out
```
```
pod/task-manager-5c9c4b86b7-2wh56   1/1  Running  2 (31s ago)
pod/task-manager-5c9c4b86b7-jg4c7   1/1  Running  0
pod/task-manager-db-0               1/1  Running  0
service/task-manager      ClusterIP  192.168.194.207  80/TCP
service/task-manager-db   ClusterIP  None             5432/TCP
persistentvolumeclaim/data-task-manager-db-0   Bound  1Gi  RWO  local-path
horizontalpodautoscaler/task-manager   cpu: <unknown>/70%   2  5  2
poddisruptionbudget/task-manager       MIN AVAILABLE 1      ALLOWED DISRUPTIONS 1
networkpolicy/task-manager-api, networkpolicy/task-manager-db
```
Notes: the first API pod restarted twice because it started before PostgreSQL accepted connections
(Flyway fails → container exits → kubelet restarts it). It self-healed; an init container waiting for the
DB would avoid the restarts. HPA shows `<unknown>` because metrics-server is not installed on this cluster.

## 3. Working user journey on the deployed version (`scripts/smoke-test.sh`)
```
info: {"app":{"commit":"74bae013a12eb156532bf304b68eeefa8e53fb31","image-tag":"74bae013..."},"build":{"version":"1.1.0",...}}
PASS running version is 74bae013a12eb156532bf304b68eeefa8e53fb31
PASS readiness (200)
created task id=1
PASS get (200)
PASS update (200)
PASS validation (400)
PASS delete (204)
PASS gone (404)
Smoke test passed
```

## 4. Observability
Prometheus (ServiceMonitor) scraping both pods:
```
task-manager-5c9c4b86b7-2wh56 http://192.168.194.3:8080/actuator/prometheus up
task-manager-5c9c4b86b7-jg4c7 http://192.168.194.6:8080/actuator/prometheus up
```
SLO rules loaded and evaluating (PrometheusRule):
```
task_manager:http_error_ratio:rate5m ok
task_manager:http_latency_p95:rate5m ok
TaskManagerHighErrorRate ok inactive
TaskManagerHighLatency  ok inactive
TaskManagerNotReady     ok inactive
```
Request counts by status (`/api/tasks`): `201: 11`, `200: 10`, `400: 1`.

Structured log line with trace correlation:
```json
{"@timestamp":"2026-10-18T18:13:19.461Z","log":{"level":"INFO","logger":"com.taskmanager.service.TaskService"},"service":{"name":"task-manager","version":"1.1.0"},"message":"Created task id=1","traceId":"b5a475afe29f98e315b5697f8a2dbf92","spanId":"ca4d2daf89699b48"}
```
Jaeger: service `task-manager` registered, 50+ traces collected via OTLP.
Grafana dashboard "Task Manager API" is provisioned from the chart ConfigMap (`grafana_dashboard: "1"`).

## 5. Fault drill: database outage
- **Symptom/hypothesis**: if PostgreSQL disappears, pods should become *unready* (removed from the Service) but must not be restarted, and data must survive on the PVC.
- **Change**: `kubectl -n task-manager scale sts task-manager-db --replicas=0`
- **Evidence** (after 20s):
  ```
  task-manager-5c9c4b86b7-2wh56   0/1   Running   2 (102s ago)   # unchanged restart count
  task-manager-5c9c4b86b7-jg4c7   0/1   Running   0
  endpoints/task-manager          <none>                         # Service has no backends
  ```
  A request through `kubectl port-forward` (which targets one pod directly, bypassing Service endpoints) returned 500;
  traffic through the Service/Ingress would get no backend instead.
- **Recovery**: `--replicas=1` → DB Ready in ~15s → both API pods back to `1/1` with no restarts.
- **Verification**: `{"taskCount":10,"status":"UP"}` — the 10 tasks created before the outage were still there (PVC persisted).

## Limitations of this evidence
- Local cluster only; NetworkPolicy enforcement depends on the CNI and was not separately tested.
- No load test was run, so HPA scaling and the latency SLO were not exercised under load.
