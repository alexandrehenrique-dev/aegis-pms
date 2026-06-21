import { useRef, useState } from "react";
import { motion } from "motion/react";
import { CheckCircle2, Loader2, Plus, X } from "lucide-react";
import { fade, Button } from "../../../shared/components/Primitives";
import { toast } from "../toast";
import { useAuth } from "../../auth/AuthContext";
import { assetsService } from "../../../domains/assets/services/assetsService";
import { feedbackService } from "../services/feedbackService";

export function FeedbackModal({ screenName, onClose }: { screenName: string; onClose: () => void }) {
  const { authUser, effectiveTenant, effectiveProduct } = useAuth();
  const cats = ["Bug", "UX confusa", "Erro visual", "Permissão incorreta", "Informação errada", "Sugestão"] as const;
  const [cat, setCat] = useState<string>("Bug");
  const [desc, setDesc] = useState("");
  const [pri, setPri] = useState<string>("média");
  const [ctx, setCtx] = useState(true);
  const [attachment, setAttachment] = useState<{ name: string; assetId: string } | null>(null);
  const [attachmentUploading, setAttachmentUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [issueId, setIssueId] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  /** Upload acontece já na escolha do arquivo (Sprint 18, Tarefa D.2) — `<input type="file">` real do sistema do usuário, nunca o picker de assets existentes. Falha aqui nunca bloqueia o envio do feedback (Tarefa D.4). */
  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setAttachmentUploading(true);
    try {
      const { assetId } = await assetsService.upload(file);
      setAttachment({ name: file.name, assetId });
    } catch {
      setAttachment(null);
      toast.error("Não foi possível enviar o anexo.", { description: "Você ainda pode enviar o feedback sem ele." });
    } finally {
      setAttachmentUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!desc.trim()) return;
    setLoading(true);
    try {
      const { id } = await feedbackService.create({
        productId: effectiveProduct?.id,
        category: cat,
        priority: pri,
        description: desc,
        screenName: ctx ? screenName : undefined,
        attachmentAssetId: attachment?.assetId,
      });
      setIssueId(id);
      toast.success(`Feedback registrado · ${id}`, { description: "Será revisado em breve.", duration: 5000 });
    } finally {
      setLoading(false);
    }
  };

  const copy = () => {
    try { navigator.clipboard.writeText(issueId!); } catch { /* noop */ }
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (issueId) return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-sm rounded-2xl border border-border bg-card p-6 text-center shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-full bg-[#ede9fe]"><CheckCircle2 size={26} className="text-primary" /></div>
        <h2 className="font-semibold">Feedback enviado!</h2>
        <p className="mt-1 text-sm text-muted-foreground">O problema foi registrado e será revisado pela equipe Aegis.</p>
        <div className="mt-5 rounded-xl bg-muted p-4"><p className="text-xs text-muted-foreground">ID do issue</p><p className="mt-1 font-mono text-2xl font-semibold tracking-wider">{issueId}</p></div>
        <div className="mt-4 flex gap-2"><Button primary onClick={copy}>{copied ? "Copiado ✓" : "Copiar ID"}</Button><Button onClick={onClose}>Fechar</Button></div>
      </motion.div>
    </motion.div>
  );

  return (
    <motion.div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30 backdrop-blur-[3px] p-4 sm:items-center" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-5 flex items-start justify-between">
          <div><h2 className="font-semibold">Reportar problema</h2><p className="text-xs text-muted-foreground mt-0.5">Ajude a melhorar o Aegis PMS</p></div>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>
        <div className="space-y-4">
          <div>
            <p className="mb-1.5 text-sm font-medium">Categoria</p>
            <div className="flex flex-wrap gap-1.5">{cats.map((c) => <button key={c} onClick={() => setCat(c)} className={`rounded-xl border px-3 py-1.5 text-sm transition ${cat === c ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{c}</button>)}</div>
          </div>
          <div>
            <p className="mb-1.5 text-sm font-medium">Prioridade</p>
            <div className="flex gap-1.5">{(["baixa", "média", "alta", "crítica"] as const).map((p) => <button key={p} onClick={() => setPri(p)} className={`flex-1 rounded-xl border py-1.5 text-sm capitalize transition ${pri === p ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>{p}</button>)}</div>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium">Descrição <span className="text-destructive">*</span></label>
            <textarea value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Descreva o problema com o máximo de detalhes. O que estava fazendo? O que aconteceu?" className="min-h-[88px] w-full resize-none rounded-xl border border-border bg-card p-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/10" />
          </div>
          {ctx && authUser && effectiveTenant && effectiveProduct && (
            <div className="rounded-xl bg-muted p-3 text-xs space-y-0.5">
              <p className="font-medium text-foreground mb-1">Contexto capturado automaticamente</p>
              <p>Tela atual: <b>{screenName}</b></p>
              <p>Usuário: <b>{authUser.name}</b> · {authUser.email}</p>
              <p>Tenant: <b>{effectiveTenant.name}</b> · Produto: <b>{effectiveProduct.name}</b></p>
            </div>
          )}
          <div className="flex items-center justify-between">
            <label className="flex cursor-pointer items-center gap-2 text-sm"><input type="checkbox" checked={ctx} onChange={(e) => setCtx(e.target.checked)} className="accent-primary" />Incluir contexto da tela</label>
            <input ref={fileInputRef} type="file" hidden onChange={handleFileSelected} />
            <button onClick={() => fileInputRef.current?.click()} disabled={attachmentUploading} className="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-xs transition hover:bg-muted disabled:cursor-not-allowed disabled:opacity-60">
              {attachmentUploading ? <><Loader2 size={12} className="animate-spin" />Enviando...</> : attachment ? <><CheckCircle2 size={12} className="text-primary" /><span className="max-w-[120px] truncate">{attachment.name}</span></> : <><Plus size={12} />Anexar arquivo</>}
            </button>
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-2"><Button onClick={onClose}>Cancelar</Button><Button primary onClick={handleSubmit} disabled={!desc.trim() || loading}>{loading ? <><Loader2 size={15} className="animate-spin" />Enviando...</> : "Enviar feedback"}</Button></div>
      </motion.div>
    </motion.div>
  );
}
