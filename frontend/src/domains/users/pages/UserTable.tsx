import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Filter } from "lucide-react";
import { Button, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { Popover, PopoverContent, PopoverTrigger } from "../../../shared/components/ui/popover";
import { UserStatusBadge } from "../components/UserStatusBadge";
import { UserCardMobile } from "../components/UserCardMobile";
import { usersService } from "../services/usersService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

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

export function UserTable() {
  const navigate = useNavigate();
  const { data: users, loading, error } = useAsyncData(() => usersService.listUsers(), []);
  const [role, setRole] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [product, setProduct] = useState<string | null>(null);

  const options = useMemo(() => ({
    roles: Array.from(new Set((users ?? []).map((u) => u.role))),
    statuses: Array.from(new Set((users ?? []).map((u) => u.status))),
    products: Array.from(new Set((users ?? []).map((u) => u.products))),
  }), [users]);

  if (loading) return <SkeletonLines />;
  if (error || !users) return <PartialErrorWidget />;

  const filtered = users.filter((u) => (!role || u.role === role) && (!status || u.status === status) && (!product || u.products === product));

  return (
    <>
      <PageHeader title="Usuários" module="Users" desc="Gestão operacional de pessoas, papéis, produtos e convites." badge="BYOP">
        <Popover>
          <PopoverTrigger asChild><Button><Filter size={15} />Papel / status / produto</Button></PopoverTrigger>
          <PopoverContent className="w-80">
            <FilterGroup label="Papel" options={options.roles} value={role} onChange={setRole} />
            <FilterGroup label="Status" options={options.statuses} value={status} onChange={setStatus} />
            <FilterGroup label="Produto" options={options.products} value={product} onChange={setProduct} />
          </PopoverContent>
        </Popover>
        <Button data-tour="users-convidar" primary onClick={() => navigate("/users/invite")}>Convidar</Button>
      </PageHeader>
      <div data-tour="users-table" className="overflow-hidden rounded-2xl border border-border bg-card">
        <table className="hidden w-full text-left text-sm lg:table">
          <thead className="bg-muted text-xs text-muted-foreground">
            <tr>{["Nome", "Email", "Papel", "Produtos", "Status", "Último acesso", "Convite", "Ações"].map((h) => <th key={h} className="p-3">{h}</th>)}</tr>
          </thead>
          <tbody>
            {filtered.map((u) => (
              <tr key={u.email} className="border-t border-border hover:bg-muted/40">
                <td className="p-3 font-medium">{u.name}</td>
                <td className="p-3">{u.email}</td>
                <td className="p-3">{u.role}</td>
                <td className="p-3">{u.products}</td>
                <td className="p-3"><UserStatusBadge s={u.status} /></td>
                <td className="p-3">{u.lastAccess}</td>
                <td className="p-3">{u.inviteStatus}</td>
                <td className="p-3"><div className="flex gap-1"><Button onClick={() => navigate("/users/1")}>Abrir</Button><Button onClick={() => navigate("/settings/permissions")}>Permissões</Button></div></td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">{filtered.map((u) => <UserCardMobile key={u.email} u={u} />)}</div>
      </div>
    </>
  );
}
