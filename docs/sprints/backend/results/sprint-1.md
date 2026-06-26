# Sprint 01 — Estrutura do repositório e variáveis de ambiente

## Etapa Concluída

Etapa 01 — Estrutura do repositório e variáveis de ambiente

Data: 2026-06-21

Status: CONCLUÍDA

---

## Objetivo

Preparar a fundação do backend Aegis PMS através da definição da estrutura do repositório e padronização das variáveis de ambiente que serão utilizadas por todas as etapas subsequentes.

---

## Estrutura Confirmada

```txt
aegis-pms/
├── docs/
├── infra/
│   ├── docker/
│   └── keycloak/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
├── scripts/
├── docker-compose.yml
└── .env.example
```

---

## Arquivos Criados ou Confirmados

### Raiz

- docker-compose.yml
- .env.example
- .gitignore

### Backend

- backend/pom.xml
- backend/Dockerfile
- backend/src/

### Infraestrutura

- infra/docker/
- infra/keycloak/

### Documentação

- docs/
- docs/sprints/backend/

---

## Variáveis de Ambiente Definidas

### Projeto

- COMPOSE_PROJECT_NAME

### Banco Principal

- AEGIS_DB_NAME
- AEGIS_DB_USER
- AEGIS_DB_PASSWORD
- AEGIS_DB_PORT

### Banco Keycloak

- KEYCLOAK_DB_NAME
- KEYCLOAK_DB_USER
- KEYCLOAK_DB_PASSWORD
- KEYCLOAK_DB_PORT

### Keycloak

- KEYCLOAK_ADMIN
- KEYCLOAK_ADMIN_PASSWORD
- KEYCLOAK_HTTP_PORT
- KEYCLOAK_REALM
- KEYCLOAK_WEB_CLIENT_ID

### Backend

- BACKEND_PORT
- SPRING_PROFILES_ACTIVE
- KEYCLOAK_ISSUER_URI

### Frontend

- FRONTEND_DEV_PORT=5173

### Storage

- AEGIS_STORAGE_LOCAL_PATH
- AEGIS_STORAGE_S3_BUCKET
- AEGIS_STORAGE_S3_REGION
- AEGIS_STORAGE_S3_ACCESS_KEY_ID
- AEGIS_STORAGE_S3_SECRET_ACCESS_KEY
- AEGIS_STORAGE_S3_PRESIGNED_URL_TTL_SECONDS

### Timezone

- TZ=America/Sao_Paulo

---

## Decisões Arquiteturais Registradas

### Frontend

Decisão:

Frontend oficial do Aegis PMS é React + Vite.

Porta padrão:

5173

Motivação:

ADR-0011 substituiu Angular por React.

---

### Backend

Decisão:

Backend será construído utilizando:

- Java 25
- Spring Boot 4.1.x
- Spring Security 7
- Spring Data JPA
- Flyway
- Spring Modulith

---

### Persistência

Decisão:

PostgreSQL é o banco oficial.

Restrições:

- Não utilizar H2 como banco principal.
- Não utilizar MongoDB.
- Não utilizar bancos alternativos sem ADR.

---

### Arquitetura

Decisão:

Monólito modular.

Restrições:

- Não criar microsserviços.
- Não dividir domínios em aplicações independentes.

---

### Segurança

Decisão:

Keycloak será o único provedor de identidade.

Restrições:

- Não implementar autenticação própria.
- Não criar tabela de usuários local para autenticação.

---

### API

Decisão:

Prefixo global obrigatório:

```txt
/api/v1
```

---

## Endpoints Confirmados

Nenhum endpoint implementado nesta etapa.

---

## Contratos Criados

Nenhum contrato criado nesta etapa.

---

## Entidades Criadas

Nenhuma entidade criada nesta etapa.

---

## Casos de Uso Criados

Nenhum caso de uso criado nesta etapa.

---

## Testes Executados

Validação estrutural:

```bash
tree -L 2
```

Validação de ambiente:

```bash
cp .env.example .env
cat .env
```

Resultado:

Estrutura e variáveis confirmadas.

---

## Pendências para Próximas Etapas

### Infraestrutura

- Docker Compose completo.
- PostgreSQL do Aegis.
- PostgreSQL do Keycloak.
- Keycloak configurado.

### Backend

- Projeto Spring Boot funcional.
- Configuração Maven.
- Configuração Flyway.
- Configuração Modulith.

### Segurança

- Realm aegis.
- Client aegis-web.
- Integração OAuth2 Resource Server.

### API

- Primeiro contrato REST.
- Primeiro endpoint sob /api/v1.

---

## Retrofits Identificados

Nenhum retrofit identificado nesta etapa.

---

## Critérios de Aceite Atendidos

- Estrutura do repositório confirmada.
- .env.example definido.
- .gitignore validado.
- Artefato de continuidade gerado.

---

Fim da Etapa 01.
