package br.com.byop.aegis.identity.api;

import java.util.Objects;

public record IdentityUser(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {

    public String displayName() {
        String fullName = (Objects.toString(firstName, "") + " " + Objects.toString(lastName, "")).trim();
        return fullName.isBlank() ? username : fullName;
    }
}
