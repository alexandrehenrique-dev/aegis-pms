package br.com.byop.aegis.audit.service;

import br.com.byop.aegis.audit.domain.AuditRisk;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRiskCatalogTest {

    private final AuditRiskCatalog catalog = new AuditRiskCatalog();

    @Test
    void shouldResolveRiskForKnownActions() {
        assertThat(catalog.resolve("TENANT_CREATED")).isEqualTo(AuditRisk.BAIXO);
        assertThat(catalog.resolve("TENANT_UPDATED")).isEqualTo(AuditRisk.MEDIO);
        assertThat(catalog.resolve("TENANT_DELETED")).isEqualTo(AuditRisk.ALTO);
        assertThat(catalog.resolve("PRODUCT_ASSIGNMENT_CREATED")).isEqualTo(AuditRisk.BAIXO);
        assertThat(catalog.resolve("PRODUCT_ASSIGNMENT_REMOVED")).isEqualTo(AuditRisk.MEDIO);
        assertThat(catalog.resolve("USER_INVITED_TO_TENANT")).isEqualTo(AuditRisk.BAIXO);
        assertThat(catalog.resolve("USER_BLOCKED")).isEqualTo(AuditRisk.MEDIO);
        assertThat(catalog.resolve("USER_REMOVED_FROM_TENANT")).isEqualTo(AuditRisk.ALTO);
        assertThat(catalog.resolve("USER_RESTORED_TO_TENANT")).isEqualTo(AuditRisk.MEDIO);
        assertThat(catalog.resolve("CONTENT_CREATED")).isEqualTo(AuditRisk.BAIXO);
        assertThat(catalog.resolve("CONTENT_PUBLISHED")).isEqualTo(AuditRisk.MEDIO);
        assertThat(catalog.resolve("ASSET_DELETED")).isEqualTo(AuditRisk.ALTO);
    }

    @Test
    void shouldResolveDefaultRiskForUnknownAction() {
        assertThat(catalog.resolve("UNKNOWN_ACTION")).isEqualTo(AuditRisk.MEDIO);
    }
}
