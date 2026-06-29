# Sprint 19 — Read Model de Formulários, Submit Público e Configurações de Segurança

Concluída em 2026-06-29 na branch `sprint/19-read-model-forms-submit-security`.

## Objetivo

Fechar três lacunas reais das etapas anteriores: popular o read model de formulários sem criar ciclo entre `form` e `submission`, expor submit público para formulários publicados, e implementar configurações reais de segurança do produto. A sprint também limpou diretórios estruturais vazios e explicitou encoding UTF-8 no Maven.

## Resultado alcançado

A sprint entregou a migration `V11__forms_read_model_and_security_settings.sql`, três endpoints REST, read model transacional pós-commit, submit público sem autenticação, rate limiting em memória, configurações de segurança por produto, Bruno cumulativo, cobertura automatizada e documentação de continuidade.

## Classes criadas

- `br.com.byop.aegis.form.api.FormResponseReadModelService`
- `br.com.byop.aegis.settings.contract.UpdateProductSecuritySettingsRequest`
- `br.com.byop.aegis.settings.controller.ProductSecuritySettingsController`
- `br.com.byop.aegis.settings.domain.ProductSecuritySettings`
- `br.com.byop.aegis.settings.dto.ProductSecuritySettingsResponse`
- `br.com.byop.aegis.settings.mapper.ProductSecuritySettingsMapper`
- `br.com.byop.aegis.settings.repository.ProductSecuritySettingsRepository`
- `br.com.byop.aegis.settings.service.ProductSecuritySettingsService`
- `br.com.byop.aegis.submission.api.SubmissionReceivedEvent`
- `br.com.byop.aegis.submission.controller.PublicSubmissionController`
- `br.com.byop.aegis.submission.exception.TooManySubmissionsException`
- `br.com.byop.aegis.submission.service.FormResponseCountListener`
- `br.com.byop.aegis.submission.service.PublicSubmissionRateLimiter`
- `br.com.byop.aegis.form.api.FormResponseReadModelServiceTest`
- `br.com.byop.aegis.settings.controller.ProductSecuritySettingsControllerTest`
- `br.com.byop.aegis.settings.mapper.ProductSecuritySettingsMapperTest`
- `br.com.byop.aegis.settings.repository.ProductSecuritySettingsRepositoryTest`
- `br.com.byop.aegis.settings.service.ProductSecuritySettingsServiceTest`
- `br.com.byop.aegis.submission.controller.PublicSubmissionControllerTest`
- `br.com.byop.aegis.submission.service.FormResponseCountListenerTest`
- `br.com.byop.aegis.submission.service.PublicSubmissionRateLimiterTest`

## Classes alteradas

- `br.com.byop.aegis.form.domain.FormDefinition`
- `br.com.byop.aegis.form.mapper.FormDefinitionMapper`
- `br.com.byop.aegis.form.service.FormService`
- `br.com.byop.aegis.security.SecurityConfig`
- `br.com.byop.aegis.submission.exception.SubmissionExceptionHandler`
- `br.com.byop.aegis.submission.service.SubmissionService`
- `br.com.byop.aegis.form.mapper.FormDefinitionMapperTest`
- `br.com.byop.aegis.form.repository.FormDefinitionRepositoryTest`
- `br.com.byop.aegis.form.service.FormServiceTest`
- `br.com.byop.aegis.security.SecurityConfigTest`
- `br.com.byop.aegis.submission.service.SubmissionServiceTest`
- `backend/pom.xml`

## Migrations criadas

- `backend/src/main/resources/db/migration/V11__forms_read_model_and_security_settings.sql`

A migration adiciona `response_count` e `last_activity_at` em `form_definitions` e cria `product_security_settings` com `product_id` como PK/FK para `products(id)` e `ON DELETE CASCADE`.

## Endpoints confirmados

- `POST /api/v1/products/{productId}/forms/{formId}/submit`
- `GET /api/v1/products/{productId}/settings/security`
- `PUT /api/v1/products/{productId}/settings/security`

## Contratos REST criados/alterados

