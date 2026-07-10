# Keycloak

## Timeouts de sessão

O realm local em `realm/aegis-realm.json` usa valores confortáveis para desenvolvimento e demonstração:

- SSO Session Idle: 8 horas (`28800`)
- SSO Session Max: 24 horas (`86400`)
- Access Token Lifespan: 30 minutos (`1800`)

Produção deve usar valores mais restritos. O script `scripts/harden-realm-prod.sh` aplica:

- SSO Session Idle: 30 minutos (`1800`)
- SSO Session Max: 8 horas (`28800`)
- Access Token Lifespan: 5 minutos (`300`)

Para mudar por ambiente, ajuste o realm importado antes de recriar o container local, ou aplique os mesmos campos via `kcadm.sh update realms/aegis -s nomeDoCampo=valorEmSegundos`.

## Keycloak efêmero de CI

A stack de CI (`infra/docker-compose.ci.yml`) sobe um Keycloak descartável que:

1. Importa `realm/aegis-realm.json` automaticamente via `start-dev --import-realm` (montado em `/opt/keycloak/data/import`).
2. Cria os usuários de teste via `scripts/bootstrap-ci-users.sh` (serviço one-shot `keycloak-init-ci`) — o realm JSON canônico não contém usuários, e os usuários usados pelo Bruno (`aegis` com `AEGIS_SUPER_ADMIN`) só existiam manualmente no runner.
3. É destruído com `docker compose down -v` ao final de cada run (dados em tmpfs; nada persiste).

Esse Keycloak jamais deve apontar para o banco de produção nem ser substituído pelo Keycloak de produção (Oracle Cloud) — ver `docs/adr/ADR-0021-stack-ci-efemera.md`.
