package br.com.byop.aegis.product.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FreemarkerProductAssignmentEmailPortTest {

    private static final String TEMPLATE_PATH = "../infra/keycloak/themes/aegis/email/html";

    @TempDir
    private File tempDir;

    @Test
    void shouldRenderAndSendAssignmentEmail() {
        JavaMailSender mailSender = mockMailSender();
        FreemarkerProductAssignmentEmailPort port = new FreemarkerProductAssignmentEmailPort(
                mailSender,
                TEMPLATE_PATH,
                "noreply@aegis.app",
                "http://localhost:8080/"
        );

        port.notifyAssignment(command());

        MimeMessage message = sentMessage(mailSender);
        assertThat(subject(message)).isEqualTo("Voce foi adicionado a um produto no Aegis PMS");
        assertThat(firstRecipient(message)).hasToString("editor@byop.dev");
        assertThat(content(message))
                .contains("Editor User")
                .contains("Aegis PMS")
                .contains("Tenant Aegis")
                .contains("http://localhost:8080/products/22222222-2222-2222-2222-222222222222");
    }

    @Test
    void shouldRenderAndSendRevocationEmail() {
        JavaMailSender mailSender = mockMailSender();
        FreemarkerProductAssignmentEmailPort port = new FreemarkerProductAssignmentEmailPort(
                mailSender,
                TEMPLATE_PATH,
                "noreply@aegis.app",
                "http://localhost:8080"
        );

        port.notifyRevocation(command());

        MimeMessage message = sentMessage(mailSender);
        assertThat(subject(message)).isEqualTo("Seu acesso a um produto foi removido");
        assertThat(content(message))
                .contains("Editor User")
                .contains("Aegis PMS")
                .contains("Tenant Aegis");
    }

    @Test
    void shouldRejectInvalidTemplateDirectory() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        File missing = new File(tempDir, "missing");
        String missingTemplatePath = missing.getAbsolutePath();

        assertThatThrownBy(() -> productAssignmentEmailPort(
                mailSender,
                missingTemplatePath,
                "noreply@aegis.app",
                "http://localhost:8080"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to configure product assignment e-mail templates");
    }

    @Test
    void shouldWrapTemplateRenderingFailure() {
        JavaMailSender mailSender = mockMailSender();
        FreemarkerProductAssignmentEmailPort port = new FreemarkerProductAssignmentEmailPort(
                mailSender,
                tempDir.getAbsolutePath(),
                "noreply@aegis.app",
                "http://localhost:8080"
        );
        ProductAssignmentEmailCommand command = command();

        assertThatThrownBy(() -> port.notifyAssignment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to send product assignment e-mail");
    }

    private FreemarkerProductAssignmentEmailPort productAssignmentEmailPort(JavaMailSender mailSender,
                                                                           String templatePath,
                                                                           String from,
                                                                           String appBaseUrl) {
        return new FreemarkerProductAssignmentEmailPort(mailSender, templatePath, from, appBaseUrl);
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

    private ProductAssignmentEmailCommand command() {
        return new ProductAssignmentEmailCommand(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Tenant Aegis",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Aegis PMS",
                "editor@byop.dev",
                "Editor User"
        );
    }
}
