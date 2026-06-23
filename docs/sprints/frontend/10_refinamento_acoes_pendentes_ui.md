# Sprint 10 — Refinamento: ações pendentes na UI (botões mortos e campos travados)

> Pré-requisito: Sprint 01 (arquitetura) concluída. Não depende da Sprint 02 (backend) nem da 06 (Keycloak) — todo trabalho aqui é client-side, sobre os serviços mock já criados pela Sprint 09 (`domains/*/services/`) ou sobre estado local de componente, deixando o gancho pronto para a Sprint 07 (toggle mock/real) trocar a fonte depois.

## Contexto

Auditoria completa de `frontend/src` (todas as 11 pastas de domínio + `shared`/`app`/`core`) encontrou **140+ elementos interativos sem ação real**: botões sem `onClick`, células de permissão sem toggle, abas sem troca de conteúdo, e campos de formulário (`Field`/`SelectLike`) sem `onChange`. Exemplo citado: em `domains/assets/pages/AssetTagManager.tsx`, o botão "Criar tag" (`<Button primary>Criar tag</Button>`, linha 9) não tem `onClick` — clicar não faz nada.

Duas causas-raiz na própria base de componentes, que devem ser corrigidas **antes** de tentar arrumar os usos individuais (Tarefa A):

1. **`SelectLike` (`shared/components/Primitives.tsx`) não aceita `onChange` nem `options` no seu tipo** — é um botão decorativo com um `ChevronDown`, nunca foi feito para ser editável. Todo lugar que usa `SelectLike` esperando que vire um seletor funcional está, na verdade, usando um componente que não tem como funcionar.
2. **`Field` em modo `textarea` usa `defaultValue` em vez de `value`/`onChange`** — então mesmo quando alguém passa `onChange`, o textarea nunca chama de volta; só o `<input>` (modo não-textarea) é controlado corretamente.

Também já existem no repositório os blocos de construção certos para resolver a maioria dos casos, então a Sprint 10 não cria infraestrutura nova, só usa o que já está lá:
- `domains/{dashboard,products,tenants,users}/services/` + `shared/services/` (Sprint 09) — para chamadas de ação que persistem algo.
- `shared/components/ui/dropdown-menu.tsx`, `dialog.tsx`, `drawer.tsx`, `popover.tsx`, `context-menu.tsx`, `shared/components/ConfirmDialog.tsx` — para menus de "...", confirmações destrutivas e painéis de filtro.
- `core/notifications/toast` — para feedback de sucesso/erro.
- O padrão já correto em `domains/users/pages/InviteUserDrawer.tsx` (estado local `useState`, `handleSend` assíncrono chamando `usersService.invite(...)`, loading/sent state no botão, `toast.success(...)`) — **use este arquivo como referência de como uma tela "termina certa"**.
- O padrão já correto de abas em `domains/content/components/EditorPanels.tsx` (linhas 72-80: `<button key={t} onClick={() => setTab(t)}>`) — **use como referência para qualquer aba sem `onClick`**, ex. as 8 abas mortas em `ProductDetail.tsx`.

`domains/{content,assets,forms,analytics,knowledge,settings,audit}` ainda não têm pasta `services/` (gap já registrado em `docs/trace/00_endpoints_esperados.md`, seção B). Sempre que uma correção desta sprint precisar persistir algo nesses domínios, **crie o `services/<dominio>Service.ts` mínimo na hora**, seguindo o mesmo formato dos serviços que já existem — isso fecha parte do gap da Sprint 09 como efeito colateral, em vez de deixar para depois.

## Objetivo

Toda ação visível na UI deve fazer algo de verdade (chamar um service mock, navegar, abrir um drawer/dialog, alternar um estado local, copiar/exportar) — ou, se for genuinely só leitura por design (poucos casos), ficar visualmente marcada como tal, nunca parecendo clicável sem ser.

## Tarefas

### A. Corrigir os primitivos compartilhados (pré-requisito de tudo abaixo)

Em `shared/components/Primitives.tsx`:

