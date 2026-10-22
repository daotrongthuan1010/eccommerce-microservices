# k8s Phase 1 — core services (discovery, gateway, auth, user)
# Registry: Docker Hub daotrongthuan/* | Namespace: ecommerce | VPS: 162.4.177.91 (k3s API :24700)

## 0. Pre-flight

```bash
git status --porcelain            # must be empty before build/push
git tag k8s-phase1-base           # traceability tag
export TAG=phase1-$(git rev-parse --short HEAD)
```

## 1. Registry auth (imagePullSecret dockerhub-pull)

Values (username + PAT) come from the operator, never committed.
PAT is also stored in GitHub Secrets as DOCKERHUB_TOKEN (username: DOCKERHUB_USERNAME)
for CI (change k8s-cicd-fullstack).

```bash
kubectl create namespace ecommerce --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret docker-registry dockerhub-pull -n ecommerce \
  --docker-username=daotrongthuan --docker-password='<DOCKERHUB_PAT>'
kubectl get secret dockerhub-pull -n ecommerce   # type must be kubernetes.io/dockerconfigjson
```

## 2. Build + push (from repo root, Dockerfile ARG SERVICE)

```bash
for SVC in discovery-service api-gateway auth-service user-service; do
  docker build --build-arg SERVICE=$SVC -t daotrongthuan/$SVC:$TAG -t daotrongthuan/$SVC:phase1-latest .
  docker push daotrongthuan/$SVC:$TAG
  docker push daotrongthuan/$SVC:phase1-latest
done
```

## 3. Runtime secrets (never committed)

Key-shape reference lives in k8s/auth/secret-template.yaml.example,
k8s/gateway/secret-template.yaml.example and k8s/user/secret-template.yaml.example.
The `.example` suffix is deliberate:
it keeps `kubectl apply -f k8s/auth/` (directory apply) from creating a
Secret with the CHANGEME placeholder — always create the real Secret via the
commands below, never by applying the template.

```bash
kubectl create secret generic auth-secrets -n ecommerce \
  --from-literal=db-password='<AUTH_DB_PASSWORD>' \
  --from-literal=redis-password='<REDIS_PASSWORD>' \
  --from-literal=iam-internal-token='<RANDOM_32B>' \
  --from-literal=keycloak-admin-client-secret='<KC_ADMIN_SECRET>'
kubectl create secret generic user-secrets -n ecommerce \
  --from-literal=db-password='<USER_DB_PASSWORD>' \
  --from-literal=redis-password='<REDIS_PASSWORD>'
kubectl create secret generic gateway-secrets -n ecommerce \
  --from-literal=iam-internal-token='<GIONG_AUTH_IAM_INTERNAL_TOKEN>'
```
iam-internal-token phai GIONG nhau giua auth-secrets va gateway-secrets.
De trong keycloak-admin-client-secret = auth register/forgot/change-password tra 501.

## 4. Rollout order (NEVER sed __IMAGE_TAG__ in repo files)

P2 note: khong `sed -i` truc tiep len file repo (lam ban working tree).
Render ra thu muc tam roi apply — CD lam san (xem .github/actions/deploy-service).
Thu cong tren VPS:

```bash
TAG=phase1-$(git rev-parse --short HEAD)
rm -rf /tmp/k8s-render && mkdir -p /tmp/k8s-render && cp -r k8s /tmp/k8s-render/
grep -rl "__IMAGE_TAG__" /tmp/k8s-render --include=deployment.yaml \
  | xargs sed -i "s/__IMAGE_TAG__/$TAG/g"
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/discovery/
kubectl rollout status deployment/discovery-service -n ecommerce
kubectl port-forward svc/discovery-service 8761:8761 -n ecommerce &
curl -fsS http://localhost:8761/actuator/health   # expect {"status":"UP"}

kubectl apply -f k8s/gateway/configmap.yaml k8s/gateway/service.yaml k8s/gateway/deployment.yaml
kubectl rollout status deployment/api-gateway -n ecommerce
curl -fsS http://localhost:8761/eureka/apps | grep -o '<name>[A-Z-]*</name>'  # expect API-GATEWAY

kubectl apply -f k8s/auth/configmap.yaml k8s/auth/service.yaml k8s/auth/deployment.yaml
kubectl rollout status deployment/auth-service -n ecommerce
# if CrashLoopBackOff: kubectl logs deploy/auth-service -n ecommerce (likely Flyway/DB schema)

kubectl apply -f k8s/user/configmap.yaml k8s/user/service.yaml k8s/user/deployment.yaml
kubectl rollout status deployment/user-service -n ecommerce

kubectl apply -f k8s/gateway/ingressroute.yaml
curl -fsS http://162.4.177.91:8080/actuator/health   # via Traefik -> gateway
```

Note: probes use /actuator/health/liveness and /readiness. If a service 404s
those paths, fall back to /actuator/health in its deployment.yaml.

## 5. Rollback SOP

Preferred (old phase1-<sha> tags remain on Docker Hub):

```bash
kubectl rollout undo deployment/<svc> -n ecommerce
kubectl rollout status deployment/<svc> -n ecommerce
```

Fallback (park K8s workloads, restore compose apps):

```bash
kubectl scale deploy --all --replicas=0 -n ecommerce
docker compose --profile apps up -d
```

## 6. Secret inventory (names only, no values)

- K8s ns ecommerce: dockerhub-pull (.dockerconfigjson),
  auth-secrets (db-password, redis-password, iam-internal-token, keycloak-admin-client-secret),
  user-secrets (db-password, redis-password),
  gateway-secrets (iam-internal-token).
- GitHub Secrets (CI): DOCKERHUB_USERNAME, DOCKERHUB_TOKEN,
  VPS_SSH_HOST/PORT/USER/KEY, IAM_INTERNAL_TOKEN, KEYCLOAK_ADMIN_CLIENT_SECRET.
  Tao 2 secret moi nay trong repo Settings > Secrets and variables > Actions
  truoc khi CD auth/gateway — de trong = kenh secret tat (safe default).
- Verify no plaintext: grep -rEi 'password\s*[:=]\s*[^C]' k8s/ must only hit CHANGEME placeholders.

## 7. Monitoring + TLS (P1/P2 note)

- Monitoring toi thieu hien tai = compose: Prometheus (infra/prometheus/prometheus.yml
  + rules/) + Grafana. Chua port sang k8s vi RAM VPS 13/15GB — neu port, dung
  manifests gon (retention 7d, resource requests nho) hoac giu compose hien co.
- Traefik smoke di qua port 80 (entryPoint web), KHONG phai 8080.
- TLS/issuer sau DEV: hien tai issuer-uri = http://162.4.177.91:7080/... (plain HTTP,
  DEV public). Khi siet TLS, doi KEYCLOAK_*_URI sang https + cap nhat ConfigMap
  auth/gateway, rollout restart ca hai.
