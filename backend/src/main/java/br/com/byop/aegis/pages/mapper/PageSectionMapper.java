package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface PageSectionMapper {

    @Mapping(target = "type", expression = "java(section == null ? null : toContractType(section.getType()))")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "settings", source = "settings")
    PageSectionResponse toResponse(PageSection section, Map<String, Object> content, Map<String, Object> settings);

    @Named("toContractType")
    default String toContractType(BlockType type) {
        return type == null ? null : type.contractValue();
    }
}
