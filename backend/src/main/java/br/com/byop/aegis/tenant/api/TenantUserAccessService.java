package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.identity.api.IdentityUserInviteActivatedEvent;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TenantUserAccessService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final List<String> ADMIN_ROLES = List.of(SUPER_ADMIN, "TENANT_ADMIN");

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public TenantUserAccessService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public TenantReference getRequiredTenant(UUID tenantId) {
        log.debug("getRequiredTenant: tenantId='{}'", tenantId);
        return tenantRepository.findById(tenantId)
                .map(tenant -> new TenantReference(tenant.getId(), tenant.getName()))
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipReference> listMemberships(UUID tenantId) {
        log.debug("listMemberships: tenantId='{}'", tenantId);
        ensureTenantExists(tenantId);
        return membershipRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toReference)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TenantMembershipReference> findMembership(UUID tenantId, String userSubject) {
        log.debug("findMembership: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        return membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .map(this::toReference);
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipReference> listActiveMemberships(String userSubject) {
        log.debug("listActiveMemberships: userSubject='{}'", userSubject);
        return membershipRepository.findAllByUserSubjectAndStatus(userSubject, TenantMembershipStatus.ACTIVE)
                .stream()
                .map(this::toReference)
                .toList();
    }

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
    public boolean hasAnyMembership(UUID tenantId, String userSubject) {
        log.debug("hasAnyMembership: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        return membershipRepository.existsByTenantIdAndUserSubject(tenantId, userSubject);
    }

    @Transactional(readOnly = true)
    public List<String> listActiveUserSubjects() {
        log.debug("listActiveUserSubjects: todos os tenants");
        return membershipRepository.findDistinctUserSubjectsByStatus(TenantMembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<String> listActiveUserSubjects(UUID tenantId) {
        log.debug("listActiveUserSubjects: tenantId='{}'", tenantId);
        ensureTenantExists(tenantId);
        return membershipRepository.findDistinctUserSubjectsByTenantIdAndStatus(tenantId, TenantMembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<String> listActiveSuperAdminSubjects() {
        log.debug("listActiveSuperAdminSubjects: consultando super admins ativos");
        return membershipRepository.findDistinctUserSubjectsByRoleAndStatus(SUPER_ADMIN, TenantMembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public boolean hasOtherActiveMembership(String userSubject, UUID ignoredTenantId) {
        log.debug("hasOtherActiveMembership: userSubject='{}', ignoredTenantId='{}'", userSubject, ignoredTenantId);
        return membershipRepository.findAllByUserSubjectAndStatus(userSubject, TenantMembershipStatus.ACTIVE)
                .stream()
                .anyMatch(membership -> !membership.getTenant().getId().equals(ignoredTenantId));
    }

    @Transactional(readOnly = true)
    public boolean wouldRemoveLastActiveAdmin(UUID tenantId, String userSubject, String nextRole, String nextStatus) {
        log.debug("wouldRemoveLastActiveAdmin: tenantId='{}', userSubject='{}', nextRole='{}', nextStatus='{}'",
                tenantId, userSubject, nextRole, nextStatus);
        return membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .filter(membership -> isActiveAdmin(membership.getRole(), membership.getStatus()))
                .filter(_ -> !isActiveAdmin(nextRole, parseStatus(nextStatus)))
                .map(_ -> membershipRepository.countByTenantIdAndRoleInAndStatus(
                        tenantId,
                        ADMIN_ROLES,
                        TenantMembershipStatus.ACTIVE
                ) <= 1)
                .orElse(false);
    }

    @Transactional
    public TenantMembershipReference invite(UUID tenantId, String userSubject, String role) {
        log.debug("invite: tenantId='{}', userSubject='{}', role='{}'", tenantId, userSubject, role);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        TenantMembership membership = new TenantMembership(tenant, userSubject, role);
        membership.invite();
        TenantMembershipReference saved = toReference(membershipRepository.save(membership));
        log.info("invite: membership criada id='{}', tenantId='{}', userSubject='{}'", saved.id(), tenantId, userSubject);
        return saved;
    }

    @Transactional
    public TenantMembershipReference update(UUID tenantId, String userSubject, String role, String status) {
        log.debug("update: tenantId='{}', userSubject='{}', role='{}', status='{}'", tenantId, userSubject, role, status);
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.changeRole(role);
        applyStatus(membership, parseStatus(status));
        TenantMembershipReference saved = toReference(membershipRepository.save(membership));
        log.info("update: membership atualizada id='{}', tenantId='{}', userSubject='{}'", saved.id(), tenantId, userSubject);
        return saved;
    }

    @Transactional
    public TenantMembershipReference block(UUID tenantId, String userSubject) {
        log.debug("block: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.suspend();
        TenantMembershipReference saved = toReference(membershipRepository.save(membership));
        log.info("block: membership bloqueada id='{}', tenantId='{}', userSubject='{}'", saved.id(), tenantId, userSubject);
        return saved;
    }

    @Transactional
    public TenantMembershipReference remove(UUID tenantId, String userSubject) {
        log.debug("remove: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.remove();
        TenantMembershipReference saved = toReference(membershipRepository.save(membership));
        log.info("remove: membership removida id='{}', tenantId='{}', userSubject='{}'", saved.id(), tenantId, userSubject);
        return saved;
    }

    /**
     * Ouve {@link IdentityUserInviteActivatedEvent} e transiciona todas as
     * {@code TenantMembership} do usuário de {@code INVITED} para {@code ACTIVE}.
     * Executado na mesma transação do evento para garantir consistência.
     */
    @EventListener
    @Transactional
    public void onUserInviteActivated(IdentityUserInviteActivatedEvent event) {
        log.debug("onUserInviteActivated: ativando memberships para keycloakId='{}'", event.keycloakId());
        membershipRepository.findAllByUserSubjectAndStatus(event.keycloakId(), TenantMembershipStatus.INVITED)
                .forEach(membership -> {
                    membership.activate();
                    membershipRepository.save(membership);
                    log.info("onUserInviteActivated: membership id='{}' transitada para ACTIVE", membership.getId());
                });
    }

    @Transactional
    public TenantMembershipReference restore(UUID tenantId, String userSubject) {
        log.debug("restore: tenantId='{}', userSubject='{}'", tenantId, userSubject);
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.activate();
        TenantMembershipReference saved = toReference(membershipRepository.save(membership));
        log.info("restore: membership restaurada id='{}', tenantId='{}', userSubject='{}'", saved.id(), tenantId, userSubject);
        return saved;
    }

    private void ensureTenantExists(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }

    private TenantMembershipReference toReference(TenantMembership membership) {
        return new TenantMembershipReference(
                membership.getId(),
                membership.getTenant().getId(),
                membership.getTenant().getName(),
                membership.getUserSubject(),
                membership.getRole(),
                toContractStatus(membership.getStatus()),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }

    private boolean isActiveAdmin(String role, TenantMembershipStatus status) {
        return status == TenantMembershipStatus.ACTIVE && ADMIN_ROLES.contains(role);
    }

    private void applyStatus(TenantMembership membership, TenantMembershipStatus status) {
        if (status == TenantMembershipStatus.ACTIVE) {
            membership.activate();
            return;
        }
        if (status == TenantMembershipStatus.INVITED) {
            membership.invite();
            return;
        }
        if (status == TenantMembershipStatus.SUSPENDED) {
            membership.suspend();
            return;
        }
        membership.remove();
    }

    private TenantMembershipStatus parseStatus(String status) {
        return switch (String.valueOf(status).trim().toLowerCase()) {
            case "ativo", "active" -> TenantMembershipStatus.ACTIVE;
            case "convidado", "invited" -> TenantMembershipStatus.INVITED;
            case "bloqueado", "suspended" -> TenantMembershipStatus.SUSPENDED;
            case "removido", "removed" -> TenantMembershipStatus.REMOVED;
            case "revoked" -> TenantMembershipStatus.REVOKED;
            default -> throw new IllegalArgumentException("Invalid membership status: " + status);
        };
    }

    private String toContractStatus(TenantMembershipStatus status) {
        return switch (status) {
            case ACTIVE -> "ativo";
            case INVITED -> "convidado";
            case SUSPENDED -> "bloqueado";
            case REVOKED, REMOVED -> "removido";
        };
    }
}
