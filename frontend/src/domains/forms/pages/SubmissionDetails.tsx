import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Badge, Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../../shared/components/ui/dialog";
import { LeadStatusBadge } from "../components/FormBadges";
import { FormsTimeline } from "../components/FormsTimeline";
import { toast } from "../../../core/notifications/toast";
import { formsService } from "../services/formsService";

const OWNERS = ["Marina Costa", "João Alves", "Camila Rocha", "Pedro Lima"];

function UTMCard() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Origem / UTM</h2>
      {[["source", "google"], ["medium", "cpc"], ["campaign", "maestro-junho"], ["content", "hero-form"]].map((x) => (
        <div key={x[0]} className="mb-2 flex justify-between rounded-lg bg-muted p-2 text-sm"><span>{x[0]}</span><b>{x[1]}</b></div>
      ))}
    </Card>
  );
}

export function SubmissionDetails() {
  const email = "camila@studio.com";
  const [assignOpen, setAssignOpen] = useState(false);
  const [assignee, setAssignee] = useState("Marina Costa");
  const [assigning, setAssigning] = useState(false);
  const [qualifying, setQualifying] = useState(false);
  const [qualified, setQualified] = useState(false);
  const [notes, setNotes] = useState("Responder ainda hoje e solicitar briefing.");

  const handleAssign = async () => {
    setAssigning(true);
    try {
      await formsService.assignSubmissions([email], assignee);
      toast.success("Lead atribuído!", { description: `${assignee} agora é responsável por este lead.` });
      setAssignOpen(false);
    } finally {
      setAssigning(false);
    }
  };

  const handleQualify = async () => {
    setQualifying(true);
    try {
      await formsService.markQualified(email);
      setQualified(true);
      toast.success("Lead marcado como qualificado!");
    } finally {
      setQualifying(false);
    }
  };

  return (
    <>
      <Dialog open={assignOpen} onOpenChange={setAssignOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Atribuir lead</DialogTitle></DialogHeader>
          <SelectLike label="Responsável" value={assignee} options={OWNERS} onChange={setAssignee} />
          <DialogFooter>
            <Button onClick={() => setAssignOpen(false)}>Cancelar</Button>
            <Button primary onClick={handleAssign} disabled={assigning}>{assigning && <Loader2 size={15} className="animate-spin" />}{assigning ? "Atribuindo..." : "Confirmar"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <PageHeader title="Camila Rocha" module="Forms" desc="Detalhe da submissão com contexto de lead, origem, timeline e qualificação." badge="Lead Novo">
        <Button onClick={() => setAssignOpen(true)}>Atribuir</Button>
        <Button primary onClick={handleQualify} disabled={qualifying || qualified}>{qualifying && <Loader2 size={15} className="animate-spin" />}{qualified ? "Qualificado" : qualifying ? "Marcando..." : "Marcar qualificado"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Dados enviados</h2>
            {[["Nome", "Camila Rocha"], ["Email", "camila@studio.com"], ["Telefone", "+55 11 98888-1212"], ["Mensagem", "Gostaria de orçamento para evento corporativo em agosto."]].map((x) => (
              <div key={x[0]} className="mb-2 rounded-lg bg-muted p-3 text-sm"><b>{x[0]}</b><p>{x[1]}</p></div>
            ))}
          </Card>
          <Card><h2 className="mb-3 text-lg font-semibold">Timeline</h2><FormsTimeline /></Card>
        </div>
        <div className="space-y-4">
          <Card>
            <h2 className="mb-3 text-lg font-semibold">Operação do lead</h2>
            <LeadStatusBadge status={qualified ? "Qualificado" : "Novo"} />
            <div className="mt-3 flex flex-wrap gap-1">{["orçamento", "corporativo", "alto-fit"].map((t) => <Badge key={t} tone="blue">{t}</Badge>)}</div>
            <Field label="Observações" value={notes} onChange={setNotes} textarea />
            <SelectLike label="Responsável" value={assignee} options={OWNERS} onChange={setAssignee} />
          </Card>
          <UTMCard />
        </div>
      </div>
    </>
  );
}
