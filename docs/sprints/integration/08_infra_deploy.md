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

## N. Keycloak realm — hardening para produção

> Esta seção deve ser aplicada **antes** do primeiro deploy em produção. As mudanças no `aegis-realm.json` valem para import inicial; se o Keycloak já estiver rodando, aplicar via Admin Console ou CLI (`kcadm.sh`).

### N.1 — Usuários mock

O realm JSON já está vazio de usuários (`"users": []`). Os usuários de demonstração (`admin@byop.io`, `editor@byop.io` etc.) são criados programaticamente pelo `IdentityDemoUserService` / `DemoSeedRunner`, que a **etapa 30 do backend remove**. Verificar que após o deploy da etapa 30 **nenhum usuário demo existe** no Keycloak de produção:

```bash
# Verificar via kcadm no servidor do Keycloak:
kcadm.sh get users -r aegis --fields username,email,enabled | grep -E "byop\.io|admin@|editor@|dev@"
# Esperado: sem resultados
```

Se algum usuário demo existir (de imports anteriores), remover:
```bash
kcadm.sh delete users/<user-id> -r aegis
```

### N.2 — Tempos de token — padrão de mercado para PMS/SaaS editorial

Para uma aplicação B2B de gestão de conteúdo onde editores trabalham em sessões longas, os valores atuais são desbalanceados: `accessTokenLifespan: 5min` causa refresh constante sem benefício perceptível de segurança, enquanto `ssoSessionMaxLifespan: 10h` é generoso demais sem refresh token rotation.

**Valores recomendados para o perfil do Aegis (PMS editorial, B2B, sessões de trabalho de 4–8h):**

| Campo | Atual | Recomendado | Justificativa |
|---|---|---|---|
| `accessTokenLifespan` | 5 min | **15 min** | Padrão de mercado para SaaS; balance segurança/UX — refresh automático a cada 15 min é imperceptível ao usuário |
| `ssoSessionIdleTimeout` | 30 min | **4 h (14400s)** | Editores ficam 4–6h na mesma sessão; 30 min deslogaria no meio do trabalho |
| `ssoSessionMaxLifespan` | 10 h | **8 h (28800s)** | Uma jornada de trabalho — forçar novo login no dia seguinte |
| `offlineSessionIdleTimeout` | 30 dias | **7 dias (604800s)** | Tokens offline (remember-me) não devem durar mais que uma semana sem uso |
| `offlineSessionMaxLifespan` | 60 dias | **30 dias (2592000s)** | Máximo absoluto de sessão persistida |
| `actionTokenGeneratedByUserLifespan` | 5 min | **15 min (900s)** | Link de confirmação de email — 5 min é muito curto se o usuário demora para abrir o email |
| `refreshTokenMaxReuse` | 0 | **0** | Manter — rotation on every use (mais seguro) |

Aplicar no `aegis-realm.json`:

```json
{
  "accessTokenLifespan": 900,
  "accessTokenLifespanForImplicitFlow": 900,
  "ssoSessionIdleTimeout": 14400,
  "ssoSessionMaxLifespan": 28800,
  "offlineSessionIdleTimeout": 604800,
  "offlineSessionMaxLifespan": 2592000,
  "clientSessionIdleTimeout": 0,
  "clientSessionMaxLifespan": 0,
  "accessCodeLifespan": 60,
  "accessCodeLifespanLogin": 1800,
  "actionTokenGeneratedByUserLifespan": 900,
  "actionTokenGeneratedByAdminLifespan": 43200,
  "refreshTokenMaxReuse": 0
}
```

Via `kcadm.sh` (se Keycloak já estiver rodando):
```bash
kcadm.sh update realms/aegis -r aegis \
  -s accessTokenLifespan=900 \
  -s ssoSessionIdleTimeout=14400 \
  -s ssoSessionMaxLifespan=28800 \
  -s offlineSessionIdleTimeout=604800 \
  -s offlineSessionMaxLifespan=2592000 \
  -s actionTokenGeneratedByUserLifespan=900
```

### N.3 — Brute force protection

`bruteForceProtected: false` e `failureFactor: 30` são configurações de desenvolvimento. Para produção:

```json
{
  "bruteForceProtected": true,
  "permanentLockout": false,
  "maxFailureWaitSeconds": 900,
  "minimumQuickLoginWaitSeconds": 60,
  "waitIncrementSeconds": 60,
  "quickLoginCheckMilliSeconds": 1000,
  "maxDeltaTimeSeconds": 43200,
  "failureFactor": 5
}
```

| Campo | Significado | Valor |
|---|---|---|
| `failureFactor` | Tentativas antes do lockout | **5** (era 30) |
| `maxFailureWaitSeconds` | Tempo de lockout máximo | 900s = 15 min |
| `waitIncrementSeconds` | Incremento de espera por tentativa | 60s |
| `permanentLockout` | Bloquear permanentemente | false (só temporário) |
| `maxDeltaTimeSeconds` | Janela de contagem de falhas | 12h |

