package br.com.byop.aegis.pages.domain;

import java.util.List;

/**
 * Uma pagina do esqueleto gerado no scaffold de produto (Etapa 26, ADR-0017):
 * {@code slug}/{@code title} da {@link Page}, e a lista ordenada de
 * {@link SectionSkeletonTemplate} a criar.
 */
public record PageSkeletonTemplate(String slug, String title, List<SectionSkeletonTemplate> sections) {
}
