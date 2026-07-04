# Sprint de Integração 08 — Infra, Deploy e CI/CD

> **Pode correr em paralelo com Sprints 04–07.**
>
> **Contexto:**
> - Servidor: genesis-lab (`198.162.0.77`), domínio `byop.dev`, Aegis em `aegis.byop.dev`
> - Banco e Keycloak externos: máquina Oracle (ainda não comprada — usar variáveis de ambiente parametrizadas)
> - CI/CD: GitHub Actions com self-hosted runner no genesis-lab
> - Reverse proxy: Caddy com HTTPS automático (Let's Encrypt)
> - O `docker-compose.yml` local permanece **intocado**
> - Frontend é buildado pelo Vite e servido como estático pelo Spring Boot (mesma origem — sem CORS em produção)
>
> **Branch:** `integration/08-infra-deploy`

---

## A. `docker-compose.prod.yml` — compose de produção

O compose de produção **não inclui** Postgres nem Keycloak — esses serviços rodam na máquina Oracle externa. O compose de prod só sobe o backend (que serve também o frontend estático).

Criar `/infra/docker-compose.prod.yml`:

```yaml
# docker-compose.prod.yml
# Uso: docker compose -f infra/docker-compose.prod.yml up -d
# Banco e Keycloak são externos (máquina Oracle) — configurados via variáveis de ambiente.
# NÃO editar docker-compose.yml (dev local) — este arquivo é exclusivo para produção.

services:
  aegis-backend:
    image: aegis-pms:${IMAGE_TAG:-latest}
    container_name: aegis-pms-prod
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod

      # Banco externo (Oracle machine)
      AEGIS_DB_HOST: ${AEGIS_DB_HOST}
      AEGIS_DB_PORT: ${AEGIS_DB_PORT:-5432}
      AEGIS_DB_NAME: ${AEGIS_DB_NAME}
      AEGIS_DB_USER: ${AEGIS_DB_USER}
      AEGIS_DB_PASSWORD: ${AEGIS_DB_PASSWORD}

      # Keycloak externo
      KEYCLOAK_ISSUER_URI: ${KEYCLOAK_ISSUER_URI}
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: ${KEYCLOAK_JWK_SET_URI}
      KEYCLOAK_INTERNAL_BASE_URL: ${KEYCLOAK_INTERNAL_BASE_URL}
      KEYCLOAK_REALM: ${KEYCLOAK_REALM:-aegis}
      KEYCLOAK_WEB_CLIENT_ID: ${KEYCLOAK_WEB_CLIENT_ID:-aegis-web}
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KEYCLOAK_ADMIN_REALMS_PATH: ${KEYCLOAK_ADMIN_REALMS_PATH:-/admin/realms/}

      # App
      AEGIS_APP_BASE_URL: https://aegis.byop.dev
      AEGIS_STORAGE_LOCAL_PATH: /data/assets

      # SMTP real
      SMTP_HOST: ${SMTP_HOST}
      SMTP_PORT: ${SMTP_PORT:-587}
      SMTP_FROM: ${SMTP_FROM:-noreply@byop.dev}
      SMTP_FROM_DISPLAY_NAME: ${SMTP_FROM_DISPLAY_NAME:-Aegis PMS}
      SMTP_AUTH: ${SMTP_AUTH:-true}
      SMTP_USER: ${SMTP_USER}
      SMTP_PASSWORD: ${SMTP_PASSWORD}
      SMTP_STARTTLS: ${SMTP_STARTTLS:-true}
      SMTP_SSL: ${SMTP_SSL:-false}

      # Multipart
      AEGIS_ASSETS_MULTIPART_MAX_FILE_SIZE: ${AEGIS_ASSETS_MULTIPART_MAX_FILE_SIZE:-260MB}
      AEGIS_ASSETS_MULTIPART_MAX_REQUEST_SIZE: ${AEGIS_ASSETS_MULTIPART_MAX_REQUEST_SIZE:-260MB}

      # Telegram (opcional)
      AEGIS_TELEGRAM_BOT_TOKEN: ${AEGIS_TELEGRAM_BOT_TOKEN:-}
      AEGIS_TELEGRAM_CHAT_ID: ${AEGIS_TELEGRAM_CHAT_ID:-}

      AEGIS_PRODUCT_EMAIL_TEMPLATE_PATH: /app/email-templates/product
    ports:
      - "127.0.0.1:8080:8080"   # só loopback — Caddy faz o proxy público
    volumes:
      - aegis_assets_data:/data/assets
      - ./infra/keycloak/themes/aegis/email/html:/app/email-templates/product:ro
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  aegis_assets_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /opt/aegis/assets
```

**Criar `/opt/aegis/assets` no genesis-lab antes do primeiro deploy:**
```bash
sudo mkdir -p /opt/aegis/assets && sudo chown $(whoami):$(whoami) /opt/aegis/assets
```

---

## B. Arquivo de variáveis de produção — `/infra/.env.prod`

**Não commitar. Criar manualmente no genesis-lab.** Adicionar ao `.gitignore`:

```bash
# .gitignore — adicionar:
infra/.env.prod
```

Template (`/infra/.env.prod.example` — este sim commitado):

```env
# Imagem Docker
IMAGE_TAG=latest

# Banco externo (Oracle machine — IP a ser preenchido quando comprado)
AEGIS_DB_HOST=<IP_ORACLE>
AEGIS_DB_PORT=5432
AEGIS_DB_NAME=aegis_db
AEGIS_DB_USER=aegis_user
AEGIS_DB_PASSWORD=<SEGREDO>

# Keycloak externo
KEYCLOAK_ISSUER_URI=https://auth.byop.dev/realms/aegis
KEYCLOAK_JWK_SET_URI=https://auth.byop.dev/realms/aegis/protocol/openid-connect/certs
KEYCLOAK_INTERNAL_BASE_URL=http://<IP_ORACLE>:8282
KEYCLOAK_REALM=aegis
KEYCLOAK_WEB_CLIENT_ID=aegis-web
KEYCLOAK_ADMIN=<SEGREDO>
KEYCLOAK_ADMIN_PASSWORD=<SEGREDO>

# SMTP real
SMTP_HOST=<PROVIDER>.example.com
SMTP_PORT=587
SMTP_FROM=noreply@byop.dev
SMTP_AUTH=true
SMTP_USER=<SEGREDO>
SMTP_PASSWORD=<SEGREDO>
SMTP_STARTTLS=true
SMTP_SSL=false

# Telegram global do Aegis (opcional)
AEGIS_TELEGRAM_BOT_TOKEN=
AEGIS_TELEGRAM_CHAT_ID=
```

---

## C. `Caddyfile` — HTTPS automático para `aegis.byop.dev`

Criar `/infra/Caddyfile`:

```caddy
aegis.byop.dev {
    # HTTPS automático via Let's Encrypt (Caddy gerencia os certificados)
    # Pré-requisito: porta 80 e 443 abertas no firewall do genesis-lab

    reverse_proxy localhost:8080

    # Compressão
    encode gzip

    # Logs
    log {
        output file /var/log/caddy/aegis-access.log {
            roll_size 100mb
            roll_keep 5
        }
    }
}
```

Instalar Caddy no genesis-lab:
```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install caddy -y

# Copiar Caddyfile e recarregar:
sudo cp /home/$USER/aegis-pms/infra/Caddyfile /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

---

## D. Backend — CORS parametrizado para produção

`SecurityConfig.java` hardcoda `http://localhost:5173` como allowed origin. Em produção, o frontend é servido pelo próprio Spring Boot (mesma origem que a API), então **CORS não é necessário em produção**. Mas o config atual bloqueia `aegis.byop.dev` de chamar a API se por acaso o frontend for servido separadamente.

Parametrizar:

```java
// SecurityConfig.java
@Value("${aegis.app.cors-allowed-origins:http://localhost:5173}")
private List<String> corsAllowedOrigins;

// ...no bean:
configuration.setAllowedOrigins(corsAllowedOrigins);
```

Em `application.yml`:
```yaml
aegis:
  app:
    cors-allowed-origins:
      - "http://localhost:5173"
```

Em `application-prod.yml` (criar novo arquivo):
```yaml
spring:
  config:
    activate:
      on-profile: prod

aegis:
  app:
    cors-allowed-origins:
      - "https://aegis.byop.dev"
```

---

## E. Keycloak realm — adicionar redirect URIs de produção

`/infra/keycloak/realm/aegis-realm.json`, cliente `aegis-web`:

```json
{
  "clientId": "aegis-web",
  "redirectUris": [
    "http://localhost:8080/*",
    "http://localhost:8080/swagger-ui/oauth2-redirect.html",
    "http://localhost:5173/*",
    "https://aegis.byop.dev/*",
    "https://aegis.byop.dev/swagger-ui/oauth2-redirect.html"
  ],
  "webOrigins": [
    "http://localhost:8080",
    "http://localhost:5173",
    "https://aegis.byop.dev"
  ]
}
```

**Atenção:** este realm JSON serve para import inicial. Quando o Keycloak de produção estiver rodando, aplicar via Admin Console ou CLI. Se o Keycloak de prod for importado a partir deste JSON, a configuração já estará correta.

---

## F. `application-prod.yml` — profile de produção

Criar `backend/src/main/resources/application-prod.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: prod

# Swagger desabilitado em produção
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

# Brute force protection explícita (não depender do padrão do Keycloak)
# Configurar no Keycloak Admin, não aqui — apenas comentário de lembrete.

aegis:
  app:
    base-url: https://aegis.byop.dev
    cors-allowed-origins:
      - "https://aegis.byop.dev"
  seed:
    demo-user-initial-credential: ""    # garantir que não existe credencial demo em prod
```

---

## G. GitHub Actions — CI/CD pipeline

Criar `.github/workflows/ci.yml`:

```yaml
name: CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: localhost:5000   # registry local no genesis-lab (self-hosted runner)
  IMAGE_NAME: aegis-pms

jobs:
  # ─── 1. Frontend ──────────────────────────────────────────────────────────
  frontend-check:
    name: Frontend — typecheck + lint
    runs-on: [self-hosted, genesis-lab]
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Typecheck
        run: npm run typecheck

      - name: Lint
        run: npm run lint

  # ─── 2. Build frontend + copiar para backend static ───────────────────────
  frontend-build:
    name: Frontend — build prod
    runs-on: [self-hosted, genesis-lab]
    needs: frontend-check
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Build
        env:
          VITE_API_MODE: api
        run: npm run build
        # O vite.config.ts deve ter build.outDir: '../backend/src/main/resources/static'
        # para o build já ir direto para onde o Spring Boot serve estáticos.

      - name: Upload static build artifact
        uses: actions/upload-artifact@v4
        with:
          name: frontend-static
          path: backend/src/main/resources/static/
          retention-days: 1

  # ─── 3. Backend — tests + JaCoCo ──────────────────────────────────────────
  backend-test:
    name: Backend — testes + JaCoCo
    runs-on: [self-hosted, genesis-lab]
    needs: frontend-check   # não precisa esperar o build do frontend
    defaults:
      run:
        working-directory: backend
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: aegis_test
          POSTGRES_USER: aegis_test
          POSTGRES_PASSWORD: aegis_test
        ports:
          - 5499:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run tests with JaCoCo
        run: |
          mvn -B test \
            -Dspring.datasource.url=jdbc:postgresql://localhost:5499/aegis_test \
            -Dspring.datasource.username=aegis_test \
            -Dspring.datasource.password=aegis_test \
            -Dspring.profiles.active=test \
            -Dkeycloak.issuer-uri=http://mock-keycloak/realms/test
        # O profile "test" deve desabilitar a validação JWT real (usar mock ou issuer-uri fake)

      - name: Upload JaCoCo report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-report
          path: backend/target/site/jacoco/
          retention-days: 7

  # ─── 4. Bruno collection — smoke tests ────────────────────────────────────
  bruno-tests:
    name: Bruno — smoke tests
    runs-on: [self-hosted, genesis-lab]
    needs: [frontend-build, backend-test]
    steps:
      - uses: actions/checkout@v4

      - name: Download frontend static build
        uses: actions/download-artifact@v4
        with:
          name: frontend-static
          path: backend/src/main/resources/static/

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Start backend (test profile with mock Keycloak)
        working-directory: backend
        run: |
          mvn -B spring-boot:run \
            -Dspring-boot.run.profiles=local \
            -Dspring-boot.run.jvmArguments="-Dserver.port=8099" &
          echo "Aguardando backend subir..."
          for i in $(seq 1 30); do
            curl -sf http://localhost:8099/actuator/health && break
            sleep 2
          done

      - name: Run Bruno collection
        run: |
          npm install -g @usebruno/cli
          bru run bruno/ --env local --reporter junit --output bruno-results.xml

      - name: Upload Bruno results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: bruno-results
          path: bruno-results.xml

  # ─── 5. Build Docker image + Deploy ───────────────────────────────────────
  deploy:
    name: Build Docker + Deploy
    runs-on: [self-hosted, genesis-lab]
    needs: [frontend-build, backend-test, bruno-tests]
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4

      - name: Download frontend static build
        uses: actions/download-artifact@v4
        with:
          name: frontend-static
          path: backend/src/main/resources/static/

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build JAR
        working-directory: backend
        run: mvn -B package -DskipTests

      - name: Build Docker image
        run: |
          docker build -t ${{ env.IMAGE_NAME }}:${{ github.sha }} -f backend/Dockerfile backend/
          docker tag ${{ env.IMAGE_NAME }}:${{ github.sha }} ${{ env.IMAGE_NAME }}:latest

      - name: Deploy
        run: |
          cd /opt/aegis
          # Parar container atual, subir novo
          docker compose -f infra/docker-compose.prod.yml \
            --env-file infra/.env.prod \
            up -d --force-recreate aegis-backend

      - name: Health check pós-deploy
        run: |
          for i in $(seq 1 15); do
            curl -sf https://aegis.byop.dev/actuator/health && echo "Deploy OK" && exit 0
            sleep 5
          done
          echo "FALHA: backend não respondeu após o deploy" && exit 1
```

---

## H. `Dockerfile` para o backend

Se não existe `backend/Dockerfile`, criar:

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# JAR gerado pelo Maven
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Assets e templates como volumes — não no layer da imagem
VOLUME ["/data/assets", "/app/email-templates"]

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

---

## I. Self-hosted runner — setup no genesis-lab

```bash
# No genesis-lab, uma vez:
mkdir -p /opt/aegis/runner && cd /opt/aegis/runner

# Baixar runner (substituir VERSION pela versão atual em github.com/actions/runner/releases)
curl -o actions-runner-linux-x64.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.319.1/actions-runner-linux-x64-2.319.1.tar.gz
tar xzf ./actions-runner-linux-x64.tar.gz

# Registrar runner (token gerado em: GitHub > repo > Settings > Actions > Runners > New runner)
./config.sh \
  --url https://github.com/alexandresilva/aegis-pms \
  --token <TOKEN_DO_GITHUB> \
  --name genesis-lab \
  --labels genesis-lab \
  --unattended

# Instalar como serviço systemd
sudo ./svc.sh install
sudo ./svc.sh start
```

O runner fica em `/opt/aegis/runner`. Ele tem acesso ao `docker` do host — garantir que o usuário do runner está no grupo `docker`:
```bash
sudo usermod -aG docker $(whoami)
```

---

## J. Firewall — genesis-lab

```bash
# UFW
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # HTTP (Let's Encrypt challenge + redirect)
sudo ufw allow 443/tcp    # HTTPS (Caddy)
sudo ufw deny 8080/tcp    # Backend não exposto diretamente — Caddy faz o proxy
sudo ufw enable
```

---

## K. Spring Boot profile `test` — desabilitar validação JWT real nos testes de CI

Criar `backend/src/test/resources/application-test.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: test

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Usar public key fake para testes — não valida contra Keycloak real
          issuer-uri: http://mock-keycloak/realms/test
          # Alternativa: usar spring-security-test com @WithMockUser
```

Ou, preferível: configurar o `SecurityConfig` para aceitar `@WithMockUser` em testes via `@SpringBootTest` com `SecurityMockMvcConfigurer`. A solução exata depende da estrutura dos testes existentes no backend.

---

## L. Critérios de aceite

- [ ] `infra/docker-compose.prod.yml` existe e sobe o backend sem Postgres ou Keycloak locais.
- [ ] `infra/Caddyfile` existe e Caddy responde HTTPS em `aegis.byop.dev` com certificado válido.
- [ ] `infra/.env.prod.example` commitado; `infra/.env.prod` (com segredos reais) está no `.gitignore`.
- [ ] `application-prod.yml` existe; Swagger desabilitado em produção.
- [ ] CORS parametrizado via `${aegis.app.cors-allowed-origins}` — prod permite `https://aegis.byop.dev`.
- [ ] Keycloak `aegis-web` tem `https://aegis.byop.dev/*` como redirect URI permitida.
- [ ] `.github/workflows/ci.yml` existe e passa no GitHub Actions (runner `genesis-lab`).
- [ ] Pipeline bloqueia deploy se `npm run typecheck` falhar.
- [ ] Pipeline bloqueia deploy se qualquer teste backend falhar.
- [ ] Pipeline bloqueia deploy se Bruno tests falharem.
- [ ] `backend/Dockerfile` existe e produz imagem funcional.
- [ ] `curl https://aegis.byop.dev/actuator/health` retorna `{"status":"UP"}` após o deploy.
- [ ] `curl https://aegis.byop.dev/` carrega a SPA React (index.html servido pelo Spring Boot).

---

## M. Commit sugerido

```bash
git add infra/ .github/ backend/src/main/resources/application-prod.yml backend/Dockerfile
git commit -m "feat(infra): docker-compose.prod, Caddyfile, GitHub Actions CI/CD e profile prod"
```
