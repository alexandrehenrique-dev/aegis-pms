import { Badge } from "./Primitives";
import type { ProductStatus } from "../types";

export function ProductStatusBadge({ status }: { status: ProductStatus }) {
  return (
    <Badge tone={status === "Ativo" ? "green" : status === "Pendente" ? "amber" : status === "Arquivado" ? "neutral" : "blue"}>
      {status}
    </Badge>
  );
}
