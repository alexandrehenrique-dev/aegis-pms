package br.com.byop.aegis.audit.api;

import br.com.byop.aegis.audit.context.AuditContext;
import br.com.byop.aegis.audit.context.AuditContextHolder;
import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.repository.AuditEventRepository;
import br.com.byop.aegis.audit.service.AuditRiskCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ponto unico de gravacao da trilha de auditoria do Aegis. Qualquer dominio
 * que precisa auditar uma acao injeta este servico e chama
 * {@link #recordEvent} — nenhum outro componente do sistema grava
 * diretamente na tabela {@code audit_events}. Risco e {@code diffJson} sao
 * calculados aqui, nunca pelo chamador, para que a logica de auditoria nunca
 * fique duplicada.
 */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditRiskCatalog riskCatalog;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, AuditRiskCatalog riskCatalog,
                        ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.riskCatalog = riskCatalog;
        this.objectMapper = objectMapper;
    }

    /**
     * Grava um novo evento de auditoria, calculando o risco a partir da acao
     * e serializando {@code before}/{@code after} em {@code diffJson}.
     * {@code traceId}/{@code ip}/{@code userAgent} sao obtidos do contexto
     * tecnico capturado pelo interceptor HTTP, mantendo services de dominio
     * independentes de {@code HttpServletRequest}.
     *
     * @param command dados do evento a gravar
     * @return o evento persistido
     */
    @Transactional
    public AuditEvent recordEvent(AuditRecordCommand command) {
        AuditContext context = AuditContextHolder.current().orElse(null);
        AuditEvent event = new AuditEvent(new AuditEvent.Creation(
                command.tenantId(),
                command.productId(),
                command.actorSubject(),
                command.action(),
                command.targetType(),
                command.targetId(),
                command.targetLabel(),
                command.module(),
                riskCatalog.resolve(command.action()),
                writeDiffJson(command.before(), command.after()),
                context == null ? null : context.traceId(),
                context == null ? null : context.ip(),
                context == null ? null : context.userAgent()
        ));
        return auditEventRepository.save(event);
    }

    private String writeDiffJson(Map<String, Object> before, Map<String, Object> after) {
        if (before == null && after == null) {
            return null;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("before", before);
        envelope.put("after", after);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize audit diff", ex);
        }
    }
}
