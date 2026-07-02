package br.com.byop.aegis.pages.dto;

import java.util.List;
import java.util.UUID;

public record PageDetail(
        UUID id,
        String slug,
        String title,
        String locale,
        String status,
        int version,
        PageSeoResponse seo,
        List<PageSectionResponse> sections
) {
}
