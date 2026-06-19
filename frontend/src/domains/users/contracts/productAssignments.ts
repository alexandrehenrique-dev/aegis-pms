/**
 * Atribuição de um produto a um usuário com um papel específico — conceito
 * novo desta sprint (não existe ainda como entidade no backend planejado;
 * ver docs/trace/00_endpoints_esperados.md, Seção C).
 */
export type AssignProductUserRequest = {
  tenantId: string;
  productId: string;
  /** Preenchido quando o usuário já existe no tenant. */
  userId?: string;
  /** Preenchido quando o usuário ainda não existe — dispara um convite. */
  inviteEmail?: string;
  inviteName?: string;
  role: string;
};

export type ProductAssignmentSummary = {
  tenantId: string;
  productId: string;
  productName: string;
  userName: string;
  userEmail: string;
  role: string;
  /** "atribuido" quando o usuário já existia; "convidado" quando nasceu de um convite. */
  status: "atribuido" | "convidado";
};
