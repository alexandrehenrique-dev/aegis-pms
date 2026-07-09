package br.com.byop.aegis.form.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Normaliza os formatos aceitos por campos de upload em contratos de formulário.
 */
public final class FormAcceptedFileTypes {

    private static final String FIELD_ACCEPTED_FILE_TYPES = "acceptedFileTypes";
    private static final String FIELD_ACCEPTED_FORMATS = "acceptedFormats";
    private static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private FormAcceptedFileTypes() {
    }

    public static List<String> fromField(Map<String, Object> field) {
        Object value = field.containsKey(FIELD_ACCEPTED_FILE_TYPES)
                ? field.get(FIELD_ACCEPTED_FILE_TYPES)
                : field.get(FIELD_ACCEPTED_FORMATS);
        return fromValue(value);
    }

    public static List<String> fromValue(Object value) {
        if (value instanceof List<?> list && list.stream().allMatch(item -> item instanceof String _)) {
            return list.stream()
                    .map(String.class::cast)
                    .flatMap(FormAcceptedFileTypes::expandAcceptedFileType)
                    .toList();
        }
        return List.of();
    }

    private static Stream<String> expandAcceptedFileType(String value) {
        return switch (value) {
            case "PDF" -> Stream.of("application/pdf");
            case "Imagem" -> Stream.of("image/jpeg", "image/png", "image/webp", "image/gif");
            case "DOCX" -> Stream.of(MIME_DOCX);
            case "ZIP" -> Stream.of("application/zip", "application/x-zip-compressed");
            default -> Stream.of(value);
        };
    }
}
