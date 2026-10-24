# syntax=docker/dockerfile:1
# NOTE: dung mirror nha daotrongthuan/* thay vi library/* public de tranh
# Docker Hub 429 (14 job CI cung HEAD 1 manifest tu IP shared runner).
# Mirror 1 lan bang lenh o ghi chu cuoi file; tag giu nguyen version goc.
FROM daotrongthuan/maven-builder:3.9.11-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace

# POM-first for dependency-layer caching: copy root + module POMs only,
# warm the local repo, then copy sources. Adding a new service POM
# invalidates this layer (rare) — acceptable vs per-service Dockerfile drift.
# NOTE: plain `COPY services/*/pom.xml services/` would flatten paths, so copy each.
COPY pom.xml .
COPY common/pom.xml ./common/pom.xml
COPY common-security/pom.xml ./common-security/pom.xml
COPY common-web/pom.xml ./common-web/pom.xml
COPY services/discovery-service/pom.xml ./services/discovery-service/pom.xml
COPY services/api-gateway/pom.xml ./services/api-gateway/pom.xml
COPY services/auth-service/pom.xml ./services/auth-service/pom.xml
COPY services/user-service/pom.xml ./services/user-service/pom.xml
COPY services/catalog-service/pom.xml ./services/catalog-service/pom.xml
COPY services/inventory-service/pom.xml ./services/inventory-service/pom.xml
COPY services/order-service/pom.xml ./services/order-service/pom.xml
COPY services/payment-service/pom.xml ./services/payment-service/pom.xml
COPY services/notification-service/pom.xml ./services/notification-service/pom.xml
COPY services/search-service/pom.xml ./services/search-service/pom.xml
COPY services/product-service/pom.xml ./services/product-service/pom.xml
COPY services/review-service/pom.xml ./services/review-service/pom.xml
COPY services/analytics-service/pom.xml ./services/analytics-service/pom.xml
COPY services/file-service/pom.xml ./services/file-service/pom.xml
# Serial on purpose: -T breaks some phase-bound plugins during go-offline.
# Scope limited to target service + its deps (-am) to keep the layer small.
# Best-effort warmup only: go-offline is known-incomplete for phase-bound
# plugins (protobuf/grpc) and some transitives, so the package step below
# stays online and resolves stragglers through the same cache mount.
RUN --mount=type=cache,target=/root/.m2 mvn -B -pl services/${SERVICE} -am dependency:go-offline

COPY common ./common
COPY common-security ./common-security
COPY common-web ./common-web
COPY services ./services
# NOTE: intentionally ONLINE (no -o). dependency:go-offline misses artifacts
# that only resolve during the real lifecycle — verified: auth-service's
# offline package failed on org.checkerframework:checker-qual:jar:3.37.0
# (transitive via grpc/protobuf). The BuildKit /root/.m2 cache mount keeps
# this fast after the first build and persists the stragglers it downloads.
RUN --mount=type=cache,target=/root/.m2 mvn -B -T1C -pl services/${SERVICE} -am package -DskipTests && cp services/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar /tmp/app.jar
RUN java -Djarmode=layertools -jar /tmp/app.jar extract --destination /tmp/layers

FROM daotrongthuan/eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build --chown=app:app /tmp/layers/dependencies/ ./
COPY --from=build --chown=app:app /tmp/layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /tmp/layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /tmp/layers/application/ ./
USER app
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
