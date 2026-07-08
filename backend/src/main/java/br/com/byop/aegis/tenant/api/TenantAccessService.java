package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.audit.api.TenantVisibilityPort;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantAccessService implements TenantVisibilityPort {

    private static final String TENANT_ADMIN = "TENANT_ADMIN";

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public TenantAccessService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public TenantReference getRequiredReference(UUID tenantId) {
        log.debug("getRequiredReference: tenantId='{}'", tenantId);
        return tenantRepository.findById(tenantId)
                .map(tenant -> new TenantReference(tenant.getId(), tenant.getName()))
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional(readOnly = true)
    public List<UUID> findActiveTenantAdminTenantIds(String userSubject) {
        log.debug("findActiveTenantAdminTenantIds: userSubject='{}'", userSubject);
        return membershipRepository.findAllByUserSubject(userSubject)
                .stream()
                .filter(this::isActiveTenantAdminMembership)
                .map(membership -> membership.getTenant().getId())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveMembership(UUID tenantId, String userSubject) {
        log.debug("hasActiveMembership: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        return membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                tenantId,
                userSubject,
                TenantMembershipStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public boolean hasActiveTenantAdminMembership(UUID tenantId, String userSubject) {
        log.debug("hasActiveTenantAdminMembership: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        return membershipRepository.findAllByUserSubjectAndStatus(userSubject, TenantMembershipStatus.ACTIVE)
                .stream()
                .anyMatch(membership -> tenantId.equals(membership.getTenant().getId())
                        && TENANT_ADMIN.equals(membership.getRole()));
    }

    @Override
    @Transactional(readOnly = true)
    public String findTenantName(UUID tenantId) {
        log.debug("findTenantName: tenantId='{}'", tenantId);
        return tenantRepository.findById(tenantId).map(Tenant::getName).orElse(null);
    }

    /**
     * Marca o tutorial de onboarding como concluido em todas as memberships do
     * usuario (Sprint 22) — "concluido" e uma decisao do usuario, nao do
     * tenant, entao vale para qualquer tenant ao qual ele pertenca.
     *
     * @param userSubject subject do usuario no Keycloak
     */
    @Transactional
    public void markTutorialCompleted(String userSubject) {
        log.debug("markTutorialCompleted: userSubject='{}'", userSubject);
        membershipRepository.findAllByUserSubject(userSubject)
                .forEach(TenantMembership::completeTutorial);
        log.info("markTutorialCompleted: tutorial marcado como concluido userSubject='{}'", userSubject);
    }

    /**
     * Verifica se o usuario ja concluiu o tutorial de onboarding em algum tenant (Sprint 22).
     *
     * @param userSubject subject do usuario no Keycloak
     * @return {@code true} quando o tutorial ja foi concluido
     */
    @Transactional(readOnly = true)
    public boolean hasCompletedTutorial(String userSubject) {
        log.debug("hasCompletedTutorial: userSubject='{}'", userSubject);
        return membershipRepository.existsByUserSubjectAndTutorialCompletedTrue(userSubject);
    }

    private boolean isActiveTenantAdminMembership(TenantMembership membership) {
        return membership.getStatus() == TenantMembershipStatus.ACTIVE && TENANT_ADMIN.equals(membership.getRole());
    }
}
