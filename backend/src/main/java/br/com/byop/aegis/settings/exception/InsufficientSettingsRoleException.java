package br.com.byop.aegis.settings.exception;

/**
 * Lancada quando o papel do chamador nao tem permissao para ler/alterar
 * configuracoes de tenant/produto ou a matriz de permissoes. Mapeada para
 * 403.
 */
public class InsufficientSettingsRoleException extends RuntimeException {
}
