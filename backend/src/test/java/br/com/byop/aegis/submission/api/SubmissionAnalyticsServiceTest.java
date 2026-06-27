package br.com.byop.aegis.submission.api;

import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.repository.SubmissionRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmissionAnalyticsServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FORM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-27T12:00:00Z");

    private SubmissionRepository submissionRepository;
    private FormReferenceService formReferenceService;
    private SubmissionAnalyticsService service;

    @BeforeEach
    void setUp() {
        submissionRepository = mock(SubmissionRepository.class);
        formReferenceService = mock(FormReferenceService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T12:00:00Z"), ZoneOffset.UTC);
        service = new SubmissionAnalyticsService(submissionRepository, formReferenceService, clock);
    }

    @Test
    void shouldReturnEmptyWhenProductHasNoForms() {
        when(formReferenceService.listFormIds(PRODUCT_ID)).thenReturn(List.of());

        SubmissionAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response).isEqualTo(new SubmissionAnalyticsResponse(0, 0, null));
        verifyNoInteractions(submissionRepository);
    }

    @Test
    void shouldSummarizeSubmissionsForProductForms() {
        Submission recent = submission(NOW.minusDays(1));
        Submission old = submission(NOW.minusDays(20));
        Submission unknownDate = submission(null);
        when(formReferenceService.listFormIds(PRODUCT_ID)).thenReturn(List.of(FORM_ID));
        when(submissionRepository.findAllByFormIdInOrderByDateDesc(List.of(FORM_ID)))
                .thenReturn(List.of(recent, old, unknownDate));

        SubmissionAnalyticsResponse response = service.summarize(PRODUCT_ID);

        assertThat(response.total()).isEqualTo(3);
        assertThat(response.recent()).isEqualTo(1);
        assertThat(response.lastSubmittedAt()).isEqualTo(NOW.minusDays(1));
    }

    private Submission submission(OffsetDateTime date) {
        Submission submission = mock(Submission.class);
        when(submission.getDate()).thenReturn(date);
        return submission;
    }
}
