
# 🛡️ AEGIS PMS — ROTEIRO DE IMPLEMENTAÇÃO DA FUNDAÇÃO OPERACIONAL

> Artefato: `001_aegis_pms_roteiro_fundacao_operacional.md`  
> Projeto: **Aegis PMS — Product Management System**  
> Responsável arquitetural: **Aegis**  
> Ecossistema: **BYOP / Genesis**  
> Estratégia: implementação por features incrementais, validáveis e rastreáveis  
> Objetivo: sair de zero até um servidor funcional com Docker Compose, PostgreSQL persistente, Keycloak persistente, backend Spring Boot modular, Angular servido pelo backend e MVP funcional de Knowledge Graph.

---

# 1. PROPÓSITO DESTE DOCUMENTO

Este documento define o roteiro operacional para construir a fundação do **Aegis PMS**.

Ele deve ser usado como fonte de verdade para chats/agentes/workers que irão implementar features uma a uma.

O objetivo não é apenas “subir uma aplicação”.

O objetivo é criar uma base operacional confiável onde:

- Keycloak não perca usuários a cada deploy;
- PostgreSQL não perca dados a cada deploy;
- backend Spring Boot rode com arquitetura modular;
- frontend Angular mais atual seja servido pelo backend;
- a aplicação já nasça com autenticação real;
- o Aegis já tenha multi-tenancy básico;
- o Aegis já tenha Product First como eixo central;
- o Aegis já tenha Contract First como diretriz;
- o Aegis já tenha um Knowledge Graph funcional em MVP;
- cada etapa seja validável antes da próxima.

---

# 2. ESCOPO DESTE ROTEIRO

Este roteiro cobre a fundação técnica inicial.

Inclui:

- estrutura de repositório;
- Docker Compose;
- PostgreSQL da aplicação;
- PostgreSQL dedicado do Keycloak;
- Keycloak persistente;
- Spring Boot;
- Flyway;
- Spring Security;
- OAuth2 Resource Server;
- Keycloak integration;
- Angular SPA;
- serving do Angular pelo Spring Boot;
- módulos base do Aegis;
- Knowledge Graph MVP;
- health checks;
- validação incremental;
- critérios de aceite;
- prompts para agentes por feature.

Não cobre ainda:

- todos os CRUDs completos de todos os domínios;
- UX final;
- design system completo;
- integração com todos os consumers;
- deploy cloud definitivo;
- observabilidade avançada;
- CI/CD completo;
- backup production-grade;
- cluster Kubernetes;
- multi-região;
- GraphQL.

---

# 3. DECISÕES OBRIGATÓRIAS

## 3.1 Stack oficial

Backend:

- Java 21+
- Spring Boot 3.x
- Spring Security
- OAuth2 Resource Server
- Spring Data JPA
- Flyway
- PostgreSQL
- Spring Modulith
- Bean Validation
- Springdoc OpenAPI
- Actuator

Frontend:

- Angular mais atual disponível no momento do desenvolvimento
- TypeScript
- SPA buildada e servida pelo backend no MVP
- eirene-ui como direção visual futura

Auth:

- Keycloak
- Realm dedicado do Aegis
- Banco PostgreSQL dedicado para Keycloak
- Client público para frontend
- Client confidencial apenas se necessário no futuro

Infra local:

- Docker Compose
- volumes nomeados persistentes
- rede interna dedicada
- health checks
- `.env` local

Banco:

- PostgreSQL dedicado para Aegis PMS
- PostgreSQL dedicado para Keycloak
- schemas/migrations controlados por Flyway
- nada de H2 para fluxo principal

Knowledge Graph MVP:

- deve existir já na primeira versão
- pode começar em PostgreSQL como grafo lógico relacional
- deve ser modelado para permitir migração futura para Neo4j, Memgraph ou similar
- não usar Neo4j obrigatório no primeiro corte, salvo se o construtor decidir aceitar o custo operacional

---

# 4. DIRETRIZES ARQUITETURAIS

## 4.1 Product First

O produto é a raiz operacional.

Tudo orbita:

```txt
Tenant
↓
Product
↓
Modules / Features / Contracts / Content / Graph
```

Aegis não é mais apenas CMS.

Aegis é um **PMS — Product Management System** para administrar produtos digitais do ecossistema BYOP.

## 4.2 Contract First

Todo sistema alimentado pelo Aegis deve receber contratos claros.

Os frontends externos não devem depender da estrutura interna do banco.

Eles consomem:

- contratos JSON;
- endpoints públicos;
- conteúdo publicado;
- configurações de produto;
- dados autorizados.

## 4.3 Modular Monolith

Não criar microsserviços agora.

Criar um monólito modular forte:

```txt
core
identity
tenant
product
contract
content
asset
form
submission
seo
analytics
knowledgegraph
integration
```

Cada módulo deve ter fronteira clara.

## 4.4 Backend como fonte da verdade

O backend decide:

- usuário autenticado;
- tenant ativo;
- autorização;
- ownership;
- status;
- publicação;
- versionamento;
- vínculos do grafo;
- validação de contratos;
- auditoria.

O frontend administra e apresenta.

Não decide regra crítica.

---

# 5. ESTRUTURA SUGERIDA DO REPOSITÓRIO

```txt
aegis-pms/
 ├── docs/
 │    ├── architecture/
 │    ├── adr/
 │    ├── contracts/
 │    └── implementation/
 ├── infra/
 │    ├── docker/
 │    └── keycloak/
 ├── backend/
 │    ├── src/
 │    ├── pom.xml
 │    └── Dockerfile
 ├── frontend/
 │    ├── src/
 │    ├── angular.json
 │    ├── package.json
 │    └── Dockerfile
 ├── scripts/
 ├── docker-compose.yml
 ├── .env.example
 └── README.md
```

---

# 6. MODELO DE EXECUÇÃO POR FEATURES

Cada etapa deste documento deve ser tratada como uma feature de Jira/Trello.

Cada feature possui:

- ID;
- título;
- objetivo;
- contexto;
- tarefas;
- arquivos esperados;
- critérios de aceite;
- validação manual;
- validação técnica;
- comando de teste;
- definição de pronto;
- prompt para agente.

Regra:

> Nenhuma feature deve ser marcada como concluída sem evidência de validação.

---

# 7. FEATURE 001 — CRIAR ESTRUTURA BASE DO REPOSITÓRIO

## Objetivo

