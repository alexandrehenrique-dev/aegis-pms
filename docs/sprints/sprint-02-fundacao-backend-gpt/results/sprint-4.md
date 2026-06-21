# SPRINT_04_RESULTADO.md

# Sprint 04 — Conteúdos

Status: CONCLUÍDA

Data: 21/06/2026

Responsável: Alexandre Silva (Loki)

---

# Objetivo da Sprint

Construir a fundação persistente do domínio de Conteúdos do Aegis PMS garantindo:

- persistência PostgreSQL;
- migrações Flyway;
- integração Spring Modulith;
- auditoria inicial;
- bootstrap de armazenamento local;
- validação de arquitetura para futuras entidades de conteúdo.

---

# Arquiteturas e ADRs Atendidos

- ADR-0001 Product First
- ADR-0002 Contract First
- ADR-0004 Shared Schema
- ADR-0006 REST First
- ADR-0008 JSON Contracts
- ADR-0010 PostgreSQL
- ADR-0013 Entidades Globais de Produto
- ADR-0015 Módulos como Portão de Acesso
- ADR-0016 Knowledge Graph

---

# Entregas Realizadas

## Persistência PostgreSQL

Banco operacional:

```txt
PostgreSQL 16
```

Validação concluída via:

```bash
docker compose
Spring Boot
Hibernate
Flyway
```

---

## Flyway

Flyway configurado e executando antes do Hibernate.

Validações:

- migration encontrada;
- migration aplicada;
- schema versionado;
- flyway_schema_history criado corretamente.

Resultado:

```txt
Schema up to date
```

---

## Spring Modulith

Persistência de eventos habilitada.

Tabela criada:

```sql
event_publication
```

Schema compatível com versão utilizada do Spring Modulith.

Campos validados:

- id
- completion_date
- event_type
- listener_id
- publication_date
- serialized_event
- status
- completion_attempts
- last_resubmission_date

Índices:

- event_publication_by_listener_id_and_serialized_event_idx
- event_publication_by_completion_date_idx

---

## Storage Local

Bootstrap implementado.

Diretório criado automaticamente:

```txt
data/assets/aegis/pms
```

Validação realizada:

```bash
rm -rf ./data/assets
mvn spring-boot:run
```

Resultado:

```txt
Storage local pronto
```

---

## Observabilidade

Actuator operacional.

Validação:

```bash
curl http://localhost:8080/actuator/health
```

Resposta:

```json
{
  "status": "UP"
}
```

---

# Modelo de Dados Implantado

Tabelas criadas:

```txt
tenants
tenant_memberships
products
product_modules
audit_events
event_publication
flyway_schema_history
```

---

# Estrutura Base de Multi-Tenant

Entidades implantadas:

## Tenant

Representa organização proprietária dos produtos.

---

## Tenant Membership

Relacionamento usuário ↔ tenant.

Permite:

- OWNER
- ADMIN
- EDITOR
- REVIEWER
- AUTHOR
- VIEWER

(conforme ADR de papéis)

---

## Product

Produto pertencente a um tenant.

Estrutura preparada para:

- sites
- portais
- CMS
- PMS
- aplicações futuras

---

## Product Module

Portão de acesso modular.

Permite ativar/desativar módulos por produto.

Base alinhada ao ADR-0015.

---

## Audit Events

Registro canônico de auditoria.

Preparado para:

- LGPD
- rastreabilidade
- observabilidade
- histórico de ações

---

# Decisões Arquiteturais Relevantes

## Consolidação de Migration

Durante a implementação foi identificado conflito entre:

- Flyway
- Hibernate
- Spring Modulith

A estratégia inicial de múltiplas migrations corretivas foi descartada.

Foi adotada:

```txt
Migration única canônica
+
Recriação controlada do banco
```

Benefícios:

- menor dívida técnica;
- menor complexidade inicial;
- schema único e rastreável;
- redução de inconsistências futuras.

---

# Evidências

## Build

```bash
mvn clean verify
```

Resultado:

```txt
BUILD SUCCESS
```

---

## Aplicação

```bash
mvn spring-boot:run
```

Resultado:

```txt
Started AegisApplication
```

---

## Health Check

```txt
UP
```

---

## Flyway

```txt
Schema up to date
```

---

## Storage

```txt
Storage local pronto
```

---

# Débito Técnico

Nenhum débito crítico identificado.

Itens futuros:

- Testes automatizados.
- Testcontainers.
- Auditoria estruturada.
- Publicação assíncrona de eventos.
- Integração Keycloak ↔ Tenant Membership.
- Storage S3 compatível com Local Storage.

---

# Critério de Conclusão

Sprint considerada concluída quando:

- Banco sobe.
- Flyway executa.
- Hibernate valida.
- Modulith persiste eventos.
- Actuator responde.
- Storage é criado.
- Build permanece verde.

Todos os critérios foram atendidos.

---

# Evidências finais do encerramento

## Build completo

Executado com `JAVA_HOME` apontando para Temurin 25.0.3:

```text
[INFO] --- surefire:3.5.4:test (default-test) @ aegis-pms-backend ---
[INFO] --- jar:3.5.0:jar (default-jar) @ aegis-pms-backend ---
[INFO] --- jacoco:0.8.14:check (jacoco-check) @ aegis-pms-backend ---
[INFO] Skipping JaCoCo execution due to missing execution data file: backend/target/jacoco.exec
[INFO] BUILD SUCCESS
[INFO] Total time: 0.939 s
```

Nesta etapa ainda não existem classes de teste, portanto o Surefire não gerou `jacoco.exec`; o goal `jacoco:check` foi invocado e o build terminou verde.

## Docker Compose e health

```text
aegis-keycloak            Up
aegis-postgres            Up (healthy)   5434->5432
keycloak-postgres-aegis   Up (healthy)   5435->5432
```

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## Flyway

```text
installed_rank | version | description | success
1              | 1       | init        | true
```

Ordem de startup observada após a correção: `FlywayExecutor` → `DbValidate` → `DbMigrate` → Hibernate `Processing PersistenceUnitInfo`.

## Storage local

```text
backend/data/assets/aegis/pms
```

O cenário negativo também foi validado:

```text
java.lang.IllegalStateException: Falha ao preparar storage local em: /dev/null/aegis/pms
Caused by: java.nio.file.FileSystemException: /dev/null/aegis: Not a directory
```

## Hashes técnicos

```text
ed42ee287826aceefcf91dce417e4dbaf704f0e2  implementação base
1081d33b507b297e737e5c43b9b8f3c754135cba  ordem Flyway antes do Hibernate
```

---

STATUS FINAL

SPRINT 04 — APROVADA

Próxima Sprint:

Sprint 05 — Identity and Access (Integração Keycloak + Modelo Canônico de Papéis)
