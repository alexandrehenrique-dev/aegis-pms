package br.com.byop.aegis.asset.service;

import br.com.byop.aegis.asset.storage.LocalStorageProvider;
import br.com.byop.aegis.asset.storage.S3StorageProvider;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssetStorageProvisioningServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private LocalStorageProvider localStorageProvider;

    @Mock
    private S3StorageProvider s3StorageProvider;

    private AssetStorageProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new AssetStorageProvisioningService(localStorageProvider, s3StorageProvider);
    }

    @Test
    void shouldResolveLocalProvider() {
        assertThat(service.resolveProvider(AssetStorageStrategy.LOCAL)).isSameAs(localStorageProvider);
    }

    @Test
    void shouldResolveS3Provider() {
        assertThat(service.resolveProvider(AssetStorageStrategy.S3)).isSameAs(s3StorageProvider);
    }

    @Test
    void shouldProvisionLocalFoldersForLocalStrategy() {
        service.provisionFor(TENANT_ID, PRODUCT_ID, AssetStorageStrategy.LOCAL);

        verify(localStorageProvider).provisionProductFolders(TENANT_ID, PRODUCT_ID);
        verify(s3StorageProvider, never()).provisionProductFolders(TENANT_ID, PRODUCT_ID);
    }

    @Test
    void shouldProvisionS3NoOpForS3Strategy() {
        service.provisionFor(TENANT_ID, PRODUCT_ID, AssetStorageStrategy.S3);

        verify(s3StorageProvider).provisionProductFolders(TENANT_ID, PRODUCT_ID);
        verify(localStorageProvider, never()).provisionProductFolders(TENANT_ID, PRODUCT_ID);
    }

    @Test
    void shouldProvisionOnProductCreatedEvent() {
        service.onProductCreated(new ProductCreatedEvent(TENANT_ID, PRODUCT_ID, AssetStorageStrategy.LOCAL,
                "SITE_INSTITUCIONAL", "pt-BR"));

        verify(localStorageProvider).provisionProductFolders(TENANT_ID, PRODUCT_ID);
    }
}
