package br.com.byop.aegis.asset.service;

import br.com.byop.aegis.asset.config.AssetLimitsProperties;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.exception.AssetSizeLimitExceededException;
import br.com.byop.aegis.asset.exception.InvalidAssetMimeTypeException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Valida mime type e tamanho de um upload contra a tabela configurada em
 * {@code aegis.assets.limits} ({@link AssetLimitsProperties}) — nunca contra
 * valores hardcoded no codigo.
 */
@Component
public class AssetUploadValidator {

    private final AssetLimitsProperties limitsProperties;

    public AssetUploadValidator(AssetLimitsProperties limitsProperties) {
        this.limitsProperties = limitsProperties;
    }

    /**
     * Resolve a categoria a partir do mime type e valida o tamanho contra o
     * limite configurado para aquela categoria.
     *
     * @param mimeType mime type informado pelo cliente no upload
     * @param sizeBytes tamanho do arquivo em bytes
     * @return categoria resolvida
     */
    public AssetCategory validate(String mimeType, long sizeBytes) {
        AssetCategory category = resolveCategory(mimeType);
        AssetLimitsProperties.CategoryLimit limit = limitsProperties.limits().get(category.contractValue());
        if (sizeBytes > limit.maxSizeBytes()) {
            throw new AssetSizeLimitExceededException(category, sizeBytes, limit.maxSizeBytes());
        }
        return category;
    }

    private AssetCategory resolveCategory(String mimeType) {
        for (Map.Entry<String, AssetLimitsProperties.CategoryLimit> entry : limitsProperties.limits().entrySet()) {
            if (entry.getValue().mimeTypes().contains(mimeType)) {
                return AssetCategory.fromContractValue(entry.getKey());
            }
        }
        throw new InvalidAssetMimeTypeException(mimeType);
    }
}
