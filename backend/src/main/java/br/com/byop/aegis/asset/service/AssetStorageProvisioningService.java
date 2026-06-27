package br.com.byop.aegis.asset.service;

import br.com.byop.aegis.asset.storage.LocalStorageProvider;
import br.com.byop.aegis.asset.storage.S3StorageProvider;
import br.com.byop.aegis.asset.storage.StorageProvider;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ProductCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Resolve o {@link StorageProvider} correto por {@link AssetStorageStrategy}
 * e provisiona a estrutura de pastas de um produto. Escuta {@link ProductCreatedEvent}
 * (publicado pelo modulo {@code product} apos a criacao do produto) para
 * provisionar automaticamente, sem nenhuma acao manual do usuario no sistema
 * operacional — evita que {@code product} dependa de {@code asset} (ciclo entre
 * modulos), conforme decisao registrada na Sprint 12.
 */
@Service
public class AssetStorageProvisioningService {

    private final LocalStorageProvider localStorageProvider;
    private final S3StorageProvider s3StorageProvider;

    public AssetStorageProvisioningService(LocalStorageProvider localStorageProvider, S3StorageProvider s3StorageProvider) {
        this.localStorageProvider = localStorageProvider;
        this.s3StorageProvider = s3StorageProvider;
    }

    public StorageProvider resolveProvider(AssetStorageStrategy strategy) {
        return switch (strategy) {
            case LOCAL -> localStorageProvider;
            case S3 -> s3StorageProvider;
        };
    }

    public void provisionFor(UUID tenantId, UUID productId, AssetStorageStrategy strategy) {
        resolveProvider(strategy).provisionProductFolders(tenantId, productId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductCreated(ProductCreatedEvent event) {
        provisionFor(event.tenantId(), event.productId(), event.assetStorageStrategy());
    }
}
