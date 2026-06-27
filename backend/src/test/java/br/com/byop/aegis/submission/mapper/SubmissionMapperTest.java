package br.com.byop.aegis.submission.mapper;

import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.domain.SubmissionStatus;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.dto.SubmissionSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionMapperTest {

    private static final OffsetDateTime DATE = OffsetDateTime.parse("2026-06-27T10:00:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-27T10:01:00Z");

    private final SubmissionMapper mapper = Mappers.getMapper(SubmissionMapper.class);

    @Test
    void shouldMapSubmissionToSummary() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Submission submission = submission(id, "owner-1", 90);

        SubmissionSummary summary = mapper.toSummary(submission);

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.date()).isEqualTo(DATE.toString());
        assertThat(summary.name()).isEqualTo("Ana");
        assertThat(summary.email()).isEqualTo("ana@example.com");
        assertThat(summary.source()).isEqualTo("site");
        assertThat(summary.status()).isEqualTo("new");
        assertThat(summary.owner()).isEqualTo("owner-1");
        assertThat(summary.score()).isEqualTo("90");
    }

    @Test
    void shouldUsePlaceholdersForOwnerAndScore() {
        SubmissionSummary summary = mapper.toSummary(submission(UUID.randomUUID(), null, null));

        assertThat(summary.owner()).isEqualTo("—");
        assertThat(summary.score()).isEqualTo("—");
    }

    @Test
    void shouldMapSubmissionToDetailWithFullAnswersJson() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Submission submission = submission(id, "owner-1", 80);
        Map<String, Object> answers = Map.of("Email", "ana@example.com", "Curriculo", "33333333-3333-3333-3333-333333333333");

        SubmissionDetail detail = mapper.toDetail(submission, answers);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.formId()).isEqualTo(submission.getFormId());
        assertThat(detail.date()).isEqualTo(DATE);
        assertThat(detail.status()).isEqualTo("new");
        assertThat(detail.owner()).isEqualTo("owner-1");
        assertThat(detail.score()).isEqualTo(80);
        assertThat(detail.answersJson()).isEqualTo(answers);
        assertThat(detail.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldReturnNullWhenSourcesAreNull() {
        assertThat(mapper.toSummary(null)).isNull();
        assertThat(mapper.toDetail(null, null)).isNull();
    }

    @Test
    void shouldMapOnlyAnswersWhenSubmissionIsNull() {
        Map<String, Object> answers = Map.of("Email", "ana@example.com");

        SubmissionDetail detail = mapper.toDetail(null, answers);

        assertThat(detail.answersJson()).isEqualTo(answers);
        assertThat(detail.name()).isNull();
        assertThat(detail.status()).isNull();
    }

    @Test
    void shouldMapDetailWithoutAnswers() {
        Submission submission = submission(UUID.randomUUID(), null, null);

        SubmissionDetail detail = mapper.toDetail(submission, null);

        assertThat(detail.id()).isEqualTo(submission.getId());
        assertThat(detail.answersJson()).isNull();
        assertThat(detail.owner()).isEqualTo("—");
    }

    @Test
    void shouldExposeNullContractValues() {
        assertThat(mapper.toContractDate(null)).isNull();
        assertThat(mapper.toContractStatus(null)).isNull();
        assertThat(mapper.toOwner(" ")).isEqualTo("—");
    }

    private Submission submission(UUID id, String ownerSubject, Integer score) {
        Submission submission = new Submission(new Submission.Creation(
                UUID.randomUUID(), DATE, "Ana", "ana@example.com", "site",
                SubmissionStatus.NEW, ownerSubject, score, "{\"Email\":\"ana@example.com\"}"
        ));
        ReflectionTestUtils.setField(submission, "id", id);
        ReflectionTestUtils.setField(submission, "createdAt", CREATED_AT);
        return submission;
    }
}
