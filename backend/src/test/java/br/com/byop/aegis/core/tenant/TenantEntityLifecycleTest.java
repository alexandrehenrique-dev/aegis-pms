package br.com.byop.aegis.core.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantEntityLifecycleTest {

    @Test
    void shouldRunTenantJpaLifecycleCallbacks() {
        Tenant tenant = new Tenant();

        tenant.prePersist();
        assertThat(tenant.getCreatedAt()).isNotNull();
        assertThat(tenant.getUpdatedAt()).isNotNull();

        tenant.preUpdate();
        assertThat(tenant.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRunTenantMembershipJpaLifecycleCallbacks() {
        TenantMembership membership = new TenantMembership();

        membership.prePersist();
        assertThat(membership.getCreatedAt()).isNotNull();
        assertThat(membership.getUpdatedAt()).isNotNull();

        membership.preUpdate();
        assertThat(membership.getUpdatedAt()).isNotNull();
    }
}
