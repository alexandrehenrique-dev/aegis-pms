package br.com.byop.aegis.system.service;

import br.com.byop.aegis.system.dto.SystemStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemStatusServiceTest {

    @Test
    void shouldReturnUpStatusWhenDependenciesAreAvailable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = mock(Environment.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(1);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", ""))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getProperty("keycloak.issuer-uri", "http://localhost:8282/realms/aegis"))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(applicationContext.containsBean("knowledgeGraphService")).thenReturn(true);

        SystemStatusService service = new SystemStatusService(
                jdbcTemplate,
                environment,
                applicationContext,
                Optional.of(buildProperties("1.2.3"))
        );

        SystemStatusResponse response = service.currentStatus();

        assertThat(response.applicationStatus()).isEqualTo("UP");
        assertThat(response.databaseStatus()).isEqualTo("UP");
        assertThat(response.databaseMessage()).isEqualTo("Banco conectado.");
        assertThat(response.keycloakIssuerConfigured()).isTrue();
        assertThat(response.keycloakIssuer()).isEqualTo("http://localhost:8282/realms/aegis");
        assertThat(response.knowledgeGraphStatus()).isEqualTo("UP");
        assertThat(response.activeProfiles()).containsExactly("local");
        assertThat(response.buildVersion()).isEqualTo("1.2.3");
    }

    @Test
    void shouldReturnDownDatabaseStatusWhenProbeIsUnexpected() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = mock(Environment.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(0);
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", ""))
                .thenReturn("");
        when(environment.getProperty("keycloak.issuer-uri", ""))
                .thenReturn("");
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        when(environment.getDefaultProfiles()).thenReturn(new String[]{"default"});
        when(applicationContext.containsBean("knowledgeGraphService")).thenReturn(false);

        SystemStatusService service = new SystemStatusService(
                jdbcTemplate,
                environment,
                applicationContext,
                Optional.empty()
        );

        SystemStatusResponse response = service.currentStatus();

        assertThat(response.databaseStatus()).isEqualTo("DOWN");
        assertThat(response.databaseMessage()).isEqualTo("Banco indisponivel.");
        assertThat(response.keycloakIssuerConfigured()).isFalse();
        assertThat(response.knowledgeGraphStatus()).isEqualTo("DOWN");
        assertThat(response.activeProfiles()).containsExactly("default");
        assertThat(response.buildVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    void shouldReturnReadableDatabaseMessageWhenConnectionFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = mock(Environment.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jdbcTemplate.queryForObject("select 1", Integer.class))
                .thenThrow(new RuntimeException("Conexao recusada"));
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", ""))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getProperty("keycloak.issuer-uri", "http://localhost:8282/realms/aegis"))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(applicationContext.containsBean("knowledgeGraphService")).thenReturn(true);

        SystemStatusService service = new SystemStatusService(
                jdbcTemplate,
                environment,
                applicationContext,
                Optional.empty()
        );

        SystemStatusResponse response = service.currentStatus();

        assertThat(response.databaseStatus()).isEqualTo("DOWN");
        assertThat(response.databaseMessage()).isEqualTo("Conexao recusada");
    }

    @Test
    void shouldReturnDefaultDatabaseMessageWhenFailureHasNoMessage() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = mock(Environment.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenThrow(new RuntimeException());
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", ""))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getProperty("keycloak.issuer-uri", "http://localhost:8282/realms/aegis"))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(applicationContext.containsBean("knowledgeGraphService")).thenReturn(true);

        SystemStatusService service = new SystemStatusService(
                jdbcTemplate,
                environment,
                applicationContext,
                Optional.empty()
        );

        SystemStatusResponse response = service.currentStatus();

        assertThat(response.databaseStatus()).isEqualTo("DOWN");
        assertThat(response.databaseMessage()).isEqualTo("Banco indisponivel.");
    }

    @Test
    void shouldReturnDefaultDatabaseMessageWhenFailureMessageIsBlank() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = mock(Environment.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenThrow(new RuntimeException(" "));
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", ""))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getProperty("keycloak.issuer-uri", "http://localhost:8282/realms/aegis"))
                .thenReturn("http://localhost:8282/realms/aegis");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(applicationContext.containsBean("knowledgeGraphService")).thenReturn(true);

        SystemStatusService service = new SystemStatusService(
                jdbcTemplate,
                environment,
                applicationContext,
                Optional.empty()
        );

        SystemStatusResponse response = service.currentStatus();

        assertThat(response.databaseStatus()).isEqualTo("DOWN");
        assertThat(response.databaseMessage()).isEqualTo("Banco indisponivel.");
    }

    @Test
    void shouldHandleSqlDatabaseFailureAndNullIssuer() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Environment environment = mock(Environment.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(jdbcTemplate.queryForObject("select 1", Integer.class))
                .thenThrow(new RuntimeException(new SQLException("Pool fechado")));
        when(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", ""))
                .thenReturn(null);
        when(environment.getProperty("keycloak.issuer-uri", (String) null))
                .thenReturn(null);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        when(applicationContext.containsBean("knowledgeGraphService")).thenReturn(true);

        SystemStatusService service = new SystemStatusService(
                jdbcTemplate,
                environment,
                applicationContext,
                Optional.of(buildProperties(" "))
        );

        SystemStatusResponse response = service.currentStatus();

        assertThat(response.databaseStatus()).isEqualTo("DOWN");
        assertThat(response.databaseMessage()).isEqualTo("java.sql.SQLException: Pool fechado");
        assertThat(response.keycloakIssuerConfigured()).isFalse();
        assertThat(response.keycloakIssuer()).isNull();
        assertThat(response.buildVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    private BuildProperties buildProperties(String version) {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        return new BuildProperties(properties);
    }
}
