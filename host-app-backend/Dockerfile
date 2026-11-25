# syntax=docker/dockerfile:1
FROM oven/bun:1 AS web

WORKDIR /web

COPY host-app-web/package.json host-app-web/bun.lock ./
RUN --mount=type=cache,target=/root/.bun/install/cache bun install --frozen-lockfile

COPY host-app-web/index.html host-app-web/vite.config.js ./
COPY host-app-web/src ./src
RUN bun run build

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY host-app-backend/pom.xml ./
COPY host-app-backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static

RUN --mount=type=cache,id=host-app-maven,target=/root/.m2/repository mvn -B package

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build --chown=10001:10001 /workspace/target/*.jar ./app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
