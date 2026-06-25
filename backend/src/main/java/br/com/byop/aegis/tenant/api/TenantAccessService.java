package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantAccessService {

    private static final String TENANT_ADMIN = "TENANT_ADMIN";

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public TenantAccessService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public TenantReference getRequiredReference(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> new TenantReference(tenant.getId()))
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional(readOnly = true)
    public List<UUID> findActiveTenantAdminTenantIds(String userSubject) {
        return membershipRepository.findAllByUserSubject(userSubject)
                .stream()
                .filter(this::isActiveTenantAdminMembership)
                .map(membership -> membership.getTenant().getId())
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMembership(UUID tenantId, String userSubject) {
        return membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                tenantId,
                userSubject,
                TenantMembershipStatus.ACTIVE
        );
    }

    private boolean isActiveTenantAdminMembership(TenantMembership membership) {
        return membership.getStatus() == TenantMembershipStatus.ACTIVE && TENANT_ADMIN.equals(membership.getRole());
    }
}
