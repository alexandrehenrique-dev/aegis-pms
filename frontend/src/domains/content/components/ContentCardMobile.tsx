import { useNavigate } from "react-router";
import { Button, Card } from "../../../shared/components/Primitives";
import { ContentStatusBadge } from "./ContentStatusBadge";

export function ContentCardMobile({ row }: { row: string[] }) {
  const navigate = useNavigate();
  return (
    <Card>
      <div className="flex justify-between gap-2">
        <div><h3 className="font-semibold">{row[0]}</h3><p className="text-sm text-muted-foreground">{row[1]} · {row[2]} · {row[3]}</p></div>
        <ContentStatusBadge status={row[4]} />
      </div>
      <p className="mt-3 text-sm text-muted-foreground">Última atualização: {row[5]} · {row[7]}</p>
      <div className="mt-3 flex gap-2">
        <Button onClick={() => navigate(`/content/${row[0].toLowerCase()}/editor`)}>Editar</Button>
        <Button onClick={() => navigate(`/content/${row[0].toLowerCase()}/preview`)}>Preview</Button>
      </div>
    </Card>
  );
}
