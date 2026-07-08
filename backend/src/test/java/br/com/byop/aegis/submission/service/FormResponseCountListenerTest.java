package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.submission.api.SubmissionReceivedEvent;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FormResponseCountListenerTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-06-29T12:00:00Z");

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FormDefinitionRepository formDefinitionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanupGeneratedTenants() {
        jdbcTemplate.update("""
                DELETE FROM tenants
                WHERE key LIKE 'test-tenant-listener-%'
                  AND name LIKE 'TEST-Form Listener %'
                """);
    }

    @Test
    void shouldIncrementResponseCountAfterCommit() {
        FormDefinition form = saveForm("listener-commit");

        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new SubmissionReceivedEvent(form.getId(), form.getProductId(), RECEIVED_AT)));

        FormDefinition reloaded = formDefinitionRepository.findById(form.getId()).orElseThrow();
        assertThat(reloaded.getResponseCount()).isEqualTo(1L);
        assertThat(reloaded.getLastActivityAt().toInstant()).isEqualTo(RECEIVED_AT);
    }

    @Test
    void shouldNotIncrementResponseCountAfterRollback() {
        FormDefinition form = saveForm("listener-rollback");

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new SubmissionReceivedEvent(form.getId(), form.getProductId(), RECEIVED_AT));
            status.setRollbackOnly();
        });

        FormDefinition reloaded = formDefinitionRepository.findById(form.getId()).orElseThrow();
        assertThat(reloaded.getResponseCount()).isZero();
        assertThat(reloaded.getLastActivityAt()).isNull();
    }

    private FormDefinition saveForm(String suffix) {
        String uniqueSuffix = suffix + "-" + java.util.UUID.randomUUID();
        Tenant tenant = tenantRepository.saveAndFlush(new Tenant("test-tenant-" + uniqueSuffix, "TEST-Form Listener " + suffix));
        Product product = productRepository.saveAndFlush(new Product(
                tenant.getId(),
                "product-" + uniqueSuffix,
                "Product " + suffix,
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        ));
        return formDefinitionRepository.saveAndFlush(new FormDefinition(new FormDefinition.Creation(
                tenant.getId(), product.getId(), "Contato " + suffix, "lead",
                "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]", "[]"
        )));
    }
}
