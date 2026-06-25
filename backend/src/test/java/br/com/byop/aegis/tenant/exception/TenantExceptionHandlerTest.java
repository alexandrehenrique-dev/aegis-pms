package br.com.byop.aegis.tenant.exception;

import br.com.byop.aegis.core.CoreErrorResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantExceptionHandlerTest {

    private final TenantExceptionHandler handler = new TenantExceptionHandler();

    @Test
    void shouldReturnTenantAlreadyExistsError() {
        CoreErrorResponse response = handler.handleTenantAlreadyExists();

        assertThat(response.error()).isEqualTo("TENANT_ALREADY_EXISTS");
    }

    @Test
    void shouldReturnTenantNotFoundError() {
        CoreErrorResponse response = handler.handleTenantNotFound();

        assertThat(response.error()).isEqualTo("TENANT_NOT_FOUND");
    }
}
