# syntax=docker/dockerfile:1
FROM oven/bun:1.4.5 AS web

WORKDIR /web

COPY host-app-web/package.json host-app-web/bun.lock ./
RUN --mount=type=cache,target=/root/.bun/install/cache bun install --frozen-lockfile

COPY host-app-web/index.html host-app-web/vite.config.js ./
COPY host-app-web/src ./src
RUN bun run build

FROM maven:3.9-eclipse-temurin-21 AS backend

WORKDIR /workspace

COPY host-app-backend/pom.xml ./
COPY host-app-backend/src ./src
RUN --mount=type=cache,id=host-app-maven,target=/root/.m2/repository mvn -B package

FROM eclipse-temurin:21-jre-noble

WORKDIR /app

ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/web/,classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/

COPY --from=backend --chown=10001:10001 /workspace/target/*.jar ./app.jar
COPY --from=web --chown=10001:10001 /web/dist/ ./web/

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
