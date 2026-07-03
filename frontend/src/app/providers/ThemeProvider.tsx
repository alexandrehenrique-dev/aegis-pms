import { useEffect, useState, type ReactNode } from "react";
import { ThemeContext, type Theme } from "./themeContext";

function applyTheme(t: Theme) {
  const dark = t === "dark" || (t === "auto" && window.matchMedia("(prefers-color-scheme: dark)").matches);
  document.documentElement.classList.toggle("dark", dark);
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    try { return (localStorage.getItem("aegis-theme") as Theme) || "auto"; } catch { return "auto"; }
  });

  useEffect(() => {
    applyTheme(theme);
    try { localStorage.setItem("aegis-theme", theme); } catch { /* noop */ }
  }, [theme]);

  useEffect(() => {
    if (theme !== "auto") return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const h = (e: MediaQueryListEvent) => document.documentElement.classList.toggle("dark", e.matches);
    mq.addEventListener("change", h);
    return () => mq.removeEventListener("change", h);
  }, [theme]);

  const setTheme = (t: Theme) => { setThemeState(t); applyTheme(t); };

  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>;
}
