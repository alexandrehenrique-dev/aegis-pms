package br.com.byop.aegis.asset.storage;

import java.time.OffsetDateTime;

/**
 * Resultado de {@link StorageProvider#resolve(java.util.UUID, String)} —
 * {@code expiresAt} e {@code null} para {@code local} (URL nao expira porque
 * e servida e autenticada pelo proprio backend) e preenchido para {@code s3}
 * (URL pre-assinada com TTL curto).
 */
public record ResolvedLocation(String url, OffsetDateTime expiresAt) {
}
