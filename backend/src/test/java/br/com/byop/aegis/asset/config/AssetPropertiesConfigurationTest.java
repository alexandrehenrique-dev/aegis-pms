package br.com.byop.aegis.asset.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetPropertiesConfigurationTest {

    @Test
    void shouldInstantiateConfiguration() {
        assertThat(new AssetPropertiesConfiguration()).isNotNull();
    }
}
