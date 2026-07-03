import { useState, type ReactNode } from "react";
import { FeedbackModalContext } from "./feedbackModalContextDefinition";

/**
 * Estado do `FeedbackModal` movido para contexto (Sprint 13, Tarefa O.2) —
 * antes vivia como `useState` local em `AppShell`, então só o botão "?" do
 * próprio `AppShell` conseguia abri-lo. A Central de Ajuda (`/help`) é uma
 * rota filha renderizada via `Outlet`, fora da árvore de `AppShell`, e
 * precisa do mesmo gatilho para a opção "Reportar um problema" no rodapé.
 */
export function FeedbackModalProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  return <FeedbackModalContext.Provider value={{ open, setOpen }}>{children}</FeedbackModalContext.Provider>;
}
