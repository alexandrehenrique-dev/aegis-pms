package br.com.byop.aegis.settings.service;

import br.com.byop.aegis.product.api.ProductAccessScope;
import br.com.byop.aegis.product.api.ProductVisibilityService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.settings.contract.UpdateProductSecuritySettingsRequest;
import br.com.byop.aegis.settings.domain.ProductSecuritySettings;
import br.com.byop.aegis.settings.dto.ProductSecuritySettingsResponse;
import br.com.byop.aegis.settings.exception.InsufficientSettingsRoleException;
import br.com.byop.aegis.settings.exception.SettingsNotFoundException;
import br.com.byop.aegis.settings.mapper.ProductSecuritySettingsMapper;
import br.com.byop.aegis.settings.repository.ProductSecuritySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductSecuritySettingsService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    private final ProductSecuritySettingsRepository repository;
    private final ProductSecuritySettingsMapper mapper;
    private final ProductVisibilityService productVisibilityService;

    public ProductSecuritySettingsService(ProductSecuritySettingsRepository repository,
                                          ProductSecuritySettingsMapper mapper,
                                          ProductVisibilityService productVisibilityService) {
        this.repository = repository;
        this.mapper = mapper;
        this.productVisibilityService = productVisibilityService;
    }

    @Transactional
    public ProductSecuritySettingsResponse getSecuritySettings(AuthenticatedUser caller, UUID productId) {
        assertSecuritySettingsRole(caller);
        resolveProductScope(caller, productId);
        ProductSecuritySettings settings = repository.findById(productId)
                .orElseGet(() -> repository.save(new ProductSecuritySettings(productId)));
        return mapper.toResponse(settings);
    }

    @Transactional
    public ProductSecuritySettingsResponse updateSecuritySettings(AuthenticatedUser caller, UUID productId,
                                                                  UpdateProductSecuritySettingsRequest request) {
        assertSecuritySettingsRole(caller);
        resolveProductScope(caller, productId);
        ProductSecuritySettings settings = repository.findById(productId)
                .orElseGet(() -> new ProductSecuritySettings(productId));
        String telegramChatId = request.telegramAlert() == null ? null : request.telegramAlert().chatId();
        String telegramBotToken = request.telegramAlert() == null ? null : request.telegramAlert().botToken();
        settings.update(new ProductSecuritySettings.Update(
                request.webhookUrl(), request.webhookSecret(), request.analyticsEnabled(),
                request.analyticsProviderKey(), request.emailDeliveryEnabled(), telegramChatId, telegramBotToken
        ));
        return mapper.toResponse(repository.save(settings));
    }

    private ProductAccessScope resolveProductScope(AuthenticatedUser caller, UUID productId) {
        return productVisibilityService.listVisibleProducts(caller)
                .stream()
                .filter(scope -> scope.productId().equals(productId))
                .findFirst()
                .orElseThrow(SettingsNotFoundException::new);
    }

    private void assertSecuritySettingsRole(AuthenticatedUser caller) {
        if (caller.authorities().contains(ROLE_SUPER_ADMIN) || caller.authorities().contains(ROLE_TENANT_ADMIN)) {
            return;
        }
        throw new InsufficientSettingsRoleException();
    }
}
