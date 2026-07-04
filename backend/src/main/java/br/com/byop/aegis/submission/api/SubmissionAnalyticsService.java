package br.com.byop.aegis.submission.api;

import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.repository.SubmissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class SubmissionAnalyticsService {

    private static final int RECENT_SUBMISSION_DAYS = 14;

    private final SubmissionRepository submissionRepository;
    private final FormReferenceService formReferenceService;
    private final Clock clock;

    public SubmissionAnalyticsService(SubmissionRepository submissionRepository, FormReferenceService formReferenceService,
                                      Clock clock) {
        this.submissionRepository = submissionRepository;
        this.formReferenceService = formReferenceService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SubmissionAnalyticsResponse summarize(UUID productId) {
        log.debug("summarize: productId='{}'", productId);
        List<UUID> formIds = formReferenceService.listFormIds(productId);
        if (formIds.isEmpty()) {
            return new SubmissionAnalyticsResponse(0, 0, null);
        }
        List<Submission> submissions = submissionRepository.findAllByFormIdInOrderByDateDesc(formIds);
        OffsetDateTime recentLimit = OffsetDateTime.now(clock).minusDays(RECENT_SUBMISSION_DAYS);
        long recent = submissions.stream()
                .filter(submission -> isOnOrAfter(submission.getDate(), recentLimit))
                .count();
        OffsetDateTime lastSubmittedAt = submissions.stream()
                .map(Submission::getDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new SubmissionAnalyticsResponse(submissions.size(), recent, lastSubmittedAt);
    }

    /**
     * Conta submissions recebidas no dia corrente (UTC), usado pelo
     * agregador do dashboard (etapa 17) — metrica adicional ao
     * {@link #summarize(UUID)}, que nao expoe um corte diario.
     *
     * @param productId produto cujos formularios devem ser somados
     * @return quantidade de submissions recebidas desde o inicio do dia corrente
     */
    @Transactional(readOnly = true)
    public long countToday(UUID productId) {
        log.debug("countToday: productId='{}'", productId);
        List<UUID> formIds = formReferenceService.listFormIds(productId);
        if (formIds.isEmpty()) {
            return 0;
        }
        OffsetDateTime startOfToday = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
        return submissionRepository.findAllByFormIdInOrderByDateDesc(formIds)
                .stream()
                .filter(submission -> isOnOrAfter(submission.getDate(), startOfToday))
                .count();
    }

    private boolean isOnOrAfter(OffsetDateTime value, OffsetDateTime limit) {
        return value != null && !value.isBefore(limit);
    }

}
