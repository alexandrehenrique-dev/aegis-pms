package br.com.byop.aegis.settings.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionMatrixDefaultsTest {

    @Test
    void shouldAllowDashboardForEveryCanonicalRole() {
        PermissionMatrixDefaults.CANONICAL_ROLES.forEach(role ->
                assertThat(PermissionMatrixDefaults.isAllowedByDefault(role, "/dashboard")).isTrue());
    }

    @Test
    void shouldDenyContentForSuperAdminPerLgpd() {
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_SUPER_ADMIN, "/content"))
                .isFalse();
    }

    @Test
    void shouldAllowContentForProductRolesOnly() {
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/content")).isFalse();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_PRODUCT_MANAGER, "/content")).isTrue();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_EDITOR, "/content")).isTrue();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_VIEWER, "/content")).isTrue();
    }

    @Test
    void shouldAllowTenantAdminGovernanceButDenyProductOperations() {
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/users")).isTrue();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/products/new")).isTrue();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/knowledge")).isFalse();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/assets/upload")).isFalse();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/content/*/publish")).isFalse();
    }

    @Test
    void shouldDenyRoleManagementForProductManagerEditorAndViewer() {
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_PRODUCT_MANAGER, "/settings/roles")).isFalse();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_EDITOR, "/settings/roles")).isFalse();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_VIEWER, "/settings/roles")).isFalse();
    }

    @Test
    void shouldDenySettingsSecurityForEveryoneButSuperAdmin() {
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_SUPER_ADMIN, "/settings/security")).isTrue();
        assertThat(PermissionMatrixDefaults.isAllowedByDefault(PermissionMatrixDefaults.ROLE_TENANT_ADMIN, "/settings/security")).isFalse();
    }

    @Test
    void shouldReturnFalseForUnknownRole() {
        assertThat(PermissionMatrixDefaults.isAllowedByDefault("INTERN", "/dashboard")).isFalse();
    }

    @Test
    void shouldContainAllPermissionKeysExactlyOnce() {
        assertThat(PermissionMatrixDefaults.PERMISSION_KEYS).doesNotHaveDuplicates();
    }

    @Test
    void shouldContainExactlyFiveCanonicalRoles() {
        assertThat(PermissionMatrixDefaults.CANONICAL_ROLES).containsExactly("SUPER_ADMIN", "TENANT_ADMIN",
                "PRODUCT_MANAGER", "EDITOR", "VIEWER");
    }
}
