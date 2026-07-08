package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAccessServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMembershipRepository membershipRepository;

    private TenantAccessService service;

    @BeforeEach
    void setUp() {
        service = new TenantAccessService(tenantRepository, membershipRepository);
    }

    @Test
    void shouldReturnRequiredTenantReference() {
        Tenant tenant = tenant(TENANT_ID, "byop");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        TenantReference reference = service.getRequiredReference(TENANT_ID);

        assertThat(reference.tenantId()).isEqualTo(TENANT_ID);
        assertThat(reference.name()).isEqualTo("Tenant byop");
    }

    @Test
    void shouldRejectMissingTenantReference() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredReference(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessage("Tenant not found: " + TENANT_ID);
    }

    @Test
    void shouldListOnlyActiveTenantAdminTenantIds() {
        Tenant activeTenant = tenant(TENANT_ID, "active");
        Tenant suspendedTenant = tenant(UUID.fromString("22222222-2222-2222-2222-222222222222"), "suspended");
        Tenant viewerTenant = tenant(UUID.fromString("33333333-3333-3333-3333-333333333333"), "viewer");
        TenantMembership activeAdmin = membership(activeTenant, "TENANT_ADMIN", TenantMembershipStatus.ACTIVE);
        TenantMembership suspendedAdmin = membership(suspendedTenant, "TENANT_ADMIN", TenantMembershipStatus.SUSPENDED);
        TenantMembership activeViewer = membership(viewerTenant, "VIEWER", TenantMembershipStatus.ACTIVE);
        when(membershipRepository.findAllByUserSubject("subject"))
                .thenReturn(List.of(activeAdmin, suspendedAdmin, activeViewer));

        List<UUID> tenantIds = service.findActiveTenantAdminTenantIds("subject");

        assertThat(tenantIds).containsExactly(TENANT_ID);
    }

    @Test
    void shouldCheckActiveMembership() {
        when(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                TENANT_ID,
                "subject",
                TenantMembershipStatus.ACTIVE
        )).thenReturn(true);

        assertThat(service.hasActiveMembership(TENANT_ID, "subject")).isTrue();
    }

    @Test
    void shouldCheckActiveTenantAdminMembership() {
        Tenant tenant = tenant(TENANT_ID, "admin");
        TenantMembership activeAdmin = membership(tenant, "TENANT_ADMIN", TenantMembershipStatus.ACTIVE);
        when(membershipRepository.findAllByUserSubjectAndStatus("subject", TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of(activeAdmin));

        assertThat(service.hasActiveTenantAdminMembership(TENANT_ID, "subject")).isTrue();
    }

    @Test
    void shouldRejectActiveMembershipThatIsNotTenantAdmin() {
        Tenant tenant = tenant(TENANT_ID, "viewer");
        TenantMembership activeViewer = membership(tenant, "VIEWER", TenantMembershipStatus.ACTIVE);
        when(membershipRepository.findAllByUserSubjectAndStatus("subject", TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of(activeViewer));

        assertThat(service.hasActiveTenantAdminMembership(TENANT_ID, "subject")).isFalse();
    }

    @Test
    void shouldRejectTenantAdminMembershipFromDifferentTenant() {
        Tenant otherTenant = tenant(UUID.fromString("22222222-2222-2222-2222-222222222222"), "other");
        TenantMembership activeAdmin = membership(otherTenant, "TENANT_ADMIN", TenantMembershipStatus.ACTIVE);
        when(membershipRepository.findAllByUserSubjectAndStatus("subject", TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of(activeAdmin));

        assertThat(service.hasActiveTenantAdminMembership(TENANT_ID, "subject")).isFalse();
    }

    @Test
    void shouldFindTenantName() {
        Tenant tenant = tenant(TENANT_ID, "byop");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        assertThat(service.findTenantName(TENANT_ID)).isEqualTo("Tenant byop");
    }

    @Test
    void shouldReturnNullTenantNameWhenTenantDoesNotExist() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThat(service.findTenantName(TENANT_ID)).isNull();
    }

    @Test
    void shouldMarkTutorialCompletedOnAllMembershipsOfUser() {
        Tenant tenantOne = tenant(TENANT_ID, "one");
        Tenant tenantTwo = tenant(UUID.fromString("22222222-2222-2222-2222-222222222222"), "two");
        TenantMembership membershipOne = membership(tenantOne, "EDITOR", TenantMembershipStatus.ACTIVE);
        TenantMembership membershipTwo = membership(tenantTwo, "VIEWER", TenantMembershipStatus.ACTIVE);
        when(membershipRepository.findAllByUserSubject("subject"))
                .thenReturn(List.of(membershipOne, membershipTwo));

        service.markTutorialCompleted("subject");

        assertThat(membershipOne.isTutorialCompleted()).isTrue();
        assertThat(membershipTwo.isTutorialCompleted()).isTrue();
    }

    @Test
    void shouldCheckTutorialCompleted() {
        when(membershipRepository.existsByUserSubjectAndTutorialCompletedTrue("subject")).thenReturn(true);

        assertThat(service.hasCompletedTutorial("subject")).isTrue();
    }

    @Test
    void shouldReportTutorialNotCompletedWhenNoMembershipHasIt() {
        when(membershipRepository.existsByUserSubjectAndTutorialCompletedTrue("subject")).thenReturn(false);

        assertThat(service.hasCompletedTutorial("subject")).isFalse();
    }

    private Tenant tenant(UUID tenantId, String key) {
        Tenant tenant = new Tenant(key, "Tenant " + key);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        return tenant;
    }

    private TenantMembership membership(Tenant tenant, String role, TenantMembershipStatus status) {
        TenantMembership membership = new TenantMembership(tenant, "subject", role);
        if (status == TenantMembershipStatus.SUSPENDED) {
            membership.suspend();
        }
        if (status == TenantMembershipStatus.REVOKED) {
            membership.revoke();
        }
        return membership;
    }
}
