# figma-make-sprints-refinamento-v2.md

# AEGIS PMS
## Plano de Refinamento UX/UI para Figma Make
### Versão Pós-Protótipo + Revisão Arquitetural

---

# 1. CONTEXTO

A implementação atual do Aegis PMS já possui uma cobertura funcional ampla: dashboards, produtos, conteúdo, assets, forms, analytics, knowledge graph, settings, users, permissions e audit.

O problema atual não é falta de telas.

O problema atual é que o protótipo ainda parece uma coleção de telas administrativas, e não uma experiência navegável de produto.

A partir deste ponto, o objetivo não é gerar novos módulos.

O objetivo é transformar o protótipo em uma plataforma coerente, interativa, validável e navegável.

Aegis PMS deve sempre preservar a hierarquia mental:

```txt
Login
→ Tenant
→ Produto
→ Módulo
→ Recurso
```

E toda tela deve responder rapidamente:

- Onde estou?
- Quem sou neste contexto?
- Qual tenant está ativo?
- Qual produto está ativo?
- Qual módulo está ativo?
- O que posso fazer?
- O que aconteceu recentemente?
- O que exige minha atenção?

---

# 2. PRINCÍPIO DE EXECUÇÃO NO FIGMA MAKE

A partir de agora, o Figma Make deve atuar como refinador do protótipo existente.

Não recriar a aplicação do zero.

Não substituir toda a estrutura atual sem necessidade.

Não gerar novos módulos de negócio.

Não multiplicar telas desnecessariamente.

Não adicionar novas opções na sidebar sem justificativa.

Prioridade absoluta:

1. Corrigir fluxo.
2. Corrigir navegação.
3. Corrigir contexto.
4. Corrigir interatividade.
5. Corrigir permissões.
6. Corrigir identidade visual.
7. Corrigir estados.
8. Corrigir sensação de produto real.

---

# 3. ORDEM OFICIAL DE EXECUÇÃO

Executar as sprints nesta ordem:

```txt
Sprint 09 — Autenticação e Entrada na Plataforma
Sprint 10 — Reorganização da Navegação e Fluxos
Sprint 11 — Identidade Visual BYOP + Apple Inspired UI
Sprint 12 — Interatividade Real do Protótipo
Sprint 13 — Sistema Visual de Permissões e Personas
Sprint 14 — Workflow Editorial com Drag and Drop
Sprint 15 — Reportar Problema / Feedback de Validação
Sprint 16 — Knowledge Graph Orientado a Entidades de Negócio
Sprint 17 — Estados Globais e Microinterações
Sprint 18 — Auditoria UX por Jornada
Sprint 19 — Consolidação Final do Protótipo
```

---

# 4. SPRINT 09 — AUTENTICAÇÃO E ENTRADA NA PLATAFORMA

## Objetivo

Criar a jornada pública e inicial da plataforma.

O protótipo atual começa dentro do sistema, mas o Aegis PMS precisa possuir uma entrada real para validação de produto.

Esta sprint deve implementar:

- Login.
- Recuperação de senha.
- Primeiro acesso.
- Convite de usuário.
- Seleção de tenant.
- Seleção de produto.
- Redirecionamento para dashboard.
- Estados de erro e sucesso.

---

## Fluxos obrigatórios

### Fluxo A — Login padrão

```txt
Login
→ Autenticação bem-sucedida
→ Seleção de Tenant, se houver mais de um
→ Seleção de Produto, se houver mais de um
→ Dashboard Global ou Dashboard de Produto
```

### Fluxo B — Usuário com apenas um tenant e um produto

```txt
Login
→ Dashboard de Produto
```

### Fluxo C — Convite

```txt
Link de convite
→ Validar convite
→ Definir senha
→ Aceitar termos
→ Selecionar tenant
→ Dashboard
```

### Fluxo D — Recuperação de senha

```txt
Login
→ Esqueci minha senha
→ Informar email
→ Confirmar envio
→ Redefinir senha
→ Voltar ao login
```

---

## Telas a implementar ou refinar

### 09.01 — Login

Deve conter:

- Logo Aegis.
- Assinatura visual BYOP discreta.
- Campo email.
- Campo senha.
- Botão Entrar.
- Link Esqueci minha senha.
- Link Primeiro acesso, se aplicável.
- Mensagem de ambiente, se staging/local.
- Alternância de tema, se já existir.

Estados:

- vazio;
- preenchido;
- loading;
- credenciais inválidas;
- conta bloqueada;
- sessão expirada;
- erro de servidor;
- sucesso.

Direção visual:

- Apple Business.
- Minimalista.
- Premium.
- Muito espaço negativo.
- Fundo limpo.
- Card central elegante.
- Nada de admin template genérico.

---

### 09.02 — Recuperação de Senha

Deve conter:

- Campo email.
- Explicação curta.
- Botão enviar instruções.
- Link voltar ao login.

Estados:

- email inválido;
- enviando;
- enviado;
- erro;
- rate limit simulado.

---

### 09.03 — Redefinir Senha

Deve conter:

- Campo nova senha.
- Campo confirmar senha.
- Validação visual de força de senha.
- Botão redefinir.

Estados:

- token inválido;
- token expirado;
- senhas divergentes;
- senha fraca;
- sucesso.

---

### 09.04 — Primeiro Acesso / Convite

Deve conter:

- Boas-vindas.
- Nome do usuário convidado.
- Tenant que convidou.
- Papel atribuído.
- Produtos autorizados.
- Definição de senha.
- Aceite de termos.
- Botão ativar conta.

Estados:

- convite válido;
- convite expirado;
- convite revogado;
- convite já utilizado;
- ativação concluída.

---

### 09.05 — Seleção de Tenant

Deve conter:

- Lista de tenants disponíveis.
- Busca.
- Tenant atual destacado.
- Status do tenant.
- Último acesso.
- Botão entrar.

Estados:

- um tenant;
- múltiplos tenants;
- nenhum tenant;
- tenant suspenso;
- loading;
- erro.

---

### 09.06 — Seleção de Produto

Deve conter:

