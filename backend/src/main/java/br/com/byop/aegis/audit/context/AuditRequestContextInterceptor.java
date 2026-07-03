package br.com.byop.aegis.audit.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AuditRequestContextInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final int TRACE_ID_MAX_LENGTH = 80;
    private static final int IP_MAX_LENGTH = 64;
    private static final int USER_AGENT_MAX_LENGTH = 512;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuditContextHolder.set(new AuditContext(
                firstPresent(request.getHeader(TRACE_ID_HEADER), UUID.randomUUID().toString(), TRACE_ID_MAX_LENGTH),
                firstPresent(request.getRemoteAddr(), "", IP_MAX_LENGTH),
                firstPresent(request.getHeader(USER_AGENT_HEADER), "", USER_AGENT_MAX_LENGTH)
        ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        AuditContextHolder.clear();
    }

    private String firstPresent(String value, String fallback, int maxLength) {
        String selected = value == null || value.isBlank() ? fallback : value.trim();
        return selected.length() <= maxLength ? selected : selected.substring(0, maxLength);
    }
}
