package br.com.byop.aegis.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Catch-all para o React SPA (ADR-0009 + ADR-0011): qualquer rota de
 * navegação client-side não mapeada por um controller de API retorna o
 * {@code index.html} do build do Vite, deixando o React Router assumir.
 *
 * <p>{@code /api/**} e {@code /actuator/**} nunca caem aqui: continuam sob os
 * controllers REST e Actuator. Arquivos com extensao tambem ficam fora do
 * fallback para que bundles, imagens, fontes e sourcemaps sejam servidos pelo
 * {@code ResourceHttpRequestHandler} do Spring Boot.
 */
@Controller
public class SpaFallbackController {

    @GetMapping(value = {
        "/",
        "/{path:^(?!api$|actuator$)[^\\.]*$}",
        "/{segment:^(?!api$|actuator$).*$}/{path:[^\\.]*}",
        "/{segment:^(?!api$|actuator$).*$}/*/{path:[^\\.]*}",
        "/{segment:^(?!api$|actuator$).*$}/*/*/{path:[^\\.]*}",
        "/{segment:^(?!api$|actuator$).*$}/*/*/*/{path:[^\\.]*}",
        "/{segment:^(?!api$|actuator$).*$}/*/*/*/*/{path:[^\\.]*}",
        "/{segment:^(?!api$|actuator$).*$}/*/*/*/*/*/{path:[^\\.]*}",
        "/{segment:^(?!api$|actuator$).*$}/*/*/*/*/*/*/{path:[^\\.]*}"
    })
    public String index(
            @PathVariable(name = "path", required = false) String path,
            @PathVariable(name = "segment", required = false) String segment) {
        return "forward:/index.html";
    }
}
