# Sprint 24 — OpenAPI/Swagger, testes e checklist final

Concluida em 2026-07-02 na branch `sprint/24-openapi-testes-checklist-final`.

## Objetivo

Habilitar a documentacao OpenAPI/Swagger do backend nos perfis `local` e `dev`, manter a mesma superficie desabilitada em `prod`, expor Bearer JWT na UI, organizar as tags por modulo na ordem canonica e validar a configuracao com testes automatizados e Bruno.

## Resultado alcancado

Criada a configuracao `OpenApiConfig`, ativa apenas nos profiles `local` e `dev`, com metadata da API REST administrativa (`Aegis PMS API`, versao `v1`), servidor local (`http://localhost:8080`), security scheme HTTP bearer JWT (`bearer-jwt`) e tags na ordem canonica:

```txt
System
Auth
Tenants
Products
Product Modules
Product Assignments
Knowledge Graph
Content
Pages
Assets
Forms
Submissions
Analytics
Users
Audit
Settings
Dashboard
```

As properties `springdoc.api-docs.enabled` e `springdoc.swagger-ui.enabled` ficaram desabilitadas por padrao em `application.yml`, habilitadas em `application-local.yml` e `application-dev.yml`, e explicitamente desabilitadas em `application-prod.yml`.

## Arquivos criados ou alterados

### Classes criadas

- `OpenApiConfig`
- `OpenApiConfigTest`

### Classes alteradas

- Nenhuma classe existente foi alterada. A sprint adicionou uma nova classe de configuracao OpenAPI e seu teste dedicado.

### Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/api/openapi/OpenApiConfig.java`
- `backend/src/test/java/br/com/byop/aegis/api/openapi/OpenApiConfigTest.java`
- `backend/src/main/resources/application-dev.yml`
- `bruno/24-openapi-testes-checklist-final/folder.bru`
- `bruno/24-openapi-testes-checklist-final/swagger-ui-local.bru`
- `bruno/24-openapi-testes-checklist-final/openapi-json-completo.bru`
- `docs/sprints/backend/results/sprint-24.md`

### Arquivos alterados

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml`
- `backend/src/main/resources/application-prod.yml`
- `docs/api-testing/README.md`
- `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md`
- `AGENTS.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

## Endpoints e contratos confirmados

### Endpoints confirmados

- `GET /swagger-ui/index.html` em profile `local`: 200, HTML da Swagger UI.
- `GET /v3/api-docs` em profile `local`: 200, JSON OpenAPI.

### Contratos OpenAPI/Swagger confirmados

- Swagger/OpenAPI habilitado em `local` e `dev`.
- Swagger/OpenAPI desabilitado em `prod`.
- Bearer JWT presente como security scheme `bearer-jwt`.
- UI do Swagger permite o fluxo Bearer JWT.
- Tags esperadas presentes na ordem canonica.
- Operacoes sem `summary`, `description` ou `responses` sao rejeitadas pelo teste Bruno.

## Decisoes de implementacao registradas

1. **Swagger/OpenAPI via profile e properties.** A classe `OpenApiConfig` so entra no contexto em `local`/`dev`; as properties Springdoc tambem seguem o mesmo contrato, evitando exposicao acidental em `prod`.
2. **Operacoes documentadas por customizador.** `OperationCustomizer` classifica cada operacao pelo path, aplica tag, summary, description, respostas padronizadas e security por operacao. Endpoints publicos (`System`, `Auth` e submissao publica) ficam sem requirement Bearer na operacao; demais endpoints herdam Bearer JWT.
3. **Tags como contrato centralizado.** A lista de tags foi centralizada em `ORDERED_TAGS`/`orderedTagNames()` e cada nome virou constante semantica (`TAG_PRODUCTS`, `TAG_AUTH`, etc.), para evitar duplicacao de literais e proteger a ordem.
4. **Sem alterar contratos REST existentes.** Nenhum controller, endpoint, payload, migration, profile de negocio ou regra de autorizacao foi alterado; a sprint adiciona apenas documentacao/configuracao OpenAPI, properties Springdoc, testes e Bruno.
5. **Teste de profile sem subir a aplicacao inteira.** `OpenApiConfigTest` usa `AnnotationConfigApplicationContext` para validar presenca dos beans por profile e `YamlPropertiesFactoryBean` para validar as properties dos resources, mantendo o teste focado e rapido.

## Rodada de correcao SonarQube for IDE

O arquivo `OpenApiConfig.java` recebeu uma rodada dedicada de correcao Sonar apos a implementacao inicial:

- **`java:S1192`**: os literais duplicados das tags OpenAPI (`System`, `Auth`, `Tenants`, `Products`, `Product Modules`, `Product Assignments`, `Knowledge Graph`, `Content`, `Pages`, `Assets`, `Forms`, `Submissions`, `Analytics`, `Users`, `Audit`, `Settings`, `Dashboard`) foram extraidos para constantes semanticas.
- **`java:S3923`**: a condicional redundante de status HTTP, que retornava o mesmo valor em ambos os ramos, foi removida sem alterar o comportamento.

