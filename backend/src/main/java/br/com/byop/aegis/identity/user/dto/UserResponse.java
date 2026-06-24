package br.com.byop.aegis.identity.user.dto;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled
) {
}
