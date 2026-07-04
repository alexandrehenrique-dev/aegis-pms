# Mudancas no Repositorio

## Infra

- `infra/docker-compose.prod.yml`: compose de producao que sobe apenas `aegis-backend`; PostgreSQL e Keycloak sao externos.
- `infra/.env.prod.example`: template versionado das variaveis de producao.
- `.gitignore`: `infra/.env.prod` fica fora do git; exemplos `*.env.example` continuam versionaveis.
- `infra/Caddyfile`: reverse proxy HTTPS para `aegis.byop.dev` apontando para `localhost:8080`.
- `infra/keycloak/scripts/harden-realm-prod.sh`: script idempotente para aplicar timeouts, brute force protection, password policy e SMTP do realm.
- `infra/keycloak/realm/aegis-realm.json`: import inicial inclui redirects/web origins de producao e hardening sem segredos.

## Backend

- `backend/src/main/java/br/com/byop/aegis/security/SecurityConfig.java`: CORS parametrizado via `aegis.app.cors-allowed-origins`.
- `backend/src/main/resources/application.yml`: origem local padrao `http://localhost:5173`.
- `backend/src/main/resources/application-prod.yml`: profile `prod` com Swagger desabilitado, base URL de producao e CORS para `https://aegis.byop.dev`.
- `backend/Dockerfile`: imagem continua empacotando frontend + backend no mesmo artefato, com `curl` para healthcheck HTTP e volumes para assets/templates.

## CI/CD

- `.github/workflows/ci.yml`: pipeline com `frontend-check`, `backend-verify`, `docker-build`, `bruno-tests` e `deploy`.
- O deploy automatico fica restrito a push em `main`.
- Push/PR para `develop` valida frontend, backend, Docker e Bruno sem promover producao.

## Divergencias Contra a Sprint

- `backend/Dockerfile` e `backend/src/main/resources/application-prod.yml` ja existiam; foram ajustados em vez de recriados.
- O repositorio usa Java 25, nao Java 21; workflow e Dockerfile seguem o `pom.xml` atual.
- O `vite.config.ts` gera `frontend/dist`; o empacotamento no backend ja e feito pelo Dockerfile multi-stage e pelo script `scripts/build-frontend-for-backend.sh`, entao nao foi alterado para escrever diretamente em `backend/src/main/resources/static`.
- O backend atual ainda usa `AuthService.login()` com `grant_type=password` em `KeycloakTokenClient`. Por isso `aegis-web.directAccessGrantsEnabled` permanece habilitado no import inicial e o script so desabilita via flag explicita `DISABLE_AEGIS_WEB_DIRECT_GRANTS=true`, apos migracao do fluxo de login.
