package br.com.byop.aegis.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Catch-all para o React SPA (ADR-0009 + ADR-0011): qualquer rota de
 * navegação client-side não mapeada por um controller de API retorna o
 * {@code index.html} do build do Vite, deixando o React Router assumir.
 *
 * <p>{@code /api/v1/**} e {@code /actuator/**} nunca caem aqui — não estão
 * na lista de padrões abaixo. {@code /assets/**} também é deliberadamente
 * excluído: é simultaneamente uma rota client-side ("Biblioteca de Assets")
 * e o diretório de saída padrão do Vite para os bundles JS/CSS
 * ({@code frontend/vite.config.ts}, {@code build.assetsDir}) — incluí-lo
 * aqui interceptaria o próprio carregamento dos bundles estáticos antes de
 * chegarem ao {@code ResourceHttpRequestHandler} do Spring Boot. Acessar
 * {@code /assets} via refresh direto do navegador é uma limitação conhecida
 * (mesmo gap que outras rotas client-side têm fora do fluxo de navegação da
 * SPA), aceitável porque o fluxo normal de navegação (clique dentro do app)
 * nunca recarrega a página.
 *
 * <p>A lista de padrões espelha as rotas de topo de
 * {@code frontend/src/app/routes/index.tsx} — ao adicionar um novo domínio
 * de topo lá, adicione o prefixo correspondente aqui e em
 * {@code SecurityConfig} (que precisa liberar o acesso público a estas
 * mesmas rotas para o {@code index.html} ser servido sem autenticação;
 * a autenticação real acontece client-side e nas chamadas a
 * {@code /api/v1/**}).
 */
@Controller
public class SpaFallbackController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/forgot-password",
        "/forgot-password/sent",
        "/reset-password",
        "/invite",
        "/select-tenant",
        "/select-product",
        "/dashboard/**",
        "/products/**",
        "/content/**",
        "/pages/**",
        "/forms/**",
        "/analytics/**",
        "/knowledge/**",
        "/settings/**",
        "/help",
        "/users/**",
        "/audit/**",
    })
    public String index() {
        return "forward:/index.html";
    }
}