1. `SelectLike` passa a aceitar `options: string[]` e `onChange?: (v: string) => void`. Implementação mínima: ao clicar, abrir um `DropdownMenu` (`shared/components/ui/dropdown-menu.tsx`) listando `options`, chamando `onChange` na seleção. Se nenhuma `options` for passada, mantém o comportamento atual (decorativo) — isso preserva os casos intencionalmente fixos (ver Tarefa C, itens `READONLY_MARK`) sem quebrá-los.
2. `Field` em modo `textarea`: trocar `defaultValue={value}` por `value={value}` e adicionar `onChange={(e) => onChange?.(e.target.value)}`, igual ao `<input>` already faz.
3. Para os casos `READONLY_MARK` (campos intencionalmente fixos, ver Tarefa C), adicionar um prop `locked?: boolean` em `Field`/`SelectLike` que aplica estilo visual de desabilitado (`opacity-60 cursor-not-allowed`, sem `ChevronDown` ativo) — assim a intenção fica visível na tela, não só no código.

### B. Padrão de decisão por tipo de correção

Para cada item da Tarefa C, aplique um destes padrões (códigos usados na tabela):

| Código | Padrão de correção |
|---|---|
| `TAB` | Estado local `useState` trocando o conteúdo renderizado — copiar exatamente o padrão de `EditorPanels.tsx` linhas 72-80. |
| `MENU` | Trocar o botão "..." por um `DropdownMenu` (`shared/components/ui/dropdown-menu.tsx`) com as ações plausíveis do contexto (ex.: produto → Abrir, Favoritar, Arquivar). |
| `DRAWER` | Abrir um `Drawer`/`Dialog` (`shared/components/ui/drawer.tsx` ou `dialog.tsx`) com um formulário mínimo, seguindo o padrão de `InviteUserDrawer.tsx`: `useState` por campo, `handleSubmit` assíncrono chamando o `service` do domínio, loading/sucesso no botão, `toast.success`/`toast.error`. |
| `SERVICE` | Chamada direta a um método de `service` (existente ou criado na hora, ver Contexto) sem precisar de um drawer — ação de um clique (ex.: habilitar módulo, marcar revisado, arquivar). Sempre com `toast` de feedback. Ações destrutivas (arquivar, bloquear, remover) passam por `ConfirmDialog` antes de chamar o service. |
| `FILTER` | Abrir um `Popover` com as opções de filtro do contexto; aplica filtro client-side sobre a lista mockada já carregada na tela (sem precisar de backend). |
| `EXPORT` | Gera um CSV/JSON a partir dos dados mock já visíveis na tela e dispara download via `Blob` + link temporário — deixar claro num comentário que é exportação do que está na tela (mock), não um endpoint real ainda. |
| `COPY` | `navigator.clipboard.writeText(...)` do valor relevante + `toast.success("Copiado")`. |
| `RETRY` | Reexecuta a função de carregamento de dados da própria tela (o mesmo `useEffect`/loader que populou o estado inicial). |
| `TOGGLE` | Alterna um valor de estado local (período selecionado, viewport, idioma, seleção em massa, célula de permissão) — não precisa de service nesta sprint, é estado de UI. |
| `EDIT_FIELD` | Depois da Tarefa A: trocar o `Field`/`SelectLike` estático por uma versão controlada (`useState` + `onChange`), seguindo `InviteUserDrawer.tsx` linha 34 (`Field` com `onChange={setName}`) como referência. |
| `READONLY_MARK` | Manter sem ação, mas marcar com `locked` (Tarefa A.3) e, se ainda não houver, um comentário no código explicando o motivo (ex.: "fixo por enquanto: o destino, plano Starter por padrão para novos tenants — ADR/Sprint 09"). |
| `NAV` | `useNavigate()` para uma rota existente (ou nova, se fizer sentido criar — ex. `/products/:id/settings` para "Editar" em `ProductDetail`). |
| `BACK` | `navigate(-1)` ou para uma rota-pai conhecida (ex.: de volta para a listagem). |
| `UPLOAD` | `<input type="file" hidden ref={...} />` acionado por `inputRef.current?.click()` no botão visível. |

### C. Inventário completo — corrigir item por item, por domínio

#### `products`
- `components/ProductCard.tsx` — botão "..." (`MoreHorizontal`) → `MENU`.
- `pages/ProductDetail.tsx` — "Editar" → `NAV` (`/products/:id/settings`); "Salvar visão" → `SERVICE` (preferência de view, pode persistir local/`localStorage` por ora); 8 abas ("Visão Geral", "Módulos", ...) → `TAB`.
- `pages/ProductEmpty.tsx` — "Editar dados do produto" → `NAV` (mesma rota do item acima).
- `pages/CreateProductForm.tsx` — 4 `SelectLike` (Tenant, Tipo, Idioma, Template) → `READONLY_MARK` (intencional — template fixo do wizard, conforme Sprint 09).
- `components/ModuleCatalog.tsx` — CTA por módulo → `SERVICE` (`productsService`, endpoint `enable`/`disable` já definido no backend — ver `docs/trace/00_endpoints_esperados.md`); "Habilitar selecionados" → `SERVICE` (mesmo service, em lote).
- `pages/ProductDashboard.tsx` — "Preview público" → `NAV` (`window.open` na URL pública do produto).

