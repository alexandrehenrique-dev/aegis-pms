package br.com.byop.aegis.product.export.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.domain.ExportTokenStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTokenRepositoryTest extends RepositoryTestSupport {

    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private ExportTokenRepository exportTokenRepository;

    @Test
    void shouldFindAvailableTokenBeforeExpiration() {
        ExportToken token = token("exports/token/export.zip");
        token.markAvailable(token.getZipPath());
        ExportToken saved = exportTokenRepository.saveAndFlush(token);

        assertThat(exportTokenRepository.findByIdAndStatusAndExpiresAtAfter(
                saved.getId(),
                ExportTokenStatus.AVAILABLE,
                saved.getCreatedAt()
        )).contains(saved);
    }

    @Test
    void shouldFindExpiredTokensNotYetMarkedExpired() {
        ExportToken token = token("exports/token/export.zip");
        ExportToken saved = exportTokenRepository.saveAndFlush(token);

        assertThat(exportTokenRepository.findExpired(saved.getExpiresAt().plusSeconds(1)))
                .contains(saved);

        saved.markExpired();
        exportTokenRepository.saveAndFlush(saved);

        assertThat(exportTokenRepository.findExpired(saved.getExpiresAt().plusSeconds(1)))
                .doesNotContain(saved);
    }

    private ExportToken token(String zipPath) {
        return new ExportToken(PRODUCT_ID, "maestro-beton", zipPath, "local", "owner@byop.dev");
    }
}
