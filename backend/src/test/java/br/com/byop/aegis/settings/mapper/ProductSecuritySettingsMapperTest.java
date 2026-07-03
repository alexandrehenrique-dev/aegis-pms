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
                true, "ga-key", true, "123", "secret-token-1234"));

        ProductSecuritySettingsResponse response = mapper.toResponse(settings);

        assertThat(response.webhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(response.webhookStatus()).isEqualTo("connected");
        assertThat(response.analyticsStatus()).isEqualTo("connected");
        assertThat(response.emailStatus()).isEqualTo("connected");
        assertThat(response.telegramAlert().chatId()).isEqualTo("123");
        assertThat(response.telegramAlert().botTokenMasked()).isEqualTo("****1234");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void shouldMapDisconnectedAndAttentionStatuses() {
        ProductSecuritySettings settings = new ProductSecuritySettings(UUID.randomUUID());
        settings.update(new ProductSecuritySettings.Update(null, null, true, null, false, null, null));

        ProductSecuritySettingsResponse response = mapper.toResponse(settings);

        assertThat(response.webhookStatus()).isEqualTo("disconnected");
        assertThat(response.analyticsStatus()).isEqualTo("attention");
        assertThat(response.emailStatus()).isEqualTo("disconnected");
        assertThat(response.telegramAlert()).isNull();
        assertThat(mapper.connectionStatus(" ")).isEqualTo("disconnected");
    }

    @Test
    void shouldMapNullSettingsToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void shouldMaskShortTokenAndIgnoreIncompleteTelegramAlert() {
        ProductSecuritySettings settings = new ProductSecuritySettings(UUID.randomUUID());
        settings.update(new ProductSecuritySettings.Update(null, null, false, null, false, "123", null));

        ProductSecuritySettingsResponse response = mapper.toResponse(settings);

        assertThat(response.telegramAlert()).isNull();
        assertThat(mapper.maskToken("abc")).isEqualTo("****abc");
        assertThat(mapper.maskToken(" ")).isNull();
        assertThat(mapper.maskToken(null)).isNull();
    }
}
