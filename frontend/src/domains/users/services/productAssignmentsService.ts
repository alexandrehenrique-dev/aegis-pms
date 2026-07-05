import { usersService } from "./usersService";
import { productsService } from "../../products/services/productsService";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AssignProductUserRequest, ProductAssignmentSummary } from "../contracts/productAssignments";

/**
 * Mock de atribuições por produto.
 * IDs correspondem aos IDs dos produtos mock (p1, p2, etc.) definidos em
 * `core/auth/mocks/users.ts` → `mockProductsByUser`. O ProductTeamPanel faz
 * `listForProduct(effectiveProduct.id)`, portanto os IDs precisam bater.
 *
 * Jornadas cobertas:
 *   - p1 (Maestro Beton):  PM = Marina, Editor = Rafael
 *   - p2 (Aion Logbook):   PM = Super Admin (o próprio dono da plataforma)
 *   - p3 (Eirene UI):      PM = Ana (Tenant Admin que também tem produtos)
 *   - p4 (Genesis):        PM = Super Admin
 *   - p6 (Conecta Talentos): PM = Beatriz
 *   - p12 (Alexandre Dev): Editor = Rafael, Viewer = João (convidado)
 */
const MOCK_ASSIGNMENTS: ProductAssignmentSummary[] = [
  // p1 — Maestro Beton
  { id: "a1", tenantId: "t1", productId: "p1", productName: "Maestro Beton",    userName: "Marina Costa", userEmail: "pm@byop.io",     userSubject: "u2", role: "PRODUCT_MANAGER", status: "atribuido" },
  { id: "a2", tenantId: "t1", productId: "p1", productName: "Maestro Beton",    userName: "Rafael Lima",  userEmail: "editor@byop.io", userSubject: "u3", role: "EDITOR",          status: "atribuido" },
  // p2 — Aion Logbook (produto do super-admin)
  { id: "a3", tenantId: "t1", productId: "p2", productName: "Aion Logbook",     userName: "Super Admin",  userEmail: "super-admin@byop.io", userSubject: "u5", role: "PRODUCT_MANAGER", status: "atribuido" },
  // p3 — Eirene UI (produto da Ana/tenant-admin)
  { id: "a4", tenantId: "t1", productId: "p3", productName: "Eirene UI",        userName: "Ana Martins",  userEmail: "admin@byop.io",  userSubject: "u1", role: "PRODUCT_MANAGER", status: "atribuido" },
  // p4 — Genesis (produto do super-admin)
  { id: "a5", tenantId: "t1", productId: "p4", productName: "Genesis",          userName: "Super Admin",  userEmail: "super-admin@byop.io", userSubject: "u5", role: "PRODUCT_MANAGER", status: "atribuido" },
  // p6 — Conecta Talentos
  { id: "a6", tenantId: "t1", productId: "p6", productName: "Conecta Talentos", userName: "Beatriz Nunes",userEmail: "bea@byop.com",  userSubject: "kc-subj-005", role: "PRODUCT_MANAGER", status: "atribuido" },
  // p12 — Alexandre Dev
  { id: "a7", tenantId: "t1", productId: "p12", productName: "Alexandre Dev",   userName: "Rafael Lima",  userEmail: "editor@byop.io", userSubject: "u3", role: "EDITOR",          status: "atribuido" },
  { id: "a8", tenantId: "t1", productId: "p12", productName: "Alexandre Dev",   userName: "João Alves",   userEmail: "viewer@byop.io", userSubject: "u6", role: "VIEWER",          status: "convidado" },
];

// Runtime store — começa com os mocks e aceita adições/remoções na sessão
export const assignmentsStore: ProductAssignmentSummary[] = [...MOCK_ASSIGNMENTS];

