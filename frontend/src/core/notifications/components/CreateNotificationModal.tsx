import { useState } from "react";
import { motion } from "motion/react";
import { CheckCircle2, Loader2, X } from "lucide-react";
import { Badge, Button, Field, SelectLike } from "../../../shared/components/Primitives";
import { fade } from "../../../shared/components/motion";
import { MarkdownField } from "../../../shared/components/MarkdownField";
import { mockUsers } from "../../auth/mocks/users";
import { notificationsService } from "../services/notificationsService";
import type { NotificationType, PresentationMode } from "../contracts/notification";
import type { NotificationRecipients } from "../contracts/requests";
import type { TenantOption } from "../../../shared/types";

const TYPE_LABELS: Record<NotificationType, string> = {
  ONBOARDING: "Onboarding",
  FEATURE: "Novidade",
  WARNING: "Aviso",
  MAINTENANCE: "Manutenção",
  GENERAL: "Geral",
};

const PRESENTATION_LABELS: Record<PresentationMode, string> = {
  MODAL_ONCE: "Aparecer como modal na primeira vez",
  BELL_ONLY: "Só no sino",
};

function labelToKey<T extends string>(labels: Record<T, string>, label: string): T {
  return (Object.entries(labels) as [T, string][]).find(([, l]) => l === label)![0];
}

type RecipientMode = "all" | "tenant" | "users";

const PLATFORM_USERS = Object.values(mockUsers).map((m) => m.user);

/**
 * Criar notificação direcionada (Sprint 14, Tarefa D) — acionado a partir de
 * `TenantSelectScreen`, só para Super Admin. A ação não é por tenant (pode
 * alcançar usuários de tenants diferentes), por isso vive como botão de
 * topo, ao lado de "Criar Tenant", não dentro do menu de contexto de um
 * tenant específico.
 */
export function CreateNotificationModal({ tenants, onClose, onCreated }: {
  tenants: TenantOption[]; onClose: () => void; onCreated: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState("");
  const [bodyMarkdown, setBodyMarkdown] = useState("");
  const [type, setType] = useState<NotificationType>("GENERAL");
  const [presentationMode, setPresentationMode] = useState<PresentationMode>("BELL_ONLY");
  const [recipientMode, setRecipientMode] = useState<RecipientMode>("all");
  const [tenantId, setTenantId] = useState(tenants[0]?.id ?? "");
  const [userQuery, setUserQuery] = useState("");
  const [selectedUserIds, setSelectedUserIds] = useState<Set<string>>(new Set());

  const toggleUser = (id: string) => {
    setSelectedUserIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const filteredUsers = PLATFORM_USERS.filter((u) => (u.name + u.email).toLowerCase().includes(userQuery.toLowerCase()));

  const canSubmit = title.trim() && bodyMarkdown.trim()
    && (recipientMode !== "tenant" || tenantId)
    && (recipientMode !== "users" || selectedUserIds.size > 0);

  const handleCreate = async () => {
    setSaving(true);
    try {
      const recipients: NotificationRecipients = recipientMode === "all" ? { mode: "all" }
        : recipientMode === "tenant" ? { mode: "tenant", tenantId }
        : { mode: "users", userIds: Array.from(selectedUserIds) };

      await notificationsService.create({ title, bodyMarkdown, type, presentationMode, recipients });
      onCreated();
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-[3px] p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
      <motion.div {...fade} className="flex max-h-[85vh] w-full max-w-lg flex-col rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]" onClick={(e) => e.stopPropagation()}>
        <div className="mb-5 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2"><h2 className="font-semibold">Criar Notificação</h2><Badge tone="violet">Super Admin</Badge></div>
            <p className="mt-0.5 text-xs text-muted-foreground">Envie um aviso para todos, um tenant específico, ou usuários escolhidos.</p>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 transition hover:bg-muted"><X size={17} /></button>
        </div>

        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto">
          <Field label="Título" value={title} onChange={setTitle} />
          <MarkdownField label="Corpo (markdown)" value={bodyMarkdown} onChange={setBodyMarkdown} />
          <div className="grid gap-3 sm:grid-cols-2">
            <SelectLike label="Tipo" value={TYPE_LABELS[type]} options={Object.values(TYPE_LABELS)} onChange={(v) => setType(labelToKey(TYPE_LABELS, v))} />
            <SelectLike label="Apresentação" value={PRESENTATION_LABELS[presentationMode]} options={Object.values(PRESENTATION_LABELS)} onChange={(v) => setPresentationMode(labelToKey(PRESENTATION_LABELS, v))} />
          </div>

          <div>
            <p className="mb-2 text-sm font-medium">Destinatários</p>
            <div className="flex flex-wrap gap-1.5">
              {([["all", "Todos os usuários"], ["tenant", "Usuários de um tenant"], ["users", "Usuários específicos"]] as [RecipientMode, string][]).map(([mode, label]) => (
                <button
                  key={mode}
                  onClick={() => setRecipientMode(mode)}
                  className={`rounded-xl border px-3 py-1.5 text-sm transition ${recipientMode === mode ? "border-primary bg-primary/5 text-primary font-medium" : "border-border bg-card text-muted-foreground hover:bg-muted"}`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          {recipientMode === "tenant" && (
            <SelectLike label="Tenant" value={tenants.find((t) => t.id === tenantId)?.name ?? "Selecione"} options={tenants.map((t) => t.name)} onChange={(name) => setTenantId(tenants.find((t) => t.name === name)?.id ?? "")} />
          )}

          {recipientMode === "users" && (
            <div className="space-y-2">
              <input value={userQuery} onChange={(e) => setUserQuery(e.target.value)} placeholder="Buscar por nome ou e-mail..." className="w-full rounded-lg border border-border bg-card p-3 text-sm outline-primary" />
              <div className="max-h-40 space-y-1.5 overflow-y-auto">
                {filteredUsers.map((u) => (
                  <button key={u.id} onClick={() => toggleUser(u.id)} className={`flex w-full items-center justify-between rounded-lg border p-2.5 text-left text-sm transition ${selectedUserIds.has(u.id) ? "border-primary bg-primary/5" : "border-border hover:bg-muted"}`}>
                    <span><b>{u.name}</b> <span className="text-muted-foreground">· {u.email}</span></span>
                    {selectedUserIds.has(u.id) && <CheckCircle2 size={14} className="text-primary" />}
                  </button>
                ))}
                {filteredUsers.length === 0 && <p className="text-sm text-muted-foreground">Nenhum usuário encontrado.</p>}
              </div>
            </div>
          )}
        </div>

        <div className="mt-5 flex justify-end gap-2 border-t border-border pt-4">
          <Button onClick={onClose}>Cancelar</Button>
          <Button primary onClick={handleCreate} disabled={saving || !canSubmit}>
            {saving ? <Loader2 size={15} className="animate-spin" /> : <CheckCircle2 size={15} />}
            {saving ? "Criando..." : "Criar notificação"}
          </Button>
        </div>
      </motion.div>
    </motion.div>
  );
}
