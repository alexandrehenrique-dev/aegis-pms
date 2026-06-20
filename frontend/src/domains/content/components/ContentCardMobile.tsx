import { useNavigate } from "react-router";
import { Button, Card } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/ViewAsRoleContext";
import { ContentStatusBadge } from "./ContentStatusBadge";
import type { ContentRow } from "../contracts/responses";

export function ContentCardMobile({ row }: { row: ContentRow }) {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const canEdit = viewAsRole !== "viewer";
  return (
    <Card>
      <div className="flex justify-between gap-2">
        <div><h3 className="font-semibold">{row.title}</h3><p className="text-sm text-muted-foreground">{row.type} · {row.lang} · {row.author}</p></div>
        <ContentStatusBadge status={row.status} />
      </div>
      <p className="mt-3 text-sm text-muted-foreground">Última atualização: {row.updatedAt} · {row.version}</p>
      <div className="mt-3 flex gap-2">
        <PermGate allowed={canEdit}><Button onClick={() => navigate(`/content/${row.title.toLowerCase()}/editor`)}>Editar</Button></PermGate>
        <Button onClick={() => navigate(`/content/${row.title.toLowerCase()}/preview`)}>Preview</Button>
      </div>
    </Card>
  );
}
