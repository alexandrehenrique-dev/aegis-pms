package br.com.byop.aegis.api.me;

import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MeResponseMapperTest {

    private final MeResponseMapper mapper = new MeResponseMapper();

    @Test
    void shouldMapAuthenticatedUserToMeResponseWithSuperAdminRole() {
        AuthenticatedUser user = userWithAuthorities("ROLE_SUPER_ADMIN");

        MeResponse response = mapper.toResponse(user);

        assertThat(response.subject()).isEqualTo("subject-123");
        assertThat(response.email()).isEqualTo("loki@byop.dev");
        assertThat(response.username()).isEqualTo("loki");
        assertThat(response.name()).isEqualTo("Loki");
        assertThat(response.role()).isEqualTo("super_admin");
    }

    @Test
    void shouldResolveTenantAdminRole() {
        assertThat(mapper.toResponse(userWithAuthorities("ROLE_TENANT_ADMIN")).role())
                .isEqualTo("tenant_admin");
    }

    @Test
    void shouldResolveProductManagerRole() {
        assertThat(mapper.toResponse(userWithAuthorities("ROLE_PRODUCT_MANAGER")).role())
                .isEqualTo("product_manager");
    }

    @Test
    void shouldResolveEditorRole() {
        assertThat(mapper.toResponse(userWithAuthorities("ROLE_EDITOR")).role())
                .isEqualTo("editor");
    }

    @Test
    void shouldResolveViewerRole() {
        assertThat(mapper.toResponse(userWithAuthorities("ROLE_VIEWER")).role())
                .isEqualTo("viewer");
    }

    @Test
    void shouldUseHighestPriorityRoleWhenMultipleRolesArePresent() {
        AuthenticatedUser user = userWithAuthorities("ROLE_VIEWER", "ROLE_EDITOR", "ROLE_SUPER_ADMIN");

        MeResponse response = mapper.toResponse(user);

        assertThat(response.role()).isEqualTo("super_admin");
    }

    @Test
    void shouldFallbackToViewerWhenNoKnownRoleIsPresent() {
        AuthenticatedUser user = userWithAuthorities("ROLE_UNKNOWN");

        MeResponse response = mapper.toResponse(user);

        assertThat(response.role()).isEqualTo("viewer");
    }

    private AuthenticatedUser userWithAuthorities(String... authorities) {
        return new AuthenticatedUser(
                "subject-123",
                "loki@byop.dev",
                "loki",
                "Loki",
                Set.of(authorities)
        );
    }
}