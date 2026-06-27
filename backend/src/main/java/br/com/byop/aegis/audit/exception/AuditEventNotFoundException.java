package br.com.byop.aegis.audit.exception;

/**
 * Lancada quando o evento/tenant solicitado nao existe ou nao pertence ao
 * escopo do usuario autenticado. Mapeada sempre para 404, nunca 403 — nao
 * revela a existencia de um tenant/evento fora do escopo do chamador.
 */
public class AuditEventNotFoundException extends RuntimeException {
}
