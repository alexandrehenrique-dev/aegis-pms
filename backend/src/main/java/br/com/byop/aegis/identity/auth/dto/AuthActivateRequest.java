package br.com.byop.aegis.identity.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * O.1 (BUG-SPRINT-05) — {@code firstName}/{@code lastName} obrigatorios na
 * ativacao de convite (separado de {@link AuthPasswordActionRequest}, usado
 * por {@code /reset-password/confirm}, que nao coleta nome). Sem nome e
 * sobrenome, o perfil no Keycloak fica incompleto e o realm bloqueia o
 * login mesmo com a conta habilitada.
 */
public record AuthActivateRequest(
        @NotNull UUID token,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
