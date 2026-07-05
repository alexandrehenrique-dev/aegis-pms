package br.com.byop.aegis.product.dto;

import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.domain.ProductTypeKey;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSummary(
        UUID id,
        UUID tenantId,
        String key,
        String name,
        ProductTypeKey type,
        ProductStatus status,
        String defaultLocale,
        AssetStorageStrategy assetStorageStrategy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        /** Número de módulos habilitados. Usado pelo frontend para determinar se o produto pode ser aberto. */
        int enabledModuleCount,
        /**
         * Papel do próprio caller neste produto especificamente (via {@code ProductAssignment}
         * com status ASSIGNED), independente do papel de plataforma dele (Keycloak realm role).
         * {@code null} quando o caller não tem nenhuma atribuição neste produto — ex.: um
         * Super Admin sem ProductAssignment aqui. Permite ao frontend mesclar a navegação da
         * sidebar com o papel de produto quando o usuário acumula os dois (ex.: Super Admin
         * que também é Editor de um produto específico).
         */
        String callerAssignedRole
) {
}
