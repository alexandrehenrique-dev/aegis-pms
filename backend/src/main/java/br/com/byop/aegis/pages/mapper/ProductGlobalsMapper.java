package br.com.byop.aegis.pages.mapper;

import br.com.byop.aegis.pages.contract.FloatingWhatsappRequest;
import br.com.byop.aegis.pages.contract.FooterRequest;
import br.com.byop.aegis.pages.contract.NavLinkRequest;
import br.com.byop.aegis.pages.contract.NavbarRequest;
import br.com.byop.aegis.pages.contract.SocialLinkRequest;
import br.com.byop.aegis.pages.contract.UpdateProductGlobalsRequest;
import br.com.byop.aegis.pages.dto.FloatingWhatsappResponse;
import br.com.byop.aegis.pages.dto.FooterResponse;
import br.com.byop.aegis.pages.dto.NavLinkResponse;
import br.com.byop.aegis.pages.dto.NavbarResponse;
import br.com.byop.aegis.pages.dto.ProductGlobalsResponse;
import br.com.byop.aegis.pages.dto.SocialLinkResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductGlobalsMapper {

    ProductGlobalsResponse toResponse(UpdateProductGlobalsRequest request);

    NavbarResponse toNavbarResponse(NavbarRequest request);

    FooterResponse toFooterResponse(FooterRequest request);

    NavLinkResponse toNavLinkResponse(NavLinkRequest request);

    SocialLinkResponse toSocialLinkResponse(SocialLinkRequest request);

    FloatingWhatsappResponse toFloatingWhatsappResponse(FloatingWhatsappRequest request);
}
