# Etapa 04 — Backend Spring Boot base e conexão com PostgreSQL/Flyway

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 02 concluída (PostgreSQL do Aegis no ar).

## Contexto fixo

Aegis PMS: monólito modular Spring Boot (não microsserviços). Módulos previstos: `core`, `identity`, `tenant`, `product`, `contract`, `content`, `asset`, `form`, `submission`, `seo`, `analytics`, `knowledgegraph`, `integration`. Banco: PostgreSQL com Flyway controlando migrations, `ddl-auto: validate` (nunca `update`/`create` em ambiente real).

## Objetivo

Criar a aplicação Spring Boot base e conectá-la ao `aegis-postgres`, com a primeira migration Flyway.

## Tarefas

### A. Projeto base

Criar projeto em `backend/` — Java 21+, Maven, Spring Boot 3.x, package base `br.com.byop.aegis`.

Dependências: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `postgresql`, `flyway-core`, `flyway-database-postgresql`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `spring-security-test`.

Estrutura de pacotes:

```txt
backend/src/main/java/br/com/byop/aegis
 ├── AegisApplication.java
 ├── config
 ├── security
 ├── shared
 ├── identity
 ├── tenant
 ├── product
 ├── contract
 ├── content
 ├── asset
 ├── form
 ├── submission
 ├── seo
 ├── analytics
 ├── knowledgegraph
 └── integration
```

### B. Configuração de datasource e Flyway

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${AEGIS_DB_HOST:localhost}:${AEGIS_DB_PORT:5432}/${AEGIS_DB_NAME:aegis_pms}
    username: ${AEGIS_DB_USER:aegis_user}
    password: ${AEGIS_DB_PASSWORD:aegis_password}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Criar também `application-local.yml` e `application-prod.yml` (perfis).

### C. Primeira migration

`V1__init.sql` (ou equivalente Flyway) criando tabelas mínimas: `tenants`, `tenant_memberships`, `products`, `product_modules`, `audit_events`. (A definição completa de colunas vem na etapa 06 — aqui pode-se criar a tabela mínima ou já completa, à escolha do GPT, desde que a etapa 06 não precise recriar do zero.)

## Critérios de aceite

- [ ] `mvn clean test` passa.
- [ ] `mvn spring-boot:run` sobe a aplicação.
- [ ] `/actuator/health` responde `{"status":"UP"}`.
- [ ] Backend conecta ao `aegis-postgres` e Flyway cria as tabelas.
- [ ] `ddl-auto` é `validate`, nunca `update` ou `create`.

## Validação

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Em outro terminal:

```bash
curl http://localhost:8080/actuator/health
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "\dt"
```

Esperado: `tenants`, `tenant_memberships`, `products`, `product_modules`, `audit_events`, `flyway_schema_history`.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): spring boot base conectado ao postgresql com flyway"
```
