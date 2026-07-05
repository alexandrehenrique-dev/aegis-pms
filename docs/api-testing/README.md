# docs/api-testing — Collection Bruno de validação manual

## Propósito

Guia de uso da **collection Bruno** (`bruno/`, na raiz do repositório) — a suíte de testes manuais cumulativa que valida, request a request, os contratos REST descritos em cada etapa de `docs/sprints/backend/`. Substitui integralmente a antiga collection Postman (`postman/aegis-postman-collection.json`, removida do repositório); ver `docs/sprints/backend/SPRINT-RESULTADO.md`, entrada "Sprint Técnica — Migração Postman → Bruno", para o histórico da migração.

> Esta collection valida contratos manualmente contra a aplicação real (`mvn spring-boot:run`). Ela **não substitui** os testes automatizados JUnit/Mockito do backend (Seção 5 de `00_padrao_qualidade_e_arquitetura.md`) — os dois são obrigatórios e independentes.

## Por que Bruno (e não Postman)

[Bruno](https://www.usebruno.com/) é um cliente de API **git-nativo**: cada request é um arquivo de texto `.bru` versionado normalmente no git (diff legível, sem aplicativo desktop sincronizando workspace em segundo plano, sem dependência de conta/cloud). A collection inteira mora em `bruno/`, dentro deste repositório.

## Instalação

- **App desktop** (recomendado para testes manuais exploratórios): [usebruno.com/downloads](https://www.usebruno.com/downloads).
- **CLI** (recomendado para validação automatizada/CI): nenhuma instalação prévia necessária, basta `npx @usebruno/cli`.

## Abrindo a collection no app desktop

1. Abra o Bruno e use **"Open Collection"**.
2. Selecione exatamente a pasta `bruno/` (onde está `bruno.json`) — **não** a raiz do repositório, nem uma subpasta de etapa.
3. No seletor de Environment (topo direito, ao lado de "No Environment"), escolha **local** antes de rodar qualquer request.
4. Se a collection já estava aberta a partir de outra pasta, feche-a primeiro e reabra apontando para `bruno/` — o app pode manter uma referência antiga.

## Rodando via CLI

```bash
cd bruno
npx @usebruno/cli run --env local
```

Roda a collection **inteira**, todas as pastas em sequência única, na ordem numérica. Isso é obrigatório (não rodar uma pasta isolada) porque variáveis capturadas em tempo de execução (token, IDs criados) só persistem dentro de uma mesma invocação do CLI — pastas posteriores dependem de IDs gerados pelas anteriores (ex.: `productId` criado em `02-products` é usado em `03-product-modules`, `05-knowledge-graph`, `11-content`, `12-assets`, `13-forms` e `14-analytics`).

Para rodar contra outro ambiente: `npx @usebruno/cli run --env dev` (ou `homolog`/`prod`, quando os respectivos `baseUrl` estiverem preenchidos).

## Environments

| Environment | Arquivo | Status |
|---|---|---|
| `local` | `bruno/environments/local.bru` | `baseUrl: http://localhost:8080`, pronto para uso imediato |
| `dev` | `bruno/environments/dev.bru` | mesma máquina local, outro profile do backend — `baseUrl: http://localhost:8080` |
| `homolog` | `bruno/environments/homolog.bru` | `baseUrl` vazio — preencher quando o ambiente existir |
| `prod` | `bruno/environments/prod.bru` | `baseUrl` vazio — preencher quando o ambiente existir |

Cada environment declara as variáveis necessárias para a collection inteira: `baseUrl`, `keycloakIssuer`, `clientId`, `username`, `password`, `tenantName` e placeholders vazios para variáveis geradas dinamicamente a cada execução (`tenantKey`, `productKey`, `inviteEmail`, `assetTagName`, `graphRefSeed`, `editorToken`) — o `script:pre-request` de `collection.bru` só preenche essas últimas com um valor único (sufixo `Date.now()`) quando o environment não tiver um valor próprio, então elas devem permanecer vazias nos arquivos de environment. Por segurança, `tenantName` deve sempre começar com `TEST-` em todos os environments, inclusive `prod`, para que tenants criados pela Bruno sejam capturados pelo teardown final e nunca pareçam tenants reais.

## Limpeza segura de tenants de teste

A pasta `99-teardown` roda por último e remove somente tenants marcados como teste. O critério principal é o prefixo `TEST-`, aplicado na criação dos tenants pela própria collection. O step `02-limpar-tenants-de-teste` também reconhece, de forma temporária e fechada, resíduos antigos de testes locais (`Tenant listener-commit`, `Tenant listener-rollback`, `Tenant <uuid>` e `Tenant E2E <timestamp>`), porque esses nomes existiram antes da padronização.

O teardown nunca usa a lógica "apague tudo exceto CLIENTES BETA". Tenants reais sem marcador de teste não entram como candidatos, e `CLIENTES BETA` ainda é protegido explicitamente por nome/key como trava adicional. Ao adicionar um request Bruno, smoke frontend ou teste de integração que possa criar tenant em banco compartilhado, use nome com prefixo `TEST-` para que a limpeza consiga removê-lo sem risco para clientes reais.

## Estrutura da collection

```txt
bruno/
├── bruno.json              # manifesto da collection
├── collection.bru          # headers herdados (Authorization Bearer) + script:pre-request com defaults
├── environments/           # local.bru, dev.bru, homolog.bru, prod.bru
├── fixtures/                # arquivos binários reais usados em upload multipart (PNG, PDF, mime invalido, PNG acima do limite)
├── 00-auth/                 # login + /me
├── 01-tenants/
├── 02-products/
├── 03-product-modules/
├── 04-manual/                # cenários avulsos (404, module-gating em endpoint futuro)
├── 05-knowledge-graph/
├── 06-health/
├── 10-product-assignments/
├── 11-content/
├── 12-assets/
├── 13-forms/
├── 14-analytics/
├── 15-users/
├── 16-audit/
├── 17-settings-dashboard/
├── 18-knowledge-graph/
├── 19-settings-security/
├── 20-spa-same-origin/
├── 21-docker-stack/
├── 22-seed-inicial-e-grafo/
├── 23-dominio-pages-secoes-e-blocos/
├── 24-openapi-testes-checklist-final/
├── 25-notifications/
├── 26-product-templates/
├── 27-feedback/
├── 28-backup-exportacao-exclusao/
├── 29-auth-action-tokens/
└── 30-seed-homologacao/
```

Os números das pastas seguem a numeração das etapas do backend em `docs/sprints/backend/` (não há pastas `07`, `08`, `09` porque essas etapas não introduziram contratos REST novos cobertos nesta collection).

## Variáveis (`bru.*`)

A collection usa **exclusivamente** a API `bru.getVar`/`bru.setVar` do Bruno — nenhuma referência a `pm.*`/`postman.*` em nenhum arquivo. Variáveis centralizadas:

- **De ambiente** (`environments/*.bru`): `baseUrl`, `keycloakIssuer`, `clientId`, `username`, `password`, `tenantName`.
- **Geradas em runtime, com default no `script:pre-request` de `collection.bru` se vazias**: `tenantKey`, `productKey`, `inviteEmail`, `assetTagName`, `graphRefSeed` (sufixo `Date.now()`, evita colisão de unique constraint em execuções repetidas).
- **Capturadas durante a execução** (via `script:post-response` do request que as cria): `token`, `refreshToken` (login), `callerSubject` (`/me`), `tenantId` (criar tenant), `productId`/`s3ProductId` (criar produto), `userId` (convidar usuário), `assetId`/`pdfAssetId` (upload), `contentId` (criar conteúdo), `articleNodeId`/`topicNodeId`/`graphEdgeId` (Knowledge Graph), `formId`/`invalidFormId` (Sprint 13 Forms), `publicSubmissionId` (Sprint 19 Submit público), `auditTenantId`/`auditTenantDeletedEventId`/`auditEventId` (Sprint 16 Audit), `orphanNodeId`/`orphanBatchNodeId`/`reviewedInsightId` (Sprint 18 Knowledge Graph), `seedClientesBetaTenantId`/`seedAegisLabsTenantId`/`seedClienteNorteTenantId`, `seedWikiDevProductId`/`seedLokiProductId` e `seedSpringBootNodeId`/`seedJpaNodeId`/`seedPoemaNodeId`/`seedMusicaNodeId` (Sprint 22 seed inicial). A Sprint 21 usa apenas `baseUrl` e `keycloakIssuer` já existentes para validar health/discovery da stack Docker.
- **Sprint 23 (`23-dominio-pages-secoes-e-blocos`)**: `pageId`, `heroSectionId`/`audioSectionId`/`videoSectionId`/`twoColumnSectionId`/`contactSectionId`/`downloadSectionId`, `eventId`. A pasta 22 (seed) termina autenticada como o usuário demo `super-admin@byop.io`, sem `ProductAssignment` no `{{productId}}` avulso criado em `02-products` — por isso o primeiro request da pasta 23 (`relogar-como-loki`) reautentica como `{{username}}`/`{{password}}` (loki) antes de qualquer chamada, restaurando o `{{token}}` com acesso real ao produto. Reutiliza `{{assetId}}` (PNG) para os blocos `download`/`hero`/`image`/`two-column` — nunca `{{pdfAssetId}}`, que já foi excluído ao final da própria pasta `12-assets` (`excluir-asset-sem-uso`) — e `{{formId}}` (Sprint 13) para o bloco `contact`.
- **Sprint 24 (`24-openapi-testes-checklist-final`)**: não cria variáveis novas. Valida `GET /swagger-ui/index.html` e `GET /v3/api-docs` em `local`/`dev`, com requests públicos (`auth: none`) para confirmar metadata, Bearer JWT, tags na ordem canônica e operações documentadas.
- **Sprint 25 (`25-notifications`)**: `notificationId`, `bellNotificationId`, `notificationInviteEmail` e `notificationTenantUserId`. A pasta alterna login entre `super-admin@byop.io`, `{{username}}`/`{{password}}` (Loki) e `editor@byop.io` para cobrir criação/listagem admin, `/mine`, `pending-modal`, `mark-shown`, `mark-read` e 403 de papel não autorizado. Antes de validar a FEATURE da pasta, marca o onboarding seedado (`25000000-0000-0000-0000-000000000001`) como shown para o Loki, porque ele é `MODAL_ONCE` e mais antigo que notificações criadas durante a execução.
- **Sprint 26 (`26-product-templates`)**: `templateTenantId`, `templateSiteProductId`, `templateCustomProductId`. Cria um tenant próprio no início da pasta (não reaproveita `{{tenantId}}` compartilhado) para que o mesmo usuário logado seja o criador/membro do tenant e quem consulta `/notifications/mine` mais abaixo, sem depender de qual foi o último login das pastas anteriores. Confirma o esqueleto de 7 páginas do `"Site Institucional"`, o 403 `MODULE_DISABLED` ao listar páginas de um produto `"Custom"` (módulo `PAGES` nunca habilitado) e as notificações `WARNING`/`BELL_ONLY` (suspensão) e `GENERAL`/`BELL_ONLY` (reativação) de tenant.
- **Sprint 27 (`27-feedback`)**: `feedbackTenantId`, `feedbackProductId`, `feedbackAssetId`, `feedbackId`, `feedbackWithAttachmentId`, `feedbackCrossTenantId`, `feedbackCrossProductId`, `feedbackCrossAssetId`, `feedbackEditorId` e `seedClientesBetaTenantId` (capturado pela propria pasta via `GET /tenants`, procurando `key=clientes-beta`). Cria tenant/produto próprios, habilita `ASSETS`, faz upload real via domínio `asset`, cria feedback com e sem anexo, valida sino `WARNING`/`BELL_ONLY` para feedback `crítica`, rejeita anexo cross-tenant, valida listagem global como `SUPER_ADMIN`, bloqueia `EDITOR`, atualiza status pelo id legível `AGS-####` e usa os usuários demo `editor@byop.io`/`admin@byop.io` para provar criação por usuário comum e listagem do tenant seed pelo `TENANT_ADMIN`.
- **Sprint 28 (`28-backup-exportacao-exclusao`)**: `exportDeleteTenantId` e `exportDeleteProductId`. A pasta possui login próprio para execução isolada, valida download por token inexistente, cria tenant/produto descartável, rejeita `confirmationText` incorreto e confirma `DELETE /products/{productId}` com `202 Accepted`. O job assíncrono envia o link de export para o e-mail do usuário autenticado e remove o produto somente depois do ZIP armazenado e validado.
- **Sprint 29 (`29-auth-action-tokens`)**: `authActionTenantKey`, `authActionTenantId`, `authActionAdminEmail`, `authActionInviteEmail`, `authActionTenantUserId`, `mailhogBaseUrl`, `inviteActivationToken`, `inviteActivationPassword`, `resetPasswordEmail`, `passwordResetToken` e `newPassword`. A pasta possui login próprio para execução isolada, cria tenant descartável, convida usuário, captura o token de convite na API do MailHog (`/api/v2/messages`), valida o contexto do convite, confirma rejeição de senha fraca, ativa a conta, solicita reset de senha para o mesmo usuário, captura o token de reset no MailHog e confirma a nova senha. Os scripts de captura normalizam quoted-printable antes de extrair o UUID dos links `http://localhost:5173/invite?token=...` e `http://localhost:5173/reset-password?token=...`. Requer backend, Keycloak e MailHog locais rodando.
- **Sprint 30 (`30-seed-homologacao`)**: `seed30ClientesBetaTenantId`, `seed30MaestroBetonProductId`, `seed30CmssProductId`, `seed30WikiDevProductId` e `seed30LokiProductId`. A pasta valida a migration SQL de homologacao sem depender de UUID fixo quando o banco local ja tinha seed antigo: captura tenant/produtos por `key`, confere paginas do Maestro Beton, nodes do grafo Loki, `telegramAlert` em `/settings/security` com token mascarado e OpenAPI com `oauth2-pkce` preservando `bearer-jwt`.
- **Geradas em runtime, com default no `script:pre-request` de `collection.bru`**: `auditTenantKey` (sufixo `Date.now()`, tenant descartável criado/excluído só dentro de `16-audit`, nunca o `{{tenantId}}` compartilhado).
- **Sem default automático, preencher manualmente se necessário**: `editorToken` — usado no cenário "EDITOR tentando publicar (403)" de `11-content` e em "EDITOR tentando editar roles (403)" de `17-settings-dashboard`; `nonMemberToken` — usado no cenário "Tenant fora do escopo do caller (404)" de `16-audit` e em "TENANT_ADMIN de outro tenant (404)" de `17-settings-dashboard` (neste último precisa ser especificamente um TENANT_ADMIN sem membership no tenant, não EDITOR/VIEWER/PRODUCT_MANAGER, que recebem 403 por papel insuficiente antes da checagem de tenant). Em ambos, sem a variável preenchida, o teste aceita o 401 resultante (Bearer vazio/inválido) como comportamento esperado da limitação — não há fluxo de seed automático de um segundo usuário (EDITOR ou sem membership) nesta suíte.

## Autenticação

O primeiro request da collection (`00-auth/login-loki-123456.bru`) autentica contra o Keycloak via Resource Owner Password Credentials e captura `token`/`refreshToken` automaticamente. O header `Authorization: Bearer {{token}}`, declarado em `collection.bru`, é herdado por toda request da collection — nenhum request precisa configurar autenticação própria, exceto os marcados `auth: none` (login e `/actuator/health`, que são públicos por contrato).

> Os access tokens do Keycloak têm TTL de 5 minutos. A collection inteira roda em poucos segundos, então isso normalmente não é um problema — mas se você pausar a execução manualmente no app desktop por mais de 5 minutos entre requests, rode o login de novo.

## Padrão obrigatório ao adicionar requests em sprints futuras

Toda nova etapa de domínio que expõe contrato REST **deve** adicionar uma pasta numerada nova em `bruno/` (mesmo número da etapa em `docs/sprints/backend/`), seguindo:

1. Um request por `curl` documentado na seção "Validação" da etapa — mesmo método, path, body.
2. Bloco `docs` em todo request, cobrindo objetivo, payload (quando houver), resultado esperado e variáveis usadas/produzidas.
3. Pelo menos um request por cenário de **rejeição** documentado na etapa (400/403/404/409/413/502), não só o caminho feliz — mesmo princípio já exigido dos testes automatizados (Seção 5 de `00_padrao_qualidade_e_arquitetura.md`).
4. Se a etapa precisar de um módulo habilitado que outra pasta deixou desabilitado (ou vice-versa) para os cenários funcionarem em sequência única de CLI, adicionar requests explícitos de habilitar/desabilitar nos pontos de transição — documentados no `docs` como "decisão de reconstrução", nunca silenciosos.
5. Ao final da implementação, validar a collection **inteira** (não só a pasta nova) via `npx @usebruno/cli run --env local`, a partir de `bruno/`, com 100% dos requests e testes aprovados, antes de considerar a etapa concluída.
6. Atualizar a tabela de variáveis acima se a etapa introduzir uma variável nova compartilhada entre pastas.
