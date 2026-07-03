import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { ArrowLeft, CheckCircle2, Loader2, X } from "lucide-react";
import { Badge, Button, Field, SelectLike } from "../../../shared/components/Primitives";
import { fade } from "../../../shared/components/motion";
import { useAuth } from "../../../core/auth/useAuth";
import { tenantsService } from "../../../core/tenants/services/tenantsService";
import { productsService } from "../../products/services/productsService";
import { productAssignmentsService } from "../../users/services/productAssignmentsService";
import { usersService } from "../../users/services/usersService";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { PRODUCT_TYPE_MODULE_DEFAULTS } from "../../../core/products/moduleDefaults";
import { slugify } from "../../../shared/utils/slugify";
import { assignmentXorError, emailError, slugError, textLengthError } from "../../../shared/utils/validation";
import type { TenantOption } from "../../../shared/types";
import type { ProductSummary } from "../../products/contracts/responses";
import type { ProductAssignmentSummary } from "../../users/contracts/productAssignments";

const INITIAL_MODULES = (PRODUCT_TYPE_MODULE_DEFAULTS["Site Institucional"] ?? []).filter((m) => m.default).map((m) => m.key);

/** Catálogo de planos de tenant (mesmos valores usados em `tenants.mocks.ts`/`EditTenantModal`). */
const PLAN_OPTIONS = ["Starter", "Pro", "Enterprise"];

type Step = 1 | 2 | 3 | 4;

/**
 * Wizard "Criar Tenant -> Criar Produto -> Atribuir Usuário" como modal de
 * múltiplas etapas sobre /admin/tenants — nunca navega para outra rota, só
 * avança/recua o `step` interno. Substitui as antigas páginas roteadas
 * (CreateTenantForm/CreateProductForm-com-tenantId/AssignProductUserForm)
 * por pedido explícito: a tela de gestão de tenants é a única tela.
 */
