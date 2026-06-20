# Sprint 14 — Onboarding real e sistema de notificações remodelado

> Pré-requisito: nenhum específico desta sprint (independe de Sprint 11/12/13 estarem 100% concluídas), mas reaproveita componentes que a Sprint 13 já entregou (`Markdown.tsx`, `MarkdownField.tsx`, `MarkdownEditModal.tsx` — confirmados em produção, não mais planejados). Esta sprint também define uma nova etapa para a Sprint 02 (backend, GPT) — ver Seção F — a ser colada na próxima sessão de backend.

## Contexto

Investigação no código confirmou exatamente o que foi relatado:

1. **O "onboarding" nunca foi um onboarding de verdade.** Existe `app/layouts/DemoWelcomeModal.tsx`, montado em `AppShell.tsx` (linhas 47-49 e 166), controlado por `localStorage.getItem("aegis-welcomed")` — não por backend. O conteúdo dele é uma apresentação do **protótipo** (6 features de demo, atalhos, contas de teste), não um guia de uso real, e não tem nenhuma menção a "homologação". Ele "sumiu" porque, uma vez fechado, o `localStorage` do navegador marca `aegis-welcomed=1` e ele nunca mais aparece naquele navegador — não foi removido por nenhuma sprint, é o próprio mecanismo (frágil, client-side, por navegador, não por usuário) fazendo exatamente o que foi programado para fazer. É exatamente esse mecanismo que esta sprint substitui.
2. **O sino de notificações (`shared/components/Notifications.tsx`, montado em `AppShell.tsx` linha 125) é só uma lista de `mocks/timeline.ts`** — strings soltas (`text` + índice usado para alternar "Produto"/"Sistema" e escolher uma rota em `timelineRoutes`), sem tipo, sem remetente, sem estado de leitura persistido, sem nenhuma notificação "clicável que abre uma modal com o conteúdo completo".
3. **Não existe nenhuma ação "Criar notificação" hoje.** `core/auth/pages/TenantSelectScreen.tsx` (a tela de seleção de tenant do Super Admin) já tem o padrão certo para encaixar isso: botão "Criar Tenant" (linhas 99-107) abrindo `CreateTenantWizardModal`, e um `TenantContextMenu` por tenant (linhas 137-151) com Editar/Deletar. A nova ação não é por tenant (pode alcançar usuários de tenants diferentes), então entra como um segundo botão de topo, ao lado de "Criar Tenant", não dentro do menu de contexto de um tenant específico.
4. **`AuthUser` (`shared/types/auth.ts`) não tem nenhum campo de "primeiro acesso"/"onboarding concluído".** Precisa ganhar um, ou (melhor, ver Decisão abaixo) a informação vive inteiramente no novo domínio de notificações, sem precisar inflar `AuthUser`.
5. **Markdown já está em produção** (`react-markdown` no `package.json`, `Markdown.tsx`/`MarkdownField.tsx`/`MarkdownEditModal.tsx` implementados) — o corpo de qualquer notificação (onboarding ou criada pelo Super Admin) usa esse componente já existente, não cria um novo editor de texto.

### Decisão de modelo — onboarding é só um tipo de notificação, não um mecanismo separado

Em vez de criar dois sistemas (um para "modal de boas-vindas" e outro para "notificação do admin"), os dois são a **mesma coisa**: uma `Notification` com `type`. O onboarding é uma notificação especial, do tipo `ONBOARDING`, criada automaticamente (seed/no momento da criação do usuário) e atribuída a cada usuário — assim, o mecanismo de "aparece como modal na primeira vez, depois só fica no sino" serve para **qualquer** tipo de notificação, não só onboarding. Isso responde diretamente ao pedido de "remodelar pensando nos tipos de notificações que surgirão": o catálogo de tipos é extensível (`ONBOARDING`, `FEATURE`, `WARNING`, `MAINTENANCE`, `GENERAL`, ...) sem precisar de mecanismo novo a cada tipo.

Duas entidades, não uma só — porque uma notificação criada pelo Super Admin é **uma mensagem, vários destinatários**, e cada destinatário tem seu próprio estado de leitura:

- **`Notification`**: a mensagem em si — título, corpo (markdown), tipo, quem criou, quando, e como ela deve se apresentar (`presentationMode`: `MODAL_ONCE` — aparece como modal automaticamente na primeira oportunidade, depois só no sino; `BELL_ONLY` — nunca aparece como modal automático, só existe no sino, para avisos de baixa prioridade).
- **`UserNotificationStatus`**: uma linha por (notificação, destinatário) — guarda `autoShown` (já apareceu como modal automático para este usuário?) e `read` (este usuário já leu?). São independentes: o usuário pode ter visto a modal (`autoShown=true`) sem nunca ter "confirmado leitura" explicitamente, e pode reabrir a modal pelo sino depois de já lida, quantas vezes quiser.

