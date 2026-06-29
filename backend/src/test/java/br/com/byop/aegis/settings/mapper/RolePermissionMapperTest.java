package br.com.byop.aegis.settings.mapper;

import br.com.byop.aegis.settings.domain.RolePermission;
import br.com.byop.aegis.settings.dto.RolePermissionSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionMapperTest {

    private final RolePermissionMapper mapper = Mappers.getMapper(RolePermissionMapper.class);

    @Test
    void shouldMapEntityToSummary() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        RolePermission permission = new RolePermission(tenantId, "TENANT_ADMIN", "/settings/roles", true);

        RolePermissionSummary summary = mapper.toSummary(permission);

        assertThat(summary.role()).isEqualTo("TENANT_ADMIN");
        assertThat(summary.permissionKey()).isEqualTo("/settings/roles");
        assertThat(summary.allowed()).isTrue();
    }

    @Test
    void shouldMapDisallowedPermission() {
        UUID tenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        RolePermission permission = new RolePermission(tenantId, "VIEWER", "/settings", false);

        assertThat(mapper.toSummary(permission).allowed()).isFalse();
    }

    @Test
    void shouldReturnNullSummaryWhenPermissionIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
    }
}
