package br.com.byop.aegis.product.export.controller;

import br.com.byop.aegis.product.export.domain.ExportToken;
import br.com.byop.aegis.product.export.domain.ExportTokenStatus;
import br.com.byop.aegis.product.export.exception.ExportDownloadNotFoundException;
import br.com.byop.aegis.product.export.exception.ExportLinkExpiredException;
import br.com.byop.aegis.product.export.repository.ExportTokenRepository;
import br.com.byop.aegis.product.export.service.ExportStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@RestController
public class ExportController {

    private static final int BUFFER_SIZE = 8192;

    private final ExportTokenRepository exportTokenRepository;
    private final ExportStorageService exportStorageService;
    private final Clock clock;

    @Autowired
    public ExportController(ExportTokenRepository exportTokenRepository, ExportStorageService exportStorageService) {
        this(exportTokenRepository, exportStorageService, Clock.systemUTC());
    }

    ExportController(ExportTokenRepository exportTokenRepository, ExportStorageService exportStorageService, Clock clock) {
        this.exportTokenRepository = exportTokenRepository;
        this.exportStorageService = exportStorageService;
        this.clock = clock;
    }

    @GetMapping("/api/v1/exports/{tokenId}/download")
    @Transactional
    public ResponseEntity<StreamingResponseBody> download(@PathVariable("tokenId") UUID tokenId) {
        ExportToken token = exportTokenRepository.findById(tokenId)
                .orElseThrow(ExportDownloadNotFoundException::new);
        Instant now = clock.instant();
        if (!token.getExpiresAt().isAfter(now)) {
            throw new ExportLinkExpiredException();
        }
        if (token.getStatus() != ExportTokenStatus.AVAILABLE) {
            throw new ExportDownloadNotFoundException();
        }
        token.markDownloaded(now);
        exportTokenRepository.save(token);
        StreamingResponseBody body = outputStream -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream inputStream = exportStorageService.openStream(token)) {
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    outputStream.write(buffer, 0, read);
                }
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(exportStorageService.filename(token.getZipPath())))
                .body(body);
    }
}
