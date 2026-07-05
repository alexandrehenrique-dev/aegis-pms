package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.identity.api.IdentityUserInviteActivatedEvent;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUserAccessServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMembershipRepository membershipRepository;

    @InjectMocks
    private TenantUserAccessService service;

    @Test
    void shouldGetRequiredTenant() {
        Tenant tenant = tenant(TENANT_ID, "BYOP");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        TenantReference reference = service.getRequiredTenant(TENANT_ID);

        assertThat(reference.tenantId()).isEqualTo(TENANT_ID);
        assertThat(reference.name()).isEqualTo("BYOP");
    }

    @Test
    void shouldRejectMissingTenant() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredTenant(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldListMemberships() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(membershipRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(membership));

        assertThat(service.listMemberships(TENANT_ID))
                .singleElement()
                .satisfies(reference -> {
                    assertThat(reference.userSubject()).isEqualTo("user-1");
                    assertThat(reference.status()).isEqualTo("ativo");
                });
    }

    @Test
    void shouldRejectListMembershipsWhenTenantDoesNotExist() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listMemberships(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldFindMembershipAndCheckActiveMembership() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                TENANT_ID,
                "user-1",
                TenantMembershipStatus.ACTIVE
        )).thenReturn(true);
        when(membershipRepository.existsByTenantIdAndUserSubject(TENANT_ID, "user-1")).thenReturn(true);

        assertThat(service.findMembership(TENANT_ID, "user-1")).isPresent();
        assertThat(service.hasActiveMembership(TENANT_ID, "user-1")).isTrue();
        assertThat(service.hasAnyMembership(TENANT_ID, "user-1")).isTrue();
    }

    @Test
    void shouldListActiveMembershipsByUserSubject() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        when(membershipRepository.findAllByUserSubjectAndStatus("user-1", TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));

        assertThat(service.listActiveMemberships("user-1"))
                .singleElement()
                .satisfies(reference -> {
                    assertThat(reference.tenantId()).isEqualTo(TENANT_ID);
                    assertThat(reference.status()).isEqualTo("ativo");
                });
    }

    @Test
    void shouldListActiveUserSubjects() {
        when(membershipRepository.findDistinctUserSubjectsByStatus(TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of("user-1", "user-2"));

        assertThat(service.listActiveUserSubjects())
                .containsExactly("user-1", "user-2");
    }

    @Test
    void shouldListActiveSuperAdminSubjects() {
        when(membershipRepository.findDistinctUserSubjectsByRoleAndStatus("SUPER_ADMIN", TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of("super-admin"));

        assertThat(service.listActiveSuperAdminSubjects()).containsExactly("super-admin");
    }

    @Test
    void shouldListActiveUserSubjectsByTenant() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(membershipRepository.findDistinctUserSubjectsByTenantIdAndStatus(TENANT_ID, TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of("tenant-user"));

        assertThat(service.listActiveUserSubjects(TENANT_ID))
                .containsExactly("tenant-user");
    }

    @Test
    void shouldDetectOtherActiveMembership() {
        TenantMembership same = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        TenantMembership other = membership(tenant(OTHER_TENANT_ID, "Other"), "user-1", "VIEWER");
        when(membershipRepository.findAllByUserSubjectAndStatus("user-1", TenantMembershipStatus.ACTIVE))
                .thenReturn(List.of(same, other));

        assertThat(service.hasOtherActiveMembership("user-1", TENANT_ID)).isTrue();
    }

    @Test
    void shouldDetectLastActiveAdmin() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "admin", "TENANT_ADMIN");
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "admin"))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countByTenantIdAndRoleInAndStatus(
                TENANT_ID,
                List.of("SUPER_ADMIN", "TENANT_ADMIN"),
                TenantMembershipStatus.ACTIVE
        )).thenReturn(1L);

        assertThat(service.wouldRemoveLastActiveAdmin(TENANT_ID, "admin", "VIEWER", "ativo")).isTrue();
    }

    @Test
    void shouldNotFlagLastAdminWhenRoleAndStatusRemainActiveAdmin() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "admin", "TENANT_ADMIN");
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "admin"))
                .thenReturn(Optional.of(membership));

        assertThat(service.wouldRemoveLastActiveAdmin(TENANT_ID, "admin", "TENANT_ADMIN", "ativo")).isFalse();
    }

    @Test
    void shouldNotFlagLastAdminWhenMoreAdminsRemain() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "admin", "TENANT_ADMIN");
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "admin"))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.countByTenantIdAndRoleInAndStatus(
                TENANT_ID,
                List.of("SUPER_ADMIN", "TENANT_ADMIN"),
                TenantMembershipStatus.ACTIVE
        )).thenReturn(2L);

        assertThat(service.wouldRemoveLastActiveAdmin(TENANT_ID, "admin", "VIEWER", "ativo")).isFalse();
    }

    @Test
    void shouldNotFlagInactiveAdminAsLastActiveAdmin() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "admin", "TENANT_ADMIN");
        membership.suspend();
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "admin"))
                .thenReturn(Optional.of(membership));

        assertThat(service.wouldRemoveLastActiveAdmin(TENANT_ID, "admin", "VIEWER", "ativo")).isFalse();
    }

    @Test
    void shouldCreateInvitedMembership() {
        Tenant tenant = tenant(TENANT_ID, "BYOP");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepository.save(any(TenantMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantMembershipReference reference = service.invite(TENANT_ID, "user-1", "EDITOR");

        assertThat(reference.status()).isEqualTo("convidado");
    }

    @Test
    void shouldRejectInviteWhenTenantDoesNotExist() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.invite(TENANT_ID, "user-1", "EDITOR"))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldUpdateBlockRemoveAndRestoreMembership() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.save(any(TenantMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.update(TENANT_ID, "user-1", "VIEWER", "ativo").status()).isEqualTo("ativo");
        assertThat(service.update(TENANT_ID, "user-1", "VIEWER", "convidado").status()).isEqualTo("convidado");
        assertThat(service.update(TENANT_ID, "user-1", "VIEWER", "bloqueado").status()).isEqualTo("bloqueado");
        assertThat(service.update(TENANT_ID, "user-1", "VIEWER", "removido").status()).isEqualTo("removido");
        assertThat(service.update(TENANT_ID, "user-1", "VIEWER", "revoked").status()).isEqualTo("removido");
        assertThat(service.block(TENANT_ID, "user-1").status()).isEqualTo("bloqueado");
        assertThat(service.remove(TENANT_ID, "user-1").status()).isEqualTo("removido");
        assertThat(service.restore(TENANT_ID, "user-1").status()).isEqualTo("ativo");

        verify(membershipRepository, times(8)).save(membership);
    }

    @Test
    void shouldRejectInvalidStatusOnUpdate() {
        TenantMembership membership = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.update(TENANT_ID, "user-1", "VIEWER", "invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingMembershipOnLifecycleOperations() {
        when(membershipRepository.findByTenantIdAndUserSubject(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(TENANT_ID, "missing", "VIEWER", "ativo"))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.block(TENANT_ID, "missing"))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.remove(TENANT_ID, "missing"))
                .isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.restore(TENANT_ID, "missing"))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldDoNothingOnActivationEventWhenNoInvitedMembershipsExist() {
        when(membershipRepository.findAllByUserSubjectAndStatus("user-1", TenantMembershipStatus.INVITED))
                .thenReturn(List.of());

        service.onUserInviteActivated(new IdentityUserInviteActivatedEvent("user-1"));

        verify(membershipRepository).findAllByUserSubjectAndStatus("user-1", TenantMembershipStatus.INVITED);
    }

    @Test
    void shouldActivateInvitedMembershipsOnUserInviteActivatedEvent() {
        TenantMembership invited = membership(tenant(TENANT_ID, "BYOP"), "user-1", "EDITOR");
        invited.invite(); // transiciona para INVITED
        when(membershipRepository.findAllByUserSubjectAndStatus("user-1", TenantMembershipStatus.INVITED))
                .thenReturn(List.of(invited));
        when(membershipRepository.save(any(TenantMembership.class))).thenAnswer(inv -> inv.getArgument(0));

        service.onUserInviteActivated(new IdentityUserInviteActivatedEvent("user-1"));

        assertThat(invited.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
        verify(membershipRepository).save(invited);
    }

    private Tenant tenant(UUID tenantId, String name) {
        Tenant tenant = new Tenant("tenant-" + tenantId, name);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        return tenant;
    }

    private TenantMembership membership(Tenant tenant, String subject, String role) {
        TenantMembership membership = new TenantMembership(tenant, subject, role);
        ReflectionTestUtils.setField(membership, "id", UUID.nameUUIDFromBytes(subject.getBytes()));
        ReflectionTestUtils.setField(membership, "createdAt", OffsetDateTime.parse("2026-06-27T10:00:00Z"));
        ReflectionTestUtils.setField(membership, "updatedAt", OffsetDateTime.parse("2026-06-27T10:00:00Z"));
        return membership;
    }
}