- Produtos do tenant selecionado.
- Busca.
- Filtro por status.
- Filtro por tipo.
- Produto favorito ou recente.
- Botão abrir produto.

Estados:

- um produto;
- múltiplos produtos;
- nenhum produto;
- produto arquivado;
- produto sem módulos;
- loading;
- erro.

---

## Critério de aceite da Sprint 09

A sprint estará concluída quando for possível simular:

```txt
Usuário não autenticado
→ login
→ seleção de tenant
→ seleção de produto
→ dashboard
```

E também:

```txt
convite
→ primeiro acesso
→ ativação
→ dashboard
```

### Feedback figma 

```
Sprint 09 está implementada. Aqui o que foi entregue:

Telas implementadas:

09.01 — Login: Card central minimalista com logo Aegis, campos email/senha, toggle de visibilidade de senha, loading state "Entrando...", 4 estados de erro (credenciais inválidas, conta bloqueada, sessão expirada, erro de servidor), link para recuperação e convite, badge staging, painel de contas demo
09.02 — Recuperação de Senha: Campo email, loading state, erro de e-mail inválido, rate limit após 3 tentativas
09.03 — Redefinir Senha: Indicador de força de senha (fraca/média/forte com barra colorida), validação de senhas divergentes, estados token expirado/inválido com botões de simulação, feedback de sucesso
09.04 — Convite / Primeiro Acesso: Card de boas-vindas com nome, tenant, papel e produtos autorizados, definição de senha, aceite de termos, estados válido/expirado/revogado/já utilizado/ativado
09.05 — Seleção de Tenant: Header com usuário + role badge, busca, grid de cards com status ativo/suspenso, último acesso, número de produtos; tenants suspensos desabilitados
09.06 — Seleção de Produto: Busca + filtros por status, seções Favoritos / Recentemente acessados / Todos os produtos, produtos arquivados e sem módulos desabilitados com visual dimmed
Fluxos funcionando:

Fluxo A (admin@byop.io): Login → TenantSelect (3 tenants) → ProductSelect → AppShell
Fluxo B (editor@byop.io): Login → AppShell direto (1 tenant, 1 produto — skip automático)
Fluxo C (Convite): InviteScreen → ativar conta → AppShell
Fluxo D (Recuperação): Login → ForgotPassword → Sent → ResetPassword → Login
AppShell atualizado: switchers Tenant/Produto refletem o contexto autenticado; avatar mostra iniciais e nome do usuário; dropdown com badge de role e botão "Sair da plataforma".
```


---

# 5. SPRINT 10 — REORGANIZAÇÃO DA NAVEGAÇÃO E FLUXOS

## Objetivo

Reduzir a bagunça visual da sidebar.

O protótipo atual possui muitas entradas de menu que são, na verdade, etapas internas de fluxos.

A sidebar deve representar módulos principais, não telas secundárias.

---

## Sidebar final obrigatória

A sidebar deve conter apenas:

```txt
Dashboard
Produtos
Conteúdo
Assets
Forms
Analytics
Knowledge Graph
Configurações
Auditoria
```

Opcional, dependendo da implementação atual:

```txt
Usuários
```

Mas se Usuários estiver dentro de Configurações, não duplicar.

---

## Itens que devem sair da sidebar

Remover da navegação principal:

- Upload Asset.
- Detalhe Asset.
- Asset Metadata.
- Asset Usage.
- Asset Picker.
- Preview de Conteúdo.
- Publish Panel.
- Version History.
- Version Compare.
- Workflow interno.
- Form Builder.
- Submission Detail.
- Reports Detail.
- Node Inspector.
- Relationship Explorer.
- Entity Details.
- Impact Analysis.
- Invite User.
- User Detail.
- Audit Event Detail.

Esses itens devem existir como:

- drawers;
- modals;
- tabs;
- subrotas internas;
- painéis contextuais;
- estados internos da tela principal.

---

## Reorganização por módulo

### Conteúdo

Menu principal:

```txt
Conteúdo
```

Dentro do módulo:

- Dashboard Editorial.
- Lista de Conteúdos.
- Workflow.
- Editor.
- Preview.
- Publish Panel.
- Histórico.
- Comparação.

Mas apenas Conteúdo aparece na sidebar.

---

### Assets

Menu principal:

```txt
Assets
```

Dentro do módulo:

- Biblioteca.
- Upload.
- Preview.
- Metadados.
- Tags.
- Uso.
- Asset Picker.

Mas apenas Assets aparece na sidebar.

---

### Forms

Menu principal:

```txt
Forms
```

Dentro do módulo:

- Lista.
- Builder.
- Preview.
- Submissions.
- Submission Detail.
- Publicação.
- Analytics básico.

Mas apenas Forms aparece na sidebar.

---

### Analytics

Menu principal:

```txt
Analytics
```

Dentro do módulo:

- Overview.
- Product Health.
- Content Analytics.
- Form Analytics.
- Traffic.
- Reports.
- Trend Cards.

Mas apenas Analytics aparece na sidebar.

---

### Knowledge Graph

Menu principal:

```txt
Knowledge Graph
```

Dentro do módulo:

- Overview.
- Graph Canvas.
- Relationship Explorer.
- Entity Details.
- Impact Analysis.
- Entity Search.
- Orphans.
- Insights.

Mas apenas Knowledge Graph aparece na sidebar.

---

## Navegação contextual obrigatória

Cada módulo deve possuir uma navegação interna clara:

- tabs;
- segmented control;
- breadcrumb secundário;
- botões contextuais;
- drawers.

Exemplo para Assets:

```txt
Assets
├ Biblioteca
├ Tags
├ Uso
└ Upload via CTA
```

Exemplo para Content:

```txt
Conteúdo
├ Dashboard
├ Lista
├ Workflow
└ Histórico
```

---

## Critério de aceite da Sprint 10

A sprint estará concluída quando:

- a sidebar tiver no máximo 9 entradas principais;
- nenhum detalhe de fluxo aparecer como menu principal;
- o usuário conseguir entrar em cada módulo e navegar internamente;
- breadcrumbs continuarem claros;
- nenhuma tela importante desaparecer, apenas mudar de lugar.