### Decisão de gatilho — quando o modal automático aparece

"Primeiro acesso" não é por sessão de login, é **por (usuário, notificação)** — cada notificação nova que chega para um usuário tem seu próprio "primeiro acesso" a partir do momento em que existe. O ponto de checagem é **a entrada em um produto** (depois de selecionar tenant e produto, ao montar a tela do produto) — não a tela de login nem a tela de seleção de tenant/produto. Se houver mais de uma notificação pendente (`autoShown=false`) para o usuário, mostrar uma por vez, em fila (mais antiga primeiro), nunca todas empilhadas.

## Objetivo

0. **Corrigir, antes de qualquer tarefa abaixo**, os bugs de UI já visíveis na navbar mobile de `TenantSelectScreen`/`ProductSelectScreen` (telas que esta sprint vai editar de qualquer forma na Tarefa D) — ver Tarefa A0.
1. Substituir `DemoWelcomeModal`/`localStorage` por um onboarding real, persistido por usuário, com conteúdo de guia de uso + aviso de homologação.
2. Criar o domínio de notificações (frontend: contratos/mocks/service; backend: registrado como nova etapa da Sprint 02).
3. Super Admin cria notificações direcionadas, a partir da tela de seleção de tenant.
4. Remodelar o sino de notificações para consumir esse domínio real, com modal reabrível.

## Tarefas

### A0. Correções de UI na navbar mobile do Super Admin (fazer primeiro, antes de A-F)

Um agente paralelo já adicionou os menus hambúrguer mobile (`MobileDrawerMenu`, `shared/components/MobileDrawerMenu.tsx`) em `TenantSelectScreen.tsx` e `ProductSelectScreen.tsx`, mas não corrigiu bugs de label/layout que já existiam nessas telas. Como esta sprint (Tarefa D) já vai editar `TenantSelectScreen.tsx` para adicionar o botão "Criar Notificação", corrigir isso **primeiro**, no mesmo arquivo, evita um segundo round de edição:

1. **Pluralização "produtos" quebrada em `TenantSelectScreen.tsx`** — dois pontos, ambos sempre no plural mesmo quando o valor é 1:
   - Linha 130 (card de tenant): `{t.productCount} produtos` → `{t.productCount} produto{t.productCount !== 1 ? "s" : ""}`.
   - Linha 167 (texto do modal de confirmação de exclusão): `Todos os ${pendingDelete.productCount} produtos deste tenant...` → `Todos os ${pendingDelete.productCount} produto${pendingDelete.productCount !== 1 ? "s" : ""} deste tenant...`.
   - Replicar exatamente o padrão que **já está certo** na mesma tela (linha 96, `tenant${userTenants.length !== 1 ? "s" : ""}`) e em `ProductSelectScreen.tsx` (linha 117, `produto${tenantProducts.length !== 1 ? "s" : ""}`) — não inventar uma forma nova.
2. **Pluralização "módulos" quebrada em `ProductSelectScreen.tsx`, linha 54** — `{p.modules} módulos{p.modules === 0 ? " · sem módulos" : ""}` trata o caso de zero módulos mas não o de um módulo só (mostra "1 módulos"). Corrigir para `{p.modules} módulo{p.modules === 1 ? "" : "s"}{p.modules === 0 ? " · sem módulos" : ""}`.
3. **Menu hambúrguer "flutuando" isolado em `TenantSelectScreen.tsx` (navbar mobile)** — causa raiz: o bloco do título (linhas 93-98, sem largura definida) e o botão/`MobileDrawerMenu` (linhas 99-108) compartilham uma única linha `flex flex-wrap items-end justify-between` (linha 92); em telas estreitas, o bloco de título ocupa a linha inteira e empurra o menu para uma linha própria, sozinho, sem nada ao lado — diferente do padrão usado em `ProductSelectScreen.tsx`, onde o `MobileDrawerMenu` fica pareado com o campo de busca na mesma linha (linhas 119-134), parecendo intencional. Mover o botão "Criar Tenant"/`MobileDrawerMenu` de `TenantSelectScreen.tsx` para a mesma linha do campo de busca (perto da linha 110-112), replicando a estrutura de `ProductSelectScreen.tsx`, em vez de deixá-lo ao lado do título.
4. Depois da correção, conferir visualmente as duas telas em largura mobile (~390px) e confirmar que o hambúrguer aparece sempre pareado com algum outro elemento na mesma linha (nunca sozinho, sem nada ao lado, numa linha própria).

### A. Modelo de dados (frontend: contratos + mocks + service)

