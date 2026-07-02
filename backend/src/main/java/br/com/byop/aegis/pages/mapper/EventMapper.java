package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.domain.Event;
import br.com.byop.aegis.pages.domain.EventAccessType;
import br.com.byop.aegis.pages.domain.EventVisibility;
import br.com.byop.aegis.pages.dto.EventDetail;
import br.com.byop.aegis.pages.dto.EventSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "type", expression = "java(toContractType(event.getType()))")
    @Mapping(target = "visibility", expression = "java(toContractVisibility(event.getVisibility()))")
    EventSummary toSummary(Event event);

    @Mapping(target = "type", expression = "java(toContractType(event.getType()))")
    @Mapping(target = "visibility", expression = "java(toContractVisibility(event.getVisibility()))")
    EventDetail toDetail(Event event);

    @Named("toContractType")
    default String toContractType(EventAccessType type) {
        return type == null ? null : type.contractValue();
    }

    @Named("toContractVisibility")
    default String toContractVisibility(EventVisibility visibility) {
        return visibility == null ? null : visibility.contractValue();
    }
}