### Feedback figma

```

Sprint 10 entregue. Aqui o que foi implementado:

Sidebar reduzida de 60+ entradas para 9 módulos principais:

Dashboard · Produtos · Conteúdo · Assets · Forms
Analytics · Knowledge Graph · Configurações · Auditoria
Navegação interna por tabs em cada módulo:

Conteúdo: Dashboard | Lista | Workflow | Editor | Histórico
Assets: Biblioteca | Tags | Impacto
Forms: Dashboard | Formulários | Submissions | Publicação | Analytics
Analytics: Overview | Saúde | Conteúdo | Forms | Canais | Relatórios | Tendências
Knowledge Graph: Overview | Graph | Relações | Busca | Órfãos | Insights
Configurações: Visão Geral | Produto | Tenant | Usuários | Permissões | Segurança
Auditoria: Timeline | Evento
Mecanismos técnicos:

parentModule — mapa de todas as sub-telas para seu módulo pai, garante que o item correto da sidebar fique ativo ao navegar internamente (ex: estando no editor, a sidebar destaca "Conteúdo")
moduleTabs — mapa de tabs por tela, cada sub-tela do módulo exibe o mesmo conjunto de tabs com o item ativo correto
ModuleTabs — componente tab bar com underline ativo no estilo Linear, renderizado acima do conteúdo de cada módulo
Nenhuma tela existente foi removida — todas foram reorganizadas como estados internos acessíveis pelos tabs

```


---

# 6. SPRINT 11 — IDENTIDADE VISUAL BYOP + APPLE INSPIRED UI

## Objetivo

Dar identidade visual forte ao Aegis PMS.

O protótipo atual está funcional, mas visualmente neutro.

A nova direção visual deve combinar:

```txt
Apple Business
+ SaaS premium
+ BYOP Violet
+ clareza operacional
```

---

## Tema Light

Características:

- branco predominante;
- superfícies suaves;
- bordas quase invisíveis;
- sombras sutis;
- tipografia limpa;
- muito espaço negativo;
- aparência premium.

Tokens sugeridos:

```css
--background: #f5f5f7;
--surface: #ffffff;
--surface-secondary: #fbfbfd;
--text-primary: #1d1d1f;
--text-secondary: #6e6e73;
--border: rgba(0, 0, 0, 0.08);
--byop-violet: #8b5cf6;
--byop-violet-soft: rgba(139, 92, 246, 0.12);
```

---

## Tema Dark

Características:

- inspirado em macOS dark;
- preto suave, não preto puro;
- superfícies em camadas;
- BYOP Violet como acento;
- contraste confortável;
- charts adaptados.

Tokens sugeridos:

```css
--background: #0f0f11;
--surface: #1c1c1e;
--surface-secondary: #2c2c2e;
--text-primary: #f5f5f7;
--text-secondary: #a1a1aa;
--border: rgba(255, 255, 255, 0.10);
--byop-violet: #a78bfa;
--byop-violet-soft: rgba(167, 139, 250, 0.16);
```

---

## Auto Theme

Implementar:

- detecção do sistema operacional;
- preferência manual do usuário;
- persistência local;
- toggle no profile ou header;
- transição suave entre temas.

Ordem de prioridade:

1. Preferência manual.
2. Preferência salva.
3. Sistema operacional.
4. Light como fallback.

---

## Componentes que devem receber refinamento visual

- App Shell.
- Header.
- Sidebar.
- Cards.
- Tabelas.
- Modais.
- Drawers.
- Buttons.
- Inputs.
- Badges.
- Toasts.
- Charts.
- Graph.
- Login.

---

## Critério de aceite da Sprint 11

A sprint estará concluída quando:

- existir Light Theme;
- existir Dark Theme;
- existir Auto Theme;
- BYOP Violet estiver aplicado de forma consistente;
- o produto parecer premium;
- o produto não parecer template admin genérico.

### Feedback figma

```

The logo is a beautiful purple/violet shield with "AEGIS PMS" — perfect for the BYOP Violet identity. Let me implement everything:

Now update App.tsx — add logo import, new icons, useTheme hook, and AegisLogo component:

Now update AuthLogo to use the PNG logo and refine the auth card:

Let me do a quick scan for any remaining hardcoded old-green values that should be updated:

Everything looks correct. Sprint 11 is complete. Here's what was delivered:

theme.css — rewritten com Apple × BYOP Violet:

Light: fundo #f5f5f7, superfícies brancas, bordas rgba(0,0,0,0.08) quase invisíveis, primary #7c3aed (violet escuro para contraste), accent #8b5cf6 (BYOP Violet)
Dark: fundo #0f0f11, cards #1c1c1e, secondary #2c2c2e em camadas, primary #a78bfa (violet claro para fundo escuro), bordas rgba(255,255,255,0.10)
Novos tokens --byop-violet, --byop-violet-soft, --byop-violet-dark expostos no @theme inline
--radius: 0.75rem (mais Apple), -webkit-font-smoothing: antialiased, scrollbar styling customizado
App.tsx — atualizações visuais:

Logo: PNG src/imports/logo.png importado como AegisLogo, substituindo ShieldCheck em todos os contextos (sidebar, auth, tenant/product select)
useTheme hook: detecta preferência do sistema, persiste em localStorage, aplica classe .dark no documentElement, escuta mudanças do prefers-color-scheme em modo auto
Theme toggle: botão no header com ícones Sun/Moon/Monitor, cicla light → dark → auto, aparece também no dropdown do usuário
Badge: cores Apple semânticas (verde #dcfce7, azul #dbeafe, vermelho #fee2e2) + novo tom violet para badges de identidade BYOP
Button primary: sombra atualizada para rgba(124,58,237,.25) (violet)
Card: sombra Apple multicamadas leve, sem sombra no dark mode
ModuleTabs: underline ativo usa var(--byop-violet) diretamente
PageHeader: badges passaram de green para violet
Header AppShell: altura 56px, backdrop-filter real com saturação, glass morphism sutil

et me read the current state of both files before making changes:

The layout issue is clear: the current structure uses min-h-screen on the root with flex inside, which doesn't give the sidebar a concrete height to fill. The fix is h-screen flex-col on the root → flex-1 overflow-hidden on the body row → overflow-y-auto on both sidebar and main so each scrolls independently.

Three targeted edits. The layout now works like this:

<div class="flex h-screen flex-col overflow-hidden">      ← locks to viewport height
  <header class="shrink-0 h-14 ...">                      ← never shrinks
  <div class="flex flex-1 overflow-hidden">               ← takes all remaining height
    <aside class="h-full w-[272px] overflow-y-auto ...">  ← fills height, scrolls alone
    <main class="flex-1 overflow-y-auto ...">             ← fills width, scrolls alone
The sidebar now fills 100% of the viewport height (below the header) and stays fixed there — it only scrolls its own content if the nav list overflows. The main content area scrolls independently, so long pages don't affect the sidebar position. The mobile drawer overlay was already fixed inset-0 so it's unaffected.

```

