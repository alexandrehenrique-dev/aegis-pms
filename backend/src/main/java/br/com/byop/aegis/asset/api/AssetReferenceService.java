package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.exception.AssetNotFoundException;
import br.com.byop.aegis.asset.repository.AssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class AssetReferenceService {

    private final AssetRepository assetRepository;

    public AssetReferenceService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public AssetReference getRequiredReference(UUID assetId) {
        log.debug("getRequiredReference: assetId='{}'", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
        return new AssetReference(asset.getId(), asset.getProductId(), asset.getMimeType(), asset.getCategory().contractValue());
    }
}
