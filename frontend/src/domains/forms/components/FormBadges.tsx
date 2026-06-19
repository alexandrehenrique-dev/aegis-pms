import { Badge } from "../../../shared/components/Primitives";

export function LeadStatusBadge({ status }: { status: string }) {
  return <Badge tone={status === "Convertido" || status === "Qualificado" ? "green" : status === "Em análise" ? "amber" : status === "Novo" ? "blue" : "neutral"}>{status}</Badge>;
}

export function FormStatusBadge({ status }: { status: string }) {
  return <Badge tone={status === "ativo" ? "green" : status === "rascunho" ? "blue" : status === "arquivado" ? "neutral" : "amber"}>{status}</Badge>;
}