---

# 7. SPRINT 12 — INTERATIVIDADE REAL DO PROTÓTIPO

## Objetivo

Eliminar a sensação de protótipo estático.

Toda ação visível deve produzir algum resultado visível.

---

## Interações obrigatórias

### Tenant Switcher

Ao clicar:

- abrir dropdown;
- permitir busca;
- selecionar tenant;
- atualizar contexto;
- atualizar breadcrumbs;
- atualizar dados mockados.

---

### Product Switcher

Ao clicar:

- abrir dropdown;
- permitir busca;
- selecionar produto;
- atualizar dashboard;
- atualizar módulos;
- atualizar breadcrumbs.

---

### Global Search

Atalho:

```txt
Cmd + K
Ctrl + K
```

Deve permitir buscar:

- produtos;
- conteúdos;
- assets;
- formulários;
- usuários;
- knowledge nodes.

Ao selecionar resultado:

- navegar para o recurso correspondente;
- atualizar contexto.

---

### Cards clicáveis

Todos os cards com CTA devem navegar ou abrir drawer/modal.

Exemplos:

- Product Card → Detalhe do Produto.
- Asset Card → Preview do Asset.
- Content Card → Editor ou Detalhe.
- Trend Card → Analytics filtrado.
- Insight Card → Recurso relacionado.

---

### Filtros

Filtros devem alterar visualmente os dados mockados.

Não precisam fazer busca real de backend.

Mas devem parecer funcionais.

---

### Breadcrumbs

Cada nível clicável deve voltar para o contexto correto.

Exemplo:

```txt
BYOP → Maestro Beton → Conteúdo → Home
```

Clicar em Conteúdo volta para o módulo Conteúdo.

---

## Critério de aceite da Sprint 12

A sprint estará concluída quando:

- nenhum dropdown parecer morto;
- nenhum CTA principal parecer morto;
- filtros alterarem resultados;
- cards navegarem ou abrirem painéis;
- breadcrumbs forem úteis;
- o protótipo permitir simular jornadas reais.


### Feedback figma

```
Now add the search index and rewrite GlobalSearch:

Now wire DashboardGlobal KPIs and OperationalTimeline to navigate:

Now update AppShell — add new props, useRef sync effects, real Switcher data, pass go to GlobalSearch:

Finally, update the App default export with switch handlers and new props:

Sprint 12 entregue. Aqui o resumo completo do que foi implementado:

Tenant Switcher — agora funcional: exibe todos os tenants disponíveis do usuário autenticado (filtrados por status ativo), permite busca, mostra checkmark no tenant ativo, ao selecionar chama onSwitchTenant → resets produto + navega para global dashboard. O AppShell sincroniza via useEffect + useRef para detectar mudança de tenant e navegar automaticamente.

Product Switcher — funcional: lista todos os produtos ativos com módulos do tenant atual, ao selecionar chama onSwitchProduct + setScreen("product"). O AppShell detecta mudança de produto e navega para o dashboard do produto.

Global Search (⌘K) — completamente reescrito com 19 itens indexados em 6 categorias (Produtos, Conteúdo, Assets, Formulários, Entidades KG, Configurações). Busca filtra em tempo real por label + sub + categoria. Clicar em qualquer resultado fecha o modal e navega para o screen correto. ESC fecha, X limpa busca.

Breadcrumbs clicáveis — "Plataforma" e nome do tenant navegam para global, nome do produto navega para product. Thread go propagada por PageHeader → Breadcrumb.

KPIs clicáveis no Dashboard Global — cada KPI Widget navega: Produtos → products, Conteúdo → contentList, Aprovações → workflow, Formulários → submissions, Assets → assets, Usuários → usersMgmt, Conversão → analytics.

OperationalTimeline — itens clicáveis que navegam para screens contextualmente relevantes (product, contentList, workflow, assets).

TrendCards — cards clicáveis que navegam para Analytics; KnowledgeInsightCard navega para entityDetails.

ProductsList — filtro de status adicionado (Todos / Ativo / Pendente / Arquivado / Sem módulos) com chips visuais selecionáveis, search com botão × para limpar.

Aproveite para deixar os breadcrumbs clicáveis e opções de voltar a página anterior, não consigo acessar a tela inicial novamente após acessar um produto

The root issue is that go isn't threaded through most screen components' PageHeader, making breadcrumbs non-interactive. The cleanest fix: add a persistent AppNav inside AppShell's <main> (always clickable, always correct context) and remove the old static Breadcrumb from PageHeader.

The logic is correct. Let me verify the key navigation paths mentally:

Screen	Renders
global	BYOP
products	BYOP › Produtos
product	BYOP › Produtos › Maestro Beton
content	BYOP › Produtos › Maestro Beton › Conteúdo
editor	BYOP › Produtos › Maestro Beton › Conteúdo [btn] › Editor
settings	BYOP › Produtos › Maestro Beton › Configurações
usersMgmt	BYOP › Produtos › Maestro Beton › Configurações [btn] › Usuários
All levels are clickable buttons except the last (current location). Clicking Produtos goes to the products list. Clicking the product name goes back to the product dashboard. Clicking the tenant name goes to global dashboard. The sidebar's Dashboard entry also goes to global.

The static Breadcrumb component is still in the file (unused now) but causes no errors — it just isn't called anywhere. The PageHeader lost its <Breadcrumb> call and gained tighter spacing (gap-3, pb-5, mt-1.5).
```

