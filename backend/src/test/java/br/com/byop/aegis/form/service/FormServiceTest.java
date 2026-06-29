package br.com.byop.aegis.form.service;

import br.com.byop.aegis.form.contract.CreateFormDefinitionRequest;
import br.com.byop.aegis.form.contract.UpdateFormDefinitionRequest;
import br.com.byop.aegis.form.contract.UpdateFormDeliveryRequest;
import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.dto.FormDetail;
import br.com.byop.aegis.form.dto.FormSummary;
import br.com.byop.aegis.form.exception.FormNotFoundException;
import br.com.byop.aegis.form.mapper.FormDefinitionMapper;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FORM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private FormDefinitionRepository formRepository;

    @Mock
    private FormDefinitionMapper formMapper;

    @Mock
    private ProductReferenceService productReferenceService;

    private FormService service;

    @BeforeEach
    void setUp() {
        service = new FormService(formRepository, formMapper, new FormPublicationPolicy(),
                new FormDeliveryPolicy(), productReferenceService, new ObjectMapper());
    }

    @Test
    void shouldCreateForm() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        when(formRepository.save(any(FormDefinition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FormDetail detail = mockDetail();
        when(formMapper.toDetail(any(), any(), eq(List.of()))).thenReturn(detail);
        CreateFormDefinitionRequest request = new CreateFormDefinitionRequest("Contato", "lead", requiredFields());

        FormDetail result = service.createForm(PRODUCT_ID, request);

        assertThat(result).isEqualTo(detail);
        ArgumentCaptor<FormDefinition> captor = ArgumentCaptor.forClass(FormDefinition.class);
        verify(formRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(captor.getValue().getFieldsJson()).contains("Email");
    }

    @Test
    void shouldListFormsWithResponseCountAndLastActivity() {
        FormDefinition form = form();
        FormSummary summary = new FormSummary(FORM_ID, "Contato", "lead", "Draft", "2", "—", "x", "—");
        when(formRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(form));
        when(formMapper.toSummary(form)).thenReturn(summary);

        assertThat(service.listForms(PRODUCT_ID)).containsExactly(summary);
    }

    @Test
    void shouldGetFormScopedByProduct() {
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(eq(form), any(), any())).thenReturn(detail);

        assertThat(service.getForm(PRODUCT_ID, FORM_ID)).isEqualTo(detail);
    }

    @Test
    void shouldRejectGetWhenFormDoesNotBelongToProduct() {
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForm(PRODUCT_ID, FORM_ID))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    void shouldUpdateFormDefinition() {
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(eq(form), any(), any())).thenReturn(detail);

        FormDetail result = service.updateForm(PRODUCT_ID, FORM_ID,
                new UpdateFormDefinitionRequest("Candidatura", "application", requiredFields()));

        assertThat(result).isEqualTo(detail);
        assertThat(form.getName()).isEqualTo("Candidatura");
        verify(formRepository).save(form);
    }

    @Test
    void shouldListFieldTypes() {
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));

        assertThat(service.listFieldTypes(PRODUCT_ID)).contains("Texto", "Email", "Upload", "Consentimento LGPD");
    }

    @Test
    void shouldPublishValidForm() {
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(eq(form), any(), any())).thenReturn(detail);

        FormDetail result = service.publish(PRODUCT_ID, FORM_ID);

        assertThat(result).isEqualTo(detail);
        assertThat(form.getStatus().contractValue()).isEqualTo("Published");
        assertThat(form.getPublication()).isNotBlank();
        verify(formRepository).save(form);
    }

    @Test
    void shouldUpdateDeliveryChannels() {
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(eq(form), any(), any())).thenReturn(detail);
        UpdateFormDeliveryRequest request = new UpdateFormDeliveryRequest(List.of(
                Map.of("type", "email", "enabled", true, "config", Map.of("to", "ops@byop.com")),
                Map.of("type", "webhook", "enabled", true, "config", Map.of("url", "https://example.com/hook"))
        ));

        assertThat(service.updateDelivery(PRODUCT_ID, FORM_ID, request)).isEqualTo(detail);
        assertThat(form.getDeliveryChannelsJson()).contains("ops@byop.com", "example.com");
    }

    @Test
    void shouldReturnEmptyListsWhenStoredJsonIsBlank() {
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        org.springframework.test.util.ReflectionTestUtils.setField(form, "fieldsJson", " ");
        org.springframework.test.util.ReflectionTestUtils.setField(form, "deliveryChannelsJson", null);
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(form, List.of(), List.of())).thenReturn(detail);

        assertThat(service.getForm(PRODUCT_ID, FORM_ID)).isEqualTo(detail);
    }

    @Test
    void shouldReturnEmptyListsWhenStoredJsonDoesNotDeserializeAsList() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doReturn("not-a-list").when(objectMapper).readValue(any(String.class), eq(List.class));
        FormService defensiveService = new FormService(formRepository, formMapper, new FormPublicationPolicy(),
                new FormDeliveryPolicy(), productReferenceService, objectMapper);
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(form, List.of(), List.of())).thenReturn(detail);

        assertThat(defensiveService.getForm(PRODUCT_ID, FORM_ID)).isEqualTo(detail);
    }

    @Test
    void shouldReturnEmptyListsWhenStoredJsonListDoesNotContainMaps() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doReturn(List.of("not-a-map")).when(objectMapper).readValue(any(String.class), eq(List.class));
        FormService defensiveService = new FormService(formRepository, formMapper, new FormPublicationPolicy(),
                new FormDeliveryPolicy(), productReferenceService, objectMapper);
        FormDefinition form = form();
        FormDetail detail = mockDetail();
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));
        when(formMapper.toDetail(form, List.of(), List.of())).thenReturn(detail);

        assertThat(defensiveService.getForm(PRODUCT_ID, FORM_ID)).isEqualTo(detail);
    }

    @Test
    void shouldFailWhenFormJsonCannotBeSerialized() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));
        FormService brokenService = new FormService(formRepository, formMapper, new FormPublicationPolicy(),
                new FormDeliveryPolicy(), productReferenceService, objectMapper);
        when(productReferenceService.getRequiredReference(PRODUCT_ID)).thenReturn(new ProductReference(PRODUCT_ID, TENANT_ID));
        CreateFormDefinitionRequest request = new CreateFormDefinitionRequest("Contato", "lead", requiredFields());

        assertThatThrownBy(() -> brokenService.createForm(PRODUCT_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to serialize form JSON");
    }

    @Test
    void shouldFailWhenStoredFormJsonCannotBeRead() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(String.class), eq(List.class))).thenThrow(mock(JacksonException.class));
        FormService brokenService = new FormService(formRepository, formMapper, new FormPublicationPolicy(),
                new FormDeliveryPolicy(), productReferenceService, objectMapper);
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form()));

        assertThatThrownBy(() -> brokenService.getForm(PRODUCT_ID, FORM_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to deserialize form JSON");
    }

    private FormDefinition form() {
        FormDefinition form = new FormDefinition(new FormDefinition.Creation(
                TENANT_ID, PRODUCT_ID, "Contato", "lead",
                "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]", "[]"
        ));
        org.springframework.test.util.ReflectionTestUtils.setField(form, "id", FORM_ID);
        return form;
    }

    private List<Map<String, Object>> requiredFields() {
        return List.of(Map.of("label", "Email", "type", "Email", "required", true));
    }

    private FormDetail mockDetail() {
        return new FormDetail(FORM_ID, "Contato", "lead", "Draft", requiredFields(), List.of(), "—", null, null);
    }
}
