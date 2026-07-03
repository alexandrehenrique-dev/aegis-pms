import { forwardRef, type ButtonHTMLAttributes, type MouseEvent, type ReactNode } from "react";
import { motion } from "motion/react";
import { AlertTriangle, ChevronDown, Circle, Lock, Plus, Sparkles } from "lucide-react";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "./ui/dropdown-menu";
import { fade } from "./motion";

/**
 * forwardRef é necessário porque Radix (`PopoverTrigger asChild`,
 * `DropdownMenuTrigger asChild` etc.) clona este componente via `Slot` e
 * precisa anexar um ref real ao `<button>` — sem isso, React avisa "Function
 * components cannot be given refs" e o trigger não funciona corretamente.
 */
export const Button = forwardRef<HTMLButtonElement, { children: ReactNode; primary?: boolean } & ButtonHTMLAttributes<HTMLButtonElement>>(
  ({ children, primary = false, className = "", ...props }, ref) => (
    <button
      ref={ref}
      {...props}
      className={`inline-flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm transition duration-150 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50 active:scale-[0.97] active:opacity-90 ${primary ? "border-primary bg-primary text-primary-foreground shadow-[0_4px_14px_rgba(124,58,237,.25)] hover:bg-primary/90" : "border-border bg-card text-foreground hover:bg-muted"} ${className}`}
    >
      {children}
    </button>
  ),
);
Button.displayName = "Button";

export function SkeletonCard() {
  return (
    <div className="rounded-2xl border border-border bg-card p-4">
      <div className="mb-3 h-4 w-24 animate-pulse rounded-lg bg-muted" />
      <div className="h-7 w-16 animate-pulse rounded-lg bg-muted" />
      <div className="mt-2 h-3 w-32 animate-pulse rounded-lg bg-muted" />
    </div>
  );
}

export function SkeletonLines() {
  return (
    <div className="space-y-2">
      {[1, 2, 3, 4].map((i) => <div key={i} className="h-10 animate-pulse rounded-lg bg-muted" />)}
    </div>
  );
}

export function Badge({ children, tone = "neutral", "data-tour": dataTour }: { children: ReactNode; tone?: "neutral" | "green" | "blue" | "amber" | "red" | "violet"; "data-tour"?: string }) {
  const c = {
    green: "bg-[#dcfce7] text-[#15803d]",
    blue: "bg-[#dbeafe] text-[#1d4ed8]",
    amber: "bg-[#fef3c7] text-[#b45309]",
    red: "bg-[#fee2e2] text-[#dc2626]",
    violet: "bg-[#ede9fe] text-[#7c3aed]",
    neutral: "bg-muted text-muted-foreground",
  }[tone];
  return <span data-tour={dataTour} className={`rounded-full px-2 py-1 text-[11px] font-medium ${c}`}>{children}</span>;
}

export function Card({ children, className = "", onClick, onContextMenu, "data-tour": dataTour }: { children: ReactNode; className?: string; onClick?: () => void; onContextMenu?: (e: MouseEvent) => void; "data-tour"?: string }) {
  return (
    <motion.div
      {...fade}
      onClick={onClick}
      onContextMenu={onContextMenu}
      data-tour={dataTour}
      className={`rounded-2xl border border-border bg-card p-4 shadow-[0_1px_3px_rgba(0,0,0,0.04),0_4px_16px_rgba(0,0,0,0.04)] dark:shadow-none ${onClick ? "cursor-pointer hover:border-primary/30 hover:shadow-[0_2px_12px_rgba(124,58,237,0.08)] transition-shadow" : ""} ${className}`}
    >
      {children}
    </motion.div>
  );
}

export function KPIWidget({ label, value, detail, locked, error, onClick }: { label: string; value: string; detail: string; locked?: boolean; error?: boolean; onClick?: () => void }) {
  return (
    <Card onClick={!locked ? onClick : undefined}>
      <div className="flex items-start justify-between">
        <p className="text-sm text-muted-foreground">{label}</p>
        {locked ? <Lock size={16} /> : error ? <AlertTriangle size={16} className="text-destructive" /> : <Circle size={10} className="mt-1 fill-primary text-primary" />}
      </div>
      <p className="mt-3 text-2xl font-semibold tracking-[-.02em]">{locked ? "Restrito" : error ? "Parcial" : value}</p>
      <p className="mt-1 text-xs text-muted-foreground">{locked ? "Sem permissão para este widget" : error ? "Alguns dados não carregaram" : detail}</p>
    </Card>
  );
}

export function PermissionHint() {
  return (
    <div className="rounded-xl border border-border bg-muted/50 p-3 text-sm text-muted-foreground">
      <Lock size={16} className="mb-2" />
      Alguns indicadores dependem do perfil Tenant Admin ou Product Manager.
    </div>
  );
}

