import { useNavigate } from "react-router";
import { Filter } from "lucide-react";
import { Button, PageHeader, SkeletonLines, PartialErrorWidget } from "../../../shared/components/Primitives";
import { UserStatusBadge } from "../components/UserStatusBadge";
import { UserCardMobile } from "../components/UserCardMobile";
import { usersService } from "../services/usersService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";

export function UserTable() {
  const navigate = useNavigate();
  const { data: users, loading, error } = useAsyncData(() => usersService.listUsers(), []);

  if (loading) return <SkeletonLines />;
  if (error || !users) return <PartialErrorWidget />;

  return (
    <>
      <PageHeader title="Usuários" module="Users" desc="Gestão operacional de pessoas, papéis, produtos e convites." badge="BYOP">
        <Button><Filter size={15} />Papel / status / produto</Button>
        <Button primary onClick={() => navigate("/users/invite")}>Convidar</Button>
      </PageHeader>
      <div className="overflow-hidden rounded-2xl border border-border bg-card">
        <table className="hidden w-full text-left text-sm lg:table">
          <thead className="bg-muted text-xs text-muted-foreground">
            <tr>{["Nome", "Email", "Papel", "Produtos", "Status", "Último acesso", "Convite", "Ações"].map((h) => <th key={h} className="p-3">{h}</th>)}</tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.email} className="border-t border-border hover:bg-muted/40">
                <td className="p-3 font-medium">{u.name}</td>
                <td className="p-3">{u.email}</td>
                <td className="p-3">{u.role}</td>
                <td className="p-3">{u.products}</td>
                <td className="p-3"><UserStatusBadge s={u.status} /></td>
                <td className="p-3">{u.lastAccess}</td>
                <td className="p-3">{u.inviteStatus}</td>
                <td className="p-3"><div className="flex gap-1"><Button onClick={() => navigate("/users/1")}>Abrir</Button><Button>Permissões</Button></div></td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">{users.map((u) => <UserCardMobile key={u.email} u={u} />)}</div>
      </div>
    </>
  );
}
