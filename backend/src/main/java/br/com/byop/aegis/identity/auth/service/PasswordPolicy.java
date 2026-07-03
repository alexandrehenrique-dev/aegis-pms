package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.exception.WeakPasswordException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    static final String WEAK_CREDENTIAL_MESSAGE =
            "A senha deve ter ao menos 8 caracteres, incluindo letras e números.";

    public void assertStrong(String password) {
        String value = String.valueOf(password);
        boolean hasMinimumLength = value.length() >= 8;
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasNumber = value.chars().anyMatch(Character::isDigit);
        if (!hasMinimumLength || !hasLetter || !hasNumber) {
            throw new WeakPasswordException(WEAK_CREDENTIAL_MESSAGE);
        }
    }
}