---

# 8. SPRINT 13 — SISTEMA VISUAL DE PERMISSÕES E PERSONAS

## Objetivo

Permitir validar o protótipo com diferentes perfis de usuário.

O Aegis possui personas distintas:

- Super Admin.
- Tenant Admin.
- Product Manager.
- Editor.
- Viewer.

O protótipo precisa simular diferenças reais entre elas.

---

## Implementar Role Simulator

Adicionar um seletor global discreto no header ou profile menu:

```txt
Visualizando como: Product Manager
```

Permitir trocar entre:

- Super Admin.
- Tenant Admin.
- Product Manager.
- Editor.
- Viewer.

---

## Comportamento por perfil

### Super Admin

Pode ver:

- todos os tenants;
- todos os produtos;
- usuários;
- permissões;
- auditoria completa;
- configurações globais.

---

### Tenant Admin

Pode ver:

- tenant atual;
- produtos do tenant;
- usuários do tenant;
- permissões do tenant;
- analytics do tenant;
- auditoria do tenant.

Não deve ver:

- configurações globais da plataforma.

---

### Product Manager

Pode ver:

- produtos autorizados;
- conteúdo;
- assets;
- forms;
- analytics;
- knowledge graph;
- publicação;
- pendências.

Não deve ver:

- administração global;
- configurações sensíveis do tenant.

---

### Editor

Pode ver:

- conteúdo;
- editor;
- assets;
- workflow;
- histórico do conteúdo.

Não pode:

- publicar diretamente, se depender de aprovação;
- alterar permissões;
- alterar tenant;
- acessar auditoria completa.

---

### Viewer

Pode ver:

- dashboards permitidos;
- conteúdo em leitura;
- analytics se autorizado.

Não pode:

- criar;
- editar;
- publicar;
- excluir;
- configurar.

---

## Estados visuais obrigatórios

- ação oculta;
- ação desabilitada com tooltip;
- tela sem permissão;
- widget parcialmente oculto;
- aviso de papel insuficiente;
- modo somente leitura.

---

## Critério de aceite da Sprint 13

A sprint estará concluída quando for possível trocar persona e ver a interface mudar imediatamente.

### Feedback figma

```
Sprint 13 entregue. Aqui o que foi implementado:

Role Simulator — no menu do usuário (dropdown do avatar), seção "Simular perfil" com os 5 papéis clicáveis. Cada linha mostra o papel com checkmark quando ativo e a primeira palavra da descrição quando inativo. Trocar de papel é instantâneo.

SimulationBanner — barra discreta em violet-soft abaixo do header sempre que o perfil simulado é diferente do perfil real. Mostra "Simulando como: Editor — Cria e revisa conteúdo" com botão "← Restaurar" para voltar ao papel original.

Sidebar filtrada por papel:

Super Admin / Tenant Admin → 9 itens (acesso completo)
Product Manager → 8 itens (sem Auditoria)
Editor → 6 itens (sem Knowledge Graph, Configurações, Auditoria)
Viewer → 4 itens (Dashboard, Produtos, Conteúdo, Analytics)
NoPermScreen — tela bloqueada com ícone de cadeado vermelho, descrição do papel e badges. Aparece ao tentar acessar:

Editor → Configurações, Publicação, Auditoria, criar produto, e mais
Viewer → todo o acima + Editor, Builder, Upload, Knowledge Graph, Workflow
Product Manager → Tenant Settings, Usuários, Permissões, Auditoria
ReadOnlyBanner — faixa amarela com "Modo somente leitura" para Viewer em telas que normalmente são editáveis, e para Editor em telas de aprovação/permissões.

Widget-level permissions no Dashboard Global:

"Usuários ativos" fica travado (🔒) para Editor e Viewer
"Financeiro" fica travado para todos exceto Super Admin
"Criar produto" fica oculto para Editor e Viewer
PermGate — componente reutilizável <PermGate allowed={bool}> para condicionar qualquer elemento a uma permissão.
```

---

# 9. SPRINT 14 — WORKFLOW EDITORIAL COM DRAG AND DROP

## Objetivo

Transformar o workflow editorial em uma experiência realista.

O fluxo editorial oficial é:

```txt
Draft
→ In Review
→ Published
→ Archived
```

---

## Implementar drag and drop

O usuário deve conseguir arrastar cards entre colunas.

Ao soltar em uma coluna diferente, abrir modal de confirmação.

---

## Modal de confirmação

Título:

```txt
Confirmar mudança de status?
```

Conteúdo:

- conteúdo afetado;
- status anterior;
- novo status;
- responsável;
- impacto da mudança;
- campo comentário;
- CTA confirmar;
- CTA cancelar.

---

## Regras

### Draft → In Review

Permitido para Editor.

Exigir comentário opcional.

---

### In Review → Published

Permitido apenas para Product Manager, Tenant Admin ou Super Admin.

Exigir confirmação clara.

---

### Published → Archived

Ação sensível.

Exigir comentário obrigatório.

Gerar evento de auditoria simulado.

---

### Archived → Draft

Restaurar como nova versão.

Exigir confirmação.

---

## Atualizações após confirmação

- mover card;
- atualizar contador da lane;
- adicionar evento na timeline;
- exibir toast;
- atualizar histórico.

---

## Critério de aceite da Sprint 14

A sprint estará concluída quando o fluxo editorial puder ser simulado sem parecer estático.


### Feedback figma

