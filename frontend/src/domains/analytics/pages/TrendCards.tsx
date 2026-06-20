import { useState } from "react";
import { useNavigate } from "react-router";
import { CheckCircle2 } from "lucide-react";
import { Badge, Button, Card, PageHeader } from "../../../shared/components/Primitives";
import { toast } from "../../../core/notifications/toast";
import { analyticsService } from "../services/analyticsService";

function TrendCard({ type, text, metric, severity, reviewed, onReview }: { type: string; text: string; metric: string; severity: string; reviewed: boolean; onReview: () => void }) {
  const navigate = useNavigate();
  const dest = severity === "positiva" ? "/analytics/content" : "/analytics";
  return (
    <Card onClick={() => navigate(dest)}>
      <div className="flex justify-between"><Badge tone={severity === "alta" ? "amber" : severity === "positiva" ? "green" : "blue"}>{type}</Badge><span className="font-mono text-xs text-muted-foreground">{metric}</span></div>
      <p className="mt-4 font-medium">{text}</p>
      <p className="mt-2 text-sm text-muted-foreground">Ação recomendada: {severity === "positiva" ? "ampliar aprendizado" : "investigar e corrigir"}.</p>
      <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
        <Button onClick={() => navigate(dest)}>Executar ação</Button>
        <Button onClick={onReview} disabled={reviewed}>{reviewed ? <><CheckCircle2 size={14} />Revisado</> : "Marcar revisado"}</Button>
      </div>
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
  const [reviewed, setReviewed] = useState<Set<string>>(new Set());

  const handleReview = async (label: string) => {
    await analyticsService.markTrendReviewed(label);
    setReviewed((prev) => new Set(prev).add(label));
    toast.success("Tendência marcada como revisada.");
  };

  return (
    <>
      <PageHeader title="Trend Cards" module="Analytics" desc="Tendências e insights operacionais que geram ação." badge="Insights">
        <Button onClick={() => cards.forEach((c) => handleReview(c[0]))}>Marcar revisado</Button>
      </PageHeader>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {cards.map((c) => <TrendCard key={c[0]} type={c[0]} text={c[1]} metric={c[2]} severity={c[3]} reviewed={reviewed.has(c[0])} onReview={() => handleReview(c[0])} />)}
      </div>
    </>
  );
}
