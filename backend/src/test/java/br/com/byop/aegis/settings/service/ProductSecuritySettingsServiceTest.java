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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSecuritySettingsServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ProductSecuritySettingsMapper mapper = Mappers.getMapper(ProductSecuritySettingsMapper.class);

    @Mock
    private ProductSecuritySettingsRepository repository;

    @Mock
    private ProductVisibilityService productVisibilityService;

    @Test
    void shouldLazyCreateSettingsOnFirstRead() {
        AuthenticatedUser caller = user("ROLE_SUPER_ADMIN");
        ProductSecuritySettings saved = new ProductSecuritySettings(PRODUCT_ID);
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of(scope()));
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(repository.save(any(ProductSecuritySettings.class))).thenReturn(saved);

        ProductSecuritySettingsResponse response = service().getSecuritySettings(caller, PRODUCT_ID);

        assertThat(response.webhookStatus()).isEqualTo("disconnected");
        verify(repository).save(any(ProductSecuritySettings.class));
    }

    @Test
    void shouldUpdateSettingsForTenantAdmin() {
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        ProductSecuritySettings settings = new ProductSecuritySettings(PRODUCT_ID);
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of(scope()));
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.of(settings));
        when(repository.save(settings)).thenReturn(settings);

        ProductSecuritySettingsResponse response = service().updateSecuritySettings(caller, PRODUCT_ID,
                new UpdateProductSecuritySettingsRequest("https://example.com/hook", "secret",
                        true, "ga-key", true, null));

        assertThat(response.webhookStatus()).isEqualTo("connected");
        assertThat(response.analyticsEnabled()).isTrue();
        assertThat(settings.getWebhookSecret()).isEqualTo("secret");
    }

    @Test
    void shouldCreateSettingsWhenUpdatingForTheFirstTime() {
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of(scope()));
        when(repository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        when(repository.save(any(ProductSecuritySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSecuritySettingsResponse response = service().updateSecuritySettings(caller, PRODUCT_ID,
                new UpdateProductSecuritySettingsRequest("https://example.com/hook", "secret",
                        true, "ga-key", true,
                        new UpdateProductSecuritySettingsRequest.TelegramAlertRequest("123", "token-1234")));

        assertThat(response.webhookStatus()).isEqualTo("connected");
        assertThat(response.telegramAlert().chatId()).isEqualTo("123");
        assertThat(response.telegramAlert().botTokenMasked()).isEqualTo("****1234");
        verify(repository).save(any(ProductSecuritySettings.class));
    }

    @Test
    void shouldRejectProductManagerWithForbidden() {
        AuthenticatedUser caller = user("ROLE_PRODUCT_MANAGER");
        ProductSecuritySettingsService settingsService = service();

        assertThatThrownBy(() -> settingsService.getSecuritySettings(caller, PRODUCT_ID))
                .isInstanceOf(InsufficientSettingsRoleException.class);
        verify(productVisibilityService, never()).listVisibleProducts(any());
    }

    @Test
    void shouldReturnNotFoundWhenProductIsOutOfScope() {
        AuthenticatedUser caller = user("ROLE_TENANT_ADMIN");
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of());
        ProductSecuritySettingsService settingsService = service();

        assertThatThrownBy(() -> settingsService.getSecuritySettings(caller, PRODUCT_ID))
                .isInstanceOf(SettingsNotFoundException.class);
    }

    private ProductSecuritySettingsService service() {
        return new ProductSecuritySettingsService(repository, mapper, productVisibilityService);
    }

    private ProductAccessScope scope() {
        return new ProductAccessScope(PRODUCT_ID, TENANT_ID, "ACTIVE");
    }

    private AuthenticatedUser user(String role) {
        return new AuthenticatedUser("user-1", "user@aegis.app", "user", "User", Set.of(role));
    }
}
