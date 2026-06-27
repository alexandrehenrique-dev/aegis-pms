package br.com.byop.aegis.asset.api;

import br.com.byop.aegis.asset.domain.Asset;
import br.com.byop.aegis.asset.domain.AssetCategory;
import br.com.byop.aegis.asset.exception.AssetNotFoundException;
import br.com.byop.aegis.asset.repository.AssetRepository;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetReferenceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASSET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private AssetRepository assetRepository;

    @Test
    void shouldReturnAssetReference() {
        Asset asset = asset();
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

        AssetReference reference = new AssetReferenceService(assetRepository).getRequiredReference(ASSET_ID);

        assertThat(reference.id()).isEqualTo(ASSET_ID);
        assertThat(reference.productId()).isEqualTo(PRODUCT_ID);
        assertThat(reference.mimeType()).isEqualTo("application/pdf");
    }

    @Test
    void shouldRejectMissingAssetReference() {
        AssetReferenceService service = new AssetReferenceService(assetRepository);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredReference(ASSET_ID))
                .isInstanceOf(AssetNotFoundException.class);
    }

    private Asset asset() {
        Asset asset = new Asset(new Asset.Creation(
                TENANT_ID, PRODUCT_ID, "documento.pdf", "application/pdf", AssetCategory.DOCUMENT,
                1024L, AssetStorageStrategy.LOCAL, "products/documento.pdf", "user-1"
        ));
        ReflectionTestUtils.setField(asset, "id", ASSET_ID);
        return asset;
    }
}
