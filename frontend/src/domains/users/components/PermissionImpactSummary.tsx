export function PermissionImpactSummary() {
  return (
    <div className="space-y-2 text-sm">
      <div className="rounded-lg bg-muted p-3">Pode criar e editar conteúdo.</div>
      <div className="rounded-lg bg-muted p-3">Não pode publicar, excluir ou alterar permissões.</div>
      <div className="rounded-lg bg-muted p-3">Alterações geram evento de auditoria.</div>
    </div>
  );
}