export function PartialErrorWidget() {
  return (
    <div className="rounded-xl border border-destructive/20 bg-[#FDEBE8]/50 p-3 text-sm">
      <AlertTriangle size={16} className="mb-2 text-destructive" />
      <b>Erro parcial:</b> dados de conversão indisponíveis. Os demais widgets continuam operacionais.
    </div>
  );
}

export type EmptyStateAction = { label: string; onClick: () => void };

/**
 * Sprint 15, Tarefa E.2 — antes, a versão não-`compact` sempre renderizava
 * dois botões hardcoded ("Criar produto"/"Ver documentação") sem `onClick`,
 * herdados por qualquer tela que não passasse título/descrição customizados
 * (ex.: lista de páginas vazia mostrava texto e ação de "produto"). Os
 * botões agora só aparecem quando o chamador passa `primaryAction`/
 * `secondaryAction` explicitamente — nenhuma ação implícita, nenhum botão
 * morto.
 */
export function EmptyState({ compact = false, title = "Nenhum item encontrado.", description = "Ajuste os filtros ou crie o primeiro item para este contexto.", primaryAction, secondaryAction }: {
  compact?: boolean; title?: string; description?: string; primaryAction?: EmptyStateAction; secondaryAction?: EmptyStateAction;
}) {
  return (
    <div className={`rounded-xl border border-dashed border-border bg-muted/40 text-center ${compact ? "p-3" : "p-8"}`}>
      <Sparkles className="mx-auto mb-2 text-primary" size={compact ? 18 : 28} />
      <p className="font-medium">{title}</p>
      <p className="mx-auto mt-1 max-w-md text-xs leading-5 text-muted-foreground">{description}</p>
      {!compact && (primaryAction || secondaryAction) && (
        <div className="mt-4 flex justify-center gap-2">
          {primaryAction && <Button primary onClick={primaryAction.onClick}><Plus size={15} />{primaryAction.label}</Button>}
          {secondaryAction && <Button onClick={secondaryAction.onClick}>{secondaryAction.label}</Button>}
        </div>
      )}
    </div>
  );
}

export function PageHeader({ title, desc, badge = "BYOP", titleBadge, children, "data-tour": dataTour }: { title: string; desc: string; module?: string; badge?: string; titleBadge?: ReactNode; children?: ReactNode; "data-tour"?: string }) {
  return (
    <motion.header {...fade} data-tour={dataTour} className="mb-6 flex flex-col gap-3 border-b border-border pb-5 md:flex-row md:items-end md:justify-between">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h1 className="text-2xl font-semibold tracking-[-.02em] md:text-3xl">{title}</h1>
          <Badge tone="violet">{badge}</Badge>
          {titleBadge}
        </div>
        <p className="mt-1.5 max-w-2xl text-sm leading-6 text-muted-foreground">{desc}</p>
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </motion.header>
  );
}

export function Field({ label, value, onChange, onBlur, textarea = false, locked = false, type = "text", error }: { label: string; value: string; onChange?: (v: string) => void; onBlur?: () => void; textarea?: boolean; locked?: boolean; type?: "text" | "date" | "datetime-local" | "password"; error?: string }) {
  const className = `w-full rounded-lg border bg-card p-3 text-sm outline-primary ${error ? "border-destructive" : "border-border"} ${textarea ? "min-h-28" : ""} ${locked ? "cursor-not-allowed opacity-60" : ""}`;
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium">{label}</span>
      {textarea ? (
        <textarea className={className} value={value} onChange={(e) => onChange?.(e.target.value)} onBlur={onBlur} disabled={locked} />
      ) : (
        <input type={type} className={className} value={value} onChange={(e) => onChange?.(e.target.value)} onBlur={onBlur} disabled={locked} />
      )}
      {error && <span className="mt-1 block text-xs text-destructive">{error}</span>}
    </label>
  );
}

export function SelectLike({ label, value, options, onChange, locked = false }: { label: string; value: string; options?: string[]; onChange?: (v: string) => void; locked?: boolean }) {
  if (locked) {
    return (
      <label className="block">
        <span className="mb-1 block text-sm font-medium">{label}</span>
        <div className="flex w-full cursor-not-allowed items-center justify-between rounded-lg border border-border bg-card p-3 text-sm opacity-60">
          <span>{value}</span>
        </div>
      </label>
    );
  }

  if (!options || options.length === 0) {
    return (
      <label className="block">
        <span className="mb-1 block text-sm font-medium">{label}</span>
        <button className="flex w-full items-center justify-between rounded-lg border border-border bg-card p-3 text-sm">
          <span>{value}</span>
          <ChevronDown size={15} />
        </button>
      </label>
    );
  }

  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium">{label}</span>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button type="button" className="flex w-full items-center justify-between rounded-lg border border-border bg-card p-3 text-sm">
            <span>{value}</span>
            <ChevronDown size={15} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-[var(--radix-dropdown-menu-trigger-width)]">
          {options.map((opt) => (
            <DropdownMenuItem key={opt} onSelect={() => onChange?.(opt)}>{opt}</DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </label>
  );
}
