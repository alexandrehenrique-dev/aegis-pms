package br.com.byop.aegis.content.mapper;

import br.com.byop.aegis.content.domain.ContentVersion;
import br.com.byop.aegis.content.dto.ContentVersionSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContentVersionMapper {

    @Mapping(target = "createdByName", source = "createdByName")
    ContentVersionSummary toSummary(ContentVersion version, String createdByName);
}
