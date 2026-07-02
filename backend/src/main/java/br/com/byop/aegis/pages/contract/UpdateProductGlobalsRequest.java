package br.com.byop.aegis.pages.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateProductGlobalsRequest(
        @NotNull @Valid NavbarRequest navbar,
        @NotNull @Valid FooterRequest footer,
        @NotNull List<@Valid SocialLinkRequest> socialLinks,
        @Valid FloatingWhatsappRequest floatingWhatsapp
) {
}
