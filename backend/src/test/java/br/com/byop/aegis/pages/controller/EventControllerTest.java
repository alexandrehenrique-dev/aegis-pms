package br.com.byop.aegis.pages.controller;

import br.com.byop.aegis.core.CoreExceptionHandler;
import br.com.byop.aegis.pages.dto.EventDetail;
import br.com.byop.aegis.pages.dto.EventSummary;
import br.com.byop.aegis.pages.exception.EventNotFoundException;
import br.com.byop.aegis.pages.exception.InvalidEventAccessTypeException;
import br.com.byop.aegis.pages.exception.PageExceptionHandler;
import br.com.byop.aegis.pages.service.EventService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.product.exception.ProductExceptionHandler;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import br.com.byop.aegis.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import({
        SecurityConfig.class,
        CoreExceptionHandler.class,
        ProductExceptionHandler.class,
        PageExceptionHandler.class
})
class EventControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private ProductAccessPort productAccessPort;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Test
    void shouldListEvents() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.listEvents(PRODUCT_ID)).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/v1/products/{productId}/events", PRODUCT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Show ao vivo"));
    }

    @Test
    void shouldCreateEvent() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.createEvent(eq(PRODUCT_ID), any(), eq(caller))).thenReturn(detail());

        mockMvc.perform(post("/api/v1/products/{productId}/events", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Show ao vivo","datetime":"2026-07-12T16:00","location":"Teatro Municipal","type":"public","visibility":"public"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Show ao vivo"));
    }

    @Test
    void shouldRejectCreateEventWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/events", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"","datetime":"2026-07-12T16:00","location":"Local","type":"public","visibility":"public"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreateEventWithInvalidType() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.createEvent(eq(PRODUCT_ID), any(), eq(caller)))
                .thenThrow(new InvalidEventAccessTypeException("unlisted"));

        mockMvc.perform(post("/api/v1/products/{productId}/events", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Show","datetime":"2026-07-12T16:00","location":"Local","type":"unlisted","visibility":"public"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EVENT_TYPE"));
    }

    @Test
    void shouldRejectCreateEventWithInvalidVisibility() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.createEvent(eq(PRODUCT_ID), any(), eq(caller)))
                .thenThrow(new br.com.byop.aegis.pages.exception.InvalidEventVisibilityException("unlisted"));

        mockMvc.perform(post("/api/v1/products/{productId}/events", PRODUCT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Show","datetime":"2026-07-12T16:00","location":"Local","type":"public","visibility":"unlisted"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EVENT_VISIBILITY"));
    }

    @Test
    void shouldGetEvent() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.getEvent(PRODUCT_ID, EVENT_ID)).thenReturn(detail());

        mockMvc.perform(get("/api/v1/products/{productId}/events/{eventId}", PRODUCT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Teatro Municipal"));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExistInProduct() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.getEvent(PRODUCT_ID, EVENT_ID)).thenThrow(new EventNotFoundException(EVENT_ID));

        mockMvc.perform(get("/api/v1/products/{productId}/events/{eventId}", PRODUCT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVENT_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenProductOutsideScope() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productAccessPort).assertAccessible(PRODUCT_ID, caller);

        mockMvc.perform(get("/api/v1/products/{productId}/events", PRODUCT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldUpdateEvent() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);
        when(eventService.updateEvent(eq(PRODUCT_ID), eq(EVENT_ID), any(), eq(caller))).thenReturn(detail());

        mockMvc.perform(put("/api/v1/products/{productId}/events/{eventId}", PRODUCT_ID, EVENT_ID)
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Show ao vivo","datetime":"2026-07-12T16:00","location":"Teatro Municipal","type":"public","visibility":"public"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteEvent() throws Exception {
        AuthenticatedUser caller = user();
        when(authenticatedUserProvider.from(any(Authentication.class))).thenReturn(caller);

        mockMvc.perform(delete("/api/v1/products/{productId}/events/{eventId}", PRODUCT_ID, EVENT_ID).with(jwt()))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(PRODUCT_ID, EVENT_ID, caller);
    }

    @Test
    void shouldRequirePagesModule() {
        RequireModule requireModule = EventController.class.getAnnotation(RequireModule.class);

        assertThat(requireModule).isNotNull();
        assertThat(requireModule.value()).isEqualTo(ModuleKey.PAGES);
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_EDITOR"));
    }

    private EventSummary summary() {
        return new EventSummary(EVENT_ID, "Show ao vivo", LocalDateTime.parse("2026-07-12T16:00"), "Teatro Municipal", "public", "public");
    }

    private EventDetail detail() {
        return new EventDetail(EVENT_ID, "Show ao vivo", LocalDateTime.parse("2026-07-12T16:00"), "Teatro Municipal",
                "public", "public", "descricao", null);
    }
}
