import { createContext } from "react";

export type FeedbackModalContextValue = { open: boolean; setOpen: (open: boolean) => void };

export const FeedbackModalContext = createContext<FeedbackModalContextValue | null>(null);
