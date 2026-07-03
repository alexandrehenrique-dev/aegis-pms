package br.com.byop.aegis.pages.domain;

import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.pages.service.PageMarkdownSanitizer;
import br.com.byop.aegis.pages.service.SectionContentValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class BlockDefaultsTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private FormReferenceService formReferenceService;

    @Mock
    private AssetReferenceService assetReferenceService;

    private SectionContentValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new SectionContentValidationService(formReferenceService, assetReferenceService,
                new PageMarkdownSanitizer());
    }

    @Test
    void shouldNeverReturnNullForAnyBlockType() {
        for (BlockType type : BlockType.values()) {
            assertThat(BlockDefaults.defaultFor(type)).isNotNull();
        }
    }

    @ParameterizedTest
    @EnumSource(value = BlockType.class, names = {"HERO", "TEXT", "RICH_TEXT", "TWO_COLUMN", "IMAGE", "IMAGE_TEXT",
            "FEATURE_GRID", "CARD_LIST", "GALLERY", "TIMELINE", "EVENT_LIST", "CTA_SECTION", "FAQ", "CONTACT",
            "FORM", "DOWNLOAD"})
    void shouldPassExistingSectionContentValidation(BlockType type) {
        assertThatCode(() -> validationService.validateSectionContent(PRODUCT_ID, type, BlockDefaults.defaultFor(type)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotReferenceAnyRealAssetOrForm() {
        assertThat(BlockDefaults.defaultFor(BlockType.GALLERY).get("items"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .allSatisfy(item -> assertThat((String) ((java.util.Map<?, ?>) item).get("src")).isEmpty());
        assertThat(BlockDefaults.defaultFor(BlockType.CONTACT)).containsEntry("formId", "");
        assertThat(BlockDefaults.defaultFor(BlockType.DOWNLOAD)).containsEntry("items", java.util.List.of());
    }
}
