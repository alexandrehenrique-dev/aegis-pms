# Etapa 01 — Estrutura do repositório e variáveis de ambiente

> Cole este arquivo inteiro numa conversa nova do GPT.

## Contexto fixo

Você é um engenheiro sênior trabalhando no backend do **Aegis PMS**, Product Management System do ecossistema BYOP — uma plataforma administrativa multi-produto, multi-tenant, modular e orientada a contratos (não um CMS comum).

Stack obrigatória: **Java 25** (não "21+", não negociável), **Spring Boot 4.1.x** (linha estável atual — vem com Spring Framework 7, Jakarta EE 11, Hibernate 7.1, Spring Security 7, Jackson 3; ver `00_padrao_qualidade_e_arquitetura.md` para os pontos de atenção na migração de 3.x para 4.x), Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, Spring Modulith, PostgreSQL (com volume persistente dedicado), Keycloak (com PostgreSQL dedicado e persistente, nunca H2), Docker Compose. O frontend já existe em React (não é Angular — decisão registrada em ADR-0011) e será buildado e servido pelo Spring Boot na mesma origem mais adiante nesta sprint.

Regras que nunca podem ser violadas: não remover persistência dos bancos; não usar H2 como banco principal; não misturar regra de negócio em controller; não criar microsserviços (é um monólito modular); manter `/api/v1` como prefixo de toda API; toda etapa precisa ter critérios de aceite e validação antes de seguir para a próxima.

## Objetivo desta etapa

Criar a estrutura base do repositório backend e definir as variáveis de ambiente que toda a infraestrutura local vai usar.

## Tarefas

### A. Estrutura de diretórios

Confirmar/criar:

```txt
aegis-pms/
 ├── docs/
 ├── infra/
 │    ├── docker/
 │    └── keycloak/
 ├── backend/
 │    ├── src/
 │    ├── pom.xml
 │    └── Dockerfile
 ├── scripts/
 ├── docker-compose.yml
 └── .env.example
```

`frontend/` já existe (não criar — pertence à Sprint 01, já em React).

### B. Variáveis de ambiente

Criar/atualizar `.env.example` na raiz com:

```env
# Project
COMPOSE_PROJECT_NAME=aegis-pms

# Aegis PostgreSQL
AEGIS_DB_NAME=aegis_pms
AEGIS_DB_USER=aegis_user
AEGIS_DB_PASSWORD=aegis_password
AEGIS_DB_PORT=5434

# Keycloak PostgreSQL (nomes com sufixo "_aegis" — ver etapa 02 — para nunca colidir com outro
# Postgres/Keycloak já existente no ambiente, ex. outros produtos BYOP rodando na mesma máquina)
KEYCLOAK_DB_NAME=keycloak_aegis
KEYCLOAK_DB_USER=keycloak_aegis_user
KEYCLOAK_DB_PASSWORD=keycloak_aegis_password
KEYCLOAK_DB_PORT=5435

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_HTTP_PORT=8282
KEYCLOAK_REALM=aegis
KEYCLOAK_WEB_CLIENT_ID=aegis-web

# Backend
BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=local
KEYCLOAK_ISSUER_URI=http://localhost:8282/realms/aegis

# Frontend (build estático servido pelo backend — ver etapa 18)
FRONTEND_DEV_PORT=5173

# Storage (estratégia por produto — local por padrão, ver etapa 11 Seção D). A pasta é criada
# automaticamente (etapa 04, bootstrap em Java multiplataforma; etapa 19, volume Docker) — nunca
# precisa ser criada manualmente, em nenhum sistema operacional. O código sempre grava dentro de um
# namespace fixo "aegis/pms" abaixo deste caminho raiz (ex.: caminho final de um asset =
# ${AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/{tenantId}/{productId}/{category}/{filename}), para o mesmo
# volume poder ser compartilhado por outros produtos BYOP no futuro sem reconfigurar nada.
AEGIS_STORAGE_LOCAL_PATH=./data/assets
# Opcional — só necessário se algum produto usar assetStorageStrategy "s3"
AEGIS_STORAGE_S3_BUCKET=
AEGIS_STORAGE_S3_REGION=
AEGIS_STORAGE_S3_ACCESS_KEY_ID=
AEGIS_STORAGE_S3_SECRET_ACCESS_KEY=
AEGIS_STORAGE_S3_PRESIGNED_URL_TTL_SECONDS=900

# Timezone
TZ=America/Sao_Paulo
```

Nota: `FRONTEND_DEV_PORT` é `5173` (porta padrão do Vite), não `4200` (Angular) — ajuste em relação ao roteiro original, já que o frontend é React+Vite.

O repositório raiz já tem um `.gitignore` consolidado (criado previamente) cobrindo backend, frontend, Docker, Keycloak e PostgreSQL — não sobrescrever, apenas confirmar que ele cobre `target/`, `*.env`, etc.

## Critérios de aceite

- [ ] Estrutura de diretórios confirmada/criada.
- [ ] `.env.example` existe na raiz com todas as variáveis acima, sem nenhuma senha real de produção.
- [ ] `.gitignore` da raiz já cobre os artefatos do backend.

## Validação

> **Entrega via collection Bruno, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os como requests `.bru` na pasta numerada desta etapa em `bruno/` (collection cumulativa, autenticação herdada via header `Authorization: Bearer {{token}}` definido em `collection.bru`) e valide a collection inteira via `npx @usebruno/cli run --env local`.

```bash
tree -L 2
cp .env.example .env
cat .env
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add .
git commit -m "chore(backend): estrutura base do backend e variaveis de ambiente"
```