export const productAssignmentsService = {
  async listForProduct(productId: string): Promise<ProductAssignmentSummary[]> {
    if (IS_API_MODE) return apiClient.get<ProductAssignmentSummary[]>(`/products/${productId}/users`);
    return assignmentsStore.filter((a) => a.productId === productId);
  },

  async remove(productId: string, userSubject: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/users/${userSubject}`);
    logApiCall("DELETE", `/api/v1/products/${productId}/users/${userSubject}`);
    const index = assignmentsStore.findIndex(
      (a) => a.productId === productId && (a.userSubject === userSubject || a.userName === userSubject),
    );
    if (index >= 0) assignmentsStore.splice(index, 1);
  },

  /**
   * Atribui um usuário a um produto.
   *
   * Lógica de detecção (mock mode):
   *   1. Se `userId` fornecido → usuário existente → salva assignment, simula
   *      e-mail de NOTIFICAÇÃO ("você foi atribuído ao produto X").
   *   2. Se `inviteEmail` fornecido:
   *      a. Email já existe no tenant → trata como existente → notificação.
   *      b. Email novo              → cria convite → simula e-mail de CONVITE
   *         ("crie sua senha e acesse o produto X").
   *
   * Em API mode, tudo é delegado ao backend que decide a mesma lógica via
   * `ProductAssignmentService.assignUser()`.
   */
  async assign(req: AssignProductUserRequest): Promise<ProductAssignmentSummary> {
    if (IS_API_MODE) return apiClient.post<ProductAssignmentSummary>(`/products/${req.productId}/users`, req);
    logApiCall("POST", `/api/v1/products/${req.productId}/users`, req);

    const products = await productsService.listProducts();
    const product = products.find((p) => p.id === req.productId);

    let userName = "";
    let userEmail = "";
    let userSubject: string | undefined;
    let status: ProductAssignmentSummary["status"] = "atribuido";
    let emailType: "notification" | "invite" = "notification";

    if (req.userId) {
      // ── Caminho 1: userId explícito → usuário existente ──────────────────
      const users = await usersService.listUsers(req.tenantId);
      const existing = users.find((u) => u.userId === req.userId);
      userName    = existing?.name  ?? req.userId;
      userEmail   = existing?.email ?? req.userId;
      userSubject = req.userId;
      emailType   = "notification";
      logApiCall("EMAIL", `[SIMULADO] Notificação de atribuição → ${userEmail}`, {
        to: userEmail, product: product?.name, role: req.role,
      });

    } else if (req.inviteEmail) {
      // ── Caminho 2: email fornecido → detectar se é existente ou novo ─────
      const users = await usersService.listUsers(req.tenantId);
      const existingByEmail = users.find(
        (u) => u.email?.toLowerCase() === req.inviteEmail!.toLowerCase(),
      );

      if (existingByEmail) {
        // Usuário já tem conta → apenas notifica
        userName    = existingByEmail.name;
        userEmail   = existingByEmail.email;
        userSubject = existingByEmail.userId;
        status      = "atribuido";
        emailType   = "notification";
        logApiCall("EMAIL", `[SIMULADO] Notificação de atribuição (usuário existente detectado) → ${userEmail}`, {
          to: userEmail, product: product?.name, role: req.role,
        });
      } else {
        // Usuário novo → criar convite com senha
        const invited = await usersService.invite(
          { name: req.inviteName || req.inviteEmail, email: req.inviteEmail, role: req.role, allowedProducts: product?.name ?? req.productId },
          req.tenantId,
        );
        userName    = invited.name;
        userEmail   = invited.email;
        userSubject = invited.userId;
        status      = "convidado";
        emailType   = "invite";
        logApiCall("EMAIL", `[SIMULADO] Convite de cadastro (novo usuário) → ${userEmail}`, {
          to: userEmail, product: product?.name, role: req.role, type: "invite-with-password-setup",
        });
      }
    }

    const created: ProductAssignmentSummary = {
      id: `mock-${Date.now()}`,
      tenantId:    req.tenantId,
      productId:   req.productId,
      productName: product?.name ?? req.productId,
      userName,
      userEmail,
      userSubject,
      role:   req.role,
      status,
    };
    assignmentsStore.push(created);

    // Expõe o tipo de email ao caller para UX granular no toast
    (created as ProductAssignmentSummary & { _emailType?: string })._emailType = emailType;
    return created;
  },

  /**
   * Adiciona um assignment direto ao store sem passar pelo fluxo de
   * convite/notificação — usado pelo `productsService.create()` para
   * auto-atribuir o criador como PRODUCT_MANAGER.
   */
  addToStore(assignment: ProductAssignmentSummary): void {
    assignmentsStore.push(assignment);
  },
};
