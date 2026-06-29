package br.com.byop.aegis.settings.mapper;

import br.com.byop.aegis.settings.domain.RolePermission;
import br.com.byop.aegis.settings.dto.RolePermissionSummary;
import org.mapstruct.Mapper;

/**
 * Conversor {@code RolePermission -> RolePermissionSummary}. O agrupamento
 * por papel (matriz {@code role x permissionKey}) e responsabilidade do
 * {@code SettingsService}, nao deste mapper — aqui e so a conversao 1:1 de
 * uma linha da tabela {@code role_permissions}.
 */
@Mapper(componentModel = "spring")
public interface RolePermissionMapper {

    RolePermissionSummary toSummary(RolePermission permission);
}
