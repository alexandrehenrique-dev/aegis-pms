package br.com.byop.aegis.analytics.dto;

public record AnalyticsKpiResponse(String label, String value, String comparison, String note, String tone) {
}
