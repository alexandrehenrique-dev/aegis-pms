package br.com.byop.aegis.asset.mapper;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.domain.AssetUsage;
import br.com.byop.aegis.asset.dto.AssetDetail;
import br.com.byop.aegis.asset.dto.AssetSummary;
import br.com.byop.aegis.asset.dto.AssetUsageSummary;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssetMapperTest {

    private static final OffsetDateTime FIXED_CREATED_AT = OffsetDateTime.parse("2026-06-26T10:00:00-03:00");

    private final AssetMapper mapper = Mappers.getMapper(AssetMapper.class);

    @Test
    void shouldMapAssetToSummaryWithAllFields() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Asset asset = asset(id, AssetCategory.PDF, 2_500_000L);

        AssetSummary summary = mapper.toSummary(asset, List.of("blog", "institucional"), List.of("Artigo Home"));

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.name()).isEqualTo("Curriculo");
        assertThat(summary.type()).isEqualTo("pdf");
        assertThat(summary.size()).isEqualTo("2.4 MB");
        assertThat(summary.status()).isEqualTo("ativo");
        assertThat(summary.tags()).isEqualTo("blog, institucional");
        assertThat(summary.usage()).isEqualTo("Artigo Home");
        assertThat(summary.uploadedAt()).isEqualTo("2026-06-26");
    }

    @Test
    void shouldFormatSizeInKbWhenBelowOneMegabyte() {
        Asset asset = asset(UUID.randomUUID(), AssetCategory.IMAGE, 2048L);

        AssetSummary summary = mapper.toSummary(asset, List.of(), List.of());

        assertThat(summary.size()).isEqualTo("2 KB");
        assertThat(summary.tags()).isEmpty();
        assertThat(summary.usage()).isEmpty();
    }

    @Test
    void shouldRoundUpToAtLeastOneKbForTinyFiles() {
        Asset asset = asset(UUID.randomUUID(), AssetCategory.IMAGE, 10L);

        AssetSummary summary = mapper.toSummary(asset, List.of(), List.of());

        assertThat(summary.size()).isEqualTo("1 KB");
    }

    @Test
    void shouldReturnNullSummaryWhenAllSourcesAreNull() {
        assertThat(mapper.toSummary(null, null, null)).isNull();
    }

    @Test
    void shouldMapOnlyTagsWhenAssetIsNull() {
        AssetSummary summary = mapper.toSummary(null, List.of("blog"), null);

        assertThat(summary.tags()).isEqualTo("blog");
        assertThat(summary.usage()).isEmpty();
        assertThat(summary.name()).isNull();
        assertThat(summary.type()).isNull();
    }

    @Test
    void shouldMapOnlyUsageWhenAssetAndTagsAreNull() {
        AssetSummary summary = mapper.toSummary(null, null, List.of("usage1"));

        assertThat(summary.usage()).isEqualTo("usage1");
        assertThat(summary.tags()).isEmpty();
        assertThat(summary.name()).isNull();
    }

    @Test
    void shouldMapAssetToDetailWithAllFields() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Asset asset = asset(id, AssetCategory.IMAGE, 1024L);
        asset.applyMetadata(new Asset.Metadata("Foto editada", "Alt text", "Legenda", "Credito"));
        List<AssetUsageSummary> usages = List.of(new AssetUsageSummary("CONTENT", "content-1", "Artigo Home"));

        AssetDetail detail = mapper.toDetail(asset, List.of("blog"), usages);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.name()).isEqualTo("curriculo.png");
        assertThat(detail.friendlyName()).isEqualTo("Foto editada");
        assertThat(detail.altText()).isEqualTo("Alt text");
        assertThat(detail.caption()).isEqualTo("Legenda");
        assertThat(detail.credit()).isEqualTo("Credito");
        assertThat(detail.mimeType()).isEqualTo("image/png");
        assertThat(detail.category()).isEqualTo("image");
        assertThat(detail.sizeBytes()).isEqualTo(1024L);
        assertThat(detail.status()).isEqualTo("ativo");
        assertThat(detail.tags()).containsExactly("blog");
        assertThat(detail.usage()).containsExactly(new AssetUsageSummary("CONTENT", "content-1", "Artigo Home"));
        assertThat(detail.uploadedBySubject()).isEqualTo("subject-1");
        assertThat(detail.createdAt()).isEqualTo(FIXED_CREATED_AT);
    }

    @Test
    void shouldReturnNullDetailWhenAllSourcesAreNull() {
        assertThat(mapper.toDetail(null, null, null)).isNull();
    }

    @Test
    void shouldMapDetailWithNullTagsAndUsageLists() {
        Asset asset = asset(UUID.randomUUID(), AssetCategory.IMAGE, 1024L);

        AssetDetail detail = mapper.toDetail(asset, null, null);

        assertThat(detail.tags()).isNull();
        assertThat(detail.usage()).isNull();
        assertThat(detail.category()).isEqualTo("image");
    }

    @Test
    void shouldMapDetailWithNullAssetButNonNullTags() {
        AssetDetail detail = mapper.toDetail(null, List.of("blog"), null);

        assertThat(detail.tags()).containsExactly("blog");
        assertThat(detail.usage()).isNull();
        assertThat(detail.name()).isNull();
        assertThat(detail.category()).isNull();
    }

    @Test
    void shouldMapDetailWithNullAssetAndTagsButNonNullUsage() {
        List<AssetUsageSummary> usages = List.of(new AssetUsageSummary("CONTENT", "content-1", "Artigo"));

        AssetDetail detail = mapper.toDetail(null, null, usages);

        assertThat(detail.usage()).containsExactlyElementsOf(usages);
        assertThat(detail.tags()).isNull();
        assertThat(detail.name()).isNull();
    }

    @Test
    void shouldExposeNullContractValuesDirectly() {
        assertThat(mapper.toCategoryContractValue(null)).isNull();
        assertThat(mapper.toStatusContractValue(null)).isNull();
    }

    @Test
    void shouldMapUsageToSummary() {
        AssetUsage usage = new AssetUsage(UUID.randomUUID(), "SEO", "seo-1", "Meta description");

        AssetUsageSummary summary = mapper.toUsageSummary(usage);

        assertThat(summary.usedInType()).isEqualTo("SEO");
        assertThat(summary.usedInRefId()).isEqualTo("seo-1");
        assertThat(summary.usedInLabel()).isEqualTo("Meta description");
    }

    @Test
    void shouldReturnNullUsageSummaryWhenSourceIsNull() {
        assertThat(mapper.toUsageSummary(null)).isNull();
    }

    private Asset asset(UUID id, AssetCategory category, long sizeBytes) {
        Asset asset = new Asset(new Asset.Creation(UUID.randomUUID(), UUID.randomUUID(), "curriculo.png", mimeTypeFor(category),
                category, sizeBytes, AssetStorageStrategy.LOCAL, "aegis/pms/x/y/z", "subject-1"));
        ReflectionTestUtils.setField(asset, "id", id);
        ReflectionTestUtils.setField(asset, "friendlyName", "Curriculo");
        ReflectionTestUtils.setField(asset, "createdAt", FIXED_CREATED_AT);
        return asset;
    }

    private String mimeTypeFor(AssetCategory category) {
        return category == AssetCategory.PDF ? "application/pdf" : "image/png";
    }
}
