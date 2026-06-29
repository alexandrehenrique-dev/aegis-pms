package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSeedServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantMembershipRepository membershipRepository;

    @InjectMocks
    private TenantSeedService service;

    @Test
    void shouldCreateActiveTenant() {
        Tenant saved = tenant("clientes-beta", "CLIENTES BETA");
        UUID tenantId = UUID.randomUUID();
        ReflectionTestUtils.setField(saved, "id", tenantId);
        when(tenantRepository.findByKey("clientes-beta")).thenReturn(Optional.empty());
        when(tenantRepository.save(org.mockito.ArgumentMatchers.any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            ReflectionTestUtils.setField(tenant, "id", tenantId);
            return tenant;
        });

        TenantSeedReference reference = service.ensureTenant(new TenantSeedCommand(
                "clientes-beta", "CLIENTES BETA", "Pro", "ACTIVE"
        ));

        assertThat(reference.id()).isEqualTo(tenantId);
        assertThat(reference.key()).isEqualTo("clientes-beta");
    }

    @Test
    void shouldUpdateSuspendedTenant() {
        Tenant existing = tenant("cliente-norte", "Cliente Norte Antigo");
        when(tenantRepository.findByKey("cliente-norte")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        service.ensureTenant(new TenantSeedCommand("cliente-norte", "Cliente Norte", "Starter", "SUSPENDED"));

        assertThat(existing.getName()).isEqualTo("Cliente Norte");
        assertThat(existing.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(existing.getPlan()).isEqualTo("Starter");
    }

    @Test
    void shouldArchiveTenantWhenSeedStatusIsArchived() {
        Tenant existing = tenant("legacy", "Legacy");
        when(tenantRepository.findByKey("legacy")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        service.ensureTenant(new TenantSeedCommand("legacy", "Legacy", "Free", "ARCHIVED"));

        assertThat(existing.getStatus()).isEqualTo(TenantStatus.ARCHIVED);
    }

    @Test
    void shouldCreateActiveMembershipWhenMissing() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant("clientes-beta", "CLIENTES BETA");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(membershipRepository.findByTenantIdAndUserSubject(tenantId, "user-id")).thenReturn(Optional.empty());

        service.ensureActiveMembership(tenantId, "user-id", "TENANT_ADMIN");

        verify(membershipRepository).save(org.mockito.ArgumentMatchers.argThat(membership ->
                membership.getTenant().equals(tenant)
                        && "user-id".equals(membership.getUserSubject())
                        && "TENANT_ADMIN".equals(membership.getRole())
                        && membership.getStatus() == TenantMembershipStatus.ACTIVE
        ));
    }

    @Test
    void shouldReactivateExistingMembership() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenant("clientes-beta", "CLIENTES BETA");
        TenantMembership membership = new TenantMembership(tenant, "user-id", "VIEWER");
        membership.remove();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(membershipRepository.findByTenantIdAndUserSubject(tenantId, "user-id"))
                .thenReturn(Optional.of(membership));

        service.ensureActiveMembership(tenantId, "user-id", "EDITOR");

        assertThat(membership.getRole()).isEqualTo("EDITOR");
        assertThat(membership.getStatus()).isEqualTo(TenantMembershipStatus.ACTIVE);
        verify(membershipRepository).save(membership);
    }

    private Tenant tenant(String key, String name) {
        return new Tenant(key, name);
    }
}
