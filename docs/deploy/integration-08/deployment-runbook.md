# Runbook de Implantacao

## 1. Preparar Infra Externa

1. Provisionar a maquina Oracle.
2. Criar PostgreSQL dedicado do Aegis com volume persistente.
3. Criar PostgreSQL dedicado do Keycloak com volume persistente.
4. Subir Keycloak contra seu banco dedicado.
5. Importar ou configurar o realm `aegis`.
6. Aplicar hardening do realm:

```bash
KEYCLOAK_URL=https://auth.byop.dev \
KEYCLOAK_ADMIN=<admin> \
KEYCLOAK_ADMIN_PASSWORD=<segredo> \
SMTP_HOST=<smtp> \
SMTP_PORT=587 \
SMTP_USER=<usuario> \
SMTP_PASSWORD=<segredo> \
bash infra/keycloak/scripts/harden-realm-prod.sh
```

## 2. Preparar Genesis-lab

1. Instalar Docker, Docker Compose plugin e Caddy.
2. Instalar o GitHub Actions self-hosted runner com label `genesis-lab`.
3. Criar `/opt/aegis/assets`.
4. Clonar ou atualizar o repositorio em `/opt/aegis`.
5. Criar `/opt/aegis/infra/.env.prod` a partir de `infra/.env.prod.example`.
6. Copiar `infra/Caddyfile` para `/etc/caddy/Caddyfile`.
7. Recarregar Caddy:

```bash
sudo systemctl reload caddy
```

## 3. Build e Deploy Manual Inicial

O ambiente já está implantado. O procedimento vigente é
[`../genesis-lab.md`](../genesis-lab.md), executado por
`scripts/deploy-aegis.sh`.

## 4. Deploy via GitHub Actions

1. Garantir que os checks da branch configurada passaram.
2. Manter `AEGIS_DEPLOY_BRANCH=develop` enquanto este for o ambiente implantado.
3. O workflow observa `develop` e `release`, mas só implanta a branch igual à variável.
4. O script constrói a tag derivada da branch e recria `aegis-backend`.
5. Healthcheck, IDs de imagem, preflight e login inválido devem passar.

## 5. Rollback

O script preserva a imagem anterior e tenta rollback automático. O procedimento
manual atualizado está em [`../genesis-lab.md`](../genesis-lab.md#rollback-e-contingência).

Migrations Flyway aplicadas nao devem ser revertidas por troca simples de imagem. Mudancas destrutivas exigem plano de rollback de dados antes do deploy.