### N.4 — Password policy

Sem `passwordPolicy` definida, qualquer senha é aceita no Keycloak. Para produção:

```json
{
  "passwordPolicy": "length(10) and upperCase(1) and lowerCase(1) and digits(1) and notUsername(undefined) and passwordHistory(5)"
}
```

| Regra | Significado |
|---|---|
| `length(10)` | Mínimo 10 caracteres |
| `upperCase(1)` | Ao menos 1 maiúscula |
| `lowerCase(1)` | Ao menos 1 minúscula |
| `digits(1)` | Ao menos 1 dígito |
| `notUsername` | Senha não pode ser igual ao username |
| `passwordHistory(5)` | Não reutilizar as últimas 5 senhas |

Via Admin Console: Realm Settings → Authentication → Password Policy.

### N.5 — Cliente `aegis-web` — desabilitar Direct Access Grants

`directAccessGrantsEnabled: true` no `aegis-web` permite o fluxo Resource Owner Password Credentials (username + password direto na API). Esse fluxo é um risco de segurança para SPAs públicas — não é usado pelo frontend real (que usa Authorization Code + PKCE). Desabilitar:

```json
{
  "clientId": "aegis-web",
  "directAccessGrantsEnabled": false,
  "standardFlowEnabled": true,
  "implicitFlowEnabled": false,
  "publicClient": true
}
```

**Atenção:** verificar que nenhuma chamada do backend (ex.: `AuthService.login()` via proxy BFF) usa o Direct Access Grant. Se usar, manter `true` apenas no cliente `aegis-backend` (confidential client), nunca no `aegis-web` (public client).

### N.6 — SMTP do realm — parametrizar para produção

O `smtpServer` do realm atual tem o mailhog hardcoded e o email pessoal do desenvolvedor (`alexandre.henrique.dev@gmail.com`). Para produção, o Keycloak deve usar o SMTP real do projeto.

Via `kcadm.sh` (aplicar no Keycloak de produção após subir):
```bash
kcadm.sh update realms/aegis \
  -s 'smtpServer.host='${SMTP_HOST} \
  -s 'smtpServer.port='${SMTP_PORT} \
  -s 'smtpServer.from=noreply@byop.dev' \
  -s 'smtpServer.fromDisplayName=Aegis PMS' \
  -s 'smtpServer.replyTo=suporte@byop.dev' \
  -s 'smtpServer.auth=true' \
  -s 'smtpServer.user='${SMTP_USER} \
  -s 'smtpServer.password='${SMTP_PASSWORD} \
  -s 'smtpServer.starttls=true' \
  -s 'smtpServer.ssl=false'
```

**Nunca commitar o `aegis-realm.json` com credenciais SMTP reais.** O realm JSON de produção só deve ser gerado com `kcadm.sh export` após configurar o SMTP via CLI ou Admin Console, e deve ficar fora do repositório.

### N.7 — Script de hardening automatizado

Criar `/infra/keycloak/scripts/harden-realm-prod.sh` para automatizar as seções N.2–N.6:

```bash
#!/usr/bin/env bash
# harden-realm-prod.sh
# Aplica configurações de produção no realm Keycloak.
# Uso: KEYCLOAK_URL=https://auth.byop.dev KEYCLOAK_ADMIN=admin KEYCLOAK_ADMIN_PASSWORD=xxx bash harden-realm-prod.sh
set -euo pipefail

REALM=aegis
KCADM="/opt/keycloak/bin/kcadm.sh"

echo "==> Autenticando no Keycloak..."
$KCADM config credentials \
  --server "$KEYCLOAK_URL" \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD"

echo "==> Aplicando timeouts de token..."
$KCADM update realms/$REALM \
  -s accessTokenLifespan=900 \
  -s ssoSessionIdleTimeout=14400 \
  -s ssoSessionMaxLifespan=28800 \
  -s offlineSessionIdleTimeout=604800 \
  -s offlineSessionMaxLifespan=2592000 \
  -s actionTokenGeneratedByUserLifespan=900

echo "==> Ativando brute force protection..."
$KCADM update realms/$REALM \
  -s bruteForceProtected=true \
  -s failureFactor=5 \
  -s maxFailureWaitSeconds=900 \
  -s waitIncrementSeconds=60 \
  -s permanentLockout=false

echo "==> Aplicando password policy..."
$KCADM update realms/$REALM \
  -s 'passwordPolicy=length(10) and upperCase(1) and lowerCase(1) and digits(1) and notUsername(undefined) and passwordHistory(5)'

echo "==> Desabilitando Direct Access Grants no aegis-web..."
CLIENT_ID=$($KCADM get clients -r $REALM --fields id,clientId | python3 -c "
import json,sys
clients = json.load(sys.stdin)
print(next(c['id'] for c in clients if c['clientId'] == 'aegis-web'))
")
$KCADM update clients/$CLIENT_ID -r $REALM -s directAccessGrantsEnabled=false

echo "==> Configurando SMTP..."
$KCADM update realms/$REALM \
  -s "smtpServer.host=$SMTP_HOST" \
  -s "smtpServer.port=$SMTP_PORT" \
  -s "smtpServer.from=noreply@byop.dev" \
  -s "smtpServer.fromDisplayName=Aegis PMS" \
  -s "smtpServer.replyTo=suporte@byop.dev" \
  -s "smtpServer.auth=true" \
  -s "smtpServer.user=$SMTP_USER" \
  -s "smtpServer.password=$SMTP_PASSWORD" \
  -s "smtpServer.starttls=true" \
  -s "smtpServer.ssl=false"

echo "==> Hardening concluido com sucesso."
```

