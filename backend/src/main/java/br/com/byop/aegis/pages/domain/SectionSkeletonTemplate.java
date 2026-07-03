package br.com.byop.aegis.pages.domain;

/**
 * Uma secao do esqueleto de pagina gerado no scaffold de produto (Etapa 26,
 * ADR-0017): {@code type} e o {@link BlockType} da secao, {@code variant} e o
 * rotulo/variante opcional (ex.: distinguir dois {@code card-list} na mesma
 * pagina, "Vagas" vs "Blog"), {@code order} e a posicao na pagina.
 */
public record SectionSkeletonTemplate(BlockType type, String variant, int order) {

    public SectionSkeletonTemplate(BlockType type, int order) {
        this(type, null, order);
    }
}
