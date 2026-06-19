// Cross-cutting "operational timeline" mock used by Dashboard, Notifications and
// the global header bell. Originally a top-level constant in App.tsx.
export const timeline = [
  "Maestro Beton recebeu 3 novas respostas de formulário.",
  "WikiDev publicou uma nova página.",
  "Aion Logbook teve conteúdo enviado para revisão.",
  "Eirene UI atualizou assets do produto.",
];

// Routes each timeline entry should navigate to when clicked (replaces the old
// `timelineScreens`/`notifScreens` Screen-based arrays with real route paths).
export const timelineRoutes = ["/products/maestro-beton", "/content", "/content", "/assets"];