Criar a estrutura inicial do projeto Aegis PMS.

## Contexto

Antes de escrever backend, frontend ou Docker, o repositório precisa ter uma estrutura clara.

## Tarefas

- Criar diretórios principais:
  - `docs/`
  - `infra/`
  - `backend/`
  - `frontend/`
  - `scripts/`
- Criar `README.md` raiz.
- Criar `.gitignore`.
- Criar `.env.example`.
- Criar `docs/implementation/`.
- Criar este roteiro dentro de `docs/implementation/`.

## Arquivos esperados

```txt
README.md
.gitignore
.env.example
docs/implementation/001_aegis_pms_roteiro_fundacao_operacional.md
```

## Critérios de aceite

- Estrutura criada.
- `.gitignore` ignora:
  - `node_modules`
  - `target`
  - `.env`
  - `.idea`
  - `.vscode`
  - `.DS_Store`
  - logs
  - arquivos temporários
- `.env.example` existe e não contém segredo real.
- README informa o propósito do projeto.

## Validação

Comando:

```bash
tree -L 3
```

Resultado esperado:

```txt
aegis-pms/
 ├── backend/
 ├── docs/
 ├── frontend/
 ├── infra/
 ├── scripts/
 ├── README.md
 ├── .gitignore
 └── .env.example
```

## Definição de pronto

Feature concluída quando a estrutura estiver criada, versionável e limpa.

## Prompt para agente

```md
Você é um engenheiro de software organizando a fundação de um novo produto chamado Aegis PMS.

Crie a estrutura inicial do repositório com diretórios para backend, frontend, infra, docs e scripts.

Requisitos:
- Criar README.md inicial.
- Criar .gitignore adequado para Java, Angular, Docker e macOS.
- Criar .env.example sem segredos reais.
- Criar docs/implementation.
- Não criar código de backend ainda.
- Não criar código de frontend ainda.

Entregue:
1. Estrutura de pastas.
2. Conteúdo sugerido do README.md.
3. Conteúdo do .gitignore.
4. Conteúdo do .env.example.
5. Checklist de validação.
```

---

# 8. FEATURE 002 — DEFINIR VARIÁVEIS DE AMBIENTE BASE

## Objetivo

Definir variáveis de ambiente necessárias para infraestrutura local.

## Tarefas

Atualizar `.env.example` com:

```env
# Project
COMPOSE_PROJECT_NAME=aegis-pms

# Aegis PostgreSQL
AEGIS_DB_NAME=aegis_pms
AEGIS_DB_USER=aegis_user
AEGIS_DB_PASSWORD=aegis_password
AEGIS_DB_PORT=5432

# Keycloak PostgreSQL
KEYCLOAK_DB_NAME=keycloak
KEYCLOAK_DB_USER=keycloak_user
KEYCLOAK_DB_PASSWORD=keycloak_password
KEYCLOAK_DB_PORT=5433

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_HTTP_PORT=8181
KEYCLOAK_REALM=aegis
KEYCLOAK_WEB_CLIENT_ID=aegis-web

# Backend
BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=local
KEYCLOAK_ISSUER_URI=http://localhost:8181/realms/aegis

# Frontend
FRONTEND_DEV_PORT=4200

# Storage
AEGIS_STORAGE_LOCAL_PATH=./data/storage

# Timezone
TZ=America/Sao_Paulo
```

## Critérios de aceite

- `.env.example` completo.
- `.env` local criado pelo desenvolvedor a partir do exemplo.
- Nenhuma senha real versionada.

## Validação

```bash
cp .env.example .env
cat .env
```

## Definição de pronto

Feature concluída quando `.env.example` estiver pronto e `.env` local funcionar com Docker Compose.

---

# 9. FEATURE 003 — DOCKER COMPOSE COM POSTGRESQL DO AEGIS

## Objetivo

Subir banco PostgreSQL persistente da aplicação Aegis PMS.

## Requisito obrigatório

PostgreSQL da aplicação não pode perder dados a cada deploy.

## Decisão

Usar volume nomeado dedicado:

```txt
aegis_postgres_data
```

## Serviço esperado

```yaml
aegis-postgres:
  image: postgres:16
  container_name: aegis-postgres
  restart: unless-stopped
  environment:
    POSTGRES_DB: ${AEGIS_DB_NAME}
    POSTGRES_USER: ${AEGIS_DB_USER}
    POSTGRES_PASSWORD: ${AEGIS_DB_PASSWORD}
    TZ: ${TZ}
  ports:
    - "${AEGIS_DB_PORT}:5432"
  volumes:
    - aegis_postgres_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${AEGIS_DB_USER} -d ${AEGIS_DB_NAME}"]
    interval: 10s
    timeout: 5s
    retries: 5
```

## Tarefas

- Criar `docker-compose.yml`.
- Criar serviço `aegis-postgres`.
- Criar volume `aegis_postgres_data`.
- Criar rede `aegis-network`.
- Configurar healthcheck.
- Validar conexão.

## Critérios de aceite

- Container sobe.
- Banco responde.
- Volume persiste dados.
- Dados permanecem após `docker compose down` normal.
- Dados só somem com `docker compose down -v`.

## Validação técnica

Subir:

```bash
docker compose up -d aegis-postgres
```

Ver logs:

```bash
docker compose logs -f aegis-postgres
```

Testar conexão:

```bash
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "SELECT current_database();"
```

Criar tabela teste:

```bash
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "CREATE TABLE IF NOT EXISTS persistence_test(id INT PRIMARY KEY); INSERT INTO persistence_test(id) VALUES (1) ON CONFLICT DO NOTHING;"
```

Reiniciar:

```bash
docker compose down
docker compose up -d aegis-postgres
```

Validar persistência:

```bash
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "SELECT * FROM persistence_test;"
```

Resultado esperado:

```txt
 id
----
  1
```

## Definição de pronto

Feature concluída quando o banco da aplicação persistir dados após restart.

---

# 10. FEATURE 004 — DOCKER COMPOSE COM POSTGRESQL DEDICADO DO KEYCLOAK

## Objetivo

Subir banco PostgreSQL exclusivo para o Keycloak.

## Requisito obrigatório

Keycloak precisa ter banco dedicado para não perder usuários, realm, clients e configurações a cada deploy.

## Decisão

Não usar banco H2/dev interno do Keycloak para persistência real.

Usar PostgreSQL dedicado:

```txt
keycloak-postgres
```

Volume:

