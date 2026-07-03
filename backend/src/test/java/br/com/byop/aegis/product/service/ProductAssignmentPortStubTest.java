package br.com.byop.aegis.product.service;

import br.com.byop.aegis.identity.api.IdentityUser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProductAssignmentPortStubTest {

    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID productId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void inviteStubShouldReturnExplicitInviteSubject() {
        StubProductAssignmentInvitePort port = new StubProductAssignmentInvitePort();

        IdentityUser user = port.invite(tenantId, productId, "guest@byop.dev");

        assertThat(user.id()).isEqualTo("invite:guest@byop.dev");
        assertThat(user.email()).isEqualTo("guest@byop.dev");
    }

    @Test
    void inviteStubShouldUseDefaultContextOverload() {
        StubProductAssignmentInvitePort port = new StubProductAssignmentInvitePort();

        IdentityUser user = port.invite(tenantId, productId, "Aegis", "guest@byop.dev", "EDITOR", "Admin");

        assertThat(user.id()).isEqualTo("invite:guest@byop.dev");
        assertThat(user.email()).isEqualTo("guest@byop.dev");
    }

    @Test
    void emailStubShouldAcceptNotificationCall() {
        StubProductAssignmentEmailPort port = new StubProductAssignmentEmailPort();
        ProductAssignmentEmailCommand command = emailCommand();

        assertThatCode(() -> port.notifyAssignment(command))
                .doesNotThrowAnyException();
    }

    @Test
    void emailStubShouldAcceptRevocationCall() {
        StubProductAssignmentEmailPort port = new StubProductAssignmentEmailPort();
        ProductAssignmentEmailCommand command = emailCommand();

        assertThatCode(() -> port.notifyRevocation(command))
                .doesNotThrowAnyException();
    }

    @Test
    void notificationStubShouldAcceptNotificationCall() {
        StubProductAssignmentNotificationPort port = new StubProductAssignmentNotificationPort();

        assertThatCode(() -> port.notifyAssignment(tenantId, productId, "invite:guest@byop.dev"))
                .doesNotThrowAnyException();
    }

    @Test
    void notificationStubShouldAcceptRevocationCall() {
        StubProductAssignmentNotificationPort port = new StubProductAssignmentNotificationPort();

        assertThatCode(() -> port.notifyRevocation(tenantId, productId, "invite:guest@byop.dev"))
                .doesNotThrowAnyException();
    }

    private ProductAssignmentEmailCommand emailCommand() {
        return new ProductAssignmentEmailCommand(
                tenantId,
                "Tenant Aegis",
                productId,
                "Aegis PMS",
                "guest@byop.dev",
                "Guest"
        );
    }
}
