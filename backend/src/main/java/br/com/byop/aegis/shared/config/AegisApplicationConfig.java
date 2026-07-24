package br.com.byop.aegis.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.Arrays;
import java.util.List;

/**
 * Registra e expõe a configuração operacional não sensível do Aegis.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AegisAppProperties.class)
public class AegisApplicationConfig {

    @Bean
    ApplicationRunner logAegisApplicationConfiguration(
            AegisAppProperties properties,
            Environment environment
    ) {
        return arguments -> {
            List<String> activeProfiles = activeProfiles(environment);
            String environmentMode = environment.acceptsProfiles(Profiles.of("prod"))
                    ? "production"
                    : "non-production";

            log.info(
                    "Aegis application configuration: profiles={}, baseUrl={}, corsOrigins={}, environmentMode={}",
                    activeProfiles,
                    properties.baseUrl(),
                    properties.corsAllowedOrigins(),
                    environmentMode
            );
        };
    }

    private List<String> activeProfiles(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return Arrays.asList(profiles);
    }
}
