# Task Manager API — Advanced DevOps Project

| | |
|---|---|
| Student | _Nayanshi Singh_ — Student ID: _&lt;fill in&gt;_ |
| Project | Task Manager API (Spring Boot REST service) |
| Repository | https://github.com/NayanshiSingh/task-manager-devops |
| Baseline commit | [`f40c954985398e6f89ad49565462f30cf43013b6`](https://github.com/NayanshiSingh/task-manager-devops/commit/f40c954985398e6f89ad49565462f30cf43013b6) |
| Improvements | One PR from branch `devops-improvements` → `main` |

A small REST API for creating, listing, updating and deleting tasks. The project takes this
existing application and makes it deployable, operable and explainable on Kubernetes.

## What existed at the baseline

- Spring Boot 3.2 app with **in-memory** storage (`ArrayList`), unit tests for the service.
- GitHub Actions CI: Checkstyle (non-blocking), CodeQL, OWASP Dependency-Check (non-blocking),
  Docker build, Trivy (non-blocking), push to DockerHub with long-lived secrets.
- "CD" workflow that only printed manifests (`kubectl --dry-run`) and ran a curl-based "DAST" against a JAR.
- Raw manifests in `k8s/` with an `IMAGE_PLACEHOLDER`, 2 replicas + HPA (which, with in-memory
  state, meant every pod had different data), a `LoadBalancer` service, no secrets, no observability.

## What this PR adds (summary)

| Area | Change |
|---|---|
| Architecture / state | PostgreSQL (StatefulSet + PVC) via Spring Data JPA; schema owned by Flyway migrations. All API replicas now share state, which makes 2+ replicas and HPA correct. Input validation → 400 instead of 500. Graceful shutdown. |
| Kubernetes | Helm chart: Deployment (rolling, `maxUnavailable: 0`), startup/liveness/readiness probes (readiness includes DB, liveness does not), requests/limits, HPA, PDB, topology spread, ClusterIP Service, optional Ingress. |
| Configuration | One chart, small per-environment overlays (`values-ci.yaml`, `values-local.yaml`); config in a ConfigMap with checksum-triggered rollouts; the raw `k8s/` copies were removed to avoid drift. `scripts/drift-check.sh` diffs live vs. rendered state. |
| CI/CD | Single pipeline: blocking Checkstyle + tests → CodeQL, gitleaks, Trivy fs/config, Helm lint + kubeconform → image build with Trivy gate → **real deployment of that exact image to an ephemeral kind cluster**, user-journey smoke test (verifies the running commit SHA) and OWASP ZAP baseline → on `main`, publish the tested image to GHCR (no long-lived registry secrets). |
| Release control | Image tag = commit SHA; SHA baked into the image and reported by `/actuator/info`, pod label `app.kubernetes.io/version`, Helm history. `helm upgrade --atomic` auto-rolls back if new pods never become Ready; `helm rollback` for manual recovery. Release to the cluster is a deliberate manual step. |
| Observability | Prometheus metrics (`/actuator/prometheus`, latency histograms, business counters, Hikari pool), ServiceMonitor, SLO recording rules + alerts (PrometheusRule), Grafana dashboard, JSON (ECS) logs with `traceId`, OpenTelemetry traces to Jaeger. |
| Security | DB credentials generated into a Kubernetes Secret out-of-band (never in git); non-root, read-only root FS, all capabilities dropped, seccomp `RuntimeDefault`, no service-account token; NetworkPolicies (only API → DB); ClusterIP instead of a public LoadBalancer; actuator exposes only health/info/prometheus with details hidden; GHCR push via `GITHUB_TOKEN` with job-scoped permissions. |

Design reasoning: [docs/DECISIONS.md](docs/DECISIONS.md) · Operations / SLOs: [docs/RUNBOOK.md](docs/RUNBOOK.md) · Evidence: [docs/EVIDENCE.md](docs/EVIDENCE.md)

## Architecture

```
            kubectl port-forward / Ingress
                        │
                ┌───────▼────────┐  ClusterIP Service "task-manager" (:80 → :8080)
                │  Service       │
                └───┬────────┬───┘
         ┌──────────▼─┐   ┌──▼─────────┐   Deployment (2–5 replicas, HPA)
         │ API pod    │   │ API pod    │   Spring Boot, non-root, read-only FS
         └──┬───────┬─┘   └─┬───────┬──┘
   JDBC     │       │ OTLP  │       │  /actuator/prometheus  ◄── Prometheus (ServiceMonitor)
 (NetPol)   │       └───────┼───────┴──────► Jaeger (observability ns)
         ┌──▼───────────────▼──┐
         │ PostgreSQL (STS+PVC)│   headless Service "task-manager-db"
         └─────────────────────┘
```

Request path: client → Service → ready API pod → `TaskController` → `TaskService` → JPA → PostgreSQL.
Failure boundaries: a DB outage makes pods **unready** (removed from the Service, not restarted);
a hung JVM fails **liveness** and is restarted; a bad release never becomes Ready and is rolled back.

## Prerequisites

- Docker, `kubectl`, Helm 3.x
- A local Kubernetes cluster: OrbStack / Docker Desktop Kubernetes, kind or Minikube (tested on OrbStack, Kubernetes v1.36)
- Java 17 + Maven only if you want to run tests outside Docker

## Run it

```bash
# 1. (once) observability stack: Prometheus, Grafana, Alertmanager, Jaeger
kubectl create namespace observability
kubectl apply -f observability/jaeger.yaml
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
  -n observability -f observability/kube-prometheus-stack-values.yaml --wait

# 2. build the image for the current commit and release it (creates the DB Secret if missing)
scripts/deploy-local.sh                 # or: PULL=1 scripts/deploy-local.sh <sha>  (image from CI/GHCR)
# without the observability stack:  VALUES=helm/task-manager/values-ci.yaml scripts/deploy-local.sh

# 3. use it
kubectl -n task-manager port-forward svc/task-manager 8080:80
scripts/smoke-test.sh http://localhost:8080 "$(git rev-parse HEAD)"
curl localhost:8080/actuator/info       # shows the running commit
```

Access to tools:

| Tool | Command | URL |
|---|---|---|
| API | `kubectl -n task-manager port-forward svc/task-manager 8080:80` | http://localhost:8080/api/tasks |
| Grafana | `kubectl -n observability port-forward svc/monitoring-grafana 3000:80` | http://localhost:3000 (user `admin`, password: `kubectl -n observability get secret monitoring-grafana -o jsonpath='{.data.admin-password}' \| base64 -d`) → dashboard "Task Manager API" |
| Prometheus | `kubectl -n observability port-forward svc/monitoring-kube-prometheus-prometheus 9090` | http://localhost:9090/alerts |
| Jaeger | `kubectl -n observability port-forward svc/jaeger-ui 16686` | http://localhost:16686 (service `task-manager`) |

Rollback / drift:

```bash
helm -n task-manager history task-manager
helm -n task-manager rollback task-manager <revision>
scripts/drift-check.sh
```

Tests without a local JDK:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-17 mvn -B checkstyle:check verify
```

## API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/tasks` | List tasks |
| GET | `/api/tasks/{id}` | Get task |
| POST | `/api/tasks` | Create (`title` required, ≤200 chars) |
| PUT | `/api/tasks/{id}` | Update |
| DELETE | `/api/tasks/{id}` | Delete |
| GET | `/api/tasks/health` | App health + task count |
| GET | `/actuator/health/liveness`, `/actuator/health/readiness` | Probes |
| GET | `/actuator/info` | Version / commit |
| GET | `/actuator/prometheus` | Metrics |

## Known limitations

- Single PostgreSQL instance (no replication / automated backups); losing the PVC loses data. A managed DB or an operator (CloudNativePG) would be used in production.
- The cloud-hosted pipeline cannot reach a laptop cluster, so CD deploys to an **ephemeral kind cluster inside CI**; the release to the local cluster is a manual `scripts/deploy-local.sh` step (a self-hosted runner or Argo CD would automate it).
- Logs are read with `kubectl logs` (structured JSON with trace IDs); no log aggregation backend (Loki) is installed. Jaeger uses in-memory storage.
- NetworkPolicies require a CNI that enforces them (kind's default kindnet and OrbStack may not enforce them).
- No TLS on the local Ingress / port-forward.
- Terraform / cloud are out of scope, as allowed by the brief.
