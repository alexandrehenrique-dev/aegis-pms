package br.com.byop.aegis.submission.domain;

public enum SubmissionStatus {
    NEW("new"),
    REVIEWED("reviewed"),
    ARCHIVED("archived");

    private final String contractValue;

    SubmissionStatus(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }
}
