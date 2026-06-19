import type { ReactNode } from "react";

export function PermGate({ allowed, children, fallback }: { allowed: boolean; children: ReactNode; fallback?: ReactNode }) {
  if (!allowed) return fallback ? <>{fallback}</> : null;
  return <>{children}</>;
}
