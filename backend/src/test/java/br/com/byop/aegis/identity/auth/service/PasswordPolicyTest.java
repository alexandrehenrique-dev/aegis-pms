package br.com.byop.aegis.identity.auth.service;

import br.com.byop.aegis.identity.auth.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void shouldAcceptStrongPassword() {
        assertThatCode(() -> policy.assertStrong("Senha123"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWeakPassword() {
        assertThatThrownBy(() -> policy.assertStrong("senhafraca"))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessage(PasswordPolicy.WEAK_CREDENTIAL_MESSAGE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345678", "Senha"})
    void shouldRejectPasswordsMissingRequiredParts(String password) {
        assertThatThrownBy(() -> policy.assertStrong(password))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessage(PasswordPolicy.WEAK_CREDENTIAL_MESSAGE);
    }
}
