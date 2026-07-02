package br.com.byop.aegis.pages.contract;

public record FloatingWhatsappRequest(
        boolean enabled,
        String number,
        String message
) {
}
