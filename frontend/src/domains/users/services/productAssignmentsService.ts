import { usersService } from "./usersService";
import { productsService } from "../../products/services/productsService";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AssignProductUserRequest, ProductAssignmentSummary } from "../contracts/productAssignments";

const assignmentsStore: ProductAssignmentSummary[] = [];

export const productAssignmentsService = {
  async listForProduct(productId: string): Promise<ProductAssignmentSummary[]> {
    if (IS_API_MODE) return apiClient.get<ProductAssignmentSummary[]>(`/products/${productId}/users`);
    return assignmentsStore.filter((a) => a.productId === productId);
  },

  async assign(req: AssignProductUserRequest): Promise<ProductAssignmentSummary> {
    if (IS_API_MODE) return apiClient.post<ProductAssignmentSummary>(`/products/${req.productId}/users`, req);
    logApiCall("POST", `/api/v1/admin/products/${req.productId}/assignments`, req);
    const products = await productsService.listProducts();
    const product = products.find((p) => p.id === req.productId);

    let userName = "";
    let userEmail = "";
    let status: ProductAssignmentSummary["status"] = "atribuido";

    if (req.inviteEmail) {
      const invited = await usersService.invite({ name: req.inviteName || req.inviteEmail, email: req.inviteEmail, role: req.role, allowedProducts: product?.name ?? req.productId }, req.tenantId);
      userName = invited.name;
      userEmail = invited.email;
      status = "convidado";
    } else if (req.userId) {
      const users = await usersService.listUsers(req.tenantId);
      const existing = users.find((u) => u.email === req.userId);
      userName = existing?.name ?? req.userId;
      userEmail = existing?.email ?? req.userId;
    }

    const created: ProductAssignmentSummary = {
      tenantId: req.tenantId,
      productId: req.productId,
      productName: product?.name ?? req.productId,
      userName,
      userEmail,
      role: req.role,
      status,
    };
    assignmentsStore.push(created);
    return created;
  },
};
