import { Badge } from "../../../shared/components/Primitives";

export function ContentStatusBadge({ status }: { status: string }) {
  return <Badge data-tour="content-status-badge" tone={status === "Published" ? "green" : status === "In Review" ? "amber" : status === "Draft" ? "blue" : "neutral"}>{status}</Badge>;
}
