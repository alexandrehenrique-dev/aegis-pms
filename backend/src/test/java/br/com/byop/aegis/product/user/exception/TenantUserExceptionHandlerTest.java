package br.com.byop.aegis.product.user.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantUserExceptionHandlerTest {

    private final TenantUserExceptionHandler handler = new TenantUserExceptionHandler();

    @Test
    void shouldMapTenantUserErrors() {
        assertThat(handler.handleTenantUserAlreadyExists().error()).isEqualTo("USER_ALREADY_EXISTS_IN_TENANT");
        assertThat(handler.handleTenantUserNotFound().error()).isEqualTo("USER_NOT_FOUND");
        assertThat(handler.handleInvalidTenantUserOperation().error()).isEqualTo("INVALID_USER_OPERATION");
        assertThat(handler.handleLastTenantAdmin().error()).isEqualTo("LAST_TENANT_ADMIN");
    }

    @Test
    void shouldInstantiateExceptions() {
        assertThat(new TenantUserAlreadyExistsException()).isInstanceOf(RuntimeException.class);
        assertThat(new TenantUserNotFoundException()).isInstanceOf(RuntimeException.class);
        assertThat(new LastTenantAdminException()).isInstanceOf(RuntimeException.class);
        assertThat(new InvalidTenantUserOperationException("invalid")).hasMessage("invalid");
    }
}
