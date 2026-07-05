package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.dto.StoredExport;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ProductExportEmailService {

    private static final String EXPORT_TEMPLATE = "productExport.ftl";
    private static final String NO_BACKUP_TEMPLATE = "productExportNoBackup.ftl";
    private static final String EXPORT_SUBJECT = "Exportacao de dados pronta no Aegis PMS";
    private static final String FAILURE_SUBJECT = "Falha na exportacao de dados no Aegis PMS";
    private static final String NO_BACKUP_SUBJECT = "Produto excluido sem backup no Aegis PMS";
    private static final BigDecimal BYTES_PER_MEGABYTE = BigDecimal.valueOf(1024L * 1024L);

    private final JavaMailSender mailSender;
    private final Configuration freemarker;
    private final String from;
    private final String appBaseUrl;

    public ProductExportEmailService(JavaMailSender mailSender,
                                     @Value("${aegis.email.product-template-path}") String templatePath,
                                     @Value("${aegis.email.from}") String from,
                                     @Value("${aegis.app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.freemarker = new Configuration(Configuration.VERSION_2_3_34);
        this.from = from;
        this.appBaseUrl = appBaseUrl;
        configureTemplates(templatePath);
    }

    public void sendExportReady(ProductExportData data, StoredExport storedExport, UUID tokenId,
                                String recipientEmail, String recipientName) {
        log.debug("sendExportReady: productId='{}', tokenId='{}'", data.productId(), tokenId);
        Map<?, ?> product = productManifest(data);
        Map<String, Object> model = Map.of(
                "userName", recipientName,
                "productName", data.productName(),
                "tenantName", String.valueOf(product.get("tenantName")),
                "downloadUrl", downloadUrl(tokenId),
                "expiresAt", formattedExpiration(data),
                "entityCounts", data.entityCounts(),
                "fileSizeMb", fileSizeMb(storedExport.sizeBytes())
        );
        sendHtml(EXPORT_TEMPLATE, recipientEmail, EXPORT_SUBJECT, model);
        log.info("sendExportReady: email de exportacao enviado productId='{}', tokenId='{}'", data.productId(), tokenId);
    }

    /**
     * Aviso enviado quando um produto é excluído sem que o backup de assets tenha
     * sido gerado — hoje, o único caso é {@code assetStorageStrategy} S3 sem bucket
     * configurado neste ambiente (ver {@code ExportAndDeleteService}).
     */
    public void sendProductDeletedWithoutBackup(String recipientEmail, String recipientName, String productName) {
        log.debug("sendProductDeletedWithoutBackup: productName='{}'", productName);
        Map<String, Object> model = Map.of(
                "userName", recipientName,
                "productName", productName
        );
        sendHtml(NO_BACKUP_TEMPLATE, recipientEmail, NO_BACKUP_SUBJECT, model);
        log.info("sendProductDeletedWithoutBackup: email de aviso enviado productName='{}'", productName);
    }

    public void sendExportFailure(String recipientEmail, String productName) {
        log.debug("sendExportFailure: productName='{}'", productName);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipientEmail);
            helper.setSubject(FAILURE_SUBJECT);
            helper.setText("Nao foi possivel gerar o backup do produto %s. Entre em contato com o suporte.".formatted(productName));
            mailSender.send(message);
            log.info("sendExportFailure: email de falha enviado productName='{}'", productName);
        } catch (MessagingException exception) {
            log.warn("sendExportFailure: falha ao enviar email de notificacao de falha productName='{}'", productName);
            throw new IllegalStateException("Unable to send export failure e-mail", exception);
        }
    }

    String downloadUrl(UUID tokenId) {
        String normalizedBaseUrl = appBaseUrl;
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/api/v1/exports/" + tokenId + "/download";
    }

    private void sendHtml(String templateName, String recipientEmail, String subject, Map<String, Object> model) {
        try {
            Template template = freemarker.getTemplate(templateName);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (IOException | MessagingException | TemplateException exception) {
            log.warn("sendHtml: falha ao enviar email subject='{}'", subject);
            throw new IllegalStateException("Unable to send product export e-mail", exception);
        }
    }

    private void configureTemplates(String templatePath) {
        try {
            freemarker.setDirectoryForTemplateLoading(new File(templatePath));
            freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to configure product export e-mail templates", exception);
        }
    }

    private String formattedExpiration(ProductExportData data) {
        Object expiresAt = data.manifest().get("downloadExpiresAt");
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("America/Sao_Paulo"))
                .format((java.time.Instant) expiresAt);
    }

    private String fileSizeMb(long sizeBytes) {
        return BigDecimal.valueOf(sizeBytes)
                .divide(BYTES_PER_MEGABYTE, 2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private Map<?, ?> productManifest(ProductExportData data) {
        Object product = data.manifest().get("product");
        if (product instanceof Map<?, ?> productMap) {
            return productMap;
        }
        return Map.of();
    }
}
