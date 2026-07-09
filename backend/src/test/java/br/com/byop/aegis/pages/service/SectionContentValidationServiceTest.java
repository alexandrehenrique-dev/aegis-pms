package br.com.byop.aegis.pages.service;

import br.com.byop.aegis.asset.api.AssetReference;
import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.asset.exception.AssetNotFoundException;
import br.com.byop.aegis.form.api.FormReference;
import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.form.exception.FormNotFoundException;
import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.exception.InvalidSectionContentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionContentValidationServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock
    private FormReferenceService formReferenceService;

    @Mock
    private AssetReferenceService assetReferenceService;

    private SectionContentValidationService service;

    @BeforeEach
    void setUp() {
        service = new SectionContentValidationService(formReferenceService, assetReferenceService, new PageMarkdownSanitizer());
    }

    @Test
    void shouldRejectNullContent() {

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.HERO, null))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("SECTION_CONTENT_REQUIRED");
    }

    // ---- hero ----

    @Test
    void shouldAcceptValidHero() {
        Map<String, Object> content = Map.of("title", "Ola", "image", Map.of("assetId", "x", "alt", "Foto"));

        service.validateSectionContent(PRODUCT_ID, BlockType.HERO, content);
    }

    @Test
    void shouldRejectHeroWithoutTitle() {
        Map<String, Object> content = Map.of();

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.HERO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("HERO_TITLE_REQUIRED");
    }

    @Test
    void shouldRejectHeroImageWithoutAlt() {
        Map<String, Object> content = Map.of("title", "Ola", "image", Map.of("assetId", "x"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.HERO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("HERO_IMAGE_ALT_REQUIRED");
    }

    @Test
    void shouldAcceptValidHeroWithoutImage() {
        service.validateSectionContent(PRODUCT_ID, BlockType.HERO, Map.of("title", "Ola"));
    }

    @Test
    void shouldRejectHeroWithBlankTitle() {
        Map<String, Object> content = Map.of("title", "   ");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.HERO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("HERO_TITLE_REQUIRED");
    }

    // ---- image ----

    @Test
    void shouldAcceptValidImage() {

        service.validateSectionContent(PRODUCT_ID, BlockType.IMAGE, Map.of("assetId", "x", "alt", "Foto"));
    }

    @Test
    void shouldRejectImageWithoutAlt() {
        Map<String, Object> content = Map.of("assetId", "x");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.IMAGE, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("IMAGE_ALT_REQUIRED");
    }

    // ---- gallery ----

    @Test
    void shouldAcceptValidGallery() {
        Map<String, Object> content = Map.of("items", List.of(Map.of("assetId", "a", "alt", "Foto A")));

        service.validateSectionContent(PRODUCT_ID, BlockType.GALLERY, content);
    }

    @Test
    void shouldRejectGalleryItemWithoutAlt() {
        Map<String, Object> content = Map.of("items", List.of(Map.of("assetId", "a")));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.GALLERY, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("GALLERY_ITEM_ALT_REQUIRED");
    }

    @Test
    void shouldRejectGalleryWithoutItems() {
        Map<String, Object> content = Map.of();

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.GALLERY, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("GALLERY_ITEMS_REQUIRED");
    }

    @Test
    void shouldRejectGalleryWithTooManyItems() {
        List<Map<String, Object>> items = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> Map.<String, Object>of("assetId", "a" + i, "alt", "Foto " + i))
                .toList();
        Map<String, Object> content = Map.of("items", items);

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.GALLERY, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("GALLERY_ITEMS_COUNT_OUT_OF_RANGE");
    }

    // ---- card-list / feature-grid ----

    @Test
    void shouldAcceptValidCardList() {

        service.validateSectionContent(PRODUCT_ID, BlockType.CARD_LIST, Map.of("items", List.of(Map.of("title", "A"))));
    }

    @Test
    void shouldRejectCardListWithoutItems() {
        Map<String, Object> content = Map.of();

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CARD_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("ITEMS_REQUIRED");
    }

    @Test
    void shouldRejectCardListWithEmptyItemsList() {
        Map<String, Object> content = Map.of("items", List.of());

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CARD_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("ITEMS_COUNT_OUT_OF_RANGE");
    }

    @Test
    void shouldRejectCardListWithNonObjectItem() {
        Map<String, Object> content = Map.of("items", List.of("not-an-object"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CARD_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("ITEMS_REQUIRED");
    }

    @Test
    void shouldRejectCardListWithMoreThanTwelveItems() {
        List<Map<String, Object>> items = java.util.stream.IntStream.range(0, 13)
                .mapToObj(i -> Map.<String, Object>of("title", "A" + i))
                .toList();
        Map<String, Object> content = Map.of("items", items);

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CARD_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("ITEMS_COUNT_OUT_OF_RANGE");
    }

    @Test
    void shouldAcceptValidFeatureGrid() {

        service.validateSectionContent(PRODUCT_ID, BlockType.FEATURE_GRID, Map.of("items", List.of(Map.of("title", "A"))));
    }

    @Test
    void shouldRejectFeatureGridWithoutItems() {
        Map<String, Object> content = Map.of();

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.FEATURE_GRID, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("ITEMS_REQUIRED");
    }

    // ---- event-list ----

    @Test
    void shouldAcceptValidEventList() {
        Map<String, Object> content = Map.of("source", Map.of("filter", Map.of("from", "2026-01-01")));

        service.validateSectionContent(PRODUCT_ID, BlockType.EVENT_LIST, content);
    }

    @Test
    void shouldAcceptEventListWithoutFilter() {

        service.validateSectionContent(PRODUCT_ID, BlockType.EVENT_LIST, Map.of("source", Map.of()));
    }

    @Test
    void shouldAcceptEventListWithAutoSourceString() {
        Map<String, Object> content = Map.of("source", "auto", "selectedEvents", List.of());

        service.validateSectionContent(PRODUCT_ID, BlockType.EVENT_LIST, content);
    }

    @Test
    void shouldRejectEventListWithoutSource() {
        Map<String, Object> content = Map.of();

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.EVENT_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("EVENT_LIST_SOURCE_REQUIRED");
    }

    @Test
    void shouldRejectEventListWithBlankSourceString() {
        Map<String, Object> content = Map.of("source", "   ");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.EVENT_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("EVENT_LIST_SOURCE_REQUIRED");
    }

    @Test
    void shouldRejectEventListWithInvalidFilter() {
        Map<String, Object> content = Map.of("source", Map.of("filter", "not-an-object"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.EVENT_LIST, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("EVENT_LIST_FILTER_INVALID");
    }

    // ---- contact / form ----

    @Test
    void shouldAcceptValidContact() {
        UUID formId = UUID.randomUUID();
        when(formReferenceService.getRequiredReference(PRODUCT_ID, formId))
                .thenReturn(new FormReference(formId, UUID.randomUUID(), PRODUCT_ID, true, "[]", "[]"));

        service.validateSectionContent(PRODUCT_ID, BlockType.CONTACT, Map.of("formId", formId.toString()));
    }

    @Test
    void shouldRejectContactWithLegacyFields() {
        Map<String, Object> content = Map.of("fields", List.of(Map.of("label", "Nome")));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CONTACT, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("SECTION_LEGACY_FIELDS_NOT_SUPPORTED");
    }

    @Test
    void shouldAcceptContactWithoutFormId() {
        service.validateSectionContent(PRODUCT_ID, BlockType.CONTACT, Map.of());
    }

    @Test
    void shouldAcceptContactWithBlankFormId() {
        service.validateSectionContent(PRODUCT_ID, BlockType.CONTACT, Map.of("formId", ""));
    }

    @Test
    void shouldAcceptFormWithoutFormId() {
        service.validateSectionContent(PRODUCT_ID, BlockType.FORM, Map.of());
    }

    @Test
    void shouldRejectContactWithNonStringFormId() {
        Map<String, Object> content = Map.of("formId", 123);

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CONTACT, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("FORM_REFERENCE_REQUIRED");
    }

    @Test
    void shouldRejectContactWithUnknownFormId() {
        UUID formId = UUID.randomUUID();
        when(formReferenceService.getRequiredReference(PRODUCT_ID, formId)).thenThrow(new FormNotFoundException(formId));
        Map<String, Object> content = Map.of("formId", formId.toString());

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.CONTACT, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("FORM_REFERENCE_NOT_FOUND");
    }

    @Test
    void shouldAcceptValidForm() {
        UUID formId = UUID.randomUUID();
        when(formReferenceService.getRequiredReference(PRODUCT_ID, formId))
                .thenReturn(new FormReference(formId, UUID.randomUUID(), PRODUCT_ID, true, "[]", "[]"));

        service.validateSectionContent(PRODUCT_ID, BlockType.FORM, Map.of("formId", formId.toString()));
    }

    @Test
    void shouldRejectFormWithInvalidFormIdFormat() {
        Map<String, Object> content = Map.of("formId", "not-a-uuid");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.FORM, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("FORM_REFERENCE_REQUIRED");
    }

    // ---- download ----

    @Test
    void shouldAcceptValidDownload() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenReturn(new AssetReference(assetId, PRODUCT_ID, "application/pdf", "document"));
        Map<String, Object> content = Map.of("items", List.of(Map.of("fileAssetId", assetId.toString(), "title", "Catalogo")));

        service.validateSectionContent(PRODUCT_ID, BlockType.DOWNLOAD, content);
    }

    @Test
    void shouldRejectDownloadItemWithoutTitle() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenReturn(new AssetReference(assetId, PRODUCT_ID, "application/pdf", "document"));
        Map<String, Object> content = Map.of("items", List.of(Map.of("fileAssetId", assetId.toString())));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.DOWNLOAD, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("DOWNLOAD_ITEM_TITLE_REQUIRED");
    }

    @Test
    void shouldAcceptDownloadWithEmptyItemsList() {
        service.validateSectionContent(PRODUCT_ID, BlockType.DOWNLOAD, Map.of("items", List.of()));
    }

    @Test
    void shouldRejectDownloadWithoutItems() {
        Map<String, Object> content = Map.of();

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.DOWNLOAD, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("DOWNLOAD_ITEMS_REQUIRED");
    }

    @Test
    void shouldRejectDownloadItemWithAssetFromAnotherProduct() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenReturn(new AssetReference(assetId, OTHER_PRODUCT_ID, "application/pdf", "document"));
        Map<String, Object> content = Map.of("items", List.of(Map.of("fileAssetId", assetId.toString(), "title", "Catalogo")));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.DOWNLOAD, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("DOWNLOAD_ITEM_ASSET_INVALID");
    }

    @Test
    void shouldRejectDownloadItemWithUnknownAsset() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenThrow(new AssetNotFoundException(assetId));
        Map<String, Object> content = Map.of("items", List.of(Map.of("fileAssetId", assetId.toString(), "title", "Catalogo")));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.DOWNLOAD, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("DOWNLOAD_ITEM_ASSET_INVALID");
    }

    // ---- audio ----

    @Test
    void shouldAcceptValidAudioUpload() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenReturn(new AssetReference(assetId, PRODUCT_ID, "audio/mpeg", "audio"));
        Map<String, Object> content = Map.of("source", "upload", "fileAssetId", assetId.toString(), "autoplay", false);

        service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content);
    }

    @Test
    void shouldAcceptValidAudioSpotifyTrack() {
        Map<String, Object> content = Map.of("source", "spotify-track", "spotifyUrl", "https://open.spotify.com/track/abc123");

        service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content);
    }

    @Test
    void shouldAcceptValidAudioSpotifyPlaylist() {
        Map<String, Object> content = Map.of("source", "spotify-playlist", "spotifyUrl", "https://open.spotify.com/playlist/abc123");

        service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content);
    }

    @Test
    void shouldAcceptAudioUploadWithBlankAsset() {
        Map<String, Object> content = Map.of("source", "upload", "fileAssetId", "");

        service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content);
    }

    @Test
    void shouldAcceptAudioSpotifyTrackWithBlankUrl() {
        Map<String, Object> content = Map.of("source", "spotify-track", "spotifyUrl", "");

        service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content);
    }

    @Test
    void shouldRejectAudioWithInvalidSource() {
        Map<String, Object> content = Map.of("source", "youtube");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("AUDIO_SOURCE_INVALID");
    }

    @Test
    void shouldRejectAudioUploadWithAssetOfWrongCategory() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenReturn(new AssetReference(assetId, PRODUCT_ID, "video/mp4", "video"));
        Map<String, Object> content = Map.of("source", "upload", "fileAssetId", assetId.toString());

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("AUDIO_ASSET_INVALID");
    }

    @Test
    void shouldRejectAudioWithInvalidSpotifyUrl() {
        Map<String, Object> content = Map.of("source", "spotify-track", "spotifyUrl", "https://spotify.com/track/abc123");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("AUDIO_SPOTIFY_URL_INVALID");
    }

    @Test
    void shouldRejectAudioWithNonBooleanAutoplay() {
        Map<String, Object> content = Map.of("source", "spotify-track", "spotifyUrl", "https://open.spotify.com/track/abc123", "autoplay", "yes");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.AUDIO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("AUDIO_AUTOPLAY_INVALID");
    }

    // ---- social-links ----

    @Test
    void shouldAcceptValidSocialLinks() {
        Map<String, Object> content = Map.of("items", List.of(Map.of("platform", "instagram", "href", "https://instagram.com/x")));

        service.validateSectionContent(PRODUCT_ID, BlockType.SOCIAL_LINKS, content);
    }

    @Test
    void shouldRejectSocialLinkWithoutPlatform() {
        Map<String, Object> content = Map.of("items", List.of(Map.of("href", "https://instagram.com/x")));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.SOCIAL_LINKS, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("SOCIAL_LINK_PLATFORM_REQUIRED");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSocialLinkHrefs")
    void shouldRejectSocialLinkWithInvalidHref(String scenario, String href) {
        Map<String, Object> content = Map.of("items", List.of(Map.of("platform", "instagram", "href", href)));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.SOCIAL_LINKS, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("SOCIAL_LINK_HREF_INVALID");
    }

    private static Stream<Arguments> invalidSocialLinkHrefs() {
        return Stream.of(
                Arguments.of("href sem esquema/host (relativa)", "not-a-url"),
                Arguments.of("href com esquema mas sem host (mailto)", "mailto:contato@example.com"),
                Arguments.of("href malformada (espaco no host)", "https://exa mple.com")
        );
    }

    // ---- video ----

    @Test
    void shouldAcceptValidVideoUpload() {
        UUID assetId = UUID.randomUUID();
        when(assetReferenceService.getRequiredReference(assetId)).thenReturn(new AssetReference(assetId, PRODUCT_ID, "video/mp4", "video"));
        Map<String, Object> content = Map.of("source", "upload", "fileAssetId", assetId.toString());

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptValidVideoYoutubeWatchUrl() {
        Map<String, Object> content = Map.of("source", "youtube", "youtubeUrl", "https://www.youtube.com/watch?v=abcdef1234");

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptValidVideoYoutubeShortUrl() {
        Map<String, Object> content = Map.of("source", "youtube", "youtubeUrl", "https://youtu.be/abcdef1234");

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptYoutubeShareUrlWithQueryString() {
        Map<String, Object> content = Map.of(
                "source", "youtube",
                "youtubeUrl", "https://youtu.be/opQ5NfaOKTQ?si=egJqbCBkOAyXGmAd"
        );

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptYoutubeWatchUrlWithAdditionalQueryParameters() {
        Map<String, Object> content = Map.of(
                "source", "youtube",
                "youtubeUrl", "https://www.youtube.com/watch?si=abc123&v=opQ5NfaOKTQ"
        );

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptYoutubeWatchUrlWithoutWww() {
        Map<String, Object> content = Map.of(
                "source", "youtube",
                "youtubeUrl", "https://youtube.com/watch?v=opQ5NfaOKTQ"
        );

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptYoutubeVideoIdWithAllowedSymbols() {
        Map<String, Object> content = Map.of(
                "source", "youtube",
                "youtubeUrl", "https://www.youtube.com/watch?v=opQ5Nfa_OK-TQ"
        );

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptVideoUploadWithBlankAsset() {
        Map<String, Object> content = Map.of("source", "upload", "fileAssetId", "");

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldAcceptVideoYoutubeWithBlankUrl() {
        Map<String, Object> content = Map.of("source", "youtube", "youtubeUrl", "");

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content);
    }

    @Test
    void shouldRejectVideoWithInvalidYoutubeUrl() {
        Map<String, Object> content = Map.of("source", "youtube", "youtubeUrl", "https://vimeo.com/12345");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("VIDEO_YOUTUBE_URL_INVALID");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidYoutubeUrls")
    void shouldRejectVideoWithMalformedYoutubeUrl(String scenario, String url) {
        Map<String, Object> content = Map.of("source", "youtube", "youtubeUrl", url);

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("VIDEO_YOUTUBE_URL_INVALID");
    }

    private static Stream<Arguments> invalidYoutubeUrls() {
        return Stream.of(
                Arguments.of("esquema nao https", "http://www.youtube.com/watch?v=abcdef1234"),
                Arguments.of("host ausente", "https:/watch?v=abcdef1234"),
                Arguments.of("url sintaticamente invalida", "https://exa mple.com/watch?v=abcdef1234"),
                Arguments.of("watch sem query", "https://www.youtube.com/watch"),
                Arguments.of("watch com query vazia", "https://www.youtube.com/watch?"),
                Arguments.of("path diferente de watch", "https://www.youtube.com/embed/abcdef1234"),
                Arguments.of("watch sem parametro v", "https://www.youtube.com/watch?si=abc123"),
                Arguments.of("watch com v sem valor", "https://www.youtube.com/watch?v"),
                Arguments.of("id curto", "https://youtu.be/abcde"),
                Arguments.of("short url sem caminho", "https://youtu.be"),
                Arguments.of("short url com caminho vazio", "https://youtu.be/"),
                Arguments.of("id com caractere invalido", "https://youtu.be/abcde%2F123")
        );
    }

    @Test
    void shouldRejectVideoWithInvalidSource() {
        Map<String, Object> content = Map.of("source", "vimeo");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("VIDEO_SOURCE_INVALID");
    }

    @Test
    void shouldRejectVideoWithNonBooleanAutoplay() {
        Map<String, Object> content = Map.of("source", "youtube", "youtubeUrl", "https://youtu.be/abcdef1234", "autoplay", "yes");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("VIDEO_AUTOPLAY_INVALID");
    }

    // ---- video-gallery ----

    @Test
    void shouldAcceptValidVideoGallery() {
        Map<String, Object> content = Map.of("items", List.of(
                Map.of("source", "youtube", "youtubeUrl", "https://youtu.be/abcdef1234", "title", "Clipe 1")
        ));

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO_GALLERY, content);
    }

    @Test
    void shouldRejectVideoGalleryWithTooManyItems() {
        List<Map<String, Object>> items = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> Map.<String, Object>of("source", "youtube", "youtubeUrl", "https://youtu.be/abcdef1234", "title", "Clipe " + i))
                .toList();
        Map<String, Object> content = Map.of("items", items);

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO_GALLERY, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("VIDEO_GALLERY_ITEMS_COUNT_OUT_OF_RANGE");
    }

    @Test
    void shouldAcceptVideoGalleryItemWithBlankYoutubeUrl() {
        Map<String, Object> content = Map.of("items", List.of(Map.of("source", "youtube", "youtubeUrl", "", "title", "Clipe 1")));

        service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO_GALLERY, content);
    }

    @Test
    void shouldRejectVideoGalleryItemWithoutTitle() {
        Map<String, Object> content = Map.of("items", List.of(Map.of("source", "youtube", "youtubeUrl", "https://youtu.be/abcdef1234")));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.VIDEO_GALLERY, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("VIDEO_GALLERY_ITEM_TITLE_REQUIRED");
    }

    // ---- demais tipos (validacao generica, sem regra especifica) ----

    @Test
    void shouldAcceptGenericBlockTypesWithoutSpecificRule() {

        service.validateSectionContent(PRODUCT_ID, BlockType.TEXT, Map.of("body", "Um texto qualquer"));
        service.validateSectionContent(PRODUCT_ID, BlockType.RICH_TEXT, Map.of("body", "Um texto"));
        service.validateSectionContent(PRODUCT_ID, BlockType.TIMELINE, Map.of());
        service.validateSectionContent(PRODUCT_ID, BlockType.FAQ, Map.of());
        service.validateSectionContent(PRODUCT_ID, BlockType.CTA_SECTION, Map.of());
        service.validateSectionContent(PRODUCT_ID, BlockType.IMAGE_TEXT, Map.of());
    }

    // ---- acceptsChildren (two-column) ----

    @Test
    void shouldAcceptValidTwoColumnChildren() {
        Map<String, Object> content = Map.of(
                "left", Map.of("type", "text", "body", "Esquerda"),
                "right", Map.of("type", "image", "assetId", "x", "alt", "Foto")
        );

        service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content);
    }

    @Test
    void shouldAcceptTwoColumnWithoutOptionalSlot() {
        Map<String, Object> content = Map.of("left", Map.of("type", "text", "body", "Esquerda"));

        service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content);
    }

    @Test
    void shouldRejectTwoColumnNestedInsideItself() {
        Map<String, Object> content = Map.of("left", Map.of("type", "two-column"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("CHILD_TYPE_NOT_ALLOWED");
    }

    @Test
    void shouldRejectTwoColumnChildTypeNotInCatalog() {
        Map<String, Object> content = Map.of("left", Map.of("type", "hero", "title", "x"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("CHILD_TYPE_NOT_ALLOWED");
    }

    @Test
    void shouldRejectTwoColumnChildWithUnknownType() {
        Map<String, Object> content = Map.of("left", Map.of("type", "carousel-3d"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("CHILD_TYPE_UNKNOWN");
    }

    @Test
    void shouldRejectTwoColumnChildWithoutType() {
        Map<String, Object> content = Map.of("left", Map.of("body", "sem tipo"));

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("CHILD_TYPE_REQUIRED");
    }

    @Test
    void shouldRejectTwoColumnChildSlotThatIsNotAnObject() {
        Map<String, Object> content = Map.of("left", "not-an-object");

        assertThatThrownBy(() -> service.validateSectionContent(PRODUCT_ID, BlockType.TWO_COLUMN, content))
                .isInstanceOf(InvalidSectionContentException.class)
                .extracting("errorCode").isEqualTo("CHILD_CONTENT_INVALID");
    }

    // ---- sanitizeContent ----

    @Test
    void shouldReturnEmptyMapWhenSanitizingEmptyContent() {
        assertThat(service.sanitizeContent(Map.of())).isEmpty();
    }

    @Test
    void shouldSanitizeBodyAndDescriptionAtAnyNestingLevel() {
        Map<String, Object> content = Map.of(
                "body", "<script>alert(1)</script><strong>ok</strong>",
                "description", "<em>ok</em><iframe src=\"x\"></iframe>",
                "left", Map.of("type", "text", "body", "<p>aninhado</p><script>x</script>"),
                "items", List.of(Map.of("title", "A", "description", "<script>y</script>texto"))
        );

        Map<String, Object> sanitized = service.sanitizeContent(content);

        assertThat((String) sanitized.get("body")).doesNotContain("script").contains("<strong>ok</strong>");
        assertThat((String) sanitized.get("description")).doesNotContain("iframe").contains("<em>ok</em>");

        Map<?, ?> left = (Map<?, ?>) sanitized.get("left");
        assertThat((String) left.get("body")).doesNotContain("script").contains("<p>aninhado</p>");

        List<?> items = (List<?>) sanitized.get("items");
        Map<?, ?> firstItem = (Map<?, ?>) items.get(0);
        assertThat((String) firstItem.get("description")).doesNotContain("script").contains("texto");
    }

    @Test
    void shouldLeaveNonTextFieldsUnchangedWhenSanitizing() {
        Map<String, Object> content = Map.of("title", "Titulo simples", "order", 3, "enabled", true);

        Map<String, Object> sanitized = service.sanitizeContent(content);

        assertThat(sanitized).containsEntry("title", "Titulo simples").containsEntry("order", 3).containsEntry("enabled", true);
    }

    @Test
    void shouldLeaveNonObjectListItemsUnchangedWhenSanitizing() {
        Map<String, Object> content = Map.of("tags", List.of("um", "dois"));

        Map<String, Object> sanitized = service.sanitizeContent(content);

        assertThat(sanitized).containsEntry("tags", List.of("um", "dois"));
    }
}
