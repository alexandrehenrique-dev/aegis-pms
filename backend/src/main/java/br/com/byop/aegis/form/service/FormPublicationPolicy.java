package br.com.byop.aegis.form.service;

import br.com.byop.aegis.form.domain.FormFieldType;
import br.com.byop.aegis.form.exception.InvalidFormPublicationException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FormPublicationPolicy {

    public static final String REQUIRED_FIELD_ERROR = "FORM_REQUIRES_REQUIRED_FIELD";
    public static final String DUPLICATE_LABEL_ERROR = "FORM_DUPLICATE_FIELD_LABEL";
    public static final String UPLOAD_ACCEPTED_TYPES_ERROR = "FORM_UPLOAD_ACCEPTED_FILE_TYPES_REQUIRED";

    public void assertPublishable(List<Map<String, Object>> fields) {
        if (fields == null || fields.stream().noneMatch(this::isRequired)) {
            throw new InvalidFormPublicationException(REQUIRED_FIELD_ERROR);
        }
        assertLabelsAreUnique(fields);
        assertUploadFieldsHaveAcceptedFileTypes(fields);
    }

    private boolean isRequired(Map<String, Object> field) {
        return Boolean.TRUE.equals(field.get("required"));
    }

    private void assertLabelsAreUnique(List<Map<String, Object>> fields) {
        Set<String> labels = new HashSet<>();
        for (Map<String, Object> field : fields) {
            String label = stringValue(field.get("label"));
            if (!label.isBlank() && !labels.add(label.toLowerCase(Locale.ROOT))) {
                throw new InvalidFormPublicationException(DUPLICATE_LABEL_ERROR);
            }
        }
    }

    private void assertUploadFieldsHaveAcceptedFileTypes(List<Map<String, Object>> fields) {
        for (Map<String, Object> field : fields) {
            if (FormFieldType.isUpload(stringValue(field.get("type"))) && acceptedFileTypes(field).isEmpty()) {
                throw new InvalidFormPublicationException(UPLOAD_ACCEPTED_TYPES_ERROR);
            }
        }
    }

    private List<String> acceptedFileTypes(Map<String, Object> field) {
        Object value = field.get("acceptedFileTypes");
        if (value instanceof List<?> list && list.stream().allMatch(item -> item instanceof String _)) {
            return list.stream()
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
