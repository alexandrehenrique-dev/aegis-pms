package br.com.byop.aegis.settings.controller;

import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.settings.contract.PermissionMatrixPreviewRequest;
import br.com.byop.aegis.settings.contract.UpdateProductSettingsRequest;
import br.com.byop.aegis.settings.dto.RoleMatrixEntry;
import br.com.byop.aegis.settings.dto.SettingCard;
import br.com.byop.aegis.settings.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SettingsController {

    private final SettingsService settingsService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public SettingsController(SettingsService settingsService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.settingsService = settingsService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/settings/overview")
    public List<SettingCard> getOverview(@PathVariable("productId") UUID productId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.getOverview(caller, productId);
    }

    @PutMapping("/api/v1/products/{productId}/settings")
    public List<SettingCard> updateProductSettings(@PathVariable("productId") UUID productId,
                                                   @Valid @RequestBody UpdateProductSettingsRequest request,
                                                   Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.updateProductSettings(caller, productId, request);
    }

    @GetMapping("/api/v1/tenants/{tenantId}/roles")
    public List<RoleMatrixEntry> getRoles(@PathVariable("tenantId") UUID tenantId, Authentication authentication) {
        return resolveRoleMatrix(tenantId, authentication);
    }

    @PutMapping("/api/v1/tenants/{tenantId}/roles")
    public List<RoleMatrixEntry> updateRoles(@PathVariable("tenantId") UUID tenantId,
                                             @RequestBody List<RoleMatrixEntry> request,
                                             Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.updateRoleMatrix(caller, tenantId, request);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/roles/restore-defaults")
    public List<RoleMatrixEntry> restoreDefaultRoles(@PathVariable("tenantId") UUID tenantId, Authentication authentication) {
        return resolveRestoreDefaults(tenantId, authentication);
    }

    @GetMapping("/api/v1/tenants/{tenantId}/permission-matrix")
    public List<RoleMatrixEntry> getPermissionMatrix(@PathVariable("tenantId") UUID tenantId, Authentication authentication) {
        return resolveRoleMatrix(tenantId, authentication);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/permission-matrix/preview")
    public RoleMatrixEntry previewPermissionMatrix(@PathVariable("tenantId") UUID tenantId,
                                                    @Valid @RequestBody PermissionMatrixPreviewRequest request,
                                                    Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.previewPermissionMatrix(caller, tenantId, request);
    }

    @PostMapping("/api/v1/tenants/{tenantId}/permission-matrix/restore-defaults")
    public List<RoleMatrixEntry> restoreDefaultPermissionMatrix(@PathVariable("tenantId") UUID tenantId,
                                                                 Authentication authentication) {
        return resolveRestoreDefaults(tenantId, authentication);
    }

    /**
     * Logica compartilhada por {@code GET .../roles} e {@code GET
     * .../permission-matrix} — as duas telas (RoleManagement.tsx,
     * PermissionMatrixView.tsx) consomem o mesmo dado (etapa 17).
     */
    private List<RoleMatrixEntry> resolveRoleMatrix(UUID tenantId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.getRoleMatrix(caller, tenantId);
    }

    /**
     * Logica compartilhada por {@code POST .../roles/restore-defaults} e
     * {@code POST .../permission-matrix/restore-defaults} — os dois botoes
     * de "restaurar padrao" das duas telas operam sobre o mesmo dado.
     */
    private List<RoleMatrixEntry> resolveRestoreDefaults(UUID tenantId, Authentication authentication) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        return settingsService.restoreDefaultPermissions(caller, tenantId);
    }
}
