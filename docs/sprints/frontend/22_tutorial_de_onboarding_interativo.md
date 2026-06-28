# Sprint 22 — Tutorial interativo de onboarding

> **Sprint final do ciclo de produto.** Cobre o guia interativo completo que um novo usuário vê na primeira vez que acessa o Aegis — e **nunca mais**.
>
> **Pré-requisitos:** Sprint 14 (sistema de notificações e onboarding real), etapa 24 do backend (notificações direcionadas), etapa 28 do backend (ativação real de conta).
>
> **Branch:** `sprint/22-tutorial-onboarding`

## A. Biblioteca — react-joyride

**Escolha:** [`react-joyride`](https://react-joyride.com/) — padrão de mercado para tours interativos em React (Figma, Notion, Linear usam abordagem equivalente). Principais critérios:

- Funciona com qualquer estrutura de routing (SPA multi-rota)
- Estilização 100% via `className` — compatível com Tailwind/tokens do design system Aegis
- Suporte a `data-*` selectors e elementos fixos (sidebar, header)
- Controle programático total (start, stop, next, skip)
- Sem dependências de UI externas

```bash
npm install react-joyride
```

Não instalar `intro.js`, `shepherd.js` ou `driver.js` — cada um tem trade-offs, mas `react-joyride` é o único que não conflita com o portal de modais já em uso no Aegis.

## B. `TutorialProvider` — contexto global

Criar `src/core/tutorial/TutorialProvider.tsx`:

```tsx
import Joyride, { type CallBackProps, STATUS, type Step } from 'react-joyride';
import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { TUTORIAL_STEPS } from './tutorialSteps';
import { tutorialService } from './tutorialService';

interface TutorialContextValue {
  startTutorial: () => void;
  stopTutorial: () => void;
  isRunning: boolean;
}

const TutorialContext = createContext<TutorialContextValue>({
  startTutorial: () => {},
  stopTutorial: () => {},
  isRunning: false,
});

export function TutorialProvider({ children }: { children: ReactNode }) {
  const [run, setRun] = useState(false);
  const [stepIndex, setStepIndex] = useState(0);

  const startTutorial = useCallback(() => {
    setStepIndex(0);
    setRun(true);
  }, []);

  const stopTutorial = useCallback(() => {
    setRun(false);
    setStepIndex(0);
  }, []);

  const handleCallback = useCallback(async (data: CallBackProps) => {
    const { status, type, index, action } = data;
    const finished = ([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status);

    if (finished) {
      setRun(false);
      setStepIndex(0);
      // Marcar tutorial como concluído no backend — nunca mais mostrar
      await tutorialService.markComplete().catch(() => {
        // Silenciar erro: o localStorage já garantiu que não mostra novamente nesta sessão
      });
    } else if (type === 'step:after') {
      if (action === 'prev') {
        setStepIndex(Math.max(0, index - 1));
      } else {
        setStepIndex(index + 1);
      }
    }
  }, []);

  return (
    <TutorialContext.Provider value={{ startTutorial, stopTutorial, isRunning: run }}>
      {children}
      <Joyride
        steps={TUTORIAL_STEPS}
        run={run}
        stepIndex={stepIndex}
        continuous
        showSkipButton
        showProgress
        disableScrolling={false}
        scrollToFirstStep
        locale={{
          back: 'Anterior',
          close: 'Fechar',
          last: 'Concluir',
          next: 'Próximo',
          skip: 'Pular tutorial',
        }}
        styles={{
          options: {
            primaryColor: 'var(--color-primary)',
            backgroundColor: 'var(--color-card)',
            textColor: 'var(--color-foreground)',
            arrowColor: 'var(--color-card)',
            overlayColor: 'rgba(0, 0, 0, 0.55)',
            zIndex: 9999,
          },
          tooltip: {
            borderRadius: '12px',
            padding: '20px 24px',
            boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
            maxWidth: '380px',
          },
          tooltipTitle: {
            fontSize: '15px',
            fontWeight: '600',
            marginBottom: '8px',
          },
          tooltipContent: {
            fontSize: '14px',
            lineHeight: '1.6',
            color: 'var(--color-muted-foreground)',
          },
          buttonNext: {
            borderRadius: '8px',
            padding: '8px 16px',
            fontSize: '13px',
            fontWeight: '600',
          },
          buttonBack: {
            borderRadius: '8px',
            padding: '8px 12px',
            fontSize: '13px',
          },
          buttonSkip: {
            fontSize: '12px',
            color: 'var(--color-muted-foreground)',
          },
        }}
        callback={handleCallback}
      />
    </TutorialContext.Provider>
  );
}

export const useTutorial = () => useContext(TutorialContext);
```

Registrar no `App.tsx` ou no provider raiz, dentro do `AuthProvider` (para ter acesso ao contexto de auth quando necessário):

```tsx
// src/app/App.tsx
<AuthProvider>
  <TutorialProvider>
    <RouterProvider ... />
  </TutorialProvider>
</AuthProvider>
```

## C. `tutorialService.ts` — gate de disparo e flag de conclusão

Criar `src/core/tutorial/tutorialService.ts`:

```ts
import api from '@/infra/api';

const LS_KEY = 'aegis:tutorial:completed'; // cache local — evita chamada desnecessária ao backend

export const tutorialService = {
  /** Retorna true se o tutorial já foi concluído (check local-first, depois backend). */
  isCompleted(): boolean {
    return localStorage.getItem(LS_KEY) === 'true';
  },

  /** Chamado ao final ou skip do tutorial — persiste no backend e no localStorage. */
  async markComplete(): Promise<void> {
    localStorage.setItem(LS_KEY, 'true');
    await api.post('/auth/tutorial/complete');
  },
};
```

**Garantia "nunca mostrar novamente":**
1. `localStorage` — impede o flash de mostrar na mesma sessão/dispositivo
2. Backend flag — impede reabertura se o usuário limpar o storage ou acessar de outro dispositivo
3. O `NotificationModal` consulta `tutorialService.isCompleted()` antes de exibir o checkbox — se já completou, nem mostra a opção

## D. Integração com o modal de onboarding existente

O `NotificationModal.tsx` exibe as notificações de onboarding do tipo `"ONBOARDING_WELCOME"` (geradas pela etapa 24 do backend). Adicionar o checkbox de tutorial nesta modal:

```tsx
// src/core/notifications/components/NotificationModal.tsx — adições

import { useTutorial } from '../../tutorial/TutorialProvider';
import { tutorialService } from '../../tutorial/tutorialService';

// Dentro do componente:
const { startTutorial } = useTutorial();
const [startTour, setStartTour] = useState(!tutorialService.isCompleted()); // pré-marcado

// No botão de fechar/começar (última ação da modal):
const handleDismiss = async () => {
  await notificationsService.markRead(notification.id);
  onClose();
  if (startTour && !tutorialService.isCompleted()) {
    // Pequeno delay para a modal fechar antes do tour iniciar
    setTimeout(() => startTutorial(), 300);
  }
};

// No JSX da modal de onboarding, antes do botão de ação:
{notification.type === 'ONBOARDING_WELCOME' && !tutorialService.isCompleted() && (
  <label className="flex cursor-pointer items-center gap-2.5 rounded-lg border border-border bg-muted/40 px-3 py-2.5 text-sm select-none">
    <input
      type="checkbox"
      checked={startTour}
      onChange={(e) => setStartTour(e.target.checked)}
      className="h-4 w-4 rounded accent-primary"
    />
    <span className="text-foreground">
      Fazer um tour pela plataforma <span className="text-muted-foreground">(recomendado)</span>
    </span>
  </label>
)}
```

**Regra:** o checkbox só aparece se `notification.type === 'ONBOARDING_WELCOME'` **E** `!tutorialService.isCompleted()`. Nenhum outro tipo de notificação exibe o checkbox. Isso garante que, mesmo que a modal apareça em outros contextos, o tutorial não é oferecido novamente.

## E. Backend — endpoint mínimo de conclusão

> Inclusão mínima em etapa existente — não cria nova etapa de backend.

### Migration

```sql
-- Adicionar em V29__tutorial_flag.sql (ou V28a se preferir agrupar)
ALTER TABLE tenant_memberships ADD COLUMN tutorial_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant_memberships ADD COLUMN tutorial_completed_at TIMESTAMPTZ;
```

### Endpoint

```
POST /api/v1/auth/tutorial/complete
Authorization: Bearer <token>   ← requer autenticação (usuário logado)
Response: 204 No Content
```

Lógica: `TenantMembership.tutorialCompleted = true`, `tutorialCompletedAt = now()`. Se o usuário tem memberships em vários tenants, marcar o do `effectiveTenantId` (presente no JWT como claim `tenant_id`).

**Quando checar se já completou:** o backend **não** precisa de um GET para isso — o `tutorialService.isCompleted()` é suficiente no frontend. O endpoint de conclusão é idempotente (chamar duas vezes não é problema).

## F. `data-tour` attributes — lista completa

Adicionar os atributos abaixo nos componentes existentes. **Regra:** nunca criar elementos só para o tutorial — o atributo vai no elemento funcional já existente.

| Atributo `data-tour` | Componente | Elemento |
|---|---|---|
| `"dashboard-global"` | `DashboardGlobal.tsx` | `<PageHeader>` (wrapper ou primeiro div filho) |
| `"dashboard-kpis"` | `DashboardGlobal.tsx` | `<div className="grid ... grid-cols-4">` (grid de KPIWidgets) |
| `"dashboard-criar-produto"` | `DashboardGlobal.tsx` | `<Button primary onClick={() => setShowCreateProduct(true)}>` |
| `"dashboard-timeline"` | `DashboardGlobal.tsx` | `<OperationalTimeline>` |
| `"products-list"` | `ProductsList.tsx` | `<div className="grid gap-4 ...">` (grid de ProductCards) |
| `"product-card"` | `ProductCard.tsx` | elemento raiz do card |
| `"products-novo"` | `ProductsList.tsx` | `<Button primary onClick={() => navigate("/products/new")}>` |
| `"content-list"` | `ContentDataGrid.tsx` | elemento raiz da tabela/grid |
| `"content-status-badge"` | `ContentStatusBadge.tsx` | elemento raiz do badge |
| `"content-novo"` | `EditorialDashboard.tsx` | botão "Novo conteúdo" |
| `"content-editor-blocks"` | `ContentEditor.tsx` | painel de blocos ou toolbar |
| `"users-table"` | `UserTable.tsx` | elemento raiz da tabela |
| `"users-convidar"` | `UserTable.tsx` | botão "Convidar" |
| `"settings-geral"` | `SettingsOverview.tsx` | primeiro card de configurações |
| `"settings-perigo"` | `TenantSettings.tsx` | seção "Zona de perigo" |
| `"sidebar-nav"` | sidebar/nav component existente | elemento `<nav>` |

## G. Steps do tutorial — textos gerados por IA

> **Nota de implementação:** os textos desta seção foram elaborados pela IA com base na análise dos componentes do Aegis. O desenvolvedor deve copiar o array `TUTORIAL_STEPS` abaixo diretamente para `src/core/tutorial/tutorialSteps.ts` sem alteração de tom — os textos foram calibrados para serem acolhedores e diretos, evitando jargão técnico.

Criar `src/core/tutorial/tutorialSteps.ts`:

```ts
import type { Step } from 'react-joyride';

export const TUTORIAL_STEPS: Step[] = [
  // ─── 1. Boas-vindas ──────────────────────────────────────────────────────
  {
    target: 'body',
    placement: 'center',
    disableBeacon: true,
    title: '👋 Seja bem-vindo ao Aegis',
    content:
      'O Aegis é o seu sistema central de gestão de produtos digitais. Em menos de 2 minutos, vamos mostrar os principais recursos para você começar com confiança. Você pode pular a qualquer momento — o tour fica disponível até o final.',
  },

  // ─── 2. Dashboard Global ─────────────────────────────────────────────────
  {
    target: '[data-tour="dashboard-global"]',
    placement: 'bottom',
    disableBeacon: true,
    title: '🏠 Seu painel de comando',
    content:
      'O Dashboard Global é a primeira tela que você vê ao entrar. Aqui você acompanha a saúde operacional de todos os seus produtos digitais em tempo real — sem precisar abrir cada produto separadamente.',
  },
  {
    target: '[data-tour="dashboard-kpis"]',
    placement: 'bottom',
    disableBeacon: true,
    title: '📊 Indicadores em tempo real',
    content:
      'Estes cartões resumem o que está acontecendo agora: conteúdos pendentes de revisão, aprovações em aberto, formulários recebidos hoje e muito mais. Clique em qualquer cartão para ir direto à área correspondente.',
  },
  {
    target: '[data-tour="dashboard-criar-produto"]',
    placement: 'bottom-end',
    disableBeacon: true,
    title: '✨ Criar um produto digital',
    content:
      'Produtos são os espaços de trabalho do Aegis — cada site, portal, base de conhecimento ou aplicação que você gerencia vive aqui. Clique em "Criar produto" para começar um novo.',
  },
  {
    target: '[data-tour="dashboard-timeline"]',
    placement: 'left',
    disableBeacon: true,
    title: '🕒 Linha do tempo operacional',
    content:
      'A linha do tempo registra as atividades recentes de toda a equipe: publicações, edições, aprovações e convites. É a sua fonte de verdade para entender o que aconteceu e quando.',
  },

  // ─── 3. Produtos ─────────────────────────────────────────────────────────
  {
    target: '[data-tour="product-card"]',
    placement: 'right',
    disableBeacon: true,
    title: '📦 Produtos digitais',
    content:
      'Cada card representa um produto do seu workspace. O card mostra o status atual, os módulos habilitados e o último acesso da equipe. Clique no card para entrar no produto e gerenciar seu conteúdo.',
  },
  {
    target: '[data-tour="products-novo"]',
    placement: 'bottom-end',
    disableBeacon: true,
    title: '🆕 Novos produtos em segundos',
    content:
      'Ao criar um produto, você escolhe um modelo (blog, portal, e-commerce, base de conhecimento e outros). O Aegis já prepara a estrutura de páginas e ativa os módulos recomendados para aquele tipo — você começa com o essencial pronto.',
  },

  // ─── 4. Conteúdo ─────────────────────────────────────────────────────────
  {
    target: '[data-tour="content-list"]',
    placement: 'top',
    disableBeacon: true,
    title: '✍️ Gerenciando conteúdo',
    content:
      'Aqui ficam todos os artigos, posts e materiais do produto selecionado. Você pode filtrar por status, autor ou data, e ver de um relance o que está em rascunho, em revisão ou já publicado.',
  },
  {
    target: '[data-tour="content-status-badge"]',
    placement: 'right',
    disableBeacon: true,
    title: '🔄 Fluxo editorial',
    content:
      'O Aegis tem um workflow editorial embutido: Rascunho → Em Revisão → Aprovado → Publicado. Cada conteúdo avança pelo fluxo com a aprovação das pessoas certas — nada vai ao ar sem passar pelo processo definido pelo seu time.',
  },
  {
    target: '[data-tour="content-editor-blocks"]',
    placement: 'left',
    disableBeacon: true,
    title: '🧱 Editor em blocos',
    content:
      'O editor funciona com blocos — cada parágrafo, imagem, vídeo ou formulário é um bloco independente que você pode arrastar, duplicar ou remover. É intuitivo e não exige conhecimento técnico.',
  },

  // ─── 5. Usuários ─────────────────────────────────────────────────────────
  {
    target: '[data-tour="users-table"]',
    placement: 'top',
    disableBeacon: true,
    title: '👥 Sua equipe',
    content:
      'Aqui você vê todos os membros do workspace e os produtos que cada um pode acessar. O Aegis tem papéis bem definidos: Super Admin, Tenant Admin, Product Manager, Editor e Viewer — cada um com acesso calibrado ao que precisa.',
  },
  {
    target: '[data-tour="users-convidar"]',
    placement: 'bottom-end',
    disableBeacon: true,
    title: '📨 Convidar alguém é simples',
    content:
      'Informe o e-mail, escolha o papel e selecione os produtos. A pessoa recebe um e-mail com um link direto para ativar a conta no Aegis — sem precisar configurar nada manualmente.',
  },

  // ─── 6. Configurações ────────────────────────────────────────────────────
  {
    target: '[data-tour="settings-geral"]',
    placement: 'right',
    disableBeacon: true,
    title: '⚙️ Configurações do workspace',
    content:
      'Em Configurações você ajusta o nome, identidade visual e políticas gerais do seu workspace. Alterações aqui afetam todos os produtos e membros deste tenant.',
  },
  {
    target: '[data-tour="settings-perigo"]',
    placement: 'top',
    disableBeacon: true,
    title: '⚠️ Zona de perigo',
    content:
      'Ações como excluir o workspace são irreversíveis. O Aegis pede confirmação explícita e faz um backup automático antes de qualquer exclusão — seus dados ficam disponíveis por 7 dias para download via e-mail.',
  },

  // ─── 7. Sidebar ──────────────────────────────────────────────────────────
  {
    target: '[data-tour="sidebar-nav"]',
    placement: 'right',
    disableBeacon: true,
    title: '🗺️ Navegação principal',
    content:
      'A barra lateral é o seu ponto de partida para qualquer área do produto selecionado. Os itens visíveis dependem dos módulos habilitados — se um módulo não aparece aqui, verifique as configurações do produto.',
  },

  // ─── 8. Conclusão ────────────────────────────────────────────────────────
  {
    target: 'body',
    placement: 'center',
    disableBeacon: true,
    title: '🎉 Você está pronto!',
    content:
      'Este foi um tour rápido pelo Aegis. Explore, crie e publique com confiança — se tiver dúvidas, a equipe de suporte está a um clique em "Reportar problema" no rodapé de qualquer tela. Bom trabalho!',
  },
];
```

**Regras de qualidade dos textos (não alterar sem revisão):**
- Títulos: curtos, com emoji contextual, sem ponto final
- Corpo: máximo 2 frases, voz ativa, segunda pessoa do singular ("você")
- Sem jargão técnico nas frases voltadas ao usuário final (o Editor/Viewer não sabe o que é "JWT" ou "endpoint")
- Tom: acolhedor, seguro, otimista — nunca alarmista (exceto na zona de perigo, onde o alerta é intencional)
- Nunca mencionar o Keycloak, banco de dados, API ou detalhes de implementação

## H. Garantia de "nunca mostrar novamente" — verificação obrigatória

Esta é a regra mais crítica da sprint. **Cenários que devem ser testados:**

| Cenário | Resultado esperado |
|---|---|
| Usuário conclui o tour (clica "Concluir") | Backend marcado, localStorage = 'true', tour nunca mais abre |
| Usuário clica "Pular tutorial" | Idem — pular = concluído |
| Usuário faz logout e login novamente | `tutorialService.isCompleted()` verifica localStorage → true → tutorial não inicia |
| Usuário limpa localStorage e faz login | Backend retorna `tutorialCompleted = true` via `GET /me` (ver Seção I) → localStorage recriado → tutorial não inicia |
| Usuário faz login em outro dispositivo | Verificado via `GET /me` antes de mostrar o checkbox → nunca exibe |
| Notificação de onboarding é re-enviada por admin | Modal aparece, mas sem o checkbox (pois `tutorialService.isCompleted() === true`) |
| Tutorial iniciado, usuário navega para outra rota no meio | Tour continua normalmente (react-joyride persiste no DOM via provider raiz) |

## I. Sincronização do flag no `GET /me`

Para funcionar no cenário de "outro dispositivo", o campo `tutorialCompleted` deve ser retornado pelo `GET /api/v1/me` (etapa 05/06 do backend). Adicionar ao response:

```ts
// Adicionar ao MeResponse existente:
type MeResponse = {
  // ... campos existentes ...
  tutorialCompleted: boolean;   // ← novo campo
};
```

No `AuthContext.tsx`, ao processar a resposta do `GET /me` após login:

```ts
if (meData.tutorialCompleted) {
  localStorage.setItem('aegis:tutorial:completed', 'true');
}
```

Isso reconstrói o cache local a partir do backend sem nenhum request adicional.

## J. Critérios de aceite

- [ ] Checkbox "Fazer um tour pela plataforma" aparece no modal de onboarding, pré-marcado, apenas na primeira vez.
- [ ] Desmarcar o checkbox e confirmar: tour não inicia.
- [ ] Marcar o checkbox e confirmar: tour inicia 300ms após fechar o modal.
- [ ] Tour exibe 16 steps com os textos exatos da Seção G (sem alteração de tom ou conteúdo).
- [ ] Botões "Anterior", "Próximo", "Pular tutorial" e "Concluir" funcionam.
- [ ] Clicar "Pular tutorial" em qualquer step: tour encerra, backend marcado, localStorage setado.
- [ ] Clicar "Concluir" no último step: idem.
- [ ] Após conclusão (por qualquer motivo): recarregar a página, fazer logout/login, limpar sessionStorage — **o tour nunca reaparece**.
- [ ] `GET /me` retorna `tutorialCompleted: true` após a conclusão — verificado via DevTools.
- [ ] Abrir a notificação de onboarding após completar o tour: checkbox não aparece mais.
- [ ] Tour funciona em viewport mobile (375px) sem overflow ou sobreposição de elementos.
- [ ] Todos os `data-tour` attributes estão presentes nos componentes (grep: `grep -r 'data-tour' src/` retorna ≥ 15 resultados).
- [ ] `npm run build` sem erros de tipo.
- [ ] Nenhum `console.error` durante a execução do tour.

## K. Testes

```ts
// TutorialProvider.test.tsx
it('inicia o tour ao chamar startTutorial()', ...)
it('marca como concluído ao terminar (STATUS.FINISHED)', ...)
it('marca como concluído ao pular (STATUS.SKIPPED)', ...)
it('chama tutorialService.markComplete() exatamente uma vez', ...)

// NotificationModal.test.tsx
it('exibe checkbox somente para notificações ONBOARDING_WELCOME não concluídas', ...)
it('não exibe checkbox se tutorialService.isCompleted() === true', ...)
it('chama startTutorial() quando checkbox está marcado e modal é fechado', ...)
it('não chama startTutorial() quando checkbox está desmarcado', ...)

// tutorialService.test.ts
it('isCompleted() retorna false quando localStorage está vazio', ...)
it('isCompleted() retorna true após markComplete()', ...)
it('markComplete() chama POST /auth/tutorial/complete', ...)
```

## L. Commit sugerido

```bash
git add src/core/tutorial/ src/core/notifications/components/NotificationModal.tsx
git add src/domains/*/components/*.tsx src/domains/*/pages/*.tsx  # data-tour attributes
git add src/core/auth/AuthContext.tsx  # tutorialCompleted sync no GET /me
git commit -m "feat(frontend): tutorial interativo de onboarding com react-joyride"
```
