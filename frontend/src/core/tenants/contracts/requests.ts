export type CreateTenantRequest = {
  name: string;
  slug: string;
  plan: string;
  /** E-mail do Tenant Admin inicial, convidado junto com a criação do tenant. */
  initialAdminEmail: string;
};

export type UpdateTenantRequest = {
  name: string;
  plan: string;
  status: "ativo" | "suspenso";
};

export type DeleteTenantRequest = {
  /** Texto digitado pelo usuário para confirmar a exclusão (ex.: o nome do tenant). Validado na própria tela, não pelo service — aqui só registra a intenção para o backend futuro auditar. */
  confirmationText: string;
};
