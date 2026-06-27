package br.com.byop.aegis.asset.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetCategoryTest {

    @Test
    void shouldResolveAllContractValues() {
        assertThat(AssetCategory.fromContractValue("image")).isEqualTo(AssetCategory.IMAGE);
        assertThat(AssetCategory.fromContractValue("pdf")).isEqualTo(AssetCategory.PDF);
        assertThat(AssetCategory.fromContractValue("audio")).isEqualTo(AssetCategory.AUDIO);
        assertThat(AssetCategory.fromContractValue("video")).isEqualTo(AssetCategory.VIDEO);
        assertThat(AssetCategory.fromContractValue("document")).isEqualTo(AssetCategory.DOCUMENT);
    }

    @Test
    void shouldExposeContractValue() {
        assertThat(AssetCategory.IMAGE.contractValue()).isEqualTo("image");
    }

    @Test
    void shouldRejectUnknownContractValue() {
        assertThatThrownBy(() -> AssetCategory.fromContractValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
