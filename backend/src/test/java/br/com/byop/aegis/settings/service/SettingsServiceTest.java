package br.com.byop.aegis.settings.service;

import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.api.ProductAccessScope;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.product.api.ProductVisibilityService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.settings.contract.PermissionMatrixPreviewRequest;
import br.com.byop.aegis.settings.contract.UpdateProductSettingsRequest;
import br.com.byop.aegis.settings.domain.RolePermission;
import br.com.byop.aegis.settings.dto.RoleMatrixEntry;
import br.com.byop.aegis.settings.dto.SettingCard;
import br.com.byop.aegis.settings.exception.InsufficientSettingsRoleException;
import br.com.byop.aegis.settings.exception.InvalidSettingsRoleException;
import br.com.byop.aegis.settings.exception.SettingsNotFoundException;
import br.com.byop.aegis.settings.mapper.RolePermissionMapper;
import br.com.byop.aegis.settings.repository.RolePermissionRepository;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SettingsServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final RolePermissionMapper rolePermissionMapper = Mappers.getMapper(RolePermissionMapper.class);

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private ProductVisibilityService productVisibilityService;

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private AuditService auditService;

    private SettingsService service;

    private SettingsService service() {
        return new SettingsService(rolePermissionRepository, rolePermissionMapper, productVisibilityService,
                productReferenceService, tenantAccessService, auditService);
    }

    @Test
    void shouldReturnOverviewForSuperAdmin() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        when(productVisibilityService.listVisibleProducts(caller))
                .thenReturn(List.of(new ProductAccessScope(PRODUCT_ID, TENANT_ID, "ACTIVE")));
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        List<SettingCard> overview = service.getOverview(caller, PRODUCT_ID);

        assertThat(overview).hasSize(8);
        assertThat(overview.getFirst().name()).isEqualTo("Produto");
        assertThat(overview.getFirst().status()).isEqualTo("configurado");
    }

    @Test
    void shouldReturnOverviewWithAttentionStatusAndCustomPermissionsForTenantAdmin() {
        service = service();
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        when(productVisibilityService.listVisibleProducts(caller))
                .thenReturn(List.of(new ProductAccessScope(PRODUCT_ID, TENANT_ID, "ARCHIVED")));
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID))
                .thenReturn(List.of(new RolePermission(TENANT_ID, "EDITOR", "/content", false)));

        List<SettingCard> overview = service.getOverview(caller, PRODUCT_ID);

        SettingCard productCard = overview.getFirst();
        assertThat(productCard.status()).isEqualTo("atenção");
        SettingCard permissionsCard = overview.stream()
                .filter(card -> card.name().equals("Permissões"))
                .findFirst()
                .orElseThrow();
        assertThat(permissionsCard.status()).isEqualTo("customizado");
    }

    @Test
    void shouldRejectOverviewForEditor() {
        service = service();
        AuthenticatedUser caller = user("ROLE_EDITOR");
        when(productVisibilityService.listVisibleProducts(caller))
                .thenReturn(List.of(new ProductAccessScope(PRODUCT_ID, TENANT_ID, "ACTIVE")));

        assertThatThrownBy(() -> service.getOverview(caller, PRODUCT_ID))
                .isInstanceOf(InsufficientSettingsRoleException.class);
    }

    @Test
    void shouldReturnNotFoundWhenProductOutOfScope() {
        service = service();
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getOverview(caller, PRODUCT_ID))
                .isInstanceOf(SettingsNotFoundException.class);
    }

    @Test
    void shouldUpdateProductSettingsAndRecordAudit() {
        service = service();
        AuthenticatedUser caller = user("ROLE_PRODUCT_MANAGER");
        when(productVisibilityService.listVisibleProducts(caller))
                .thenReturn(List.of(new ProductAccessScope(PRODUCT_ID, TENANT_ID, "ACTIVE")));
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        List<SettingCard> overview = service.updateProductSettings(caller, PRODUCT_ID,
                new UpdateProductSettingsRequest("Novo Nome"));

        verify(productReferenceService).renameProduct(PRODUCT_ID, "Novo Nome");
        verify(auditService).recordEvent(any());
        assertThat(overview).hasSize(8);
    }

    @Test
    void shouldRejectUpdateProductSettingsForViewer() {
        service = service();
        AuthenticatedUser caller = user("ROLE_VIEWER");
        when(productVisibilityService.listVisibleProducts(caller))
                .thenReturn(List.of(new ProductAccessScope(PRODUCT_ID, TENANT_ID, "ACTIVE")));

        UpdateProductSettingsRequest request = new UpdateProductSettingsRequest("Novo Nome");

        assertThatThrownBy(() -> service.updateProductSettings(caller, PRODUCT_ID, request))
                .isInstanceOf(InsufficientSettingsRoleException.class);
        verify(productReferenceService, never()).renameProduct(any(), any());
    }

    @Test
    void shouldReturnDefaultMatrixWhenNoOverridesExist() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        List<RoleMatrixEntry> matrix = service.getRoleMatrix(caller, TENANT_ID);

        assertThat(matrix).hasSize(5);
        RoleMatrixEntry tenantAdmin = matrix.stream().filter(entry -> entry.role().equals("TENANT_ADMIN")).findFirst().orElseThrow();
        assertThat(tenantAdmin.permissions().get("/settings/roles")).isTrue();
        RoleMatrixEntry viewer = matrix.stream().filter(entry -> entry.role().equals("VIEWER")).findFirst().orElseThrow();
        assertThat(viewer.permissions().get("/settings/roles")).isFalse();
    }

    @Test
    void shouldReturnPersistedMatrixWhenOverridesExist() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        RolePermission override = new RolePermission(TENANT_ID, "EDITOR", "/content", false);
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(override));

        List<RoleMatrixEntry> matrix = service.getRoleMatrix(caller, TENANT_ID);

        RoleMatrixEntry editor = matrix.stream().filter(entry -> entry.role().equals("EDITOR")).findFirst().orElseThrow();
        assertThat(editor.permissions().get("/content")).isFalse();
    }

    @Test
    void shouldRejectRoleMatrixForProductManager() {
        service = service();
        AuthenticatedUser caller = user("ROLE_PRODUCT_MANAGER");

        assertThatThrownBy(() -> service.getRoleMatrix(caller, TENANT_ID))
                .isInstanceOf(InsufficientSettingsRoleException.class);
    }

    @Test
    void shouldReturnNotFoundWhenTenantAdminHasNoMembership() {
        service = service();
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        when(tenantAccessService.hasActiveMembership(TENANT_ID, "user-1")).thenReturn(false);

        assertThatThrownBy(() -> service.getRoleMatrix(caller, TENANT_ID))
                .isInstanceOf(SettingsNotFoundException.class);
    }

    @Test
    void shouldAllowTenantAdminWithActiveMembership() {
        service = service();
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        when(tenantAccessService.hasActiveMembership(TENANT_ID, "user-1")).thenReturn(true);
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        assertThat(service.getRoleMatrix(caller, TENANT_ID)).hasSize(5);
    }

    @Test
    void shouldUpsertRoleMatrixEntries() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        RolePermission existingOverride = new RolePermission(TENANT_ID, "VIEWER", "/content", true);
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(existingOverride));
        when(rolePermissionRepository.findByTenantIdAndRoleAndPermissionKey(TENANT_ID, "EDITOR", "/settings"))
                .thenReturn(Optional.empty());

        service.updateRoleMatrix(caller, TENANT_ID, List.of(new RoleMatrixEntry("editor", java.util.Map.of("/settings", true))));

        verify(rolePermissionRepository).save(any(RolePermission.class));
        verify(auditService).recordEvent(any());
    }

    @Test
    void shouldUpdateExistingRoleMatrixEntry() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        RolePermission existing = new RolePermission(TENANT_ID, "EDITOR", "/settings", false);
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());
        when(rolePermissionRepository.findByTenantIdAndRoleAndPermissionKey(TENANT_ID, "EDITOR", "/settings"))
                .thenReturn(Optional.of(existing));

        service.updateRoleMatrix(caller, TENANT_ID, List.of(new RoleMatrixEntry("EDITOR", java.util.Map.of("/settings", true))));

        assertThat(existing.isAllowed()).isTrue();
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
    }

    @Test
    void shouldRejectInvalidRoleOnUpdate() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");

        List<RoleMatrixEntry> entries = List.of(new RoleMatrixEntry("INTERN", java.util.Map.of("/settings", true)));

        assertThatThrownBy(() -> service.updateRoleMatrix(caller, TENANT_ID, entries))
                .isInstanceOf(InvalidSettingsRoleException.class);
    }

    @Test
    void shouldPreviewPersistedRole() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        RolePermission persisted = new RolePermission(TENANT_ID, "VIEWER", "/content", true);
        when(rolePermissionRepository.findAllByTenantIdAndRole(TENANT_ID, "VIEWER")).thenReturn(List.of(persisted));

        RoleMatrixEntry preview = service.previewPermissionMatrix(caller, TENANT_ID,
                new PermissionMatrixPreviewRequest("viewer"));

        assertThat(preview.role()).isEqualTo("VIEWER");
        assertThat(preview.permissions().get("/content")).isTrue();
    }

    @Test
    void shouldPreviewDefaultRoleWhenNoOverridesExist() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        when(rolePermissionRepository.findAllByTenantIdAndRole(TENANT_ID, "EDITOR")).thenReturn(List.of());

        RoleMatrixEntry preview = service.previewPermissionMatrix(caller, TENANT_ID,
                new PermissionMatrixPreviewRequest("EDITOR"));

        assertThat(preview.permissions().get("/content")).isTrue();
        assertThat(preview.permissions().get("/audit")).isFalse();
    }

    @Test
    void shouldRejectPreviewForInvalidRole() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");

        PermissionMatrixPreviewRequest request = new PermissionMatrixPreviewRequest("INTERN");

        assertThatThrownBy(() -> service.previewPermissionMatrix(caller, TENANT_ID, request))
                .isInstanceOf(InvalidSettingsRoleException.class);
    }

    @Test
    void shouldRestoreDefaultPermissions() {
        service = service();
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        when(rolePermissionRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        List<RoleMatrixEntry> restored = service.restoreDefaultPermissions(caller, TENANT_ID);

        verify(rolePermissionRepository).deleteAllByTenantId(TENANT_ID);
        verify(rolePermissionRepository, times(
                PermissionMatrixDefaults.CANONICAL_ROLES.size() * PermissionMatrixDefaults.PERMISSION_KEYS.size()
        )).save(any(RolePermission.class));
        verify(auditService).recordEvent(any());
        assertThat(restored).hasSize(5);
    }

    @Test
    void shouldRejectRestoreDefaultsForNonAdminRole() {
        service = service();
        AuthenticatedUser caller = user("ROLE_EDITOR");

        assertThatThrownBy(() -> service.restoreDefaultPermissions(caller, TENANT_ID))
                .isInstanceOf(InsufficientSettingsRoleException.class);
        verify(rolePermissionRepository, never()).deleteAllByTenantId(TENANT_ID);
    }

    private AuthenticatedUser user(String authority) {
        return new AuthenticatedUser("user-1", "user@aegis.app", "user", "User", Set.of(authority));
    }
}
