package br.com.byop.aegis.settings.exception;

/**
 * Lancada quando o tenant/produto solicitado nao existe ou nao pertence ao
 * escopo do usuario autenticado. Mapeada sempre para 404, nunca 403 — nao
 * revela a existencia de um tenant/produto fora do escopo do chamador.
 */
public class SettingsNotFoundException extends RuntimeException {
}
