# Sprint 23 — Inbox de feedbacks e configuração Telegram por produto

> Pré-requisitos: Sprint 22 (tutorial de onboarding) e etapa 30 do backend (`30_seed_homologacao_e_remocao_seed_java.md`) — especificamente a Seção D.4, que adiciona o Telegram global do Aegis para `POST /feedback`, `GET/PUT /api/v1/products/{id}/settings/security` com `telegramAlert` para produtos/formularios, e os endpoints `GET /api/v1/feedback` / `GET /api/v1/tenants/{tenantId}/feedback`. O feedback **já é persistido** pela etapa 27 do backend; esta sprint só cria as telas que consumem o que já existe.
>
> **Por que agora:** o domínio `feedback` está implementado no backend desde a etapa 27, mas os registros só eram visíveis via banco de dados ou chamada direta à API. Com a etapa 30 adicionando o dispatch para o Telegram global do Aegis, o Super Admin pode receber alertas imediatamente — mas ainda precisa de uma tela para gerenciar o histórico e atualizar status. A configuração Telegram por produto continua existindo em `SecuritySettingsPanel`, mas pertence ao fluxo de produtos/formularios externos, nao ao fluxo interno de feedback do Aegis.
>
> **Branch:** `sprint/23-feedback-inbox-e-telegram`

---

## Contexto

Situação atual após a implementação das etapas 27 e 30:
- `POST /api/v1/feedback` persiste feedbacks com ID legível (`AGS-####`), categoria, prioridade, contexto da tela e anexo.
- `GET /api/v1/feedback` (Super Admin) e `GET /api/v1/tenants/{tenantId}/feedback` (Tenant Admin) respondem com a lista.
- `PUT /api/v1/feedback/{feedbackId}/status` permite atualizar o status (`aberto` → `em_analise` → `resolvido`).
- `POST /api/v1/feedback` envia alerta para o Telegram global do Aegis quando `aegis.telegram.alert.*` estiver ativo no backend.
- `GET/PUT /api/v1/products/{id}/settings/security` agora inclui `telegramAlert.chatId` + `telegramAlert.botTokenMasked` para fluxos de produtos/formularios externos.
- `SecuritySettingsPanel.tsx` já tem uma linha placeholder `["Telegram futuro", "futuro", "—", "—"]` esperando ser preenchida.
- `feedbackService.ts` já tem `create()` e `listAll()` implementados com toggle `IS_API_MODE`.

Lacunas que esta sprint fecha:
1. Nenhuma tela lista ou gerencia os feedbacks recebidos.
2. O Super Admin não tem como saber quantos feedbacks estão abertos sem consultar a API diretamente.
3. `SecuritySettingsPanel.tsx` mostra "Telegram futuro" mas não tem formulário real para configurar.
4. Não existe rota `/admin/feedback` no roteador.

---

## A. Rota e entrada de menu

### A.1 — Adicionar rota `/admin/feedback`

Em `AppRouter.tsx` (ou equivalente), registrar:

```tsx
<Route path="/admin/feedback" element={<FeedbackInboxPage />} />
```

A rota só é acessível para `SUPER_ADMIN` — mesma guarda de rota que `/admin/tenants` e `/admin/users` usam (ver `ProtectedRoute` ou equivalente).

### A.2 — Adicionar entrada na sidebar do Super Admin

Na sidebar, dentro da seção "Administração" (onde ficam Tenants, Usuários etc.), adicionar:

```
🐛 Feedbacks   → /admin/feedback
         badge: contagem de feedbacks com status "aberto"
```

O badge é um número inteiro ou "•" se >0. Buscar a contagem via `feedbackService.listAll()` filtrado por `status === "aberto"` — sem endpoint dedicado de contagem, só filtro client-side sobre o `listAll` que já existe.

---

## B. `FeedbackInboxPage` — tela principal

### B.1 — Layout e filtros

