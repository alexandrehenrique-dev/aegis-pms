import { createContext, useContext, useState, type ReactNode } from "react";

type FeedbackModalContextValue = { open: boolean; setOpen: (open: boolean) => void };

const FeedbackModalContext = createContext<FeedbackModalContextValue | null>(null);

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

export function useFeedbackModal() {
  const ctx = useContext(FeedbackModalContext);
  if (!ctx) throw new Error("useFeedbackModal deve ser usado dentro de FeedbackModalProvider");
  return ctx;
}
