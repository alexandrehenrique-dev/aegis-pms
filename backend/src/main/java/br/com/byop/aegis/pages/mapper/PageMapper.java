package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageStatus;
import br.com.byop.aegis.pages.dto.PageDetail;
import br.com.byop.aegis.pages.dto.PageSeoResponse;
import br.com.byop.aegis.pages.dto.PageSectionResponse;
import br.com.byop.aegis.pages.dto.PageSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PageMapper {

    @Mapping(target = "status", expression = "java(toContractStatus(page.getStatus()))")
    PageSummary toSummary(Page page);

    @Mapping(target = "status", expression = "java(page == null ? null : toContractStatus(page.getStatus()))")
    @Mapping(target = "seo", expression = "java(page == null ? null : toSeo(page))")
    @Mapping(target = "sections", source = "sections")
    PageDetail toDetail(Page page, List<PageSectionResponse> sections);

    @Named("toContractStatus")
    default String toContractStatus(PageStatus status) {
        return status == null ? null : status.contractValue();
    }

    default PageSeoResponse toSeo(Page page) {
        return new PageSeoResponse(page.getSeoTitle(), page.getSeoDescription(), page.getSeoCanonical(),
                page.getSeoOgImageAssetId(), page.isSeoNoIndex());
    }
}
