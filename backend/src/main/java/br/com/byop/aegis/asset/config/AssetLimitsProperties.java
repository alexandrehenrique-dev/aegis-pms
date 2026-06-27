package br.com.byop.aegis.asset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Tabela de mime types aceitos e limite de tamanho por categoria de asset,
 * configuravel via {@code application.yml} ({@code aegis.assets.limits}) —
 * nunca hardcoded no codigo, conforme exigido pelo artefato da Sprint 12.
 */
@ConfigurationProperties(prefix = "aegis.assets")
public record AssetLimitsProperties(Map<String, CategoryLimit> limits) {

    public record CategoryLimit(List<String> mimeTypes, long maxSizeBytes) {
    }
}
