package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class ExportCleanupJob {

    private final ExportTokenRepository exportTokenRepository;
    private final ExportStorageService exportStorageService;
    private final Clock clock;

    @Autowired
    public ExportCleanupJob(ExportTokenRepository exportTokenRepository, ExportStorageService exportStorageService) {
        this(exportTokenRepository, exportStorageService, Clock.systemUTC());
    }

    ExportCleanupJob(ExportTokenRepository exportTokenRepository, ExportStorageService exportStorageService, Clock clock) {
        this.exportTokenRepository = exportTokenRepository;
        this.exportStorageService = exportStorageService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireExports() {
        Instant now = clock.instant();
        List<ExportToken> expiredTokens = exportTokenRepository.findExpired(now);
        expiredTokens.forEach(this::expireToken);
    }

    private void expireToken(ExportToken token) {
        exportStorageService.delete(token);
        token.markExpired();
        exportTokenRepository.save(token);
    }
}
