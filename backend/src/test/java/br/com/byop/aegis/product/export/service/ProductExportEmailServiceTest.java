package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductExportEmailServiceTest {

    private static final String TEMPLATE_PATH = "../infra/keycloak/themes/aegis/email/html";
    private static final UUID TOKEN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @TempDir
    private File tempDir;

    @Test
    void shouldRenderAndSendExportReadyEmail() {
        JavaMailSender mailSender = mockMailSender();
        ProductExportEmailService service = service(mailSender, TEMPLATE_PATH, "http://localhost:8080/");

        service.sendExportReady(dataWithProductManifest(), storedExport(), TOKEN_ID, "admin@byop.dev", "Admin");

        MimeMessage message = sentMessage(mailSender);
        assertThat(subject(message)).isEqualTo("Exportacao de dados pronta no Aegis PMS");
        assertThat(firstRecipient(message)).hasToString("admin@byop.dev");
        assertThat(content(message))
                .contains("Admin")
                .contains("Maestro Beton")
                .contains("BYOP")
                .contains("http://localhost:8080/api/v1/exports/11111111-1111-1111-1111-111111111111/download")
                .contains("03/07/2026 09:00")
                .contains("0.50");
    }

    @Test
    void shouldRenderExportReadyEmailWithoutProductManifest() {
        JavaMailSender mailSender = mockMailSender();
        ProductExportEmailService service = service(mailSender, TEMPLATE_PATH, "http://localhost:8080");

        service.sendExportReady(dataWithoutProductManifest(), storedExport(), TOKEN_ID, "admin@byop.dev", "Admin");

        MimeMessage message = sentMessage(mailSender);
        assertThat(content(message))
                .contains("Maestro Beton")
                .contains("http://localhost:8080/api/v1/exports/11111111-1111-1111-1111-111111111111/download");
    }

    @Test
    void shouldSendFailureEmail() {
        JavaMailSender mailSender = mockMailSender();
        ProductExportEmailService service = service(mailSender, TEMPLATE_PATH, "http://localhost:8080");

        service.sendExportFailure("admin@byop.dev", "Maestro Beton");

        MimeMessage message = sentMessage(mailSender);
        assertThat(subject(message)).isEqualTo("Falha na exportacao de dados no Aegis PMS");
        assertThat(content(message)).contains("Nao foi possivel gerar o backup do produto Maestro Beton");
    }

    @Test
    void shouldRejectInvalidTemplateDirectory() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        File missing = new File(tempDir, "missing");
        String missingTemplatePath = missing.getAbsolutePath();

        assertThatThrownBy(() -> new ProductExportEmailService(
                mailSender,
                missingTemplatePath,
                "noreply@aegis.app",
                "http://localhost:8080"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to configure product export e-mail templates");
    }

    @Test
    void shouldWrapTemplateRenderingFailure() {
        JavaMailSender mailSender = mockMailSender();
        ProductExportEmailService service = service(mailSender, tempDir.getAbsolutePath(), "http://localhost:8080");
        ProductExportData data = dataWithProductManifest();
        StoredExport storedExport = storedExport();

        assertThatThrownBy(() -> service.sendExportReady(data, storedExport, TOKEN_ID, "admin@byop.dev", "Admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to send product export e-mail");
    }

    @Test
    void shouldWrapFailureEmailMessagingError() throws MessagingException {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);
        ProductExportEmailService service = service(mailSender, TEMPLATE_PATH, "http://localhost:8080");
        doThrow(new MessagingException("broken")).when(message).setFrom(org.mockito.Mockito.any(Address.class));

        assertThatThrownBy(() -> service.sendExportFailure("admin@byop.dev", "Maestro Beton"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to send export failure e-mail");
    }

    private ProductExportEmailService service(JavaMailSender mailSender, String templatePath, String appBaseUrl) {
        return new ProductExportEmailService(mailSender, templatePath, "noreply@aegis.app", appBaseUrl);
    }

    private JavaMailSender mockMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        return mailSender;
    }

    private MimeMessage sentMessage(JavaMailSender mailSender) {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private ProductExportData dataWithProductManifest() {
        return data(Map.of(
                "downloadExpiresAt", Instant.parse("2026-07-03T12:00:00Z"),
                "product", Map.of("tenantName", "BYOP")
        ));
    }

    private ProductExportData dataWithoutProductManifest() {
        return data(Map.of("downloadExpiresAt", Instant.parse("2026-07-03T12:00:00Z")));
    }

    private ProductExportData data(Map<String, Object> manifest) {
        return new ProductExportData(
                TENANT_ID,
                PRODUCT_ID,
                "maestro-beton",
                "Maestro Beton",
                AssetStorageStrategy.LOCAL,
                "aegis-export.zip",
                Map.of(),
                List.of(),
                manifest,
                Map.of(
                        "contentEntries", 3,
                        "pages", 2,
                        "forms", 1,
                        "formSubmissions", 4,
                        "assets", 2,
                        "knowledgeGraphNodes", 5,
                        "auditEvents", 6
                )
        );
    }

    private StoredExport storedExport() {
        return new StoredExport("local", "exports/token/aegis-export.zip", "aegis-export.zip", 524288L, Path.of("exports/token/aegis-export.zip"));
    }

    private String subject(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (MessagingException exception) {
            throw new AssertionError(exception);
        }
    }

    private Object firstRecipient(MimeMessage message) {
        try {
            return message.getAllRecipients()[0];
        } catch (MessagingException exception) {
            throw new AssertionError(exception);
        }
    }

    private String content(MimeMessage message) {
        try {
            return message.getContent().toString();
        } catch (IOException | MessagingException exception) {
            throw new AssertionError(exception);
        }
    }
}
