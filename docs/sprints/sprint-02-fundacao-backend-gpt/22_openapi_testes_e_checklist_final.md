# Etapa 22 — OpenAPI/Swagger, testes mínimos e checklist final do servidor

> Cole este arquivo inteiro numa conversa nova do GPT. Última etapa — pré-requisito: todas as etapas 01-21 concluídas.

## Contexto fixo

Última etapa da Sprint 02: documentar a API completa (fundação + todos os domínios de produto), garantir uma cobertura mínima de testes nas regras críticas, e validar com um checklist único que tudo está de fato funcionando de ponta a ponta — inclusive os domínios adicionados nas etapas 09-17 e 21 (`content`, `assets`, `forms`, `analytics`, `users`, `audit`, `settings`, `dashboard`, `ProductAssignment`, `pages`).

## Objetivo

Swagger funcionando em dev, suíte de testes mínima passando, e checklist final do servidor 100% marcado.

## Tarefas

### A. OpenAPI/Swagger

- Habilitado em `local`/`dev`, desabilitado em `prod`.
- Bearer JWT configurado na UI do Swagger (permite colar o token e testar endpoints autenticados).
- Tags por módulo: `System`, `Auth`, `Tenants`, `Products`, `Product Modules`, `Product Assignments`, `Knowledge Graph`, `Content`, `Pages`, `Assets`, `Forms`, `Submissions`, `Analytics`, `Users`, `Audit`, `Settings`, `Dashboard`.
- Acessível em `http://localhost:8080/swagger-ui/index.html`.

### B. Testes mínimos

Classes (fundação): `TenantServiceTest`, `ProductServiceTest`, `ProductModuleServiceTest`, `KnowledgeGraphServiceTest`, `GraphConsistencyPolicyTest`, `AuthenticatedUserProviderTest`, e um smoke test de security.

Classes (domínios, etapas 09-17 e 21): `ProductAssignmentServiceTest`, `ContentServiceTest` + `ContentWorkflowPolicyTest`, `AssetServiceTest`, `FormServiceTest` + `SubmissionServiceTest` + `FormDeliveryValidationTest`, `AnalyticsServiceTest`, `UserServiceTest`, `AuditServiceTest`, `SettingsServiceTest`, `PageServiceTest` + `SectionContentValidationTest` + `ProductGlobalsServiceTest`.

Cenários obrigatórios (fundação): criar tenant; criar produto (com `assetStorageStrategy` local e s3, confirmando que só `local` provisiona pasta); bloquear produto de tenant alheio; habilitar módulo válido; rejeitar módulo inválido; criar node; criar edge válida; rejeitar edge com node inexistente; rejeitar edge cross-tenant; listar neighbors; `/api/v1/me` sem token retorna 401; subir o backend com `AEGIS_STORAGE_LOCAL_PATH` inexistente e confirmar criação automática da pasta.

Cenários obrigatórios (domínios): editar/excluir tenant; `super_admin` lista todos os tenants, demais papéis só os seus; atribuir produto a usuário existente e a um convite novo (nunca os dois ao mesmo tempo); transição de workflow de conteúdo inválida é rejeitada (`Draft → Published` direto, por exemplo); upload de asset de qualquer tipo (imagem, PDF, áudio) e exclusão; criar/editar formulário, configurar entrega por múltiplos canais e listar submissions; convidar usuário; consultar trilha de auditoria; salvar configurações de produto/tenant; criar página com seções, reordenar seções, rejeitar seção com `type` fora do catálogo (incluindo `footer`/`navbar`, removidos do catálogo), rejeitar sub-bloco fora de `acceptsChildren`, validar bloco `contact` exigindo `formId` real e bloco `audio` exigindo `fileAssetId`/`spotifyUrl` válidos; salvar `ProductGlobals` e confirmar upsert; consultar `/graph/nodes/{nodeId}/preview` e receber o shape leve.

### C. Checklist final do servidor funcional

Confirmar, nesta ordem, tudo o que foi construído nas etapas 01-21:

**Infra**: `docker compose up -d` sobe tudo · `aegis-postgres` healthy · `keycloak-postgres` healthy · `keycloak` acessível · `backend` acessível · volumes existem (incluindo `aegis_assets_data`, etapa 19) · dados persistem após restart · pasta de storage local existe automaticamente tanto containerizado quanto via `mvn spring-boot:run` direto (etapa 04), em qualquer SO.

**Keycloak**: realm `aegis` existe · client `aegis-web` existe · as 5 roles globais existem, todas com prefixo `AEGIS_` (`AEGIS_SUPER_ADMIN`, `AEGIS_TENANT_ADMIN`, `AEGIS_PRODUCT_MANAGER`, `AEGIS_EDITOR`, `AEGIS_VIEWER` — ADR-0014, nunca "CONTENT_EDITOR" ou variante) · os 5 usuários de teste existem e persistem após restart · token pode ser emitido para cada um · OpenID config acessível.

**Backend**: `/actuator/health` UP · `/api/v1/me` sem token = 401, com token = 200, e devolve `role` singular minúsculo (`"super_admin"`, nunca array/maiúsculo — ADR-0014) · Flyway criou as tabelas · Swagger abre em local · API usa `/api/v1`.

