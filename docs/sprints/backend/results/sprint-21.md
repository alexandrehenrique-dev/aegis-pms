# Sprint 21 — Dockerfile do Backend e Docker Compose Completo

Concluida em 2026-06-29 na branch `sprint/21-dockerfile-e-compose-completo`.

## Objetivo

Concluir a infraestrutura Docker oficial do Aegis PMS, permitindo subir a stack local completa com:

```bash
docker compose up -d --build
```

O fluxo sobe PostgreSQL Aegis, PostgreSQL Keycloak, Keycloak, MailHog, backend Spring Boot e a SPA React embutida no backend.

## Resultado alcancado

O backend passou a ter Dockerfile multi-stage com build da SPA dentro do Docker, Java 25 para o backend, imagem final somente com JRE e JAR executavel, e o `docker-compose.yml` oficial agora inclui o servico `aegis-backend`, volume persistente de assets e configuracao container-to-container para banco, Keycloak, SMTP e templates de e-mail.

A stack foi validada com containers reais, healthcheck do backend, issuer externo do Keycloak, persistencia dos bancos e persistencia de assets apos `docker compose down && docker compose up -d`.

## Classes criadas ou alteradas

Nenhuma classe Java foi criada ou alterada nesta sprint.

## Arquivos criados

- `.dockerignore`
- `bruno/21-docker-stack/folder.bru`
- `bruno/21-docker-stack/backend-health-containerizado.bru`
- `bruno/21-docker-stack/keycloak-discovery-issuer.bru`
- `docs/sprints/backend/results/sprint-21.md`

## Arquivos alterados

- `backend/Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `docs/api-testing/README.md`
- `docs/deploy/README.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

## Dockerfile do backend

O `backend/Dockerfile` usa build multi-stage:

- frontend build: `node:22-alpine`
- build: `maven:3.9-eclipse-temurin-25`
- runtime: `eclipse-temurin:25-jre`
- SPA: `npm ci` e `npm run build`, copiando `frontend/dist` para `src/main/resources/static`
- empacotamento: `mvn -B -DskipTests package spring-boot:repackage`
- entrypoint: `java -jar /app/aegis-pms-backend.jar`

A imagem final contem apenas JRE e o JAR executavel. Maven, codigo fonte e dependencias de build ficam somente no stage de build.

O Dockerfile exige `src/main/resources/static/index.html` apos copiar o build da SPA para evitar gerar uma imagem sem frontend embutido. O compose usa contexto da raiz do repositorio e `dockerfile: backend/Dockerfile`, permitindo que `docker compose up -d --build` funcione em checkout limpo sem depender de `backend/src/main/resources/static` pregerado.

## Docker Compose

Servico `aegis-backend` adicionado com:

- `build.context: .`
- `build.dockerfile: backend/Dockerfile`
- `restart: unless-stopped`
- `depends_on` para PostgreSQL Aegis healthy, Keycloak started e MailHog started
- rede `aegis-network`
- porta `${BACKEND_PORT}:8080`
- volume persistente `aegis_assets_data:/app/assets`
- bind read-only dos templates de e-mail de produto em `/app/email-templates/product`
- variaveis de banco, Keycloak, SMTP, storage local e multipart

Volume persistente adicionado:

- `aegis_assets_data`

O volume do Keycloak e o volume do PostgreSQL Aegis foram preservados, sem mudanca destrutiva.

## Keycloak Issuer

O issuer externo foi preservado:

- `KEYCLOAK_ISSUER_URI=http://localhost:8282/realms/aegis`

Para o backend containerizado conseguir buscar chaves sem alterar o issuer, foi configurado:

- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/${KEYCLOAK_REALM}/protocol/openid-connect/certs`
- `KEYCLOAK_INTERNAL_BASE_URL=http://keycloak:8080`
- `KC_HOSTNAME=http://localhost:${KEYCLOAK_HTTP_PORT}`

Assim, o token continua emitido e validado com issuer externo `localhost:8282`, enquanto a comunicacao interna entre containers usa o hostname Docker `keycloak`.

## Storage local e assets

No container:

- raiz configurada: `/app/assets`
- volume Docker: `aegis_assets_data`
- path final criado pelo bootstrap: `/app/assets/aegis/pms`

O bootstrap Java existente com `Files.createDirectories(...)` foi preservado. Localmente fora do Docker, `AEGIS_STORAGE_LOCAL_PATH=./data/assets` continua valido; no container, o compose sobrescreve para `/app/assets`.

## Bruno

Criada a pasta `bruno/21-docker-stack`:

- `backend-health-containerizado.bru`: valida `GET {{baseUrl}}/actuator/health`
- `keycloak-discovery-issuer.bru`: valida `GET {{keycloakIssuer}}/.well-known/openid-configuration` e issuer externo exato `http://localhost:8282/realms/aegis`

`docs/api-testing/README.md` foi atualizado para registrar a pasta 21 e informar que a sprint usa apenas `baseUrl` e `keycloakIssuer`.

## Documentacao

`docs/deploy/README.md` recebeu a secao "Docker Compose Local Oficial", com o comando unico, os servicos, volumes persistentes e observacao sobre o issuer externo.

`.env.example` teve apenas comentario ajustado para referenciar corretamente a Sprint 21 como origem do volume Docker de storage.

## Comandos executados

