# Design decisions and trade-offs

| Decision | Why | Alternatives / when they'd be better |
|---|---|---|
| **PostgreSQL instead of in-memory list** | Baseline ran 2 replicas + HPA with per-pod memory, so a task created on one pod was invisible on another and lost on restart. A shared DB makes the API stateless and horizontally scalable. | Single replica + in-memory (simplest, but no HA and data loss on restart). Managed DB (RDS/Cloud SQL) in the cloud. |
| **DB as StatefulSet + PVC, single replica** | Stable network identity and storage that survives pod restarts; enough for a personal project. | CloudNativePG operator for replication/backups/failover when data matters; external managed DB to keep state out of the cluster. |
| **Flyway + `ddl-auto=validate`** | Schema is versioned and repeatable; app fails fast if code and schema disagree. | Hibernate `update` (drifts silently); Liquibase (equivalent). |
| **Deployment (not StatefulSet) for the API** | API pods are interchangeable once state is in the DB. | — |
| **Probes split**: startup → liveness (process only) → readiness (process + DB) | A DB outage should remove pods from traffic, not restart them in a loop. Startup probe absorbs JVM + migration time so liveness can be strict. | Single `/actuator/health` for both (baseline) causes restart storms when the DB is down. |
| **Rolling update, `maxSurge 1 / maxUnavailable 0` + `helm --atomic`** | Zero-downtime for a stateless API; failed rollouts (pods never Ready) are automatically rolled back to the previous Helm revision. | Blue-green (instant switch/rollback, double resources); canary with Argo Rollouts/Flagger (gradual exposure gated by metrics) when user impact of bad releases is high. Migrations must stay backward compatible (expand/contract) for any of these. |
| **Helm chart + per-env values overlays** | One source of truth; environments differ only in small overlay files; `helm history`/`rollback` give release control. Raw `k8s/` copies were deleted to avoid drift. | Kustomize (no templating, overlays only); Argo CD for continuous drift reconciliation. |
| **Image tag = commit SHA** (never `latest`) | Exact traceability commit → image → pod; SHA reported by `/actuator/info` and checked by the smoke test. | SemVer tags for public releases (in addition to the SHA). |
| **CD to ephemeral kind in CI; manual release locally** | The brief allows local Kubernetes; GitHub-hosted runners cannot reach a laptop cluster. The kind job proves the exact image deploys and passes the user journey with the same chart and script used locally. Running the release script is the human approval boundary. | Self-hosted runner or Argo CD (pull-based GitOps) for automatic deploys; GitHub Environment with required reviewers for a cloud cluster. |
| **GHCR with `GITHUB_TOKEN`** | No long-lived registry credentials; `packages: write` only in the publish job; push only after all gates pass on `main`. | DockerHub with access token (baseline). |
| **Security gates: blocking on fixable CRITICAL** | Blocking on everything (incl. unfixable) causes alert fatigue and bypasses; CRITICAL+fixable is actionable. Misconfig scan is reported. | Stricter thresholds (HIGH) for regulated workloads. |
| **Dependency CVE handling** | The image gate blocked 8 CRITICAL CVEs on Spring Boot 3.5.3. Fixed by upgrading to Boot 3.5.16 and overriding Tomcat to 10.1.61. The 2 remaining (Spring WebMVC `XsltView` RCE, SSE stream corruption) only have a fix in Spring 7 and do not apply to this JSON-only API, so they are risk-accepted in `.trivyignore` with a reason and an expiry date (31 Dec 2026). | Migrate to Spring Boot 4 (the real fix; a major upgrade, planned as a follow-up), or block the release until then. |
| **Metrics: Prometheus + SLO rules; Traces: OTel → Jaeger; Logs: JSON stdout** | RED metrics answer "is it healthy?", traces answer "where is time spent (API vs DB)?", logs with `traceId` answer "what happened for this request?". | Loki/ELK for log aggregation; Tempo instead of Jaeger; Datadog/New Relic SaaS. |
| **SLOs: 99% non-5xx, p95 < 500ms over /api** | Reflects the user journey; alerts link to runbook actions. | Multi-window burn-rate alerts at higher traffic. |
| **ClusterIP + port-forward / optional Ingress** | Avoids exposing a public LoadBalancer; only the API path is routed through the Ingress, not `/actuator`. | LoadBalancer + TLS + auth in a cloud environment. |
| **Hardened pod security** | Non-root, read-only root FS (`/tmp` emptyDir), drop all caps, seccomp, no SA token (app doesn't call the K8s API), NetworkPolicy API→DB only. | Pod Security Admission `restricted` label on the namespace; service mesh mTLS. |

## Changed-condition questions I prepared for

- **DB goes down** → readiness fails on all API pods → Service has no endpoints → 503 at the client;
  `TaskManagerNotReady` fires; pods are *not* restarted. Recovery: DB pod Ready → readiness passes → traffic returns.
- **Bad image (crash on start)** → new pod never Ready, `maxUnavailable: 0` keeps old pods serving,
  `helm --atomic` rolls back after the timeout.
- **Traffic ×10** → HPA scales API pods on CPU up to 5; DB connection pool (5/pod) becomes the limit → watch
  `hikaricp_connections_pending`; next steps: PgBouncer, bigger DB, read replicas.
- **Need multiple environments** → add `values-staging.yaml`, separate namespace, promote the same SHA.
