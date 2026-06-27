package br.com.byop.aegis.core;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductAssignment;
import br.com.byop.aegis.product.domain.ProductAssignmentRole;
import br.com.byop.aegis.product.domain.ProductAssignmentStatus;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CoreEntityBehaviorTest {

    @Test
    void shouldChangeTenantState() {
        Tenant tenant = new Tenant("byop", "BYOP");

        tenant.rename("BYOP Digital");
        tenant.suspend();
        assertThat(tenant.getName()).isEqualTo("BYOP Digital");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);

        tenant.activate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        tenant.archive();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ARCHIVED);
    }

    @Test
    void shouldChangeMembershipState() {
        TenantMembership membership = new TenantMembership(new Tenant("tenant", "Tenant"), "subject", "TENANT_ADMIN");

        membership.suspend();
        assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.SUSPENDED);

        membership.activate();
        assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);

        membership.revoke();
        assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.REVOKED);
    }

    @Test
    void shouldChangeProductStateAndDefaultStorage() {
        Product product = new Product(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "product",
                "Product",
                ProductTypeKey.CUSTOM,
                "pt-BR"
        );

        product.rename("Product Updated");
        product.suspend();
        assertThat(product.getName()).isEqualTo("Product Updated");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        assertThat(product.getAssetStorageStrategy()).isEqualTo(AssetStorageStrategy.LOCAL);

        product.activate();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        product.archive();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
    }

    @Test
    void shouldChangeProductModuleStateAndSettings() {
        ProductModule module = new ProductModule(product(), ModuleKey.CONTENT);

        module.enable();
        module.updateSettings("{\"enabledBy\":\"test\"}");
        assertThat(module.isEnabled()).isTrue();
        assertThat(module.getSettingsJson()).isEqualTo("{\"enabledBy\":\"test\"}");

        module.disable();
        assertThat(module.isEnabled()).isFalse();
    }

    @Test
    void shouldChangeProductAssignmentStateAndRole() {
        ProductAssignment assignment = new ProductAssignment(product(), "subject", ProductAssignmentRole.VIEWER);

        assertThat(assignment.getTenantId()).isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        assertThat(assignment.getProductId()).isNull();

        assignment.changeRole(ProductAssignmentRole.EDITOR);
        assertThat(assignment.getRole()).isEqualTo(ProductAssignmentRole.EDITOR);

        assignment.revoke();
        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.INVITED);

        assignment.assign();
        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.ASSIGNED);
    }

    @Test
    void shouldExposeProductTypeLabel() {
        assertThat(ProductTypeKey.SITE_INSTITUCIONAL.label()).isEqualTo("Site Institucional");
        assertThat(ProductTypeKey.LIBRARY_BOOKS_MUSIC.label()).isEqualTo("Library/Books/Music");
    }

    private Product product() {
        return new Product(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "product",
                "Product",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.S3
        );
    }
}
