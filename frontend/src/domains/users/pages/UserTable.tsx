import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Filter, RefreshCw, UserRound } from "lucide-react";
import { Button, EmptyState, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { UserStatusBadge } from "../components/UserStatusBadge";
import { UserCardMobile } from "../components/UserCardMobile";
import { usersService } from "../services/usersService";
import { productAssignmentsService } from "../services/productAssignmentsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useAuth } from "../../../core/auth/useAuth";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import type { UserSummary } from "../contracts/responses";
import type { ProductAssignmentSummary } from "../contracts/productAssignments";

// ─── Helpers ─────────────────────────────────────────────────────────────────

const ROLE_LABELS: Record<string, string> = {
  PRODUCT_MANAGER: "Product Manager",
  EDITOR: "Editor",
  VIEWER: "Visualizador",
};

const ADMIN_ROLES = ["super_admin", "tenant_admin"];

function isAdminRole(role: string): boolean {
  return ADMIN_ROLES.includes(role);
}

/** Converte um `ProductAssignmentSummary` para o shape de exibição da tabela */
function assignmentToRow(a: ProductAssignmentSummary): UserSummary {
  return {
    userId: a.userSubject ?? a.userName,
    name: a.userName,
    email: a.userEmail,
    role: ROLE_LABELS[a.role] ?? a.role,
    products: a.productName,
    status: a.status === "convidado" ? "convidado" : "ativo",
    lastAccess: a.createdAt ? new Date(a.createdAt).toLocaleDateString("pt-BR") : "—",
    inviteStatus: a.status === "convidado" ? "pendente" : "—",
  };
}

// ─── Sub-componente de filtros ────────────────────────────────────────────────

function FilterGroup({ label, options, value, onChange }: { label: string; options: string[]; value: string | null; onChange: (v: string | null) => void }) {
  return (
    <div className="mb-3">
      <p className="mb-1 text-sm font-medium">{label}</p>
      <div className="flex flex-wrap gap-1">
        <Button onClick={() => onChange(null)} primary={!value}>Todos</Button>
        {options.map((o) => <Button key={o} onClick={() => onChange(o)} primary={value === o}>{o}</Button>)}
      </div>
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

export function UserTable() {
  const navigate = useNavigate();
  const { effectiveTenant, effectiveProduct } = useAuth();
  const { viewAsRole } = useViewAsRole();

  const [roleFilter, setRoleFilter] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | null>(null);
  const [productFilter, setProductFilter] = useState<string | null>(null);
  // Incrementar força re-fetch do useAsyncData (ex: botão "Atualizar")
  const [refreshKey, setRefreshKey] = useState(0);

  // Super Admin e Tenant Admin → visão de todos os usuários do tenant
  const isTenantWideView = isAdminRole(viewAsRole);

  const { data: tenantUsers, loading: loadingTenant, error: errorTenant } = useAsyncData(
    () => (isTenantWideView ? usersService.listUsers(effectiveTenant?.id) : Promise.resolve(null)),
    [isTenantWideView, effectiveTenant?.id, refreshKey],
  );

  // PM / Editor / Viewer → visão dos membros do produto atual
  const { data: productAssignments, loading: loadingProduct, error: errorProduct } = useAsyncData(
    () => (!isTenantWideView && effectiveProduct?.id ? productAssignmentsService.listForProduct(effectiveProduct.id) : Promise.resolve(null)),
    [isTenantWideView, effectiveProduct?.id, refreshKey],
  );

  const loading = loadingTenant || loadingProduct;
  const error = errorTenant || errorProduct;

  // Normaliza os dois formatos para a mesma shape de exibição
  const users: UserSummary[] = useMemo(() => {
    if (isTenantWideView) return tenantUsers ?? [];
    return (productAssignments ?? []).map(assignmentToRow);
  }, [isTenantWideView, tenantUsers, productAssignments]);

  const options = useMemo(() => ({
    roles:    Array.from(new Set(users.map((u) => u.role))).filter(Boolean),
    statuses: Array.from(new Set(users.map((u) => u.status))).filter(Boolean),
    products: Array.from(new Set(users.map((u) => u.products))).filter(Boolean),
  }), [users]);

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  const filtered = users.filter((u) =>
    (!roleFilter   || u.role     === roleFilter) &&
    (!statusFilter || u.status   === statusFilter) &&
    (!productFilter || u.products === productFilter),
  );

  const pageTitle = isTenantWideView ? "Usuários do tenant" : `Equipe — ${effectiveProduct?.name ?? "Produto"}`;
  const pageDesc  = isTenantWideView
    ? "Gestão de pessoas, papéis e convites no tenant atual."
    : "Membros atribuídos a este produto. Para gerenciar a equipe, acesse Configurações → Equipe.";

  return (
    <>
      <PageHeader title={pageTitle} module="Users" desc={pageDesc} badge={isTenantWideView ? effectiveTenant?.name : effectiveProduct?.name}>
        {options.roles.length > 0 && (
          <Popover>
            <PopoverTrigger asChild>
              <Button><Filter size={15} />Papel / status / produto</Button>
            </PopoverTrigger>
            <PopoverContent className="w-80">
              {options.roles.length > 1    && <FilterGroup label="Papel"   options={options.roles}    value={roleFilter}    onChange={setRoleFilter} />}
              {options.statuses.length > 1  && <FilterGroup label="Status"  options={options.statuses} value={statusFilter}  onChange={setStatusFilter} />}
              {options.products.length > 1  && <FilterGroup label="Produto" options={options.products} value={productFilter} onChange={setProductFilter} />}
            </PopoverContent>
          </Popover>
        )}
        <Button onClick={() => setRefreshKey((k) => k + 1)} title="Atualizar lista">
          <RefreshCw size={15} />
        </Button>
        {isTenantWideView && (
          <Button data-tour="users-convidar" primary onClick={() => navigate("/users/invite")}>
            Convidar
          </Button>
        )}
        {!isTenantWideView && (
          <Button primary onClick={() => navigate("/settings/team")}>
            <UserRound size={15} /> Gerenciar equipe
          </Button>
        )}
      </PageHeader>

      {filtered.length === 0 ? (
        <EmptyState
          title={isTenantWideView ? "Nenhum usuário encontrado" : "Nenhum membro neste produto"}
          description={
            isTenantWideView
              ? "Convide o primeiro usuário para este tenant."
              : "Atribua membros via Configurações → Equipe."
          }
          primaryAction={
            isTenantWideView
              ? { label: "Convidar usuário", onClick: () => navigate("/users/invite") }
              : { label: "Gerenciar equipe", onClick: () => navigate("/settings/team") }
          }
        />
      ) : (
        <div data-tour="users-table" className="overflow-hidden rounded-2xl border border-border bg-card">
          <table className="hidden w-full text-left text-sm lg:table">
            <thead className="bg-muted text-xs text-muted-foreground">
              <tr>
                {["Nome", "Email", "Papel", "Produtos", "Status", "Último acesso", "Convite", "Ações"].map((h) => (
                  <th key={h} className="p-3">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((u) => (
                <tr key={u.email} className="border-t border-border hover:bg-muted/40">
                  <td className="p-3 font-medium">{u.name}</td>
                  <td className="p-3 text-muted-foreground">{u.email}</td>
                  <td className="p-3">{u.role}</td>
                  <td className="p-3 text-muted-foreground">{u.products}</td>
                  <td className="p-3"><UserStatusBadge s={u.status} /></td>
                  <td className="p-3 text-muted-foreground">{u.lastAccess}</td>
                  <td className="p-3 text-muted-foreground">{u.inviteStatus}</td>
                  <td className="p-3">
                    <div className="flex gap-1">
                      <Button onClick={() => navigate(`/users/${u.userId}`)}>Abrir</Button>
                      {isTenantWideView && (
                        <Button onClick={() => navigate("/settings/permissions")}>Permissões</Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="grid gap-3 p-3 lg:hidden">
            {filtered.map((u) => <UserCardMobile key={u.email} u={u} />)}
          </div>
        </div>
      )}

      {/* Hint para roles não-admin: mostrar link para a gestão real */}
      {!isTenantWideView && filtered.length > 0 && (
        <p className="mt-3 text-center text-xs text-muted-foreground">
          Esta é a visão somente-leitura da equipe deste produto.{" "}
          <button
            className="text-primary underline underline-offset-2"
            onClick={() => navigate("/settings/team")}
          >
            Gerenciar membros em Configurações → Equipe
          </button>
        </p>
      )}
    </>
  );
}