#### `forms`
- `pages/FormsList.tsx` — "Tipo / Status" → `FILTER`; "Exportar" → `EXPORT`.
- `pages/SubmissionDetails.tsx` — "Atribuir" → `DRAWER`; "Marcar qualificado" → `SERVICE`; `SelectLike` "Responsável" → `EDIT_FIELD`.
- `pages/SubmissionTable.tsx` — "Exportar" → `EXPORT`; "Atribuir selecionados" → `DRAWER` (em lote).
- `pages/PublicationPanel.tsx` — "Copiar embed" → `COPY`; "Publicar alterações" → `SERVICE`.
- `pages/FormPreviewFrame.tsx` — "Voltar" → `BACK`; "Enviar teste" e "Enviar" (canvas) → `SERVICE` (mock submit, toast "Envio de teste registrado").
- `pages/FormBuilder.tsx` — botões de campo na paleta (cada tipo) → `SERVICE`/estado local (adiciona o campo ao array do canvas — não precisa de backend); 8 `Field`/`SelectLike` do painel de propriedades → `EDIT_FIELD` (refletem o campo selecionado no canvas); "Salvar rascunho" → `SERVICE`; "Preview" → `NAV`; "Publicar" → `SERVICE`.
- `pages/BasicFormAnalytics.tsx` — "Últimos 30 dias" → `TOGGLE`.

#### `content`
- `components/VersionTimeline.tsx` — "Visualizar"/"Comparar" → `NAV`; "Restaurar" → `SERVICE` (com `ConfirmDialog`); "Sem permissão para restaurar" → `READONLY_MARK` (já intencional — só falta o estilo `locked`).
- `components/EditorPanels.tsx` — "Adicionar bloco" → `SERVICE`/estado local (array de blocos do editor); 7 `Field`/`SelectLike` do `BlockEditorCanvas` e do `SEOPanel` → `EDIT_FIELD`; CTAs de preview *dentro* do canvas (linha 41) → `READONLY_MARK` com nota — são preview do conteúdo sendo editado, não controles da tela, não tratar como botão morto comum; "Enviar para revisão"/"Publicar"/"Arquivar" → `SERVICE` (transição de workflow).
- `pages/ContentDataGrid.tsx` — filtro "Status / Idioma / Tipo / Autor" → `FILTER`.
- `pages/VersionCompareView.tsx` — "Voltar" → `BACK`; "Restaurar versão" → `SERVICE`.
- `pages/ResponsivePreviewFrame.tsx` — "Voltar ao editor" → `BACK`; "Enviar para revisão" → `SERVICE`; `SelectLike` "Viewport" → `TOGGLE`; "PT-BR" → `TOGGLE`; CTA dentro do canvas → `READONLY_MARK` (mesma nota do item anterior).
- `pages/PublishPanel.tsx` — "Salvar rascunho", "Agendar publicação", "Enviar para revisão" → `SERVICE`.

#### `settings`
- `pages/RoleManagement.tsx` — "Editar"/"Impacto" por role → `NAV`/`DRAWER`; "Restaurar padrão" → `SERVICE` (com `ConfirmDialog`); "Criar role" → `DRAWER`.
- `pages/PermissionMatrixView.tsx` — "Restaurar padrão" → `SERVICE` (com `ConfirmDialog`); "Salvar permissões" → `SERVICE`.
- `components/PermissionBits.tsx` — célula de permissão → `TOGGLE` (cicla estado local da matriz; persiste só quando "Salvar permissões" for clicado).
- `pages/TenantSettings.tsx` — "Cancelar" → `BACK`; "Salvar tenant" → `SERVICE` (`core/tenants/services` já existe).
- `pages/ProductSettings.tsx` — "Cancelar" → `BACK`; "Salvar alterações" → `SERVICE` (`productsService`).
- `pages/SecuritySettingsPanel.tsx` — "Configurar"/"Revisar sessões" → `NAV`/`DRAWER`; "Salvar segurança" → `SERVICE`.
- `pages/AccessPreviewPanel.tsx` — "Selecionar usuário" → `DRAWER`; "Gerar preview" → `SERVICE`; 3 `SelectLike` → `EDIT_FIELD`.

