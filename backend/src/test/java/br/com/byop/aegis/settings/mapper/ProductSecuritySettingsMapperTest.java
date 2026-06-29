package br.com.byop.aegis.settings.mapper;

import br.com.byop.aegis.settings.domain.ProductSecuritySettings;
import br.com.byop.aegis.settings.dto.ProductSecuritySettingsResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSecuritySettingsMapperTest {

    private final ProductSecuritySettingsMapper mapper = Mappers.getMapper(ProductSecuritySettingsMapper.class);

    @Test
    void shouldMapConnectedSecuritySettingsWithoutSecret() {
        ProductSecuritySettings settings = new ProductSecuritySettings(UUID.randomUUID());
        settings.update(new ProductSecuritySettings.Update("https://example.com/hook", "secret",
                true, "ga-key", true));

        ProductSecuritySettingsResponse response = mapper.toResponse(settings);

        assertThat(response.webhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(response.webhookStatus()).isEqualTo("connected");
        assertThat(response.analyticsStatus()).isEqualTo("connected");
        assertThat(response.emailStatus()).isEqualTo("connected");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void shouldMapDisconnectedAndAttentionStatuses() {
        ProductSecuritySettings settings = new ProductSecuritySettings(UUID.randomUUID());
        settings.update(new ProductSecuritySettings.Update(null, null, true, null, false));

        ProductSecuritySettingsResponse response = mapper.toResponse(settings);

        assertThat(response.webhookStatus()).isEqualTo("disconnected");
        assertThat(response.analyticsStatus()).isEqualTo("attention");
        assertThat(response.emailStatus()).isEqualTo("disconnected");
        assertThat(mapper.connectionStatus(" ")).isEqualTo("disconnected");
    }

    @Test
    void shouldMapNullSettingsToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
