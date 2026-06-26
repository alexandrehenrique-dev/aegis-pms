package br.com.byop.aegis.product.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class FreemarkerProductAssignmentEmailPort implements ProductAssignmentEmailPort {

    private static final String ASSIGNMENT_TEMPLATE = "productAssignment.ftl";
    private static final String REVOCATION_TEMPLATE = "productAccessRevoked.ftl";

    private final JavaMailSender mailSender;
    private final Configuration freemarker;
    private final String from;
    private final String appBaseUrl;

    public FreemarkerProductAssignmentEmailPort(JavaMailSender mailSender,
                                                @Value("${aegis.email.product-template-path}") String templatePath,
                                                @Value("${aegis.email.from}") String from,
                                                @Value("${aegis.app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.freemarker = new Configuration(Configuration.VERSION_2_3_34);
        this.from = from;
        this.appBaseUrl = appBaseUrl;
        configureTemplates(templatePath);
    }

    @Override
    public void notifyAssignment(ProductAssignmentEmailCommand command) {
        send(
                command,
                ASSIGNMENT_TEMPLATE,
                "Voce foi adicionado a um produto no Aegis PMS",
                model(command, productUrl(command))
        );
    }

    @Override
    public void notifyRevocation(ProductAssignmentEmailCommand command) {
        send(
                command,
                REVOCATION_TEMPLATE,
                "Seu acesso a um produto foi removido",
                model(command, null)
        );
    }

    private void send(ProductAssignmentEmailCommand command,
                      String templateName,
                      String subject,
                      Map<String, Object> model) {
        try {
            Template template = freemarker.getTemplate(templateName);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(command.recipientEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (IOException | MessagingException | TemplateException exception) {
            throw new IllegalStateException("Unable to send product assignment e-mail", exception);
        }
    }

    private void configureTemplates(String templatePath) {
        try {
            freemarker.setDirectoryForTemplateLoading(new File(templatePath));
            freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to configure product assignment e-mail templates", exception);
        }
    }

    private Map<String, Object> model(ProductAssignmentEmailCommand command, String productUrl) {
        return Map.of(
                "userName", command.recipientName(),
                "productName", command.productName(),
                "tenantName", command.tenantName(),
                "productUrl", productUrl == null ? "" : productUrl
        );
    }

    private String productUrl(ProductAssignmentEmailCommand command) {
        String normalizedBaseUrl = appBaseUrl;
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/products/" + command.productId();
    }
}
