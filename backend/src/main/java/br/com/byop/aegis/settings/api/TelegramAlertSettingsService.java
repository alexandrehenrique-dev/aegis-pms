package br.com.byop.aegis.settings.api;

import br.com.byop.aegis.settings.repository.ProductSecuritySettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TelegramAlertSettingsService {

    private final ProductSecuritySettingsRepository repository;

    public TelegramAlertSettingsService(ProductSecuritySettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<TelegramAlertSettings> findConfiguredProductAlert(UUID productId) {
        log.debug("findConfiguredProductAlert: productId='{}'", productId);
        if (productId == null) {
            return Optional.empty();
        }
        return repository.findById(productId)
                .filter(settings -> hasText(settings.getTelegramAlertChatId()))
                .filter(settings -> hasText(settings.getTelegramAlertBotToken()))
                .map(settings -> new TelegramAlertSettings(
                        settings.getTelegramAlertChatId(),
                        settings.getTelegramAlertBotToken()
                ));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
