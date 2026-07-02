package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.pages.contract.CreateEventRequest;
import br.com.byop.aegis.pages.contract.UpdateEventRequest;
import br.com.byop.aegis.pages.domain.Event;
import br.com.byop.aegis.pages.domain.EventAccessType;
import br.com.byop.aegis.pages.domain.EventVisibility;
import br.com.byop.aegis.pages.dto.EventDetail;
import br.com.byop.aegis.pages.dto.EventSummary;
import br.com.byop.aegis.pages.exception.EventNotFoundException;
import br.com.byop.aegis.pages.exception.InvalidEventAccessTypeException;
import br.com.byop.aegis.pages.exception.InvalidEventVisibilityException;
import br.com.byop.aegis.pages.mapper.EventMapper;
import br.com.byop.aegis.pages.repository.EventRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import br.com.byop.aegis.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final AuthenticatedUser CALLER = new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_EDITOR"));

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private AuditService auditService;

    private EventService service;

    @BeforeEach
    void setUp() {
        EventMapper mapper = Mappers.getMapper(EventMapper.class);
        service = new EventService(eventRepository, mapper, productReferenceService, auditService);
    }

    @Test
    void shouldListEvents() {
        Event event = event(UUID.randomUUID());
        when(eventRepository.findAllByProductIdOrderByDatetimeAsc(PRODUCT_ID)).thenReturn(List.of(event));

        List<EventSummary> summaries = service.listEvents(PRODUCT_ID);

        assertThat(summaries).extracting(EventSummary::title).containsExactly("Show ao vivo");
    }

    @Test
    void shouldCreateEvent() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        CreateEventRequest request = new CreateEventRequest("Show ao vivo", LocalDateTime.parse("2026-07-12T16:00"),
                "Teatro Municipal", "public", "public", "descricao", null);
        EventDetail result = service.createEvent(PRODUCT_ID, request, CALLER);

        assertThat(result.title()).isEqualTo("Show ao vivo");
        assertThat(result.type()).isEqualTo("public");
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("EVENT_CREATED");
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void shouldRejectCreateEventWithInvalidType() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        CreateEventRequest request = new CreateEventRequest("Show", LocalDateTime.parse("2026-07-12T16:00"),
                "Local", "unlisted", "public", null, null);

        assertThatThrownBy(() -> service.createEvent(PRODUCT_ID, request, CALLER))
                .isInstanceOf(InvalidEventAccessTypeException.class);
    }

    @Test
    void shouldRejectCreateEventWithInvalidVisibility() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        CreateEventRequest request = new CreateEventRequest("Show", LocalDateTime.parse("2026-07-12T16:00"),
                "Local", "public", "unlisted", null, null);

        assertThatThrownBy(() -> service.createEvent(PRODUCT_ID, request, CALLER))
                .isInstanceOf(InvalidEventVisibilityException.class);
    }

    @Test
    void shouldGetEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId);
        when(eventRepository.findByProductIdAndId(PRODUCT_ID, eventId)).thenReturn(Optional.of(event));

        EventDetail detail = service.getEvent(PRODUCT_ID, eventId);

        assertThat(detail.id()).isEqualTo(eventId);
    }

    @Test
    void shouldRejectGetEventWhenNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findByProductIdAndId(PRODUCT_ID, eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEvent(PRODUCT_ID, eventId)).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void shouldUpdateEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId);
        when(eventRepository.findByProductIdAndId(PRODUCT_ID, eventId)).thenReturn(Optional.of(event));

        UpdateEventRequest request = new UpdateEventRequest("Show remarcado", LocalDateTime.parse("2026-08-01T20:30"),
                "Novo local", "private", "public-summary", "nova descricao", null);
        EventDetail result = service.updateEvent(PRODUCT_ID, eventId, request, CALLER);

        assertThat(result.title()).isEqualTo("Show remarcado");
        assertThat(result.visibility()).isEqualTo("public-summary");
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("EVENT_UPDATED");
    }

    @Test
    void shouldDeleteEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId);
        when(eventRepository.findByProductIdAndId(PRODUCT_ID, eventId)).thenReturn(Optional.of(event));

        service.deleteEvent(PRODUCT_ID, eventId, CALLER);

        verify(eventRepository).delete(event);
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("EVENT_DELETED");
    }

    private Event event(UUID id) {
        Event event = new Event(TENANT_ID, PRODUCT_ID, "Show ao vivo", LocalDateTime.parse("2026-07-12T16:00"),
                "Teatro Municipal", EventAccessType.PUBLIC, EventVisibility.PUBLIC);
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
