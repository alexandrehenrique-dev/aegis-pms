package br.com.byop.aegis.form.api;

import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.exception.FormNotFoundException;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormReferenceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FORM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private FormDefinitionRepository formRepository;

    @Test
    void shouldReturnPublishedFormReference() {
        FormDefinition form = form();
        form.publish("2026-06-27/forms/contato");
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form));

        FormReference reference = new FormReferenceService(formRepository).getRequiredReference(PRODUCT_ID, FORM_ID);

        assertThat(reference.id()).isEqualTo(FORM_ID);
        assertThat(reference.tenantId()).isEqualTo(TENANT_ID);
        assertThat(reference.productId()).isEqualTo(PRODUCT_ID);
        assertThat(reference.published()).isTrue();
        assertThat(reference.fieldsJson()).contains("Email");
    }

    @Test
    void shouldReturnDraftFormReference() {
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.of(form()));

        FormReference reference = new FormReferenceService(formRepository).getRequiredReference(PRODUCT_ID, FORM_ID);

        assertThat(reference.published()).isFalse();
    }

    @Test
    void shouldRejectMissingFormReference() {
        FormReferenceService service = new FormReferenceService(formRepository);
        when(formRepository.findByProductIdAndId(PRODUCT_ID, FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredReference(PRODUCT_ID, FORM_ID))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    void shouldListFormIdsForProduct() {
        when(formRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(form()));

        assertThat(new FormReferenceService(formRepository).listFormIds(PRODUCT_ID)).containsExactly(FORM_ID);
    }

    private FormDefinition form() {
        FormDefinition form = new FormDefinition(new FormDefinition.Creation(
                TENANT_ID, PRODUCT_ID, "Contato", "lead",
                "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]", "[]"
        ));
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        return form;
    }
}
