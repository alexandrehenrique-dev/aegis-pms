package br.com.byop.aegis.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreExceptionHandlerTest {

    @Test
    void shouldReturnGenericValidationError() {
        CoreExceptionHandler handler = new CoreExceptionHandler();

        CoreErrorResponse response = handler.handleValidationFailure();

        assertThat(response.error()).isEqualTo("REQUEST_VALIDATION_FAILED");
        assertThat(response.moduleKey()).isNull();
        assertThat(response.requires()).isNull();
    }
}
