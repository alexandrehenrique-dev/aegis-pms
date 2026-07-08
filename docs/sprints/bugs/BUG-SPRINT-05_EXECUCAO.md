# BUG-SPRINT-05 — Documento de Execução

> **Referência canônica:** `BUG-SPRINT-05_qualidade_transversal_conteudo_kg.md` — este arquivo é a versão de execução (decisões tomadas, desvios do texto original, status por item). Não substitui o original.
>
> **Branch:** `bugfix/sprint-05-qualidade-conteudo-kg`
>
> **Escopo desta rodada:** K.1, K.2, G.3, H.1, H.2, H.3, J.1, J.4, J.5, I.1, I.2+I.3, J.2, J.3, G.1, G.2, O.1 (adicionado a pedido do usuário durante a execução — não estava na lista original de 16 itens), L.1.
> **Fora de escopo nesta rodada** (não implementados): G.4–G.8, M.1, N.1–N.4 — presentes no documento de referência mas não solicitados na ordem de execução desta sessão.

---

## Status por item

| Item | Status | Observação |
|---|---|---|
| K.1 | ✅ | Erro mapeado por `code` (`module_disabled`), UI contextual + "Tentar novamente" no `AssetPickerModal`. Migration `V20` habilita `ASSETS` no produto Loki (única falha real de módulo encontrada no seed). |
| K.2 | ✅ | `useAuthenticatedImage` (blob URL autenticado em API mode; `resolveAssetSrc` em mock mode) usado em `BlockRenderer` (hero/image/image-text/gallery/audio/video) e `MediaField` (thumbnail). |
| G.3 | ✅ | Backend já tinha `GET /tenants/{tenantId}/audit-events/{eventId}` completo — só faltava o frontend consumir. `AuditEventDetail.tsx` reescrito com `useParams` + `auditService.getEvent`. Validado visualmente (screenshot) mostrando ator/módulo/severidade reais, não mais `aud_8f42`. |
| H.1 | ✅ | `SubmissionTable` com seletor dinâmico (padrão `PublicationPanel`). Rota "Abrir" corrigida para `/forms/:formId/submissions/:id` (nova). `SubmissionSummary.id` adicionado ao contrato — já existia no backend, faltava no frontend. |
| H.2 | ✅ | Nome do formulário editável no canvas; modal de criação (`CreateFormDialog`) com nome obrigatório substitui a criação silenciosa. Validado visualmente: botão "Criar formulário" desabilitado sem nome, builder abre com o nome preenchido. |
| H.3 | ✅ | Validação frontend (nome + campos) antes de publicar. Backend: `FormPublicationPolicy.assertPublishable(name, fields)` ganhou `FORM_NAME_REQUIRED`/`FORM_HAS_NO_FIELDS`, com 100% JaCoCo. Botão Preview corrigido para `/forms/:formId/preview` — **desvio**: `FormPreviewFrame.tsx` estava 100% hardcoded ("Contato Comercial"); reescrito para consumir o form real via `formId`. |
| J.1 | ✅ | Normalização de `image.src`/`alt` em `EditorPanels.tsx` aplicada sempre (não só quando `image` está ausente). Backend `BlockDefaults.HERO` já tinha o default correto. |
| J.4 | ✅ | try/catch em `handleSubmitForReview`, toast humanizado por `code` (`INVALID_CONTENT_TRANSITION` — já existia no backend). Botão condicional: só aparece com `status === "Draft"`. |
| J.5 | ✅ | Thumbnail via `useAuthenticatedImage` no `MediaField`. Erro contextual e cleanup de upload de sessão implementados junto com K.1 (mesmo componente, `AssetPickerModal`). |
| I.1 | ✅ | `EventListPreview.tsx` novo — hidrata por `selectedEventIds` quando `selectedEvents` vem vazio. `BlockRenderer` ganhou prop opcional `productSlug`, repassada por `ResponsivePreviewFrame`. Persistência do PUT já estava correta (`toUpdateSectionDto` serializa `content` inteiro); não havia bug ali. |
| I.2 | ✅ | Mesma solução de K.2 aplicada em `EventsManagerDrawer.PublicPreview` (troca de `resolveAssetSrc` direto por `useAuthenticatedImage`). |
| I.3 | ✅ | `BlockRenderer` casos `audio`/`video` renderizam `<audio>`/`<video>` reais com `useAuthenticatedImage`; Spotify vira link, YouTube vira iframe embed. |
| J.2 | ✅ | `MusicPicker.tsx` novo (abas Asset áudio / Spotify URL), substitui o `EntityPicker` genérico em `ContentArticleEditor.MetadataPanel`. Layout do botão com `flex items-center gap-1.5`. |
| J.3 | ✅ | `knowledgeService.seedNodesFromContent` novo; `KnowledgeOverview` semeia o grafo a partir de conteúdo publicado quando o grafo está vazio. `EntityPicker` distingue "grafo vazio" de "busca sem resultado". |
| G.1 | ✅ | CI (`ci.yml`) corrigido com `--sandbox developer` + pré/pós-teardown. Guarda de prefixo `TEST-` adicionada ao `collection.bru` (pre-request script) + `_envGuard` em `prod.bru`. Auditoria confirmou os 10 scripts de criação de tenant já usavam `TEST-` (nenhuma mudança necessária ali). |
| G.2 | ✅ | Verificado via Playwright (bounding-box check) que o `FeedbackDetailDrawer` já renderiza corretamente dentro do viewport, portalizado em `document.body`. Nenhuma mudança de código necessária — o bug não reproduziu no estado atual do código. |
| O.1 | ✅ | Adicionado ao escopo a pedido do usuário. `InviteScreen` coleta nome/sobrenome obrigatórios; `AuthActivationService.activate()` ganhou `firstName`/`lastName` e chama `KeycloakAdminClient.updateUserProfile()` antes de habilitar a conta; novo `AuthActivateRequest` DTO; `LoginScreen` distingue "perfil incompleto" (`ACCOUNT_NOT_FULLY_SET_UP`, novo) de "conta bloqueada" (`ACCOUNT_DISABLED`) — backend agora separa os dois casos em `KeycloakTokenClient`. `InviteUserDrawer` exige nome + sobrenome no convite. |
| L.1 | ✅ | Ver seção "Verificação" abaixo — regressão completa via Playwright (19/21 scripts), Bruno collection local, screenshots de fluxos-chave, `mvn verify` com JaCoCo 100%. |

