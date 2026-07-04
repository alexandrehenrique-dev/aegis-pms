# Plano de Validacao

## Validacao Local Obrigatoria da Sprint

```bash
cd frontend
npm run lint
npm run typecheck
cd ..
```

Como esta sprint altera backend/config Docker/runtime, tambem validar:

```bash
cd backend
mvn clean verify
cd ..
docker compose config
docker compose -f infra/docker-compose.prod.yml --env-file infra/.env.prod.example config
git diff --check
```

O compose de producao com `.env.prod.example` valida forma, nao conectividade real, porque os hosts e segredos de producao sao placeholders.

## Validacao da SPA Servida Pelo Backend

Depois de subir a imagem:

```bash
curl -fsS https://aegis.byop.dev/
curl -fsS https://aegis.byop.dev/actuator/health
```

O primeiro comando deve retornar o `index.html` da SPA React. O segundo deve retornar status `UP`.

## Validacao de API e Auth

```bash
curl -fsS https://aegis.byop.dev/actuator/health
curl -i https://aegis.byop.dev/api/v1/me
```

`/api/v1/me` sem token deve responder como rota protegida. Login real deve ser validado pela UI atual, que chama `/api/v1/auth/login` sem expor a tela nativa do Keycloak ao usuario final.

## Validacao de Banco e Migrations

- Conferir logs do backend no primeiro boot.
- Confirmar Flyway sem erro.
- Confirmar que o banco do Aegis e o banco do Keycloak sao instancias/databases dedicados.
- Confirmar que nenhum volume persistente foi removido em redeploy.

## Validacao Keycloak

```bash
kcadm.sh get realms/aegis --fields accessTokenLifespan,ssoSessionIdleTimeout,ssoSessionMaxLifespan,bruteForceProtected,failureFactor,passwordPolicy
kcadm.sh get clients -r aegis -q clientId=aegis-web --fields clientId,redirectUris,webOrigins,directAccessGrantsEnabled
```

Esperado:

- `accessTokenLifespan` igual a `900`.
- `ssoSessionIdleTimeout` igual a `14400`.
- `ssoSessionMaxLifespan` igual a `28800`.
- `bruteForceProtected` igual a `true`.
- `failureFactor` igual a `5`.
- `passwordPolicy` definida.
- `https://aegis.byop.dev/*` presente em `redirectUris`.

Enquanto o backend usar `AuthService.login()` com password grant, `directAccessGrantsEnabled` deve permanecer `true` no cliente usado pelo backend. A desativacao fica pendente de migracao de auth.

## Validacao Bruno

Para ambiente local completo:

```bash
docker compose up -d --build
cd bruno
npx @usebruno/cli run --env local
cd ..
docker compose down
```

Para producao, preencher o environment `prod` da collection apenas quando o ambiente existir e houver credenciais reais apropriadas.
