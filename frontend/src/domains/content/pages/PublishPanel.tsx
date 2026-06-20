import { useState } from "react";
import { AnimatePresence } from "motion/react";
import { AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { toast } from "../../../core/notifications/toast";
import { contentService } from "../services/contentService";

export function PublishPanel() {
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [confirmPublish, setConfirmPublish] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [scheduling, setScheduling] = useState(false);
  const [submittingReview, setSubmittingReview] = useState(false);

  const handleArchive = async () => {
    setArchiving(true);
    try {
      await contentService.archive();
      toast.success("Conteúdo arquivado.", { description: "Evento de auditoria registrado." });
      setConfirmArchive(false);
    } finally {
      setArchiving(false);
    }
  };
  const handlePublish = async () => {
    setPublishing(true);
    try {
      await contentService.publish();
      toast.success("Conteúdo publicado!", { description: "Maestro Beton · Página Home · v19" });
      setConfirmPublish(false);
    } finally {
      setPublishing(false);
    }
  };
  const handleSaveDraft = async () => {
    setSavingDraft(true);
    try {
      await contentService.saveDraft();
      toast.success("Rascunho salvo!");
    } finally {
      setSavingDraft(false);
    }
  };
  const handleSchedule = async () => {
    setScheduling(true);
    try {
      await contentService.schedulePublish();
      toast.success("Publicação agendada!");
    } finally {
      setScheduling(false);
    }
  };
  const handleSubmitForReview = async () => {
    setSubmittingReview(true);
    try {
      await contentService.submitForReview();
      toast.success("Enviado para revisão!");
    } finally {
      setSubmittingReview(false);
    }
  };

  return (
    <>
      <AnimatePresence>{confirmArchive && <ConfirmDialog title="Arquivar este conteúdo?" desc="O conteúdo será removido da listagem pública. Esta ação gera um evento de auditoria e não pode ser desfeita facilmente." onConfirm={handleArchive} onCancel={() => setConfirmArchive(false)} danger loading={archiving} />}</AnimatePresence>
      <AnimatePresence>{confirmPublish && <ConfirmDialog title="Publicar agora?" desc="Esta versão ficará visível publicamente em Maestro Beton. A equipe editorial será notificada." onConfirm={handlePublish} onCancel={() => setConfirmPublish(false)} loading={publishing} />}</AnimatePresence>
      <PageHeader title="Publish Panel" desc="Checklist e decisão de publicação para a versão atual." badge="Publicação">
        <Button onClick={handleSaveDraft} disabled={savingDraft}>{savingDraft && <Loader2 size={15} className="animate-spin" />}{savingDraft ? "Salvando..." : "Salvar rascunho"}</Button>
        <Button primary onClick={() => setConfirmPublish(true)}>Publicar agora</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Checklist pré-publicação</h2>
          {([["SEO completo", false], ["Traduções completas", false], ["Assets com alt text", false], ["Formulário vinculado", true]] as [string, boolean][]).map(([x, ok]) => (
            <div key={x} className="mb-2 flex items-center justify-between rounded-lg bg-muted p-3 text-sm"><span>{x}</span>{ok ? <CheckCircle2 className="text-primary" size={16} /> : <AlertTriangle className="text-[#b45309]" size={16} />}</div>
          ))}
          <div className="mt-4 rounded-xl border border-[#fef3c7] bg-[#fef3c7]/60 p-3 text-sm text-[#b45309]">Pendências detectadas. Revise antes de publicar.</div>
        </Card>
        <Card>
          <h2 className="text-lg font-semibold">Ações</h2>
          <p className="mt-2 text-sm text-muted-foreground">Publicar agora tornará esta versão visível no produto.</p>
          <div className="mt-4 space-y-2">
            <Button primary onClick={() => setConfirmPublish(true)}>Publicar agora</Button>
            <Button onClick={handleSchedule} disabled={scheduling}>{scheduling && <Loader2 size={15} className="animate-spin" />}{scheduling ? "Agendando..." : "Agendar publicação"}</Button>
            <Button onClick={handleSubmitForReview} disabled={submittingReview}>{submittingReview && <Loader2 size={15} className="animate-spin" />}{submittingReview ? "Enviando..." : "Enviar para revisão"}</Button>
            <Button onClick={() => setConfirmArchive(true)}>Arquivar</Button>
          </div>
        </Card>
      </div>
    </>
  );
}
