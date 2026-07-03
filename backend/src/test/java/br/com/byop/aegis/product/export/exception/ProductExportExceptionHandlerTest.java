package br.com.byop.aegis.product.export.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductExportExceptionHandlerTest {

    private final ProductExportExceptionHandler handler = new ProductExportExceptionHandler();

    @Test
    void shouldMapExportErrors() {
        assertThat(handler.handleInvalidConfirmation().error()).isEqualTo("INVALID_PRODUCT_DELETE_CONFIRMATION");
        assertThat(handler.handleForbiddenDelete().error()).isEqualTo("PRODUCT_DELETE_FORBIDDEN");
        assertThat(handler.handleExportAlreadyInProgress().error()).isEqualTo("EXPORT_ALREADY_IN_PROGRESS");
        assertThat(handler.handleDownloadNotFound().error()).isEqualTo("EXPORT_DOWNLOAD_NOT_FOUND");
        assertThat(handler.handleExpiredLink().error()).isEqualTo("EXPORT_LINK_EXPIRED");
    }
}
