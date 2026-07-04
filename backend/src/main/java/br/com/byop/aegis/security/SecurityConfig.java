package br.com.byop.aegis.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Configuração explícita de segurança HTTP do Aegis PMS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final List<String> corsAllowedOrigins;

    public SecurityConfig(@Value("${aegis.app.cors-allowed-origins:http://localhost:5173}") List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = List.copyOf(corsAllowedOrigins);
    }

    /**
     * Define as regras HTTP da API.
     *
     * @param http configuração HTTP do Spring Security
     * @return cadeia de filtros de segurança
     * @throws Exception caso a configuração falhe
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOrigins(corsAllowedOrigins);
                    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    configuration.setAllowedHeaders(List.of("*"));
                    configuration.setAllowCredentials(true);
                    return configuration;
                }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/exports/*/download").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/forms/*/submit").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // ADR-0009: a SPA é servida pelo próprio Spring Boot na mesma origem —
                        // o shell estático (index.html + bundles) precisa ser público porque a
                        // autenticação real acontece client-side e nas chamadas a /api/v1/**.
                        .requestMatchers(
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
                                "/assets/**",
                                "/favicon.svg"
                        ).permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    /**
     * Configura a conversão de roles Keycloak para authorities Spring.
     *
     * @return conversor de autenticação JWT
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }
}
