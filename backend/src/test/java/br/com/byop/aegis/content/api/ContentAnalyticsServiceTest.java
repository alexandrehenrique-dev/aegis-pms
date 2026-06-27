package br.com.byop.aegis.content.api;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.repository.ContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentAnalyticsServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-27T12:00:00Z");

    private ContentRepository contentRepository;
    private ContentAnalyticsService service;

    @BeforeEach
    void setUp() {
        contentRepository = mock(ContentRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T12:00:00Z"), ZoneOffset.UTC);
        service = new ContentAnalyticsService(contentRepository, clock);
    }

    @Test
    void shouldSummarizeEmptyContent() {
        when(contentRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of());

        ContentAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response).isEqualTo(new ContentAnalyticsResponse(0, 0, 0, 0, 0, null));
    }

    @Test
    void shouldSummarizeContentByStatusAndStaleReview() {
        Content published = content(ContentStatus.PUBLISHED, NOW.minusDays(1));
        Content pending = content(ContentStatus.IN_REVIEW, NOW.minusDays(31));
        Content recentPending = content(ContentStatus.IN_REVIEW, NOW.minusDays(1));
        Content draft = content(ContentStatus.DRAFT, NOW.minusDays(2));
        when(contentRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(published, pending, recentPending, draft));

        ContentAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response.total()).isEqualTo(4);
        assertThat(response.published()).isEqualTo(1);
        assertThat(response.pendingReview()).isEqualTo(2);
        assertThat(response.drafts()).isEqualTo(1);
        assertThat(response.stalePendingReview()).isEqualTo(1);
        assertThat(response.lastUpdatedAt()).isEqualTo(NOW.minusDays(1));
    }

    @Test
    void shouldIgnorePendingReviewWithoutUpdatedAtWhenCheckingStaleness() {
        Content pendingWithoutDate = content(ContentStatus.IN_REVIEW, null);
        when(contentRepository.findAllByProductId(PRODUCT_ID)).thenReturn(List.of(pendingWithoutDate));

        ContentAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response.pendingReview()).isEqualTo(1);
        assertThat(response.stalePendingReview()).isZero();
        assertThat(response.lastUpdatedAt()).isNull();
    }

    private Content content(ContentStatus status, OffsetDateTime updatedAt) {
        Content content = mock(Content.class);
        when(content.getStatus()).thenReturn(status);
        when(content.getUpdatedAt()).thenReturn(updatedAt);
        return content;
    }
}
