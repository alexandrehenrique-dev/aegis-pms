package br.com.byop.aegis.audit.exception;

public class InvalidAuditRiskFilterException extends RuntimeException {

    public InvalidAuditRiskFilterException(String risk) {
        super("Invalid audit risk filter: " + risk);
    }
}