- `docker manifest inspect maven:3.9-eclipse-temurin-25`
- `docker manifest inspect eclipse-temurin:25-jre`
- `docker compose config`
- `docker build -t aegis-backend -f backend/Dockerfile .`
- `docker compose up -d --build`
- `docker compose ps`
- `docker compose logs --tail=200 aegis-backend`
- `curl http://localhost:8080/actuator/health`
- `curl http://localhost:8282/realms/aegis/.well-known/openid-configuration`
- `docker exec aegis-backend ls -la /app/assets/aegis/pms`
- `docker compose exec aegis-postgres psql -U aegis_user -d aegis_pms -c 'select count(*) from flyway_schema_history;'`
- `docker compose exec keycloak-postgres psql -U keycloak_aegis_user -d keycloak_aegis -c 'select count(*) from realm;'`
- `docker compose down && docker compose up -d`
- `docker compose stop aegis-backend`
- `cd backend && mvn clean verify`
- `cd bruno && npx @usebruno/cli run --env local`
- `git diff --check`

## Resultado do `mvn clean verify`

Ultima execucao obrigatoria no fechamento da sprint:

- Comando: `mvn clean verify`
- Diretorio: `backend/`
- Resultado: `BUILD SUCCESS`
- Tests run: 1003
- Failures: 0
- Errors: 0
- Skipped: 0
- JaCoCo: `All coverage checks have been met`
- Spring Modulith: aprovado via suite completa

## Resultado do Docker

`docker compose up -d --build` subiu os cinco servicos:

- `aegis-backend`
- `aegis-keycloak`
- `aegis-mailhog`
- `aegis-postgres`
- `keycloak-postgres-aegis`

Health do backend containerizado:

- `GET http://localhost:8080/actuator/health`
- resultado: `{"groups":["liveness","readiness"],"status":"UP"}`

Discovery do Keycloak:

- `GET http://localhost:8282/realms/aegis/.well-known/openid-configuration`
- issuer confirmado: `http://localhost:8282/realms/aegis`

Validacao autenticada:

- token obtido via Keycloak externo `localhost:8282`
- `GET /api/v1/me` no backend containerizado retornou 200 com role `super_admin`

## Resultado de persistencia

Validado com `docker compose down && docker compose up -d`, sem `-v`:

- volume de assets preservou o arquivo marcador em `/app/assets/aegis/pms/sprint-21/marker.txt`
- PostgreSQL Aegis preservou `flyway_schema_history` com contagem `11`
- PostgreSQL Keycloak preservou `realm` com contagem `2`

Volumes existentes confirmados:

- `aegis-pms_aegis_assets_data`
- `aegis-pms_aegis_postgres_data`
- `aegis-pms_keycloak_aegis_postgres_data`

## Resultado do Bruno

Ultima execucao contra a stack containerizada:

- Comando: `cd bruno && npx @usebruno/cli run --env local`
- Status: PASS
- Requests: 169 executados, 169 aprovados
- Tests: 331/331 aprovados
- Duration: 23207 ms

## Limpeza operacional

Ao final das validacoes, foi executado:

```bash
docker compose stop aegis-backend
```

Resultado esperado confirmado em `docker compose ps`:

- `aegis-backend`: parado
- `aegis-postgres`: rodando e healthy
- `keycloak-postgres-aegis`: rodando e healthy
- `aegis-keycloak`: rodando
- `aegis-mailhog`: rodando

Nenhum volume foi removido e `docker compose down -v` nao foi executado.

## SonarQube for IDE

Nenhum codigo Java foi alterado nesta sprint. Nao houve apontamento Sonar novo tratado via supressao, `NOSONAR` ou desativacao de regra.

Observacao: SonarQube for IDE nao possui runner CLI neste ambiente; a validacao objetiva desta sprint ficou concentrada em build Maven, JaCoCo, Spring Modulith, Docker Compose e Bruno.

## Bugs ou ajustes reais encontrados

- A primeira imagem gerada pelo Dockerfile continha JAR sem manifesto executavel para `java -jar`; corrigido adicionando `spring-boot:repackage` ao build Docker.
- A revisao do criterio de "um comando" mostrou que depender de `backend/src/main/resources/static` pregerado deixaria o compose fragil em checkout limpo; corrigido adicionando stage Node no Dockerfile e mudando o build do compose para contexto raiz.
- A porta 8080 estava ocupada por uma execucao local do backend via IntelliJ; o processo local foi encerrado para validar a porta oficial do compose.
- O teste Bruno inicial do discovery tentava ler `bru.getVar("keycloakIssuer")`, mas a CLI nao expunha essa variavel no bloco de teste apesar de interpolar a URL corretamente; o teste passou a validar explicitamente o issuer externo esperado.
- O MailHog em host arm64 emitiu warning de plataforma da imagem `linux/amd64`, sem impacto funcional; o container subiu e respondeu normalmente.

## Criterios de aceite

- Dockerfile multi-stage com Java 25.
- Build da SPA executado dentro do Docker antes do Maven.
- Imagem final somente com JRE, JAR e entrypoint.
- `docker compose up -d --build` sobe a stack completa.
- `aegis_assets_data` criado como volume Docker nomeado.
- `/app/assets` montado no volume persistente.
- Storage local por Java NIO preservado.
- Issuer externo do Keycloak preservado como `http://localhost:8282/realms/aegis`.
- Backend containerizado valida token real e responde `/api/v1/me`.
- `/actuator/health` aprovado na stack containerizada.
- PostgreSQL Aegis, PostgreSQL Keycloak e assets sobrevivem a `docker compose down && docker compose up -d`.
- Bruno completo aprovado.
- `mvn clean verify` aprovado com JaCoCo e Spring Modulith.

## Retrofits pendentes

- Reexecutar SonarQube for IDE na IDE local para confirmar ausencia de alertas visuais no workspace.
- Automatizar em pipeline futuro a geracao do artefato SPA antes do build Docker, caso a imagem precise ser construida em ambiente limpo sem `backend/src/main/resources/static` gerado previamente.
