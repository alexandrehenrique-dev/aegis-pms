package br.com.byop.aegis.product.user.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserLifecycleService;
import br.com.byop.aegis.product.api.ProductUserAccess;
import br.com.byop.aegis.product.api.ProductUserAccessService;
import br.com.byop.aegis.product.user.contract.InviteTenantUserRequest;
import br.com.byop.aegis.product.user.contract.UpdateTenantUserRequest;
import br.com.byop.aegis.product.user.dto.TenantUserSummary;
import br.com.byop.aegis.product.user.exception.InvalidTenantUserOperationException;
import br.com.byop.aegis.product.user.exception.LastTenantAdminException;
import br.com.byop.aegis.product.user.exception.TenantUserAlreadyExistsException;
import br.com.byop.aegis.product.user.exception.TenantUserNotFoundException;
import br.com.byop.aegis.product.user.mapper.TenantUserMapper;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantMembershipReference;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TenantUserService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String TENANT_ADMIN = "TENANT_ADMIN";
    private static final String STATUS_ACTIVE = "ativo";
    private static final String STATUS_INVITED = "convidado";
    private static final String STATUS_REMOVED = "removido";
    private static final String STATUS_BLOCKED = "bloqueado";
    private static final String STATUS_ACTIVE_ALIAS = "active";
    private static final String STATUS_INVITED_ALIAS = "invited";
    private static final String STATUS_REMOVED_ALIAS = "removed";
    private static final String STATUS_BLOCKED_ALIAS = "suspended";
    private static final String DIFF_KEY_STATUS = "status";

    private final IdentityUserLifecycleService identityUserLifecycleService;
    private final TenantUserAccessService tenantUserAccessService;
    private final ProductUserAccessService productUserAccessService;
    private final TenantUserMapper userMapper;
    private final AuditService auditService;

    public TenantUserService(IdentityUserLifecycleService identityUserLifecycleService,
                             TenantUserAccessService tenantUserAccessService,
                             ProductUserAccessService productUserAccessService,
                             TenantUserMapper userMapper,
                             AuditService auditService) {
        this.identityUserLifecycleService = identityUserLifecycleService;
        this.tenantUserAccessService = tenantUserAccessService;
        this.productUserAccessService = productUserAccessService;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TenantUserSummary> listUsers(AuthenticatedUser caller, UUID tenantId) {
        assertTenantVisible(caller, tenantId);
        Set<String> visibleSubjects = visibleSubjects(caller, tenantId);
        return tenantUserAccessService.listMemberships(tenantId)
                .stream()
                .filter(membership -> visibleSubjects.isEmpty() || visibleSubjects.contains(membership.userSubject()))
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public TenantUserSummary inviteUser(AuthenticatedUser caller, UUID tenantId, InviteTenantUserRequest request) {
        assertTenantVisible(caller, tenantId);
        assertCanInvite(caller, tenantId);
        identityUserLifecycleService.findByEmail(request.email())
                .filter(user -> tenantUserAccessService.hasAnyMembership(tenantId, user.id()))
                .ifPresent(_ -> {
                    throw new TenantUserAlreadyExistsException();
                });

        IdentityUser user = identityUserLifecycleService.invite(request.email(), request.name());
        if (tenantUserAccessService.hasAnyMembership(tenantId, user.id())) {
            throw new TenantUserAlreadyExistsException();
        }

        String role = parseRole(request.role());
        TenantMembershipReference membership = tenantUserAccessService.invite(tenantId, user.id(), role);
        recordAudit(tenantId, caller.subject(), "USER_INVITED_TO_TENANT", user.id(), user.displayName(),
                null, Map.of("role", role, DIFF_KEY_STATUS, STATUS_INVITED));
        return toSummary(membership, user);
    }

    @Transactional(readOnly = true)
    public TenantUserSummary getUser(AuthenticatedUser caller, UUID tenantId, String userId) {
        return toSummary(getVisibleMembership(caller, tenantId, userId));
    }

    @Transactional
    public TenantUserSummary updateUser(AuthenticatedUser caller, UUID tenantId, String userId,
                                        UpdateTenantUserRequest request) {
        TenantMembershipReference current = getVisibleMembership(caller, tenantId, userId);
        String role = parseRole(request.role());
        String status = parseStatus(request.status());
        assertNotLastActiveAdmin(tenantId, current.userSubject(), role, status);
        return toSummary(tenantUserAccessService.update(tenantId, userId, role, status));
    }

    @Transactional
    public TenantUserSummary resendInvite(AuthenticatedUser caller, UUID tenantId, String userId) {
        TenantMembershipReference membership = getVisibleMembership(caller, tenantId, userId);
        if (!STATUS_INVITED.equals(membership.status())) {
            throw new InvalidTenantUserOperationException("Invite can only be resent for pending users");
        }
        identityUserLifecycleService.executeActionsEmail(userId, List.of("UPDATE_PASSWORD"));
        return toSummary(membership);
    }

    @Transactional
    public TenantUserSummary blockUser(AuthenticatedUser caller, UUID tenantId, String userId) {
        TenantMembershipReference current = getVisibleMembership(caller, tenantId, userId);
        assertNotLastActiveAdmin(tenantId, current.userSubject(), current.role(), STATUS_BLOCKED);
        TenantUserSummary summary = toSummary(tenantUserAccessService.block(tenantId, userId));
        recordAudit(tenantId, caller.subject(), "USER_BLOCKED", userId, null,
                Map.of(DIFF_KEY_STATUS, current.status()), Map.of(DIFF_KEY_STATUS, STATUS_BLOCKED));
        return summary;
    }

    @Transactional
    public void removeUser(AuthenticatedUser caller, UUID tenantId, String userId) {
        TenantMembershipReference current = getVisibleMembership(caller, tenantId, userId);
        assertNotLastActiveAdmin(tenantId, current.userSubject(), current.role(), STATUS_REMOVED);
        tenantUserAccessService.remove(tenantId, userId);
        productUserAccessService.removeTenantAssignments(tenantId, userId);
        if (!tenantUserAccessService.hasOtherActiveMembership(userId, tenantId)) {
            identityUserLifecycleService.setUserEnabled(userId, false);
        }
        recordAudit(tenantId, caller.subject(), "USER_REMOVED_FROM_TENANT", userId, null,
                Map.of(DIFF_KEY_STATUS, current.status()), Map.of(DIFF_KEY_STATUS, STATUS_REMOVED));
    }

    @Transactional
    public TenantUserSummary restoreUser(AuthenticatedUser caller, UUID tenantId, String userId) {
        TenantMembershipReference current = getVisibleMembership(caller, tenantId, userId, true);
        if (!STATUS_REMOVED.equals(current.status()) && !STATUS_BLOCKED.equals(current.status())) {
            throw new InvalidTenantUserOperationException("Only removed or blocked users can be restored");
        }
        TenantMembershipReference restored = tenantUserAccessService.restore(tenantId, userId);
        identityUserLifecycleService.setUserEnabled(userId, true);
        identityUserLifecycleService.executeActionsEmail(userId, List.of("UPDATE_PASSWORD"));
        recordAudit(tenantId, caller.subject(), "USER_RESTORED_TO_TENANT", userId, null,
                Map.of(DIFF_KEY_STATUS, current.status()), Map.of(DIFF_KEY_STATUS, STATUS_ACTIVE));
        return toSummary(restored);
    }

    private TenantMembershipReference getVisibleMembership(AuthenticatedUser caller, UUID tenantId, String userId) {
        return getVisibleMembership(caller, tenantId, userId, false);
    }

    private TenantMembershipReference getVisibleMembership(AuthenticatedUser caller, UUID tenantId, String userId,
                                                           boolean includeRemoved) {
        assertTenantVisible(caller, tenantId);
        TenantMembershipReference membership = tenantUserAccessService.findMembership(tenantId, userId)
                .filter(reference -> includeRemoved || !STATUS_REMOVED.equals(reference.status()))
                .orElseThrow(TenantUserNotFoundException::new);
        Set<String> visibleSubjects = visibleSubjects(caller, tenantId);
        if (!visibleSubjects.isEmpty() && !visibleSubjects.contains(userId)) {
            throw new TenantUserNotFoundException();
        }
        return membership;
    }

    private void assertTenantVisible(AuthenticatedUser caller, UUID tenantId) {
        tenantUserAccessService.getRequiredTenant(tenantId);
        if (isSuperAdmin(caller) || tenantUserAccessService.hasActiveMembership(tenantId, caller.subject())) {
            return;
        }
        throw new TenantUserNotFoundException();
    }

    private void assertCanInvite(AuthenticatedUser caller, UUID tenantId) {
        if (isSuperAdmin(caller)) {
            return;
        }
        if (isTenantAdmin(caller)) {
            return;
        }
        Set<String> sharedSubjects = productUserAccessService.listSharedUserSubjects(tenantId, caller.subject());
        if (sharedSubjects.contains(caller.subject())) {
            return;
        }
        throw new TenantUserNotFoundException();
    }

    private Set<String> visibleSubjects(AuthenticatedUser caller, UUID tenantId) {
        if (isSuperAdmin(caller) || isTenantAdmin(caller)) {
            return Set.of();
        }
        return productUserAccessService.listSharedUserSubjects(tenantId, caller.subject());
    }

    private void assertNotLastActiveAdmin(UUID tenantId, String userSubject, String nextRole, String nextStatus) {
        if (tenantUserAccessService.wouldRemoveLastActiveAdmin(tenantId, userSubject, nextRole, nextStatus)) {
            throw new LastTenantAdminException();
        }
    }

    private TenantUserSummary toSummary(TenantMembershipReference membership) {
        return toSummary(membership, identityUserLifecycleService.getRequiredUser(membership.userSubject()));
    }

    private TenantUserSummary toSummary(TenantMembershipReference membership, IdentityUser user) {
        List<ProductUserAccess> assignments = productUserAccessService.listTenantAssignments(
                membership.tenantId(),
                membership.userSubject()
        );
        return userMapper.toSummary(membership, user, assignments);
    }

    private boolean isSuperAdmin(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_SUPER_ADMIN);
    }

    private boolean isTenantAdmin(AuthenticatedUser caller) {
        return caller.authorities().contains(ROLE_TENANT_ADMIN);
    }

    private String parseRole(String role) {
        String normalized = String.valueOf(role).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case SUPER_ADMIN, TENANT_ADMIN, "PRODUCT_MANAGER", "EDITOR", "VIEWER" -> normalized;
            default -> throw new InvalidTenantUserOperationException("Invalid user role: " + role);
        };
    }

    private String parseStatus(String status) {
        return switch (String.valueOf(status).trim().toLowerCase(Locale.ROOT)) {
            case STATUS_ACTIVE, STATUS_ACTIVE_ALIAS -> STATUS_ACTIVE;
            case STATUS_INVITED, STATUS_INVITED_ALIAS -> STATUS_INVITED;
            case STATUS_BLOCKED, STATUS_BLOCKED_ALIAS -> STATUS_BLOCKED;
            case STATUS_REMOVED, STATUS_REMOVED_ALIAS -> STATUS_REMOVED;
            default -> throw new InvalidTenantUserOperationException("Invalid user status: " + status);
        };
    }

    private void recordAudit(UUID tenantId, String actorSubject, String action, String targetSubject,
                             String targetLabel, Map<String, Object> before, Map<String, Object> after) {
        auditService.recordEvent(new AuditRecordCommand(
                tenantId, null, actorSubject, action, "User", targetSubject, targetLabel, null, before, after
        ));
    }
}
