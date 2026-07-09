import { useState, useCallback } from "react";
import { Loader2, Plus, Trash2, X } from "lucide-react";
import { Button, Card, EmptyState, Field, PartialErrorWidget, SkeletonLines } from "../../../shared/components/Primitives";
import { ModalShell } from "../../../shared/components/ModalShell";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { productAssignmentsService } from "../../users/services/productAssignmentsService";
import { usersService } from "../../users/services/usersService";
import { toast } from "../../../core/notifications/toast";
import type { ProductAssignmentSummary } from "../../users/contracts/productAssignments";
import type { UserSummary } from "../../users/contracts/responses";

// ─── Constantes ──────────────────────────────────────────────────────────────

const ROLES: { value: string; label: string; desc: string }[] = [
  { value: "PRODUCT_MANAGER", label: "Product Manager", desc: "Dono do produto — recebe emails de exportação e tem controle total." },
  { value: "EDITOR",          label: "Editor",          desc: "Edita conteúdo, assets e pages. Não pode excluir o produto." },
  { value: "VIEWER",          label: "Visualizador",    desc: "Acesso somente-leitura a todo o produto." },
];

const ROLE_LABELS: Record<string, string> = {
  PRODUCT_MANAGER: "Product Manager",
  EDITOR: "Editor",
  VIEWER: "Visualizador",
};

const ROLE_TONES: Record<string, string> = {
  PRODUCT_MANAGER: "bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-300",
  EDITOR:          "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300",
  VIEWER:          "bg-muted text-muted-foreground",
};

// ─── Badges ──────────────────────────────────────────────────────────────────

function RoleBadge({ role }: { role: string }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${ROLE_TONES[role] ?? ROLE_TONES.VIEWER}`}>
      {ROLE_LABELS[role] ?? role}
    </span>
  );
}

function StatusBadge({ status }: { status: string }) {
  const isInvited = status === "convidado" || status === "INVITED" || status === "PENDING";
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs ${isInvited ? "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300" : "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300"}`}>
      {isInvited ? "Convidado" : "Ativo"}
    </span>
  );
}

// ─── Modal Adicionar Membro ───────────────────────────────────────────────────