- Criado `UpdateProductSecuritySettingsRequest` para `PUT /settings/security`.
- Criado `ProductSecuritySettingsResponse` sem exposição de `webhookSecret`.
- Reutilizado `SubmitFormCommand` no submit público.
- `FormSummary` passou a receber dados reais de `responses` e `lastActivity`; `conversion` permanece `"—"`.
- `InvalidSubmissionException` passou a ser serializada como 422 no domínio de submission.
- `TooManySubmissionsException` retorna 429 com erro `TOO_MANY_SUBMISSIONS`.

## Decisões de implementação

- O read model é atualizado por evento publicado pelo módulo `submission` após persistir a submissão.
- O listener ficou em `submission.service` e chama `form.api.FormResponseReadModelService`. A alternativa literal de colocar o listener no módulo `form` importando `submission.api` criava ciclo no Spring Modulith, porque `submission` já depende de `form.api`.
- `FormResponseCountListener` usa `@TransactionalEventListener(phase = AFTER_COMMIT)` e transação `REQUIRES_NEW`, para só incrementar após commit da submissão.
- `conversion` permanece neutro como `"—"`, porque conversão real depende de tracking/analytics de tráfego.
- `lastActivity` usa formato relativo simples em pt-BR e retorna `"Nenhuma resposta ainda"` sem atividade.
- Submit público usa controller separado, `permitAll` no `SecurityConfig` e `@RequireModule(ModuleKey.FORMS)`.
- Rate limit é em memória, por IP + formulário, limite 10 submissões por hora.
- Erro de form não publicado foi preservado como `SUBMISSION_FORM_NOT_PUBLISHED`, mantendo compatibilidade com a Sprint 13.
- `ProductSecuritySettings` é lazy-created no primeiro GET.
- `webhookSecret` é persistido e preservado quando o PUT recebe segredo vazio/nulo, mas nunca retorna no response.
- `GET/PUT /settings/security` aceitam somente `ROLE_SUPER_ADMIN` e `ROLE_TENANT_ADMIN`.
- Encoding Maven passou a declarar `project.build.sourceEncoding` e `project.reporting.outputEncoding` em UTF-8.

## Regras de negócio implementadas

- Submit público funciona sem header `Authorization`.
- Formulário precisa estar publicado.
- `formId` precisa pertencer ao `productId`.
- Campos obrigatórios precisam estar preenchidos.
- Campo obrigatório vazio, ausente, string em branco, lista vazia ou `answers = null` retorna `MISSING_REQUIRED_FIELD`.
- Submissão válida publica `SubmissionReceivedEvent`.
- Read model incrementa `responseCount` e atualiza `lastActivityAt` somente após commit.
- Mais de 10 submissões do mesmo IP para o mesmo form dentro de 1 hora retorna `TOO_MANY_SUBMISSIONS`.
- `webhookSecret` nunca é serializado.
- Analytics habilitado sem provider key gera status derivado de atenção.
- `ProductSecuritySettings` é criado sob demanda no primeiro GET.

## Segurança

- `POST /api/v1/products/*/forms/*/submit` foi liberado como endpoint público.
- Demais endpoints de `/api/v1/**` permanecem autenticados.
- Configurações de segurança são restritas a SUPER_ADMIN e TENANT_ADMIN.
- `webhookSecret` é protegido no response e testado para não aparecer no JSON.
- A sprint não alterou contrato de autenticação, Keycloak, CORS, migrations antigas nem UI de login.

## Rate limiting

`PublicSubmissionRateLimiter` é um bean Spring com estado de runtime (`ConcurrentHashMap`) e `Clock` injetável em teste. O limite é 10 submissões por IP/form/hora. Na correção final de Sonar, o construtor público usado pelo Spring foi marcado explicitamente com `@Autowired`, mantendo o construtor package-private com `Clock` apenas para testes.

## Read model de formulários

`FormDefinition` ganhou `responseCount` e `lastActivityAt`. `SubmissionService` publica `SubmissionReceivedEvent` ao salvar uma submissão. `FormResponseCountListener` processa o evento pós-commit e chama `FormResponseReadModelService`, que registra a resposta no formulário. `FormDefinitionMapper` transforma o contador em string e formata a última atividade relativa.

## Configurações de segurança do produto

`ProductSecuritySettings` persiste `webhookUrl`, `webhookSecret`, `analyticsEnabled`, `analyticsProviderKey`, `emailDeliveryEnabled` e `updatedAt`. O response expõe apenas dados não secretos e status derivados: `webhookStatus`, `analyticsStatus` e `emailStatus`.

