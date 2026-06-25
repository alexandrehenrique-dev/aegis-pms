package br.com.byop.aegis.tenant.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldSaveTenant() {
        Tenant saved = tenantRepository.saveAndFlush(tenant("save"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKey()).isEqualTo("tenant-save");
        assertThat(saved.getName()).isEqualTo("Tenant save");
        assertThat(saved.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(tenantRepository.existsByKey("tenant-save")).isTrue();
    }

    @Test
    void shouldFindTenantByKey() {
        Tenant saved = tenantRepository.saveAndFlush(tenant("find"));

        assertThat(tenantRepository.findByKey("tenant-find"))
                .contains(saved);
        assertThat(tenantRepository.findByKey("tenant-missing"))
                .isEmpty();
    }

    @Test
    void shouldRejectDuplicateTenantKey() {
        tenantRepository.saveAndFlush(tenant("duplicate"));
        Tenant duplicate = tenant("duplicate");

        assertThatThrownBy(() -> tenantRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