#### `tenants`
- `pages/CreateTenantForm.tsx` — `SelectLike` "Plano / tier" → `READONLY_MARK` (intencional — Starter fixo para novo tenant, Sprint 09).
- `pages/TenantsManagement.tsx` — confirmar que "Criar Tenant" navega corretamente para `/admin/tenants/new` (rota já existe em `app/routes/index.tsx`); se algum botão da listagem (linha/ação por tenant) estiver sem handler, aplicar `NAV` para o detalhe do tenant.

#### `users`
- `pages/InviteUserDrawer.tsx` — 3 `SelectLike` (Papel, Produtos permitidos, Módulos permitidos) → `EDIT_FIELD` (única tela já com `handleSend` funcionando — só os seletores estão mortos; depois da Tarefa A já funciona).
- `pages/AssignProductUserForm.tsx` — `SelectLike` "Papel neste produto" → `EDIT_FIELD`.
- `pages/UserDetailPanel.tsx` — "Reenviar convite" → `SERVICE`; "Bloquear" → `SERVICE` (com `ConfirmDialog`); "Editar permissões" → `NAV`/`DRAWER`.
- `pages/UserTable.tsx` — filtro "Papel / status / produto" → `FILTER`.

#### `assets`
- `components/AssetBits.tsx` — botão "..." → `MENU`.
- `components/AssetMetadataFormCard.tsx` — 8 `Field`/`SelectLike` → `EDIT_FIELD`; "Salvar metadados" → `SERVICE`; "Cancelar" → `BACK`.
- `pages/AssetTagManager.tsx` — "Mesclar tags" → `DRAWER` (seleção múltipla); **"Criar tag" → `DRAWER`, exemplo do enunciado do usuário — ver implementação de referência abaixo**; "Editar"/"Remover" por tag → `DRAWER`/`SERVICE` (Remover com `ConfirmDialog`).
- `pages/AssetPicker.tsx` — "Upload rápido" → `UPLOAD`; "Confirmar seleção"/"Usar asset" → `SERVICE`.
- `pages/AssetDetail.tsx` — "Copiar referência" → `COPY`; "Baixar" → `SERVICE` (mock download); "Substituir arquivo" → `UPLOAD`; "Arquivar" → `SERVICE` (com `ConfirmDialog`).
- `pages/AssetMetadataForm.tsx` — "Cancelar" → `BACK`; "Salvar" → `SERVICE`.
- `pages/AssetUsageScreen.tsx` — "Copiar relatório" → `COPY`; "Abrir asset" → `NAV`.
- `pages/AssetUploadScreen.tsx` — "Selecionar arquivos" → `UPLOAD`; "Cancelar" → `BACK`; "Concluir upload" → `SERVICE`.

#### `audit`
- `pages/AuditEventDetail.tsx` — "Copiar ID" → `COPY`; "Exportar evento" → `EXPORT`; "Abrir recurso" → `NAV`.
- `pages/AuditTimeline.tsx` — filtro → `FILTER`; "Exportar timeline" → `EXPORT`.

#### `knowledge`
- `pages/EntityDetails.tsx` — "Abrir recurso"/"Ver no Graph" → `NAV`.
- `pages/RelationshipExplorer.tsx` — filtro → `FILTER`; "Exportar" → `EXPORT`.
- `pages/EntitySearch.tsx` — "Filtros" → `FILTER`.
- `pages/KnowledgeInsights.tsx` — "Marcar revisado" → `SERVICE`.
- `pages/OrphanEntityTable.tsx` — "Selecionar todos" → `TOGGLE` (seleção em massa); "Resolver órfãos" → `SERVICE` (em lote); ações por linha (Arquivar/Vincular/Associar/Mesclar/Revisar) → `SERVICE`.

