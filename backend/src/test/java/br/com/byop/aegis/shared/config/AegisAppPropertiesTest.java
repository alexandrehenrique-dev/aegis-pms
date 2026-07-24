package br.com.byop.aegis.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AegisAppPropertiesTest {

    private static final String PROD_ORIGIN = "https://aegis.byop.dev";

    @Test
    void shouldBindProductionYamlAsImmutableList() {
        StandardEnvironment environment = environmentWithApplicationDefaults();
        environment.getPropertySources().addFirst(
                new PropertiesPropertySource("application-prod.yml", yamlProperties("application-prod.yml"))
        );

        AegisAppProperties properties = bind(environment);

        assertThat(properties.baseUrl()).isEqualTo(PROD_ORIGIN);
        assertThat(properties.corsAllowedOrigins()).containsExactly(PROD_ORIGIN);
        List<String> origins = properties.corsAllowedOrigins();
        assertThatThrownBy(() -> origins.add("https://other.example"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldBindCommaSeparatedEnvironmentVariableAsMultipleOrigins() {
        StandardEnvironment environment = environmentWithApplicationDefaults();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "testEnvironment",
                Map.of(
                        "AEGIS_APP_BASE_URL", PROD_ORIGIN,
                        "AEGIS_APP_CORS_ALLOWED_ORIGINS", PROD_ORIGIN + ",https://other.example"
                )
        ));

        AegisAppProperties properties = bind(environment);

        assertThat(properties.baseUrl()).isEqualTo(PROD_ORIGIN);
        assertThat(properties.corsAllowedOrigins())
                .containsExactly(PROD_ORIGIN, "https://other.example");
    }

    @Test
    void shouldUseSafeLocalDefaults() {
        AegisAppProperties properties = bind(environmentWithApplicationDefaults());

        assertThat(properties.baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(properties.corsAllowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void shouldRejectWildcardOriginDuringConfigurationBinding() {
        new ApplicationContextRunner()
                .withUserConfiguration(AegisApplicationConfig.class)
                .withPropertyValues(
                        "aegis.app.base-url=https://aegis.byop.dev",
                        "aegis.app.cors-allowed-origins=*"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("corsAllowedOrigins[0]")
                            .hasStackTraceContaining("sem wildcard");
                });
    }

    private StandardEnvironment environmentWithApplicationDefaults() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addLast(
                new PropertiesPropertySource("application.yml", yamlProperties("application.yml"))
        );
        return environment;
    }

    private AegisAppProperties bind(StandardEnvironment environment) {
        return Binder.get(environment)
                .bind("aegis.app", Bindable.of(AegisAppProperties.class))
                .orElseThrow(() -> new IllegalStateException("A configuração aegis.app não foi carregada."));
    }

    private Properties yamlProperties(String resourceName) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
