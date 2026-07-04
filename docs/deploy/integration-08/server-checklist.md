# Checklist de Servidor

## Genesis-lab

- DNS `aegis.byop.dev` apontando para o `genesis-lab`.
- Portas liberadas no firewall:
  - `22/tcp` para SSH.
  - `80/tcp` para desafio HTTP do Let's Encrypt.
  - `443/tcp` para HTTPS publico.
  - `8080/tcp` negada externamente; o backend deve escutar apenas em loopback via compose.
- Docker Engine e Docker Compose plugin instalados.
- Diretorio do deploy em `/opt/aegis`.
- Diretorio persistente de assets:

```bash
sudo mkdir -p /opt/aegis/assets
sudo chown "$(whoami):$(whoami)" /opt/aegis/assets
```

- Arquivo `/opt/aegis/infra/.env.prod` criado a partir de `infra/.env.prod.example`, com segredos reais.
- `infra/Caddyfile` copiado para `/etc/caddy/Caddyfile`.
- Caddy instalado e ativo como servico systemd.
- GitHub Actions self-hosted runner instalado em `/opt/aegis/runner`, com label `genesis-lab`.
- Usuario do runner com permissao para executar Docker.
- Secrets do GitHub Actions cadastrados para Keycloak e SMTP reais.

## Maquina Oracle

- PostgreSQL dedicado do Aegis criado e persistente.
- PostgreSQL dedicado do Keycloak criado e persistente.
- Keycloak rodando contra o banco dedicado do Keycloak.
- Realm `aegis` importado de `infra/keycloak/realm/aegis-realm.json` ou configurado de forma equivalente.
- Redirect URI `https://aegis.byop.dev/*` liberada no cliente `aegis-web`.
- SMTP real configurado no realm.
- Brute force protection, password policy e lifespans aplicados pelo script `infra/keycloak/scripts/harden-realm-prod.sh` ou manualmente.

## Pendencias Antes do Primeiro Deploy Real

- Comprar/provisionar a maquina Oracle citada na sprint.
- Definir IPs privados/publicos e conectividade entre `genesis-lab` e Oracle.
- Criar secrets reais fora do repositorio.
- Validar backup e restore dos dois PostgreSQL antes de dados reais.
- Decidir quando migrar o login atual por password grant para Authorization Code + PKCE puro ou cliente backend confidencial.
