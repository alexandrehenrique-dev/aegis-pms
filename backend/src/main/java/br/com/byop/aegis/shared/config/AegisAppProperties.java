package br.com.byop.aegis.shared.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuração pública da aplicação e das origens autorizadas a acessar a API.
 */
@Validated
@ConfigurationProperties(prefix = "aegis.app")
public record AegisAppProperties(
        @NotBlank
        @Pattern(
                regexp = "https?://[^/?:#*]+(?::[0-9]{1,5})?",
                message = "deve ser uma URL HTTP(S) sem caminho, query, fragmento ou wildcard"
        )
        String baseUrl,
        @NotEmpty
        List<
                @NotBlank
                @Pattern(
                        regexp = "https?://[^/?:#*]+(?::[0-9]{1,5})?",
                        message = "deve conter somente origens HTTP(S) explícitas, sem wildcard ou caminho"
                )
                String
                > corsAllowedOrigins
) {

    public AegisAppProperties {
        corsAllowedOrigins = List.copyOf(corsAllowedOrigins);
    }
}
