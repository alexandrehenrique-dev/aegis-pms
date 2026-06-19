# Etapa 10 — Dockerfile do backend e Docker Compose completo

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 02-09 concluídas.

## Contexto fixo

Última etapa de infraestrutura: containerizar o backend e ligar tudo (os dois PostgreSQL, o Keycloak e o backend) em um único `docker compose up -d`.

## Objetivo

`docker compose up -d` sobe a stack inteira (PostgreSQL Aegis, PostgreSQL Keycloak, Keycloak, backend com frontend embutido) com um comando.

## Tarefas

### A. `backend/Dockerfile`

Build multi-stage: build do jar com Maven, depois imagem final só com JRE rodando o jar. Expor porta 8080.

### B. Observação crítica de issuer

Dentro da rede Docker, o backend acessaria o Keycloak via `http://keycloak:8080/realms/aegis`, mas o browser acessa via `http://localhost:8282/realms/aegis`. Isso pode gerar conflito de issuer.

Solução para o MVP: manter o issuer **externo** (`http://localhost:8282/realms/aegis`) configurado via `KEYCLOAK_ISSUER_URI`, mesmo quando o backend roda containerizado — não silenciar a validação de issuer para "resolver" o problema.

### C. Serviço `aegis-backend` no compose

```yaml
  aegis-backend:
    build:
      context: ./backend
    container_name: aegis-backend
    restart: unless-stopped
    environment:
      AEGIS_DB_HOST: aegis-postgres
      AEGIS_DB_PORT: 5432
      AEGIS_DB_NAME: ${AEGIS_DB_NAME}
      AEGIS_DB_USER: ${AEGIS_DB_USER}
      AEGIS_DB_PASSWORD: ${AEGIS_DB_PASSWORD}
      KEYCLOAK_ISSUER_URI: ${KEYCLOAK_ISSUER_URI}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
    ports:
      - "${BACKEND_PORT}:8080"
    depends_on:
      aegis-postgres:
        condition: service_healthy
      keycloak:
        condition: service_started
    networks:
      - aegis-network
```

## Critérios de aceite

- [ ] `docker build -t aegis-backend ./backend` builda sem erro.
- [ ] `docker compose up -d --build` sobe os 4 serviços.
- [ ] Backend conecta no PostgreSQL pelo nome do serviço Docker (`aegis-postgres`), não `localhost`.
- [ ] Backend valida o Keycloak pelo issuer externo configurado corretamente.
- [ ] `/actuator/health` responde UP com tudo containerizado.
- [ ] Todos os volumes/dados persistem entre `docker compose down` (sem `-v`) e novo `up`.

## Validação

```bash
docker compose up -d --build
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8282/realms/aegis/.well-known/openid-configuration
```

## Commit sugerido

```bash
git add backend/Dockerfile docker-compose.yml
git commit -m "feat(infra): dockerfile do backend e docker compose completo da stack"
```