```txt
keycloak_postgres_data
```

## Serviço esperado

```yaml
keycloak-postgres:
  image: postgres:16
  container_name: keycloak-postgres
  restart: unless-stopped
  environment:
    POSTGRES_DB: ${KEYCLOAK_DB_NAME}
    POSTGRES_USER: ${KEYCLOAK_DB_USER}
    POSTGRES_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
    TZ: ${TZ}
  ports:
    - "${KEYCLOAK_DB_PORT}:5432"
  volumes:
    - keycloak_postgres_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${KEYCLOAK_DB_USER} -d ${KEYCLOAK_DB_NAME}"]
    interval: 10s
    timeout: 5s
    retries: 5
```

## Critérios de aceite

- `keycloak-postgres` sobe.
- Banco `keycloak` existe.
- Usuário `keycloak_user` conecta.
- Volume dedicado existe.
- Banco do Keycloak é separado do banco Aegis.

## Validação

```bash
docker compose up -d keycloak-postgres
docker exec -it keycloak-postgres psql -U keycloak_user -d keycloak -c "SELECT current_database();"
```

Esperado:

```txt
 current_database
------------------
 keycloak
```

Validar volumes:

```bash
docker volume ls | grep keycloak
```

## Definição de pronto

Feature concluída quando o banco dedicado do Keycloak estiver funcionando e persistente.

---

# 11. FEATURE 005 — KEYCLOAK PERSISTENTE COM POSTGRESQL DEDICADO

## Objetivo

Subir Keycloak conectado ao banco PostgreSQL dedicado.

## Requisito obrigatório

Usuários, clients e realms devem sobreviver a redeploy/restart.

## Serviço esperado

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0
  container_name: aegis-keycloak
  restart: unless-stopped
  command: start-dev
  environment:
    KC_DB: postgres
    KC_DB_URL_HOST: keycloak-postgres
    KC_DB_URL_PORT: 5432
    KC_DB_URL_DATABASE: ${KEYCLOAK_DB_NAME}
    KC_DB_USERNAME: ${KEYCLOAK_DB_USER}
    KC_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
    KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN}
    KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
    KC_HTTP_ENABLED: "true"
    KC_HOSTNAME_STRICT: "false"
    KC_HOSTNAME_STRICT_HTTPS: "false"
  ports:
    - "${KEYCLOAK_HTTP_PORT}:8080"
  depends_on:
    keycloak-postgres:
      condition: service_healthy
```

## Tarefas

- Adicionar serviço `keycloak`.
- Configurar dependência no `keycloak-postgres`.
- Subir Keycloak.
- Acessar console admin.
- Criar realm manualmente ou via import futuro.

## Critérios de aceite

- Keycloak acessa `http://localhost:8181`.
- Login admin funciona.
- Keycloak usa PostgreSQL.
- Realm criado persiste após restart.
- Usuário criado persiste após restart.

## Validação manual

Acessar:

```txt
http://localhost:8181
```

Login:

```txt
admin / admin
```

Criar realm:

```txt
aegis
```

Criar usuário teste:

```txt
username: loki
email: loki@byop.dev
password: 123456
email verified: true
```

Reiniciar:

```bash
docker compose restart keycloak
```

Verificar se realm e usuário continuam.

Derrubar sem volume:

```bash
docker compose down
docker compose up -d
```

Verificar novamente.

## Validação no banco

```bash
docker exec -it keycloak-postgres psql -U keycloak_user -d keycloak -c "SELECT count(*) FROM realm;"
```

Esperado:

```txt
count >= 1
```

## Definição de pronto

Feature concluída quando realm e usuário sobreviverem a restart e `docker compose down/up`.

---

# 12. FEATURE 006 — EXPORTAR REALM KEYCLOAK PARA VERSIONAMENTO

## Objetivo

Evitar configuração manual irrepetível.

## Tarefas

- Criar pasta `infra/keycloak/realm`.
- Exportar realm `aegis`.
- Salvar como `infra/keycloak/realm/aegis-realm.json`.
- Documentar como importar.

## Comando sugerido

```bash
docker exec -it aegis-keycloak /opt/keycloak/bin/kc.sh export   --dir /tmp/keycloak-export   --realm aegis   --users realm_file
```

Copiar arquivo do container:

```bash
docker cp aegis-keycloak:/tmp/keycloak-export/aegis-realm.json ./infra/keycloak/realm/aegis-realm.json
```

## Critérios de aceite

- Arquivo exportado existe.
- Arquivo não contém segredo sensível indevido.
- Realm pode ser recriado por import no futuro.

## Validação

```bash
ls -la infra/keycloak/realm/
cat infra/keycloak/realm/aegis-realm.json | head
```

## Definição de pronto

Feature concluída quando o realm estiver exportado e versionável.

---

# 13. FEATURE 007 — CONFIGURAR REALM, CLIENT E ROLES DO AEGIS

## Objetivo

Preparar Keycloak para autenticação do Aegis PMS.

## Realm

```txt
aegis
```

## Client frontend

```txt
aegis-web
```

Configuração:

- Client type: OpenID Connect
- Client authentication: OFF
- Standard flow: ON
- Direct access grants: OFF, exceto se precisar testar temporariamente
- Valid redirect URIs:
  - `http://localhost:4200/*`
  - `http://localhost:8080/*`
- Web origins:
  - `http://localhost:4200`
  - `http://localhost:8080`
- PKCE: S256, se disponível

## Roles globais sugeridas

- `AEGIS_SUPER_ADMIN`
- `TENANT_ADMIN`
- `PRODUCT_MANAGER`
- `CONTENT_EDITOR`
- `CONTENT_REVIEWER`
- `VIEWER`

## Observação arquitetural

Keycloak autentica.

Aegis autoriza operacionalmente por tenant, membership e módulo.

Roles do Keycloak devem ser amplas.

Permissões finas pertencem ao backend Aegis.

## Critérios de aceite

- Realm criado.
- Client criado.
- Usuário teste criado.
- Usuário teste possui role adequada.
- OpenID config acessível.
- Token pode ser emitido.

## URLs importantes

```txt
http://localhost:8181/realms/aegis
http://localhost:8181/realms/aegis/.well-known/openid-configuration
http://localhost:8181/realms/aegis/protocol/openid-connect/certs
```

## Validação

Acessar:

```bash
curl http://localhost:8181/realms/aegis/.well-known/openid-configuration
```

Resultado esperado:

