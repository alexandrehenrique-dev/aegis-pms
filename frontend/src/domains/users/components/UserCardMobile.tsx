import { useNavigate } from "react-router";
import { Button, Card } from "../../../shared/components/Primitives";
import { UserStatusBadge } from "./UserStatusBadge";

export function UserCardMobile({ u }: { u: string[] }) {
  const navigate = useNavigate();
  return (
    <Card>
      <div className="flex justify-between">
        <div><h3 className="font-semibold">{u[0]}</h3><p className="text-sm text-muted-foreground">{u[1]} · {u[2]}</p></div>
        <UserStatusBadge s={u[4]} />
      </div>
      <p className="mt-2 text-sm text-muted-foreground">Produtos: {u[3]} · Último acesso: {u[5]}</p>
      <Button onClick={() => navigate("/users/1")}>Abrir usuário</Button>
    </Card>
  );
}
