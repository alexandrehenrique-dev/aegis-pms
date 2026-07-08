package br.com.byop.aegis.identity.auth.exception;

/**
 * O.1 (BUG-SPRINT-05) — Keycloak recusa o login com "Account is not fully
 * set up" quando o perfil do usuário está incompleto (ex.: sem
 * firstName/lastName), mesmo com a conta {@code enabled}. Antes desta
 * sprint essa causa era tratada como {@link AccountDisabledException}
 * genérica — a mensagem "conta bloqueada, fale com o administrador" era
 * enganosa para um usuário que só precisa reabrir o link de convite para
 * completar a ativação.
 */
public class AccountNotFullySetUpException extends RuntimeException {

    public AccountNotFullySetUpException() {
        super("Account is not fully set up");
    }
}
