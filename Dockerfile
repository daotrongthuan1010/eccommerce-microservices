# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace
COPY pom.xml .
COPY common ./common
COPY common-security ./common-security
COPY common-web ./common-web
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 mvn -B -pl services/${SERVICE} -am package -DskipTests && cp services/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar /tmp/app.jar

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build --chown=app:app /tmp/app.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
