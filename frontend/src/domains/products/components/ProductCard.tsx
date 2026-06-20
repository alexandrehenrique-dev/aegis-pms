import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence } from "motion/react";
import { Archive, ExternalLink, MoreHorizontal, Pencil, Star, Trash2 } from "lucide-react";
import { Badge, Button, Card } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { ProductStatusBadge } from "../../../shared/components/ProductStatusBadge";
import { getProductSlug } from "../../../shared/utils/productSlugs";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../../../shared/components/ui/dropdown-menu";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../services/productsService";
import { useProductActions } from "../hooks/useProductActions";
import type { ProductSummary } from "../contracts/responses";

export function ProductCard({ p, onChanged }: { p: ProductSummary; onChanged: () => void }) {
  const navigate = useNavigate();
  const slug = getProductSlug(p.name);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const { canManage, onContextMenu, openEdit, openDelete, portal } = useProductActions(p, onChanged);

  const handleOpen = () => {
    if (!p.modules) { navigate("/products/maestro-beton?empty=1"); return; }
    navigate(slug ? `/products/${slug}` : "/products");
  };

  const handleFavorite = () => {
    toast.success("Adicionado aos favoritos", { description: p.name });
  };

  const handleArchive = async () => {
    setArchiving(true);
    try {
      await productsService.archiveProduct(p.id ?? p.name);
      toast.success("Produto arquivado.", { description: `${p.name} foi movido para arquivados.` });
      setConfirmArchive(false);
      onChanged();
    } finally {
      setArchiving(false);
    }
  };

  return (
    <Card onContextMenu={onContextMenu}>
      <AnimatePresence>
        {confirmArchive && (
          <ConfirmDialog title="Arquivar este produto?" desc={`${p.name} ficará indisponível para operação até ser restaurado.`} danger loading={archiving} onConfirm={handleArchive} onCancel={() => setConfirmArchive(false)} />
        )}
      </AnimatePresence>
      {portal}
      <div className="flex items-start justify-between gap-3">
        <div><h3 className="font-semibold">{p.name}</h3><p className="text-sm text-muted-foreground">{p.type}</p></div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="rounded-lg p-1 hover:bg-muted"><MoreHorizontal size={18} /></button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onSelect={handleOpen}><ExternalLink size={14} />Abrir</DropdownMenuItem>
            <DropdownMenuItem onSelect={handleFavorite}><Star size={14} />Favoritar</DropdownMenuItem>
            {canManage && <DropdownMenuItem onSelect={openEdit}><Pencil size={14} />Editar</DropdownMenuItem>}
            <DropdownMenuItem variant="destructive" onSelect={() => setConfirmArchive(true)}><Archive size={14} />Arquivar</DropdownMenuItem>
            {canManage && <DropdownMenuItem variant="destructive" onSelect={openDelete}><Trash2 size={14} />Excluir</DropdownMenuItem>}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <div className="mt-4 flex flex-wrap gap-2"><ProductStatusBadge status={p.status} /><Badge tone="blue">{p.modules} módulos</Badge></div>
      <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
        <div className="rounded-lg bg-muted p-3"><p className="text-xs text-muted-foreground">Saúde</p><p className="font-semibold">{p.score}</p></div>
        <div className="rounded-lg bg-muted p-3"><p className="text-xs text-muted-foreground">Atividade</p><p className="truncate font-semibold">Hoje</p></div>
      </div>
      <p className="mt-4 text-sm text-muted-foreground">{p.last}</p>
      <Button primary onClick={handleOpen}><ExternalLink size={15} />Abrir produto</Button>
      {canManage && p.status === "Sem módulos" && (
        <p className="mt-2 text-center text-xs text-muted-foreground">Sem módulos habilitados. Clique com o botão direito para editar ou excluir.</p>
      )}
    </Card>
  );
}
