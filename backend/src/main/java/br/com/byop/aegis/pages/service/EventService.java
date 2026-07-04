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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class EventService {

    private static final String TARGET_TYPE_EVENT = "Event";
    private static final String MODULE_PAGES = "PAGES";
    private static final String DIFF_KEY_TITLE = "title";
    private static final String DIFF_KEY_DATETIME = "datetime";

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final ProductReferenceService productReferenceService;
    private final AuditService auditService;

    public EventService(EventRepository eventRepository, EventMapper eventMapper,
                        ProductReferenceService productReferenceService, AuditService auditService) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.productReferenceService = productReferenceService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<EventSummary> listEvents(UUID productId) {
        log.debug("listEvents: productId='{}'", productId);
        return eventRepository.findAllByProductIdOrderByDatetimeAsc(productId).stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Transactional
    public EventDetail createEvent(UUID productId, CreateEventRequest request, AuthenticatedUser caller) {
        log.debug("createEvent: productId='{}', title='{}'", productId, request.title());
        ProductReference product = productReferenceService.getRequiredReference(productId);
        EventAccessType type = parseAccessType(request.type());
        EventVisibility visibility = parseVisibility(request.visibility());

        Event event = new Event(product.tenantId(), productId, request.title(), request.datetime(),
                request.location(), type, visibility);
        event.applyEdit(new Event.Edit(request.title(), request.datetime(), request.location(), type, visibility,
                request.description(), request.imageAssetId()));
        eventRepository.save(event);
        recordAudit("EVENT_CREATED", event, caller.subject(), null, currentEventState(event));
        log.info("createEvent: evento criado id='{}', productId='{}'", event.getId(), productId);

        return eventMapper.toDetail(event);
    }

    @Transactional(readOnly = true)
    public EventDetail getEvent(UUID productId, UUID eventId) {
        log.debug("getEvent: productId='{}', eventId='{}'", productId, eventId);
        return eventMapper.toDetail(findEventInProduct(productId, eventId));
    }

    @Transactional
    public EventDetail updateEvent(UUID productId, UUID eventId, UpdateEventRequest request, AuthenticatedUser caller) {
        log.debug("updateEvent: productId='{}', eventId='{}'", productId, eventId);
        Event event = findEventInProduct(productId, eventId);
        Map<String, Object> before = currentEventState(event);
        EventAccessType type = parseAccessType(request.type());
        EventVisibility visibility = parseVisibility(request.visibility());

        event.applyEdit(new Event.Edit(request.title(), request.datetime(), request.location(), type, visibility,
                request.description(), request.imageAssetId()));
        eventRepository.save(event);
        recordAudit("EVENT_UPDATED", event, caller.subject(), before, currentEventState(event));
        log.info("updateEvent: evento atualizado id='{}'", event.getId());

        return eventMapper.toDetail(event);
    }

    @Transactional
    public void deleteEvent(UUID productId, UUID eventId, AuthenticatedUser caller) {
        log.debug("deleteEvent: productId='{}', eventId='{}'", productId, eventId);
        Event event = findEventInProduct(productId, eventId);
        recordAudit("EVENT_DELETED", event, caller.subject(), currentEventState(event), null);
        eventRepository.delete(event);
        log.info("deleteEvent: evento removido id='{}'", eventId);
    }

    private Event findEventInProduct(UUID productId, UUID eventId) {
        return eventRepository.findByProductIdAndId(productId, eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private EventAccessType parseAccessType(String type) {
        try {
            return EventAccessType.fromContractValue(type);
        } catch (IllegalArgumentException _) {
            throw new InvalidEventAccessTypeException(type);
        }
    }

    private EventVisibility parseVisibility(String visibility) {
        try {
            return EventVisibility.fromContractValue(visibility);
        } catch (IllegalArgumentException _) {
            throw new InvalidEventVisibilityException(visibility);
        }
    }

    private void recordAudit(String action, Event event, String actorSubject, Map<String, Object> before,
                             Map<String, Object> after) {
        auditService.recordEvent(new AuditRecordCommand(event.getTenantId(), event.getProductId(), actorSubject,
                action, TARGET_TYPE_EVENT, event.getId().toString(), event.getTitle(), MODULE_PAGES, before, after));
    }

    private Map<String, Object> currentEventState(Event event) {
        return Map.of(DIFF_KEY_TITLE, event.getTitle(), DIFF_KEY_DATETIME, event.getDatetime().toString());
    }
}
