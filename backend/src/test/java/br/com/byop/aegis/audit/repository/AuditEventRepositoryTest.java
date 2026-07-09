package br.com.byop.aegis.audit.repository;

import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void shouldSaveAuditEvent() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-save"));
        Product product = productRepository.saveAndFlush(product(tenant, "audit-save"));

        AuditEvent saved = auditEventRepository.saveAndFlush(new AuditEvent(new AuditEvent.Creation(
                tenant.getId(), product.getId(), "subject-1", "CONTENT_PUBLISHED", "Content",
                UUID.randomUUID().toString(), "Pagina inicial", "CONTENT", AuditRisk.MEDIO,
                "{\"before\":{\"status\":\"DRAFT\"},\"after\":{\"status\":\"PUBLISHED\"}}",
                "trace-1", "127.0.0.1", "JUnit"
        )));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(tenant.getId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getActorSubject()).isEqualTo("subject-1");
        assertThat(saved.getAction()).isEqualTo("CONTENT_PUBLISHED");
        assertThat(saved.getTargetType()).isEqualTo("Content");
        assertThat(saved.getTargetLabel()).isEqualTo("Pagina inicial");
        assertThat(saved.getModule()).isEqualTo("CONTENT");
        assertThat(saved.getRisk()).isEqualTo(AuditRisk.MEDIO);
        assertThat(saved.getDiffJson()).contains("PUBLISHED");
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("JUnit");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldSaveAuditEventWithoutOptionalFields() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-minimal"));

        AuditEvent saved = auditEventRepository.saveAndFlush(new AuditEvent(new AuditEvent.Creation(
                tenant.getId(), null, "subject-1", "TENANT_CREATED", "Tenant", tenant.getId().toString(),
                tenant.getName(), null, AuditRisk.BAIXO, null, null, null, null
        )));

        assertThat(saved.getProductId()).isNull();
        assertThat(saved.getModule()).isNull();
        assertThat(saved.getDiffJson()).isNull();
        assertThat(saved.getTraceId()).isNull();
        assertThat(saved.getIp()).isNull();
        assertThat(saved.getUserAgent()).isNull();
    }

    @Test
    void shouldFilterByTenantIsolation() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-isolation"));
        Tenant otherTenant = tenantRepository.saveAndFlush(tenant("audit-isolation-other"));
        AuditEvent event = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "TENANT_UPDATED", null, AuditRisk.MEDIO));
        auditEventRepository.saveAndFlush(newEvent(otherTenant.getId(), null, "subject-1",
                "TENANT_UPDATED", null, AuditRisk.MEDIO));

        assertThat(auditEventRepository.findAllByFilters(tenant.getId(), null, null, null, null))
                .containsExactly(event);
    }

    @Test
    void shouldPageAuditEventsByTenantIsolation() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-page"));
        Tenant otherTenant = tenantRepository.saveAndFlush(tenant("audit-page-other"));
        auditEventRepository.saveAndFlush(newEvent(otherTenant.getId(), null, "subject-1",
                "TENANT_UPDATED", null, AuditRisk.MEDIO));
        AuditEvent first = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "TENANT_UPDATED", null, AuditRisk.MEDIO));
        AuditEvent second = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "USER_BLOCKED", null, AuditRisk.ALTO));

        Page<AuditEvent> firstPage = auditEventRepository.findPageByFilters(
                tenant.getId(), null, null, null, null, PageRequest.of(0, 1));
        Page<AuditEvent> secondPage = auditEventRepository.findPageByFilters(
                tenant.getId(), null, null, null, null, PageRequest.of(1, 1));

        assertThat(firstPage.getContent()).containsExactly(second);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent()).isEqualTo(List.of(first));
    }

    @Test
    void shouldFilterByActorSubject() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-actor"));
        AuditEvent matching = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "USER_BLOCKED", null, AuditRisk.MEDIO));
        auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-2",
                "USER_BLOCKED", null, AuditRisk.MEDIO));

        assertThat(auditEventRepository.findAllByFilters(tenant.getId(), "subject-1", null, null, null))
                .containsExactly(matching);
    }

    @Test
    void shouldFilterByProductId() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-product"));
        Product product = productRepository.saveAndFlush(product(tenant, "audit-product"));
        Product otherProduct = productRepository.saveAndFlush(product(tenant, "audit-product-other"));
        AuditEvent matching = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), product.getId(),
                "subject-1", "ASSET_DELETED", "ASSETS", AuditRisk.ALTO));
        auditEventRepository.saveAndFlush(newEvent(tenant.getId(), otherProduct.getId(),
                "subject-1", "ASSET_DELETED", "ASSETS", AuditRisk.ALTO));

        assertThat(auditEventRepository.findAllByFilters(tenant.getId(), null, product.getId(), null, null))
                .containsExactly(matching);
    }

    @Test
    void shouldFilterByModule() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-module"));
        AuditEvent matching = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "CONTENT_CREATED", "CONTENT", AuditRisk.BAIXO));
        auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "ASSET_DELETED", "ASSETS", AuditRisk.ALTO));

        assertThat(auditEventRepository.findAllByFilters(tenant.getId(), null, null, "CONTENT", null))
                .containsExactly(matching);
    }

    @Test
    void shouldFilterByRisk() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-risk"));
        AuditEvent matching = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "TENANT_DELETED", null, AuditRisk.ALTO));
        auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "TENANT_CREATED", null, AuditRisk.BAIXO));

        assertThat(auditEventRepository.findAllByFilters(tenant.getId(), null, null, null, AuditRisk.ALTO))
                .containsExactly(matching);
    }

    @Test
    void shouldFindByIdScopedByTenant() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-find-by-id"));
        Tenant otherTenant = tenantRepository.saveAndFlush(tenant("audit-find-by-id-other"));
        AuditEvent event = auditEventRepository.saveAndFlush(newEvent(tenant.getId(), null, "subject-1",
                "TENANT_UPDATED", null, AuditRisk.MEDIO));

        assertThat(auditEventRepository.findByIdAndTenantId(event.getId(), tenant.getId())).contains(event);
        assertThat(auditEventRepository.findByIdAndTenantId(event.getId(), otherTenant.getId())).isEmpty();
    }

    @Test
    void shouldSurviveTenantDeletionBecauseThereIsNoForeignKey() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("audit-survive-delete"));
        UUID tenantId = tenant.getId();
        AuditEvent event = auditEventRepository.saveAndFlush(newEvent(tenantId, null, "subject-1",
                "TENANT_DELETED", null, AuditRisk.ALTO));

        tenantRepository.delete(tenant);
        tenantRepository.flush();

        assertThat(auditEventRepository.findById(event.getId())).isPresent();
        assertThat(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).contains(event);
    }

    private AuditEvent newEvent(UUID tenantId, UUID productId, String actorSubject, String action,
                                 String module, AuditRisk risk) {
        return new AuditEvent(new AuditEvent.Creation(
                tenantId, productId, actorSubject, action, null, null, null, module, risk, null, null, null, null
        ));
    }
}
