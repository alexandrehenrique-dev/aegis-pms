package br.com.byop.aegis.feedback.mapper;

import br.com.byop.aegis.feedback.domain.Feedback;
import br.com.byop.aegis.feedback.domain.FeedbackCategory;
import br.com.byop.aegis.feedback.domain.FeedbackPriority;
import br.com.byop.aegis.feedback.domain.FeedbackStatus;
import br.com.byop.aegis.feedback.dto.FeedbackSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "category", expression = "java(toContractCategory(feedback.getCategory()))")
    @Mapping(target = "priority", expression = "java(toContractPriority(feedback.getPriority()))")
    @Mapping(target = "status", expression = "java(toContractStatus(feedback.getStatus()))")
    FeedbackSummary toSummary(Feedback feedback);

    @Named("toContractCategory")
    default String toContractCategory(FeedbackCategory category) {
        return category == null ? null : category.contractValue();
    }

    @Named("toContractPriority")
    default String toContractPriority(FeedbackPriority priority) {
        return priority == null ? null : priority.contractValue();
    }

    @Named("toContractStatus")
    default String toContractStatus(FeedbackStatus status) {
        return status == null ? null : status.contractValue();
    }
}
