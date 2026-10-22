# Infra on k8s (all infra in-cluster since Phase 3, 22/10/2026)

Ports the former `docker-compose.yml` infra services to k3s (`ecommerce` namespace,
VPS 162.4.177.91). Compose is removed from the repo and stopped on the VPS
(volumes kept until 29/10/2026). All 14 app services run in-cluster and use the
in-cluster DNS names below.

## Design notes

- **External access**: every externally-used infra port is `type: LoadBalancer`.
  k3s ships Klipper service-lb, which binds `LoadBalancer` services on the node
  IP at the *service port* — so 5432 stays 5432, 7080 stays 7080, exactly like the former compose `0.0.0.0:<port>` (DEV public-access behavior per vps-infra memory).
  Plain `NodePort` was NOT used because the default node-port range
  (30000-32767) cannot preserve those ports. In-cluster traffic uses the
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
- **Kafka** (fix notes — do NOT revert): internal `kafka:29092` for pods
  via headless Service (`clusterIP: None` + `publishNotReadyAddresses: true`);
  a regular ClusterIP deadlocks single-node KRaft startup because kube-proxy only
  routes to Ready endpoints while the readiness probe redirects through the
  advertised listener `kafka:29092`. `enableServiceLinks: false` on the pod
  (and on the init job) avoids the injected `KAFKA_PORT` env colliding with the
  confluent entrypoint (exit 1). Controller voter is `1@localhost:29093` —
  localhost is deliberate (in-pod quorum, no DNS). `EXTERNAL` advertised
  listener is pinned to the VPS IP in `kafka.yaml` — update it there if the IP
  ever changes. External DEV access `162.4.177.91:9092` via `kafka-external`
  LoadBalancer. Note kafdrop/redis-commander were dropped: kafka-ui (provectus)
  once ate 3Gi RAM + 700% CPU and reset SSH — use port-forward instead.

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
# Keycloak custom login theme `htad` (MISA-style + logo HTAD, text files + binary logo):
kubectl create configmap keycloak-theme -n ecommerce \
  --from-file=theme.properties=infra/keycloak/themes/htad/login/theme.properties \
  --from-file=template.ftl=infra/keycloak/themes/htad/login/template.ftl \
  --from-file=login.css=infra/keycloak/themes/htad/login/resources/css/login.css \
  --from-file=logo.png=infra/keycloak/themes/htad/login/resources/img/logo.png \
  --from-file=messages_vi.properties=infra/keycloak/themes/htad/login/messages/messages_vi.properties
# NOTE: realm.json chi import luc DB rong. Doi loginTheme/i18n tren realm da co
# phai update qua Admin API (realm cu khong tu nhan realm.json moi):
#   Admin token + PUT /admin/realms/ecommerce (loginTheme=htad, i18n vi/en, defaultLocale vi).
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

# Wave 3 — keycloak (needs postgres healthy) + clamav (zipkin removed Phase 4, Tempo owns :9411):
kubectl apply -f k8s/infra/keycloak.yaml k8s/infra/clamav.yaml
kubectl wait --for=condition=ready pod -l app=keycloak -n ecommerce --timeout=600s
kubectl wait --for=condition=ready pod -l app=clamav -n ecommerce --timeout=900s
```

Equivalently: `kubectl apply -k k8s/infra/` (after step 1), then the same waits.

## 3. Verify (mirrors the former compose healthchecks)

```bash
PGPASSWORD='<POSTGRES_ADMIN_PASSWORD>' pg_isready -h 162.4.177.91 -U postgres
MYSQL_PWD='<MYSQL_ROOT_PASSWORD>' mysql -h 162.4.177.91 -uroot -e "SELECT 1"
REDISCLI_AUTH='<REDIS_PASSWORD>' redis-cli -h 162.4.177.91 ping            # expect PONG
curl -fsS http://162.4.177.91:9200/_cluster/health | grep -o '"status":"[a-z]*"'
curl -fsS http://162.4.177.91:9000/minio/health/live
curl -fsS http://162.4.177.91:7080/health/ready
curl -fsS http://162.4.177.91:3200/ready
kubectl exec -n ecommerce deploy/kafka -- kafka-topics --bootstrap-server kafka:29092 --list
# expect 6 topics: ecommerce.auth.events, ecommerce.user.events, product-events,
# product-events.DLT, inventory-stock-movement-events, inventory-transfer-events
```

## 4. Data migration note

**Recommended: fresh init.** DEV data is disposable — the init scripts recreate
all databases/users (`01-create-databases.sh`), demo seeds
(`02-seed-auth-user.sql`) and buckets/topics (jobs). Just point app services at
k8s endpoints when they migrate.

Phase 3 cutover used fresh init (DEV data is disposable). If data must be kept in
future cutovers, dump/restore per component BEFORE switching traffic:

```bash
# postgres (source still up): pg_dumpall, then psql into k8s svc
# mysql: mysqldump --all-databases, then mysql into k8s svc
# redis: SAVE + copy dump.rdb into the redis PVC (/data) before pod start
# kafka: topics are event streams — let consumers replay, no dump
# minio: mc mirror <source-alias>/ k8s-minio/
# ES: snapshot to shared repo, restore (or reindex — DEV default)
```

## 5. Rollback (compose stopped 22/10/2026, volumes kept until 29/10/2026)

```bash
kubectl delete -k k8s/infra/   # removes workloads + PVCs; secrets/configmaps stay
cd /opt/eccommerce && docker compose up -d   # compose file still on VPS (infra profile)
```

After 29/10/2026 volumes are deleted and this rollback path expires.

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
