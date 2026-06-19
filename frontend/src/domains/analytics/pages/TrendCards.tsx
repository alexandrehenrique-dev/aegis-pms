import { useNavigate } from "react-router";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";

function TrendCard({ type, text, metric, severity }: { type: string; text: string; metric: string; severity: string }) {
  const navigate = useNavigate();
  const dest = severity === "positiva" ? "/analytics/content" : "/analytics";
  return (
    <Card onClick={() => navigate(dest)}>
      <div className="flex justify-between"><Badge tone={severity === "alta" ? "amber" : severity === "positiva" ? "green" : "blue"}>{type}</Badge><span className="font-mono text-xs text-muted-foreground">{metric}</span></div>
      <p className="mt-4 font-medium">{text}</p>
      <p className="mt-2 text-sm text-muted-foreground">Ação recomendada: {severity === "positiva" ? "ampliar aprendizado" : "investigar e corrigir"}.</p>
      <Button onClick={() => navigate(dest)}>Executar ação</Button>
    </Card>
  );
}

export function TrendCards() {
  const cards = [
    ["Crescimento", "Página Galeria gerou 42% mais interação esta semana.", "+42%", "positiva"],
    ["Queda", "Formulário de orçamento teve queda de conversão.", "-6%", "alta"],
    ["Estabilidade", "Tráfego direto permaneceu estável no período.", "0.4%", "neutra"],
    ["Anomalia", "Pico de visitas sem aumento proporcional de leads.", "3x", "alta"],
    ["Oportunidade", "Assets sem alt text podem prejudicar SEO.", "18 assets", "alta"],
    ["Atenção", "Conteúdo Sobre o Maestro não é atualizado há 45 dias.", "45 dias", "alta"],
  ];
  return (
    <>
      <PageHeader title="Trend Cards" module="Analytics" desc="Tendências e insights operacionais que geram ação." badge="Insights">
        <Button>Marcar revisado</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{cards.map((c) => <TrendCard key={c[0]} type={c[0]} text={c[1]} metric={c[2]} severity={c[3]} />)}</div>
    </>
  );
}
