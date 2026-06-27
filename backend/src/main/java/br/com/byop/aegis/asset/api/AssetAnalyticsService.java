package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssetAnalyticsService {

    private static final int RECENT_ASSET_DAYS = 30;

    private final AssetRepository assetRepository;
    private final Clock clock;

    public AssetAnalyticsService(AssetRepository assetRepository, Clock clock) {
        this.assetRepository = assetRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssetAnalyticsResponse summarize(UUID productId) {
        List<Asset> assets = assetRepository.findAllByProductId(productId);
        OffsetDateTime recentLimit = OffsetDateTime.now(clock).minusDays(RECENT_ASSET_DAYS);
        long recent = assets.stream()
                .filter(asset -> isOnOrAfter(asset.getCreatedAt(), recentLimit))
                .count();
        long missingAltText = assets.stream()
                .filter(asset -> asset.getAltText() == null || asset.getAltText().isBlank())
                .count();
        OffsetDateTime lastUploadedAt = assets.stream()
                .map(Asset::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new AssetAnalyticsResponse(assets.size(), recent, missingAltText, lastUploadedAt);
    }

    private boolean isOnOrAfter(OffsetDateTime value, OffsetDateTime limit) {
        return value != null && !value.isBefore(limit);
    }

}
