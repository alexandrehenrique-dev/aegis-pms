package br.com.byop.aegis.content.api;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContentAnalyticsService {

    private static final int STALE_REVIEW_DAYS = 30;

    private final ContentRepository contentRepository;
    private final Clock clock;

    public ContentAnalyticsService(ContentRepository contentRepository, Clock clock) {
        this.contentRepository = contentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ContentAnalyticsResponse summarize(UUID productId) {
        List<Content> contents = contentRepository.findAllByProductId(productId);
        OffsetDateTime staleLimit = OffsetDateTime.now(clock).minusDays(STALE_REVIEW_DAYS);
        long published = countByStatus(contents, ContentStatus.PUBLISHED);
        long pendingReview = countByStatus(contents, ContentStatus.IN_REVIEW);
        long drafts = countByStatus(contents, ContentStatus.DRAFT);
        long stalePendingReview = contents.stream()
                .filter(content -> content.getStatus() == ContentStatus.IN_REVIEW)
                .filter(content -> isBefore(content.getUpdatedAt(), staleLimit))
                .count();
        OffsetDateTime lastUpdatedAt = contents.stream()
                .map(Content::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new ContentAnalyticsResponse(contents.size(), published, pendingReview, drafts, stalePendingReview, lastUpdatedAt);
    }

    private long countByStatus(List<Content> contents, ContentStatus status) {
        return contents.stream()
                .filter(content -> content.getStatus() == status)
                .count();
    }

    private boolean isBefore(OffsetDateTime value, OffsetDateTime limit) {
        return value != null && value.isBefore(limit);
    }
}
