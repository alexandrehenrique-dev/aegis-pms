package br.com.byop.aegis;

import br.com.byop.aegis.identity.auth.config.KeycloakProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.Modulithic;

@Modulithic
@SpringBootApplication
@EnableConfigurationProperties(KeycloakProperties.class)
public class AegisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisApplication.class, args);
    }
}