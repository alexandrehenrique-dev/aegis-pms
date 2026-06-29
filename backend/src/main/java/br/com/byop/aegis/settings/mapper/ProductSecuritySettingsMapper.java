package br.com.byop.aegis.settings.mapper;

import br.com.byop.aegis.settings.domain.ProductSecuritySettings;
import br.com.byop.aegis.settings.dto.ProductSecuritySettingsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSecuritySettingsMapper {

    String CONNECTED = "connected";
    String DISCONNECTED = "disconnected";
    String ATTENTION = "attention";

    @Mapping(target = "webhookUrl", expression = "java(settings.getWebhookUrl())")
    @Mapping(target = "analyticsProviderKey", expression = "java(settings.getAnalyticsProviderKey())")
    @Mapping(target = "webhookStatus", expression = "java(connectionStatus(settings.getWebhookUrl()))")
    @Mapping(target = "analyticsStatus", expression = "java(analyticsStatus(settings))")
    @Mapping(target = "emailStatus", expression = "java(settings.isEmailDeliveryEnabled() ? CONNECTED : DISCONNECTED)")
    ProductSecuritySettingsResponse toResponse(ProductSecuritySettings settings);

    default String connectionStatus(String value) {
        return value == null || value.isBlank() ? DISCONNECTED : CONNECTED;
    }

    default String analyticsStatus(ProductSecuritySettings settings) {
        if (!settings.isAnalyticsEnabled()) {
            return DISCONNECTED;
        }
        return connectionStatus(settings.getAnalyticsProviderKey()).equals(CONNECTED) ? CONNECTED : ATTENTION;
    }
}
