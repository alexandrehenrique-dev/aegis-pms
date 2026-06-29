package br.com.byop.aegis.settings.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.settings.domain.RolePermission;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Test
    void shouldSaveRolePermission() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("role-permission-save"));

        RolePermission saved = rolePermissionRepository.saveAndFlush(
                new RolePermission(tenant.getId(), "TENANT_ADMIN", "/settings/roles", true));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(tenant.getId());
        assertThat(saved.getRole()).isEqualTo("TENANT_ADMIN");
        assertThat(saved.getPermissionKey()).isEqualTo("/settings/roles");
        assertThat(saved.isAllowed()).isTrue();
    }

    @Test
    void shouldFindAllByTenantId() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("role-permission-tenant"));
        Tenant otherTenant = tenantRepository.saveAndFlush(tenant("role-permission-tenant-other"));
        RolePermission matching = rolePermissionRepository.saveAndFlush(
                new RolePermission(tenant.getId(), "EDITOR", "/content", true));
        rolePermissionRepository.saveAndFlush(new RolePermission(otherTenant.getId(), "EDITOR", "/content", true));

        assertThat(rolePermissionRepository.findAllByTenantId(tenant.getId())).containsExactly(matching);
    }

    @Test
    void shouldFindAllByTenantIdAndRole() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("role-permission-role"));
        RolePermission editorPermission = rolePermissionRepository.saveAndFlush(
                new RolePermission(tenant.getId(), "EDITOR", "/content", true));
        rolePermissionRepository.saveAndFlush(new RolePermission(tenant.getId(), "VIEWER", "/content", true));

        assertThat(rolePermissionRepository.findAllByTenantIdAndRole(tenant.getId(), "EDITOR"))
                .containsExactly(editorPermission);
    }

    @Test
    void shouldFindByTenantIdAndRoleAndPermissionKey() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("role-permission-find-one"));
        RolePermission saved = rolePermissionRepository.saveAndFlush(
                new RolePermission(tenant.getId(), "VIEWER", "/audit", false));

        assertThat(rolePermissionRepository.findByTenantIdAndRoleAndPermissionKey(tenant.getId(), "VIEWER", "/audit"))
                .contains(saved);
        assertThat(rolePermissionRepository.findByTenantIdAndRoleAndPermissionKey(tenant.getId(), "VIEWER", "/settings"))
                .isEmpty();
    }

    @Test
    void shouldDeleteAllByTenantId() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("role-permission-delete"));
        Tenant otherTenant = tenantRepository.saveAndFlush(tenant("role-permission-delete-other"));
        rolePermissionRepository.saveAndFlush(new RolePermission(tenant.getId(), "EDITOR", "/content", true));
        RolePermission otherTenantPermission = rolePermissionRepository.saveAndFlush(
                new RolePermission(otherTenant.getId(), "EDITOR", "/content", true));

        rolePermissionRepository.deleteAllByTenantId(tenant.getId());
        rolePermissionRepository.flush();

        assertThat(rolePermissionRepository.findAllByTenantId(tenant.getId())).isEmpty();
        assertThat(rolePermissionRepository.findById(otherTenantPermission.getId())).isPresent();
    }

    @Test
    void shouldChangeAllowed() {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("role-permission-change"));
        RolePermission saved = rolePermissionRepository.saveAndFlush(
                new RolePermission(tenant.getId(), "EDITOR", "/content", false));

        saved.changeAllowed(true);
        rolePermissionRepository.saveAndFlush(saved);

        assertThat(rolePermissionRepository.findById(saved.getId()).orElseThrow().isAllowed()).isTrue();
    }
}
