package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductReferenceService {

    private final ProductRepository productRepository;

    public ProductReferenceService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ProductReference getRequiredReference(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return new ProductReference(product.getId(), product.getTenantId());
    }

    /**
     * Estrategia de storage de assets configurada para o produto no momento
     * da chamada — usada apenas no momento do upload (etapa 12, dominio
     * {@code asset}); um asset ja existente preserva o {@code storageProvider}
     * gravado no momento do proprio upload, mesmo que esta configuracao mude depois.
     *
     * @param productId identificador do produto
     * @return estrategia de storage atual do produto
     */
    @Transactional(readOnly = true)
    public AssetStorageStrategy getRequiredAssetStorageStrategy(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId))
                .getAssetStorageStrategy();
    }

    /**
     * Renomeia um produto — usado por {@code settings.service.SettingsService}
     * (etapa 17, {@code PUT /products/{productId}/settings}). O chamador
     * precisa ter validado o escopo do produto antes (via
     * {@link ProductVisibilityService}); este metodo nao repete a checagem
     * de acesso, apenas a mutacao.
     *
     * @param productId identificador do produto
     * @param name novo nome do produto
     */
    @Transactional
    public void renameProduct(UUID productId, String name) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.rename(name);
    }
}
