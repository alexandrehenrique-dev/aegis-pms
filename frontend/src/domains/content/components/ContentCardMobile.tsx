import { useNavigate } from "react-router";
import { Trash2 } from "lucide-react";
import { Button, Card } from "../../../shared/components/Primitives";
import { PermGate } from "../../../app/guards/PermGate";
import { useViewAsRole } from "../../../core/permissions/useViewAsRole";
import { ContentStatusBadge } from "./ContentStatusBadge";
import type { ContentRow } from "../contracts/responses";

export function ContentCardMobile({ row, onArchive, onRequestDelete, canDelete }: {
  row: ContentRow;
  onArchive: () => void;
  onRequestDelete: () => void;
  canDelete: boolean;
}) {
  const navigate = useNavigate();
  const { viewAsRole } = useViewAsRole();
  const canEdit = ["product_manager", "editor"].includes(viewAsRole);
  return (
    <Card>
      <div className="flex justify-between gap-2">
        <div><h3 className="font-semibold">{row.title}</h3><p className="text-sm text-muted-foreground">{row.type} · {row.lang} · {row.author}</p></div>
        <ContentStatusBadge status={row.status} />
      </div>
      <p className="mt-3 text-sm text-muted-foreground">Última atualização: {row.updatedAt} · {row.version}</p>
      <div className="mt-3 flex flex-wrap gap-2">
        <PermGate allowed={canEdit}><Button onClick={() => navigate(`/content/${row.id}/editor`)}>Editar</Button></PermGate>
        <Button onClick={() => navigate(`/content/${row.id}/preview`)}>Preview</Button>
        {(row.status === "Published" || row.status === "In Review") && (
          <PermGate allowed={canEdit}><Button onClick={onArchive}>Arquivar</Button></PermGate>
        )}
        {row.status === "Draft" && (
          <PermGate allowed={canDelete}>
            <button onClick={onRequestDelete} aria-label={`Excluir ${row.title}`} className="rounded-lg p-2 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"><Trash2 size={14} /></button>
          </PermGate>
        )}
      </div>
    </Card>
  );
}
