export type CreateProductRequest = {
  name: string;
  slug: string;
  type: string;
  language: string;
  description: string;
  template: string;
  initialModules: string[];
  /** Presente quando o produto nasce dentro do wizard de Super Admin (Sprint 09, Tarefa C). */
  tenantId?: string;
};
