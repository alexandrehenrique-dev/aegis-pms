import { useNavigate } from "react-router";
import { Button, Card } from "../../../shared/components/Primitives";
import { UserStatusBadge } from "./UserStatusBadge";
import type { UserSummary } from "../contracts/responses";

export function UserCardMobile({ u }: { u: UserSummary }) {
  const navigate = useNavigate();
  return (
    <Card>
      <div className="flex justify-between">
        <div><h3 className="font-semibold">{u.name}</h3><p className="text-sm text-muted-foreground">{u.email} · {u.role}</p></div>
        <UserStatusBadge s={u.status} />
      </div>
      <p className="mt-2 text-sm text-muted-foreground">Produtos: {u.products} · Último acesso: {u.lastAccess}</p>
      <Button onClick={() => navigate("/users/1")}>Abrir usuário</Button>
    </Card>
  );
}
