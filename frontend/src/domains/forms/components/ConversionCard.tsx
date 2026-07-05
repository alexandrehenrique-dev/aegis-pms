import { Card } from "../../../shared/components/Primitives";

export function ConversionCard({ value = "—" }: { value?: string }) {
  const numericValue = Number.parseFloat(value.replace("%", "").replace(",", "."));
  const width = Number.isNaN(numericValue) ? 0 : Math.max(0, Math.min(100, numericValue * 10));
  return (
    <Card>
      <h2 className="mb-2 text-lg font-semibold">Conversão operacional</h2>
      <p className="text-3xl font-semibold tracking-[-.02em]">{value}</p>
      <p className="mt-1 text-sm text-muted-foreground">Média dos formulários publicados no produto.</p>
      <div className="mt-4 h-2 rounded-full bg-muted"><div className="h-2 rounded-full bg-primary" style={{ width: `${width}%` }} /></div>
    </Card>
  );
}
