package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.contract.FloatingWhatsappRequest;
import br.com.byop.aegis.pages.contract.FooterRequest;
import br.com.byop.aegis.pages.contract.NavLinkRequest;
import br.com.byop.aegis.pages.contract.NavbarRequest;
import br.com.byop.aegis.pages.contract.SocialLinkRequest;
import br.com.byop.aegis.pages.contract.UpdateProductGlobalsRequest;
import br.com.byop.aegis.pages.dto.ProductGlobalsResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ProductGlobalsMapperTest {

    private final ProductGlobalsMapper mapper = Mappers.getMapper(ProductGlobalsMapper.class);

    @Test
    void shouldMapRequestToResponse() {
        UUID logoAssetId = UUID.randomUUID();
        UpdateProductGlobalsRequest request = new UpdateProductGlobalsRequest(
                new NavbarRequest(logoAssetId, List.of(new NavLinkRequest("Home", "/"))),
                new FooterRequest("Rua Um, 100", List.of(new NavLinkRequest("Contato", "/contato"))),
                List.of(new SocialLinkRequest("instagram", "https://instagram.com/x")),
                new FloatingWhatsappRequest(true, "5511999999999", "Oi")
        );

        ProductGlobalsResponse response = mapper.toResponse(request);

        assertThat(response.navbar().logoAssetId()).isEqualTo(logoAssetId);
        assertThat(response.navbar().links()).extracting("label", "href").containsExactly(tuple("Home", "/"));
        assertThat(response.footer().addressText()).isEqualTo("Rua Um, 100");
        assertThat(response.footer().links()).extracting("label", "href").containsExactly(tuple("Contato", "/contato"));
        assertThat(response.socialLinks()).extracting("platform", "href")
                .containsExactly(tuple("instagram", "https://instagram.com/x"));
        assertThat(response.floatingWhatsapp().enabled()).isTrue();
        assertThat(response.floatingWhatsapp().number()).isEqualTo("5511999999999");
        assertThat(response.floatingWhatsapp().message()).isEqualTo("Oi");
    }

    @Test
    void shouldMapWithoutFloatingWhatsapp() {
        UpdateProductGlobalsRequest request = new UpdateProductGlobalsRequest(
                new NavbarRequest(null, List.of()),
                new FooterRequest(null, List.of()),
                List.of(),
                null
        );

        ProductGlobalsResponse response = mapper.toResponse(request);

        assertThat(response.navbar().logoAssetId()).isNull();
        assertThat(response.navbar().links()).isEmpty();
        assertThat(response.floatingWhatsapp()).isNull();
    }

    @Test
    void shouldMapRequestWithNullSocialLinks() {
        UpdateProductGlobalsRequest request = new UpdateProductGlobalsRequest(
                new NavbarRequest(null, List.of()), new FooterRequest(null, List.of()), null, null
        );

        assertThat(mapper.toResponse(request).socialLinks()).isNull();
    }

    @Test
    void shouldMapNavbarAndFooterWithNullLinks() {
        assertThat(mapper.toNavbarResponse(new NavbarRequest(null, null)).links()).isNull();
        assertThat(mapper.toFooterResponse(new FooterRequest(null, null)).links()).isNull();
    }

    @Test
    void shouldReturnNullWhenRequestIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toNavbarResponse(null)).isNull();
        assertThat(mapper.toFooterResponse(null)).isNull();
        assertThat(mapper.toNavLinkResponse(null)).isNull();
        assertThat(mapper.toSocialLinkResponse(null)).isNull();
        assertThat(mapper.toFloatingWhatsappResponse(null)).isNull();
    }
}
