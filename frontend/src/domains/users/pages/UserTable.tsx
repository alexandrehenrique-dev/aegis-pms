import { useNavigate } from "react-router";
import { Filter } from "lucide-react";
import { Button, PageHeader } from "../../../shared/components/Primitives";
import { UserStatusBadge } from "../components/UserStatusBadge";
import { UserCardMobile } from "../components/UserCardMobile";
import { usersRows } from "../mocks/users.mocks";

export function UserTable() {
  const navigate = useNavigate();
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
            {usersRows.map((r) => (
              <tr key={r[1]} className="border-t border-border hover:bg-muted/40">
                <td className="p-3 font-medium">{r[0]}</td>
                {r.slice(1, 4).map((c, i) => <td key={`${r[1]}-${i}`} className="p-3">{c}</td>)}
                <td className="p-3"><UserStatusBadge s={r[4]} /></td>
                <td className="p-3">{r[5]}</td>
                <td className="p-3">{r[6]}</td>
                <td className="p-3"><div className="flex gap-1"><Button onClick={() => navigate("/users/1")}>Abrir</Button><Button>Permissões</Button></div></td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="grid gap-3 p-3 lg:hidden">{usersRows.map((u) => <UserCardMobile key={u[1]} u={u} />)}</div>
      </div>
    </>
  );
}
