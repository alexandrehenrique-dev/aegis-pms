FROM node:22-alpine AS frontend-build

WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend ./

ARG VITE_API_MODE=api
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_KEYCLOAK_URL=https://auth.buildyourownpath.io
ARG VITE_KEYCLOAK_REALM=aegis-pms
ARG VITE_KEYCLOAK_CLIENT_ID=aegis-web

ENV VITE_API_MODE=${VITE_API_MODE}
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_KEYCLOAK_URL=${VITE_KEYCLOAK_URL}
ENV VITE_KEYCLOAK_REALM=${VITE_KEYCLOAK_REALM}
ENV VITE_KEYCLOAK_CLIENT_ID=${VITE_KEYCLOAK_CLIENT_ID}

RUN npm run build
RUN ! grep -R --fixed-strings "http://localhost:8080/api/v1" dist

FROM maven:3.9-eclipse-temurin-25 AS backend-build

WORKDIR /workspace/backend

COPY backend/pom.xml .
RUN mvn -B dependency:go-offline

COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static

RUN test -f src/main/resources/static/index.html
RUN mvn -B -DskipTests package spring-boot:repackage

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd --system aegis \
    && useradd --system --gid aegis --home-dir /app --shell /usr/sbin/nologin aegis \
    && mkdir -p /data/assets /app/email-templates \
    && chown -R aegis:aegis /app /data/assets \
    && rm -rf /var/lib/apt/lists/*

COPY --from=backend-build --chown=aegis:aegis /workspace/backend/target/aegis-pms-backend-0.0.1-SNAPSHOT.jar /app/aegis-pms-backend.jar

VOLUME ["/data/assets", "/app/email-templates"]

EXPOSE 8080

USER aegis

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/aegis-pms-backend.jar"]
