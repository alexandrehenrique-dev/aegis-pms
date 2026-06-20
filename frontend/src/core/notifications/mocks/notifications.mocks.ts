import type { Notification, UserNotificationStatus } from "../contracts/notification";

// Notificação de onboarding real (substitui o DemoWelcomeModal/localStorage) +
// 2 notificações de exemplo (FEATURE lida, WARNING não lida) para validar o
// sino com conteúdo variado — ver Sprint 14, Tarefa A.3.
export const seedNotifications: Notification[] = [
  {
    id: "n1-onboarding",
    type: "ONBOARDING",
    title: "Bem-vindo ao Aegis PMS",
    presentationMode: "MODAL_ONCE",
    homologationBadge: true,
    createdBySubject: "system",
    createdAt: "2026-06-01T09:00:00.000Z",
    bodyMarkdown: [
      "O **Aegis PMS** é o Product Operating System onde você gerencia o conteúdo, as páginas, os assets e os formulários do seu produto digital.",
      "",
      "**Como navegar**",
      "- Use os seletores de **Tenant** e **Produto** no topo para trocar de contexto.",
      "- O menu lateral leva às áreas do produto atual: Páginas, Conteúdo, Assets, Forms, Analytics e mais.",
      "- Precisa de ajuda em qualquer tela? Use o botão **\"?\"** no topo para abrir a Central de Ajuda.",
      "",
      "**Onde pedir ajuda**",
      "Encontrou algo que não funciona como esperado? Use o botão de feedback (ícone de alerta no menu da sua conta) para reportar.",
      "",
      "_Este produto está em processo de homologação — encontrou algo estranho? Reporte pelo botão de feedback._",
    ].join("\n"),
  },
  {
    id: "n2-feature-help-center",
    type: "FEATURE",
    title: "Nova Central de Ajuda disponível",
    presentationMode: "BELL_ONLY",
    createdBySubject: "system",
    createdAt: "2026-06-10T14:30:00.000Z",
    bodyMarkdown: "A Central de Ajuda agora reúne, em um só lugar, guias organizados por módulo habilitado no seu produto. Acesse pelo botão **\"?\"** no topo da tela.",
  },
  {
    id: "n3-warning-maintenance",
    type: "WARNING",
    title: "Manutenção programada",
    presentationMode: "BELL_ONLY",
    createdBySubject: "system",
    createdAt: "2026-06-18T08:00:00.000Z",
    bodyMarkdown: "Uma manutenção programada ocorrerá neste fim de semana. Nenhuma ação é necessária da sua parte — o acesso pode ficar indisponível por curtos períodos.",
  },
];

// Estado por (usuário, notificação) — independente da notificação em si.
// u4 (conta de teste "bloqueado") fica de fora de propósito.
export const seedStatusByUser: Record<string, UserNotificationStatus[]> = {
  u1: [
    { notificationId: "n1-onboarding", autoShown: false, read: false },
    { notificationId: "n2-feature-help-center", autoShown: false, read: true, readAt: "2026-06-11T10:00:00.000Z" },
    { notificationId: "n3-warning-maintenance", autoShown: false, read: false },
  ],
  u2: [
    { notificationId: "n1-onboarding", autoShown: false, read: false },
    { notificationId: "n2-feature-help-center", autoShown: false, read: true, readAt: "2026-06-11T11:00:00.000Z" },
    { notificationId: "n3-warning-maintenance", autoShown: false, read: false },
  ],
  u3: [
    { notificationId: "n1-onboarding", autoShown: false, read: false },
    { notificationId: "n2-feature-help-center", autoShown: false, read: true, readAt: "2026-06-11T12:00:00.000Z" },
    { notificationId: "n3-warning-maintenance", autoShown: false, read: false },
  ],
  u5: [
    { notificationId: "n1-onboarding", autoShown: false, read: false },
    { notificationId: "n2-feature-help-center", autoShown: false, read: true, readAt: "2026-06-11T13:00:00.000Z" },
    { notificationId: "n3-warning-maintenance", autoShown: false, read: false },
  ],
  u6: [
    { notificationId: "n1-onboarding", autoShown: false, read: false },
    { notificationId: "n2-feature-help-center", autoShown: false, read: true, readAt: "2026-06-11T14:00:00.000Z" },
    { notificationId: "n3-warning-maintenance", autoShown: false, read: false },
  ],
};
