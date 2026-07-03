package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.asset.api.AssetReference;
import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.BlockTypeCatalog;
import br.com.byop.aegis.pages.domain.BlockTypeDefinition;
import br.com.byop.aegis.pages.exception.InvalidSectionContentException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * Validacao centralizada de {@code contentJson} de uma {@link
 * br.com.byop.aegis.pages.domain.PageSection}, por {@link BlockType} (Secao C
 * da Sprint 23) — a classe mais critica do dominio {@code pages}. Toda regra
 * fica nesta classe, nunca espalhada por controllers/services; o mecanismo de
 * sub-blocos ({@code acceptsChildren}) e sempre generico, orientado pelo
 * catalogo de {@link BlockTypeCatalog}, nunca um {@code if}/{@code switch}
 * hardcoded por {@link BlockType}.
 */
@Service
public class SectionContentValidationService {

    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_ALT = "alt";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_FILTER = "filter";
    private static final String KEY_FORM_ID = "formId";
    private static final String KEY_FIELDS = "fields";
    private static final String KEY_FILE_ASSET_ID = "fileAssetId";
    private static final String KEY_SPOTIFY_URL = "spotifyUrl";
    private static final String KEY_YOUTUBE_URL = "youtubeUrl";
    private static final String KEY_AUTOPLAY = "autoplay";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_HREF = "href";
    private static final String KEY_TYPE = "type";

    private static final String SOURCE_UPLOAD = "upload";
    private static final String SOURCE_SPOTIFY_TRACK = "spotify-track";
    private static final String SOURCE_SPOTIFY_PLAYLIST = "spotify-playlist";
    private static final String SOURCE_YOUTUBE = "youtube";

    private static final String CATEGORY_AUDIO = "audio";
    private static final String CATEGORY_VIDEO = "video";

    private static final Pattern SPOTIFY_URL_PATTERN = Pattern.compile("^https://open\\.spotify\\.com/.+");
    private static final Pattern YOUTUBE_WATCH_PATTERN = Pattern.compile("^https://(www\\.)?youtube\\.com/watch\\?v=[\\w-]{6,}$");
    private static final Pattern YOUTUBE_SHORT_PATTERN = Pattern.compile("^https://youtu\\.be/[\\w-]{6,}$");

    private final FormReferenceService formReferenceService;
    private final AssetReferenceService assetReferenceService;
    private final PageMarkdownSanitizer markdownSanitizer;
    private final Map<BlockType, BiConsumer<UUID, Map<String, Object>>> rules;

    public SectionContentValidationService(FormReferenceService formReferenceService,
                                           AssetReferenceService assetReferenceService,
                                           PageMarkdownSanitizer markdownSanitizer) {
        this.formReferenceService = formReferenceService;
        this.assetReferenceService = assetReferenceService;
        this.markdownSanitizer = markdownSanitizer;
        this.rules = buildRules();
    }

    /**
     * Valida {@code content} contra a regra minima do {@code type} informado
     * (Secao C) e, de forma sempre generica, contra o mecanismo de sub-blocos
     * (Secao B) definido em {@link BlockTypeCatalog}.
     *
     * @param productId produto ao qual a secao pertence — usado para validar referencias cruzadas (formId/fileAssetId)
     * @param type tipo de bloco, ja validado como pertencente ao catalogo fechado
     * @param content {@code contentJson} decodificado da secao
     */
    public void validateSectionContent(UUID productId, BlockType type, Map<String, Object> content) {
        if (content == null) {
            throw invalid("SECTION_CONTENT_REQUIRED", "Section content is required");
        }
        rules.getOrDefault(type, (ignoredProductId, ignoredContent) -> { }).accept(productId, content);
        validateChildren(BlockTypeCatalog.definitionOf(type), content);
    }

