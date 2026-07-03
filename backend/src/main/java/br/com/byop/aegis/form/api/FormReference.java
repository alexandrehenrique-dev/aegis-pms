package br.com.byop.aegis.form.api;

import java.util.UUID;

public record FormReference(
        UUID id,
        UUID tenantId,
        UUID productId,
        boolean published,
        String fieldsJson,
        String deliveryChannelsJson
) {
}
