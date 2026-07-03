import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useLocation, useNavigate } from "react-router";
import { ACTIONS, EVENTS, Joyride, STATUS, type EventData } from "react-joyride";
import { getVisibleTutorialSteps } from "./tutorialSteps";
import { tutorialService } from "./tutorialService";
import { TutorialContext } from "./tutorialContextDefinition";
import { useViewAsRole } from "../permissions/useViewAsRole";
import { useCurrentProduct } from "../products/useCurrentProduct";
import { resolveEnabledModules } from "../products/moduleDefaults";

/**
 * Tour interativo de onboarding (Sprint 22). A biblioteca instalada é a
 * v3 do react-joyride, que substituiu a prop `callback`/`CallBackProps`
 * (v2, usada na especificação original da sprint) por `onEvent`/`EventData`
 * e moveu `styles.options.*` para uma prop `options` própria — a lógica de
 * navegação controlada (STATUS/ações) é equivalente, só a superfície mudou.
 *
 * Os steps mostrados dependem do papel efetivo (`useViewAsRole`) e dos
 * módulos habilitados no produto selecionado (`resolveEnabledModules`) —
 * um Product Manager nunca vê o passo de "criar produto", e o editor de
 * blocos só aparece se o produto atual tiver o módulo "Páginas" (ver
 * `tutorialSteps.ts`, `getVisibleTutorialSteps`).
 */
export function TutorialProvider({ children }: { children: ReactNode }) {
  const { viewAsRole } = useViewAsRole();
  const { product } = useCurrentProduct();
  const navigate = useNavigate();
  const location = useLocation();
  const [run, setRun] = useState(false);
  const [stepIndex, setStepIndex] = useState(0);

  const steps = useMemo(
    () => getVisibleTutorialSteps(viewAsRole, resolveEnabledModules(product)),
    [viewAsRole, product],
  );

  // Os alvos do tour vivem em rotas diferentes (dashboard/produtos/conteúdo/
  // usuários/settings) e o react-joyride não navega sozinho — antes de cada
  // step, se ele declarar `route` e não formos essa rota, navega até ela.
  useEffect(() => {
    if (!run) return;
    const targetRoute = steps[stepIndex]?.data?.route;
    if (targetRoute && location.pathname !== targetRoute) {
      navigate(targetRoute);
    }
  }, [run, stepIndex, steps, location.pathname, navigate]);

  const startTutorial = useCallback(() => {
    setStepIndex(0);
    setRun(true);
  }, []);

  const stopTutorial = useCallback(() => {
    setRun(false);
    setStepIndex(0);
  }, []);

  const finishTutorial = useCallback(() => {
    setRun(false);
    setStepIndex(0);
    // Marcar tutorial como concluído no backend — nunca mais mostrar
    tutorialService.markComplete().catch(() => {
      // Silenciar erro: o localStorage já garantiu que não mostra novamente nesta sessão
    });
  }, []);

  const handleEvent = useCallback((data: EventData) => {
    const { status, type, index, action } = data;
    const finished = ([STATUS.FINISHED, STATUS.SKIPPED] as string[]).includes(status);

    if (finished) {
      finishTutorial();
    } else if (type === EVENTS.STEP_AFTER) {
      setStepIndex(action === ACTIONS.PREV ? Math.max(0, index - 1) : index + 1);
    } else if (type === EVENTS.TARGET_NOT_FOUND) {
      // Alvo não existe na rota atual (ex.: editor de blocos sem página
      // aberta) — pula para o próximo step; se já era o último, encerra.
      if (index >= steps.length - 1) finishTutorial();
      else setStepIndex(index + 1);
    }
  }, [steps.length, finishTutorial]);

  return (
    <TutorialContext.Provider value={{ startTutorial, stopTutorial, isRunning: run }}>
      {children}
      <Joyride
        steps={steps}
        run={run}
        stepIndex={stepIndex}
        continuous
        scrollToFirstStep
        locale={{
          back: "Anterior",
          close: "Fechar",
          last: "Concluir",
          next: "Próximo",
          // `showProgress: true` faz o v3 do react-joyride usar este texto no
          // lugar de `next` — sem sobrescrever, o botão cai no default em
          // inglês ("Next ({current} of {total})").
          nextWithProgress: "Próximo ({current} de {total})",
          skip: "Pular tutorial",
        }}
        options={{
          buttons: ["back", "skip", "primary"],
          showProgress: true,
          skipBeacon: true,
          primaryColor: "var(--color-primary)",
          backgroundColor: "var(--color-card)",
          textColor: "var(--color-foreground)",
          arrowColor: "var(--color-card)",
          overlayColor: "rgba(0, 0, 0, 0.55)",
          zIndex: 9999,
        }}
        styles={{
          tooltip: {
            borderRadius: "12px",
            padding: "20px 24px",
            boxShadow: "0 8px 32px rgba(0,0,0,0.18)",
          },
          tooltipTitle: {
            fontSize: "15px",
            fontWeight: "600",
            marginBottom: "8px",
          },
          tooltipContent: {
            fontSize: "14px",
            lineHeight: "1.6",
            color: "var(--color-muted-foreground)",
          },
          buttonPrimary: {
            borderRadius: "8px",
            padding: "8px 16px",
            fontSize: "13px",
            fontWeight: "600",
          },
          buttonBack: {
            borderRadius: "8px",
            padding: "8px 12px",
            fontSize: "13px",
          },
          buttonSkip: {
            fontSize: "12px",
            color: "var(--color-muted-foreground)",
          },
        }}
        onEvent={handleEvent}
      />
    </TutorialContext.Provider>
  );
}
