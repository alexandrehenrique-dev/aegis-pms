package br.com.byop.aegis.identity.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthActionTokenTest {

    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");

    @Test
    void shouldCreatePendingTokenAndManageLifecycle() {
        AuthActionToken token = new AuthActionToken(
                "keycloak-id",
                "user@byop.dev",
                "User",
                AuthActionType.INVITE,
                NOW,
                NOW.plusSeconds(60)
        );
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        token.addInviteContext(tenantId, "BYOP", "[\"Aegis\"]", null, "EDITOR", "Admin", null);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getKeycloakId()).isEqualTo("keycloak-id");
        assertThat(token.getUserEmail()).isEqualTo("user@byop.dev");
        assertThat(token.getUserName()).isEqualTo("User");
        assertThat(token.getType()).isEqualTo(AuthActionType.INVITE);
        assertThat(token.getStatus()).isEqualTo(AuthActionStatus.PENDING);
        assertThat(token.getTenantId()).isEqualTo(tenantId);
        assertThat(token.getTenantName()).isEqualTo("BYOP");
        assertThat(token.getProductNames()).isEqualTo("[\"Aegis\"]");
        assertThat(token.getRole()).isEqualTo("EDITOR");
        assertThat(token.getInviterName()).isEqualTo("Admin");
        assertThat(token.getInviteMessage()).isNull();
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(token.isExpired(NOW.plusSeconds(59))).isFalse();
        assertThat(token.isExpired(NOW.plusSeconds(60))).isTrue();

        token.markUsed(NOW.plusSeconds(10));

        assertThat(token.getStatus()).isEqualTo(AuthActionStatus.USED);
        assertThat(token.getUsedAt()).isEqualTo(NOW.plusSeconds(10));

        token.markExpired();

        assertThat(token.getStatus()).isEqualTo(AuthActionStatus.EXPIRED);
    }

    @Test
    void shouldInitializeNullPersistenceDefaults() {
        AuthActionToken token = new AuthActionToken(
                "keycloak-id",
                "user@byop.dev",
                "User",
                AuthActionType.INVITE,
                NOW,
                NOW.plusSeconds(60)
        );
        org.springframework.test.util.ReflectionTestUtils.setField(token, "createdAt", null);
        org.springframework.test.util.ReflectionTestUtils.setField(token, "status", null);

        token.prePersist();

        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getStatus()).isEqualTo(AuthActionStatus.PENDING);
    }
}