Nenhuma correcao usou `@SuppressWarnings`, `NOSONAR`, desativacao de regra ou justificativa de falso positivo. `AGENTS.md` e `00_padrao_qualidade_e_arquitetura.md` foram atualizados para prevenir recorrencia: tags OpenAPI centralizadas, lista fixa unica, sem condicionais redundantes e testes por profile quando OpenAPI depender de ambiente.

## Testes automatizados

- `mvn clean test`: **BUILD SUCCESS**, 1238 testes, 0 falhas, 0 erros, 0 ignorados.
- `mvn -Dtest=OpenApiConfigTest test`: **BUILD SUCCESS**, 8 testes, 0 falhas, 0 erros.
- `mvn clean verify`: **BUILD SUCCESS**, 1238 testes, 0 falhas, 0 erros, 0 ignorados.
- JaCoCo: `All coverage checks have been met`.
- Spring Modulith: `ModulithArchitectureTest` aprovado, 1 teste, 0 falhas, 0 erros.

`OpenApiConfigTest` cobre:

- bean OpenAPI criado no profile `local`;
- bean OpenAPI criado no profile `dev`;
- beans OpenAPI ausentes no profile `prod`;
- properties Springdoc habilitadas apenas em `local`/`dev` e desabilitadas em `prod`;
- Bearer JWT presente;
- tags esperadas presentes e na ordem correta;
- classificacao de endpoints protegidos, `Auth` publico e submissao antes de `Forms`;
- responses e security por operacao.

Nenhum teste existente foi removido. A suite ganhou apenas o teste dedicado de configuracao OpenAPI.

## Bruno

Criada a pasta `bruno/24-openapi-testes-checklist-final` com:

- `swagger-ui-local.bru`: valida `GET {{baseUrl}}/swagger-ui/index.html` com 200 e HTML da Swagger UI.
- `openapi-json-completo.bru`: valida `GET {{baseUrl}}/v3/api-docs`, metadata, servidor local, Bearer JWT, tags na ordem canonica, summaries, descriptions e responses das operacoes.

Validacao contra aplicacao real em profile `local`:

```txt
24-openapi-testes-checklist-final: 2 requests, 2 aprovados, 7/7 testes aprovados.
Collection cumulativa: 220 requests, 220 aprovados, 434/434 testes aprovados.
```

## Bugs encontrados

Nenhum bug funcional novo foi encontrado na sprint. A rodada Sonar apontou dois problemas estruturais em `OpenApiConfig` (`java:S1192` e `java:S3923`), corrigidos sem alterar contrato ou comportamento.

## Validacoes adicionais

```bash
cd backend
mvn clean test
mvn -Dtest=OpenApiConfigTest test
mvn clean verify
grep -rn "@SuppressWarnings\|NOSONAR" src/main src/test
grep -rn "@MockBean\|@SpyBean" src/test
git diff --check
git status --short
git diff --stat

cd ../bruno
npx @usebruno/cli run --env local
```

O grep obrigatorio encontrou somente ocorrencias preexistentes fora do escopo desta sprint:

```txt
src/main/java/br/com/byop/aegis/security/SecurityConfig.java:30:    @SuppressWarnings({"java:S4502", "java:S112"})
src/main/java/br/com/byop/aegis/audit/service/AuditEventQueryService.java:138:    @SuppressWarnings("unchecked")
```

Nenhuma nova ocorrencia foi adicionada em `OpenApiConfig`, testes de OpenAPI ou arquivos alterados nesta sprint.

`grep -rn "@MockBean\|@SpyBean" src/test` nao retornou ocorrencias.

## Documentacao de padroes atualizada

- `AGENTS.md`: padroes preventivos para tags OpenAPI centralizadas, condicionais redundantes (`java:S3923`) e testes OpenAPI por profile.
- `docs/sprints/backend/00_padrao_qualidade_e_arquitetura.md`: nova secao `1.1 OpenAPI/Swagger por profile`.
- `docs/api-testing/README.md`: registro da pasta Bruno `24-openapi-testes-checklist-final`.

## Criterios de aceite atendidos

- [x] Swagger funcional em `local` e `dev`.
- [x] Swagger/OpenAPI desabilitado em `prod`.
- [x] Bearer JWT mantido na UI.
- [x] Tags esperadas presentes e na ordem correta.
- [x] `OpenApiConfig.java` sem os apontamentos Sonar reportados (`java:S3923`, `java:S1192`).
- [x] `mvn clean verify` com BUILD SUCCESS.
- [x] JaCoCo aprovado.
- [x] Spring Modulith aprovado.
- [x] Bruno atualizado e validado.
- [x] Documentacao de padroes atualizada com a licao aprendida.

## Retrofits pendentes

Nenhum retrofit obrigatorio identificado para OpenAPI. Uma sprint futura pode enriquecer schemas/examples por dominio, mas isso nao e necessario para o criterio de aceite desta etapa.
