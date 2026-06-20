import { useState } from "react";
import { AnimatePresence } from "motion/react";
import { Loader2 } from "lucide-react";
import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConfirmDialog } from "../../../shared/components/ConfirmDialog";
import { ConflictAlert } from "../../../shared/components/Banners";
import { PermissionCell, PermissionImpactSummary, UnsavedPermissionChanges } from "../components/PermissionBits";
import { toast } from "../../../core/notifications/toast";
import { settingsService } from "../services/settingsService";

const MODULES = ["Conteúdo", "Assets", "Forms", "Analytics", "Knowledge Graph", "Users", "Settings", "Audit"];
const COLS = ["Visualizar", "Criar", "Editar", "Publicar", "Arquivar", "Excluir", "Exportar", "Administrar"];

function buildDefaultMatrix(): string[][] {
  return MODULES.map((_, ri) => COLS.map((_, ci) => (ri > 4 && ci > 1 ? "locked" : ci < 3 ? "on" : "off")));
}

export function PermissionMatrixView() {
  const [matrix, setMatrix] = useState<string[][]>(buildDefaultMatrix);
  const [confirmRestore, setConfirmRestore] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [saving, setSaving] = useState(false);

  const toggleCell = (ri: number, ci: number) => {
    setMatrix((prev) => prev.map((row, r) => (r !== ri ? row : row.map((c, ci2) => (ci2 !== ci ? c : c === "on" ? "off" : "on")))));
  };

  const handleRestore = async () => {
    setRestoring(true);
    try {
      await settingsService.restoreDefaultPermissions();
      setMatrix(buildDefaultMatrix());
      toast.success("Permissões restauradas ao padrão.");
      setConfirmRestore(false);
    } finally {
      setRestoring(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await settingsService.savePermissions();
      toast.success("Permissões salvas!");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <AnimatePresence>
        {confirmRestore && <ConfirmDialog title="Restaurar permissões padrão?" desc="Todas as alterações não salvas nesta matriz serão perdidas." danger loading={restoring} onConfirm={handleRestore} onCancel={() => setConfirmRestore(false)} />}
      </AnimatePresence>
      <PageHeader title="Permission Matrix" module="Permissions" desc="Matriz crítica por módulo, ação e papel." badge="Editor">
        <Button onClick={() => setConfirmRestore(true)}>Restaurar padrão</Button>
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar permissões"}</Button>
      </PageHeader>
      <UnsavedPermissionChanges />
      <div className="grid gap-4 xl:grid-cols-[1fr_320px]">
        <Card>
          <div className="overflow-auto">
            <table className="min-w-[820px] w-full text-left text-sm">
              <thead><tr><th className="p-2">Módulo</th>{COLS.map((c) => <th key={c} className="p-2 text-xs text-muted-foreground">{c}</th>)}</tr></thead>
              <tbody>
                {MODULES.map((m, ri) => (
                  <tr key={m} className="border-t border-border">
                    <td className="p-2 font-medium">{m}</td>
                    {COLS.map((c, ci) => <td key={`${m}-${c}`} className="p-2"><PermissionCell state={matrix[ri][ci]} onToggle={() => toggleCell(ri, ci)} /></td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Resumo de impacto</h2>
          <PermissionImpactSummary />
          <ConflictAlert />
        </Card>
      </div>
    </>
  );
}
