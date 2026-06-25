package br.com.byop.aegis.core;

import br.com.byop.aegis.core.product.AssetStorageStrategy;
import br.com.byop.aegis.core.product.ModuleKey;
import br.com.byop.aegis.core.product.Product;
import br.com.byop.aegis.core.product.ProductAssignment;
import br.com.byop.aegis.core.product.ProductAssignmentRole;
import br.com.byop.aegis.core.product.ProductAssignmentStatus;
import br.com.byop.aegis.core.product.ProductModule;
import br.com.byop.aegis.core.product.ProductStatus;
import br.com.byop.aegis.core.product.ProductTypeKey;
import br.com.byop.aegis.core.tenant.Tenant;
import br.com.byop.aegis.core.tenant.TenantMembership;
import br.com.byop.aegis.core.tenant.TenantMembershipStatus;
import br.com.byop.aegis.core.tenant.TenantStatus;
import org.junit.jupiter.api.Test;

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
                new Tenant("tenant", "Tenant"),
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

        assignment.changeRole(ProductAssignmentRole.EDITOR);
        assertThat(assignment.getRole()).isEqualTo(ProductAssignmentRole.EDITOR);

        assignment.revoke();
        assertThat(assignment.getStatus()).isEqualTo(ProductAssignmentStatus.REVOKED);

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
                new Tenant("tenant", "Tenant"),
                "product",
                "Product",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.S3
        );
    }
}