function AddMemberModal({
  productId,
  tenantId,
  existingSubjects,
  onClose,
  onAdded,
}: {
  productId: string;
  tenantId: string;
  existingSubjects: Set<string>;
  onClose: () => void;
  onAdded: (a: ProductAssignmentSummary) => void;
}) {
  const [email, setEmail] = useState("");
  const [inviteName, setInviteName] = useState("");
  const [role, setRole] = useState("PRODUCT_MANAGER");
  const [saving, setSaving] = useState(false);

  const { data: tenantUsers, loading } = useAsyncData(
    () => usersService.listUsers(tenantId),
    [tenantId],
  );

  // Detecção em tempo real: se o email digitado bate com algum usuário do tenant,
  // o fluxo é de NOTIFICAÇÃO (não de convite com senha).
  const emailTrimmed = email.trim().toLowerCase();
  const detectedUser = emailTrimmed
    ? (tenantUsers ?? []).find(
        (u: UserSummary) => u.email?.toLowerCase() === emailTrimmed,
      ) ?? null
    : null;

  const isExistingAlready = detectedUser
    ? existingSubjects.has(detectedUser.userId) || existingSubjects.has(detectedUser.name)
    : false;
  const inviteNameRequired = email.trim() !== "" && !detectedUser;
  const inviteNameInvalid = inviteNameRequired && !/^\S+\s+\S+/.test(inviteName.trim());

  const canSubmit =
    !saving &&
    role !== "" &&
    email.trim() !== "" &&
    !isExistingAlready &&
    !inviteNameInvalid;

  const handleSubmit = async () => {
    setSaving(true);
    try {
      const result = await productAssignmentsService.assign({
        tenantId,
        productId,
        // Se usuário detectado → usar userId para garantir caminho de notificação
        userId: detectedUser ? detectedUser.userId : undefined,
        inviteEmail: detectedUser ? undefined : email.trim(),
        inviteName: detectedUser ? undefined : (inviteName.trim() || undefined),
        role,
      });
      onAdded(result);

      const emailType = (result as ProductAssignmentSummary & { _emailType?: string })._emailType;
      if (emailType === "invite") {
        toast.success("Convite de cadastro enviado!", {
          description: `${email.trim()} receberá um link para definir senha e acessar o produto.`,
        });
      } else {
        toast.success("Membro adicionado!", {
          description: `${detectedUser?.name ?? email.trim()} foi notificado como ${ROLE_LABELS[role]}.`,
        });
      }
      onClose();
    } catch {
      toast.error("Erro ao adicionar membro", { description: "Tente novamente." });
    } finally {
      setSaving(false);
    }
  };

  // Botão label dinâmico
  const submitLabel = saving
    ? "Adicionando..."
    : detectedUser
      ? "Adicionar membro"
      : email.trim()
        ? "Enviar convite"
        : "Adicionar";

  return (
    <ModalShell onClose={onClose} maxWidthClassName="max-w-lg">
      <div className="mb-4 flex items-start justify-between">
        <h2 className="font-semibold">Adicionar membro ao produto</h2>
        <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
      </div>

      <div className="space-y-4">
        {/* Campo de e-mail único com detecção automática */}
        <div>
          {loading ? (
            <SkeletonLines />
          ) : (
            <>
              <Field label="E-mail" value={email} onChange={setEmail} type="text" />

              {/* Banner de detecção */}
              {email.trim() !== "" && (
                <div className={`mt-2 rounded-xl border p-3 text-sm ${
                  isExistingAlready
                    ? "border-destructive/30 bg-destructive/5 text-destructive"
                    : detectedUser
                      ? "border-green-200 bg-green-50 text-green-800 dark:border-green-800 dark:bg-green-900/20 dark:text-green-300"
                      : "border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-800 dark:bg-amber-900/20 dark:text-amber-300"
                }`}>
                  {isExistingAlready
                    ? `${detectedUser?.name ?? email.trim()} já está atribuído a este produto.`
                    : detectedUser
                      ? `✓ Usuário existente (${detectedUser.name}) — será notificado por e-mail sobre a nova atribuição.`
                      : "Novo usuário — receberá convite de cadastro com link para definir senha."}
                </div>
              )}

              {/* Nome: só para novos usuários */}
              {email.trim() !== "" && !detectedUser && (
                <div className="mt-3">
                  <Field label="Nome e sobrenome" value={inviteName} onChange={setInviteName} error={inviteNameInvalid ? "Informe nome e sobrenome (ex.: Alexandre Henrique)." : undefined} />
                </div>
              )}
            </>
          )}
        </div>

        {/* Seleção de papel */}
        <div>
          <p className="mb-2 text-sm font-medium">Papel no produto</p>
          <div className="space-y-2">
            {ROLES.map((r) => (
              <button
                key={r.value}
                onClick={() => setRole(r.value)}
                className={`flex w-full items-start gap-3 rounded-xl border p-3 text-left text-sm transition ${role === r.value ? "border-primary bg-primary/5" : "border-border hover:bg-muted"}`}
              >
                <span className={`mt-0.5 h-4 w-4 shrink-0 rounded-full border-2 ${role === r.value ? "border-primary bg-primary" : "border-muted-foreground"}`} />
                <span>
                  <span className="font-medium">{r.label}</span>
                  <span className="mt-0.5 block text-xs text-muted-foreground">{r.desc}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-5 flex justify-end gap-2">
        <Button onClick={onClose}>Cancelar</Button>
        <Button primary onClick={handleSubmit} disabled={!canSubmit}>
          {saving && <Loader2 size={15} className="animate-spin" />}
          {submitLabel}
        </Button>
      </div>
    </ModalShell>
  );
}

// ─── Linha de assignment ──────────────────────────────────────────────────────

function AssignmentRow({
  a,
  onRemove,
  removing,
}: {
  a: ProductAssignmentSummary;
  onRemove: () => void;
  removing: boolean;
}) {
  const initials = (a.userName || a.userEmail || "?").slice(0, 2).toUpperCase();
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold">
        {initials}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium text-sm">{a.userName || "—"}</p>
        <p className="truncate text-xs text-muted-foreground">{a.userEmail || a.userSubject}</p>
      </div>
      <RoleBadge role={a.role} />
      <StatusBadge status={a.status} />
      <button
        onClick={onRemove}
        disabled={removing}
        title="Remover do produto"
        className="ml-1 rounded-lg p-1.5 text-muted-foreground transition hover:bg-red-100 hover:text-red-600 disabled:opacity-40"
      >
        {removing ? <Loader2 size={15} className="animate-spin" /> : <Trash2 size={15} />}
      </button>
    </div>
  );
}

// ─── Painel principal ─────────────────────────────────────────────────────────

export function ProductTeamPanel({ productId, tenantId }: { productId: string; tenantId: string }) {
  const [showAddModal, setShowAddModal] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);
  // Estado local derivado do fetch — permite mutações sem refetch
  const [localAssignments, setLocalAssignments] = useState<ProductAssignmentSummary[] | null>(null);

  const fetchAssignments = useCallback(
    () => productAssignmentsService.listForProduct(productId),
    [productId],
  );
  const { data: fetched, loading, error } = useAsyncData(fetchAssignments, [productId]);

  // Sincroniza uma única vez quando o fetch termina e ainda não há estado local
  if (!loading && !error && fetched !== null && localAssignments === null) {
    setLocalAssignments(fetched);
  }

  const assignments = localAssignments ?? fetched ?? [];
  const existingSubjects = new Set(assignments.map((a) => a.userSubject ?? a.userName));

  const handleRemove = async (a: ProductAssignmentSummary) => {
    const subject = a.userSubject ?? a.userName;
    setRemovingId(subject);
    try {
      await productAssignmentsService.remove(productId, subject);
      setLocalAssignments((prev) => (prev ?? []).filter((x) => (x.userSubject ?? x.userName) !== subject));
      toast.success("Membro removido", { description: `${a.userName} foi removido do produto.` });
    } catch {
      toast.error("Erro ao remover membro");
    } finally {
      setRemovingId(null);
    }
  };

  const handleAdded = (a: ProductAssignmentSummary) => {
    setLocalAssignments((prev) => [...(prev ?? []), a]);
  };

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  return (
    <>
      <Card>
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold">Equipe do produto</h2>
            <p className="mt-0.5 text-sm text-muted-foreground">
              Usuários com acesso a este produto e seus papéis. Product Managers recebem notificações de exportação.
            </p>
          </div>
          <Button primary onClick={() => setShowAddModal(true)}>
            <Plus size={15} /> Adicionar membro
          </Button>
        </div>

        {(assignments ?? []).length === 0 ? (
          <EmptyState
            compact
            title="Nenhum membro atribuído"
            description="Adicione o primeiro Product Manager ou colaborador."
            primaryAction={{ label: "Adicionar membro", onClick: () => setShowAddModal(true) }}
          />
        ) : (
          <div className="space-y-2">
            {(assignments ?? []).map((a) => {
              const key = a.id ?? `${a.userSubject ?? a.userName}-${a.role}`;
              const subject = a.userSubject ?? a.userName;
              return (
                <AssignmentRow
                  key={key}
                  a={a}
                  onRemove={() => handleRemove(a)}
                  removing={removingId === subject}
                />
              );
            })}
          </div>
        )}

        {/* Legenda de papéis */}
        <div className="mt-5 rounded-xl border border-dashed border-border p-3">
          <p className="mb-2 text-xs font-medium text-muted-foreground">Papéis disponíveis</p>
          <div className="flex flex-wrap gap-3">
            {ROLES.map((r) => (
              <div key={r.value} className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <RoleBadge role={r.value} />
                <span>— {r.desc}</span>
              </div>
            ))}
          </div>
        </div>
      </Card>

      {showAddModal && (
        <AddMemberModal
          productId={productId}
          tenantId={tenantId}
          existingSubjects={existingSubjects}
          onClose={() => setShowAddModal(false)}
          onAdded={handleAdded}
        />
      )}
    </>
  );
}
