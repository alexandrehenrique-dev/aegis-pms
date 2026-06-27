package br.com.byop.aegis.form.api;

import br.com.byop.aegis.form.domain.FormFieldType;

public final class FormFieldCatalog {

    private FormFieldCatalog() {
    }

    public static boolean isUpload(String value) {
        return FormFieldType.isUpload(value);
    }
}