Adicionar ao pipeline de deploy (`.github/workflows/ci.yml`, job `deploy`, após health check):

```yaml
- name: Hardening do Keycloak
  run: |
    KEYCLOAK_URL="${{ secrets.KEYCLOAK_URL }}" \
    KEYCLOAK_ADMIN="${{ secrets.KEYCLOAK_ADMIN }}" \
    KEYCLOAK_ADMIN_PASSWORD="${{ secrets.KEYCLOAK_ADMIN_PASSWORD }}" \
    SMTP_HOST="${{ secrets.SMTP_HOST }}" \
    SMTP_PORT="${{ secrets.SMTP_PORT }}" \
    SMTP_USER="${{ secrets.SMTP_USER }}" \
    SMTP_PASSWORD="${{ secrets.SMTP_PASSWORD }}" \
    bash infra/keycloak/scripts/harden-realm-prod.sh
  # Idempotente: pode rodar em todo deploy sem efeito colateral
```

---

## L. Critérios de aceite

**Infra / Docker:**
- [ ] `infra/docker-compose.prod.yml` existe e sobe o backend sem Postgres ou Keycloak locais.
- [ ] `infra/Caddyfile` existe e Caddy responde HTTPS em `aegis.byop.dev` com certificado válido.
- [ ] `infra/.env.prod.example` commitado; `infra/.env.prod` (com segredos reais) está no `.gitignore`.
- [ ] `application-prod.yml` existe; Swagger desabilitado em produção.
- [ ] CORS parametrizado via `${aegis.app.cors-allowed-origins}` — prod permite `https://aegis.byop.dev`.

**CI/CD:**
- [ ] `.github/workflows/ci.yml` existe e passa no GitHub Actions (runner `genesis-lab`).
- [ ] Pipeline bloqueia deploy se `npm run typecheck` falhar.
- [ ] Pipeline bloqueia deploy se qualquer teste backend falhar.
- [ ] Pipeline bloqueia deploy se Bruno tests falharem.
- [ ] `backend/Dockerfile` existe e produz imagem funcional.
- [ ] `curl https://aegis.byop.dev/actuator/health` retorna `{"status":"UP"}` após o deploy.
- [ ] `curl https://aegis.byop.dev/` carrega a SPA React (index.html servido pelo Spring Boot).

**Keycloak — hardening:**
- [ ] Nenhum usuário demo existe no Keycloak de produção após o deploy da etapa 30.
  ```bash
  kcadm.sh get users -r aegis | grep -E "byop\.io|demo" | wc -l  # deve retornar 0
  ```
- [ ] `accessTokenLifespan` é **900s (15 min)** — não mais 300s.
- [ ] `ssoSessionIdleTimeout` é **14400s (4h)** — não mais 1800s.
- [ ] `ssoSessionMaxLifespan` é **28800s (8h)** — não mais 36000s.
- [ ] `bruteForceProtected: true` e `failureFactor: 5` — não mais `false` e `30`.
- [ ] `passwordPolicy` definida com `length(10) and upperCase(1) and lowerCase(1) and digits(1)`.
- [ ] `aegis-web.directAccessGrantsEnabled: false` — Direct Access Grant desabilitado no cliente público.
- [ ] SMTP do realm aponta para o provedor real (`byop.dev`), não para mailhog ou email pessoal.
- [ ] `infra/keycloak/scripts/harden-realm-prod.sh` existe, é executável e idempotente.
- [ ] Keycloak `aegis-web` tem `https://aegis.byop.dev/*` como redirect URI permitida.

---

## M. Commit sugerido

```bash
git add infra/ .github/ backend/src/main/resources/application-prod.yml backend/Dockerfile
git commit -m "feat(infra): docker-compose.prod, Caddyfile, GitHub Actions CI/CD, profile prod e hardening Keycloak"
```
