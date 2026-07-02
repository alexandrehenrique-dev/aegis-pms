package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.pages.contract.FloatingWhatsappRequest;
import br.com.byop.aegis.pages.contract.FooterRequest;
import br.com.byop.aegis.pages.contract.NavLinkRequest;
import br.com.byop.aegis.pages.contract.NavbarRequest;
import br.com.byop.aegis.pages.contract.SocialLinkRequest;
import br.com.byop.aegis.pages.contract.UpdateProductGlobalsRequest;
import br.com.byop.aegis.pages.domain.ProductGlobals;
import br.com.byop.aegis.pages.dto.FooterResponse;
import br.com.byop.aegis.pages.dto.NavbarResponse;
import br.com.byop.aegis.pages.dto.ProductGlobalsResponse;
import br.com.byop.aegis.pages.mapper.ProductGlobalsMapper;
import br.com.byop.aegis.pages.repository.ProductGlobalsRepository;
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
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductGlobalsServiceTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final AuthenticatedUser CALLER = new AuthenticatedUser("user-1", "user@example.com", "user", "User", Set.of("ROLE_EDITOR"));

    @Mock
    private ProductGlobalsRepository globalsRepository;

    @Mock
    private ProductReferenceService productReferenceService;

    @Mock
    private AuditService auditService;

    private ProductGlobalsService service;

    @BeforeEach
    void setUp() {
        ProductGlobalsMapper mapper = Mappers.getMapper(ProductGlobalsMapper.class);
        service = new ProductGlobalsService(globalsRepository, mapper, productReferenceService, new ObjectMapper(), auditService);
    }

    @Test
    void shouldReturnEmptyResponseWhenGlobalsDoNotExistYet() {
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        ProductGlobalsResponse response = service.getGlobals(PRODUCT_ID);

        assertThat(response.navbar().links()).isEmpty();
        assertThat(response.footer().links()).isEmpty();
        assertThat(response.socialLinks()).isEmpty();
        assertThat(response.floatingWhatsapp()).isNull();
    }

    @Test
    void shouldReturnExistingGlobals() {
        ProductGlobals globals = new ProductGlobals(PRODUCT_ID,
                "{\"links\":[{\"label\":\"Home\",\"href\":\"/\"}]}",
                "{\"links\":[]}",
                "[{\"platform\":\"instagram\",\"href\":\"https://instagram.com/x\"}]",
                "{\"enabled\":true,\"number\":\"5511999999999\",\"message\":\"Oi\"}");
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(globals));

        ProductGlobalsResponse response = service.getGlobals(PRODUCT_ID);

        assertThat(response.navbar().links()).extracting("label").containsExactly("Home");
        assertThat(response.socialLinks()).extracting("platform").containsExactly("instagram");
        assertThat(response.floatingWhatsapp().number()).isEqualTo("5511999999999");
    }

    @Test
    void shouldReturnExistingGlobalsWithoutFloatingWhatsapp() {
        ProductGlobals globals = new ProductGlobals(PRODUCT_ID, "{\"links\":[]}", "{\"links\":[]}", "[]", null);
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(globals));

        ProductGlobalsResponse response = service.getGlobals(PRODUCT_ID);

        assertThat(response.floatingWhatsapp()).isNull();
    }

    @Test
    void shouldCreateGlobalsOnFirstUpdate() {
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));

        UpdateProductGlobalsRequest request = updateRequest();
        ProductGlobalsResponse response = service.updateGlobals(PRODUCT_ID, request, CALLER);

        assertThat(response.navbar().links()).extracting("label").containsExactly("Home");
        verify(globalsRepository).save(any(ProductGlobals.class));
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PRODUCT_GLOBALS_CREATED");
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void shouldUpdateExistingGlobals() {
        ProductGlobals existing = new ProductGlobals(PRODUCT_ID, "{}", "{}", "[]", null);
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));

        service.updateGlobals(PRODUCT_ID, updateRequest(), CALLER);

        assertThat(existing.getNavbarJson()).contains("Home");
        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("PRODUCT_GLOBALS_UPDATED");
    }

    @Test
    void shouldUpdateGlobalsWithoutFloatingWhatsapp() {
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        UpdateProductGlobalsRequest request = new UpdateProductGlobalsRequest(
                new NavbarRequest(null, List.of()), new FooterRequest(null, List.of()), List.of(), null
        );

        ProductGlobalsResponse response = service.updateGlobals(PRODUCT_ID, request, CALLER);

        assertThat(response.floatingWhatsapp()).isNull();
    }

    @Test
    void shouldWrapGlobalsSerializationFailure() {
        ObjectMapper brokenObjectMapper = mock(ObjectMapper.class);
        ProductGlobalsService brokenService = new ProductGlobalsService(globalsRepository,
                Mappers.getMapper(ProductGlobalsMapper.class), productReferenceService, brokenObjectMapper, auditService);
        when(brokenObjectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));
        UpdateProductGlobalsRequest request = updateRequest();

        assertThatThrownBy(() -> brokenService.updateGlobals(PRODUCT_ID, request, CALLER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");
    }

    @Test
    void shouldWrapGlobalsDeserializationFailureForObject() {
        ObjectMapper brokenObjectMapper = mock(ObjectMapper.class);
        ProductGlobalsService brokenService = new ProductGlobalsService(globalsRepository,
                Mappers.getMapper(ProductGlobalsMapper.class), productReferenceService, brokenObjectMapper, auditService);
        ProductGlobals globals = new ProductGlobals(PRODUCT_ID, "{\"links\":[]}", "{\"links\":[]}", "[]", null);
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(globals));
        when(brokenObjectMapper.readValue(anyString(), any(Class.class))).thenThrow(mock(JacksonException.class));

        assertThatThrownBy(() -> brokenService.getGlobals(PRODUCT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deserialize");
    }

    @Test
    void shouldWrapGlobalsDeserializationFailureForList() {
        ObjectMapper partiallyBrokenObjectMapper = spy(new ObjectMapper());
        ProductGlobalsService brokenService = new ProductGlobalsService(globalsRepository,
                Mappers.getMapper(ProductGlobalsMapper.class), productReferenceService, partiallyBrokenObjectMapper, auditService);
        when(globalsRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());
        doCallRealMethod().when(partiallyBrokenObjectMapper).readValue(anyString(), eq(NavbarResponse.class));
        doCallRealMethod().when(partiallyBrokenObjectMapper).readValue(anyString(), eq(FooterResponse.class));
        doThrow(mock(JacksonException.class)).when(partiallyBrokenObjectMapper).readValue(anyString(), any(TypeReference.class));

        assertThatThrownBy(() -> brokenService.getGlobals(PRODUCT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deserialize");
    }

    private UpdateProductGlobalsRequest updateRequest() {
        return new UpdateProductGlobalsRequest(
                new NavbarRequest(null, List.of(new NavLinkRequest("Home", "/"))),
                new FooterRequest(null, List.of()),
                List.of(new SocialLinkRequest("instagram", "https://instagram.com/x")),
                new FloatingWhatsappRequest(true, "5511999999999", "Oi")
        );
    }
}
