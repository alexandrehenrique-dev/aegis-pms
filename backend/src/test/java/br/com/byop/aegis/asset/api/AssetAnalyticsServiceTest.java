package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetAnalyticsServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-27T12:00:00Z");

    private AssetRepository assetRepository;
    private AssetAnalyticsService service;

    @BeforeEach
    void setUp() {
        assetRepository = mock(AssetRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T12:00:00Z"), ZoneOffset.UTC);
        service = new AssetAnalyticsService(assetRepository, clock);
    }

    @Test
    void shouldSummarizeEmptyAssets() {
        when(assetRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of());

        AssetAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response).isEqualTo(new AssetAnalyticsResponse(0, 0, 0, null));
    }

    @Test
    void shouldSummarizeRecentAssetsAndAltTextGaps() {
        Asset recentWithoutAlt = asset(NOW.minusDays(1), null);
        Asset oldBlankAlt = asset(NOW.minusDays(40), " ");
        Asset recentWithAlt = asset(NOW.minusDays(5), "Foto do produto");
        Asset unknownDate = asset(null, "Legenda");
        when(assetRepository.findAllByProductId(PRODUCT_ID))
                .thenReturn(List.of(recentWithoutAlt, oldBlankAlt, recentWithAlt, unknownDate));

        AssetAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response.total()).isEqualTo(4);
        assertThat(response.recent()).isEqualTo(2);
        assertThat(response.missingAltText()).isEqualTo(2);
        assertThat(response.lastUploadedAt()).isEqualTo(NOW.minusDays(1));
    }

    private Asset asset(OffsetDateTime createdAt, String altText) {
        Asset asset = mock(Asset.class);
        when(asset.getCreatedAt()).thenReturn(createdAt);
        when(asset.getAltText()).thenReturn(altText);
        return asset;
    }
}
