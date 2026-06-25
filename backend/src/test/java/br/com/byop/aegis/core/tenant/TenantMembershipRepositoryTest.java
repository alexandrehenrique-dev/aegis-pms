package br.com.byop.aegis.core.tenant;

import br.com.byop.aegis.core.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantMembershipRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Test
    void shouldSaveMembership() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("membership-save"));

        TenantMembership saved = membershipRepository.saveAndFlush(
                new TenantMembership(tenant, "subject-save", "TENANT_ADMIN")
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenant()).isEqualTo(tenant);
        assertThat(saved.getUserSubject()).isEqualTo("subject-save");
        assertThat(saved.getRole()).isEqualTo("TENANT_ADMIN");
        assertThat(saved.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByTenantIdAndUserSubject() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("membership-find"));
        TenantMembership saved = membershipRepository.saveAndFlush(
                new TenantMembership(tenant, "subject-find", "TENANT_ADMIN")
        );

        assertThat(membershipRepository.findByTenantIdAndUserSubject(tenant.getId(), "subject-find"))
                .contains(saved);
        assertThat(membershipRepository.findAllByUserSubject("subject-find"))
                .containsExactly(saved);
        assertThat(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                tenant.getId(), "subject-find", TenantMembershipStatus.ACTIVE
        )).isTrue();
    }

    @Test
    void shouldRejectDuplicateTenantAndUserSubject() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("membership-duplicate"));
        membershipRepository.saveAndFlush(new TenantMembership(tenant, "subject-duplicate", "TENANT_ADMIN"));
        TenantMembership duplicate = new TenantMembership(tenant, "subject-duplicate", "VIEWER");

        assertThatThrownBy(() -> membershipRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
