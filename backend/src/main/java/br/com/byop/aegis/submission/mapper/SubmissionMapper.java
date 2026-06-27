package br.com.byop.aegis.submission.mapper;

import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.domain.SubmissionStatus;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.dto.SubmissionSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {

    String NO_OWNER_PLACEHOLDER = "—";
    String NO_SCORE_PLACEHOLDER = "—";

    @Mapping(target = "date", expression = "java(toContractDate(submission.getDate()))")
    @Mapping(target = "status", expression = "java(toContractStatus(submission.getStatus()))")
    @Mapping(target = "owner", expression = "java(toOwner(submission.getOwnerSubject()))")
    @Mapping(target = "score", expression = "java(toScore(submission.getScore()))")
    SubmissionSummary toSummary(Submission submission);

    @Mapping(target = "status", expression = "java(submission == null ? null : toContractStatus(submission.getStatus()))")
    @Mapping(target = "owner", expression = "java(submission == null ? null : toOwner(submission.getOwnerSubject()))")
    @Mapping(target = "answersJson", source = "answersJson")
    SubmissionDetail toDetail(Submission submission, Map<String, Object> answersJson);

    @Named("toContractDate")
    default String toContractDate(OffsetDateTime date) {
        return date == null ? null : date.toString();
    }

    @Named("toContractStatus")
    default String toContractStatus(SubmissionStatus status) {
        return status == null ? null : status.contractValue();
    }

    @Named("toOwner")
    default String toOwner(String ownerSubject) {
        return ownerSubject == null || ownerSubject.isBlank() ? NO_OWNER_PLACEHOLDER : ownerSubject;
    }

    @Named("toScore")
    default String toScore(Integer score) {
        return score == null ? NO_SCORE_PLACEHOLDER : score.toString();
    }
}
