/**
 * Atribuição de um produto a um usuário com um papel específico — conceito
 * Atribuição real no endpoint de usuários do produto.
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
  id?: string;
  tenantId: string;
  productId: string;
  productName: string;
  userSubject?: string;
  userName: string;
  userEmail: string;
  role: string;
  /** "atribuido" quando o usuário já existia; "convidado" quando nasceu de um convite. */
  status: "atribuido" | "convidado";
  createdAt?: string;
  updatedAt?: string;
};