```
Sprint 14 entregue. Aqui o que foi implementado:

Drag & Drop real — react-dnd com HTML5Backend. Cada WFCard é um drag source com handle GripVertical, opacidade 35% enquanto arrastando. Cada WFLane é um drop target com ring violeta e texto "↓ Soltar aqui" quando hover ativo.

8 cards com estado real — useState inicializado com os 8 itens de conteúdo (Home, Sobre o Maestro, Apresentações, Galeria, Depoimentos, Disponibilidade, Contato, Agenda). Contadores das lanes atualizam ao mover.

TransitionModal — abre ao soltar em nova lane, mostra:

Card de contexto: título, tipo, idioma, autor, versão
Bloco de mudança de status: [Draft] → [In Review] com badges coloridos
Bloco de impacto em amarelo (contextual por destino)
Bloco de erro em vermelho se transição não permitida (ex: Editor tentando publicar)
Campo comentário com asterisco * obrigatório para Archived
Botão "Sem permissão" desabilitado quando role insuficiente
Regras de transição:

Draft → In Review: qualquer editor
In Review → Published: PM ou superior; outros veem mensagem de bloqueio
Published → Archived: todos com permissão; comentário obrigatório
Archived → Draft: restaura como nova versão
Saltos não sequenciais (Draft → Published) são bloqueados com explicação
Timeline da sessão — painel lateral que acumula todos os eventos da sessão com texto, comentário em itálico, timestamp e papel do usuário simulado.

Toasts — sonner com richColors: sucesso ao mover, info adicional ao arquivar ("Evento de auditoria registrado").

Integração com Role Simulator — badge no header do board muda entre "Publicação permitida" (PM+) e "Somente Draft → In Review" (Editor/Viewer). O modal reflete as permissões do papel simulado em tempo real.
```

---

# 10. SPRINT 15 — REPORTAR PROBLEMA / FEEDBACK DE VALIDAÇÃO

## Objetivo

Adicionar funcionalidade fundamental para validação do produto.

Usuários precisam reportar problemas diretamente pelo protótipo.

---

## Localização

Adicionar no menu do perfil:

```txt
Reportar problema
```

Também adicionar atalho discreto no footer ou help menu:

```txt
Enviar feedback
```

---

## Modal de reporte

Campos:

- categoria;
- descrição;
- prioridade;
- tela atual;
- usuário atual;
- tenant atual;
- produto atual;
- anexos simulados;
- checkbox “incluir contexto da tela”.

Categorias:

- Bug.
- UX confusa.
- Erro visual.
- Permissão incorreta.
- Informação errada.
- Sugestão.

Prioridade:

- baixa;
- média;
- alta;
- crítica.

---

## Resultado simulado

Após envio:

- mostrar toast de sucesso;
- gerar ID de issue simulada;
- permitir copiar ID;
- adicionar item em atividade recente.

---

## Critério de aceite da Sprint 15

A sprint estará concluída quando qualquer usuário conseguir simular envio de bug/feedback.

### Feedback figma

```
Sprint 15 entregue. Aqui o que foi implementado:

Dois pontos de entrada para o modal de feedback:

Menu do usuário — item "Reportar problema" com ícone de alerta âmbar, posicionado acima da seção de simular perfil
Botão HelpCircle no header — agora tem onClick e tooltip "Enviar feedback"
FeedbackModal com todos os campos:

Categoria — chips clicáveis: Bug, UX confusa, Erro visual, Permissão incorreta, Informação errada, Sugestão
Prioridade — 4 botões segmentados: baixa, média, alta, crítica
Descrição — textarea com placeholder contextual (obrigatório)
Contexto da tela — painel automático mostrando tela atual, usuário, email, tenant e produto; controlado por checkbox
Anexar arquivo — botão simulado que alterna entre "Anexar arquivo" e mostra o nome do arquivo "screenshot-XXXX.png" (sem upload real)
Animação items-end mobile → items-center desktop
Estado de sucesso — após 850ms de loading:

Modal muda para tela de confirmação com ícone violeta, texto de agradecimento
ID gerado no formato AGS-XXXX (ex: AGS-2847) em fonte mono grande
Botão "Copiar ID" que usa clipboard.writeText e muda para "Copiado ✓"
Toast sonner simultâneo com ID e descrição
Integração automática de contexto — a tela atual vem de screenLabels[screen], o usuário/tenant/produto vêm de auth, garantindo que o formulário já chegue pré-preenchido com dados relevantes para validação.
```

---

# 11. SPRINT 16 — KNOWLEDGE GRAPH ORIENTADO A ENTIDADES DE NEGÓCIO

## Objetivo

Corrigir o Knowledge Graph.

O problema atual é que o graph liga telas ou áreas do sistema.

Isso está errado.

O Knowledge Graph deve ligar entidades reais de negócio.

---

## Entidades válidas

- Tenant.
- Produto.
- Conteúdo.
- Página.
- Bloco.
- Asset.
- Formulário.
- Submission.
- Lead.
- Categoria.
- Tag.
- Autor.
- SEO.
- Usuário.
- Evento de auditoria.

---

## Relações válidas

Exemplos:

```txt
Produto → possui → Conteúdo
Produto → possui → Asset
Produto → possui → Formulário
Página Home → usa → Asset Hero
Página Home → contém → Formulário Orçamento
Conteúdo Sobre → pertence a → Categoria Institucional
Conteúdo Sobre → escrito por → Usuário Editor
Formulário Orçamento → gera → Submission
Submission → cria → Lead
Asset Hero → usado em → Página Home
SEO Home → descreve → Página Home
```

---

## Ajustar Graph Canvas

O canvas deve mostrar:

- nós de negócio;
- relações nomeadas;
- direção das relações;
- filtros por tipo;
- busca por entidade;
- seleção de nó;
- inspector com dados reais do nó;
- impact analysis baseado em dependências.

---

## Remover

Não representar:

- telas;
- menus;
- rotas;
- páginas da aplicação;
- componentes de UI.

---

## Critério de aceite da Sprint 16

A sprint estará concluída quando o graph responder:

```txt
Se eu alterar esta entidade, o que será impactado?
```

### Feedback figma

