package br.com.byop.aegis.tenant.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
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
    void shouldFindDistinctActiveUserSubjects() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("membership-distinct"));
        Tenant otherTenant = tenantRepository.saveAndFlush(tenant("membership-distinct-other"));
        membershipRepository.saveAndFlush(new TenantMembership(tenant, "subject-active", "TENANT_ADMIN"));
        membershipRepository.saveAndFlush(new TenantMembership(otherTenant, "subject-active", "VIEWER"));
        TenantMembership suspended = new TenantMembership(tenant, "subject-suspended", "VIEWER");
        suspended.suspend();
        membershipRepository.saveAndFlush(suspended);

        assertThat(membershipRepository.findDistinctUserSubjectsByStatus(TenantMembershipStatus.ACTIVE))
                .contains("subject-active")
                .doesNotContain("subject-suspended");
        assertThat(membershipRepository.findDistinctUserSubjectsByTenantIdAndStatus(
                tenant.getId(),
                TenantMembershipStatus.ACTIVE
        )).containsExactly("subject-active");
    }

    @Test
    void shouldCheckTutorialCompletedAcrossMemberships() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("membership-tutorial"));
        TenantMembership notCompleted = new TenantMembership(tenant, "subject-tutorial-pending", "EDITOR");
        membershipRepository.saveAndFlush(notCompleted);

        assertThat(membershipRepository.existsByUserSubjectAndTutorialCompletedTrue("subject-tutorial-pending")).isFalse();

        TenantMembership completed = new TenantMembership(tenant, "subject-tutorial-done", "EDITOR");
        completed.completeTutorial();
        membershipRepository.saveAndFlush(completed);

        assertThat(membershipRepository.existsByUserSubjectAndTutorialCompletedTrue("subject-tutorial-done")).isTrue();
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
