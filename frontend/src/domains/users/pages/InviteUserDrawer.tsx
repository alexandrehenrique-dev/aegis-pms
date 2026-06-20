import { useState } from "react";
import { CheckCircle2, Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { usersService } from "../services/usersService";
import { PermissionImpactSummary } from "../components/PermissionImpactSummary";

const ROLES = ["Editor", "Viewer", "Product Manager", "Tenant Admin"];
const PRODUCTS = ["Maestro Beton", "Conecta Talentos", "Todos os produtos"];
const MODULES = ["Conteúdo, Assets, Forms", "Conteúdo, Analytics", "Todos os módulos"];

export function InviteUserDrawer() {
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const [name, setName] = useState("João Alves");
  const [email, setEmail] = useState("joao@byop.com");
  const [role, setRole] = useState(ROLES[0]);
  const [allowedProducts, setAllowedProducts] = useState(PRODUCTS[0]);
  const [allowedModules, setAllowedModules] = useState(MODULES[0]);

  const handleSend = async () => {
    setSending(true);
    try {
      await usersService.invite({ name, email, role, allowedProducts });
      setSent(true);
      toast.success("Convite enviado!", { description: `${name} receberá um email com instruções de acesso.` });
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <PageHeader title="Convidar Usuário" desc="Convide com papéis, produtos permitidos e resumo de risco." badge="Convite">
        <Button>Cancelar</Button>
        <Button primary onClick={handleSend} disabled={sending || sent}>{sending && <Loader2 size={15} className="animate-spin" />}{sent ? <><CheckCircle2 size={15} />Enviado</> : sending ? "Enviando..." : "Enviar convite"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <div className="grid gap-3 md:grid-cols-2">
            <Field label="Nome" value={name} onChange={setName} />
            <Field label="Email" value={email} onChange={setEmail} />
            <SelectLike label="Papel" value={role} options={ROLES} onChange={setRole} />
            <SelectLike label="Produtos permitidos" value={allowedProducts} options={PRODUCTS} onChange={setAllowedProducts} />
            <SelectLike label="Módulos permitidos" value={allowedModules} options={MODULES} onChange={setAllowedModules} />
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
