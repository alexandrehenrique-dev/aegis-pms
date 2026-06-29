package br.com.byop.aegis.settings.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.api.ProductAccessScope;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.product.api.ProductVisibilityService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.settings.contract.PermissionMatrixPreviewRequest;
import br.com.byop.aegis.settings.contract.UpdateProductSettingsRequest;
import br.com.byop.aegis.settings.domain.RolePermission;
import br.com.byop.aegis.settings.dto.RoleMatrixEntry;
import br.com.byop.aegis.settings.dto.RolePermissionSummary;
import br.com.byop.aegis.settings.dto.SettingCard;
import br.com.byop.aegis.settings.exception.InsufficientSettingsRoleException;
import br.com.byop.aegis.settings.exception.InvalidSettingsRoleException;
import br.com.byop.aegis.settings.exception.SettingsNotFoundException;
import br.com.byop.aegis.settings.mapper.RolePermissionMapper;
import br.com.byop.aegis.settings.repository.RolePermissionRepository;
import br.com.byop.aegis.tenant.api.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestracao do dominio {@code settings}: configuracoes de produto (cards
 * de overview, edicao basica) e a matriz de permissoes por papel/tenant
 * (ADR-0014). Dominio de infraestrutura (Secao 10.6 do padrao de
 * qualidade) — nao usa {@code ProductAccessResolver} (ADR-0018, especifico
 * de conteudo de produto), apenas a regra geral de isolamento por
 * tenant/produto (Secao 10.1-10.4) e a restricao explicita de papel desta
 * etapa.
 */
