package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductStatus;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.api.TenantProductExportPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductExportCoordinatorService implements TenantProductExportPort {

    private final ProductRepository productRepository;
    private final ExportAndDeleteService exportAndDeleteService;

    public ProductExportCoordinatorService(ProductRepository productRepository, ExportAndDeleteService exportAndDeleteService) {
        this.productRepository = productRepository;
        this.exportAndDeleteService = exportAndDeleteService;
    }

    @Override
    @Transactional
    public void startTenantProductExports(UUID tenantId, AuthenticatedUser caller) {
        productRepository.findAllByTenantId(tenantId).stream()
                .filter(product -> product.getStatus() != ProductStatus.DELETED)
                .forEach(product -> startProductExport(product, caller));
    }

    private void startProductExport(Product product, AuthenticatedUser caller) {
        product.markDeleting();
        productRepository.save(product);
        exportAndDeleteService.exportAndDelete(product.getId(), caller.subject(), caller.email(), caller.name(), true);
    }
}