export function CreateTenantWizardModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const navigate = useNavigate();
  const { selectTenant, selectProduct } = useAuth();
  const [step, setStep] = useState<Step>(1);
  const [saving, setSaving] = useState(false);

  const [tenant, setTenant] = useState<TenantOption | null>(null);
  const [product, setProduct] = useState<ProductSummary | null>(null);
  const [assignment, setAssignment] = useState<ProductAssignmentSummary | null>(null);

  // Step 1 fields
  const [tenantName, setTenantName] = useState("Novo Tenant");
  const [tenantSlug, setTenantSlug] = useState("novo-tenant");
  const [touchedTenantSlug, setTouchedTenantSlug] = useState(false);
  const [adminEmail, setAdminEmail] = useState("");
  const [plan, setPlan] = useState(PLAN_OPTIONS[0]);
  const [touched1, setTouched1] = useState<{ name?: boolean; slug?: boolean; email?: boolean }>({});

  const tenantNameErr = textLengthError(tenantName, 3, 100, "Nome do tenant");
  const tenantSlugErr = slugError(tenantSlug);
  const adminEmailErr = emailError(adminEmail);
  const step1HasErrors = !!tenantNameErr || !!tenantSlugErr || !!adminEmailErr;

  const handleTenantNameChange = (v: string) => {
    setTenantName(v);
    if (!touchedTenantSlug) setTenantSlug(slugify(v));
  };

  // Step 2 fields
  const [productName, setProductName] = useState("Novo Produto");
  const [productSlug, setProductSlug] = useState("novo-produto");
  const [touchedProductSlug, setTouchedProductSlug] = useState(false);
  const [touched2, setTouched2] = useState<{ name?: boolean; slug?: boolean }>({});
  const [productSlugTakenErr, setProductSlugTakenErr] = useState<string | undefined>();
  const [checkingProductSlug, setCheckingProductSlug] = useState(false);

  const productNameErr = textLengthError(productName, 3, 100, "Nome do produto");
  const productSlugErr = slugError(productSlug) || productSlugTakenErr;
  const step2HasErrors = !!productNameErr || !!productSlugErr || checkingProductSlug;

  const handleProductNameChange = (v: string) => {
    setProductName(v);
    if (!touchedProductSlug) setProductSlug(slugify(v));
  };

  const handleProductSlugBlur = async () => {
    setTouched2((t) => ({ ...t, slug: true }));
    setProductSlugTakenErr(undefined);
    if (slugError(productSlug)) return;
    setCheckingProductSlug(true);
    try {
      const available = await productsService.checkSlugAvailable(productSlug);
      if (!available) setProductSlugTakenErr("Este identificador já está em uso neste tenant.");
    } finally {
      setCheckingProductSlug(false);
    }
  };

  // Step 3 fields
  const { data: existingUsers } = useAsyncData(() => usersService.listUsers(), []);
  const [assignMode, setAssignMode] = useState<"existing" | "invite">("invite");
  const [selectedEmail, setSelectedEmail] = useState("");
  const [inviteName, setInviteName] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [touched3, setTouched3] = useState<{ inviteEmail?: boolean; xor?: boolean }>({});
  const [role, setRole] = useState("Editor");

  const inviteEmailErr = assignMode === "invite" ? emailError(inviteEmail) : undefined;
  const assignXorErr = assignmentXorError(selectedEmail, inviteEmail);

  const handleCreateTenant = async () => {
    setTouched1({ name: true, slug: true, email: true });
    if (step1HasErrors) return;
    setSaving(true);
    try {
      const created = await tenantsService.create({ name: tenantName, slug: tenantSlug, plan, initialAdminEmail: adminEmail });
      setTenant(created);
      setStep(2);
    } finally {
      setSaving(false);
    }
  };

  const handleCreateProduct = async () => {
    setTouched2({ name: true, slug: true });
    if (step2HasErrors) return;
    if (!tenant) return;
    setSaving(true);
    try {
      const created = await productsService.create({
        name: productName, slug: productSlug, type: "Site Institucional", language: "pt-BR",
        description: "Produto criado pelo wizard de onboarding do Super Admin.",
        initialModules: INITIAL_MODULES, tenantId: tenant.id, assetStorageStrategy: "local",
      });
      setProduct(created);
      setStep(3);
    } finally {
      setSaving(false);
    }
  };

  const handleAssignUser = async () => {
    setTouched3({ inviteEmail: assignMode === "invite", xor: true });
    if (assignXorErr) return;
    if (assignMode === "invite" && inviteEmailErr) return;
    if (!tenant || !product?.id) return;
    setSaving(true);
    try {
      const summary = await productAssignmentsService.assign({
        tenantId: tenant.id, productId: product.id,
        userId: assignMode === "existing" ? selectedEmail : undefined,
        inviteEmail: assignMode === "invite" ? inviteEmail : undefined,
        inviteName: assignMode === "invite" ? inviteName : undefined,
        role,
      });
      setAssignment(summary);
      setStep(4);
      onDone();
    } finally {
      setSaving(false);
    }
  };

  const canSubmitAssign = !assignXorErr && (assignMode === "existing" ? !!selectedEmail : !!inviteEmail.trim() && !!inviteName.trim() && !inviteEmailErr);

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={step < 4 ? onClose : undefined}>
      <motion.div {...fade} className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-5 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2"><h2 className="font-semibold">Criar Tenant</h2><Badge tone="violet">Passo {Math.min(step, 3)} de 3</Badge></div>
            <p className="mt-0.5 text-xs text-muted-foreground">Tenant → primeiro produto → usuário responsável.</p>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>

        {step === 1 && (
          <div className="space-y-3">
            <Field label="Nome do tenant" value={tenantName} onChange={handleTenantNameChange} onBlur={() => setTouched1((t) => ({ ...t, name: true }))} error={touched1.name ? tenantNameErr : undefined} />
            <Field label="Slug / identificador único" value={tenantSlug} onChange={(v) => { setTouchedTenantSlug(true); setTenantSlug(slugify(v)); }} onBlur={() => setTouched1((t) => ({ ...t, slug: true }))} error={touched1.slug ? tenantSlugErr : undefined} />
            <Field label="E-mail do Tenant Admin inicial" value={adminEmail} onChange={setAdminEmail} onBlur={() => setTouched1((t) => ({ ...t, email: true }))} error={touched1.email ? adminEmailErr : undefined} />
            <SelectLike label="Plano" value={plan} options={PLAN_OPTIONS} onChange={setPlan} />
          </div>
        )}

        {step === 2 && (
          <div className="space-y-3">
            <div className="rounded-lg bg-muted p-2.5 text-xs text-muted-foreground">Tenant: <b className="text-foreground">{tenant?.name}</b></div>
            <Field label="Nome do produto" value={productName} onChange={handleProductNameChange} onBlur={() => setTouched2((t) => ({ ...t, name: true }))} error={touched2.name ? productNameErr : undefined} />
            <Field label="Slug" value={productSlug} onChange={(v) => { setTouchedProductSlug(true); setProductSlugTakenErr(undefined); setProductSlug(slugify(v)); }} onBlur={handleProductSlugBlur} error={touched2.slug ? (checkingProductSlug ? "Verificando disponibilidade..." : productSlugErr) : undefined} />
          </div>
        )}

        {step === 3 && (
          <div className="space-y-3">
            <div className="rounded-lg bg-muted p-2.5 text-xs text-muted-foreground">Atribuir <b className="text-foreground">{product?.name}</b> em <b className="text-foreground">{tenant?.name}</b> a:</div>
            <div className="flex gap-1.5">
              <button onClick={() => setAssignMode("invite")} className={`rounded-xl border px-3 py-1.5 text-sm transition ${assignMode === "invite" ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>Convidar novo</button>
              <button onClick={() => setAssignMode("existing")} className={`rounded-xl border px-3 py-1.5 text-sm transition ${assignMode === "existing" ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}>Usuário existente</button>
            </div>
            {assignMode === "invite" ? (
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Nome" value={inviteName} onChange={setInviteName} />
                <Field label="Email" value={inviteEmail} onChange={setInviteEmail} onBlur={() => setTouched3((t) => ({ ...t, inviteEmail: true, xor: true }))} error={touched3.inviteEmail ? inviteEmailErr : undefined} />
              </div>
            ) : (
              <div className="max-h-40 space-y-1.5 overflow-y-auto">
                {existingUsers?.map((u) => (
                  <button key={u.email} onClick={() => { setSelectedEmail(u.email); setTouched3((t) => ({ ...t, xor: true })); }} className={`flex w-full items-center justify-between rounded-lg border p-2.5 text-left text-sm transition ${selectedEmail === u.email ? "border-primary bg-primary/5" : "border-border hover:bg-muted"}`}>
                    <span><b>{u.name}</b> <span className="text-muted-foreground">· {u.email}</span></span>
                    {selectedEmail === u.email && <CheckCircle2 size={14} className="text-primary" />}
                  </button>
                ))}
              </div>
            )}
            {touched3.xor && assignXorErr && <p className="text-xs text-destructive">{assignXorErr}</p>}
            <div className="flex flex-wrap gap-2">{["Editor", "Product Manager", "Viewer"].map((r) => <Badge key={r} tone={role === r ? "violet" : "neutral"}><button onClick={() => setRole(r)}>{r}</button></Badge>)}</div>
          </div>
        )}

        {step === 4 && assignment && (
          <div className="space-y-3 text-sm">
            <div className="mx-auto mb-2 grid h-12 w-12 place-items-center rounded-full bg-[#ede9fe]"><CheckCircle2 size={22} className="text-primary" /></div>
            <div className="flex justify-between"><span>Tenant</span><b>{tenant?.name}</b></div>
            <div className="flex justify-between"><span>Produto</span><b>{assignment.productName}</b></div>
            <div className="flex justify-between"><span>Usuário</span><b>{assignment.userName} · {assignment.userEmail}</b></div>
            <div className="flex justify-between"><span>Papel</span><b>{assignment.role}</b></div>
            <div className="flex justify-between"><span>Status</span><Badge tone={assignment.status === "convidado" ? "blue" : "green"}>{assignment.status}</Badge></div>
          </div>
        )}

        <div className="mt-5 flex justify-between gap-2">
          {step > 1 && step < 4 ? (
            <Button onClick={() => setStep((s) => (s - 1) as Step)}><ArrowLeft size={14} />Voltar</Button>
          ) : <span />}
          {step === 1 && <Button primary onClick={handleCreateTenant} disabled={saving || step1HasErrors}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Criando..." : "Criar tenant"}</Button>}
          {step === 2 && <Button primary onClick={handleCreateProduct} disabled={saving || step2HasErrors}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Criando..." : "Criar produto"}</Button>}
          {step === 3 && <Button primary onClick={handleAssignUser} disabled={saving || !canSubmitAssign}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Atribuindo..." : "Concluir"}</Button>}
          {step === 4 && (
            <div className="flex gap-2">
              <Button
                primary
                onClick={() => {
                  if (tenant) selectTenant(tenant);
                  if (product) selectProduct({ id: product.id ?? "", name: product.name, type: product.type, status: product.status, modules: product.modules });
                  onClose();
                  navigate(`/products/${product?.id}`);
                }}
              >
                Ir para o Produto
              </Button>
              <Button onClick={onClose}>Fechar</Button>
            </div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
}