@Service
public class SettingsService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "ROLE_PRODUCT_MANAGER";
    private static final String TARGET_TYPE_ROLE_PERMISSIONS = "RolePermission";
    private static final String TARGET_TYPE_PRODUCT_SETTINGS = "ProductSettings";
    private static final String MODULE_SETTINGS = "SETTINGS";
    private static final String STATUS_CONFIGURADO = "configurado";
    private static final String RISK_MEDIO = "medio";
    private static final String RISK_ALTO = "alto";
    private static final String OVERVIEW_PLACEHOLDER = "—";

    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionMapper rolePermissionMapper;
    private final ProductVisibilityService productVisibilityService;
    private final ProductReferenceService productReferenceService;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public SettingsService(RolePermissionRepository rolePermissionRepository,
                           RolePermissionMapper rolePermissionMapper,
                           ProductVisibilityService productVisibilityService,
                           ProductReferenceService productReferenceService,
                           TenantAccessService tenantAccessService,
                           AuditService auditService) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.rolePermissionMapper = rolePermissionMapper;
        this.productVisibilityService = productVisibilityService;
        this.productReferenceService = productReferenceService;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SettingCard> getOverview(AuthenticatedUser caller, UUID productId) {
        ProductAccessScope scope = resolveProductScope(caller, productId);
        assertProductSettingsRole(caller);
        return buildOverview(scope);
    }

    @Transactional
    public List<SettingCard> updateProductSettings(AuthenticatedUser caller, UUID productId,
                                                    UpdateProductSettingsRequest request) {
        ProductAccessScope scope = resolveProductScope(caller, productId);
        assertProductSettingsRole(caller);
        productReferenceService.renameProduct(productId, request.name());
        auditService.recordEvent(new AuditRecordCommand(
                scope.tenantId(), productId, caller.subject(), "PRODUCT_SETTINGS_UPDATED",
                TARGET_TYPE_PRODUCT_SETTINGS, productId.toString(), request.name(), MODULE_SETTINGS, null,
                Map.of("name", request.name())
        ));
        return buildOverview(scope);
    }

    @Transactional(readOnly = true)
    public List<RoleMatrixEntry> getRoleMatrix(AuthenticatedUser caller, UUID tenantId) {
        assertRoleMatrixAccess(caller, tenantId);
        return buildMatrix(tenantId);
    }

    @Transactional
    public List<RoleMatrixEntry> updateRoleMatrix(AuthenticatedUser caller, UUID tenantId, List<RoleMatrixEntry> entries) {
        assertRoleMatrixAccess(caller, tenantId);
        Map<String, Object> before = matrixSnapshot(tenantId);

        for (RoleMatrixEntry entry : entries) {
            String role = requireCanonicalRole(entry.role());
            entry.permissions().forEach((permissionKey, allowed) -> upsert(tenantId, role, permissionKey, allowed));
        }

        List<RoleMatrixEntry> updated = buildMatrix(tenantId);
        auditService.recordEvent(new AuditRecordCommand(
                tenantId, null, caller.subject(), "ROLE_PERMISSIONS_UPDATED", TARGET_TYPE_ROLE_PERMISSIONS,
                tenantId.toString(), null, MODULE_SETTINGS, before, matrixSnapshot(tenantId)
        ));
        return updated;
    }

    @Transactional
    public RoleMatrixEntry previewPermissionMatrix(AuthenticatedUser caller, UUID tenantId,
                                                    PermissionMatrixPreviewRequest request) {
        assertRoleMatrixAccess(caller, tenantId);
        String role = requireCanonicalRole(request.role());
        return matrixEntryForRole(tenantId, role);
    }

    @Transactional
    public List<RoleMatrixEntry> restoreDefaultPermissions(AuthenticatedUser caller, UUID tenantId) {
        assertRoleMatrixAccess(caller, tenantId);
        Map<String, Object> before = matrixSnapshot(tenantId);

        rolePermissionRepository.deleteAllByTenantId(tenantId);
        rolePermissionRepository.flush();
        for (String role : PermissionMatrixDefaults.CANONICAL_ROLES) {
            for (String permissionKey : PermissionMatrixDefaults.PERMISSION_KEYS) {
                rolePermissionRepository.save(new RolePermission(
                        tenantId, role, permissionKey, PermissionMatrixDefaults.isAllowedByDefault(role, permissionKey)
                ));
            }
        }

        List<RoleMatrixEntry> restored = buildMatrix(tenantId);
        auditService.recordEvent(new AuditRecordCommand(
                tenantId, null, caller.subject(), "ROLE_PERMISSIONS_RESTORED", TARGET_TYPE_ROLE_PERMISSIONS,
                tenantId.toString(), null, MODULE_SETTINGS, before, matrixSnapshot(tenantId)
        ));
        return restored;
    }

    private ProductAccessScope resolveProductScope(AuthenticatedUser caller, UUID productId) {
        return productVisibilityService.listVisibleProducts(caller)
                .stream()
                .filter(scope -> scope.productId().equals(productId))
                .findFirst()
                .orElseThrow(SettingsNotFoundException::new);
    }

    private void assertProductSettingsRole(AuthenticatedUser caller) {
        if (caller.authorities().contains(ROLE_SUPER_ADMIN)
                || caller.authorities().contains(ROLE_TENANT_ADMIN)
                || caller.authorities().contains(ROLE_PRODUCT_MANAGER)) {
            return;
        }
        throw new InsufficientSettingsRoleException();
    }

    private void assertRoleMatrixAccess(AuthenticatedUser caller, UUID tenantId) {
        if (caller.authorities().contains(ROLE_SUPER_ADMIN)) {
            return;
        }
        if (!caller.authorities().contains(ROLE_TENANT_ADMIN)) {
            throw new InsufficientSettingsRoleException();
        }
        if (!tenantAccessService.hasActiveMembership(tenantId, caller.subject())) {
            throw new SettingsNotFoundException();
        }
    }

    /**
     * Monta a matriz completa do tenant. Cada papel comeca com o catalogo de
     * fabrica inteiro (31 chaves) e recebe por cima apenas as linhas que o
     * tenant de fato customizou — nunca o contrario (so as linhas
     * persistidas), senao customizar uma unica chave de um papel faria as
     * demais 30 chaves (e os outros 4 papeis sem nenhuma linha) desaparecerem
     * da resposta em vez de cair no padrao.
     */
    private List<RoleMatrixEntry> buildMatrix(UUID tenantId) {
        Map<String, Map<String, Boolean>> byRole = new LinkedHashMap<>();
        for (String role : PermissionMatrixDefaults.CANONICAL_ROLES) {
            byRole.put(role, defaultPermissionsForRole(role));
        }
        for (RolePermission permission : rolePermissionRepository.findAllByTenantId(tenantId)) {
            RolePermissionSummary summary = rolePermissionMapper.toSummary(permission);
            byRole.get(summary.role()).put(summary.permissionKey(), summary.allowed());
        }
        return byRole.entrySet().stream()
                .map(roleEntry -> new RoleMatrixEntry(roleEntry.getKey(), Map.copyOf(roleEntry.getValue())))
                .toList();
    }

    private RoleMatrixEntry matrixEntryForRole(UUID tenantId, String role) {
        Map<String, Boolean> permissionMap = defaultPermissionsForRole(role);
        rolePermissionRepository.findAllByTenantIdAndRole(tenantId, role)
                .stream()
                .map(rolePermissionMapper::toSummary)
                .forEach(summary -> permissionMap.put(summary.permissionKey(), summary.allowed()));
        return new RoleMatrixEntry(role, Map.copyOf(permissionMap));
    }

    private Map<String, Boolean> defaultPermissionsForRole(String role) {
        Map<String, Boolean> permissionMap = new LinkedHashMap<>();
        PermissionMatrixDefaults.PERMISSION_KEYS.forEach(permissionKey ->
                permissionMap.put(permissionKey, PermissionMatrixDefaults.isAllowedByDefault(role, permissionKey)));
        return permissionMap;
    }

    private void upsert(UUID tenantId, String role, String permissionKey, boolean allowed) {
        rolePermissionRepository.findByTenantIdAndRoleAndPermissionKey(tenantId, role, permissionKey)
                .ifPresentOrElse(
                        existing -> existing.changeAllowed(allowed),
                        () -> rolePermissionRepository.save(new RolePermission(tenantId, role, permissionKey, allowed))
                );
    }

    private String requireCanonicalRole(String role) {
        String normalized = String.valueOf(role).trim().toUpperCase(Locale.ROOT);
        if (!PermissionMatrixDefaults.CANONICAL_ROLES.contains(normalized)) {
            throw new InvalidSettingsRoleException(role);
        }
        return normalized;
    }

    private Map<String, Object> matrixSnapshot(UUID tenantId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        rolePermissionRepository.findAllByTenantId(tenantId)
                .forEach(permission -> snapshot.put(permission.getRole() + ":" + permission.getPermissionKey(),
                        permission.isAllowed()));
        return snapshot;
    }

    private List<SettingCard> buildOverview(ProductAccessScope scope) {
        boolean hasCustomPermissions = !rolePermissionRepository.findAllByTenantId(scope.tenantId()).isEmpty();
        return List.of(
                new SettingCard("Produto", "Dados, branding, SEO e publicação",
                        "ACTIVE".equals(scope.status()) ? STATUS_CONFIGURADO : "atenção",
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_MEDIO),
                new SettingCard("Tenant", "Governança, limites e identidade BYOP", STATUS_CONFIGURADO,
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_ALTO),
                new SettingCard("Equipe", "Usuários, convites e acessos", STATUS_CONFIGURADO,
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_MEDIO),
                new SettingCard("Permissões", "Matriz por papel e módulo",
                        hasCustomPermissions ? "customizado" : "padrão",
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_ALTO),
                new SettingCard("Integrações", "Webhooks, e-mail e canais externos", STATUS_CONFIGURADO,
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_MEDIO),
                new SettingCard("Segurança", "Políticas de senha e sessão", STATUS_CONFIGURADO,
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_ALTO),
                new SettingCard("Auditoria", "Trilha de eventos do tenant", STATUS_CONFIGURADO,
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, RISK_MEDIO),
                new SettingCard("SEO", "Title, description e OG image padrão", STATUS_CONFIGURADO,
                        OVERVIEW_PLACEHOLDER, OVERVIEW_PLACEHOLDER, "baixo")
        );
    }
}
