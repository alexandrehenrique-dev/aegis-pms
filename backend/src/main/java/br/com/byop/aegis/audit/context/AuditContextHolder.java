package br.com.byop.aegis.audit.context;

import java.util.Optional;

/**
 * Holder por thread do contexto de auditoria da request atual.
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> CURRENT = new ThreadLocal<>();

    private AuditContextHolder() {
    }

    public static void set(AuditContext context) {
        CURRENT.set(context);
    }

    public static Optional<AuditContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
