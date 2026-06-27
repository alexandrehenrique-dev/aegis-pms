package br.com.byop.aegis.tenant.api;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.domain.TenantMembership;
import br.com.byop.aegis.tenant.domain.TenantMembershipStatus;
import br.com.byop.aegis.tenant.exception.TenantNotFoundException;
import br.com.byop.aegis.tenant.repository.TenantMembershipRepository;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantUserAccessService {

    private static final List<String> ADMIN_ROLES = List.of("SUPER_ADMIN", "TENANT_ADMIN");

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;

    public TenantUserAccessService(TenantRepository tenantRepository, TenantMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public TenantReference getRequiredTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> new TenantReference(tenant.getId(), tenant.getName()))
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional(readOnly = true)
    public List<TenantMembershipReference> listMemberships(UUID tenantId) {
        ensureTenantExists(tenantId);
        return membershipRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toReference)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TenantMembershipReference> findMembership(UUID tenantId, String userSubject) {
        return membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .map(this::toReference);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveMembership(UUID tenantId, String userSubject) {
        return membershipRepository.existsByTenantIdAndUserSubjectAndStatus(
                tenantId,
                userSubject,
                TenantMembershipStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public boolean hasAnyMembership(UUID tenantId, String userSubject) {
        return membershipRepository.existsByTenantIdAndUserSubject(tenantId, userSubject);
    }

    @Transactional(readOnly = true)
    public boolean hasOtherActiveMembership(String userSubject, UUID ignoredTenantId) {
        return membershipRepository.findAllByUserSubjectAndStatus(userSubject, TenantMembershipStatus.ACTIVE)
                .stream()
                .anyMatch(membership -> !membership.getTenant().getId().equals(ignoredTenantId));
    }

    @Transactional(readOnly = true)
    public boolean wouldRemoveLastActiveAdmin(UUID tenantId, String userSubject, String nextRole, String nextStatus) {
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
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        TenantMembership membership = new TenantMembership(tenant, userSubject, role);
        membership.invite();
        return toReference(membershipRepository.save(membership));
    }

    @Transactional
    public TenantMembershipReference update(UUID tenantId, String userSubject, String role, String status) {
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.changeRole(role);
        applyStatus(membership, parseStatus(status));
        return toReference(membershipRepository.save(membership));
    }

    @Transactional
    public TenantMembershipReference block(UUID tenantId, String userSubject) {
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.suspend();
        return toReference(membershipRepository.save(membership));
    }

    @Transactional
    public TenantMembershipReference remove(UUID tenantId, String userSubject) {
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.remove();
        return toReference(membershipRepository.save(membership));
    }

    @Transactional
    public TenantMembershipReference restore(UUID tenantId, String userSubject) {
        TenantMembership membership = membershipRepository.findByTenantIdAndUserSubject(tenantId, userSubject)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        membership.activate();
        return toReference(membershipRepository.save(membership));
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
