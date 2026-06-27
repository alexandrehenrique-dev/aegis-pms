package br.com.byop.aegis.analytics.dto;

public record TrendCardResponse(String type, String text, String metric, String severity, boolean reviewed) {
}
