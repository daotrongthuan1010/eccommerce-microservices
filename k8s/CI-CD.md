# CI/CD — k8s fullstack (Docker Hub + GitHub Actions + k3s)

Registry: Docker Hub `daotrongthuan/*` | Namespace: `ecommerce` | VPS: `162.4.177.91`
Phase 1 runbook: `k8s/README.md`. This doc covers CI/CD + phase-2 services.

## 1. Secrets catalog (GitHub → Settings → Secrets and variables → Actions)

| Name | Purpose |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub login for CI push + VPS pull |
| `DOCKERHUB_TOKEN` | Docker Hub PAT (never commit) |
| `VPS_SSH_HOST` | `162.4.177.91` |
| `VPS_SSH_PORT` | SSH port |
| `VPS_SSH_USER` | SSH user |
| `VPS_SSH_KEY` | Dedicated private key for Actions (do NOT reuse dev key) |
| `AUTH_DB_PASSWORD` / `USER_DB_PASSWORD` | Phase-1 service DB passwords |
| `CATALOG_DB_PASSWORD` / `INVENTORY_DB_PASSWORD` / `ORDER_DB_PASSWORD` / `PAYMENT_DB_PASSWORD` / `NOTIFICATION_DB_PASSWORD` / `PRODUCT_DB_PASSWORD` / `REVIEW_DB_PASSWORD` / `ANALYTICS_DB_PASSWORD` | Phase-2 service DB passwords |
| `REDIS_PASSWORD` | Shared Redis password |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO creds (catalog, product, file) |

Values must match the passwords already seeded in k8s infra secrets (`infra-postgres-secrets`, `infra-mysql-secrets`, ... created from `/opt/eccommerce/.env` per `k8s/infra/README.md` step 1; compose removed 22/10/2026) — CD only transports them, it does not rotate.

## 2. imagePullSecret creation (one-time, on VPS)

```bash
kubectl create namespace ecommerce --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret docker-registry dockerhub-pull -n ecommerce \
  --docker-username=daotrongthuan --docker-password='<DOCKERHUB_PAT>'
kubectl get secret dockerhub-pull -n ecommerce   # type must be kubernetes.io/dockerconfigjson
```

CD re-applies `dockerhub-pull` on every run from `DOCKERHUB_*` secrets (idempotent).

## 3. Runtime Secrets (rendered on VPS, never committed)

Key shapes live in `k8s/<svc>/secret-template.yaml.example` (`.example` suffix is
deliberate — directory `kubectl apply` must not create CHANGEME secrets).
CD renders `/tmp/k8s-secrets-${SVC}-${IMAGE_TAG}.yaml` from GitHub Secrets, applies it, then
`shred -u`s the file (`trap` on EXIT).

Manual equivalent:

```bash
kubectl create secret generic catalog-secrets -n ecommerce \
  --from-literal=db-password='<CATALOG_DB_PASSWORD>' \
  --from-literal=redis-password='<REDIS_PASSWORD>' \
  --from-literal=minio-access-key='<MINIO_USER>' \
  --from-literal=minio-secret-key='<MINIO_PASSWORD>'
# search-secrets: redis-password only. file-secrets: minio-access-key/minio-secret-key only.
```

## 4. Rollout order

```text
file → catalog/product → inventory → order/payment → search/notification → review/analytics
```

(Phase 1 first: discovery → gateway → auth/user. Infra `k8s/infra/` is applied once,
separately — CD waves cover app services only.)

Rationale: file + catalog/product own MinIO buckets; inventory gates order/payment;
search/notification are downstream consumers; review/analytics are leaf services.
Each wave: `kubectl apply -f k8s/<svc>/`, `rollout status --timeout=300s`, fail →
`rollout undo` + status again, workflow exits non-zero.

## 5. Rollback SOP

Preferred (old `daotrongthuan/<svc>:<sha>` tags remain on Docker Hub):

```bash
kubectl rollout undo deployment/<svc> -n ecommerce
kubectl rollout status deployment/<svc> -n ecommerce
kubectl rollout history deployment/<svc> -n ecommerce   # pick revision if needed
```

