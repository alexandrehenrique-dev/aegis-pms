import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Filter, RefreshCw, X, UserRound } from "lucide-react";
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

type FilterKey = "role" | "status" | "product";

const ROLE_LABELS: Record<string, string> = {
  PRODUCT_MANAGER: "Product Manager",
  EDITOR: "Editor",
  VIEWER: "Visualizador",
};

const ADMIN_ROLES = ["super_admin", "tenant_admin"];
const ROLE_OPTIONS = ["Super Admin", "Tenant Admin", "Product Manager", "Editor", "Viewer", "Visualizador"];
const STATUS_OPTIONS = ["ativo", "convidado", "bloqueado", "removido"];

function isAdminRole(role: string): boolean {
  return ADMIN_ROLES.includes(role);
}

function uniqueSorted(values: string[]): string[] {
  return Array.from(new Set(values.filter(Boolean))).sort((a, b) => a.localeCompare(b, "pt-BR"));
}

function productNamesFrom(user: UserSummary): string[] {
  return user.products.split(",").map((product) => product.trim()).filter(Boolean);
}

function matchesProduct(user: UserSummary, productName: string): boolean {
  return user.products === "Todos" || productNamesFrom(user).includes(productName);
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

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-medium transition-colors ${
        active ? "bg-primary text-primary-foreground shadow-sm" : "bg-muted text-muted-foreground hover:bg-muted/70"
      }`}
    >
      {children}
    </button>
  );
}

function FilterGroup({ label, options, value, onChange }: { label: string; options: string[]; value: string | null; onChange: (v: string | null) => void }) {
  return (
    <div className="mb-4">
      <p className="mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
      <div className="flex flex-wrap gap-1">
        <Chip active={!value} onClick={() => onChange(null)}>Todos</Chip>
        {options.map((o) => (
          <Chip key={o} active={value === o} onClick={() => onChange(value === o ? null : o)}>
            {o}
          </Chip>
        ))}
      </div>
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

export function UserTable() {
  const navigate = useNavigate();
  const { effectiveTenant, effectiveProduct, tenantProducts } = useAuth();
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

  const options = useMemo(() => {
    const productNames = users.flatMap(productNamesFrom).filter((product) => product !== "Todos");
    return {
      roles: uniqueSorted([...ROLE_OPTIONS, ...users.map((u) => u.role)]),
      statuses: uniqueSorted([...STATUS_OPTIONS, ...users.map((u) => u.status)]),
      products: uniqueSorted([...tenantProducts.map((product) => product.name), ...productNames]),
    };
  }, [tenantProducts, users]);

  const filters: Partial<Record<FilterKey, string | null>> = {
    role: roleFilter,
    status: statusFilter,
    product: productFilter,
  };

  const activeFilterCount = Object.values(filters).filter(Boolean).length;
  const clearFilters = () => {
    setRoleFilter(null);
    setStatusFilter(null);
    setProductFilter(null);
  };

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  const filtered = users.filter((u) =>
    (!roleFilter   || u.role     === roleFilter) &&
    (!statusFilter || u.status   === statusFilter) &&
    (!productFilter || matchesProduct(u, productFilter)),
  );

  const pageTitle = isTenantWideView ? "Usuários do tenant" : `Equipe — ${effectiveProduct?.name ?? "Produto"}`;
  const pageDesc  = isTenantWideView
    ? "Gestão de pessoas, papéis e convites no tenant atual."
    : "Membros atribuídos a este produto. Para gerenciar a equipe, acesse Configurações → Equipe.";

  return (
    <>
      <PageHeader title={pageTitle} module="Users" desc={pageDesc} badge={isTenantWideView ? effectiveTenant?.name : effectiveProduct?.name}>
        <Popover>
          <PopoverTrigger asChild>
            <Button><Filter size={15} />Papel / status / produto</Button>
          </PopoverTrigger>
          <PopoverContent className="w-80">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-sm font-semibold">Filtros</h2>
              {activeFilterCount > 0 && (
                <button onClick={clearFilters} className="flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] text-muted-foreground transition-colors hover:bg-muted hover:text-foreground">
                  <X size={10} /> Limpar
                </button>
              )}
            </div>
            <FilterGroup label="Papel" options={options.roles} value={roleFilter} onChange={setRoleFilter} />
            <FilterGroup label="Status" options={options.statuses} value={statusFilter} onChange={setStatusFilter} />
            <FilterGroup label="Produto" options={options.products} value={productFilter} onChange={setProductFilter} />
          </PopoverContent>
        </Popover>
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
