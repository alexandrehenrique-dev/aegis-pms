# Variaveis de Ambiente

O arquivo real de producao deve ficar em `/opt/aegis/infra/.env.prod` no servidor e nunca deve ser commitado. Use `infra/.env.prod.example` como base.

## Imagem

- `AEGIS_DEPLOY_BRANCH`: branch canônica implantada. Atual: `develop`; futuro: `release`.
- `IMAGE_TAG`: tag Docker opcional; o script oficial deriva da branch por padrão.

## Banco Aegis

- `AEGIS_DB_HOST`: host/IP do PostgreSQL dedicado do Aegis.
- `AEGIS_DB_PORT`: porta do PostgreSQL do Aegis. Padrao: `5432`.
- `AEGIS_DB_NAME`: database do Aegis.
- `AEGIS_DB_USER`: usuario da aplicacao.
- `AEGIS_DB_PASSWORD`: segredo do usuario da aplicacao.

## Keycloak

- `KEYCLOAK_ISSUER_URI`: issuer publico, ex. `https://auth.byop.dev/realms/aegis`.
- `KEYCLOAK_JWK_SET_URI`: JWKS publico do realm.
- `KEYCLOAK_INTERNAL_BASE_URL`: URL interna usada pelo backend para chamadas administrativas.
- `KEYCLOAK_REALM`: realm, padrao `aegis`.
- `KEYCLOAK_WEB_CLIENT_ID`: cliente OIDC, padrao `aegis-web`.
- `KEYCLOAK_ADMIN`: usuario administrativo usado pelo backend/scripts.
- `KEYCLOAK_ADMIN_PASSWORD`: senha administrativa.
- `KEYCLOAK_ADMIN_REALMS_PATH`: path admin, padrao `/admin/realms/`.

## Aplicacao

- `SPRING_PROFILES_ACTIVE`: profile Spring, padrão de produção `prod`.
- `AEGIS_APP_BASE_URL`: URL publica do painel, padrao `https://aegis.byop.dev`.
- `AEGIS_APP_CORS_ALLOWED_ORIGINS`: origens explícitas separadas por vírgula.
- `AEGIS_ASSETS_MULTIPART_MAX_FILE_SIZE`: limite multipart por arquivo, padrao `260MB`.
- `AEGIS_ASSETS_MULTIPART_MAX_REQUEST_SIZE`: limite multipart por request, padrao `260MB`.

## SMTP

- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_FROM`
- `SMTP_FROM_DISPLAY_NAME`
- `SMTP_AUTH`
- `SMTP_USER`
- `SMTP_PASSWORD`
- `SMTP_STARTTLS`
- `SMTP_SSL`

## Telegram Global do Aegis

Este canal e somente para feedback interno do Aegis (`POST /feedback`), nao para submissions de produtos externos.

- `AEGIS_TELEGRAM_ALERT_ENABLED`
- `AEGIS_TELEGRAM_ALERT_BOT_TOKEN`
- `AEGIS_TELEGRAM_ALERT_CHAT_ID`

## GitHub Actions Secrets

- `KEYCLOAK_URL`
- `KEYCLOAK_ADMIN`
- `KEYCLOAK_ADMIN_PASSWORD`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_FROM`
- `SMTP_FROM_DISPLAY_NAME`
- `SMTP_USER`
- `SMTP_PASSWORD`