## Bruno criado/alterado

Criados em `bruno/13-forms`:

- `submit-publico-sucesso.bru`
- `submit-publico-form-nao-publicado-422.bru`
- `submit-publico-campo-obrigatorio-422.bru`
- `listar-forms-read-model-apos-submit.bru`

Criados em `bruno/19-settings-security`:

- `folder.bru`
- `consultar-security-settings.bru`
- `atualizar-security-settings.bru`
- `atualizar-security-settings-analytics-sem-chave.bru`

## Documentação alterada

- `AGENTS.md`: padrões preventivos Sonar para relógio real em testes, campos privados mortos, lambdas de exceção, classes utilitárias versus beans com estado e construtor `@Autowired` em beans Spring com múltiplos construtores.
- `docs/api-testing/README.md`: pasta 19 e variável `publicSubmissionId`.
- `docs/sprints/backend/SPRINT-RESULTADO.md`: entrada cumulativa da Etapa 19.
- `docs/sprints/backend/results/sprint-19.md`: este relatório autocontido.

## SonarQube for IDE

Corrigidos apontamentos reais sem `@SuppressWarnings`, sem `NOSONAR` e sem desabilitar regra:

- `java:S8692`: testes deixaram de usar relógio do sistema, usando timestamps fixos e overload testável.
- `java:S1068`: removido campo privado morto em teste.
- `java:S5778`: lambdas de assertions de exceção passaram a conter somente a chamada sob teste.
- `java:S6829`: `PublicSubmissionRateLimiter` foi mantido como bean stateful, com construtor de injeção explícito via `@Autowired`.

Observação: `SecurityConfig` já possuía `@SuppressWarnings({"java:S4502", "java:S112"})` antes desta sprint e isso não foi alterado.

## Comandos executados

- `mvn -Dtest='SubmissionServiceTest,PublicSubmissionControllerTest,FormDefinitionMapperTest' test`
- `mvn -Dtest='FormDefinitionMapperTest,FormResponseCountListenerTest,ProductSecuritySettingsServiceTest' test`
- `mvn -Dtest='PublicSubmissionControllerTest,PublicSubmissionRateLimiterTest' test`
- `mvn jacoco:report`
- `mvn clean verify`
- `mvn spring-boot:run`
- `cd bruno && npx @usebruno/cli run --env local`
- `git status`
- `git diff --stat`
- `git diff`
- `git log --oneline --decorate -n 10`

## Resultado do `mvn clean verify`

Última execução obrigatória antes da documentação final:

- Comando: `cd backend && mvn clean verify`
- Resultado: `BUILD SUCCESS`
- Tests run: 1001
- Failures: 0
- Errors: 0
- Skipped: 0
- JaCoCo: `All coverage checks have been met`
- Spring Modulith: aprovado via suíte completa

## Resultado do Bruno

Última execução obrigatória antes da documentação final:

- Backend local: `mvn spring-boot:run` em `localhost:8080`
- Comando: `cd bruno && npx @usebruno/cli run --env local`
- Status: PASS
- Requests: 163 executados, 163 aprovados
- Tests: 318/318 aprovados
- Duration: 21311 ms

## Bugs reais encontrados

- Listener literal em `form` importando `submission.api` criava ciclo Spring Modulith (`submission -> form` e `form -> submission`). Corrigido mantendo o listener em `submission.service` e expondo atualização por `form.api`.
- Listener genérico recebendo `Object` fazia o Spring Modulith tentar serializar eventos internos não relacionados. Corrigido com listener tipado para `SubmissionReceivedEvent`.
- MapStruct tentou usar helper de status como conversor genérico `String -> String` no mapper de security settings. Corrigido com mappings explícitos.
- Uma validação Bruno inicial usou uma instância antiga do backend. A validação final registrada neste relatório foi refeita em `localhost:8080` com a branch atual.

## Retrofits pendentes

