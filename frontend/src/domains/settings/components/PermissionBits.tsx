export function PermissionCell({ state, onToggle }: { state: string; onToggle?: () => void }) {
  return (
    <button
      onClick={state !== "locked" ? onToggle : undefined}
      disabled={state === "locked"}
      className={`grid h-8 w-8 place-items-center rounded-lg border ${state === "on" ? "border-primary bg-primary text-white" : state === "locked" ? "cursor-not-allowed border-border bg-muted text-muted-foreground" : "border-border bg-card hover:bg-muted"}`}
    >
      {state === "on" ? "✓" : state === "locked" ? "—" : ""}
    </button>
  );
}

export function PermissionImpactSummary() {
  return (
    <div className="space-y-2 text-sm">
      <div className="rounded-lg bg-muted p-3">Pode criar e editar conteúdo.</div>
      <div className="rounded-lg bg-muted p-3">Não pode publicar, excluir ou alterar permissões.</div>
      <div className="rounded-lg bg-muted p-3">Alterações geram evento de auditoria.</div>
    </div>
  );
}

export function UnsavedPermissionChanges() {
  return <div className="mb-4 rounded-xl border border-[#D97706]/25 bg-[#FBF1DF] p-3 text-sm text-[#8A5A12]">Alterações de permissão não salvas. Revise o impacto antes de aplicar.</div>;
}
