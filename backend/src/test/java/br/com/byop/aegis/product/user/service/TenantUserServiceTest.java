package br.com.byop.aegis.product.user.service;

import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.identity.api.IdentityActionInviteCommand;
import br.com.byop.aegis.identity.api.IdentityActionTokenService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserLifecycleService;
import br.com.byop.aegis.notification.api.NotificationOnboardingService;
import br.com.byop.aegis.product.api.ProductUserAccessService;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.repository.ProductRepository;
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
import br.com.byop.aegis.tenant.api.TenantReference;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUserServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private IdentityUserLifecycleService identityUserLifecycleService;

    @Mock
    private IdentityActionTokenService identityActionTokenService;

    @Mock
    private TenantUserAccessService tenantUserAccessService;

    @Mock
    private ProductUserAccessService productUserAccessService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TenantUserMapper userMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationOnboardingService notificationOnboardingService;

    @InjectMocks
    private TenantUserService service;

    @Test
    void shouldListAllUsersForTenantAdmin() {
        AuthenticatedUser caller = caller("admin", "ROLE_TENANT_ADMIN");
        TenantMembershipReference membership = membership("user-1", "EDITOR", "ativo");
        IdentityUser user = user("user-1");
        TenantUserSummary summary = summary("user-1", "ativo");
        visibleTenant(caller);
        when(tenantUserAccessService.listMemberships(TENANT_ID)).thenReturn(List.of(membership));
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user);
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(membership, user, List.of())).thenReturn(summary);

        assertThat(service.listUsers(caller, TENANT_ID)).containsExactly(summary);
    }

    @Test
    void shouldFilterUsersForProductScopedCaller() {
        AuthenticatedUser caller = caller("pm", "ROLE_PRODUCT_MANAGER");
        TenantMembershipReference visible = membership("user-1", "EDITOR", "ativo");
        TenantMembershipReference hidden = membership("user-2", "VIEWER", "ativo");
        visibleTenant(caller);
        when(productUserAccessService.listSharedUserSubjects(TENANT_ID, "pm")).thenReturn(Set.of("user-1"));
        when(tenantUserAccessService.listMemberships(TENANT_ID)).thenReturn(List.of(visible, hidden));
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(visible, user("user-1"), List.of())).thenReturn(summary("user-1", "ativo"));

        assertThat(service.listUsers(caller, TENANT_ID)).hasSize(1);

        verify(identityUserLifecycleService, never()).getRequiredUser("user-2");
    }

    @Test
    void shouldHideCallerOutsideTenant() {
        AuthenticatedUser caller = caller("outsider", "ROLE_VIEWER");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.hasActiveMembership(TENANT_ID, "outsider")).thenReturn(false);

        assertThatThrownBy(() -> service.listUsers(caller, TENANT_ID))
                .isInstanceOf(TenantUserNotFoundException.class);
    }

    @Test
    void shouldInviteUser() {
        AuthenticatedUser caller = caller("admin", "ROLE_TENANT_ADMIN");
        InviteTenantUserRequest request = new InviteTenantUserRequest(
                "Guest User",
                "guest@byop.dev",
                "EDITOR",
                "Maestro Beton",
                List.of(PRODUCT_ID),
                null
        );
        TenantMembershipReference membership = membership("user-1", "EDITOR", "convidado");
        Product product = product(PRODUCT_ID, "conecta-talentos", "Conecta Talentos");
        visibleTenant(caller);
        when(identityUserLifecycleService.findByEmail("guest@byop.dev")).thenReturn(Optional.empty());
        when(identityUserLifecycleService.invite("guest@byop.dev", "Guest User")).thenReturn(user("user-1"));
        when(tenantUserAccessService.hasAnyMembership(TENANT_ID, "user-1")).thenReturn(false);
        when(tenantUserAccessService.invite(TENANT_ID, "user-1", "EDITOR")).thenReturn(membership);
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(membership, user("user-1"), List.of())).thenReturn(summary("user-1", "convidado"));

        assertThat(service.inviteUser(caller, TENANT_ID, request).status()).isEqualTo("convidado");
        verify(productUserAccessService).inviteTenantAssignments(TENANT_ID, "user-1", "EDITOR", List.of(PRODUCT_ID));
        org.mockito.ArgumentCaptor<IdentityActionInviteCommand> inviteCaptor =
                org.mockito.ArgumentCaptor.forClass(IdentityActionInviteCommand.class);
        verify(identityActionTokenService).sendInviteActivation(inviteCaptor.capture());
        assertThat(inviteCaptor.getValue().productNames()).containsExactly("Conecta Talentos");
        verify(notificationOnboardingService).assignOnboarding("user-1");
        org.mockito.ArgumentCaptor<br.com.byop.aegis.audit.api.AuditRecordCommand> auditCaptor =
                org.mockito.ArgumentCaptor.forClass(br.com.byop.aegis.audit.api.AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("USER_INVITED_TO_TENANT");
        assertThat(auditCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(auditCaptor.getValue().targetId()).isEqualTo("user-1");
        verify(notificationOnboardingService).assignOnboarding("user-1");
    }

    @Test
    void shouldSplitInviteProductNamesDefensively() {
        assertThat((List<String>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service,
                "splitProductNames",
                (String) null
        )).isEmpty();
        assertThat((List<String>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service,
                "splitProductNames",
                " , Aegis, , Forms "
        )).containsExactly("Aegis", "Forms");
    }

    @Test
    void shouldInviteUserAsSuperAdmin() {
        AuthenticatedUser caller = caller("root", "ROLE_SUPER_ADMIN");
        InviteTenantUserRequest request = new InviteTenantUserRequest("Guest User", "guest@byop.dev", "EDITOR", "Aegis", null);
        TenantMembershipReference membership = membership("user-1", "EDITOR", "convidado");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(identityUserLifecycleService.findByEmail("guest@byop.dev")).thenReturn(Optional.empty());
        when(identityUserLifecycleService.invite("guest@byop.dev", "Guest User")).thenReturn(user("user-1"));
        when(tenantUserAccessService.hasAnyMembership(TENANT_ID, "user-1")).thenReturn(false);
        when(tenantUserAccessService.invite(TENANT_ID, "user-1", "EDITOR")).thenReturn(membership);
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(membership, user("user-1"), List.of())).thenReturn(summary("user-1", "convidado"));

        assertThat(service.inviteUser(caller, TENANT_ID, request).status()).isEqualTo("convidado");
    }

    @Test
    void shouldRejectDuplicateEmailInTenant() {
        AuthenticatedUser caller = caller("admin", "ROLE_TENANT_ADMIN");
        InviteTenantUserRequest request = new InviteTenantUserRequest("Guest User", "guest@byop.dev", "EDITOR", "Aegis", null);
        visibleTenant(caller);
        when(identityUserLifecycleService.findByEmail("guest@byop.dev")).thenReturn(Optional.of(user("user-1")));
        when(tenantUserAccessService.hasAnyMembership(TENANT_ID, "user-1")).thenReturn(true);

        assertThatThrownBy(() -> service.inviteUser(caller, TENANT_ID, request))
                .isInstanceOf(TenantUserAlreadyExistsException.class);
    }

    @Test
    void shouldRejectDuplicateMembershipAfterKeycloakInvite() {
        AuthenticatedUser caller = caller("admin", "ROLE_TENANT_ADMIN");
        InviteTenantUserRequest request = new InviteTenantUserRequest("Guest User", "guest@byop.dev", "EDITOR", "Aegis", null);
        visibleTenant(caller);
        when(identityUserLifecycleService.findByEmail("guest@byop.dev")).thenReturn(Optional.empty());
        when(identityUserLifecycleService.invite("guest@byop.dev", "Guest User")).thenReturn(user("user-1"));
        when(tenantUserAccessService.hasAnyMembership(TENANT_ID, "user-1")).thenReturn(true);

        assertThatThrownBy(() -> service.inviteUser(caller, TENANT_ID, request))
                .isInstanceOf(TenantUserAlreadyExistsException.class);
    }

    @Test
    void shouldAllowProductManagerInviteWhenCallerSharesProduct() {
        AuthenticatedUser caller = caller("pm", "ROLE_PRODUCT_MANAGER");
        InviteTenantUserRequest request = new InviteTenantUserRequest("Guest User", "guest@byop.dev", "VIEWER", "Aegis", null);
        TenantMembershipReference membership = membership("user-1", "VIEWER", "convidado");
        visibleTenant(caller);
        when(productUserAccessService.listSharedUserSubjects(TENANT_ID, "pm")).thenReturn(Set.of("pm"));
        when(identityUserLifecycleService.findByEmail("guest@byop.dev")).thenReturn(Optional.empty());
        when(identityUserLifecycleService.invite("guest@byop.dev", "Guest User")).thenReturn(user("user-1"));
        when(tenantUserAccessService.hasAnyMembership(TENANT_ID, "user-1")).thenReturn(false);
        when(tenantUserAccessService.invite(TENANT_ID, "user-1", "VIEWER")).thenReturn(membership);
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(membership, user("user-1"), List.of())).thenReturn(summary("user-1", "convidado"));

        assertThat(service.inviteUser(caller, TENANT_ID, request).userId()).isEqualTo("user-1");
        verify(notificationOnboardingService).assignOnboarding("user-1");
    }

    @Test
    void shouldRejectProductScopedInviteWhenCallerDoesNotShareProduct() {
        AuthenticatedUser caller = caller("editor", "ROLE_EDITOR");
        InviteTenantUserRequest request = new InviteTenantUserRequest("Guest User", "guest@byop.dev", "VIEWER", "Aegis", null);
        visibleTenant(caller);
        when(productUserAccessService.listSharedUserSubjects(TENANT_ID, "editor")).thenReturn(Set.of("other"));

        assertThatThrownBy(() -> service.inviteUser(caller, TENANT_ID, request))
                .isInstanceOf(TenantUserNotFoundException.class);
    }

    @Test
    void shouldUpdateUserAndRejectLastAdmin() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference current = membership("user-1", "TENANT_ADMIN", "ativo");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(current));
        when(tenantUserAccessService.wouldRemoveLastActiveAdmin(TENANT_ID, "user-1", "VIEWER", "ativo")).thenReturn(true);
        UpdateTenantUserRequest request = new UpdateTenantUserRequest("VIEWER", "Aegis", "ativo");

        assertThatThrownBy(() -> service.updateUser(caller, TENANT_ID, "user-1", request))
                .isInstanceOf(LastTenantAdminException.class);
    }

    @Test
    void shouldGetAndUpdateUser() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference current = membership("user-1", "EDITOR", "ativo");
        TenantMembershipReference updated = membership("user-1", "VIEWER", "bloqueado");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(current));
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(current, user("user-1"), List.of())).thenReturn(summary("user-1", "ativo"));
        when(userMapper.toSummary(updated, user("user-1"), List.of())).thenReturn(summary("user-1", "bloqueado"));
        when(tenantUserAccessService.update(TENANT_ID, "user-1", "VIEWER", "bloqueado")).thenReturn(updated);

        assertThat(service.getUser(caller, TENANT_ID, "user-1").status()).isEqualTo("ativo");
        assertThat(service.updateUser(caller, TENANT_ID, "user-1",
                new UpdateTenantUserRequest("VIEWER", "Aegis", "bloqueado")).status()).isEqualTo("bloqueado");
    }

    @Test
    void shouldHideUserOutsideProductScope() {
        AuthenticatedUser caller = caller("pm", "ROLE_PRODUCT_MANAGER");
        visibleTenant(caller);
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(membership("user-1", "EDITOR", "ativo")));
        when(productUserAccessService.listSharedUserSubjects(TENANT_ID, "pm")).thenReturn(Set.of("other"));

        assertThatThrownBy(() -> service.getUser(caller, TENANT_ID, "user-1"))
                .isInstanceOf(TenantUserNotFoundException.class);
    }

    @Test
    void shouldGetUserWhenProductScopedCallerSharesProduct() {
        AuthenticatedUser caller = caller("pm", "ROLE_PRODUCT_MANAGER");
        TenantMembershipReference current = membership("user-1", "EDITOR", "ativo");
        visibleTenant(caller);
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(current));
        when(productUserAccessService.listSharedUserSubjects(TENANT_ID, "pm")).thenReturn(Set.of("user-1"));
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(current, user("user-1"), List.of())).thenReturn(summary("user-1", "ativo"));

        assertThat(service.getUser(caller, TENANT_ID, "user-1").userId()).isEqualTo("user-1");
    }

    @Test
    void shouldHideRemovedUserOnDefaultDetailLookup() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(membership("user-1", "EDITOR", "removido")));

        assertThatThrownBy(() -> service.getUser(caller, TENANT_ID, "user-1"))
                .isInstanceOf(TenantUserNotFoundException.class);
    }

    @Test
    void shouldRejectInvalidRoleAndStatus() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference current = membership("user-1", "EDITOR", "ativo");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(current));
        UpdateTenantUserRequest invalidRole = new UpdateTenantUserRequest("invalid", "Aegis", "ativo");
        UpdateTenantUserRequest invalidStatus = new UpdateTenantUserRequest("VIEWER", "Aegis", "invalid");

        assertThatThrownBy(() -> service.updateUser(caller, TENANT_ID, "user-1", invalidRole))
                .isInstanceOf(InvalidTenantUserOperationException.class);
        assertThatThrownBy(() -> service.updateUser(caller, TENANT_ID, "user-1", invalidStatus))
                .isInstanceOf(InvalidTenantUserOperationException.class);
    }

    @Test
    void shouldAcceptAllEditableStatuses() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference current = membership("user-1", "EDITOR", "ativo");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(current));
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        for (String status : List.of("convidado", "removido")) {
            TenantMembershipReference updated = membership("user-1", "VIEWER", status);
            when(tenantUserAccessService.update(TENANT_ID, "user-1", "VIEWER", status)).thenReturn(updated);
            when(userMapper.toSummary(updated, user("user-1"), List.of())).thenReturn(summary("user-1", status));
            assertThat(service.updateUser(caller, TENANT_ID, "user-1",
                    new UpdateTenantUserRequest("VIEWER", "Aegis", status)).status()).isEqualTo(status);
        }
    }

    @Test
    void shouldAllowTenantAdminInvite() {
        AuthenticatedUser caller = caller("tenant-admin", "ROLE_TENANT_ADMIN");
        InviteTenantUserRequest request = new InviteTenantUserRequest("Guest User", "guest@byop.dev", "VIEWER", "Aegis", null);
        TenantMembershipReference membership = membership("user-1", "VIEWER", "convidado");
        visibleTenant(caller);
        when(identityUserLifecycleService.findByEmail("guest@byop.dev")).thenReturn(Optional.empty());
        when(identityUserLifecycleService.invite("guest@byop.dev", "Guest User")).thenReturn(user("user-1"));
        when(tenantUserAccessService.hasAnyMembership(TENANT_ID, "user-1")).thenReturn(false);
        when(tenantUserAccessService.invite(TENANT_ID, "user-1", "VIEWER")).thenReturn(membership);
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(membership, user("user-1"), List.of())).thenReturn(summary("user-1", "convidado"));

        assertThat(service.inviteUser(caller, TENANT_ID, request).status()).isEqualTo("convidado");
    }

    @Test
    void shouldResendInviteOnlyForPendingUser() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference membership = membership("user-1", "EDITOR", "convidado");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(membership));
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(membership, user("user-1"), List.of())).thenReturn(summary("user-1", "convidado"));

        service.resendInvite(caller, TENANT_ID, "user-1");

        verify(identityActionTokenService).sendInviteActivation(any(IdentityActionInviteCommand.class));
    }

    @Test
    void shouldRejectResendForActiveUser() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(membership("user-1", "EDITOR", "ativo")));

        assertThatThrownBy(() -> service.resendInvite(caller, TENANT_ID, "user-1"))
                .isInstanceOf(InvalidTenantUserOperationException.class);
    }

    @Test
    void shouldBlockRemoveAndRestoreUser() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference active = membership("user-1", "EDITOR", "ativo");
        TenantMembershipReference blocked = membership("user-1", "EDITOR", "bloqueado");
        TenantMembershipReference removed = membership("user-1", "EDITOR", "removido");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1"))
                .thenReturn(Optional.of(active), Optional.of(active), Optional.of(removed));
        when(tenantUserAccessService.block(TENANT_ID, "user-1")).thenReturn(blocked);
        when(tenantUserAccessService.hasOtherActiveMembership("user-1", TENANT_ID)).thenReturn(false);
        when(tenantUserAccessService.restore(TENANT_ID, "user-1")).thenReturn(active);
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(blocked, user("user-1"), List.of())).thenReturn(summary("user-1", "bloqueado"));
        when(userMapper.toSummary(active, user("user-1"), List.of())).thenReturn(summary("user-1", "ativo"));

        assertThat(service.blockUser(caller, TENANT_ID, "user-1").status()).isEqualTo("bloqueado");
        service.removeUser(caller, TENANT_ID, "user-1");
        assertThat(service.restoreUser(caller, TENANT_ID, "user-1").status()).isEqualTo("ativo");

        verify(productUserAccessService).removeTenantAssignments(TENANT_ID, "user-1");
        verify(identityUserLifecycleService).setUserEnabled("user-1", false);
        verify(identityUserLifecycleService).setUserEnabled("user-1", true);
        verify(identityActionTokenService).sendInviteActivation(any(IdentityActionInviteCommand.class));
        verify(auditService, times(3)).recordEvent(any());
    }

    @Test
    void shouldNotDisableKeycloakWhenUserHasOtherTenantAndRejectActiveRestore() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference active = membership("user-1", "EDITOR", "ativo");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(active));
        when(tenantUserAccessService.hasOtherActiveMembership("user-1", TENANT_ID)).thenReturn(true);

        service.removeUser(caller, TENANT_ID, "user-1");

        verify(identityUserLifecycleService, never()).setUserEnabled("user-1", false);

        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> service.restoreUser(caller, TENANT_ID, "user-1"))
                .isInstanceOf(InvalidTenantUserOperationException.class);
    }

    @Test
    void shouldRestoreBlockedUser() {
        AuthenticatedUser caller = caller("admin", "ROLE_SUPER_ADMIN");
        TenantMembershipReference blocked = membership("user-1", "EDITOR", "bloqueado");
        TenantMembershipReference active = membership("user-1", "EDITOR", "ativo");
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.findMembership(TENANT_ID, "user-1")).thenReturn(Optional.of(blocked));
        when(tenantUserAccessService.restore(TENANT_ID, "user-1")).thenReturn(active);
        when(identityUserLifecycleService.getRequiredUser("user-1")).thenReturn(user("user-1"));
        when(productUserAccessService.listTenantAssignments(TENANT_ID, "user-1")).thenReturn(List.of());
        when(userMapper.toSummary(active, user("user-1"), List.of())).thenReturn(summary("user-1", "ativo"));

        assertThat(service.restoreUser(caller, TENANT_ID, "user-1").status()).isEqualTo("ativo");
    }

    private void visibleTenant(AuthenticatedUser caller) {
        when(tenantUserAccessService.getRequiredTenant(TENANT_ID)).thenReturn(new TenantReference(TENANT_ID, "BYOP"));
        when(tenantUserAccessService.hasActiveMembership(TENANT_ID, caller.subject())).thenReturn(true);
    }

    private AuthenticatedUser caller(String subject, String authority) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of(authority));
    }

    private TenantMembershipReference membership(String subject, String role, String status) {
        return new TenantMembershipReference(UUID.nameUUIDFromBytes(subject.getBytes()), TENANT_ID, "BYOP", subject, role, status,
                OffsetDateTime.parse("2026-06-27T10:00:00Z"), OffsetDateTime.parse("2026-06-27T10:00:00Z"));
    }

    private IdentityUser user(String subject) {
        return new IdentityUser(subject, subject, subject + "@byop.dev", "User", subject);
    }

    private Product product(UUID id, String key, String name) {
        Product product = new Product(TENANT_ID, key, name, ProductTypeKey.PRODUTO_SAAS, "pt-BR");
        org.springframework.test.util.ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private TenantUserSummary summary(String subject, String status) {
        return new TenantUserSummary(subject, "User " + subject, subject + "@byop.dev", "editor", "", status, "nunca", "ativo");
    }
}
