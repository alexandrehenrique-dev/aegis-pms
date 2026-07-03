package br.com.byop.aegis.product.export.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTokenTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldInitializeExpirationAndStatus() {
        ExportToken token = token();

        token.prePersist();

        assertThat(token.getId()).isNotNull();
        assertThat(token.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(token.getProductKey()).isEqualTo("maestro-beton");
        assertThat(token.getZipPath()).isEqualTo("exports/token/export.zip");
        assertThat(token.getStorageProvider()).isEqualTo("local");
        assertThat(token.getRecipientEmail()).isEqualTo("owner@byop.dev");
        assertThat(token.getStatus()).isEqualTo(ExportTokenStatus.PENDING);
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getExpiresAt()).isEqualTo(token.getCreatedAt().plusSeconds(604800));
        assertThat(token.getDownloadedAt()).isNull();
    }

    @Test
    void shouldPreserveExistingExpirationOnPersist() {
        ExportToken token = token();
        Instant createdAt = Instant.parse("2026-07-03T10:00:00Z");
        Instant expiresAt = Instant.parse("2026-07-04T10:00:00Z");
        org.springframework.test.util.ReflectionTestUtils.setField(token, "createdAt", createdAt);
        org.springframework.test.util.ReflectionTestUtils.setField(token, "expiresAt", expiresAt);

        token.prePersist();

        assertThat(token.getCreatedAt()).isEqualTo(createdAt);
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void shouldMarkAvailableExpiredAndDownloadedOnce() {
        ExportToken token = token();
        Instant firstDownload = Instant.parse("2026-07-03T10:00:00Z");
        Instant secondDownload = Instant.parse("2026-07-03T11:00:00Z");

        token.markAvailable("exports/token/final.zip");
        token.markDownloaded(firstDownload);
        token.markDownloaded(secondDownload);
        token.markExpired();

        assertThat(token.getZipPath()).isEqualTo("exports/token/final.zip");
        assertThat(token.getDownloadedAt()).isEqualTo(firstDownload);
        assertThat(token.getStatus()).isEqualTo(ExportTokenStatus.EXPIRED);
    }

    private ExportToken token() {
        return new ExportToken(PRODUCT_ID, "maestro-beton", "exports/token/export.zip", "local", "owner@byop.dev");
    }
}