```
┌──────────────────────────────────────────────────────────────────────┐
│  Feedbacks recebidos                             [Filtrar por tenant] │
│  ─────────────────────────────────────────────────────────────────── │
│  [Todos] [Abertos N] [Em análise N] [Resolvidos N]   [Alta] [Crítica]│
│                                                                        │
│  AGS-1234  Bug · crítica   /content/list  CLIENTES BETA · Loki  [···]│
│  AGS-1235  UX confusa · m  /pages         CLIENTES BETA · CMSS   [···]│
│  …                                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

Filtros:
- Status (`aberto` / `em_analise` / `resolvido` / todos) — chips, um de cada vez.
- Prioridade (`alta` / `crítica`) — checkbox, múltiplos.
- Tenant (dropdown) — só para Super Admin; Tenant Admin vê apenas o próprio tenant.

Ordenação padrão: `priority desc, createdAt desc` (crítica primeiro, mais recente primeiro).

### B.2 — Linha de feedback

Cada linha exibe: ID legível (`AGS-####`) em `font-mono`, categoria, prioridade (chip colorido), tela (`screenName`), tenant/produto, data relativa (`2h atrás`, `ontem`), e botão "⋯" com menu:
- **Ver detalhes** → expande um drawer (ver B.3)
- **Marcar como em análise** / **Marcar como resolvido** → chama `PUT .../status`; atualiza a linha sem recarregar a página inteira.

Mapeamento de cores de prioridade (usar variáveis CSS do tema, não `bg-red-500` hardcoded):
- `crítica` → `bg-destructive/10 text-destructive border-destructive/20`
- `alta` → `bg-warning/10 text-warning border-warning/20` (se existir) ou `bg-orange-500/10 text-orange-600`
- `média` → `bg-muted text-muted-foreground`
- `baixa` → `bg-muted text-muted-foreground` (mais apagado)

### B.3 — Drawer de detalhe do feedback

Ao clicar em "Ver detalhes", abrir um drawer lateral (mesmo padrão de `ContentDetailDrawer` ou similar já existente):

```
AGS-1234 · Bug · crítica                            [Marcar resolvido ▾]
─────────────────────────────────────────────────────────────────────
Descrição
  Botão "Publicar" não responde ao clicar na tela de conteúdo.
  Reproduzível em 100% dos casos.

Contexto
  Tela: /content/list
  Usuário: joao@email.com
  Tenant: CLIENTES BETA · Produto: Loki
  Enviado em: 02/07/2026 às 14:23

Anexo
  [📎 screenshot.png  →  Baixar]    ← link para GET /assets/{id}/download
```

O link de download abre em nova aba. Nunca baixar o arquivo automaticamente — só exibir o link.

### B.4 — Estado vazio

Quando não houver feedbacks no filtro atual:

```
🎉  Nenhum feedback por aqui
Parece que tudo está funcionando bem.
[Ver todos os feedbacks]
```

---

## C. `feedbackService.ts` — novos métodos

Estender o `feedbackService` existente com os métodos de listagem por tenant e atualização de status, seguindo o mesmo padrão `IS_API_MODE` já estabelecido:

```ts
// src/core/notifications/services/feedbackService.ts

export const feedbackService = {
  // (create e listAll já existem — não alterar)

  /** GET /api/v1/tenants/{tenantId}/feedback — para Tenant Admin */
  async listByTenant(tenantId: string): Promise<FeedbackSummary[]> {
    if (IS_API_MODE) return apiClient.get<FeedbackSummary[]>(`/tenants/${tenantId}/feedback`);
    return feedbackStore.filter(f => f.tenantId === tenantId);
  },

  /** PUT /api/v1/feedback/{feedbackId}/status */
  async updateStatus(feedbackId: string, status: FeedbackStatus): Promise<void> {
    if (IS_API_MODE) {
      return apiClient.put<void>(`/feedback/${feedbackId}/status`, { status });
    }
    const item = feedbackStore.find(f => f.id === feedbackId);
    if (item) item.status = status;
  },
};
```

Atualizar também o tipo `FeedbackSummary` em `contracts/feedback.ts` para incluir `tenantId` e `productName` (campos que o backend já devolve mas o contrato atual não declara):

