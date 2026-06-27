package br.com.byop.aegis.form.domain;

public enum FormStatus {
    DRAFT("Draft"),
    PUBLISHED("Published");

    private final String contractValue;

    FormStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }
}
