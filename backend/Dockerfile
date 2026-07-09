FROM node:22-alpine AS frontend-build

WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

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
