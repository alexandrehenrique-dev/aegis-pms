package br.com.byop.aegis.pages.dto;

public record FloatingWhatsappResponse(
        boolean enabled,
        String number,
        String message
) {
}
