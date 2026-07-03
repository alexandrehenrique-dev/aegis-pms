package br.com.byop.aegis.settings.api;

import br.com.byop.aegis.settings.domain.ProductSecuritySettings;
import br.com.byop.aegis.settings.repository.ProductSecuritySettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAlertSettingsServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ProductSecuritySettingsRepository repository;

    @Test
    void shouldReturnConfiguredProductTelegramAlert() {
        ProductSecuritySettings settings = new ProductSecuritySettings(PRODUCT_ID);
        settings.update(new ProductSecuritySettings.Update(null, null, false, null, false,
                "chat-1", "token-1"));
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(settings));

        Optional<TelegramAlertSettings> result = service().findConfiguredProductAlert(PRODUCT_ID);

        assertThat(result).hasValue(new TelegramAlertSettings("chat-1", "token-1"));
    }

    @Test
    void shouldReturnEmptyWhenProductTelegramAlertIsMissing() {
        ProductSecuritySettings settings = new ProductSecuritySettings(PRODUCT_ID);
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(settings));

        Optional<TelegramAlertSettings> result = service().findConfiguredProductAlert(PRODUCT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenProductTelegramAlertIsBlank() {
        ProductSecuritySettings settings = new ProductSecuritySettings(PRODUCT_ID);
        ReflectionTestUtils.setField(settings, "telegramAlertChatId", "chat-1");
        ReflectionTestUtils.setField(settings, "telegramAlertBotToken", " ");
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(settings));

        Optional<TelegramAlertSettings> result = service().findConfiguredProductAlert(PRODUCT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNullProductId() {
        Optional<TelegramAlertSettings> result = service().findConfiguredProductAlert(null);

        assertThat(result).isEmpty();
    }

    private TelegramAlertSettingsService service() {
        return new TelegramAlertSettingsService(repository);
    }
}
