import { Button, Card } from "../../../shared/components/Primitives";

export function AssetBrokenUsageAlert() {
  return <div className="mt-3 rounded-xl border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">Antes de arquivar ou substituir, revise impactos: este asset está em uso público.</div>;
}

export function AssetUsagePanel() {
  return (
    <Card>
      <h2 className="mb-3 text-lg font-semibold">Uso no sistema</h2>
      {["Página Home → Bloco Hero", "Página Sobre → Galeria", "SEO → OG Image", "Preview público → Hero principal"].map((u, i) => (
        <div key={u} className="mb-2 flex items-center justify-between rounded-xl border border-border p-3 text-sm">
          <div><b>{u}</b><p className="text-muted-foreground">Módulo {i === 2 ? "SEO" : "Conteúdo"} · ativo</p></div>
          <Button>Abrir recurso</Button>
        </div>
      ))}
      <AssetBrokenUsageAlert />
    </Card>
  );
}