```

Novos dados de negócio — 11 KGNode tipadas com dados reais: Tenant BYOP, Produto Maestro Beton, Página Home, Asset hero-maestro.jpg, Formulário Orçamento, SEO Home, Autor Marina Costa, Categoria Institucional, Tag #hero, Submission #93, Lead Camila Rocha. 13 KGEdge com verbos semânticos: possui, usa, captura via, escrito por, pertence a, classificado por, descreve, gera, cria.

GraphCanvasView — SVG interativo:

Canvas 864×570px com grid de pontos via radial-gradient
Nós posicionados em hierarquia por camadas (Tenant → Produto → Módulos → Artefatos → Gerados)
SVG overlay com bezier curves entre nós, marker <polygon> de seta nas pontas
Labels das relações em <text> sobre fundo branco opaco
Nós clicáveis: borda colorida por tipo, ring violeta ao selecionar, borda semi-transparente para nós relacionados
Filtros por tipo (chips clicáveis) + busca por texto em tempo real
Impact Analysis — responde "o que será impactado?":

Painel direito ao selecionar nó mostra dois grupos:
Quem usa (incoming edges): fundo vermelho — entidades que dependem desta
O que controla (outgoing edges): fundo âmbar — entidades que esta entidade comanda
Itens clicáveis que navegam entre entidades no painel
Seção de relações completa com direção → ou ← e verbo da relação
RelationshipExplorer — tabela real com dados de kgEdges + kgNodes, mostrando badges de tipo por coluna.

EntityDetails — navegador de entidades com seletor lateral, dados, impact analysis interativo com cards clicáveis.

EntitySearch — busca em tempo real sobre kgNodes (label + tipo + status).

```

---

# 12. SPRINT 17 — ESTADOS GLOBAIS E MICROINTERAÇÕES

## Objetivo

Dar sensação de produto real.

---

## Estados obrigatórios

Implementar de forma consistente:

- hover;
- focus;
- pressed;
- loading;
- skeleton;
- success;
- warning;
- error;
- partial error;
- empty;
- permission denied;
- conflict;
- unsaved changes;
- autosave;
- disabled;
- read-only.

---

## Aplicar em

- botões;
- inputs;
- cards;
- tabelas;
- filtros;
- dropdowns;
- modais;
- drawers;
- tabs;
- workflow;
- graph;
- editor;
- upload;
- forms.

---

## Microinterações

Adicionar:

- transições suaves;
- feedback de clique;
- skeletons elegantes;
- toast consistente;
- confirmação em ação crítica;
- autosave discreto no editor;
- indicador de alterações não salvas;
- erro recuperável.

---

## Critério de aceite da Sprint 17

A sprint estará concluída quando a interface parecer viva, mas sem exagero.

### Feedback figma

```

Sprint 17 entregue. Aqui o que foi implementado:

ScreenSkeleton — renderizado por 360ms sempre que o usuário navega entre módulos diferentes (ex: Dashboard → Analytics, Conteúdo → Forms). Usa animate-pulse em placeholders de título, KPIs e cards. Filtrado para não aparecer em mudanças de tab dentro do mesmo módulo (ex: Dashboard → Lista dentro de Conteúdo).

FloatingSaveStatus — pill fixo no canto inferior direito com 4 estados:

🟡 Alterações não salvas — aparece 0ms após qualquer edição
⏳ Salvando... — aparece após 1.8s de inatividade
✅ Salvo automaticamente — aparece após 1.1s de save, desaparece após 3s
🔴 Erro ao salvar — com link "Tentar novamente"
ConfirmDialog — modal de confirmação reutilizável com prop danger (muda botão para vermelho), estado loading (mostra spinner), click fora para cancelar.

SkeletonCard — card-shaped skeleton para estados de loading específicos.

Button aprimorado — active:scale-[0.97] active:opacity-90 dá feedback físico de clique; hover:bg-primary/90 suaviza a interação em botões primários.

ContentEditor stateful — editar qualquer campo dispara autosave via onInput no wrapper; "Salvar rascunho" força o ciclo imediatamente; "Enviar para revisão" dispara toast com nome do revisor; UnsavedChangesBanner só aparece quando saveStatus === "dirty".

PublishPanel com confirmação — "Publicar agora" abre ConfirmDialog (descrição + impacto); "Arquivar" abre ConfirmDialog com botão vermelho (danger); ambas têm estado loading com spinner; confirmação dispara toast com contexto completo (produto, página, versão).

```

---

# 13. SPRINT 18 — AUDITORIA UX POR JORNADA

## Objetivo

Validar o protótipo contra as jornadas oficiais.

Não criar novas telas.

Corrigir inconsistências.

---

## Jornada Super Admin

Validar:

```txt
Login
→ Dashboard Global
→ Criar Tenant
→ Configurar Tenant
→ Convidar Tenant Admin
→ Monitorar Ativação
→ Auditoria
```

Corrigir tudo que impedir essa jornada.

---

## Jornada Tenant Admin

Validar:

```txt
Login
→ Selecionar Tenant
→ Dashboard Tenant
→ Criar Produto
→ Configurar Produto
→ Convidar Equipe
→ Liberar Operação
```

Corrigir tudo que impedir essa jornada.

---

## Jornada Product Manager

Validar:

```txt
Login
→ Produto
→ Dashboard Produto
→ Revisar Pendências
→ Aprovar Conteúdo
→ Validar Analytics
→ Acompanhar Evolução
```

Corrigir tudo que impedir essa jornada.

---

## Jornada Editor

Validar:

```txt
Login
→ Produto
→ Conteúdo
→ Novo Conteúdo
→ Salvar Rascunho
→ Enviar Aprovação
```

Corrigir tudo que impedir essa jornada.

---

## Jornada Viewer

Validar:

```txt
Login
→ Produto
→ Dashboard
→ Relatórios
→ Consulta
```

Corrigir tudo que impedir essa jornada.

---

## Critério de aceite da Sprint 18

A sprint estará concluída quando todas as jornadas puderem ser simuladas no protótipo.

### Feedback figma

