package br.com.byop.aegis.analytics.dto;

public record AnalyticsReportResponse(
        String name,
        String description,
        String period,
        String format,
        String status,
        String lastGenerated
) {
}