1. Criar `core/notifications/contracts/`: 
   ```ts
   type NotificationType = "ONBOARDING" | "FEATURE" | "WARNING" | "MAINTENANCE" | "GENERAL";
   type PresentationMode = "MODAL_ONCE" | "BELL_ONLY";
   type Notification = {
     id: string; type: NotificationType; title: string; bodyMarkdown: string;
     presentationMode: PresentationMode; createdBySubject: string; createdAt: string;
   };
   type UserNotificationStatus = {
     notificationId: string; autoShown: boolean; read: boolean; readAt?: string;
   };
   type NotificationWithStatus = Notification & UserNotificationStatus;
   ```
2. Criar `core/notifications/services/notificationsService.ts` (mock-backed, mesmo padrão dos demais services — `logApiCall`, Sprint 12 Tarefa A): `listMine(): NotificationWithStatus[]`, `getPendingModal(): NotificationWithStatus | null` (a mais antiga com `autoShown=false`), `markShown(id)`, `markRead(id)`, e (Super Admin) `create(request)`, `listAll()` (notificações criadas, para gestão/auditoria).
3. Mock inicial: pelo menos 1 notificação `ONBOARDING` (`presentationMode: "MODAL_ONCE"`) com `autoShown: false` para os usuários de teste novos/recém-criados, e 1-2 notificações `FEATURE`/`WARNING` de exemplo (uma já lida, uma não) para validar o sino com conteúdo variado.

### B. Onboarding real (substitui `DemoWelcomeModal`)

1. Renomear/recriar `app/layouts/DemoWelcomeModal.tsx` → `core/notifications/components/NotificationModal.tsx` — **um componente genérico** que recebe qualquer `Notification` (não só onboarding) e renderiza `title` + corpo via `<Markdown>` (componente já existente, Sprint 13) + um badge "Em homologação" quando o conteúdo mencionar isso (ou um campo `homologationBadge?: boolean` na notificação — mais simples e não depende de parsing de texto).
2. Reescrever o conteúdo da notificação `ONBOARDING` do mock como um **guia de uso real**, não a lista de features de demo atual: o que é o Aegis PMS, como navegar entre tenant/produto, onde encontrar conteúdo/páginas/assets/formulários, onde pedir ajuda (Central de Ajuda da Sprint 13, se já existir; senão, mencionar o botão "?"), e a frase explícita "Este produto está em processo de homologação — encontrou algo estranho? Reporte pelo botão de feedback." O texto fica no mock por enquanto, mas **o contrato já é `bodyMarkdown: string` vindo de `Notification`**, então no dia em que o backend existir, é só trocar a fonte do dado, não o componente.
3. Remover completamente a lógica de `localStorage.getItem("aegis-welcomed")` do `AppShell.tsx` (linhas 47-49, 166) — a partir desta sprint, "já viu o onboarding" é **só** `UserNotificationStatus.autoShown`, nunca mais client-side/por navegador.

### C. Gatilho de exibição automática

1. Criar `core/notifications/PendingNotificationGate.tsx` (ou hook `usePendingNotification()`): ao montar a tela de produto (ponto de entrada do produto, depois de tenant+produto selecionados — não em `/select-tenant` nem `/select-product`), chama `notificationsService.getPendingModal()`. Se houver resultado, renderiza `<NotificationModal>` com ela; ao fechar, chama `markShown(id)` e checa de novo (fila) — se houver outra pendente, mostra a próxima; se não, encerra.
2. Este Gate fica uma vez no layout do produto (não em cada página do produto), para não checar/mostrar de novo a cada navegação interna.

### D. Criar notificação (Super Admin, a partir de `TenantSelectScreen`)

1. Novo botão "Criar Notificação" em `TenantSelectScreen.tsx`, ao lado do "Criar Tenant" já existente (linhas 99-107) — visível só para `super_admin` (mesmo padrão de `PermGate`/checagem de papel já usado no botão de criar tenant).
2. Novo `CreateNotificationModal.tsx` (mesmo padrão visual de `CreateTenantWizardModal.tsx`): campos `title` (texto), `bodyMarkdown` (usar `MarkdownField`/`MarkdownEditModal` já existentes da Sprint 13 — não criar outro editor), `type` (select com o catálogo da Tarefa A.1), `presentationMode` (select: "Aparecer como modal na primeira vez" / "Só no sino"), e **seleção de destinatários**:
   - "Todos os usuários" (qualquer tenant/produto).
   - "Usuários de um tenant específico" (select de tenant, reaproveitando a lista que `TenantSelectScreen` já tem).
   - "Usuários específicos" (busca/multi-select por nome/e-mail — reaproveitar o padrão de busca de usuário já usado em `AssignProductUserForm`/`InviteUserDrawer`, Sprint 09/12).
3. Ao salvar, chama `notificationsService.create(request)` — no mock, isso "espalha" (fan-out) a notificação para os usuários-alvo simulados (criar uma `UserNotificationStatus` por destinatário no mock em memória), exatamente como o backend vai fazer de verdade (Seção F).

### E. Remodelar o sino de notificações

