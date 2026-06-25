package br.com.byop.aegis.identity.auth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakPropertiesConfigurationTest {

    @Test
    void shouldInstantiateConfiguration() {
        assertThat(new KeycloakPropertiesConfiguration()).isNotNull();
    }
}
