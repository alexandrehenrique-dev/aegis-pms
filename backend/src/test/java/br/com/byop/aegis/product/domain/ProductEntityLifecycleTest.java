package br.com.byop.aegis.product.domain;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityLifecycleTest {

    @Test
    void shouldRunProductJpaLifecycleCallbacks() {
        Product product = new Product();

        product.prePersist();
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();

        product.preUpdate();
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldTransitionProductDeletionStatuses() {
        Product product = new Product();

        product.markDeleting();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETING);

        product.markDeleted();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);

        product.markExportFailed();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.EXPORT_FAILED);

        product.markDeleteFailed();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETE_FAILED);
    }

    @Test
    void shouldRunProductModuleJpaLifecycleCallbacks() {
        ProductModule module = new ProductModule();

        module.prePersist();
        assertThat(module.getCreatedAt()).isNotNull();
        assertThat(module.getUpdatedAt()).isNotNull();

        module.preUpdate();
        assertThat(module.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRunProductAssignmentJpaLifecycleCallbacks() {
        ProductAssignment assignment = new ProductAssignment();

        assertThat(assignment.getProductId()).isNull();

        assignment.prePersist();
        assertThat(assignment.getCreatedAt()).isNotNull();
        assertThat(assignment.getUpdatedAt()).isNotNull();

        assignment.preUpdate();
        assertThat(assignment.getUpdatedAt()).isNotNull();
    }
}