---

## Decisões e desvios do texto de referência

1. **K.2 — não foi criado um hook duplicado do zero.** O código já tinha `domains/assets/hooks/useAssetObjectUrl.ts`, que resolve exatamente o problema de autenticação via `apiClient.getBlob`. Em vez de reimplementar do zero, `useAuthenticatedImage` (novo, em `shared/hooks/`) segue a mesma estratégia mas devolve só a `string | undefined` (não `{url, loading}`) e faz fallback para `resolveAssetSrc` em mock mode.
2. **H.1 — `SubmissionSummary.id` e `AuditEvent.id`** não existiam nos mocks locais (só no contrato/backend). Adicionados nos stores mock com IDs sintéticos (`mock-sub-{i}`, `mock-evt-{i}`).
3. **H.3 — `FormPreviewFrame.tsx` reescrito**, não só a chamada de navegação. O componente antigo era 100% estático — corrigir só o link sem o destino consumir `formId` teria deixado o bug funcionalmente idêntico.
4. **K.1.3 — módulo Assets no seed**: o produto Loki (`b0000000-...-006`) era o único produto de teste sem `ASSETS` habilitado, e é o produto usado nos fluxos de mídia/música desta sprint. Nova migration `V20__assets_module_test_products.sql` habilita o módulo especificamente para ele.
5. **G.1** — diagnóstico do documento de referência já estava correto; a auditoria dos scripts de criação de tenant não encontrou nenhum sem prefixo `TEST-`.
6. **G.2** — investigado com um script Playwright dedicado que mede a bounding box real do drawer no DOM: ele já é filho direto de `<body>` (portal correto) e fica 100% dentro do viewport em 1600×1100. Nenhuma alteração de código foi necessária; documentado como "verificado, sem regressão" em vez de aplicar uma mudança especulativa.
7. **O.1 — indo além do pedido original**: além de coletar firstName/lastName, o backend (`KeycloakTokenClient`) tratava "conta desabilitada" e "perfil incompleto" com o mesmo código de erro (`ACCOUNT_DISABLED`) por design anterior. Como o próprio texto do bug pede uma mensagem de login diferenciada para "perfil incompleto", foi necessário separar as duas causas em `AccountNotFullySetUpException`/`ACCOUNT_NOT_FULLY_SET_UP` no backend para que o frontend pudesse де fato diferenciar as mensagens.
8. **Achados corrigidos fora da lista original, mas necessários para o build/CI ficar verde:**
   - `BlockDefaults.java`: apontamento pré-existente do Sonar (literal duplicado `"youtubeUrl"` em vez da constante `KEY_YOUTUBE_URL`) — corrigido a pedido do usuário.
   - `HealthSignalResponse.java`: construtor de conveniência de 4 argumentos nunca usado em nenhum lugar do código (nem produção nem testes) — removido (era a única causa de um gap de cobertura pré-existente).
   - `TenantUserService`/`ProductUserAccessService`: gaps de cobertura JaCoCo pré-existentes (branches defensivos nunca exercitados por teste) — fechados com novos testes, sem alterar nenhuma lógica de produção.
   - Todos os três já existiam em `develop` antes desta sessão (confirmado via `git diff develop --stat`); foram fechados porque o gate `mvn verify` é bundle-wide (100% do projeto), não por arquivo alterado.

---

## Verificação (L.1)

