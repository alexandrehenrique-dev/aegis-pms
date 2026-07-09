import { Paperclip } from "lucide-react";
import { Badge, Button } from "../../../shared/components/Primitives";
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle, DrawerDescription, DrawerFooter } from "../../../shared/components/ui/drawer";
import { resolveBaseUrl } from "../../../shared/services/apiClient";
import type { FeedbackStatus, FeedbackSummary } from "../../../core/notifications/contracts/feedback";

/**
 * Drawer de detalhe do feedback (Sprint 23, Seção B.3) — modelo
 * `EventsManagerDrawer` (vaul, `direction="right"`, controlado por
 * `open`/`onOpenChange`), já que `ContentDetailDrawer` citado no artefato da
 * sprint não existe neste código-fonte.
 */
export function FeedbackDetailDrawer({ feedback, tenantName, onOpenChange, onStatusChange }: {
  feedback: FeedbackSummary | null;
  tenantName: string;
  onOpenChange: (open: boolean) => void;
  onStatusChange: (feedbackId: string, status: FeedbackStatus) => void;
}) {
  const attachmentFilename = feedback?.attachmentAssetId ? `feedback-${feedback.id}-${feedback.attachmentAssetId}` : undefined;
  return (
    <Drawer open={feedback !== null} onOpenChange={onOpenChange} direction="right">
      <DrawerContent className="max-h-[100dvh] w-full sm:max-w-2xl">
        {feedback && (
          <>
            <DrawerHeader>
              <DrawerTitle className="font-mono">{feedback.id} · {feedback.category} · {feedback.priority}</DrawerTitle>
              <DrawerDescription>Detalhe do feedback recebido.</DrawerDescription>
            </DrawerHeader>
            <div className="flex-1 space-y-4 overflow-auto px-4 pb-4 text-sm">
              <div>
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Descrição</p>
                <p>{feedback.description}</p>
              </div>
              <div className="space-y-0.5">
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Contexto</p>
                <p>Tela: <b>{feedback.screenName ?? "—"}</b></p>
                <p>Usuário: <b>{feedback.createdBySubject}</b></p>
                <p>
                  Tenant: <b>{tenantName || feedback.tenantId}</b>
                  {feedback.productId && <> · Produto: <b>{feedback.productId}</b></>}
                </p>
                <p>Enviado em: <b>{new Date(feedback.createdAt).toLocaleString("pt-BR")}</b></p>
              </div>
              <div>
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Anexo</p>
                {feedback.attachmentAssetId ? (
                  <a
                    href={`${resolveBaseUrl()}/assets/${feedback.attachmentAssetId}/download`}
                    download={attachmentFilename}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 text-primary hover:underline"
                  >
                    <Paperclip size={14} />Baixar anexo
                  </a>
                ) : (
                  <p className="text-muted-foreground">Nenhum anexo.</p>
                )}
              </div>
            </div>
            <DrawerFooter>
              {feedback.status === "aberto" && (
                <Button onClick={() => onStatusChange(feedback.id, "em_analise")}>Marcar como em análise</Button>
              )}
              {feedback.status !== "resolvido" ? (
                <Button primary onClick={() => onStatusChange(feedback.id, "resolvido")}>Marcar como resolvido</Button>
              ) : (
                <Badge tone="green">Resolvido</Badge>
              )}
            </DrawerFooter>
          </>
        )}
      </DrawerContent>
    </Drawer>
  );
}
