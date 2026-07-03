package br.com.byop.aegis.audit.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditContextHolderTest {

    @Test
    void shouldStoreAndClearCurrentContext() {
        AuditContext context = new AuditContext("trace", "127.0.0.1", "JUnit");

        AuditContextHolder.set(context);

        assertThat(AuditContextHolder.current()).contains(context);

        AuditContextHolder.clear();

        assertThat(AuditContextHolder.current()).isEmpty();
    }
}
