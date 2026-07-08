package br.com.byop.aegis.form.service;

import br.com.byop.aegis.form.exception.InvalidFormPublicationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormPublicationPolicyTest {

    private final FormPublicationPolicy policy = new FormPublicationPolicy();

    @Test
    void shouldAcceptPublishableFields() {
        policy.assertPublishable("Contato", List.of(
                Map.of("label", "Email", "type", "Email", "required", true),
                Map.of("label", "Curriculo", "type", "Upload", "required", false,
                        "acceptedFileTypes", List.of("application/pdf"))
        ));
    }

    @Test
    void shouldAcceptFieldsWithoutLabelsWhenOtherRequiredFieldExists() {
        policy.assertPublishable("Contato", List.of(
                Map.of("required", true, "type", "Texto"),
                Map.of("type", "Texto")
        ));
    }

    @Test
    void shouldRejectPublicationWithBlankName() {
        List<Map<String, Object>> fields = List.of(Map.of("label", "Email", "type", "Email", "required", true));

        assertThatThrownBy(() -> policy.assertPublishable("  ", fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.NAME_REQUIRED_ERROR);
    }

    @Test
    void shouldRejectPublicationWithNullName() {
        List<Map<String, Object>> fields = List.of(Map.of("label", "Email", "type", "Email", "required", true));

        assertThatThrownBy(() -> policy.assertPublishable(null, fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.NAME_REQUIRED_ERROR);
    }

    @Test
    void shouldRejectPublicationWithEmptyFields() {
        assertThatThrownBy(() -> policy.assertPublishable("Contato", List.of()))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.NO_FIELDS_ERROR);
    }

    @Test
    void shouldRejectPublicationWithNullFields() {
        assertThatThrownBy(() -> policy.assertPublishable("Contato", null))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.NO_FIELDS_ERROR);
    }

    @Test
    void shouldRejectPublicationWithoutRequiredField() {
        List<Map<String, Object>> fields = List.of(Map.of("label", "Email", "type", "Email"));

        assertThatThrownBy(() -> policy.assertPublishable("Contato", fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.REQUIRED_FIELD_ERROR);
    }

    @Test
    void shouldRejectDuplicateLabelsIgnoringCase() {
        List<Map<String, Object>> fields = List.of(
                Map.of("label", "Email", "type", "Email", "required", true),
                Map.of("label", " email ", "type", "Texto")
        );

        assertThatThrownBy(() -> policy.assertPublishable("Contato", fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.DUPLICATE_LABEL_ERROR);
    }

    @Test
    void shouldRejectUploadWithoutAcceptedFileTypes() {
        List<Map<String, Object>> fields = List.of(
                Map.of("label", "Nome", "type", "Texto", "required", true),
                Map.of("label", "Curriculo", "type", "Upload")
        );

        assertThatThrownBy(() -> policy.assertPublishable("Contato", fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.UPLOAD_ACCEPTED_TYPES_ERROR);
    }

    @Test
    void shouldRejectUploadWithNonStringAcceptedFileTypes() {
        List<Map<String, Object>> fields = List.of(
                Map.of("label", "Nome", "type", "Texto", "required", true),
                Map.of("label", "Curriculo", "type", "Upload", "acceptedFileTypes", List.of(123))
        );

        assertThatThrownBy(() -> policy.assertPublishable("Contato", fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.UPLOAD_ACCEPTED_TYPES_ERROR);
    }

    @Test
    void shouldRejectUploadWithAcceptedFileTypesOutsideList() {
        List<Map<String, Object>> fields = List.of(
                Map.of("label", "Nome", "type", "Texto", "required", true),
                Map.of("label", "Curriculo", "type", "Upload", "acceptedFileTypes", "application/pdf")
        );

        assertThatThrownBy(() -> policy.assertPublishable("Contato", fields))
                .isInstanceOf(InvalidFormPublicationException.class)
                .hasMessage(FormPublicationPolicy.UPLOAD_ACCEPTED_TYPES_ERROR);
    }
}
