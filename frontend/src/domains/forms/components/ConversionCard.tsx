import { Card } from "../../../shared/components/Primitives";

export function ConversionCard() {
  return (
    <Card>
      <h2 className="mb-2 text-lg font-semibold">Conversão operacional</h2>
      <p className="text-3xl font-semibold tracking-[-.02em]">7.4%</p>
      <p className="mt-1 text-sm text-muted-foreground">Média dos formulários publicados no produto.</p>
      <div className="mt-4 h-2 rounded-full bg-muted"><div className="h-2 w-[74%] rounded-full bg-primary" /></div>
    </Card>
  );
}
