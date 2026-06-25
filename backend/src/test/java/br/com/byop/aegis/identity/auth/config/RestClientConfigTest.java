package br.com.byop.aegis.identity.auth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    @Test
    void shouldCreateRestClientBean() {
        RestClientConfig config = new RestClientConfig();

        assertThat(config.restClient()).isNotNull();
    }
}
