package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.domain.AuthActionToken;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class AuthActionEmailService {

    private static final String INVITE_TEMPLATE = "inviteActivation.ftl";
    private static final String ACCOUNT_RECOVERY_TEMPLATE = "passwordReset.ftl";
    private static final String INVITE_SUBJECT = "Voce foi convidado para o Aegis PMS";
    private static final String ACCOUNT_RECOVERY_SUBJECT = "Recuperacao de senha - Aegis PMS";
    private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'as' HH:mm", Locale.forLanguageTag("pt-BR"))
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private final JavaMailSender mailSender;
    private final Configuration freemarker;
    private final AuthActionTokenService tokenService;
    private final String from;
    private final String appBaseUrl;

    public AuthActionEmailService(JavaMailSender mailSender,
                                  AuthActionTokenService tokenService,
                                  @Value("${aegis.email.product-template-path}") String templatePath,
                                  @Value("${aegis.email.from}") String from,
                                  @Value("${aegis.app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.tokenService = tokenService;
        this.freemarker = new Configuration(Configuration.VERSION_2_3_34);
        this.from = from;
        this.appBaseUrl = appBaseUrl;
        configureTemplates(templatePath);
    }

    public void sendInviteActivation(AuthActionToken token) {
        log.debug("sendInviteActivation: keycloakId='{}'", token.getKeycloakId());
        send(token, INVITE_TEMPLATE, INVITE_SUBJECT, inviteModel(token));
        log.info("sendInviteActivation: e-mail de convite enviado para keycloakId='{}'", token.getKeycloakId());
    }

    public void sendPasswordReset(AuthActionToken token) {
        log.debug("sendPasswordReset: keycloakId='{}'", token.getKeycloakId());
        send(token, ACCOUNT_RECOVERY_TEMPLATE, ACCOUNT_RECOVERY_SUBJECT, passwordResetModel(token));
        log.info("sendPasswordReset: e-mail de redefinicao enviado para keycloakId='{}'", token.getKeycloakId());
    }

    private void send(AuthActionToken token, String templateName, String subject, Map<String, Object> model) {
        try {
            Template template = freemarker.getTemplate(templateName);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(token.getUserEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (IOException | MessagingException | TemplateException exception) {
            log.warn("send: falha ao enviar e-mail, template='{}'", templateName);
            throw new IllegalStateException("Unable to send auth action e-mail", exception);
        }
    }

    private void configureTemplates(String templatePath) {
        try {
            freemarker.setDirectoryForTemplateLoading(new File(templatePath));
            freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        } catch (IOException exception) {
            log.warn("configureTemplates: falha ao configurar templates de e-mail em '{}'", templatePath);
            throw new IllegalStateException("Unable to configure auth action e-mail templates", exception);
        }
    }

    private Map<String, Object> inviteModel(AuthActionToken token) {
        return Map.of(
                "userName", token.getUserName(),
                "inviterName", token.getInviterName(),
                "tenantName", token.getTenantName(),
                "productNames", String.join(", ", tokenService.productNames(token)),
                "role", token.getRole(),
                "activationUrl", normalizedAppBaseUrl() + "/invite?token=" + token.getId(),
                "expiresAt", EXPIRATION_FORMATTER.format(token.getExpiresAt())
        );
    }

    private Map<String, Object> passwordResetModel(AuthActionToken token) {
        return Map.of(
                "userName", token.getUserName(),
                "resetUrl", normalizedAppBaseUrl() + "/reset-password?token=" + token.getId(),
                "expiresAt", EXPIRATION_FORMATTER.format(token.getExpiresAt())
        );
    }

    private String normalizedAppBaseUrl() {
        String normalized = appBaseUrl;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