    /**
     * Aplica, de forma recursiva, a sanitizacao de markdown a cada campo de
     * texto longo dentro de {@code content} ({@code body}/{@code description},
     * em qualquer nivel de aninhamento — inclusive dentro de sub-blocos e
     * itens de lista), com a mesma allowlist da Sprint 11. Aplicado ao salvar
     * a secao, nunca so na hora de renderizar. O chamador decide se {@code
     * content} ausente (campo opcional, ex. {@code settings}) deve ou nao ser
     * sanitizado — este metodo sempre recebe e devolve um mapa nao nulo,
     * nunca {@code null} (java:S1168).
     *
     * @param content {@code contentJson} decodificado da secao, nunca {@code null}
     * @return copia sanitizada de {@code content}, nunca {@code null}
     */
    public Map<String, Object> sanitizeContent(Map<String, Object> content) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        content.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value)));
        return sanitized;
    }

    private Object sanitizeValue(String key, Object value) {
        if (value instanceof String text && (KEY_BODY.equals(key) || KEY_DESCRIPTION.equals(key))) {
            return markdownSanitizer.sanitize(text);
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeContent(toStringObjectMap(map));
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> item instanceof Map<?, ?> map ? sanitizeContent(toStringObjectMap(map)) : item)
                    .toList();
        }
        return value;
    }

    private Map<BlockType, BiConsumer<UUID, Map<String, Object>>> buildRules() {
        Map<BlockType, BiConsumer<UUID, Map<String, Object>>> table = new EnumMap<>(BlockType.class);
        table.put(BlockType.HERO, this::validateHero);
        table.put(BlockType.IMAGE, this::validateImage);
        table.put(BlockType.GALLERY, this::validateGallery);
        table.put(BlockType.CARD_LIST, this::validateItemsBounded1to12);
        table.put(BlockType.FEATURE_GRID, this::validateItemsBounded1to12);
        table.put(BlockType.EVENT_LIST, this::validateEventList);
        table.put(BlockType.CONTACT, this::validateFormReference);
        table.put(BlockType.FORM, this::validateFormReference);
        table.put(BlockType.DOWNLOAD, this::validateDownload);
        table.put(BlockType.AUDIO, this::validateAudio);
        table.put(BlockType.SOCIAL_LINKS, this::validateSocialLinks);
        table.put(BlockType.VIDEO, this::validateVideo);
        table.put(BlockType.VIDEO_GALLERY, this::validateVideoGallery);
        return Map.copyOf(table);
    }

    private void validateHero(UUID productId, Map<String, Object> content) {
        requireNonBlankString(content, KEY_TITLE, "HERO_TITLE_REQUIRED");
        Object image = content.get(KEY_IMAGE);
        if (image instanceof Map<?, ?> imageMap) {
            requireNonBlankString(toStringObjectMap(imageMap), KEY_ALT, "HERO_IMAGE_ALT_REQUIRED");
        }
    }

    private void validateImage(UUID productId, Map<String, Object> content) {
        requireNonBlankString(content, KEY_ALT, "IMAGE_ALT_REQUIRED");
    }

    private void validateGallery(UUID productId, Map<String, Object> content) {
        List<Map<String, Object>> items = requireItemsList(content, "GALLERY_ITEMS_REQUIRED");
        requireItemsCountBetween(items, 1, 50, "GALLERY_ITEMS_COUNT_OUT_OF_RANGE");
        for (Map<String, Object> item : items) {
            requireNonBlankString(item, KEY_ALT, "GALLERY_ITEM_ALT_REQUIRED");
        }
    }

    private void validateItemsBounded1to12(UUID productId, Map<String, Object> content) {
        List<Map<String, Object>> items = requireItemsList(content, "ITEMS_REQUIRED");
        requireItemsCountBetween(items, 1, 12, "ITEMS_COUNT_OUT_OF_RANGE");
    }

    private void validateEventList(UUID productId, Map<String, Object> content) {
        Object source = content.get(KEY_SOURCE);
        if (!(source instanceof Map<?, ?> sourceMap)) {
            throw invalid("EVENT_LIST_SOURCE_REQUIRED", "source is required");
        }
        Object filter = toStringObjectMap(sourceMap).get(KEY_FILTER);
        if (filter != null && !(filter instanceof Map<?, ?>)) {
            throw invalid("EVENT_LIST_FILTER_INVALID", "filter must be an object");
        }
    }

    /**
     * Valida a referencia de {@code formId}, exceto quando ausente/em branco —
     * um {@code contact}/{@code form} "ainda nao vinculado a um form" e um
     * estado valido (ex.: esqueleto de pagina criado no scaffold de produto,
     * Etapa 26, antes de o Tenant Admin escolher/criar o form real). Um
     * {@code formId} preenchido continua validado normalmente, inclusive
     * formato invalido ou referencia inexistente.
     */
    private void validateFormReference(UUID productId, Map<String, Object> content) {
        if (content.containsKey(KEY_FIELDS)) {
            throw invalid("SECTION_LEGACY_FIELDS_NOT_SUPPORTED", "fields[] is no longer supported, use formId");
        }
        if (isBlank(content.get(KEY_FORM_ID))) {
            return;
        }
        UUID formId = requireUuid(content, KEY_FORM_ID, "FORM_REFERENCE_REQUIRED");
        try {
            formReferenceService.getRequiredReference(productId, formId);
        } catch (RuntimeException _) {
            throw invalid("FORM_REFERENCE_NOT_FOUND", "formId does not reference an existing form in this product");
        }
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }

    /**
     * Valida a lista de {@code items}, que pode estar vazia — um {@code download}
     * "ainda sem arquivos" e um estado valido (ex.: esqueleto de pagina criado
     * no scaffold de produto, Etapa 26, antes de o Tenant Admin subir o
     * arquivo real); a chave {@code items} continua obrigatoria (nunca
     * {@code null}/ausente), só o tamanho minimo deixou de ser exigido.
     */
    private void validateDownload(UUID productId, Map<String, Object> content) {
        List<Map<String, Object>> items = requireItemsList(content, "DOWNLOAD_ITEMS_REQUIRED");
        for (Map<String, Object> item : items) {
            requireAsset(productId, item, "DOWNLOAD_ITEM_ASSET_INVALID", null);
            requireNonBlankString(item, KEY_TITLE, "DOWNLOAD_ITEM_TITLE_REQUIRED");
        }
    }

    private void validateAudio(UUID productId, Map<String, Object> content) {
        String source = requireNonBlankString(content, KEY_SOURCE, "AUDIO_SOURCE_REQUIRED");
        if (SOURCE_UPLOAD.equals(source)) {
            requireAsset(productId, content, "AUDIO_ASSET_INVALID", CATEGORY_AUDIO);
        } else if (SOURCE_SPOTIFY_TRACK.equals(source) || SOURCE_SPOTIFY_PLAYLIST.equals(source)) {
            requirePattern(content, KEY_SPOTIFY_URL, SPOTIFY_URL_PATTERN, "AUDIO_SPOTIFY_URL_INVALID");
        } else {
            throw invalid("AUDIO_SOURCE_INVALID", "source must be upload, spotify-track or spotify-playlist");
        }
        requireOptionalBoolean(content, KEY_AUTOPLAY, "AUDIO_AUTOPLAY_INVALID");
    }

    private void validateSocialLinks(UUID productId, Map<String, Object> content) {
        List<Map<String, Object>> items = requireItemsList(content, "SOCIAL_LINKS_ITEMS_REQUIRED");
        for (Map<String, Object> item : items) {
            requireNonBlankString(item, KEY_PLATFORM, "SOCIAL_LINK_PLATFORM_REQUIRED");
            requireValidUrl(item, KEY_HREF, "SOCIAL_LINK_HREF_INVALID");
        }
    }

    private void validateVideo(UUID productId, Map<String, Object> content) {
        validateVideoItem(productId, content, "VIDEO");
        requireOptionalBoolean(content, KEY_AUTOPLAY, "VIDEO_AUTOPLAY_INVALID");
    }

    private void validateVideoGallery(UUID productId, Map<String, Object> content) {
        List<Map<String, Object>> items = requireItemsList(content, "VIDEO_GALLERY_ITEMS_REQUIRED");
        requireItemsCountBetween(items, 1, 50, "VIDEO_GALLERY_ITEMS_COUNT_OUT_OF_RANGE");
        for (Map<String, Object> item : items) {
            validateVideoItem(productId, item, "VIDEO_GALLERY_ITEM");
            requireNonBlankString(item, KEY_TITLE, "VIDEO_GALLERY_ITEM_TITLE_REQUIRED");
        }
    }

    private void validateVideoItem(UUID productId, Map<String, Object> content, String errorPrefix) {
        String source = requireNonBlankString(content, KEY_SOURCE, errorPrefix + "_SOURCE_REQUIRED");
        if (SOURCE_UPLOAD.equals(source)) {
            requireAsset(productId, content, errorPrefix + "_ASSET_INVALID", CATEGORY_VIDEO);
        } else if (SOURCE_YOUTUBE.equals(source)) {
            String url = requireNonBlankString(content, KEY_YOUTUBE_URL, errorPrefix + "_YOUTUBE_URL_REQUIRED");
            if (!YOUTUBE_WATCH_PATTERN.matcher(url).matches() && !YOUTUBE_SHORT_PATTERN.matcher(url).matches()) {
                throw invalid(errorPrefix + "_YOUTUBE_URL_INVALID", "youtubeUrl does not match the expected pattern");
            }
        } else {
            throw invalid(errorPrefix + "_SOURCE_INVALID", "source must be upload or youtube");
        }
    }

    private void validateChildren(BlockTypeDefinition definition, Map<String, Object> content) {
        if (!definition.acceptsChildBlocks()) {
            return;
        }
        for (String slot : definition.childSlots()) {
            validateChildSlot(definition, slot, content.get(slot));
        }
    }

    private void validateChildSlot(BlockTypeDefinition definition, String slot, Object rawChild) {
        if (rawChild == null) {
            return;
        }
        if (!(rawChild instanceof Map<?, ?> childMap)) {
            throw invalid("CHILD_CONTENT_INVALID", slot + " must be an object");
        }
        Map<String, Object> child = toStringObjectMap(childMap);
        BlockType childType = parseChildType(child, slot);
        if (childType == definition.type() || !definition.acceptsChildren().contains(childType)) {
            throw invalid("CHILD_TYPE_NOT_ALLOWED", slot + " has an unsupported child type");
        }
    }

    private BlockType parseChildType(Map<String, Object> child, String slot) {
        Object rawType = child.get(KEY_TYPE);
        if (!(rawType instanceof String text)) {
            throw invalid("CHILD_TYPE_REQUIRED", slot + ".type is required");
        }
        try {
            return BlockType.fromContractValue(text);
        } catch (IllegalArgumentException _) {
            throw invalid("CHILD_TYPE_UNKNOWN", slot + ".type is not a known block type");
        }
    }

    private List<Map<String, Object>> requireItemsList(Map<String, Object> content, String errorCode) {
        Object items = content.get(KEY_ITEMS);
        if (!(items instanceof List<?> list) || list.stream().anyMatch(item -> !(item instanceof Map<?, ?>))) {
            throw invalid(errorCode, "items must be a list of objects");
        }
        return list.stream().map(item -> toStringObjectMap((Map<?, ?>) item)).toList();
    }

    private void requireItemsCountBetween(List<Map<String, Object>> items, int min, int max, String errorCode) {
        if (items.size() < min || items.size() > max) {
            throw invalid(errorCode, "items must have between " + min + " and " + max + " elements");
        }
    }

    private String requireNonBlankString(Map<String, Object> content, String key, String errorCode) {
        Object value = content.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalid(errorCode, key + " is required");
        }
        return text;
    }

    private UUID requireUuid(Map<String, Object> content, String key, String errorCode) {
        String raw = requireNonBlankString(content, key, errorCode);
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException _) {
            throw invalid(errorCode, key + " must be a valid UUID");
        }
    }

    private void requireAsset(UUID productId, Map<String, Object> content, String errorCode, String expectedCategory) {
        UUID assetId = requireUuid(content, KEY_FILE_ASSET_ID, errorCode);
        AssetReference asset;
        try {
            asset = assetReferenceService.getRequiredReference(assetId);
        } catch (RuntimeException _) {
            throw invalid(errorCode, "fileAssetId does not reference an existing asset");
        }
        if (!productId.equals(asset.productId())) {
            throw invalid(errorCode, "fileAssetId belongs to another product");
        }
        if (expectedCategory != null && !expectedCategory.equals(asset.category())) {
            throw invalid(errorCode, "fileAssetId does not reference an asset of category " + expectedCategory);
        }
    }

    private void requirePattern(Map<String, Object> content, String key, Pattern pattern, String errorCode) {
        String value = requireNonBlankString(content, key, errorCode);
        if (!pattern.matcher(value).matches()) {
            throw invalid(errorCode, key + " does not match the expected pattern");
        }
    }

    private void requireValidUrl(Map<String, Object> content, String key, String errorCode) {
        String value = requireNonBlankString(content, key, errorCode);
        try {
            URI uri = new URI(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw invalid(errorCode, key + " must be a valid URL");
            }
        } catch (URISyntaxException _) {
            throw invalid(errorCode, key + " must be a valid URL");
        }
    }

    private void requireOptionalBoolean(Map<String, Object> content, String key, String errorCode) {
        Object value = content.get(key);
        if (value != null && !(value instanceof Boolean)) {
            throw invalid(errorCode, key + " must be a boolean");
        }
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(key.toString(), value));
        return result;
    }

    private InvalidSectionContentException invalid(String errorCode, String message) {
        return new InvalidSectionContentException(errorCode, message);
    }
}
