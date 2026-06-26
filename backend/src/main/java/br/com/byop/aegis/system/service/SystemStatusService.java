package br.com.byop.aegis.system.service;

import br.com.byop.aegis.system.dto.SystemStatusResponse;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class SystemStatusService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String KNOWLEDGE_GRAPH_SERVICE_BEAN = "knowledgeGraphService";
    private static final String DEFAULT_BUILD_VERSION = "0.0.1-SNAPSHOT";
    private static final String DATABASE_UNAVAILABLE_MESSAGE = "Banco indisponivel.";

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final Optional<BuildProperties> buildProperties;

    public SystemStatusService(
            JdbcTemplate jdbcTemplate,
            Environment environment,
            ApplicationContext applicationContext,
            Optional<BuildProperties> buildProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.buildProperties = buildProperties;
    }

    public SystemStatusResponse currentStatus() {
        DatabaseStatus databaseStatus = databaseStatus();
        String keycloakIssuer = keycloakIssuer();

        return new SystemStatusResponse(
                STATUS_UP,
                databaseStatus.status(),
                databaseStatus.message(),
                isConfigured(keycloakIssuer),
                keycloakIssuer,
                knowledgeGraphStatus(),
                activeProfiles(),
                buildVersion()
        );
    }

    private DatabaseStatus databaseStatus() {
        try {
            Integer probe = jdbcTemplate.queryForObject("select 1", Integer.class);
            if (Integer.valueOf(1).equals(probe)) {
                return new DatabaseStatus(STATUS_UP, "Banco conectado.");
            }

            return new DatabaseStatus(STATUS_DOWN, DATABASE_UNAVAILABLE_MESSAGE);
        } catch (Exception exception) {
            return new DatabaseStatus(STATUS_DOWN, readableMessage(exception));
        }
    }

    private String readableMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return DATABASE_UNAVAILABLE_MESSAGE;
        }

        return message;
    }

    private String keycloakIssuer() {
        return environment.getProperty(
                "keycloak.issuer-uri",
                environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "")
        );
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private String knowledgeGraphStatus() {
        if (applicationContext.containsBean(KNOWLEDGE_GRAPH_SERVICE_BEAN)) {
            return STATUS_UP;
        }

        return STATUS_DOWN;
    }

    private List<String> activeProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return Arrays.asList(activeProfiles);
        }

        return Arrays.asList(environment.getDefaultProfiles());
    }

    private String buildVersion() {
        return buildProperties
                .map(BuildProperties::getVersion)
                .filter(this::isConfigured)
                .orElse(DEFAULT_BUILD_VERSION);
    }

    private record DatabaseStatus(String status, String message) {
    }
}