- JSON com issuer.
- issuer deve ser `http://localhost:8181/realms/aegis`.

## Definição de pronto

Feature concluída quando o Keycloak emitir token válido para o Aegis.

---

# 14. FEATURE 008 — BACKEND SPRING BOOT BASE

## Objetivo

Criar aplicação Spring Boot base.

## Tarefas

Criar projeto em `backend/` com:

- Java 21+
- Maven
- Spring Boot 3.x
- package base `br.com.byop.aegis`

## Dependências obrigatórias

- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-oauth2-resource-server
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-actuator
- spring-boot-starter-cache, opcional
- spring-modulith-starter-core
- spring-modulith-starter-jpa
- postgresql
- flyway-core
- flyway-database-postgresql, se necessário
- springdoc-openapi-starter-webmvc-ui
- spring-boot-starter-test
- spring-security-test
- testcontainers, depois

## Estrutura esperada

```txt
backend/src/main/java/br/com/byop/aegis
 ├── AegisApplication.java
 ├── config
 ├── security
 ├── shared
 ├── identity
 ├── tenant
 ├── product
 ├── contract
 ├── content
 ├── asset
 ├── form
 ├── submission
 ├── seo
 ├── analytics
 ├── knowledgegraph
 └── integration
```

## Critérios de aceite

- Backend compila.
- Backend sobe.
- `/actuator/health` responde.
- Ainda sem segurança aplicada à API, se necessário nesta etapa.

## Validação

```bash
cd backend
mvn clean test
mvn spring-boot:run
curl http://localhost:8080/actuator/health
```

Esperado:

```json
{"status":"UP"}
```

## Definição de pronto

Feature concluída quando backend Spring Boot base estiver rodando.

---

# 15. FEATURE 009 — CONFIGURAR BACKEND COM POSTGRESQL E FLYWAY

## Objetivo

Conectar backend ao PostgreSQL do Aegis e criar migrations iniciais.

## Configuração

`application.yml` deve usar env vars:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${AEGIS_DB_HOST:localhost}:${AEGIS_DB_PORT:5432}/${AEGIS_DB_NAME:aegis_pms}
    username: ${AEGIS_DB_USER:aegis_user}
    password: ${AEGIS_DB_PASSWORD:aegis_password}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## Tarefas

- Criar `application.yml`.
- Criar `application-local.yml`.
- Criar `application-prod.yml`.
- Configurar datasource.
- Configurar Flyway.
- Criar migration V1 com tabelas mínimas:
  - `tenants`
  - `tenant_memberships`
  - `products`
  - `product_modules`
  - `audit_events`

## Critérios de aceite

- Backend sobe conectado ao banco.
- Flyway cria tabelas.
- Hibernate validate passa.
- Banco mantém dados após restart.

## Validação

```bash
cd backend
mvn spring-boot:run
```

Em outro terminal:

```bash
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "\dt"
```

Esperado:

```txt
tenants
tenant_memberships
products
product_modules
audit_events
flyway_schema_history
```

## Definição de pronto

Feature concluída quando migrations executarem e tabelas existirem.

---

# 16. FEATURE 010 — SECURITY RESOURCE SERVER COM KEYCLOAK

## Objetivo

Integrar backend ao Keycloak via JWT.

