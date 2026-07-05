import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { usersService } from "../services/usersService";
import { PermissionImpactSummary } from "../components/PermissionImpactSummary";
import { emailError, textLengthError } from "../../../shared/utils/validation";
import { useAuth } from "../../../core/auth/useAuth";

const ROLES = ["Editor", "Viewer", "Product Manager", "Tenant Admin"];
const MODULES = ["Conteúdo, Assets, Forms", "Conteúdo, Analytics", "Todos os módulos"];

export function InviteUserDrawer() {
  const navigate = useNavigate();
  const { tenantProducts, effectiveTenant } = useAuth();

  // Constrói lista de produtos dinâmica a partir do tenant atual.
  // "Todos os produtos" é sempre adicionado como última opção.
  const productOptions = useMemo(() => {
    const names = tenantProducts.map((p) => p.name);
    return names.length > 0 ? [...names, "Todos os produtos"] : ["Todos os produtos"];
  }, [tenantProducts]);

  const [sending, setSending] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState(ROLES[0]);
  const [allowedProducts, setAllowedProducts] = useState(productOptions[0]);
  const [allowedModules, setAllowedModules] = useState(MODULES[0]);
  const [touched, setTouched] = useState<{ name?: boolean; email?: boolean }>({});

  const nameErr = textLengthError(name, 2, 100, "Nome");
  const emailErr = emailError(email);
  const hasErrors = !!nameErr || !!emailErr;

  const handleSend = async () => {
    setTouched({ name: true, email: true });
    if (hasErrors) return;
    setSending(true);
    try {
      await usersService.invite({ name, email, role, allowedProducts }, effectiveTenant?.id);
      toast.success("Convite enviado!", { description: `${name} receberá um email com instruções de acesso.` });
      navigate("/users");
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <PageHeader title="Convidar Usuário" desc="Convide com papéis, produtos permitidos e resumo de risco." badge="Convite">
        <Button onClick={() => navigate("/users")}>Cancelar</Button>
        <Button primary onClick={handleSend} disabled={sending || hasErrors}>{sending && <Loader2 size={15} className="animate-spin" />}{sending ? "Enviando..." : "Enviar convite"}</Button>
      </PageHeader>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <Card>
          <div className="grid gap-3 md:grid-cols-2">
            <Field label="Nome" value={name} onChange={setName} onBlur={() => setTouched((t) => ({ ...t, name: true }))} error={touched.name ? nameErr : undefined} />
            <Field label="Email" value={email} onChange={setEmail} onBlur={() => setTouched((t) => ({ ...t, email: true }))} error={touched.email ? emailErr : undefined} />
            <SelectLike label="Papel" value={role} options={ROLES} onChange={setRole} />
            <SelectLike label="Produtos permitidos" value={allowedProducts} options={productOptions} onChange={setAllowedProducts} />
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
