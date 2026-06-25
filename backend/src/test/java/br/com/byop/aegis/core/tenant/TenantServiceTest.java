package br.com.byop.aegis.core.tenant;

import br.com.byop.aegis.core.tenant.command.CreateTenantCommand;
import br.com.byop.aegis.core.tenant.dto.TenantSummary;
import br.com.byop.aegis.core.tenant.exception.TenantAlreadyExistsException;
import br.com.byop.aegis.core.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMembershipRepository membershipRepository;

    @Mock
    private TenantMapper tenantMapper;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldCreateTenantAndAutomaticTenantAdminMembership() {
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateTenantCommand command = new CreateTenantCommand("byop", "BYOP");
        Tenant savedTenant = new Tenant("byop", "BYOP");
        TenantSummary summary = tenantSummary("byop");
        when(tenantRepository.existsByKey("byop")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMapper.toSummary(savedTenant)).thenReturn(summary);

        TenantSummary result = tenantService.createTenant(caller, command);

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<TenantMembership> membershipCaptor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        TenantMembership membership = membershipCaptor.getValue();
        assertThat(membership.getTenant()).isEqualTo(savedTenant);
        assertThat(membership.getUserSubject()).isEqualTo("creator-subject");
        assertThat(membership.getRole()).isEqualTo("TENANT_ADMIN");
        assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
    }

    @Test
    void shouldRejectDuplicateTenantKey() {
        when(tenantRepository.existsByKey("byop")).thenReturn(true);
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateTenantCommand command = new CreateTenantCommand("byop", "BYOP");

        assertThatThrownBy(() -> tenantService.createTenant(caller, command))
                .isInstanceOf(TenantAlreadyExistsException.class)
                .hasMessage("Tenant already exists with key: byop");

        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(membershipRepository, never()).save(any(TenantMembership.class));
    }

    @Test
    void shouldListAllTenantsForSuperAdmin() {
        Tenant first = new Tenant("byop", "BYOP");
        Tenant second = new Tenant("aegis", "Aegis");
        TenantSummary firstSummary = tenantSummary("byop");
        TenantSummary secondSummary = tenantSummary("aegis");
        when(tenantRepository.findAll()).thenReturn(List.of(first, second));
        when(tenantMapper.toSummary(first)).thenReturn(firstSummary);
        when(tenantMapper.toSummary(second)).thenReturn(secondSummary);

        List<TenantSummary> result = tenantService.listTenants(user("super-subject", "ROLE_SUPER_ADMIN"));

        assertThat(result).containsExactly(firstSummary, secondSummary);
        verify(membershipRepository, never()).findAllByUserSubject("super-subject");
    }

    @Test
    void shouldListOnlyActiveMembershipTenantsForNonSuperAdmin() {
        Tenant activeTenant = new Tenant("active", "Active");
        Tenant suspendedTenant = new Tenant("suspended", "Suspended");
        TenantMembership activeMembership = new TenantMembership(activeTenant, "caller-subject", "TENANT_ADMIN");
        TenantMembership suspendedMembership = new TenantMembership(suspendedTenant, "caller-subject", "VIEWER");
        suspendedMembership.suspend();
        TenantSummary activeSummary = tenantSummary("active");
        when(membershipRepository.findAllByUserSubject("caller-subject"))
                .thenReturn(List.of(activeMembership, suspendedMembership));
        when(tenantMapper.toSummary(activeTenant)).thenReturn(activeSummary);

        List<TenantSummary> result = tenantService.listTenants(user("caller-subject", "ROLE_VIEWER"));

        assertThat(result).containsExactly(activeSummary);
        verify(tenantRepository, never()).findAll();
    }

    @Test
    void shouldFindTenantById() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Tenant tenant = new Tenant("byop", "BYOP");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThat(tenantService.getTenantOrThrow(tenantId)).isEqualTo(tenant);
    }

    @Test
    void shouldThrowWhenTenantIsNotFound() {
        UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantOrThrow(tenantId))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessage("Tenant not found: " + tenantId);
    }

    private AuthenticatedUser user(String subject, String authority) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(authority));
    }

    private TenantSummary tenantSummary(String key) {
        return new TenantSummary(
                UUID.nameUUIDFromBytes(key.getBytes()),
                key,
                "Tenant " + key,
                TenantStatus.ACTIVE,
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }
}
