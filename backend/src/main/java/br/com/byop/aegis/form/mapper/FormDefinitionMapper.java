package br.com.byop.aegis.form.mapper;

import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.domain.FormStatus;
import br.com.byop.aegis.form.dto.FormDetail;
import br.com.byop.aegis.form.dto.FormSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface FormDefinitionMapper {

    String NO_ACTIVITY_PLACEHOLDER = "Nenhuma resposta ainda";
    String NO_PUBLICATION_PLACEHOLDER = "—";

    @Mapping(target = "status", expression = "java(toContractStatus(form.getStatus()))")
    @Mapping(target = "responses", expression = "java(toResponses(form.getResponseCount()))")
    @Mapping(target = "conversion", constant = "—")
    @Mapping(target = "lastActivity", expression = "java(toLastActivity(form.getLastActivityAt()))")
    @Mapping(target = "publication", expression = "java(toPublication(form.getPublication()))")
    FormSummary toSummary(FormDefinition form);

    @Mapping(target = "status", expression = "java(form == null ? null : toContractStatus(form.getStatus()))")
    @Mapping(target = "fields", source = "fields")
    @Mapping(target = "deliveryChannels", source = "deliveryChannels")
    @Mapping(target = "publication", expression = "java(form == null ? null : toPublication(form.getPublication()))")
    FormDetail toDetail(FormDefinition form, List<Map<String, Object>> fields, List<Map<String, Object>> deliveryChannels);

    @Named("toContractStatus")
    default String toContractStatus(FormStatus status) {
        return status == null ? null : status.contractValue();
    }

    @Named("toResponses")
    default String toResponses(long responses) {
        return Long.toString(responses);
    }

    @Named("toLastActivity")
    default String toLastActivity(OffsetDateTime lastActivity) {
        return toLastActivity(lastActivity, OffsetDateTime.now(ZoneOffset.UTC));
    }

    default String toLastActivity(OffsetDateTime lastActivity, OffsetDateTime referenceTime) {
        if (lastActivity == null) {
            return NO_ACTIVITY_PLACEHOLDER;
        }
        Duration duration = Duration.between(lastActivity, referenceTime);
        long minutes = Math.max(0L, duration.toMinutes());
        if (minutes < 60L) {
            return "há " + minutes + " " + pluralize(minutes, "minuto", "minutos");
        }
        long hours = duration.toHours();
        if (hours < 24L) {
            return "há " + hours + " " + pluralize(hours, "hora", "horas");
        }
        long days = duration.toDays();
        return "há " + days + " " + pluralize(days, "dia", "dias");
    }

    @Named("toPublication")
    default String toPublication(String publication) {
        return publication == null ? NO_PUBLICATION_PLACEHOLDER : publication;
    }

    private String pluralize(long value, String singular, String plural) {
        return value == 1L ? singular : plural;
    }
}
