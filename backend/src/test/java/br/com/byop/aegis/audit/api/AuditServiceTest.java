package br.com.byop.aegis.audit.api;

import br.com.byop.aegis.audit.context.AuditContext;
import br.com.byop.aegis.audit.context.AuditContextHolder;
import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import br.com.byop.aegis.audit.repository.AuditEventRepository;
import br.com.byop.aegis.audit.service.AuditRiskCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private AuditRiskCatalog riskCatalog;

    @Test
    void shouldRecordEventWithoutDiffWhenBeforeAndAfterAreAbsent() {
        AuditService service = new AuditService(auditEventRepository, riskCatalog, new ObjectMapper());
        UUID tenantId = UUID.randomUUID();
        when(riskCatalog.resolve("TENANT_CREATED")).thenReturn(AuditRisk.BAIXO);
        when(auditEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEvent saved = service.recordEvent(new AuditRecordCommand(
                tenantId, null, "subject-1", "TENANT_CREATED", "Tenant", tenantId.toString(), "ACME",
                null, null, null
        ));

        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getActorSubject()).isEqualTo("subject-1");
        assertThat(saved.getAction()).isEqualTo("TENANT_CREATED");
        assertThat(saved.getTargetLabel()).isEqualTo("ACME");
        assertThat(saved.getRisk()).isEqualTo(AuditRisk.BAIXO);
        assertThat(saved.getDiffJson()).isNull();
        verify(auditEventRepository).save(any());
    }

    @Test
    void shouldSerializeBeforeAndAfterIntoDiffJson() {
        AuditService service = new AuditService(auditEventRepository, riskCatalog, new ObjectMapper());
        when(riskCatalog.resolve("CONTENT_PUBLISHED")).thenReturn(AuditRisk.MEDIO);
        when(auditEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEvent saved = service.recordEvent(new AuditRecordCommand(
                UUID.randomUUID(), UUID.randomUUID(), "subject-1", "CONTENT_PUBLISHED", "Content",
                UUID.randomUUID().toString(), "Pagina inicial", "CONTENT",
                Map.of("status", "DRAFT"), Map.of("status", "PUBLISHED")
        ));

        assertThat(saved.getDiffJson()).contains("DRAFT").contains("PUBLISHED");
    }

    @Test
    void shouldSerializeDiffJsonWhenOnlyAfterIsPresent() {
        AuditService service = new AuditService(auditEventRepository, riskCatalog, new ObjectMapper());
        when(riskCatalog.resolve("CONTENT_CREATED")).thenReturn(AuditRisk.BAIXO);
        when(auditEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEvent saved = service.recordEvent(new AuditRecordCommand(
                UUID.randomUUID(), UUID.randomUUID(), "subject-1", "CONTENT_CREATED", "Content",
                UUID.randomUUID().toString(), "Pagina nova", "CONTENT",
                null, Map.of("status", "DRAFT")
        ));

        assertThat(saved.getDiffJson()).contains("DRAFT");
    }

    @Test
    void shouldEnrichEventWithRequestAuditContext() {
        AuditService service = new AuditService(auditEventRepository, riskCatalog, new ObjectMapper());
        when(riskCatalog.resolve("CONTENT_CREATED")).thenReturn(AuditRisk.BAIXO);
        when(auditEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AuditContextHolder.set(new AuditContext("trace-123", "127.0.0.1", "JUnit"));

        try {
            AuditEvent saved = service.recordEvent(new AuditRecordCommand(
                    UUID.randomUUID(), UUID.randomUUID(), "subject-1", "CONTENT_CREATED", "Content",
                    UUID.randomUUID().toString(), "Pagina nova", "CONTENT", null, null
            ));

            assertThat(saved.getTraceId()).isEqualTo("trace-123");
            assertThat(saved.getIp()).isEqualTo("127.0.0.1");
            assertThat(saved.getUserAgent()).isEqualTo("JUnit");
        } finally {
            AuditContextHolder.clear();
        }
    }

    @Test
    void shouldWrapSerializationFailureAsIllegalStateException() {
        ObjectMapper brokenObjectMapper = mock(ObjectMapper.class);
        when(brokenObjectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));
        AuditService service = new AuditService(auditEventRepository, riskCatalog, brokenObjectMapper);
        when(riskCatalog.resolve("TENANT_DELETED")).thenReturn(AuditRisk.ALTO);
        AuditRecordCommand command = new AuditRecordCommand(
                UUID.randomUUID(), null, "subject-1", "TENANT_DELETED", "Tenant",
                UUID.randomUUID().toString(), "ACME", null, Map.of("status", "ACTIVE"), null
        );

        assertThatThrownBy(() -> service.recordEvent(command)).isInstanceOf(IllegalStateException.class);
    }
}
