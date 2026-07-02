package br.com.byop.aegis.pages.dto;

import java.util.List;

public record ProductGlobalsResponse(
        NavbarResponse navbar,
        FooterResponse footer,
        List<SocialLinkResponse> socialLinks,
        FloatingWhatsappResponse floatingWhatsapp
) {
}
