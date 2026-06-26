package br.com.byop.aegis.tenant.mapper;

import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperTest {

    private final TenantMapper mapper = Mappers.getMapper(TenantMapper.class);

    @Test
    void shouldMapEntityToSummary() {
        UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-25T10:00:00-03:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-25T10:10:00-03:00");
        Tenant tenant = new Tenant("byop", "BYOP");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        ReflectionTestUtils.setField(tenant, "createdAt", createdAt);
        ReflectionTestUtils.setField(tenant, "updatedAt", updatedAt);

        TenantSummary summary = mapper.toSummary(tenant);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(tenantId);
        assertThat(summary.key()).isEqualTo("byop");
        assertThat(summary.name()).isEqualTo("BYOP");
        assertThat(summary.status()).isEqualTo("ativo");
        assertThat(summary.plan()).isEqualTo("FREE");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldMapSuspendedAndArchivedStatusToContractValues() {
        Tenant suspended = new Tenant("suspended", "Suspended");
        Tenant archived = new Tenant("archived", "Archived");
        suspended.suspend();
        archived.archive();

        assertThat(mapper.toSummary(suspended).status()).isEqualTo("suspenso");
        assertThat(mapper.toSummary(archived).status()).isEqualTo("arquivado");
    }

    @Test
    void shouldMapCustomPlan() {
        Tenant tenant = new Tenant("byop-pro", "BYOP Pro");
        tenant.changePlan("PRO");

        TenantSummary summary = mapper.toSummary(tenant);

        assertThat(summary.plan()).isEqualTo("PRO");
    }

    @Test
    void shouldReturnNullSummaryWhenTenantIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
    }

    @Test
    void shouldReturnNullContractStatusWhenStatusIsNull() {
        assertThat(mapper.toContractStatus(null)).isNull();
    }
}
