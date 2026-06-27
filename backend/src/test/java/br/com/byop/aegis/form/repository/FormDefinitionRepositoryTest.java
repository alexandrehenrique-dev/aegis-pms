package br.com.byop.aegis.form.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.domain.FormStatus;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FormDefinitionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FormDefinitionRepository formDefinitionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveFormDefinition() {
        Product product = saveProduct("form-save");

        FormDefinition saved = formDefinitionRepository.saveAndFlush(newForm(product, "Contato"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getName()).isEqualTo("Contato");
        assertThat(saved.getType()).isEqualTo("lead");
        assertThat(saved.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(saved.getFieldsJson()).contains("Email");
        assertThat(saved.getDeliveryChannelsJson()).isEqualTo("[]");
        assertThat(saved.getPublication()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyDefinitionDeliveryAndPublication() {
        Product product = saveProduct("form-edit");
        FormDefinition form = formDefinitionRepository.saveAndFlush(newForm(product, "Contato"));

        form.applyDefinition(new FormDefinition.Edit("Candidatura", "application", requiredTextFieldJson()));
        form.updateDeliveryChannels("[{\"type\":\"email\",\"enabled\":true,\"config\":{\"to\":\"rh@byop.com\"}}]");
        form.publish("2026-06-27T10:00:00Z");
        formDefinitionRepository.saveAndFlush(form);

        FormDefinition reloaded = formDefinitionRepository.findById(form.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Candidatura");
        assertThat(reloaded.getType()).isEqualTo("application");
        assertThat(reloaded.getFieldsJson()).contains("Nome");
        assertThat(reloaded.getDeliveryChannelsJson()).contains("rh@byop.com");
        assertThat(reloaded.getStatus()).isEqualTo(FormStatus.PUBLISHED);
        assertThat(reloaded.getPublication()).isEqualTo("2026-06-27T10:00:00Z");
    }

    @Test
    void shouldFindFormScopedByProduct() {
        Product product = saveProduct("form-find");
        Product otherProduct = saveProduct("form-find-other");
        FormDefinition form = formDefinitionRepository.saveAndFlush(newForm(product, "Contato"));
        formDefinitionRepository.saveAndFlush(newForm(otherProduct, "Outro"));

        assertThat(formDefinitionRepository.findAllByProductId(product.getId())).containsExactly(form);
        assertThat(formDefinitionRepository.findByProductIdAndId(product.getId(), form.getId())).contains(form);
        assertThat(formDefinitionRepository.findByProductIdAndId(otherProduct.getId(), form.getId())).isEmpty();
    }

    @Test
    void shouldCascadeDeleteFormWhenProductIsDeleted() {
        Product product = saveProduct("form-cascade");
        FormDefinition form = formDefinitionRepository.saveAndFlush(newForm(product, "Contato"));

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("form_definitions", form.getId())).isZero();
    }

    private FormDefinition newForm(Product product, String name) {
        return new FormDefinition(new FormDefinition.Creation(
                product.getTenantId(), product.getId(), name, "lead", emailFieldJson(), "[]"
        ));
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }

    private String emailFieldJson() {
        return "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]";
    }

    private String requiredTextFieldJson() {
        return "[{\"label\":\"Nome\",\"type\":\"Texto\",\"required\":true}]";
    }

    private Long countRows(String tableName, UUID id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Long.class, id);
    }
}