- [x] `cd frontend && npm run typecheck` — 0 erros
- [x] `cd frontend && npm run lint` — 0 erros (1 violação de `react-hooks/rules-of-hooks` encontrada e corrigida em `EventsManagerDrawer.tsx`)
- [x] `cd backend && mvn clean verify` — **BUILD SUCCESS**, exit code 0, JaCoCo bundle em 100% linhas/branches
- [x] Suite completa de smoke tests Playwright (`node tests/run-all.mjs`, 21 scripts, ~200 assertions): **19/21 passando** após correções.
  - **Regressões reais desta sprint corrigidas nos próprios testes** (comportamento antigo era o bug, o novo é a correção esperada): `tests/forms.mjs` (preview agora usa `/forms/:formId/preview`, submissions usa `/forms/:formId/submissions/:id`, embed em vez de domínio inexistente), `tests/audit.mjs` ("Abrir recurso" agora navega por `event.module`, testado com o evento de módulo "Permissions"), `tests/assets.mjs` (nome de botão desatualizado `"Concluir upload"` → `"Enviar upload"`, mismatch pré-existente não relacionado a esta sprint mas corrigido).
  - **2 falhas remanescentes confirmadas pré-existentes e não relacionadas** (via `git diff develop --stat` mostrando zero mudança nesses arquivos): `assets.mjs` (console error `ERR_REQUEST_RANGE_NOT_SATISFIABLE` vindo de `useAssetObjectUrl`/`loadAssetFile`, hook pré-existente não tocado nesta sprint) e `products.mjs` (`CreateProductForm` navega para o produto criado em vez de permanecer em `/products/new`, comportamento não tocado por esta sprint — fora do escopo N.3/N.4, explicitamente não solicitados).
- [x] Bruno — teardown de tenants validado **duas vezes**: primeiro isoladamente (`99-teardown --sandbox developer`) contra o Postgres local com estado acumulado, depois a collection completa (`bru run --env local --sandbox developer`) contra um **banco 100% limpo** (reset dos volumes Docker `aegis_postgres_data`/`keycloak_aegis_postgres_data`/`aegis_assets_data`, confirmado com o usuário antes de executar). Nos dois casos: **CLIENTES BETA sempre sobrevive, tenants `TEST-*` são aceitos para exclusão (202), health-check final confirma**. Prova final via `GET /api/v1/tenants` após a collection inteira rodar: `[{"name": "CLIENTES BETA", "key": "clientes-beta"}]` — exatamente 1 tenant. **CI fix confirmado funcional de ponta a ponta** (antes, sem `--sandbox developer`, o teardown rodava sem erro mas não apagava nada).
  - O reset de DB expôs que os usuários demo do Keycloak (`loki`/super_admin, `super-admin@byop.io`) nunca foram automatizados no repositório (nenhum `CommandLineRunner`/endpoint chama `IdentityDemoUserService.ensureDemoUser`; o realm exportado em `infra/keycloak/realm/aegis-realm.json` não inclui usuários) — foram recriados manualmente via `kcadm.sh` para esta validação. Isso é uma lacuna de tooling pré-existente do projeto (documentado aqui, fora do escopo desta sprint corrigir).
  - Único problema real causado por esta sprint: dois scripts de `29-auth-action-tokens` (`ativar-convite.bru`, `ativar-convite-senha-fraca-422.bru`) não enviavam `firstName`/`lastName`, agora obrigatórios (O.1) — corrigidos.
  - Falhas remanescentes na collection completa (50/306, reproduzem identicamente com estado acumulado ou com banco limpo) — confirmadas pré-existentes e fora do escopo desta sprint: `03-product-modules`, `04-manual`, `14-analytics`, `16-audit` (1 script cada), `22-seed-inicial-e-grafo`/`23-dominio-pages-secoes-e-blocos` (seeds/fixtures de sprints anteriores dessincronizados do schema atual), `25-notifications`, `27-feedback`, `30-seed-homologacao` (drift entre a política de senha do realm exportado — `length(10)` — e a senha de teste `Senha123` de 8 caracteres usada nos scripts de convite). Nenhum toca código desta sprint.
- [x] Screenshots de fluxos-chave capturados via Playwright confirmando visualmente: H.2 (modal de criação de formulário exige nome, botão desabilitado sem preenchimento), G.3 (detalhe de evento de auditoria com dados reais — ator "Ana Martins", módulo "Permissions", sem `aud_8f42` hardcoded).
- [x] Nenhum arquivo de especificação (`BUG-SPRINT-03/04/05`) modificado.

---

## Fechamento

Todos os 16 itens do escopo original + O.1 (adicionado a pedido do usuário) foram implementados, com testes de unidade/integração no backend (100% JaCoCo em todo código novo) e validação end-to-end via Playwright no frontend. O build completo (`mvn clean verify` + `npm run typecheck` + `npm run lint`) está verde.

A collection Bruno completa foi validada contra um banco 100% limpo (reset de volumes Docker, autorizado explicitamente pelo usuário). O objetivo central do G.1 — **nenhum tenant fantasma sobrevive, CLIENTES BETA nunca é tocada** — está confirmado de ponta a ponta com prova via API (`GET /api/v1/tenants` retorna só `CLIENTES BETA` após a collection inteira criar e o teardown limpar dezenas de tenants `TEST-*`). As falhas remanescentes na collection (50/306) são todas de domínios fora do escopo desta sprint e reproduzem identicamente em banco limpo ou não — não são regressões desta sprint.
