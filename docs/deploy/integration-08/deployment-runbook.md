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

Enquanto a implantacao real ainda nao for acionada pelo GitHub Actions:

```bash
docker build -t aegis-pms:latest -f backend/Dockerfile .
docker compose -f infra/docker-compose.prod.yml --env-file infra/.env.prod up -d
```

## 4. Deploy via GitHub Actions

1. Garantir que os checks em `develop` passaram.
2. Promover para `main` somente por decisao humana.
3. O job `deploy` roda apenas em push para `main`.
4. O deploy recria o container `aegis-backend` em `/opt/aegis`.
5. O health check publico deve passar antes da conclusao do job.

## 5. Rollback

1. Identificar a imagem anterior disponivel no host.
2. Ajustar `IMAGE_TAG` em `/opt/aegis/infra/.env.prod`.
3. Recriar o container:

```bash
docker compose -f infra/docker-compose.prod.yml --env-file infra/.env.prod up -d --force-recreate aegis-backend
```

4. Validar `https://aegis.byop.dev/actuator/health`.

Migrations Flyway aplicadas nao devem ser revertidas por troca simples de imagem. Mudancas destrutivas exigem plano de rollback de dados antes do deploy.