```ts
export type FeedbackSummary = CreateFeedbackRequest & {
  id: string;
  status: FeedbackStatus;
  createdAt: string;
  tenantId: string;           // adicionado
  productName?: string;       // adicionado — nome resolvido pelo backend, facilita a exibição
  createdBySubject: string;   // adicionado
};
```

Mock store: adicionar `tenantId` e `createdBySubject` ao `feedbackStore.push(...)` em `feedbackService.create()`, usando `effectiveTenant?.id` e `authUser?.email` como valores — exatamente o que o `FeedbackModal` já captura pelo `useAuth()`.

---

## D. Configuração Telegram em `SecuritySettingsPanel`

### D.1 — Substituir a linha placeholder por formulário real

`SecuritySettingsPanel.tsx` tem a linha:
```ts
["Telegram futuro", "futuro", "—", "—"],
```

Substituir pelo formulário de configuração. Não alterar a estrutura da tabela existente — adicionar uma seção separada abaixo da tabela de canais (ou onde couber sem quebrar o layout):

```
Alertas por Telegram
─────────────────────────────────────────────────────────────────────
Bot Token   [••••••••••••••••aBcD]  [Alterar]
Chat ID     [-1001234567890       ]  ✓ configurado
                                     [Salvar]  [Remover configuração]
```

Comportamento:
- Ao carregar, buscar via `GET /api/v1/products/{effectiveProduct.id}/settings/security` — exibir `telegramAlert.botTokenMasked` (nunca o token real).
- "Alterar" troca o campo masked por um `<input type="password">` para o usuário digitar o novo token.
- "Salvar" chama `PUT /api/v1/products/{effectiveProduct.id}/settings/security` com `{ telegramAlert: { chatId, botToken } }`.
- "Remover configuração" envia `{ telegramAlert: null }` — limpa os campos no backend.
- Após salvar, exibir um toast "Telegram configurado ✓" com `toast.success(...)`.

### D.2 — Mock de settings para Telegram

`settingsService.ts` (ou equivalente) — adicionar ao mock de retorno de `getProductSettings()`:

```ts
telegramAlert: null, // ou { chatId: "-1001234567890", botTokenMasked: "••••aBcD" }
```

e ao mock de `updateProductSettings(...)`, aceitar e persistir `telegramAlert` no store em memória.

---

## E. Critérios de aceite

- [ ] Rota `/admin/feedback` existe e só é acessível para `SUPER_ADMIN` — qualquer outro papel recebe redirect ou 403.
- [ ] Sidebar do Super Admin tem entrada "Feedbacks" com badge de abertos (badge some quando `count === 0`).
- [ ] `FeedbackInboxPage` lista todos os feedbacks (mode mock: os do `feedbackStore`; mode API: via `GET /feedback`).
- [ ] Filtros de status e prioridade funcionam client-side sem recarregar a página.
- [ ] "Marcar como em análise" / "Marcar como resolvido" atualiza o status na linha sem reload de página.
- [ ] Drawer de detalhe mostra todos os campos, incluindo link de download do anexo quando `attachmentAssetId` existe.
- [ ] Link de download do anexo aponta para `/api/v1/assets/{id}/download` e abre em nova aba.
- [ ] `SecuritySettingsPanel` mostra o formulário de Telegram (não o placeholder "futuro").
- [ ] Salvar configuração Telegram chama `PUT /products/{id}/settings/security` com `telegramAlert`; token nunca aparece em claro na UI após salvar (sempre mascarado).
- [ ] "Remover configuração" limpa o `telegramAlert` (mock: limpa do store; API: envia `null`).
- [ ] Em modo mock (`VITE_API_MODE !== 'api'`), todas as interações acima funcionam com os stores em memória — sem erros de runtime.
- [ ] Sem regressão em nenhuma funcionalidade existente (permissões, `FeedbackModal`, upload de asset no feedback).

---

## F. Commit sugerido

```bash
git add frontend/src/
git commit -m "feat(frontend): inbox de feedbacks para Super Admin e configuracao Telegram por produto"
```
