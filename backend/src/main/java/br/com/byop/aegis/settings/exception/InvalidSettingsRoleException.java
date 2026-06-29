package br.com.byop.aegis.settings.exception;

/**
 * Lancada quando o {@code role} informado em
 * {@code permission-matrix/preview} nao e um dos 5 papeis canonicos
 * (ADR-0014).
 */
public class InvalidSettingsRoleException extends RuntimeException {

    public InvalidSettingsRoleException(String role) {
        super("Invalid role: " + role);
    }
}
