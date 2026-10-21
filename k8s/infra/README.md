# Infra on k8s (phase 0 — before app services)

Ports `docker-compose.yml` infra services to k3s (`ecommerce` namespace, VPS
162.4.177.91). App services (`k8s/discovery|gateway|auth|user`, compose
`profiles: apps`) are untouched by this change.

## Design notes

- **External access**: every externally-used infra port is `type: LoadBalancer`.
  k3s ships Klipper service-lb, which binds `LoadBalancer` services on the node
  IP at the *service port* — so 5432 stays 5432, 7080 stays 7080, exactly like
  compose `0.0.0.0:<port>` (DEV public-access behavior per vps-infra memory).
  Plain `NodePort` was NOT used because the default node-port range
  (30000-32767) cannot preserve compose ports. In-cluster traffic uses the
  `ClusterIP` services / DNS names (`postgres:5432`, `kafka:29092`,
  `minio:9000`, `clamav:3310`, ...).
- **Storage**: all PVCs use `storageClassName: local-path` (k3s default), single
  replica + `strategy: Recreate` (never two writers on an RWO volume).
- **Init scripts / realm**: `infra/postgres/*.sh`, `infra/mysql/*` and
  `infra/keycloak/realm.json` stay the single source of truth in git and are
  loaded via imperative ConfigMaps (step 1 below) — they are NOT duplicated
  into these manifests, so no drift.
- **Secrets**: key shapes live in `secret-templates.yaml.example` (`.example`
  suffix is deliberate — `kubectl apply -k` never creates CHANGEME secrets).
  Runtime values come from `/opt/eccommerce/.env` on the VPS.
- **Kafka**: internal `kafka:29092` (ClusterIP) for pods, external
  `162.4.177.91:9092` (LoadBalancer) for DEV tools. `EXTERNAL` advertised
  listener is pinned to the VPS IP in `kafka.yaml` — update it there if the IP
  ever changes.

## 0. Pre-flight (on the VPS)

```bash
kubectl get nodes                        # k3s Ready
kubectl get ns ecommerce                 # must exist (phase-1 created it)
kubectl get storageclass local-path      # k3s default provisioner
```

## 1. Secrets + init ConfigMaps (order matters — first)

```bash
set -a; source /opt/eccommerce/.env; set +a

kubectl create secret generic infra-postgres-secrets -n ecommerce \
  --from-literal=postgres-password="$POSTGRES_ADMIN_PASSWORD" \
  --from-literal=keycloak-db-password="$KEYCLOAK_DB_PASSWORD" \
  --from-literal=catalog-db-password="$CATALOG_DB_PASSWORD" \
  --from-literal=inventory-db-password="$INVENTORY_DB_PASSWORD" \
  --from-literal=order-db-password="$ORDER_DB_PASSWORD" \
  --from-literal=notification-db-password="$NOTIFICATION_DB_PASSWORD" \
  --from-literal=product-db-password="$PRODUCT_DB_PASSWORD" \
  --from-literal=review-db-password="$REVIEW_DB_PASSWORD" \
  --from-literal=analytics-db-password="$ANALYTICS_DB_PASSWORD"

kubectl create secret generic infra-mysql-secrets -n ecommerce \
  --from-literal=mysql-root-password="$MYSQL_ROOT_PASSWORD" \
  --from-literal=auth-db-password="$AUTH_DB_PASSWORD" \
  --from-literal=user-db-password="$USER_DB_PASSWORD" \
  --from-literal=payment-db-password="$PAYMENT_DB_PASSWORD"

kubectl create secret generic infra-redis-secret -n ecommerce \
  --from-literal=redis-password="$REDIS_PASSWORD"

kubectl create secret generic infra-minio-secrets -n ecommerce \
  --from-literal=root-user="$MINIO_ROOT_USER" \
  --from-literal=root-password="$MINIO_ROOT_PASSWORD"

kubectl create secret generic infra-keycloak-secrets -n ecommerce \
  --from-literal=admin-username="${KEYCLOAK_ADMIN:-admin}" \
  --from-literal=admin-password="$KEYCLOAK_ADMIN_PASSWORD" \
  --from-literal=db-password="$KEYCLOAK_DB_PASSWORD"

# Init scripts + realm (single source of truth stays in infra/...):
kubectl create configmap postgres-init -n ecommerce --from-file=infra/postgres/
kubectl create configmap mysql-init -n ecommerce --from-file=infra/mysql/
kubectl create configmap keycloak-realm -n ecommerce \
  --from-file=realm.json=infra/keycloak/realm.json
```

## 2. Apply order

kustomize does not order resources — apply in waves and wait for Ready:

