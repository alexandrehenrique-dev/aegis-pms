# Sprint 09 — Health, info e readiness

## Identificação

- **Sprint:** 09 — Health, info e readiness
- **Status:** Concluída em 2026-06-26
- **Resultado local:** BUILD SUCCESS

## Resumo Executivo

A Sprint 09 entregou um endpoint operacional autenticado para diagnóstico do backend Aegis PMS, cobrindo aplicação, banco PostgreSQL, configuração do issuer Keycloak, presença do módulo Knowledge Graph, profiles ativos e versão de build.

## Entregas

### API

- `GET /actuator/health` público, mantido via Spring Actuator.
- `GET /actuator/info` público, mantido conforme configuração do Actuator.
- `GET /api/v1/system/status` autenticado via JWT.

### Escopo entregue

- Exposição pública de health/info operacional via Actuator.
- Endpoint próprio de status sistêmico para diagnóstico autenticado.
- Verificação de conectividade do banco com mensagem controlada em caso de falha.
- Indicação se o issuer Keycloak está configurado.
- Indicação de disponibilidade do módulo Knowledge Graph por presença de bean Spring.
- Profiles ativos e versão de build retornados no contrato de resposta.
- Teste de auditoria Spring Modulith executado dentro do `mvn clean verify`.

### Classes criadas/alteradas

Arquivos criados:

- `br.com.byop.aegis.system.controller.SystemStatusController`
- `br.com.byop.aegis.system.dto.SystemStatusResponse`
- `br.com.byop.aegis.system.service.SystemStatusService`
- `br.com.byop.aegis.ModulithArchitectureTest`
- `br.com.byop.aegis.system.controller.SystemStatusControllerTest`
- `br.com.byop.aegis.system.service.SystemStatusServiceTest`

Arquivos alterados:

- `br.com.byop.aegis.security.SecurityConfigTest`
- `docs/sprints/backend/09_health_status.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `postman/aegis-pms-sprint-07-core-postman-collection.json`
- `postman/aegis-postman-collection.json`

### Contrato de resposta

`GET /api/v1/system/status` retorna um objeto com:

- `applicationStatus`
- `databaseStatus`
- `databaseMessage`
- `keycloakIssuerConfigured`
- `knowledgeGraphStatus`
- `activeProfiles`
- `buildVersion`

### Regras de negócio

- `/actuator/health` e `/actuator/info` ficam públicos para observabilidade básica.
- `/api/v1/system/status` exige autenticação.
- Falha de banco não expõe stack trace bruto.
- Knowledge Graph é diagnosticado sem violar fronteiras modulares.

### Decisões reais tomadas

- O status do banco usa `JdbcTemplate` com `select 1`, delegando o ciclo de vida da conexão ao Spring.
- O status do Knowledge Graph não importa service/repository interno do módulo; ele verifica a presença do bean `knowledgeGraphService` via `ApplicationContext`.
- `buildVersion` usa `BuildProperties` quando disponível e retorna `0.0.1-SNAPSHOT` como fallback controlado.
- Falhas de banco retornam mensagem legível no campo `databaseMessage`, sem stack trace bruto.

### Integração com módulos existentes

- Integração com `knowledgegraph` feita por inspeção do contexto Spring, sem dependência direta de API interna do módulo.
- Integração com segurança mantida no `SecurityConfig` existente; a sprint adicionou cobertura de teste para garantir que health/info permanecem públicos.
- Integração com Actuator mantida por configuração já existente no backend.

### Persistência e migrations

- Nenhuma migration Flyway criada.
- Nenhuma tabela, entidade persistente ou repository novo foi necessário.
- A única consulta de banco é diagnóstica (`select 1`) e não altera estado.

### Segurança

- Endpoint operacional detalhado permanece protegido por JWT.
- Endpoints públicos limitados a `/actuator/health` e `/actuator/info`.
- Não houve alteração no modelo de papéis, Keycloak, roles ou claims.

### Postman

- Atualizada a collection cumulativa em `postman/aegis-pms-sprint-07-core-postman-collection.json`.
- Gerado o arquivo canônico `postman/aegis-postman-collection.json`.
- Adicionada a pasta `06 - Health Status` com:
  - `GET {{baseUrl}}/actuator/health`
  - `GET {{baseUrl}}/api/v1/system/status`

## Qualidade

- `mvn clean verify`: BUILD SUCCESS
- Testes: 268 executados, 0 failures, 0 errors, 0 skipped
- JaCoCo: aprovado (`All coverage checks have been met`)
- Spring Modulith: aprovado via `ApplicationModules.of(AegisApplication.class).verify()`
- Sonar/SonarQube for IDE: não executado via CLI; sem alertas bloqueantes registrados na validação de integração/manual.
- Validação de integração/manual: confirmada pelo humano responsável antes do commit.

## Riscos encontrados

- O endpoint de status depende do contexto Spring para identificar o Knowledge Graph; se o nome do bean mudar em refatoração futura, a regra deve ser atualizada junto.
- `BuildProperties` pode não existir em execução local sem build metadata; o fallback controlado evita falha operacional.

## Retrofits pendentes

- Nenhum retrofit obrigatório identificado para a Sprint 09.

## Observações para próximas sprints

- Manter o endpoint `/api/v1/system/status` como diagnóstico autenticado e não transformá-lo em API pública.
- Caso novas integrações operacionais sejam adicionadas, preferir checks desacoplados de módulos internos.

## Conclusão

A Sprint 09 foi concluída com endpoint de status operacional, Postman atualizado, documentação consolidada, JaCoCo aprovado, auditoria Spring Modulith aprovada e validação de integração/manual confirmada antes do commit.