Emergency fallback (compose stopped 22/10/2026, volumes kept until 29/10/2026, compose file still at /opt/eccommerce on VPS):

```bash
kubectl scale deploy --all --replicas=0 -n ecommerce
cd /opt/eccommerce && docker compose up -d   # infra profile
```

## 6. Pipelines

- **CI** (`.github/workflows/ci.yml`): push/PR to `develop`/`main` + `workflow_dispatch`
  (input `service`: `all` + 14 names; a chosen service builds regardless of
  paths-filter). `dorny/paths-filter` maps `services/<svc>/**` → build that
  service; `common/**`, `pom.xml`, `Dockerfile` → build all 14. Temurin 21 +
  Maven cache + public Central mirror (VPS Nexus unreachable from runners).
  Three ordered stages — **build** pushes only `daotrongthuan/<svc>:<sha>` →
  **scan** (Trivy HIGH/CRITICAL: report-only on `develop`, blocking
  (`exit-code: 1`) on `main`) → **promote** pushes the `develop`/`latest`
  alias via `buildx imagetools create` (no rebuild), only if scan passed
  (`needs: [build, scan]`, push events only).
- **CD single-service** (`.github/workflows/cd-dev.yml`): on CI success
  (`workflow_run`) or manual dispatch with inputs `service` (same 14 + `all`)
  + `sha`. Each service is deployed by its **own matrix job** (`fail-fast:
  false`): SCP only `k8s/<svc>/` to VPS, substitute `__IMAGE_TAG__` with the
  sha in a temp copy, render ONLY that service's Secret from GitHub Secrets,
  `kubectl apply -f` only that dir, `rollout status` + auto `undo` scoped to
  ONLY that Deployment, then smoke that service (gateway
  `http://162.4.177.91:8080/actuator/health` 30 × 10s expecting
  `"status":"UP"` + direct in-cluster
  `http://<svc>:<port>/actuator/health` via ephemeral curl pod). A failure
  fails only that service's job — other services are untouched.
- **CD full rollout** (`.github/workflows/cd-all.yml`): manual dispatch with
  input `sha`. Six sequential wave jobs (`needs` chain), each a matrix over
  its wave, every service deployed via the shared composite action
  (`.github/actions/deploy-service/action.yml`) used by cd-dev.yml.
- **Shared deploy action** (`.github/actions/deploy-service/action.yml`):
  single-service deploy logic (scp → tag substitute → per-service Secret →
  apply → rollout/undo → dual smoke) shared by cd-dev.yml and cd-all.yml.

### Deploy one service

Actions → cd-dev → Run workflow → `service: catalog-service`, `sha:
<commit-sha>` (empty = HEAD of develop). Only the `catalog-service` job runs;
only `k8s/catalog/` is copied/applied; only `catalog-secrets` is rendered;
only `deployment/catalog-service` rolls.

### Deploy all (ordered)

Actions → cd-all → Run workflow → `sha: <commit-sha>`. Waves run in order
(core → identity → data → trade → downstream → review/analytics); within a
wave services deploy in parallel; a wave failure stops later waves
(`fail-fast: true` + `needs` chain).

### Roll back a single service

Preferred (old `daotrongthuan/<svc>:<sha>` tags remain on Docker Hub):

```bash
kubectl rollout undo deployment/<svc> -n ecommerce
kubectl rollout status deployment/<svc> -n ecommerce
kubectl rollout history deployment/<svc> -n ecommerce   # pick revision if needed
```

Or redeploy the known-good sha of just that service:

Actions → cd-dev → `service: <svc>`, `sha: <last-good-sha>`.

Other services are not touched in either path.

## 7. Verify no plaintext

```bash
grep -rEi 'password\s*[:=]\s*[^C]' k8s/   # must only hit CHANGEME placeholders
```

## 8. Phase 3 infra migration — DONE 22/10/2026

Compose stopped on VPS, infra runs in-cluster (single-node + local-path PVC =
disk failure loses data; backup via Phase 4 cron pg_dump/mysqldump +
`mc mirror` to MinIO). Service ConfigMaps use in-cluster DNS; external DEV access
kept via LoadBalancer ports (memory vps-infra). Do NOT expose ES via Ingress.