**Autorização — papéis e módulos (ADR-0014/ADR-0015)**: `JwtRoleConverter` produz `ROLE_<NOME>` a partir de `AEGIS_<NOME>` do token · todo controller de domínio gateável por módulo (`07`/`17` Knowledge Graph, `10` content, `11` assets, `12` forms, `13` analytics, `21` pages) está anotado com `@RequireModule` e retorna 403 `MODULE_DISABLED` quando o módulo correspondente está desabilitado no produto, mesmo para `SUPER_ADMIN` · todo domínio com entidade escopada por tenant/produto (`09` a `17`, `21`, `23`) retorna 404 — nunca 403 — para recurso fora do escopo do usuário autenticado.

**Core**: tenant pode ser criado/editado/excluído · `super_admin` vê todos os tenants · produto pode ser criado · produto pode habilitar módulo · listagem respeita membership · produto alheio não é acessível.

**Fluxo Super Admin**: criar tenant → criar produto → atribuir produto a um usuário (existente e por convite) funciona de ponta a ponta via API, espelhando o wizard de 3 passos do frontend (`CreateTenantWizardModal.tsx`).

**Domínios de produto**: `content`, `assets`, `forms`/`submissions`, `analytics`, `users`, `audit`, `settings`, `dashboard` — cada um responde aos endpoints da etapa correspondente (09-16) com os payloads exatos descritos em `docs/trace/00_endpoints_esperados.md`.

**Pages (etapa 21)**: página pode ser criada com seções; seção fora do catálogo de `BlockType` é rejeitada; `hero` sem `title` ou imagem sem `alt` é rejeitado; reordenar seções persiste a nova ordem; excluir página remove seções em cascata.

**Knowledge Graph**: node pode ser criado · edge pode ser criada · neighbor pode ser consultado · edge inválida é bloqueada · cross-tenant é bloqueado · `x`/`y`/`props` persistem (etapa 17) · `/graph/orphans` retorna nós sem edge · `/graph/nodes/{nodeId}/preview` retorna o shape leve (`summary`/`difficulty` vindo do `Content` quando aplicável) · seed inicial funciona.

**Frontend (React, via etapa 18)**: build gera `dist/` · backend serve a SPA · refresh de rota SPA funciona · API não cai no fallback da SPA.

**Padrão de qualidade e arquitetura (`00_padrao_qualidade_e_arquitetura.md`)**: `java -version` confirma **Java 25** · `pom.xml` declara **Spring Boot 4.1.x** (não 3.x) · `mvn clean verify` passa **e** o `jacoco:check` confirma 100% de cobertura (LINE e BRANCH) em todas as classes funcionais de todas as etapas (entities com comportamento, repositories, mappers, services, controllers, policies/validators — DTOs de transporte puro fora da régua) · todo `Repository` do projeto (`TenantRepository`, `ProductRepository`, `GraphNodeRepository`, `ContentRepository`, `AssetRepository`, `FormDefinitionRepository`, `AuditEventRepository`, `PageRepository`, etc.) tem Javadoc na interface e em todo método declarado · todo mapper é MapStruct (`grep -r "class.*MapperImpl" backend/src/main` não deve aparecer escrito manualmente fora do `target/generated-sources`) · nenhum teste usa `@MockBean`/`@SpyBean` (removidos no Spring Boot 4 — só `@MockitoBean`/`@MockitoSpyBean`).

## Critérios de aceite

- [ ] `mvn clean test` passa sem falhas.
- [ ] Swagger funcional em `local`, desabilitado em `prod`.
- [ ] Todo item do checklist acima confirmado manualmente.
- [ ] `mvn clean verify` (não só `test`) passa em todo o projeto, com o `jacoco:check` confirmando 100% de cobertura nas classes elegíveis de **todas** as etapas — não só a etapa que acabou de ser feita, o projeto inteiro.
- [ ] Nenhum `Repository` do projeto está sem Javadoc na interface ou em algum método.
- [ ] Nenhum uso de `@MockBean`/`@SpyBean` em nenhum teste do projeto.
- [ ] `aegis-postman-collection.json` tem uma pasta por etapa (03 a 22), a pasta "Auth" autentica e captura `{{token}}` automaticamente para os 5 usuários de teste, e importar a collection no Postman permite rodar todo o fluxo (login → CRUD de cada domínio) sem editar nenhum request manualmente.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
cd backend
mvn clean test
mvn clean verify   # confirma jacoco:check em 100% no projeto inteiro
java -version      # confirma Java 25
grep -A1 "<artifactId>spring-boot-starter-parent" pom.xml   # confirma versao 4.1.x
grep -rn "@MockBean\|@SpyBean" src/test/   # esperado: nenhum resultado
```

```txt
http://localhost:8080/swagger-ui/index.html
```

Percorrer o checklist da seção C item a item.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): swagger, testes minimos, 100% cobertura jacoco e checklist final do servidor funcional"
```

## Ao terminar esta etapa

Volte para `00_indice_e_instrucoes.md` e siga os passos finais de `git push`/merge da branch `sprint/02-fundacao-backend` em `develop`. A partir daqui, a Sprint 09 em diante volta a ser executada com Claude/Cowork (ver `docs/sprints/README.md` para a ordem recomendada).
