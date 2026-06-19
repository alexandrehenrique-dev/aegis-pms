import { useState } from "react";
import { CheckCircle2 } from "lucide-react";
import { Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { PermissionImpactSummary } from "../components/PermissionImpactSummary";

export function InviteUserDrawer() {
  const [sent, setSent] = useState(false);
  const handleSend = () => {
    setSent(true);
    toast.success("Convite enviado!", { description: "João Alves receberá um email com instruções de acesso." });
  };
  return (
    <>
      <PageHeader title="Convidar Usuário" desc="Convide com papéis, produtos permitidos e resumo de risco." badge="Convite">
        <Button>Cancelar</Button>
        <Button primary onClick={handleSend} disabled={sent}>{sent ? <><CheckCircle2 size={15} />Enviado</> : "Enviar convite"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <div className="grid gap-3 md:grid-cols-2">
            <Field label="Nome" value="João Alves" />
            <Field label="Email" value="joao@byop.com" />
            <SelectLike label="Papel" value="Editor" />
            <SelectLike label="Produtos permitidos" value="Maestro Beton" />
            <SelectLike label="Módulos permitidos" value="Conteúdo, Assets, Forms" />
            <Field label="Mensagem opcional" value="Você foi convidado para operar conteúdo do produto." textarea />
          </div>
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Resumo de permissões</h2>
          <PermissionImpactSummary />
          <div className="mt-3 rounded-xl border border-[#D97706]/25 bg-[#FBF1DF] p-3 text-sm text-[#8A5A12]">Papéis administrativos aumentam risco operacional e geram auditoria.</div>
        </Card>
      </div>
    </>
  );
}
