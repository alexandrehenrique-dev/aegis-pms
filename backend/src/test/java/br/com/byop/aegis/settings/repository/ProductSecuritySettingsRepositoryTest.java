package br.com.byop.aegis.settings.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.settings.domain.ProductSecuritySettings;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSecuritySettingsRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSecuritySettingsRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveProductSecuritySettings() {
        Product product = saveProduct("security-settings-save");
        ProductSecuritySettings settings = new ProductSecuritySettings(product.getId());

        settings.update(new ProductSecuritySettings.Update("https://example.com/hook", "secret",
                true, "ga-key", true, "123", "telegram-token"));
        ProductSecuritySettings saved = repository.saveAndFlush(settings);

        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getWebhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(saved.getWebhookSecret()).isEqualTo("secret");
        assertThat(saved.isAnalyticsEnabled()).isTrue();
        assertThat(saved.getAnalyticsProviderKey()).isEqualTo("ga-key");
        assertThat(saved.isEmailDeliveryEnabled()).isTrue();
        assertThat(saved.getTelegramAlertChatId()).isEqualTo("123");
        assertThat(saved.getTelegramAlertBotToken()).isEqualTo("telegram-token");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldKeepExistingWebhookSecretWhenRequestDoesNotSendSecret() {
        Product product = saveProduct("security-settings-secret");
        ProductSecuritySettings settings = new ProductSecuritySettings(product.getId());
        settings.update(new ProductSecuritySettings.Update("https://example.com/hook", "secret",
                false, null, false, "123", "telegram-token"));

        settings.update(new ProductSecuritySettings.Update(" ", "", true, "ga-key", true, " ", ""));

        assertThat(settings.getWebhookUrl()).isNull();
        assertThat(settings.getWebhookSecret()).isEqualTo("secret");
        assertThat(settings.getAnalyticsProviderKey()).isEqualTo("ga-key");
        assertThat(settings.getTelegramAlertChatId()).isNull();
        assertThat(settings.getTelegramAlertBotToken()).isNull();
    }

    @Test
    void shouldCascadeDeleteSettingsWhenProductIsDeleted() {
        Product product = saveProduct("security-settings-cascade");
        repository.saveAndFlush(new ProductSecuritySettings(product.getId()));

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());
        entityManager.clear();

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_security_settings WHERE product_id = ?",
                Long.class, product.getId());
        assertThat(count).isZero();
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }
}
