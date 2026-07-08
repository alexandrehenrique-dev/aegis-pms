package br.com.byop.aegis.analytics.dto;

public record HealthSignalResponse(String label, String status, String score, String tone,
                                   String detail, String actionLabel, String actionTarget) {
}
