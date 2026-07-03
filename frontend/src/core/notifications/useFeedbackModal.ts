import { useContext } from "react";
import { FeedbackModalContext } from "./feedbackModalContextDefinition";

export function useFeedbackModal() {
  const ctx = useContext(FeedbackModalContext);
  if (!ctx) throw new Error("useFeedbackModal deve ser usado dentro de FeedbackModalProvider");
  return ctx;
}