## Configuração

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8181/realms/aegis}
```

## Regras

- `/api/v1/**` protegido.
- `/actuator/health` público.
- `/actuator/info` público.
- Swagger local liberado.
- Frontend estático público.
- CSRF off para API stateless.
- CORS local configurado.

## Classes esperadas

```txt
security/SecurityConfig.java
security/AuthenticatedUser.java
security/AuthenticatedUserProvider.java
security/JwtRoleConverter.java
```

## AuthenticatedUser

Campos:

- `subject`
- `email`
- `username`
- `name`
- `roles`

## Critérios de aceite

- Sem token, `/api/v1/me` retorna 401.
- Com token válido, `/api/v1/me` retorna 200 depois da feature de `me`.
- Token inválido retorna 401.
- Backend valida issuer correto.

## Validação

Sem token:

```bash
curl -i http://localhost:8080/api/v1/me
```

Esperado:

```txt
HTTP/1.1 401
```

Com token:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me
```

Esperado:

```txt
200
```

após feature 011.

## Definição de pronto

Feature concluída quando backend rejeitar requisições sem token em `/api/v1/**`.

---

# 17. FEATURE 011 — ENDPOINT /API/V1/ME

## Objetivo

Criar primeiro endpoint autenticado.

## Endpoint

```txt
GET /api/v1/me
```

## Comportamento

Retorna dados do usuário autenticado vindos do JWT.

Response:

```json
{
  "subject": "uuid-keycloak",
  "email": "loki@byop.dev",
  "username": "loki",
  "name": "Loki",
  "roles": ["AEGIS_SUPER_ADMIN"]
}
```

## Critérios de aceite

- Endpoint exige token.
- Endpoint retorna claims.
- Não consulta domínio ainda, se não necessário.
- Não aceita body.
- Não aceita userId no request.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me
```

## Definição de pronto

Feature concluída quando `/api/v1/me` retornar usuário autenticado.

---

# 18. FEATURE 012 — MODELO CORE: TENANT, MEMBERSHIP E PRODUCT

## Objetivo

Criar núcleo do Aegis PMS.

## Entidades

### Tenant

Representa organização/espaço dono de produtos.

Campos mínimos:

- id
- key
- name
- status
- createdAt
- updatedAt

### TenantMembership

Relaciona usuário autenticado ao tenant.

Campos mínimos:

- id
- tenantId
- userSubject
- role
- status
- createdAt
- updatedAt

### Product

Representa produto digital alimentado pelo Aegis.

Campos mínimos:

- id
- tenantId
- key
- name
- type
- status
- defaultLocale
- createdAt
- updatedAt

### ProductModule

Representa módulo habilitado em um produto.

Campos mínimos:

- id
- productId
- moduleKey
- enabled
- settingsJson
- createdAt
- updatedAt

## Módulos possíveis

- `CONTENT`
- `PAGES`
- `ASSETS`
- `FORMS`
- `SUBMISSIONS`
- `SEO`
- `ANALYTICS`
- `JOBS`
- `PORTFOLIO`
- `LIBRARY`
- `KNOWLEDGE_BASE`
- `MUSIC`
- `BOOKS`
- `INTEGRATIONS`
- `KNOWLEDGE_GRAPH`

## Endpoints mínimos

```txt
GET  /api/v1/tenants
POST /api/v1/tenants
GET  /api/v1/products
POST /api/v1/products
GET  /api/v1/products/{productId}
POST /api/v1/products/{productId}/modules/{moduleKey}/enable
POST /api/v1/products/{productId}/modules/{moduleKey}/disable
```

## Regras

- Usuário criador vira `TENANT_ADMIN`.
- Produto pertence a um tenant.
- Listagem retorna apenas produtos de tenants onde usuário tem membership ativa.
- Produto de outro tenant retorna 404.
- `product.key` único por tenant.
- `tenant.key` único globalmente.

## Critérios de aceite

- Criar tenant.
- Criar produto.
- Listar produtos do usuário.
- Habilitar módulo em produto.
- Dados persistem.

## Validação

Criar tenant:

```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"key":"byop","name":"BYOP"}'
```

Criar produto:

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenant-id>","key":"maestro-beton","name":"Maestro Beton","type":"MUSICIAN_SITE","defaultLocale":"pt-BR"}'
```

## Definição de pronto

Feature concluída quando tenant, membership e product estiverem funcionais.

---

# 19. FEATURE 013 — CATÁLOGO DE MÓDULOS DO AEGIS

## Objetivo

Cadastrar os módulos que o Aegis suporta.

## Estratégia

Criar enum ou tabela controlada de módulos.

Para MVP, enum no backend pode ser suficiente.

Módulos obrigatórios:

```txt
CORE
CONTENT
PAGES
BLOCKS
ASSETS
FORMS
SUBMISSIONS
SEO
ANALYTICS
INTEGRATIONS
JOBS
PORTFOLIO
LIBRARY
KNOWLEDGE_BASE
MUSIC
BOOKS
COMMENTS
CONTRIBUTORS
KNOWLEDGE_GRAPH
```

## Critérios de aceite

- Backend sabe quais módulos existem.
- Produto pode habilitar/desabilitar módulos.
- Módulo desconhecido gera erro.
- Módulos habilitados aparecem no detalhe do produto.

## Validação

```bash
curl http://localhost:8080/api/v1/products/<productId> \
  -H "Authorization: Bearer $TOKEN"
```

Esperado:

```json
{
  "modules": [
    {
      "moduleKey": "CONTENT",
      "enabled": true
    }
  ]
}
```

## Definição de pronto

Feature concluída quando módulos forem controláveis por produto.

---

# 20. FEATURE 014 — KNOWLEDGE GRAPH MVP: MODELO RELACIONAL

## Objetivo

Criar MVP funcional de Knowledge Graph usando PostgreSQL.

## Justificativa

O Aegis alimentará sistemas como WikiDev, Loki, portfólio, sites institucionais e produtos com conteúdos relacionados.

O Knowledge Graph permitirá registrar relações entre entidades:

- produto → página;
- página → conteúdo;
- conteúdo → música;
- artigo → tópico;
- artigo → artigo relacionado;
- vaga → candidatura;
- livro → download;
- poema → música;
- tópico → categoria;
- projeto → tecnologia;
- produto → contrato.

## Decisão

MVP em PostgreSQL:

- mais simples;
- menos custo operacional;
- transacional;
- já integrado ao core;
- suficiente para primeira versão.

Preparar arquitetura para futura migração/replicação para Neo4j/Memgraph.

## Entidades

### GraphNode

Campos:

- id
- tenantId
- productId
- nodeType
- refType
- refId
- label
- slug
- metadataJson
- createdAt
- updatedAt

### GraphEdge

Campos:

- id
- tenantId
- productId
- sourceNodeId
- targetNodeId
- edgeType
- weight
- metadataJson
- createdAt
- updatedAt

## Tipos de node iniciais

```txt
PRODUCT
PAGE
CONTENT
ARTICLE
CATEGORY
TOPIC
TAG
ASSET
FORM
JOB_POSTING
CANDIDATE
BOOK
POEM
MANIFESTO
REFLECTION
MUSIC_REFERENCE
PROJECT
SKILL
SERVICE
EVENT
CONTRACT
```

## Tipos de edge iniciais

```txt
CONTAINS
BELONGS_TO
REFERENCES
RELATED_TO
INSPIRED_BY
USES
IMPLEMENTS
PUBLISHED_AS
SUBMITTED_TO
TAGGED_WITH
PART_OF
DEPENDS_ON
```

## Índices obrigatórios

- tenantId
- productId
- nodeType
- refType/refId
- sourceNodeId
- targetNodeId
- edgeType
- unique por productId + refType + refId

## Critérios de aceite

- Criar node.
- Criar edge.
- Listar nodes por produto.
- Listar edges por produto.
- Obter vizinhança de um node.
- Obter conteúdos relacionados.
- Impedir edge entre tenants diferentes.
- Impedir edge com node inexistente.
- Impedir duplicidade básica.

## Endpoints mínimos

```txt
POST /api/v1/products/{productId}/graph/nodes
GET  /api/v1/products/{productId}/graph/nodes
GET  /api/v1/products/{productId}/graph/nodes/{nodeId}
POST /api/v1/products/{productId}/graph/edges
GET  /api/v1/products/{productId}/graph/nodes/{nodeId}/neighbors
GET  /api/v1/products/{productId}/graph/nodes/{nodeId}/related
```

## Validação

Criar node de artigo:

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/nodes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nodeType": "ARTICLE",
    "refType": "ARTICLE",
    "refId": "spring-boot-intro",
    "label": "Introdução ao Spring Boot",
    "slug": "introducao-spring-boot",
    "metadata": {
      "difficulty": "beginner"
    }
  }'
```

Criar node de tópico:

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/nodes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nodeType": "TOPIC",
    "refType": "TOPIC",
    "refId": "spring-boot",
    "label": "Spring Boot",
    "slug": "spring-boot"
  }'
```

Criar edge:

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/edges \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceNodeId": "<article-node-id>",
    "targetNodeId": "<topic-node-id>",
    "edgeType": "TAGGED_WITH",
    "weight": 1
  }'
```

Consultar vizinhança:

```bash
curl http://localhost:8080/api/v1/products/<productId>/graph/nodes/<article-node-id>/neighbors \
  -H "Authorization: Bearer $TOKEN"
```

Resultado esperado:

```json
{
  "nodeId": "...",
  "neighbors": [
    {
      "edgeType": "TAGGED_WITH",
      "node": {
        "nodeType": "TOPIC",
        "label": "Spring Boot"
      }
    }
  ]
}
```

## Definição de pronto

Feature concluída quando for possível criar nodes, edges e consultar vizinhança.

---

# 21. FEATURE 015 — KNOWLEDGE GRAPH SERVICE E REGRAS DE CONSISTÊNCIA

## Objetivo

Garantir que o grafo não vire um depósito caótico de relações.

## Regras obrigatórias

- Node pertence a um tenant.
- Node pode pertencer a um produto.
- Edge só pode ligar nodes do mesmo tenant.
- Edge só pode ligar nodes do mesmo produto, salvo edge cross-product futura explicitamente autorizada.
- Edge exige source e target existentes.
- RefId não pode ser vazio.
- Label não pode ser vazio.
- EdgeType deve estar em catálogo controlado.
- NodeType deve estar em catálogo controlado.
- Duplicidade `productId + refType + refId` deve ser bloqueada.
- Duplicidade `sourceNodeId + targetNodeId + edgeType` deve ser bloqueada ou tratada por upsert.

## Casos de uso

- registrar node;
- registrar edge;
- atualizar label/metadados de node;
- consultar vizinhos;
- consultar relacionados;
- consultar grafo por tipo;
- consultar nodes órfãos;
- remover/arquivar relação.

## Critérios de aceite

- Não é possível criar edge inválida.
- Não é possível misturar tenants por engano.
- Duplicidade controlada.
- Erros claros.
- Testes unitários das regras principais.

## Validação

Tentar criar edge com node inexistente:

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/graph/edges \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceNodeId": "00000000-0000-0000-0000-000000000000",
    "targetNodeId": "00000000-0000-0000-0000-000000000001",
    "edgeType": "RELATED_TO"
  }'
```

Esperado:

```txt
404 ou 400 com erro claro
```

## Definição de pronto

Feature concluída quando as regras de consistência do grafo estiverem protegidas.

---

# 22. FEATURE 016 — API DE HEALTH, INFO E READINESS

## Objetivo

Permitir validar rapidamente se aplicação, banco, Keycloak e dependências estão funcionando.

## Endpoints

Spring Actuator:

```txt
GET /actuator/health
GET /actuator/info
```

Endpoint customizado:

```txt
GET /api/v1/system/status
```

## Status deve informar

- app status;
- database status;
- keycloak issuer configurado;
- graph module status;
- active profile;
- build version, se disponível.

## Critérios de aceite

- Health público responde.
- Status autenticado responde.
- Banco aparece como UP.
- Falhas são legíveis.

## Validação

```bash
curl http://localhost:8080/actuator/health
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/system/status
```

## Definição de pronto

Feature concluída quando status do sistema indicar app + db + graph.

---

# 23. FEATURE 017 — FRONTEND ANGULAR BASE

## Objetivo

Criar frontend Angular mais atual.

## Tarefas

- Criar projeto Angular em `frontend/`.
- Usar standalone components.
- Criar estrutura por feature.
- Configurar ambiente local.
- Preparar integração futura com Keycloak.
- Preparar build para ser servido pelo backend.

## Estrutura sugerida

```txt
frontend/src/app
 ├── core
 │    ├── auth
 │    ├── http
 │    └── layout
 ├── shared
 ├── features
 │    ├── dashboard
 │    ├── tenants
 │    ├── products
 │    ├── modules
 │    ├── contracts
 │    ├── content
 │    ├── assets
 │    └── knowledge-graph
 └── app.routes.ts
```

## Rotas mínimas

```txt
/login
/dashboard
/tenants
/products
/products/:productId
/products/:productId/graph
```

## Critérios de aceite

- Angular roda em `http://localhost:4200`.
- Build gera `dist`.
- Página inicial renderiza.
- Estrutura por feature criada.

## Validação

```bash
cd frontend
npm install
npm start
npm run build
```

## Definição de pronto

Feature concluída quando Angular rodar e buildar.

---

# 24. FEATURE 018 — SERVIR ANGULAR PELO SPRING BOOT

## Objetivo

Backend Spring Boot deve servir a SPA Angular no MVP.

## Estratégia

Build Angular deve ser copiado para:

```txt
backend/src/main/resources/static/
```

Ou via script:

```txt
scripts/build-frontend-to-backend.sh
```

## Regras

- `/api/v1/**` não pode cair no fallback da SPA.
- `/actuator/**` não pode cair no fallback.
- rotas SPA devem retornar `index.html`.
- assets devem ser servidos normalmente.

## Rotas SPA previstas

```txt
/
 /login
 /dashboard
 /tenants
 /products
 /products/:id
 /products/:id/graph
```

## Critérios de aceite

- Backend serve `index.html`.
- Refresh em `/dashboard` funciona.
- `/api/v1/me` continua sendo API.
- `/actuator/health` continua respondendo JSON.

## Validação

Build:

```bash
cd frontend
npm run build
```

Copiar:

```bash
rm -rf ../backend/src/main/resources/static/*
cp -R dist/<nome-do-app>/browser/* ../backend/src/main/resources/static/
```

Rodar backend:

```bash
cd ../backend
mvn spring-boot:run
```

Testar:

```bash
curl http://localhost:8080/
curl http://localhost:8080/dashboard
curl http://localhost:8080/api/v1/me
curl http://localhost:8080/actuator/health
```

Esperado:

- `/` retorna HTML.
- `/dashboard` retorna HTML.
- `/api/v1/me` retorna 401 sem token.
- `/actuator/health` retorna JSON.

## Definição de pronto

Feature concluída quando backend servir a SPA sem quebrar APIs.

---

# 25. FEATURE 019 — DOCKERFILE DO BACKEND

## Objetivo

Containerizar o backend.

## Tarefas

- Criar `backend/Dockerfile`.
- Buildar jar.
- Rodar jar em container.
- Configurar envs.
- Expor porta 8080.

## Critérios de aceite

- Docker image builda.
- Container sobe.
- Conecta no banco via nome do serviço Docker.
- Valida Keycloak pelo issuer interno/externo configurado corretamente.

## Observação crítica

Dentro do Docker, o backend deve acessar:

```txt
http://keycloak:8080/realms/aegis
```

Mas o browser acessa:

```txt
http://localhost:8181/realms/aegis
```

Isso pode gerar problema de issuer.

Solução MVP:

- usar issuer externo `http://localhost:8181/realms/aegis` quando backend roda local;
- para backend containerizado, avaliar `KC_HOSTNAME_URL` ou configuração adequada de hostname;
- não ignorar validação de issuer.

## Critérios de aceite

- Backend containerizado sobe.
- `/actuator/health` responde.
- API protegida responde 401 sem token.

## Validação

```bash
docker build -t aegis-backend ./backend
docker run --rm -p 8080:8080 --env-file .env aegis-backend
```

## Definição de pronto

Feature concluída quando backend containerizado rodar.

---

# 26. FEATURE 020 — DOCKER COMPOSE COMPLETO COM BACKEND

## Objetivo

Subir stack completa:

- PostgreSQL Aegis;
- PostgreSQL Keycloak;
- Keycloak;
- backend.

## Tarefas

- Adicionar serviço `aegis-backend`.
- Configurar `depends_on`.
- Configurar envs.
- Configurar healthcheck.
- Garantir rede única.

## Critérios de aceite

- `docker compose up -d` sobe tudo.
- Backend conecta no PostgreSQL.
- Backend valida Keycloak.
- Health responde.
- Dados persistem.

## Validação

```bash
docker compose up -d --build
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8181/realms/aegis/.well-known/openid-configuration
```

## Definição de pronto

Feature concluída quando stack completa subir com um comando.

---

# 27. FEATURE 021 — SEED INICIAL DO AEGIS

## Objetivo

Criar dados iniciais para desenvolvimento.

## Dados iniciais sugeridos

Tenant:

```txt
BYOP
```

Produtos:

- `maestro-beton`
- `conecta-talentos`
- `alexandre-dev`
- `cmss`
- `loki`
- `wikidev`

Módulos habilitados:

Maestro Beton:

- CONTENT
- PAGES
- ASSETS
- FORMS
- SEO
- ANALYTICS
- MUSIC
- KNOWLEDGE_GRAPH

Conecta Talentos:

- CONTENT
- PAGES
- FORMS
- SUBMISSIONS
- JOBS
- SEO
- ANALYTICS
- INTEGRATIONS
- KNOWLEDGE_GRAPH

Alexandre Dev:

- PORTFOLIO
- CONTENT
- PAGES
- ASSETS
- SEO
- ANALYTICS
- KNOWLEDGE_GRAPH

CMSS:

- CONTENT
- PAGES
- ASSETS
- FORMS
- SEO
- ANALYTICS
- KNOWLEDGE_GRAPH

Loki:

- LIBRARY
- BOOKS
- MUSIC
- CONTENT
- SEO
- ANALYTICS
- KNOWLEDGE_GRAPH

WikiDev:

- KNOWLEDGE_BASE
- CONTENT
- COMMENTS
- CONTRIBUTORS
- FORMS
- ANALYTICS
- KNOWLEDGE_GRAPH

## Estratégia

Pode ser:

- migration Flyway de seed local;
- endpoint admin temporário protegido;
- command line runner apenas profile local.

Recomendação:

- usar migration para dados técnicos globais;
- usar command line runner local para dados demo.

## Critérios de aceite

- Produtos aparecem na API.
- Módulos habilitados aparecem.
- Grafo pode receber nodes para cada produto.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products
```

## Definição de pronto

Feature concluída quando produtos iniciais existirem.

---

# 28. FEATURE 022 — SEED DO KNOWLEDGE GRAPH MVP

## Objetivo

Criar grafo inicial para validar o módulo.

## Exemplo WikiDev

Nodes:

- WikiDev product
- Categoria Programação
- Tópico Java
- Artigo Spring Boot
- Artigo JPA

Edges:

- WikiDev CONTAINS Categoria Programação
- Categoria Programação CONTAINS Tópico Java
- Tópico Java CONTAINS Artigo Spring Boot
- Artigo Spring Boot RELATED_TO Artigo JPA

## Exemplo Loki

Nodes:

- Loki product
- Poema
- Música
- Playlist

Edges:

- Poema INSPIRED_BY Música
- Música PART_OF Playlist

## Critérios de aceite

- Seed cria nodes.
- Seed cria edges.
- Endpoint de neighbors mostra relações.
- Endpoint related retorna conteúdo relacionado.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/products/<wikidev-id>/graph/nodes/<spring-boot-node-id>/neighbors
```

## Definição de pronto

Feature concluída quando o grafo demonstrar relações reais de pelo menos WikiDev e Loki.

---

# 29. FEATURE 023 — OPENAPI / SWAGGER

## Objetivo

Documentar APIs para agentes e frontend.

## Regras

- Swagger habilitado local/dev.
- Swagger desabilitado prod.
- Bearer JWT configurado.
- Tags por módulo.

## Tags mínimas

- System
- Auth
- Tenants
- Products
- Product Modules
- Knowledge Graph

## Critérios de aceite

- Swagger abre em local.
- Permite informar token.
- Endpoints aparecem agrupados.
- Em profile prod fica desabilitado.

## Validação

```txt
http://localhost:8080/swagger-ui/index.html
```

## Definição de pronto

Feature concluída quando Swagger documentar endpoints iniciais.

---

# 30. FEATURE 024 — TESTES MÍNIMOS DA FUNDAÇÃO

## Objetivo

Evitar que a fundação nasça cega.

## Testes obrigatórios

- TenantServiceTest
- ProductServiceTest
- ProductModuleServiceTest
- KnowledgeGraphServiceTest
- GraphConsistencyPolicyTest
- AuthenticatedUserProviderTest
- Security smoke test

## Cenários obrigatórios

- criar tenant;
- criar produto;
- bloquear produto de tenant alheio;
- habilitar módulo válido;
- rejeitar módulo inválido;
- criar node;
- criar edge válida;
- rejeitar edge com node inexistente;
- rejeitar edge cross-tenant;
- listar neighbors;
- `/api/v1/me` sem token = 401.

## Critérios de aceite

- `mvn test` passa.
- Regras críticas cobertas.
- Testes não dependem de dados manuais do Keycloak.

## Validação

```bash
cd backend
mvn clean test
```

## Definição de pronto

Feature concluída quando testes mínimos passarem.

---

# 31. FEATURE 025 — CHECKLIST FINAL DO SERVIDOR FUNCIONAL

## Objetivo

Validar o fim da primeira etapa operacional.

## Checklist

Infra:

- [ ] `docker compose up -d` sobe tudo.
- [ ] `aegis-postgres` está healthy.
- [ ] `keycloak-postgres` está healthy.
- [ ] `keycloak` está acessível.
- [ ] `backend` está acessível.
- [ ] volumes existem.
- [ ] dados persistem após restart.

Keycloak:

- [ ] Realm `aegis` existe.
- [ ] Client `aegis-web` existe.
- [ ] Usuário teste existe.
- [ ] Usuário persiste após restart.
- [ ] Token pode ser emitido.
- [ ] OpenID config acessível.

Backend:

- [ ] `/actuator/health` responde UP.
- [ ] `/api/v1/me` sem token retorna 401.
- [ ] `/api/v1/me` com token retorna usuário.
- [ ] Flyway criou tabelas.
- [ ] Swagger abre em local.
- [ ] API usa `/api/v1`.

Core:

- [ ] Tenant pode ser criado.
- [ ] Product pode ser criado.
- [ ] Product pode habilitar módulo.
- [ ] Listagem respeita membership.
- [ ] Produto alheio não é acessível.

Knowledge Graph:

- [ ] Node pode ser criado.
- [ ] Edge pode ser criada.
- [ ] Neighbor pode ser consultado.
- [ ] Edge inválida é bloqueada.
- [ ] Cross-tenant é bloqueado.
- [ ] Seed inicial funciona.

Frontend:

- [ ] Angular roda local.
- [ ] Angular builda.
- [ ] Backend serve SPA.
- [ ] Refresh de rota SPA funciona.
- [ ] API não cai no fallback da SPA.

## Definição de pronto geral

A fundação operacional do Aegis PMS está pronta quando:

```txt
Docker Compose sobe tudo
+
Keycloak persiste usuários
+
PostgreSQL persiste dados
+
Backend autentica JWT
+
Tenant/Product funcionam
+
Knowledge Graph MVP funciona
+
Angular é servido pelo backend
```

---

# 32. ORDEM RECOMENDADA DE EXECUÇÃO

Executar nesta ordem:

1. Feature 001 — Estrutura base
2. Feature 002 — Env base
3. Feature 003 — PostgreSQL Aegis
4. Feature 004 — PostgreSQL Keycloak
5. Feature 005 — Keycloak persistente
6. Feature 007 — Realm/client/roles
7. Feature 006 — Export realm
8. Feature 008 — Backend base
9. Feature 009 — PostgreSQL + Flyway
10. Feature 010 — Security Keycloak
11. Feature 011 — `/api/v1/me`
12. Feature 012 — Tenant/Membership/Product
13. Feature 013 — Catálogo de módulos
14. Feature 014 — Knowledge Graph MVP
15. Feature 015 — Regras de consistência do grafo
16. Feature 016 — Health/status
17. Feature 017 — Angular base
18. Feature 018 — Servir Angular pelo backend
19. Feature 019 — Dockerfile backend
20. Feature 020 — Docker Compose completo
21. Feature 021 — Seed Aegis
22. Feature 022 — Seed Knowledge Graph
23. Feature 023 — Swagger
24. Feature 024 — Testes
25. Feature 025 — Checklist final

---

# 33. PROMPT MESTRE PARA QUALQUER AGENTE

Use este prompt no início de qualquer chat/agente.

```md
Você é um engenheiro sênior trabalhando no Aegis PMS, Product Management System do ecossistema BYOP.

Contexto:
Aegis PMS é uma plataforma administrativa multi-produto, multi-tenant, modular e orientada a contratos. Ela alimentará produtos como Maestro Beton, Conecta Talentos, Alexandre Dev, CMSS, Loki e WikiDev.

Stack obrigatória:
- Docker Compose
- PostgreSQL da aplicação com volume persistente
- PostgreSQL dedicado do Keycloak com volume persistente
- Keycloak
- Java 21+
- Spring Boot 3.x
- Spring Security OAuth2 Resource Server
- Spring Data JPA
- Flyway
- Spring Modulith
- Angular mais atual
- Backend servindo SPA Angular no MVP

Decisões:
- Product First
- Contract First
- Modular Monolith
- REST First
- GraphQL apenas futuro
- PostgreSQL como banco inicial
- Knowledge Graph MVP obrigatório já nesta versão
- Keycloak autentica
- Aegis autoriza por tenant/membership/product/module

Regras:
1. Não remover persistência dos bancos.
2. Não usar H2 como banco principal.
3. Não aceitar userId/tenantId como verdade sem validar autorização.
4. Não expor entidades diretamente.
5. Não misturar regra de negócio em controller.
6. Não criar microsserviços.
7. Não quebrar `/api/v1`.
8. Não deixar endpoint administrativo sem autenticação.
9. Não criar grafo sem validação de tenant/product.
10. Cada feature precisa ter critérios de aceite e validação.

Feature atual:
[COLE A FEATURE AQUI]

Entregue:
- arquivos a criar/alterar
- código/configuração sugerida
- comandos de execução
- comandos de validação
- critérios de aceite
- riscos
- definição de pronto
```

---

# 34. VEREDITO FINAL DE AEGIS

Loki, se a decisão é construir tudo, então o caminho não é reduzir.

O caminho é impor ordem.

O Aegis PMS só sobreviverá se a fundação for construída nesta sequência:

```txt
Persistência
↓
Identidade
↓
Backend seguro
↓
Tenant/Product
↓
Modules
↓
Knowledge Graph
↓
Frontend
↓
Servidor único funcional
```

O Knowledge Graph já nesta versão é uma decisão ambiciosa, mas correta, porque os produtos que o Aegis alimentará dependem de relações:

- WikiDev precisa de conhecimento conectado;
- Loki precisa ligar obras, músicas, livros e reflexões;
- Alexandre Dev precisa ligar projetos, skills, artigos e experiências;
- Conecta Talentos precisa ligar vagas, candidatos, empresas e leads;
- Maestro Beton precisa ligar serviços, agenda, repertório, vídeos e produtos futuros;
- CMSS precisa ligar páginas, eventos, história, assets e formas de apoio.

Mas o grafo deve nascer simples.

Não como Neo4j obrigatório.

Não como teoria infinita.

Nasce como:

```txt
graph_nodes
graph_edges
```

com regras fortes.

Se um dia isso virar knowledge graph real com motor dedicado, a arquitetura já estará pronta.

O escudo está claro:

> primeiro persistir, depois autenticar, depois autorizar, depois modelar produto, depois conectar conhecimento.

Essa é a fundação do Aegis PMS.
