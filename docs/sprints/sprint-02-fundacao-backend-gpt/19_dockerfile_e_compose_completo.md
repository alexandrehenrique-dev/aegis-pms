# Etapa 19 — Dockerfile do backend e Docker Compose completo

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 02-18 concluídas (todos os domínios e o build do frontend já existem, para a imagem final do backend já servir tudo).

## Contexto fixo

Última etapa de infraestrutura: containerizar o backend e ligar tudo (os dois PostgreSQL, o Keycloak e o backend) em um único `docker compose up -d`.

## Objetivo

`docker compose up -d` sobe a stack inteira (PostgreSQL Aegis, PostgreSQL Keycloak, Keycloak, backend com frontend embutido) com um comando.

## Tarefas

### A. `backend/Dockerfile`

Build multi-stage: build do jar com Maven, depois imagem final só com JRE rodando o jar. Expor porta 8080.

Imagens base — **fixar Java 25 explicitamente**, nunca uma tag genérica que pode resolver para uma versão antiga:

```dockerfile
# build stage
FROM maven:3.9-eclipse-temurin-25 AS build
# ...

# runtime stage
FROM eclipse-temurin:25-jre
# ...
```

Se a tag exata `eclipse-temurin-25`/`25-jre` ainda não existir no Docker Hub no momento da execução desta etapa, usar a imagem oficial mais próxima disponível para Java 25 (ex.: Eclipse Temurin, Amazon Corretto ou a imagem oficial do projeto Eclipse Adoptium) — nunca rebaixar para Java 21/17 só porque a tag "25" demorou a publicar.

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
      AEGIS_STORAGE_LOCAL_PATH: /app/assets
    ports:
      - "${BACKEND_PORT}:8080"
    volumes:
      - aegis_assets_data:/app/assets
    depends_on:
      aegis-postgres:
        condition: service_healthy
      keycloak:
        condition: service_started
    networks:
      - aegis-network
```

Adicionar `aegis_assets_data` à lista de `volumes:` nomeados do `docker-compose.yml` (mesmo nível de `aegis_postgres_data`/`keycloak_postgres_data` já existentes) — **persistente, nunca efêmero**, mesma regra já aplicada aos bancos (ADR-0005/ADR-0010).

### D. Pasta de storage: automática tanto containerizado quanto local (Sprint 13 do frontend)

Dois mecanismos complementares, não um substituindo o outro:

- **Containerizado** (esta etapa): o Docker já cria o volume nomeado `aegis_assets_data` automaticamente no primeiro `docker compose up -d --build`, sem nenhum comando manual — é assim que o requisito "criação automática no compose" é atendido. `AEGIS_STORAGE_LOCAL_PATH` dentro do container aponta para `/app/assets` (o ponto de montagem do volume — caminho final de um arquivo: `/app/assets/aegis/pms/{tenantId}/{productId}/{category}/{filename}`, ver etapa 11 Seção D.1), não para o `./data/assets` relativo usado em dev local sem Docker.
- **Local, sem Docker** (etapa 04): o bootstrap em Java (`Files.createDirectories`) cria a pasta no caminho configurado em `AEGIS_STORAGE_LOCAL_PATH`, funcionando igual em Linux, macOS ou Windows, sem depender de `docker compose` estar em uso. Isso cobre quem roda o backend direto com `mvn spring-boot:run` durante o desenvolvimento.

Os dois mecanismos juntos garantem que a pasta de assets **sempre existe** antes do primeiro upload, independente de como o backend está sendo executado.

## Critérios de aceite

- [ ] `docker build -t aegis-backend ./backend` builda sem erro.
- [ ] `docker compose up -d --build` sobe os 4 serviços.
- [ ] Backend conecta no PostgreSQL pelo nome do serviço Docker (`aegis-postgres`), não `localhost`.
- [ ] Backend valida o Keycloak pelo issuer externo configurado corretamente.
- [ ] `/actuator/health` responde UP com tudo containerizado.
- [ ] Todos os volumes/dados persistem entre `docker compose down` (sem `-v`) e novo `up`.
- [ ] Volume `aegis_assets_data` é criado automaticamente no primeiro `up`, sem comando manual; um asset enviado sobrevive a `docker compose down` (sem `-v`) seguido de novo `up`.

## Validação

```bash
docker compose up -d --build
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8282/realms/aegis/.well-known/openid-configuration

# confirmar volume de assets criado automaticamente
docker volume ls | grep aegis_assets_data
docker exec -it aegis-backend ls -la /app/assets/aegis/pms

# confirmar persistencia: subir, gravar um asset de teste, derrubar sem -v, subir de novo
docker compose down
docker compose up -d
docker exec -it aegis-backend ls -la /app/assets/aegis/pms
```

## Commit sugerido

```bash
git add backend/Dockerfile docker-compose.yml
git commit -m "feat(infra): dockerfile do backend, docker compose completo da stack e volume persistente de assets"
```