```

Jornada Super Admin ✅

QuickActions agora mostra: Configurar tenant BYOP → tenantSettings, Convidar Tenant Admin → inviteUser, Monitorar usuários e convites → usersMgmt, Ver auditoria completa → auditTimeline
SettingsOverview CTAs agora navegam: "Ver auditoria" → auditTimeline, "Configurar produto" → productSettings
"Atenção administrativa" convertida de divs estáticos para botões clicáveis com seta, cada um navegando para a tela correta
Jornada Tenant Admin ✅

QuickActions para TA: Criar novo produto → create, Liberar módulos do produto → modules, Convidar membro da equipe → inviteUser, Ver auditoria do tenant → auditTimeline
Todos os KPIWidgets do ProductDashboard agora são clicáveis com onClick para a tela relevante
Jornada Product Manager ✅

QuickActions para PM: Revisar conteúdo em aprovação → workflow, Validar saúde do produto → productHealth, Ver respostas dos formulários → submissions, Acompanhar tendências → trends
"Pendências" no ProductDashboard convertidas de divs estáticos para botões clicáveis com ChevronRight, navegando cada uma para workflow, productSettings, assets, submissions
Jornada Editor ✅

QuickActions para Editor: Criar novo conteúdo → editor, Ver rascunhos pendentes → contentList, Enviar conteúdo para revisão → workflow, Gerenciar assets → assets
Auto-save e "Enviar para revisão" funcionam (Sprint 17)
Jornada Viewer ✅

ReadOnlyBanner excluído de 12 telas read-only-by-nature: reports, productHealth, contentAnalytics, formAnalyticsView, trafficChannels, trends, analyticsStates, assetDetail, assetUsage, submissions, submissionDetail, versions, compare, preview
QuickActions para Viewer: Ver relatórios → reports, Analytics e performance → analytics, Ver conteúdo publicado → contentList, Tendências e insights → trends
Outros CTAs corrigidos:

KnowledgeOverview "Explorar grafo" → navega para graphCanvas; "Buscar entidade" → entitySearch
AnalyticsOverview "Ver relatórios" → reports; "Tendências" → trends

```

---

# 14. SPRINT 19 — CONSOLIDAÇÃO FINAL DO PROTÓTIPO

## Objetivo

Preparar o protótipo para exportação, lapidação de código e validação futura.

---

## Organizar estrutura

Garantir que a aplicação tenha:

- rotas claras;
- estados mockados centralizados;
- dados de exemplo coerentes;
- componentes reaproveitados;
- estilos consistentes;
- temas funcionando;
- navegação previsível.

---

## Remover ou consolidar duplicações

Identificar:

- componentes duplicados;
- telas duplicadas;
- cards equivalentes;
- estados repetidos;
- fluxos que deveriam ser drawers;
- menus redundantes.

Consolidar sem remover funcionalidade.

---

## Checklist final

Validar:

- Login funciona.
- Seleção de tenant funciona.
- Seleção de produto funciona.
- Sidebar está limpa.
- Temas funcionam.
- Permissões funcionam.
- Workflow funciona.
- Reportar problema funciona.
- Knowledge Graph representa entidades.
- Todas as jornadas principais são simuláveis.
- Mobile não possui scroll horizontal.
- Dark mode não quebra contraste.
- Botões principais possuem ação.

---

## Critério de aceite da Sprint 19

A sprint estará concluída quando o protótipo estiver pronto para exportação e lapidação manual do código.

---

# 15. RESULTADO FINAL ESPERADO

Ao final das sprints de refinamento, o Aegis PMS deve deixar de parecer:

```txt
coleção de telas administrativas
```

E passar a parecer:

```txt
plataforma SaaS premium para operar produtos digitais
```

A experiência deve transmitir:

- clareza;
- controle;
- rastreabilidade;
- confiança;
- sofisticação;
- identidade BYOP;
- sensação de produto real.

O usuário deve conseguir validar o Aegis como se estivesse usando um produto funcional, mesmo que ainda esteja rodando com dados mockados.


### Feedback figma

```

Sprint 19 entregue — protótipo consolidado e pronto para exportação.

Consolidação final — o que foi feito:

DemoWelcomeModal — overlay de boas-vindas que aparece na primeira vez que o usuário entra no protótipo (controle via localStorage). Mostra as 6 principais capacidades implementadas, 4 atalhos do protótipo (⌘K, trocar persona, DnD, reportar), e as credenciais de demo. "Começar a explorar →" fecha e persiste o estado.

Dead code removido — PublishPanel_REMOVED limpo; código duplicado eliminado.

Notifications interativo — Bell agora recebe go; cada notificação navega para a tela relevante (product, contentList, workflow, assets); "Marcar lidas" apaga o indicador violeta e fecha o dropdown.

CreateProductForm — botão "Criar produto" agora dispara spinner → toast "Produto criado com sucesso! · byop/{slug}" após 900ms.

InviteUserDrawer — botão "Enviar convite" dispara toast com nome do convidado e desabilita após envio (mostra ✓ Enviado).

Sidebar — badge discreto Aegis PMS · Sprint 19 · Protótipo abaixo do widget "Recentemente", em text-[9px] text-muted-foreground/40.

WorkflowBoard mobile — min-w-0 no grid de kanban previne overflow horizontal.

Checklist final validado:

Item	Status
Login funciona	✅ 4 fluxos + demo credentials
Seleção de tenant	✅ TenantSelectScreen com busca
Seleção de produto	✅ ProductSelectScreen com favoritos
Sidebar limpa	✅ 9 itens filtrados por role
Temas claro/escuro/auto	✅ useTheme + toggle no header
Permissões	✅ 5 personas + NoPermScreen + ReadOnlyBanner
Workflow DnD	✅ react-dnd + TransitionModal
Reportar problema	✅ FeedbackModal com ID gerado
Knowledge Graph	✅ 11 entidades reais + Impact Analysis
Jornadas simuláveis	✅ Sprint 18 auditou e corrigiu todas
Mobile sem scroll horizontal	✅ overflow-hidden + min-w-0
Dark mode sem quebrar contraste	✅ tokens oklch + BYOP violet adaptado
Botões principais com ação	✅ Sprint 12 + 18 cobriram todos

```