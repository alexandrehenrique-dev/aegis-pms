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

        assignment.prePersist();
        assertThat(assignment.getCreatedAt()).isNotNull();
        assertThat(assignment.getUpdatedAt()).isNotNull();

        assignment.preUpdate();
        assertThat(assignment.getUpdatedAt()).isNotNull();
    }
}