```bash
# Wave 1 — stateful base (postgres/mysql/redis first):
kubectl apply -f k8s/infra/postgres.yaml k8s/infra/mysql.yaml k8s/infra/redis.yaml
kubectl wait --for=condition=ready pod -l app=postgres -n ecommerce --timeout=300s
kubectl wait --for=condition=ready pod -l app=mysql -n ecommerce --timeout=420s
kubectl wait --for=condition=ready pod -l app=redis -n ecommerce --timeout=180s

# Wave 2 — kafka + ES + minio (+ init jobs):
kubectl apply -f k8s/infra/kafka.yaml k8s/infra/elasticsearch.yaml k8s/infra/minio.yaml
kubectl wait --for=condition=ready pod -l app=kafka -n ecommerce --timeout=600s
kubectl wait --for=condition=ready pod -l app=elasticsearch -n ecommerce --timeout=600s
kubectl wait --for=condition=ready pod -l app=minio -n ecommerce --timeout=300s
kubectl wait --for=condition=complete job/kafka-init-topics job/minio-init -n ecommerce --timeout=900s

# Wave 3 — keycloak (needs postgres healthy) + zipkin + clamav:
kubectl apply -f k8s/infra/keycloak.yaml k8s/infra/zipkin.yaml k8s/infra/clamav.yaml
kubectl wait --for=condition=ready pod -l app=keycloak -n ecommerce --timeout=600s
kubectl wait --for=condition=ready pod -l app=zipkin -n ecommerce --timeout=300s
kubectl wait --for=condition=ready pod -l app=clamav -n ecommerce --timeout=900s
```

Equivalently: `kubectl apply -k k8s/infra/` (after step 1), then the same waits.

## 3. Verify (mirrors compose healthchecks)

```bash
PGPASSWORD='<POSTGRES_ADMIN_PASSWORD>' pg_isready -h 162.4.177.91 -U postgres
MYSQL_PWD='<MYSQL_ROOT_PASSWORD>' mysql -h 162.4.177.91 -uroot -e "SELECT 1"
REDISCLI_AUTH='<REDIS_PASSWORD>' redis-cli -h 162.4.177.91 ping            # expect PONG
curl -fsS http://162.4.177.91:9200/_cluster/health | grep -o '"status":"[a-z]*"'
curl -fsS http://162.4.177.91:9000/minio/health/live
curl -fsS http://162.4.177.91:7080/health/ready
curl -fsS http://162.4.177.91:9411/health
kubectl exec -n ecommerce deploy/kafka -- kafka-topics --bootstrap-server kafka:29092 --list
# expect: ecommerce.auth.events, ecommerce.user.events
```

## 4. Data migration note

**Recommended: fresh init.** DEV data is disposable — the init scripts recreate
all databases/users (`01-create-databases.sh`), demo seeds
(`02-seed-auth-user.sql`) and buckets/topics (jobs). Just point app services at
k8s endpoints when they migrate.

If compose data must be kept, dump/restore per component BEFORE switching
traffic (one backup per cutover, verify restore into k8s first):

```bash
# postgres (compose still up): pg_dumpall, then psql into k8s svc
# mysql: mysqldump --all-databases, then mysql into k8s svc
# redis: SAVE + copy dump.rdb into the redis PVC (/data) before pod start
# kafka: topics are event streams — let consumers replay, no dump
# minio: mc mirror compose-minio/ k8s-minio/
# ES: snapshot to shared repo, restore (or reindex — DEV default)
```

## 5. Rollback (park k8s infra, restore compose)

```bash
kubectl delete -k k8s/infra/   # removes workloads + PVCs; secrets/configmaps stay
docker compose up -d           # compose infra profile (no --profile apps)
```

Secrets and init ConfigMaps are intentionally left behind by `delete -k`
(they are not in kustomization resources) — delete explicitly if rotating:

```bash
kubectl delete secret infra-postgres-secrets infra-mysql-secrets infra-redis-secret \
  infra-minio-secrets infra-keycloak-secrets -n ecommerce
kubectl delete configmap postgres-init mysql-init keycloak-realm -n ecommerce
```

## 6. Secret inventory (names only, no values)

- ns `ecommerce`: `infra-postgres-secrets` (postgres-password,
  keycloak/catalog/inventory/order/notification/product/review/analytics-db-password),
  `infra-mysql-secrets` (mysql-root-password, auth/user/payment-db-password),
  `infra-redis-secret` (redis-password), `infra-minio-secrets` (root-user,
  root-password), `infra-keycloak-secrets` (admin-username, admin-password,
  db-password).
- Verify no plaintext: `grep -rEi 'password\s*[:=]\s*[^C]' k8s/infra/` must
  only hit CHANGEME placeholders and `$VAR` references.
