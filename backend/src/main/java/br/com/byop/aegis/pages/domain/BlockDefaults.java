package br.com.byop.aegis.pages.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fonte unica de conteudo default (vazio ou minimo estrutural) por {@link BlockType},
 * inspirada em {@code frontend/src/domains/pages/blockDefaults.ts}
 * ({@code DEFAULT_BLOCK_CONTENT}) — reaproveitada tanto pelo scaffold de
 * produto (Etapa 26, ADR-0017) quanto por qualquer fluxo futuro de
 * "adicionar bloco novo" no editor de paginas, para nunca existir duas listas
 * de default divergentes entre frontend e backend.
 *
 * <p>Para {@link BlockType} sem nenhuma regra em
 * {@link br.com.byop.aegis.pages.service.SectionContentValidationService}
 * (ex.: {@code text}, {@code two-column}, {@code timeline}) o default e 100%
 * vazio. Para os tipos com regra minima (ex.: {@code hero} exige titulo
 * nao-branco; {@code gallery}/{@code card-list}/{@code feature-grid} exigem
 * pelo menos 1 item), o default usa o minimo estrutural que passa na
 * validacao sem referenciar nenhum asset/form/evento/link real — nunca um
 * item com dado de negocio inventado. {@code contact}/{@code form}
 * (referencia obrigatoria a um Form real), {@code download} (lista nunca
 * vazia) e {@code audio}/{@code video}/{@code video-gallery} (asset ou URL
 * externa obrigatorios) exigiam relaxar essa validacao para aceitar o estado
 * "ainda nao configurado" — decisao registrada em {@code SPRINT-RESULTADO.md}
 * (Etapa 26) e estendida a audio/video no bug fix consolidado de paginas
 * (E.9.2, {@code SectionContentValidationService#requireAssetOrBlank}).
 */
public final class BlockDefaults {

    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_YOUTUBE_URL = "youtubeUrl";
    private static final Map<BlockType, Map<String, Object>> DEFAULTS = buildDefaults();

    private BlockDefaults() {
    }

    /**
     * Conteudo default (imutavel) para {@code type}.
     *
     * @param type tipo de bloco
     * @return mapa de conteudo default, nunca {@code null}
     */
    public static Map<String, Object> defaultFor(BlockType type) {
        return DEFAULTS.getOrDefault(type, Map.of());
    }

    private static Map<BlockType, Map<String, Object>> buildDefaults() {
        Map<BlockType, Map<String, Object>> defaults = new EnumMap<>(BlockType.class);
        // Sem regra de validacao propria — pode ser 100% vazio.
        defaults.put(BlockType.TEXT, Map.of(KEY_TITLE, "", KEY_BODY, ""));
        defaults.put(BlockType.RICH_TEXT, Map.of(KEY_TITLE, "", KEY_BODY, ""));
        defaults.put(BlockType.TWO_COLUMN, Map.of());
        defaults.put(BlockType.IMAGE_TEXT, Map.of(KEY_TITLE, "", KEY_BODY, ""));
        defaults.put(BlockType.TIMELINE, Map.of(KEY_ITEMS, List.of()));
        defaults.put(BlockType.CTA_SECTION, Map.of(KEY_TITLE, ""));
        defaults.put(BlockType.FAQ, Map.of(KEY_ITEMS, List.of()));
        // Com regra minima — usa o minimo estrutural que passa na validacao existente.
        defaults.put(BlockType.HERO, Map.of(KEY_TITLE, "Novo título", "image", Map.of("src", "", "alt", "Descrição da imagem"),
                "ctas", List.of(Map.of("label", "Saiba mais", "href", "#"))));
        defaults.put(BlockType.IMAGE, Map.of("alt", "Imagem", "src", ""));
        defaults.put(BlockType.GALLERY, Map.of(KEY_ITEMS, List.of(Map.of("src", "", "alt", "Imagem"))));
        defaults.put(BlockType.FEATURE_GRID, Map.of(KEY_ITEMS, List.of(Map.of())));
        defaults.put(BlockType.CARD_LIST, Map.of(KEY_ITEMS, List.of(Map.of())));
        defaults.put(BlockType.EVENT_LIST, Map.of(KEY_TITLE, "Agenda", KEY_SOURCE, Map.of()));
        // Referencia obrigatoria a entidade real (form/asset) — exigem a
        // relaxacao de validacao para o estado "ainda nao configurado".
        defaults.put(BlockType.CONTACT, Map.of("formId", ""));
        defaults.put(BlockType.FORM, Map.of("formId", ""));
        defaults.put(BlockType.DOWNLOAD, Map.of(KEY_TITLE, "Downloads", KEY_ITEMS, List.of()));
        // Tipos AUDIO, VIDEO, VIDEO_GALLERY e SOCIAL_LINKS nao sao usados pelo
        // esqueleto desta etapa. Mantidos aqui por completude do catalogo,
        // espelhando o frontend. AUDIO/VIDEO tem asset/URL em branco — estado
        // "ainda nao configurado" valido apos a relaxacao acima. VIDEO_GALLERY
        // continua exigindo ao menos 1 item (regra de contagem, nao de
        // asset/URL), entao o default traz um item com fonte/URL em branco.
        defaults.put(BlockType.AUDIO, Map.of(KEY_SOURCE, "upload", "fileAssetId", "", "spotifyUrl", "", "autoplay", false));
        defaults.put(BlockType.SOCIAL_LINKS, Map.of(KEY_ITEMS, List.of()));
        defaults.put(BlockType.VIDEO, Map.of(KEY_SOURCE, "upload", "fileAssetId", "", "youtubeUrl", "", "autoplay", false));
        defaults.put(BlockType.VIDEO_GALLERY, Map.of(KEY_ITEMS, List.of(Map.of(KEY_TITLE, "Vídeo 1", KEY_SOURCE, "youtube", KEY_YOUTUBE_URL, ""))));
        return Map.copyOf(defaults);
    }
}
