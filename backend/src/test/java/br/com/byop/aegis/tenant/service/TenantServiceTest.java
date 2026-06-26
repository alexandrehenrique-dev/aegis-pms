package br.com.byop.aegis.tenant.service;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.command.CreateTenantCommand;
import br.com.byop.aegis.tenant.contract.DeleteTenantRequest;
import br.com.byop.aegis.tenant.contract.UpdateTenantRequest;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import br.com.byop.aegis.tenant.exception.InvalidTenantConfirmationException;
import br.com.byop.aegis.tenant.exception.InvalidTenantStatusException;
import br.com.byop.aegis.tenant.exception.TenantAlreadyExistsException;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.mapper.TenantMapper;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldCreateTenantAndAutomaticTenantAdminMembership() {
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateTenantCommand command = new CreateTenantCommand("byop", "BYOP", "PRO", "admin@byop.dev");
        Tenant savedTenant = new Tenant("byop", "BYOP");
        savedTenant.changePlan("PRO");
        TenantSummary summary = tenantSummary("byop");
        when(tenantRepository.existsByKey("byop")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMapper.toSummary(savedTenant)).thenReturn(summary);

        TenantSummary result = tenantService.createTenant(caller, command);

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getPlan()).isEqualTo("PRO");
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
        CreateTenantCommand command = new CreateTenantCommand("byop", "BYOP", null, null);

        assertThatThrownBy(() -> tenantService.createTenant(caller, command))
                .isInstanceOf(TenantAlreadyExistsException.class)
                .hasMessage("Tenant already exists with key: byop");

        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(membershipRepository, never()).save(any(TenantMembership.class));
    }

    @Test
    void shouldCreateTenantWithDefaultPlanWhenCommandPlanIsBlank() {
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateTenantCommand command = new CreateTenantCommand("blank-plan", "Blank Plan", " ", null);
        Tenant savedTenant = new Tenant("blank-plan", "Blank Plan");
        TenantSummary summary = tenantSummary("blank-plan");
        when(tenantRepository.existsByKey("blank-plan")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMapper.toSummary(savedTenant)).thenReturn(summary);

        TenantSummary result = tenantService.createTenant(caller, command);

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getPlan()).isEqualTo("FREE");
    }

    @Test
    void shouldCreateTenantWithDefaultPlanWhenCommandPlanIsNull() {
        AuthenticatedUser caller = user("creator-subject", "ROLE_TENANT_ADMIN");
        CreateTenantCommand command = new CreateTenantCommand("null-plan", "Null Plan", null, null);
        Tenant savedTenant = new Tenant("null-plan", "Null Plan");
        TenantSummary summary = tenantSummary("null-plan");
        when(tenantRepository.existsByKey("null-plan")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMapper.toSummary(savedTenant)).thenReturn(summary);

        TenantSummary result = tenantService.createTenant(caller, command);

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getPlan()).isEqualTo("FREE");
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
    void shouldUpdateTenant() {
        UUID tenantId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Tenant tenant = new Tenant("byop", "BYOP");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantSummary summary = tenantSummary("byop");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(tenantMapper.toSummary(tenant)).thenReturn(summary);

        TenantSummary result = tenantService.updateTenant(
                tenantId,
                new UpdateTenantRequest("BYOP Updated", "PRO", "suspenso")
        );

        assertThat(result).isEqualTo(summary);
        assertThat(tenant.getName()).isEqualTo("BYOP Updated");
        assertThat(tenant.getPlan()).isEqualTo("PRO");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    void shouldUpdateTenantToActiveStatus() {
        UUID tenantId = UUID.fromString("abababab-abab-abab-abab-abababababab");
        Tenant tenant = new Tenant("byop", "BYOP");
        tenant.suspend();
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantSummary summary = tenantSummary("byop");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(tenantMapper.toSummary(tenant)).thenReturn(summary);

        TenantSummary result = tenantService.updateTenant(
                tenantId,
                new UpdateTenantRequest("BYOP", "FREE", "ACTIVE")
        );

        assertThat(result).isEqualTo(summary);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void shouldUpdateTenantToArchivedStatus() {
        UUID tenantId = UUID.fromString("acacacac-acac-acac-acac-acacacacacac");
        Tenant tenant = new Tenant("byop", "BYOP");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        TenantSummary summary = tenantSummary("byop");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(tenantMapper.toSummary(tenant)).thenReturn(summary);

        TenantSummary result = tenantService.updateTenant(
                tenantId,
                new UpdateTenantRequest("BYOP", "FREE", "arquivado")
        );

        assertThat(result).isEqualTo(summary);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ARCHIVED);
    }

    @Test
    void shouldRejectInvalidTenantStatus() {
        UUID tenantId = UUID.fromString("adadadad-adad-adad-adad-adadadadadad");
        Tenant tenant = new Tenant("byop", "BYOP");
        UpdateTenantRequest request = new UpdateTenantRequest("BYOP", "FREE", "bloqueado");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.updateTenant(tenantId, request))
                .isInstanceOf(InvalidTenantStatusException.class)
                .hasMessage("Invalid tenant status: bloqueado");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void shouldRejectUpdateWhenTenantDoesNotExist() {
        UUID tenantId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UpdateTenantRequest request = new UpdateTenantRequest("Missing", "FREE", "ativo");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant(tenantId, request))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessage("Tenant not found: " + tenantId);

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void shouldDeleteTenantWhenConfirmationMatches() {
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UUID tenantId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        Tenant tenant = new Tenant("byop", "BYOP");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant(caller, tenantId, new DeleteTenantRequest("BYOP"));

        verify(jdbcTemplate).update(any(String.class), any(), any(), any(), any(), any());
        verify(tenantRepository).delete(tenant);
    }

    @Test
    void shouldRejectDeleteWhenConfirmationDoesNotMatch() {
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UUID tenantId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        Tenant tenant = new Tenant("byop", "BYOP");
        DeleteTenantRequest request = new DeleteTenantRequest("Wrong");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.deleteTenant(caller, tenantId, request))
                .isInstanceOf(InvalidTenantConfirmationException.class)
                .hasMessage("Tenant deletion confirmation text does not match tenant name");

        verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any(), any());
        verify(tenantRepository, never()).delete(any(Tenant.class));
    }

    @Test
    void shouldRejectDeleteWhenTenantDoesNotExist() {
        AuthenticatedUser caller = user("super-subject", "ROLE_SUPER_ADMIN");
        UUID tenantId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        DeleteTenantRequest request = new DeleteTenantRequest("BYOP");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.deleteTenant(caller, tenantId, request))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessage("Tenant not found: " + tenantId);

        verify(jdbcTemplate, never()).update(any(String.class), any(), any(), any(), any(), any());
        verify(tenantRepository, never()).delete(any(Tenant.class));
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
                "ativo",
                "FREE",
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }
}
