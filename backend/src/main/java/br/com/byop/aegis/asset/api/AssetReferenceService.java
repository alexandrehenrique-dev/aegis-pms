package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.exception.AssetNotFoundException;
import br.com.byop.aegis.asset.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AssetReferenceService {

    private final AssetRepository assetRepository;

    public AssetReferenceService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public AssetReference getRequiredReference(UUID assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
        return new AssetReference(asset.getId(), asset.getProductId(), asset.getMimeType(), asset.getCategory().contractValue());
    }
}
