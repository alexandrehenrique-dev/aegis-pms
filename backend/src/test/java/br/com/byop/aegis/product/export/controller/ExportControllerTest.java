package br.com.byop.aegis.product.export.controller;

import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.domain.ExportTokenStatus;
import br.com.byop.aegis.product.export.exception.ExportDownloadNotFoundException;
import br.com.byop.aegis.product.export.exception.ExportLinkExpiredException;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import br.com.byop.aegis.product.export.service.ExportStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    private static final UUID TOKEN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-07-03T12:00:00Z");

    @Mock
    private ExportTokenRepository exportTokenRepository;

    @Mock
    private ExportStorageService exportStorageService;

    @Test
    void shouldStreamAvailableExportAndMarkFirstDownload() throws Exception {
        ExportToken token = availableToken();
        byte[] content = "zip-content".getBytes(StandardCharsets.UTF_8);
        when(exportTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(exportStorageService.filename(token.getZipPath())).thenReturn("aegis-export.zip");
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(content));

        ResponseEntity<StreamingResponseBody> response = controller().download(TOKEN_ID);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"aegis-export.zip\"");
        assertThat(outputStream.toByteArray()).isEqualTo(content);
        assertThat(token.getDownloadedAt()).isEqualTo(NOW);
        verify(exportTokenRepository).save(token);
    }

    @Test
    void shouldNotOverwriteExistingDownloadTimestamp() throws Exception {
        ExportToken token = availableToken();
        Instant firstDownload = Instant.parse("2026-07-02T12:00:00Z");
        token.markDownloaded(firstDownload);
        when(exportTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(exportStorageService.filename(token.getZipPath())).thenReturn("aegis-export.zip");
        when(exportStorageService.openStream(token)).thenReturn(new ByteArrayInputStream(new byte[] {1}));

        ResponseEntity<StreamingResponseBody> response = controller().download(TOKEN_ID);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getBody().writeTo(outputStream);

        assertThat(token.getDownloadedAt()).isEqualTo(firstDownload);
        verify(exportTokenRepository).save(token);
    }

    @Test
    void shouldRejectMissingToken() {
        when(exportTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.empty());
        ExportController controller = controller();

        assertThatThrownBy(() -> controller.download(TOKEN_ID))
                .isInstanceOf(ExportDownloadNotFoundException.class);
    }

    @Test
    void shouldRejectExpiredToken() {
        ExportToken token = availableToken();
        ReflectionTestUtils.setField(token, "expiresAt", NOW);
        when(exportTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        ExportController controller = controller();

        assertThatThrownBy(() -> controller.download(TOKEN_ID))
                .isInstanceOf(ExportLinkExpiredException.class);

        verify(exportTokenRepository, never()).save(token);
    }

    @Test
    void shouldRejectUnavailableToken() {
        ExportToken token = availableToken();
        ReflectionTestUtils.setField(token, "status", ExportTokenStatus.PENDING);
        when(exportTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        ExportController controller = controller();

        assertThatThrownBy(() -> controller.download(TOKEN_ID))
                .isInstanceOf(ExportDownloadNotFoundException.class);

        verify(exportTokenRepository, never()).save(token);
    }

    private ExportController controller() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new ExportController(exportTokenRepository, exportStorageService, clock);
    }

    private ExportToken availableToken() {
        ExportToken token = new ExportToken(PRODUCT_ID, "maestro-beton", "exports/token/aegis-export.zip", "local", "admin@byop.dev");
        ReflectionTestUtils.setField(token, "id", TOKEN_ID);
        ReflectionTestUtils.setField(token, "createdAt", NOW.minusSeconds(60));
        ReflectionTestUtils.setField(token, "expiresAt", NOW.plusSeconds(60));
        token.markAvailable("exports/token/aegis-export.zip");
        return token;
    }
}
