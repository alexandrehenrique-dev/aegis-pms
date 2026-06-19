export function UnsavedChangesBanner() {
  return <div className="mb-4 rounded-xl border border-[#D97706]/25 bg-[#FBF1DF] p-3 text-sm text-[#8A5A12]">Alterações não salvas · salvando/salvo/erro são refletidos neste banner operacional.</div>;
}

export function ConflictAlert() {
  return <div className="rounded-xl border border-destructive/20 bg-[#FDEBE8] p-3 text-sm text-destructive">Conflito de edição: outro editor alterou este conteúdo há 2 min. Revise antes de publicar.</div>;
}
