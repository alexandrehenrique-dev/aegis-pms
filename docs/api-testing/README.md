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

Cada environment declara as variáveis necessárias para a collection inteira: `baseUrl`, `keycloakIssuer`, `clientId`, `username`, `password`, `tenantName` (valores reais em `local`/`dev`) e placeholders vazios para variáveis geradas dinamicamente a cada execução (`tenantKey`, `productKey`, `inviteEmail`, `assetTagName`, `graphRefSeed`, `editorToken`) — o `script:pre-request` de `collection.bru` só preenche essas últimas com um valor único (sufixo `Date.now()`) quando o environment não tiver um valor próprio, então elas devem permanecer vazias nos arquivos de environment.

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
└── 17-settings-dashboard/
```

Os números das pastas seguem a numeração das etapas do backend em `docs/sprints/backend/` (não há pastas `07`, `08`, `09` porque essas etapas não introduziram contratos REST novos cobertos nesta collection).

## Variáveis (`bru.*`)

A collection usa **exclusivamente** a API `bru.getVar`/`bru.setVar` do Bruno — nenhuma referência a `pm.*`/`postman.*` em nenhum arquivo. Variáveis centralizadas:

- **De ambiente** (`environments/*.bru`): `baseUrl`, `keycloakIssuer`, `clientId`, `username`, `password`, `tenantName`.
- **Geradas em runtime, com default no `script:pre-request` de `collection.bru` se vazias**: `tenantKey`, `productKey`, `inviteEmail`, `assetTagName`, `graphRefSeed` (sufixo `Date.now()`, evita colisão de unique constraint em execuções repetidas).
- **Capturadas durante a execução** (via `script:post-response` do request que as cria): `token`, `refreshToken` (login), `callerSubject` (`/me`), `tenantId` (criar tenant), `productId`/`s3ProductId` (criar produto), `userId` (convidar usuário), `assetId`/`pdfAssetId` (upload), `contentId` (criar conteúdo), `articleNodeId`/`topicNodeId`/`graphEdgeId` (Knowledge Graph), `formId`/`invalidFormId` (Sprint 13 Forms), `auditTenantId`/`auditTenantDeletedEventId`/`auditEventId` (Sprint 16 Audit).
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
