# Sprint 13 — Domínio form/submission

## Resultado

Sprint implementada em `sprint/13-form-submission`, com domínio `form`/`submission`, contratos REST, persistence, module-gating, testes automatizados, Bruno e documentação.

## Objetivo

Implementar o domínio de formulários e submissions para que produtos possam criar, editar, publicar e configurar entrega de formulários, além de listar submissions recebidas, respeitando Product First, Contract First, REST First, Shared Schema, JSON Contracts, PostgreSQL, Keycloak, Spring Modulith e module-gating por `ModuleKey.FORMS`.

## Escopo entregue

- Definição de formulário com `fieldsJson`, `deliveryChannelsJson`, status/publication e catálogo fixo de tipos de campo.
- Submission vinculada a formulário publicado, com `answersJson` completo e vínculo de upload por `assetId`.
- Publicação com validações de campo obrigatório, labels duplicados e `Upload.acceptedFileTypes`.
- Configuração de delivery channels para `email`, `whatsapp`, `telegram` e `webhook`, apenas validada e persistida.
- Isolamento por produto retornando 404 para recursos fora do escopo.
- Module-gating com `@RequireModule(ModuleKey.FORMS)`, retornando 403 `MODULE_DISABLED` quando o módulo está desabilitado.

## Classes e arquivos criados/alterados

- Domínio e persistência: `FormDefinition`, `FormStatus`, `FormFieldType`, `Submission`, `SubmissionStatus`, `FormDefinitionRepository`, `SubmissionRepository`, migration `V7__forms_and_submissions.sql`.
- Contratos/API: `CreateFormDefinitionRequest`, `UpdateFormDefinitionRequest`, `UpdateFormDeliveryRequest`, `FormSummary`, `FormDetail`, `SubmissionSummary`, `SubmissionDetail`.
- Mappers: `FormDefinitionMapper`, `SubmissionMapper`.
- Casos de uso: `FormService`, `SubmissionService`, `FormPublicationPolicy`, `FormDeliveryPolicy`.
- Controllers e erros: `FormController`, `SubmissionController`, exceções/handlers de `form` e `submission`.
- APIs públicas entre módulos: `asset.api.AssetReferenceService`/`AssetReference`; `form.api.FormReferenceService`/`FormReference`/`FormFieldCatalog`.
- Bruno: pasta `bruno/13-forms` com requests para todos os endpoints da sprint, incluindo cenários de validação.
- Documentação: `docs/sprints/backend/SPRINT-RESULTADO.md`, `docs/sprints/backend/results/sprint-13.md`, `docs/sprints/backend/13_dominio_forms.md`, `docs/trace/00_endpoints_esperados.md` e `docs/api-testing/README.md`.
- Ajustes SonarQube for IDE: testes de exceção refatorados para `assertThatThrownBy` com apenas uma chamada na lambda; `instanceof` ajustado para unnamed pattern quando aplicável; casts/suppressions removidos sem alterar comportamento.

## Endpoints confirmados

- `GET /api/v1/products/{productId}/forms`
- `POST /api/v1/products/{productId}/forms`
- `GET /api/v1/products/{productId}/forms/{formId}`
- `PUT /api/v1/products/{productId}/forms/{formId}`
- `GET /api/v1/products/{productId}/forms/field-types`
- `GET /api/v1/products/{productId}/forms/{formId}/submissions`
- `GET /api/v1/products/{productId}/forms/submissions`
- `GET /api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}`
- `POST /api/v1/products/{productId}/forms/{formId}/publish`
- `PUT /api/v1/products/{productId}/forms/{formId}/delivery`

## Regras de negócio implementadas

- `field-types` é catálogo fixo do sistema, exposto por API para evitar hardcode no frontend.
- `publish` rejeita formulário sem ao menos um campo obrigatório.
- `publish` rejeita labels duplicados em `fieldsJson`.
- Campo `Upload` exige `acceptedFileTypes` não vazio.
- Submission só pode ser criada contra formulário publicado.
- Upload em submission guarda `assetId` em `answersJson` e valida produto/MIME via API pública de `asset`.
- Delivery channel `webhook` exige URL válida.
- Delivery channel `telegram` exige `chatId` e `botToken`.
- Delivery channel `email` e `whatsapp` são validados e persistidos, sem envio real nesta sprint.
- Recursos de outro produto retornam 404, nunca 403.
- Produto com módulo `FORMS` desabilitado retorna 403 `MODULE_DISABLED`.

