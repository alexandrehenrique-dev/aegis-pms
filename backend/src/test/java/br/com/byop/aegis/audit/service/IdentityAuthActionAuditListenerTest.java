package br.com.byop.aegis.audit.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.identity.api.IdentityAuthActionAuditEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IdentityAuthActionAuditListenerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final AuditService auditService = mock(AuditService.class);
    private final IdentityAuthActionAuditListener listener = new IdentityAuthActionAuditListener(auditService);

    @Test
    void shouldRecordIdentityAuthActionAsAuditEvent() {
        listener.onIdentityAuthAction(new IdentityAuthActionAuditEvent(
                TENANT_ID,
                "user-id",
                "PASSWORD_RESET_COMPLETED",
                "user-id",
                "User Name",
                Map.of("tokenId", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        ));

        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        AuditRecordCommand command = captor.getValue();
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
        assertThat(command.productId()).isNull();
        assertThat(command.actorSubject()).isEqualTo("user-id");
        assertThat(command.action()).isEqualTo("PASSWORD_RESET_COMPLETED");
        assertThat(command.targetType()).isEqualTo("User");
        assertThat(command.targetId()).isEqualTo("user-id");
        assertThat(command.targetLabel()).isEqualTo("User Name");
        assertThat(command.module()).isNull();
        assertThat(command.before()).isNull();
        assertThat(command.after()).containsEntry("tokenId", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }
}
