package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantStatus;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Porta publica do modulo tenant para seeds locais idempotentes.
 */
@Service
public class TenantSeedService {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public TenantSeedService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public TenantSeedReference ensureTenant(TenantSeedCommand command) {
        Tenant tenant = tenantRepository.findByKey(command.key())
                .orElseGet(() -> new Tenant(command.key(), command.name()));
        tenant.rename(command.name());
        tenant.changePlan(command.plan());
        applyTenantStatus(tenant, command.status());
        Tenant saved = tenantRepository.save(tenant);
        return new TenantSeedReference(saved.getId(), saved.getKey(), saved.getName());
    }

    @Transactional
    public void ensureActiveMembership(UUID tenantId, String userSubject, String role) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for seed: " + tenantId));
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseGet(() -> new TenantMembership(tenant, userSubject, role));
        membership.changeRole(role);
        membership.activate();
        membershipRepository.save(membership);
    }

    private void applyTenantStatus(Tenant tenant, String status) {
        TenantStatus parsedStatus = TenantStatus.valueOf(Objects.toString(status, "").trim().toUpperCase(Locale.ROOT));
        if (parsedStatus == TenantStatus.ACTIVE) {
            tenant.activate();
            return;
        }
        if (parsedStatus == TenantStatus.SUSPENDED) {
            tenant.suspend();
            return;
        }
        tenant.archive();
    }
}
