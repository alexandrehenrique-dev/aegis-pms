package br.com.byop.aegis.audit.mapper;

import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import br.com.byop.aegis.audit.dto.AuditEventDetail;
import br.com.byop.aegis.audit.dto.AuditEventSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventMapperTest {

    private final AuditEventMapper mapper = Mappers.getMapper(AuditEventMapper.class);

    @Test
    void shouldMapEventToSummary() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AuditEvent event = event(id, AuditRisk.ALTO, "TENANT_DELETED", null);

        AuditEventSummary summary = mapper.toSummary(event, "Ana Martins", "ACME", "Tenant ACME");

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.actor()).isEqualTo("Ana Martins");
        assertThat(summary.action()).isEqualTo("TENANT_DELETED");
        assertThat(summary.target()).isEqualTo("Tenant ACME");
        assertThat(summary.tenant()).isEqualTo("ACME");
        assertThat(summary.module()).isNull();
        assertThat(summary.time()).isEqualTo("2026-06-27T10:00-03:00");
        assertThat(summary.risk()).isEqualTo("alto");
    }

    @Test
    void shouldMapEventToDetailWithDiffJson() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AuditEvent event = event(id, AuditRisk.MEDIO, "CONTENT_PUBLISHED", "CONTENT");
        ReflectionTestUtils.setField(event, "traceId", "trace-1");
        ReflectionTestUtils.setField(event, "ip", "127.0.0.1");
        ReflectionTestUtils.setField(event, "userAgent", "JUnit");
        Map<String, Object> diff = Map.of("before", Map.of("status", "DRAFT"), "after", Map.of("status", "PUBLISHED"));

        AuditEventDetail detail = mapper.toDetail(event, "Ana Martins", "ACME", "Pagina inicial", diff);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.actor()).isEqualTo("Ana Martins");
        assertThat(detail.action()).isEqualTo("CONTENT_PUBLISHED");
        assertThat(detail.target()).isEqualTo("Pagina inicial");
        assertThat(detail.tenant()).isEqualTo("ACME");
        assertThat(detail.module()).isEqualTo("CONTENT");
        assertThat(detail.risk()).isEqualTo("medio");
        assertThat(detail.diffJson()).isEqualTo(diff);
        assertThat(detail.traceId()).isEqualTo("trace-1");
        assertThat(detail.ip()).isEqualTo("127.0.0.1");
        assertThat(detail.userAgent()).isEqualTo("JUnit");
    }

    @Test
    void shouldReturnNullWhenAllSourcesAreNullForSummary() {
        assertThat(mapper.toSummary(null, null, null, null)).isNull();
    }

    @Test
    void shouldReturnNullWhenAllSourcesAreNullForDetail() {
        assertThat(mapper.toDetail(null, null, null, null, null)).isNull();
    }

    @Test
    void shouldMapOnlyActorWhenEventIsNull() {
        AuditEventSummary summary = mapper.toSummary(null, "Only Actor", null, null);

        assertThat(summary.actor()).isEqualTo("Only Actor");
        assertThat(summary.action()).isNull();
        assertThat(summary.time()).isNull();
        assertThat(summary.risk()).isNull();
    }

    @Test
    void shouldMapNullRiskToNullContractValue() {
        assertThat(mapper.toContractRisk(null)).isNull();
    }

    @Test
    void shouldMapOnlyTenantWhenEventActorAndTargetAreNullForSummary() {
        AuditEventSummary summary = mapper.toSummary(null, null, "ACME", null);

        assertThat(summary.tenant()).isEqualTo("ACME");
        assertThat(summary.actor()).isNull();
        assertThat(summary.target()).isNull();
    }

    @Test
    void shouldMapOnlyTargetWhenEventActorAndTenantAreNullForSummary() {
        AuditEventSummary summary = mapper.toSummary(null, null, null, "Tenant ACME");

        assertThat(summary.target()).isEqualTo("Tenant ACME");
        assertThat(summary.actor()).isNull();
        assertThat(summary.tenant()).isNull();
    }

    @Test
    void shouldMapOnlyTenantWhenOtherSourcesAreNullForDetail() {
        AuditEventDetail detail = mapper.toDetail(null, null, "ACME", null, null);

        assertThat(detail.tenant()).isEqualTo("ACME");
        assertThat(detail.actor()).isNull();
    }

    @Test
    void shouldMapOnlyTargetWhenOtherSourcesAreNullForDetail() {
        AuditEventDetail detail = mapper.toDetail(null, null, null, "Tenant ACME", null);

        assertThat(detail.target()).isEqualTo("Tenant ACME");
        assertThat(detail.actor()).isNull();
    }

    @Test
    void shouldMapOnlyDiffJsonWhenOtherSourcesAreNullForDetail() {
        Map<String, Object> diff = Map.of("status", "DRAFT");

        AuditEventDetail detail = mapper.toDetail(null, null, null, null, diff);

        assertThat(detail.diffJson()).isEqualTo(diff);
        assertThat(detail.actor()).isNull();
    }

    @Test
    void shouldMapOnlyActorWhenEventIsNullForDetail() {
        AuditEventDetail detail = mapper.toDetail(null, "Only Actor", null, null, null);

        assertThat(detail.actor()).isEqualTo("Only Actor");
        assertThat(detail.action()).isNull();
        assertThat(detail.diffJson()).isNull();
    }

    @Test
    void shouldMapDetailWithoutDiffJsonWhenEventHasNoDiff() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        AuditEvent event = event(id, AuditRisk.BAIXO, "TENANT_CREATED", null);

        AuditEventDetail detail = mapper.toDetail(event, "Ana Martins", "ACME", "ACME", null);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.diffJson()).isNull();
    }

    private AuditEvent event(UUID id, AuditRisk risk, String action, String module) {
        AuditEvent event = new AuditEvent(new AuditEvent.Creation(
                UUID.randomUUID(), null, "subject-1", action, "Tenant", id.toString(), null, module, risk,
                null, null, null, null
        ));
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.parse("2026-06-27T10:00:00-03:00"));
        return event;
    }
}