## Migration

- `V7__forms_and_submissions.sql` cria as tabelas de `forms` e `submissions`, com JSONB para `fieldsJson`, `deliveryChannelsJson` e `answersJson`, índices por produto/formulário e constraints de integridade compatíveis com Shared Schema.

## Decisões tomadas

- `POST /api/v1/products/{productId}/forms` foi adicionado ao contrato da sprint. A lista inicial tinha `GET`/`PUT`, mas o produto precisa criar formulários novos pela UI; sem `POST`, a aplicação dependeria de seed manual.
- `fieldsJson`, `deliveryChannelsJson` e `answersJson` foram persistidos como JSONB via Hibernate `@JdbcTypeCode(SqlTypes.JSON)`, mantendo Shared Schema e PostgreSQL.
- Upload em submission salva `assetId` em `answersJson`; a validação usa somente `asset.api`, sem acoplar `submission` a entidades/repositories internos de `asset`.
- `form` expõe `form.api` para `submission`. `form` não depende de `submission`, evitando ciclo no Spring Modulith. Por isso `responses`, `conversion` e `lastActivity` retornam valores neutros nesta sprint.
- Delivery valida e persiste `email`, `whatsapp`, `telegram` e `webhook`, mas não envia nada de verdade.
- O detalhe feliz de submission é coberto por testes automatizados de service/mapper; na Bruno collection foi validado o 404 controlado, porque esta sprint não expõe endpoint público de criação de submission.

## Bugs encontrados e corrigidos

- Ausência de endpoint de criação de formulário no escopo inicial: corrigida com `POST /api/v1/products/{productId}/forms` e registrada no contrato/documentação.
- Ciclo modular potencial `form -> submission -> form`: evitado removendo dependência de `form` para `submission` e mantendo estatísticas de resumo como valores neutros até read model futuro.
- Apontamentos SonarQube for IDE da Sprint 13 corrigidos sem alterar regra de negócio: 27 ocorrências `java:S5778` em testes e 3 usos diretos de `java:S7467` em `SubmissionService`, além de usos equivalentes nos services/policies de forms.

## Retrofits pendentes

- Criar um read model/evento para atualizar contagem de responses, conversão e última atividade dos forms sem introduzir dependência cíclica `form -> submission`.
- Quando existir endpoint público de submissão, adicionar fluxo Bruno feliz para criar submission e consultar detalhe com `answersJson` completo.
- Quando o domínio consumidor remover/alterar asset referenciado em answers, avaliar se será necessário registrar `AssetUsage` para uploads de forms.

## Riscos conhecidos

- Métricas de `FormSummary` (`responses`, `conversion`, `lastActivity`) ainda são valores neutros por decisão arquitetural para preservar o Spring Modulith sem ciclo.
- Bruno ainda não cobre um caminho feliz de criação de submission porque a sprint não expôs endpoint público de submissão; a cobertura funcional está nos testes automatizados.
- Delivery channels são apenas configuração persistida; envio real por e-mail, WhatsApp, Telegram e webhook depende de sprint futura de integrações.

## Evidências de testes

- `mvn -Dtest='FormDefinitionRepositoryTest,SubmissionRepositoryTest' test`: BUILD SUCCESS, 7 testes.
- `mvn -Dtest='FormDefinitionMapperTest,SubmissionMapperTest' test`: BUILD SUCCESS, 12 testes.
- `mvn -Dtest='FormPublicationPolicyTest,FormDeliveryPolicyTest,FormServiceTest,SubmissionServiceTest' test`: BUILD SUCCESS, 32 testes.
- `mvn -Dtest='FormControllerTest,SubmissionControllerTest' test`: BUILD SUCCESS, 19 testes.
- `mvn -Dtest='FormServiceTest,SubmissionServiceTest,ModulithArchitectureTest' test`: BUILD SUCCESS, Spring Modulith aprovado.
- `mvn clean verify`: BUILD SUCCESS, 720 testes, 0 failures, 0 errors, 0 skipped, JaCoCo aprovado (`All coverage checks have been met`), Spring Modulith aprovado.
- Conferência Sonar local: padrões `@SuppressWarnings`, `String.class::isInstance` e lambdas com builders dentro de `assertThatThrownBy` removidos dos arquivos afetados.

## Evidência Bruno

- `cd bruno && npx @usebruno/cli run --env local`: PASS, 102 requests executados, 102 aprovados, 194/194 testes aprovados.
