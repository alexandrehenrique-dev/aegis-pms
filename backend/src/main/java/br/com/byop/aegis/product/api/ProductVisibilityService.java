package br.com.byop.aegis.product.api;

import br.com.byop.aegis.product.service.ProductService;
import br.com.byop.aegis.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resolve quais produtos sao visiveis ao caller, reaproveitando a regra de
 * filtro por papel (ADR-0019) ja implementada em
 * {@code ProductService#listProducts} — nunca duplica a logica de
 * resolucao de papel, apenas adapta o resultado para o contrato publico
 * deste modulo.
 */
@Slf4j
@Service
public class ProductVisibilityService {

    private final ProductService productService;

    public ProductVisibilityService(ProductService productService) {
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<ProductAccessScope> listVisibleProducts(AuthenticatedUser caller) {
        log.debug("listVisibleProducts: caller='{}'", caller.subject());
        return productService.listProducts(caller)
                .stream()
                .map(product -> new ProductAccessScope(product.id(), product.tenantId(), product.status().name()))
                .toList();
    }
}
