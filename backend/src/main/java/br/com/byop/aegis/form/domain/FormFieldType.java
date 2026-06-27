package br.com.byop.aegis.form.domain;

import java.util.Arrays;
import java.util.List;

public enum FormFieldType {
    TEXT("Texto"),
    EMAIL("Email"),
    PHONE("Telefone"),
    UPLOAD("Upload"),
    LGPD_CONSENT("Consentimento LGPD"),
    TEXT_AREA("Texto longo"),
    SELECT("Selecao"),
    CHECKBOX("Checkbox"),
    RADIO("Radio");

    private final String contractValue;

    FormFieldType(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static List<String> contractValues() {
        return Arrays.stream(values())
                .map(FormFieldType::contractValue)
                .toList();
    }

    public static boolean isUpload(String value) {
        return UPLOAD.contractValue.equals(value);
    }
}
