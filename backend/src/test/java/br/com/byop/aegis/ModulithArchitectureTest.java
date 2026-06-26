package br.com.byop.aegis;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void shouldRespectSpringModulithBoundaries() {
        ApplicationModules.of(AegisApplication.class).verify();
    }
}