#### `analytics`
- `components/AnalyticsBits.tsx` — "Investigar"/"Ver detalhe" por KPI → `NAV`; "Últimos 30 dias" → `TOGGLE`; "Comparar período anterior" → `TOGGLE`; filtro "Canal" → `FILTER`; "Conteúdo" → `NAV` (confirmar destino pelo contexto exato do componente).
- `pages/FormAnalyticsModule.tsx` — "Formulário: todos" → `FILTER`.
- `pages/ReportGrid.tsx` — "Gerar" → `SERVICE`; "Baixar" → `EXPORT`; "Gerar relatório" → `SERVICE`.
- `pages/AnalyticsStates.tsx` — "Tentar novamente" (loading e erro) → `RETRY`.
- `pages/TrendCards.tsx` — "Marcar revisado" → `SERVICE`.
- `pages/ContentAnalytics.tsx` — "Filtrar idioma" → `FILTER`.
- `pages/ChannelBreakdown.tsx` — "Últimos 30 dias" → `TOGGLE`.
- `pages/ProductHealthPanel.tsx` — "Resolver pendência"/"Ver sinais" → `NAV`/`DRAWER`; "Atualizar leitura" → `RETRY`; "Gerar plano de ação" → `SERVICE`.

#### `dashboard`
- `pages/DashboardGlobal.tsx` — "Últimos 30 dias" → `TOGGLE`.

### D. Implementação de referência (o exemplo do enunciado)

`AssetTagManager.tsx`, botão "Criar tag" (`DRAWER`). Depois da correção, deve seguir o mesmo formato de `InviteUserDrawer.tsx`:

```tsx
const [open, setOpen] = useState(false);
const [name, setName] = useState("");
const [creating, setCreating] = useState(false);

const handleCreate = async () => {
  setCreating(true);
  try {
    await assetsService.createTag({ name });
    toast.success("Tag criada!", { description: `#${name} já está disponível para uso.` });
    setOpen(false);
  } finally {
    setCreating(false);
  }
};
```
com o `Button primary` abrindo um `Dialog`/`Drawer` contendo um `Field` para o nome e um botão de confirmação chamando `handleCreate`. Como `domains/assets` ainda não tem `services/`, criar `domains/assets/services/assetsService.ts` mínimo (lendo/gravando em `mocks/assets.mocks.ts`) como parte desta correção.

### E. Não regressão

Depois de cada domínio corrigido, rodar `npm run typecheck` e `npm run lint` (já configurados em `frontend/package.json`) antes de seguir para o próximo — os 140+ pontos tornam fácil deixar um `useState` ou import esquecido.

## Critérios de aceite

- [ ] `SelectLike` aceita `options`/`onChange`; `Field` em modo `textarea` é controlado.
- [ ] Nenhum `<Button>`/`<button>` em `src/domains/**` está sem `onClick` — exceto os marcados `READONLY_MARK` (e estes têm o prop `locked` e um comentário explicando o motivo).
- [ ] Todo `SelectLike`/`Field` dentro de um formulário de criação/edição é editável (`EDIT_FIELD`) e propaga para o estado do componente.
- [ ] As 8 abas de `ProductDetail.tsx` trocam de conteúdo ao clicar.
- [ ] `AssetTagManager.tsx`: "Criar tag" abre um drawer/dialog, cria a tag via `assetsService` (novo) e aparece na lista após criar.
- [ ] Toda ação destrutiva (Bloquear, Remover, Arquivar, Restaurar padrão) passa por `ConfirmDialog` antes de executar.
- [ ] `npm run typecheck` e `npm run lint` passam sem erros novos.
- [ ] Click-through manual em cada um dos 11 domínios confirma que todo botão visível produz uma reação perceptível (toast, navegação, abertura de drawer, mudança de estado, download).

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/10-refinamento-acoes-ui

git commit -m "fix(shared): selectlike aceita onchange/options; field textarea controlado"
git commit -m "fix(products): liga edicao, abas e modulos a acoes reais"
git commit -m "fix(forms): liga builder, submissions e publicacao a acoes reais"
git commit -m "fix(content): liga editor, versoes e publish panel a acoes reais"
git commit -m "fix(settings): liga roles, permissoes e configuracoes a acoes reais"
git commit -m "fix(tenants,users): liga fluxo super admin e gestao de usuarios a acoes reais"
git commit -m "fix(assets): cria assetsService e liga tags, upload e metadados a acoes reais"
git commit -m "fix(audit,knowledge,analytics,dashboard): liga filtros, exportacoes e retries a acoes reais"

git push -u origin sprint/10-refinamento-acoes-ui
```

Ao final, finalize a sprint no gitflow com o script já existente (merge `--no-ff` em `develop`, push, e remoção da branch só no remoto):

```bash
./scripts/gitflow-finish-sprint.sh sprint/10-refinamento-acoes-ui
```
