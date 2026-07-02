package br.com.byop.aegis.pages.controller;

import br.com.byop.aegis.pages.contract.CreateEventRequest;
import br.com.byop.aegis.pages.contract.UpdateEventRequest;
import br.com.byop.aegis.pages.dto.EventDetail;
import br.com.byop.aegis.pages.dto.EventSummary;
import br.com.byop.aegis.pages.service.EventService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Agenda de eventos de um produto — entidade propria (Secao A.1 da Sprint 23),
 * gateada pelo mesmo modulo {@code PAGES} do {@link PageController} (nenhum
 * modulo novo criado para este dominio).
 */
@RestController
@RequireModule(ModuleKey.PAGES)
public class EventController {

    private final EventService eventService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public EventController(EventService eventService, ProductAccessPort productAccessPort,
                           AuthenticatedUserProvider authenticatedUserProvider) {
        this.eventService = eventService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/events")
    public List<EventSummary> listEvents(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return eventService.listEvents(productId);
    }

    @PostMapping("/api/v1/products/{productId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDetail createEvent(@PathVariable("productId") UUID productId,
                                   @Valid @RequestBody CreateEventRequest request,
                                   Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return eventService.createEvent(productId, request, caller);
    }

    @GetMapping("/api/v1/products/{productId}/events/{eventId}")
    public EventDetail getEvent(@PathVariable("productId") UUID productId,
                                @PathVariable("eventId") UUID eventId,
                                Authentication authentication) {
        assertProductAccess(authentication, productId);
        return eventService.getEvent(productId, eventId);
    }

    @PutMapping("/api/v1/products/{productId}/events/{eventId}")
    public EventDetail updateEvent(@PathVariable("productId") UUID productId,
                                   @PathVariable("eventId") UUID eventId,
                                   @Valid @RequestBody UpdateEventRequest request,
                                   Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        return eventService.updateEvent(productId, eventId, request, caller);
    }

    @DeleteMapping("/api/v1/products/{productId}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable("productId") UUID productId,
                            @PathVariable("eventId") UUID eventId,
                            Authentication authentication) {
        AuthenticatedUser caller = assertProductAccess(authentication, productId);
        eventService.deleteEvent(productId, eventId, caller);
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }
}
