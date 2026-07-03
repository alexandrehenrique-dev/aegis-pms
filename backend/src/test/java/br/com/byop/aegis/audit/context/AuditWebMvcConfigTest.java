package br.com.byop.aegis.audit.context;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditWebMvcConfigTest {

    @Test
    void shouldRegisterAuditRequestContextInterceptor() {
        AuditRequestContextInterceptor interceptor = new AuditRequestContextInterceptor();
        AuditWebMvcConfig config = new AuditWebMvcConfig(interceptor);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
    }
}
