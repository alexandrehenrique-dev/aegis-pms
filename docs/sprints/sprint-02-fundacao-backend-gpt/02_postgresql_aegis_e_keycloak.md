# Etapa 02 — PostgreSQL dedicado do Aegis e do Keycloak

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 01 concluída.

## Contexto fixo

Aegis PMS, monólito modular Spring Boot, multi-tenant. Regra inegociável desta etapa: **Keycloak e Aegis usam bancos PostgreSQL separados, cada um com volume Docker nomeado e persistente.** Nenhum dos dois pode perder dados num `docker compose down` normal (sem `-v`). Nunca usar H2.

## Objetivo

Subir dois containers PostgreSQL 16 independentes, cada um com seu próprio volume nomeado, healthcheck e rede.

## Tarefas

### A. `aegis-postgres` (banco da aplicação)

Criar/atualizar `docker-compose.yml` na raiz com:

```yaml
services:
  aegis-postgres:
    image: postgres:16
    container_name: aegis-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${AEGIS_DB_NAME}
      POSTGRES_USER: ${AEGIS_DB_USER}
      POSTGRES_PASSWORD: ${AEGIS_DB_PASSWORD}
      TZ: ${TZ}
    ports:
      - "${AEGIS_DB_PORT}:5432"
    volumes:
      - aegis_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${AEGIS_DB_USER} -d ${AEGIS_DB_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - aegis-network

volumes:
  aegis_postgres_data:

networks:
  aegis-network:
```

### B. `keycloak-postgres` (banco exclusivo do Keycloak)

Adicionar ao mesmo `docker-compose.yml`:

```yaml
  keycloak-postgres:
    image: postgres:16
    container_name: keycloak-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${KEYCLOAK_DB_NAME}
      POSTGRES_USER: ${KEYCLOAK_DB_USER}
      POSTGRES_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
      TZ: ${TZ}
    ports:
      - "${KEYCLOAK_DB_PORT}:5432"
    volumes:
      - keycloak_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${KEYCLOAK_DB_USER} -d ${KEYCLOAK_DB_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - aegis-network
```

Adicionar `keycloak_postgres_data:` na seção `volumes:` do compose.

## Critérios de aceite

- [ ] `aegis-postgres` e `keycloak-postgres` sobem como containers separados.
- [ ] Cada um tem volume nomeado próprio (`aegis_postgres_data`, `keycloak_postgres_data`).
- [ ] Dados persistem após `docker compose down` (sem `-v`) e novo `up`.
- [ ] Dados só desaparecem com `docker compose down -v` (comportamento esperado e aceitável só em ambiente de desenvolvimento, nunca em produção).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
docker compose up -d aegis-postgres keycloak-postgres
docker compose logs -f aegis-postgres keycloak-postgres

# testar aegis
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "SELECT current_database();"
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "CREATE TABLE IF NOT EXISTS persistence_test(id INT PRIMARY KEY); INSERT INTO persistence_test(id) VALUES (1) ON CONFLICT DO NOTHING;"

# testar keycloak
docker exec -it keycloak-postgres psql -U keycloak_user -d keycloak -c "SELECT current_database();"

# validar persistencia
docker compose down
docker compose up -d aegis-postgres keycloak-postgres
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "SELECT * FROM persistence_test;"
```

Resultado esperado: a linha `id = 1` continua existindo após o restart, e ambos os bancos respondem com o nome de banco correto.

## Commit sugerido

```bash
git add docker-compose.yml
git commit -m "feat(infra): postgresql dedicado e persistente para aegis e para keycloak"
```
