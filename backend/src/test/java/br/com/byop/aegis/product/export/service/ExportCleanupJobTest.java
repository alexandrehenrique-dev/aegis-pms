package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.domain.ExportTokenStatus;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-07-03T03:00:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final ExportTokenRepository exportTokenRepository = mock(ExportTokenRepository.class);
    private final ExportStorageService exportStorageService = mock(ExportStorageService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldExpireAndDeleteExpiredTokens() {
        ExportToken token = new ExportToken(PRODUCT_ID, "maestro-beton", "exports/token/export.zip", "local", "owner@byop.dev");
        when(exportTokenRepository.findExpired(NOW)).thenReturn(List.of(token));

        new ExportCleanupJob(exportTokenRepository, exportStorageService, clock).expireExports();

        assertThat(token.getStatus()).isEqualTo(ExportTokenStatus.EXPIRED);
        verify(exportStorageService).delete(token);
        verify(exportTokenRepository).save(token);
    }
}