1. `shared/components/Notifications.tsx` deixa de ler `mocks/timeline.ts` e passa a chamar `notificationsService.listMine()`.
2. Cada item da lista mostra: ícone por `type` (ex.: 🎓/lightbulb para `ONBOARDING`, megafone para `FEATURE`, alerta para `WARNING`/`MAINTENANCE`), `title`, indicador de lido/não lido (reaproveitar o badge/dot já existente, linha 54, agora ligado a `read` real em vez de uma variável local solta), e tempo relativo a partir de `createdAt` (não mais o índice usado para simular "há N min").
3. Clicar num item **sempre** abre `NotificationModal` com o conteúdo completo (reaproveitando o componente da Tarefa B.1) — não navega mais para uma rota arbitrária via `timelineRoutes`. Se quiser manter uma ação de navegação para notificações que apontam para algo específico (ex.: "uma nova página foi publicada, ver página"), isso é um CTA **dentro** da modal (ex.: um botão "Ver página"), não o clique no item da lista. Abrir a modal não depende de `autoShown` — só marca `read=true` se ainda não estava.

### F. Backend — nova etapa da Sprint 02 (GPT)

Registrar como **etapa 23** (depois da 22, que continua sendo o checklist final — etapas novas entram numeradas depois daquilo que já existe, mesmo padrão já usado para a etapa 21/`pages`): domínio `notification`, com `Notification`, `UserNotificationStatus`, fan-out na criação, endpoints `GET /notifications/mine`, `GET /notifications/mine/pending-modal`, `POST /notifications/{id}/mark-shown`, `POST /notifications/{id}/mark-read`, `POST /notifications` (Super Admin), `GET /notifications` (Super Admin, gestão). Esta sprint **não escreve o arquivo da etapa 23 agora** — isso é trabalho desta mesma sessão, mas como tarefa separada (ver mensagem seguinte/Seção G abaixo), para manter esta sprint focada no frontend. O arquivo da etapa 23, quando criado, deve seguir integralmente o `00_padrao_qualidade_e_arquitetura.md` (Java 25, Spring Boot 4.1.x, Javadoc em repository, MapStruct, JaCoCo 100%, rodadas) — mesma régua de todas as etapas anteriores.

## Critérios de aceite

- [ ] `TenantSelectScreen.tsx` (linhas 130 e 167) e `ProductSelectScreen.tsx` (linha 54) nunca mostram "1 produtos"/"1 módulos" — singular correto em todos os contadores.
- [ ] Em largura mobile (~390px), o menu hambúrguer de `TenantSelectScreen.tsx` aparece pareado com o campo de busca (mesmo padrão de `ProductSelectScreen.tsx`), nunca isolado numa linha própria abaixo do subtítulo.
- [ ] `DemoWelcomeModal`/`localStorage("aegis-welcomed")` removidos; onboarding usa `UserNotificationStatus`.
- [ ] Onboarding aparece como modal a primeira vez que o usuário entra em um produto (qualquer produto), nunca mais depois disso, mesmo em outro navegador/dispositivo (a flag é do usuário no backend/mock, não do navegador).
- [ ] Conteúdo do onboarding é um guia de uso real, menciona explicitamente que o produto está em homologação, renderizado via `<Markdown>`.
- [ ] Super Admin vê o botão "Criar Notificação" em `/select-tenant`; outros papéis não veem.
- [ ] Criar notificação permite escolher tipo, modo de apresentação, e destinatários (todos / tenant / usuários específicos), com corpo em markdown via os componentes já existentes da Sprint 13.
- [ ] Notificação criada para "usuários específicos" aparece como modal (se `MODAL_ONCE`) só para esses usuários, na próxima vez que entrarem em um produto.
- [ ] Sino de notificações lista notificações reais (não `timeline.ts`), com indicador de lido/não lido real e ícone por tipo.
- [ ] Clicar numa notificação do sino sempre reabre a modal com o conteúdo completo, independente de já ter sido mostrada automaticamente antes.
- [ ] Se houver mais de uma notificação pendente, elas aparecem em fila (uma por vez), não simultâneas.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/14-onboarding-e-notificacoes

git commit -m "fix(ui): corrige pluralizacao de produtos/modulos e layout do menu mobile em tenant/product select"
git commit -m "feat(notifications): modelo de dados (notification + usernotificationstatus) e service mock"
git commit -m "feat(notifications): substitui demowelcomemodal por onboarding real persistido"
git commit -m "feat(notifications): gate de exibicao automatica ao entrar em um produto"
git commit -m "feat(notifications): super admin cria notificacao direcionada a partir do select-tenant"
git commit -m "feat(notifications): remodela sino de notificacoes para consumir dados reais"

git push -u origin sprint/14-onboarding-e-notificacoes
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/14-onboarding-e-notificacoes
```
