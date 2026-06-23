package br.com.byop.aegis.api.me;

/**
 * Resposta pública do endpoint autenticado /api/v1/me.
 *
 * @param subject identificador do usuário no Keycloak
 * @param email e-mail do usuário autenticado
 * @param username nome de usuário preferencial
 * @param name nome exibido do usuário
 * @param role papel público canônico em formato singular e minúsculo
 */
public record MeResponse(
        String subject,
        String email,
        String username,
        String name,
        String role
) {
}
