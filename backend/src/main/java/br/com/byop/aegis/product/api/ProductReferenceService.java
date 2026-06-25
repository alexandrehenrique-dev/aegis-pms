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
}
