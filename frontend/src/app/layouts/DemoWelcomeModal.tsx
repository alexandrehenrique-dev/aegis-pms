import { motion } from "motion/react";
import { fade } from "../../shared/components/Primitives";
import { AegisLogo } from "../../shared/components/AegisLogo";
import { CheckCircle2 } from "lucide-react";

export function DemoWelcomeModal({ onClose }: { onClose: () => void }) {
  const feats = [
    "Login → Tenant Select → Product Select → Dashboard",
    "9 módulos com tabs de navegação interna por módulo",
    "Sistema de permissões com 5 personas simuláveis",
    "Workflow editorial com drag & drop e modal de confirmação",
    "Knowledge Graph de entidades reais de negócio",
    "Auto-save, ConfirmDialog e estados globais reativos",
  ];
  return (
    <motion.div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
      <motion.div {...fade} className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-[0_24px_80px_rgba(0,0,0,0.2)]">
        <div className="mb-5 text-center">
          <AegisLogo size="md" className="mx-auto" />
          <h2 className="mt-3 font-semibold tracking-[-.02em]">Bem-vindo ao Aegis PMS</h2>
          <p className="mt-1 text-sm text-muted-foreground">Protótipo interativo · Sprint 19 · BYOP Platform</p>
        </div>
        <div className="mb-5 space-y-2">
          {feats.map((f) => <div key={f} className="flex items-start gap-2 text-sm"><CheckCircle2 size={13} className="mt-0.5 shrink-0 text-primary" /><span>{f}</span></div>)}
        </div>
        <div className="mb-5 rounded-xl bg-muted p-3 text-xs">
          <p className="mb-2 font-medium text-foreground">Atalhos do protótipo</p>
          {[["⌘K", "Busca global em tempo real"], ["Avatar → Simular perfil", "Trocar entre 5 personas"], ["Arrastar cards", "Workflow editorial DnD"], ["? no header", "Reportar problema / bug"]].map(([k, v]) => (
            <div key={k} className="flex justify-between py-0.5 text-muted-foreground"><span className="font-medium text-foreground">{k}</span><span>{v}</span></div>
          ))}
        </div>
        <div className="mb-3 rounded-xl border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
          <p className="font-medium text-foreground">Contas de demonstração</p>
          <p>admin@byop.io · pm@byop.io · editor@byop.io</p>
          <p>Senha: <span className="font-mono">senha123</span></p>
        </div>
        <button onClick={onClose} className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-2.5 text-sm font-medium text-primary-foreground transition hover:bg-primary/90 active:scale-[0.97] shadow-[0_4px_14px_rgba(124,58,237,.25)]">Começar a explorar →</button>
      </motion.div>
    </motion.div>
  );
}