- Migrar rate limit para Redis/storage centralizado se o Aegis rodar com múltiplas instâncias.
- Implementar conversão real de formulários quando existir tracking/analytics de tráfego.
- Ajustar o frontend para usar `PUT /api/v1/products/{productId}/settings/security` no lugar do path antigo `PUT /api/v1/admin/settings/security`.

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/form/api/FormResponseReadModelService.java`
- `backend/src/main/java/br/com/byop/aegis/settings/contract/UpdateProductSecuritySettingsRequest.java`
- `backend/src/main/java/br/com/byop/aegis/settings/controller/ProductSecuritySettingsController.java`
- `backend/src/main/java/br/com/byop/aegis/settings/domain/ProductSecuritySettings.java`
- `backend/src/main/java/br/com/byop/aegis/settings/dto/ProductSecuritySettingsResponse.java`
- `backend/src/main/java/br/com/byop/aegis/settings/mapper/ProductSecuritySettingsMapper.java`
- `backend/src/main/java/br/com/byop/aegis/settings/repository/ProductSecuritySettingsRepository.java`
- `backend/src/main/java/br/com/byop/aegis/settings/service/ProductSecuritySettingsService.java`
- `backend/src/main/java/br/com/byop/aegis/submission/api/SubmissionReceivedEvent.java`
- `backend/src/main/java/br/com/byop/aegis/submission/controller/PublicSubmissionController.java`
- `backend/src/main/java/br/com/byop/aegis/submission/exception/TooManySubmissionsException.java`
- `backend/src/main/java/br/com/byop/aegis/submission/service/FormResponseCountListener.java`
- `backend/src/main/java/br/com/byop/aegis/submission/service/PublicSubmissionRateLimiter.java`
- `backend/src/main/resources/db/migration/V11__forms_read_model_and_security_settings.sql`
- `backend/src/test/java/br/com/byop/aegis/form/api/FormResponseReadModelServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/controller/ProductSecuritySettingsControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/mapper/ProductSecuritySettingsMapperTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/repository/ProductSecuritySettingsRepositoryTest.java`
- `backend/src/test/java/br/com/byop/aegis/settings/service/ProductSecuritySettingsServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/submission/controller/PublicSubmissionControllerTest.java`
- `backend/src/test/java/br/com/byop/aegis/submission/service/FormResponseCountListenerTest.java`
- `backend/src/test/java/br/com/byop/aegis/submission/service/PublicSubmissionRateLimiterTest.java`
- `bruno/13-forms/listar-forms-read-model-apos-submit.bru`
- `bruno/13-forms/submit-publico-campo-obrigatorio-422.bru`
- `bruno/13-forms/submit-publico-form-nao-publicado-422.bru`
- `bruno/13-forms/submit-publico-sucesso.bru`
- `bruno/19-settings-security/folder.bru`
- `bruno/19-settings-security/consultar-security-settings.bru`
- `bruno/19-settings-security/atualizar-security-settings.bru`
- `bruno/19-settings-security/atualizar-security-settings-analytics-sem-chave.bru`
- `docs/sprints/backend/results/sprint-19.md`

## Arquivos alterados

- `AGENTS.md`
- `backend/pom.xml`
- `backend/src/main/java/br/com/byop/aegis/form/domain/FormDefinition.java`
- `backend/src/main/java/br/com/byop/aegis/form/mapper/FormDefinitionMapper.java`
- `backend/src/main/java/br/com/byop/aegis/form/service/FormService.java`
- `backend/src/main/java/br/com/byop/aegis/security/SecurityConfig.java`
- `backend/src/main/java/br/com/byop/aegis/submission/exception/SubmissionExceptionHandler.java`
- `backend/src/main/java/br/com/byop/aegis/submission/service/SubmissionService.java`
- `backend/src/test/java/br/com/byop/aegis/form/mapper/FormDefinitionMapperTest.java`
- `backend/src/test/java/br/com/byop/aegis/form/repository/FormDefinitionRepositoryTest.java`
- `backend/src/test/java/br/com/byop/aegis/form/service/FormServiceTest.java`
- `backend/src/test/java/br/com/byop/aegis/security/SecurityConfigTest.java`
- `backend/src/test/java/br/com/byop/aegis/submission/service/SubmissionServiceTest.java`
- `docs/api-testing/README.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`

## Arquivos removidos

Nenhum arquivo versionado foi removido. Durante a sprint, foram eliminados diretórios vazios não versionados sob `backend/src/main/java/br/com/byop/aegis/`: `seo`, `contract`, `config` e `integration`.
