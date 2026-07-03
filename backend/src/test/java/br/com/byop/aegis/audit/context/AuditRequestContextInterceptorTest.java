package br.com.byop.aegis.audit.context;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRequestContextInterceptorTest {

    @Test
    void shouldCaptureTraceIpAndUserAgentThenClearContext() {
        AuditRequestContextInterceptor interceptor = new AuditRequestContextInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Trace-Id", "trace-abc");
        request.addHeader("User-Agent", "JUnit");

        boolean proceed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(proceed).isTrue();
        assertThat(AuditContextHolder.current())
                .contains(new AuditContext("trace-abc", "10.0.0.1", "JUnit"));

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertThat(AuditContextHolder.current()).isEmpty();
    }

    @Test
    void shouldGenerateTraceIdWhenHeaderIsMissingAndLimitLongValues() {
        AuditRequestContextInterceptor interceptor = new AuditRequestContextInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1".repeat(80));
        request.addHeader("User-Agent", "A".repeat(600));

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        AuditContext context = AuditContextHolder.current().orElseThrow();
        assertThat(context.traceId()).hasSize(36);
        assertThat(context.ip()).hasSize(64);
        assertThat(context.userAgent()).hasSize(512);

        AuditContextHolder.clear();
    }

    @Test
    void shouldUseFallbacksForBlankRequestValues() {
        AuditRequestContextInterceptor interceptor = new AuditRequestContextInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(" ");
        request.addHeader("X-Trace-Id", " ");
        request.addHeader("User-Agent", " ");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        AuditContext context = AuditContextHolder.current().orElseThrow();
        assertThat(context.traceId()).hasSize(36);
        assertThat(context.ip()).isEmpty();
        assertThat(context.userAgent()).isEmpty();

        AuditContextHolder.clear();
    }
}
