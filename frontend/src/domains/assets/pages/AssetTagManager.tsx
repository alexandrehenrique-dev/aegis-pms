import { useState } from "react";
import { AnimatePresence } from "motion/react";
import { Loader2 } from "lucide-react";
import { Badge, Button, Card, EmptyState, Field, PageHeader, PartialErrorWidget, PermissionHint, SkeletonLines } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { assetsService } from "../services/assetsService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";

export function AssetTagManager() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: loadedTags, loading, error } = useAsyncData(() => (productId ? assetsService.listTags(productId) : Promise.resolve([])), [productId]);
  const [tags, setTags] = useState<string[] | null>(null);
  const assetTags = tags ?? loadedTags;

  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState("");
  const [creating, setCreating] = useState(false);

  const [mergeOpen, setMergeOpen] = useState(false);
  const [mergeSelected, setMergeSelected] = useState<Set<string>>(new Set());
  const [mergeInto, setMergeInto] = useState("");
  const [merging, setMerging] = useState(false);

  const [editingTag, setEditingTag] = useState<string | null>(null);
  const [editValue, setEditValue] = useState("");
  const [savingEdit, setSavingEdit] = useState(false);

  const [removingTag, setRemovingTag] = useState<string | null>(null);
  const [removing, setRemoving] = useState(false);

  const refresh = async () => setTags(await assetsService.listTags(productId));

  const handleCreate = async () => {
    setCreating(true);
    try {
      await assetsService.createTag(productId, name);
      toast.success("Tag criada!", { description: `#${name} já está disponível para uso.` });
      setCreateOpen(false);
      setName("");
      await refresh();
    } finally {
      setCreating(false);
    }
  };

  const toggleMergeSelect = (t: string) => {
    setMergeSelected((prev) => {
      const next = new Set(prev);
      if (next.has(t)) next.delete(t); else next.add(t);
      return next;
    });
  };

  const handleMerge = async () => {
    setMerging(true);
    try {
      await assetsService.mergeTags(productId, Array.from(mergeSelected), mergeInto);
      toast.success("Tags mescladas!", { description: `Unificadas em #${mergeInto}.` });
      setMergeOpen(false);
      setMergeSelected(new Set());
      setMergeInto("");
      await refresh();
    } finally {
      setMerging(false);
    }
  };

  const handleSaveEdit = async () => {
    if (!editingTag) return;
    setSavingEdit(true);
    try {
      await assetsService.renameTag(productId, editingTag, editValue);
      toast.success("Tag renomeada!");
      setEditingTag(null);
      await refresh();
    } finally {
      setSavingEdit(false);
    }
  };

  const handleRemove = async () => {
    if (!removingTag) return;
    setRemoving(true);
    try {
      await assetsService.removeTag(productId, removingTag);
      toast.success("Tag removida.", { description: `#${removingTag}` });
      setRemovingTag(null);
      await refresh();
    } finally {
      setRemoving(false);
    }
  };

  return (
    <>
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Criar tag</DialogTitle></DialogHeader>
          <Field label="Nome da tag" value={name} onChange={setName} />
          <DialogFooter>
            <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
            <Button primary onClick={handleCreate} disabled={creating || !name.trim()}>{creating && <Loader2 size={15} className="animate-spin" />}{creating ? "Criando..." : "Criar"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={mergeOpen} onOpenChange={setMergeOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Mesclar tags</DialogTitle></DialogHeader>
          <div className="space-y-2">
            <p className="text-sm text-muted-foreground">Selecione as tags a mesclar:</p>
            <div className="flex flex-wrap gap-2">
              {(assetTags ?? []).map((t) => (
                <button key={t} onClick={() => toggleMergeSelect(t)} className={`rounded-full border px-3 py-1 text-xs ${mergeSelected.has(t) ? "border-primary bg-primary/10 text-primary" : "border-border"}`}>#{t}</button>
              ))}
            </div>
            <Field label="Nome final da tag mesclada" value={mergeInto} onChange={setMergeInto} />
          </div>
          <DialogFooter>
            <Button onClick={() => setMergeOpen(false)}>Cancelar</Button>
            <Button primary onClick={handleMerge} disabled={merging || mergeSelected.size < 2 || !mergeInto.trim()}>{merging && <Loader2 size={15} className="animate-spin" />}{merging ? "Mesclando..." : "Mesclar"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={editingTag !== null} onOpenChange={(open) => !open && setEditingTag(null)}>
        <DialogContent>
          <DialogHeader><DialogTitle>Editar tag</DialogTitle></DialogHeader>
          <Field label="Nome da tag" value={editValue} onChange={setEditValue} />
          <DialogFooter>
            <Button onClick={() => setEditingTag(null)}>Cancelar</Button>
            <Button primary onClick={handleSaveEdit} disabled={savingEdit || !editValue.trim()}>{savingEdit && <Loader2 size={15} className="animate-spin" />}{savingEdit ? "Salvando..." : "Salvar"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AnimatePresence>
        {removingTag && <ConfirmDialog title={`Remover #${removingTag}?`} desc="Os assets que usam esta tag perderão esta associação." danger loading={removing} onConfirm={handleRemove} onCancel={() => setRemovingTag(null)} />}
      </AnimatePresence>

      <PageHeader title="Tags de Assets" module="Assets" desc="Organize tags como estrutura operacional, não como decoração." badge="Tags">
        <Button onClick={() => setMergeOpen(true)}>Mesclar tags</Button>
        <Button primary onClick={() => setCreateOpen(true)}>Criar tag</Button>
      </PageHeader>
      {loading && !tags ? <SkeletonLines /> : error || !assetTags ? <PartialErrorWidget /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {assetTags.map((t, i) => (
            <Card key={t}>
              <div className="flex items-center justify-between"><h3 className="font-semibold">#{t}</h3><Badge tone={i % 3 === 0 ? "green" : "neutral"}>{i + 2} assets</Badge></div>
              <p className="mt-2 text-sm text-muted-foreground">Tag em uso no produto {product?.name ?? "atual"}.</p>
              <div className="mt-4 flex gap-2">
                <Button onClick={() => { setEditingTag(t); setEditValue(t); }}>Editar</Button>
                <Button onClick={() => setRemovingTag(t)}>Remover</Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <EmptyState compact title="Sem tags" description="Estado previsto para produtos novos." />
        <PartialErrorWidget />
        <PermissionHint />
      </div>
    </>
  );
}
