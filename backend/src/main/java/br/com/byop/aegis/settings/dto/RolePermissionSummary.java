package br.com.byop.aegis.settings.dto;

/**
 * Uma unica linha da matriz de permissoes: papel, chave de permissao
 * (mesma string ja usada pelo frontend) e se o papel pode ou nao executar
 * aquela acao/ver aquela rota.
 */
public record RolePermissionSummary(
        String role,
        String permissionKey,
        boolean allowed
) {
}
