import { Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { ConflictAlert } from "../../../shared/components/Banners";
import { PermissionCell, PermissionImpactSummary, UnsavedPermissionChanges } from "../components/PermissionBits";

export function PermissionMatrixView() {
  const mods = ["Conteúdo", "Assets", "Forms", "Analytics", "Knowledge Graph", "Users", "Settings", "Audit"];
  const cols = ["Visualizar", "Criar", "Editar", "Publicar", "Arquivar", "Excluir", "Exportar", "Administrar"];
  return (
    <>
      <PageHeader title="Permission Matrix" module="Permissions" desc="Matriz crítica por módulo, ação e papel." badge="Editor">
        <Button>Restaurar padrão</Button>
        <Button primary>Salvar permissões</Button>
      </PageHeader>
      <UnsavedPermissionChanges />
      <div className="grid gap-4 xl:grid-cols-[1fr_320px]">
        <Card>
          <div className="overflow-auto">
            <table className="min-w-[820px] w-full text-left text-sm">
              <thead><tr><th className="p-2">Módulo</th>{cols.map((c) => <th key={c} className="p-2 text-xs text-muted-foreground">{c}</th>)}</tr></thead>
              <tbody>
                {mods.map((m, ri) => (
                  <tr key={m} className="border-t border-border">
                    <td className="p-2 font-medium">{m}</td>
                    {cols.map((c, ci) => <td key={`${m}-${c}`} className="p-2"><PermissionCell state={ri > 4 && ci > 1 ? "locked" : ci < 3 ? "on" : "off"} /></td>)}
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
