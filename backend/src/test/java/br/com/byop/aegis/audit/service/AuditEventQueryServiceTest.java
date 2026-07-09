package br.com.byop.aegis.audit.service;

import br.com.byop.aegis.audit.api.TenantVisibilityPort;
import br.com.byop.aegis.audit.domain.AuditEvent;
import br.com.byop.aegis.audit.domain.AuditRisk;
import br.com.byop.aegis.audit.dto.AuditEventDetail;
import br.com.byop.aegis.audit.dto.AuditEventPage;
import br.com.byop.aegis.audit.dto.AuditEventPageQuery;
import br.com.byop.aegis.audit.dto.AuditEventSummary;
import br.com.byop.aegis.audit.exception.AuditEventNotFoundException;
import br.com.byop.aegis.audit.exception.InvalidAuditRiskFilterException;
import br.com.byop.aegis.audit.mapper.AuditEventMapper;
import br.com.byop.aegis.audit.repository.AuditEventRepository;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.identity.api.IdentityUserDirectory;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventQueryServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private AuditEventMapper auditEventMapper;

    @Mock
    private TenantVisibilityPort tenantVisibilityPort;

    @Mock
    private IdentityUserDirectory identityUserDirectory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditEventQueryService service() {
        return new AuditEventQueryService(auditEventRepository, auditEventMapper, tenantVisibilityPort,
                identityUserDirectory, objectMapper);
    }

    @Test
    void shouldListEventsWhenSuperAdmin() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "TENANT_UPDATED", "Tenant", null, "ACME");
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).thenReturn(List.of(event));
        AuditEventSummary expected = new AuditEventSummary(event.getId(), "Ana Martins", "TENANT_UPDATED", "ACME",
                "ACME", null, "2026-06-27T10:00-03:00", "medio");
        when(auditEventMapper.toSummary(event, "Ana Martins", "ACME", "ACME")).thenReturn(expected);

        List<AuditEventSummary> result = service().listEvents(caller, tenantId, null, null, null, null);

        assertThat(result).containsExactly(expected);
    }

    @Test
    void shouldListEventsWhenActiveMember() {
        AuthenticatedUser caller = caller(Set.of("ROLE_TENANT_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        when(tenantVisibilityPort.hasActiveMembership(tenantId, "subject-1")).thenReturn(true);
        when(auditEventRepository.findAllByFilters(tenantId, "subject-1", null, "CONTENT", AuditRisk.BAIXO))
                .thenReturn(List.of());

        List<AuditEventSummary> result = service().listEvents(caller, tenantId, "subject-1", null, "CONTENT", "baixo");

        assertThat(result).isEmpty();
        verify(auditEventRepository).findAllByFilters(tenantId, "subject-1", null, "CONTENT", AuditRisk.BAIXO);
    }

    @Test
    void shouldListPagedEventsWithBoundedSize() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "TENANT_UPDATED", "Tenant", null, "ACME");
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        PageRequest pageRequest = PageRequest.of(0, 100);
        when(auditEventRepository.findPageByFiltersAndQuery(eq(tenantId), isNull(), isNull(), isNull(), isNull(), eq("%tenant%"),
                eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(event), pageRequest, 1));
        AuditEventSummary expected = new AuditEventSummary(event.getId(), "Ana Martins", "TENANT_UPDATED", "ACME",
                "ACME", null, "2026-06-27T10:00-03:00", "medio");
        when(auditEventMapper.toSummary(event, "Ana Martins", "ACME", "ACME")).thenReturn(expected);

        AuditEventPageQuery pageQuery = new AuditEventPageQuery(null, null, null, null, " tenant ", -1, 500);
        AuditEventPage result = service().listEventsPage(caller, tenantId, pageQuery);

        assertThat(result.items()).containsExactly(expected);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(100);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldListPagedEventsWithoutSearchQueryUsingMinimumSize() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(2, 1);
        when(auditEventRepository.findPageByFilters(tenantId, null, null, null, null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AuditEventPageQuery pageQuery = new AuditEventPageQuery(null, null, null, null, "   ", 2, 0);
        AuditEventPage result = service().listEventsPage(caller, tenantId, pageQuery);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isZero();
        verify(auditEventRepository).findPageByFilters(tenantId, null, null, null, null, pageRequest);
    }

    @Test
    void shouldListPagedEventsWithoutSearchQueryWhenQueryIsAbsent() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(auditEventRepository.findPageByFilters(tenantId, null, null, null, null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AuditEventPageQuery pageQuery = new AuditEventPageQuery(null, null, null, null, null, 0, 10);
        AuditEventPage result = service().listEventsPage(caller, tenantId, pageQuery);

        assertThat(result.items()).isEmpty();
        assertThat(result.size()).isEqualTo(10);
        verify(auditEventRepository).findPageByFilters(tenantId, null, null, null, null, pageRequest);
    }

    @Test
    void shouldRejectListingWhenTenantNotVisible() {
        AuthenticatedUser caller = caller(Set.of("ROLE_VIEWER"));
        UUID tenantId = UUID.randomUUID();
        when(tenantVisibilityPort.hasActiveMembership(tenantId, "subject-1")).thenReturn(false);
        AuditEventQueryService service = service();

        assertThatThrownBy(() -> service.listEvents(caller, tenantId, null, null, null, null))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    @Test
    void shouldRejectInvalidRiskFilter() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEventQueryService service = service();

        assertThatThrownBy(() -> service.listEvents(caller, tenantId, null, null, null, "inexistente"))
                .isInstanceOf(InvalidAuditRiskFilterException.class);
    }

    @Test
    void shouldFallBackToSubjectWhenActorCannotBeResolved() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "TENANT_DELETED", "Tenant", null, "ACME");
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn(null);
        when(identityUserDirectory.getRequiredUser("subject-1")).thenThrow(new RuntimeException("not found"));
        when(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).thenReturn(List.of(event));

        service().listEvents(caller, tenantId, null, null, null, null);

        verify(auditEventMapper).toSummary(event, "subject-1", "ACME", "ACME");
    }

    @Test
    void shouldFallBackToRawTenantIdWhenTenantDeletedAndEventIsNotAboutTheTenant() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "USER_REMOVED_FROM_TENANT", "User", "user-1", null);
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn(null);
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).thenReturn(List.of(event));

        service().listEvents(caller, tenantId, null, null, null, null);

        verify(auditEventMapper).toSummary(event, "Ana Martins", String.valueOf(tenantId), "User user-1");
    }

    @Test
    void shouldFallBackToRawTenantIdWhenTenantDeletedAndEventTargetsTenantWithoutLabel() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "TENANT_DELETED", "Tenant", null, null);
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn(null);
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).thenReturn(List.of(event));

        service().listEvents(caller, tenantId, null, null, null, null);

        verify(auditEventMapper).toSummary(event, "Ana Martins", String.valueOf(tenantId), "Tenant");
    }

    @Test
    void shouldResolveTargetWithoutLabelOrId() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "TENANT_UPDATED", null, null, null);
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).thenReturn(List.of(event));

        service().listEvents(caller, tenantId, null, null, null, null);

        verify(auditEventMapper).toSummary(eq(event), eq("Ana Martins"), eq("ACME"), isNull());
    }

    @Test
    void shouldResolveTargetTypeOnlyWhenTargetIdIsAbsent() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = event(tenantId, "TENANT_UPDATED", "Tenant", null, null);
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findAllByFilters(tenantId, null, null, null, null)).thenReturn(List.of(event));

        service().listEvents(caller, tenantId, null, null, null, null);

        verify(auditEventMapper).toSummary(event, "Ana Martins", "ACME", "Tenant");
    }

    @Test
    void shouldGetEventDetailWithParsedDiffJson() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = eventWithDiff(tenantId, "{\"before\":{\"status\":\"DRAFT\"},\"after\":{\"status\":\"PUBLISHED\"}}");
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findByIdAndTenantId(event.getId(), tenantId)).thenReturn(Optional.of(event));
        AuditEventDetail expected = new AuditEventDetail(event.getId(), "Ana Martins", "CONTENT_PUBLISHED", "ACME",
                "ACME", "CONTENT", "2026-06-27T10:00-03:00", "medio", null, null, null, null);
        when(auditEventMapper.toDetail(eq(event), eq("Ana Martins"), eq("ACME"), any(), any())).thenReturn(expected);

        AuditEventDetail result = service().getEvent(caller, tenantId, event.getId());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenEventNotFoundForTenant() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(auditEventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.empty());
        AuditEventQueryService service = service();

        assertThatThrownBy(() -> service.getEvent(caller, tenantId, eventId))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    @Test
    void shouldReturnEmptyDiffJsonWhenAbsent() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = eventWithDiff(tenantId, null);
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findByIdAndTenantId(event.getId(), tenantId)).thenReturn(Optional.of(event));

        service().getEvent(caller, tenantId, event.getId());

        verify(auditEventMapper).toDetail(eq(event), eq("Ana Martins"), eq("ACME"), any(), eq(Map.of()));
    }

    @Test
    void shouldReturnEmptyDiffJsonWhenBlank() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = eventWithDiff(tenantId, "   ");
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findByIdAndTenantId(event.getId(), tenantId)).thenReturn(Optional.of(event));

        service().getEvent(caller, tenantId, event.getId());

        verify(auditEventMapper).toDetail(eq(event), eq("Ana Martins"), eq("ACME"), any(), eq(Map.of()));
    }

    @Test
    void shouldThrowWhenDiffJsonIsMalformed() {
        AuthenticatedUser caller = caller(Set.of("ROLE_SUPER_ADMIN"));
        UUID tenantId = UUID.randomUUID();
        AuditEvent event = eventWithDiff(tenantId, "{not-json");
        when(tenantVisibilityPort.findTenantName(tenantId)).thenReturn("ACME");
        when(identityUserDirectory.getRequiredUser("subject-1")).thenReturn(identityUser("Ana Martins"));
        when(auditEventRepository.findByIdAndTenantId(event.getId(), tenantId)).thenReturn(Optional.of(event));
        AuditEventQueryService service = service();
        UUID eventId = event.getId();

        assertThatThrownBy(() -> service.getEvent(caller, tenantId, eventId))
                .isInstanceOf(IllegalStateException.class);
    }

    private AuthenticatedUser caller(Set<String> authorities) {
        return new AuthenticatedUser("subject-1", "ana@acme.com", "ana", "Ana Martins", authorities);
    }

    private IdentityUser identityUser(String displayName) {
        String[] parts = displayName.split(" ", 2);
        return new IdentityUser("subject-1", "ana", "ana@acme.com", parts[0], parts.length > 1 ? parts[1] : "");
    }

    private AuditEvent event(UUID tenantId, String action, String targetType, String targetId, String targetLabel) {
        AuditEvent event = new AuditEvent(new AuditEvent.Creation(
                tenantId, null, "subject-1", action, targetType, targetId, targetLabel, null,
                AuditRisk.MEDIO, null, null, null, null
        ));
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.parse("2026-06-27T10:00:00-03:00"));
        return event;
    }

    private AuditEvent eventWithDiff(UUID tenantId, String diffJson) {
        AuditEvent event = new AuditEvent(new AuditEvent.Creation(
                tenantId, UUID.randomUUID(), "subject-1", "CONTENT_PUBLISHED", "Content", UUID.randomUUID().toString(),
                "Pagina inicial", "CONTENT", AuditRisk.MEDIO, diffJson, null, null, null
        ));
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "createdAt", OffsetDateTime.parse("2026-06-27T10:00:00-03:00"));
        return event;
    }
}
