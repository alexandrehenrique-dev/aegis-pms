package br.com.byop.aegis.api.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void shouldCreateOpenApiBeansInLocalProfile() {

        try (AnnotationConfigApplicationContext context = contextForProfile("local")) {
            assertThat(context.containsBean("aegisOpenApi")).isTrue();
            assertThat(context.containsBean("documentedOperations")).isTrue();
        }
    }

    @Test
    void shouldCreateOpenApiBeansInDevProfile() {

        try (AnnotationConfigApplicationContext context = contextForProfile("dev")) {
            assertThat(context.containsBean("aegisOpenApi")).isTrue();
            assertThat(context.containsBean("documentedOperations")).isTrue();
        }
    }

    @Test
    void shouldNotCreateOpenApiBeansInProdProfile() {

        try (AnnotationConfigApplicationContext context = contextForProfile("prod")) {
            assertThat(context.containsBean("aegisOpenApi")).isFalse();
            assertThat(context.containsBean("documentedOperations")).isFalse();
        }
    }

    @Test
    void shouldKeepSpringdocEnabledOnlyInLocalAndDevResources() {

        assertSpringdocEnabled("application-local.yml", true);
        assertSpringdocEnabled("application-dev.yml", true);
        assertSpringdocEnabled("application-prod.yml", false);
    }

    @Test
    void shouldExposeApiMetadataAndBearerJwtSecurityScheme() {

        OpenAPI openAPI = config.aegisOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Aegis PMS API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getServers()).singleElement()
                .satisfies(server -> assertThat(server.getUrl()).isEqualTo("http://localhost:8080"));
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.BEARER_AUTH_SCHEME);
        assertThat(openAPI.getTags())
                .extracting(io.swagger.v3.oas.models.tags.Tag::getName)
                .containsExactlyElementsOf(OpenApiConfig.orderedTagNames());
    }

    @Test
    void shouldDocumentProtectedEndpointWithTagSummaryResponsesAndSecurity() throws NoSuchMethodException {

        Operation operation = customize(new ProductSampleController(), "productMethod");

        assertThat(operation.getTags()).containsExactly(OpenApiConfig.TAG_PRODUCTS);
        assertThat(operation.getSummary()).isEqualTo("Consulta products");
        assertThat(operation.getDescription())
                .contains("GET /api/v1/products")
                .contains("contratos REST em /api/v1");
        assertThat(operation.getResponses()).containsKeys("200", "400", "401", "403", "404");
        assertThat(operation.getSecurity()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey(OpenApiConfig.BEARER_AUTH_SCHEME));
    }

    @Test
    void shouldNotAddOperationSecurityToAuthEndpoint() throws NoSuchMethodException {

        Operation operation = customize(new AuthSampleController(), "authMethod");

        assertThat(operation.getTags()).containsExactly(OpenApiConfig.TAG_AUTH);
        assertThat(operation.getSecurity()).isEmpty();
        assertThat(operation.getResponses()).containsKeys("200", "400", "401", "403", "404");
    }

    @Test
    void shouldClassifyNestedSubmissionEndpointBeforeForms() throws NoSuchMethodException {

        Operation operation = customize(new ProductSampleController(), "submissionMethod");

        assertThat(operation.getTags()).containsExactly(OpenApiConfig.TAG_SUBMISSIONS);
        assertThat(operation.getSummary()).isEqualTo("Consulta submissions");
    }

    @Test
    void shouldClassifyFeedbackEndpoint() throws NoSuchMethodException {

        Operation operation = customize(new FeedbackSampleController(), "feedbackMethod");
        Operation tenantOperation = customize(new FeedbackSampleController(), "tenantFeedbackMethod");

        assertThat(operation.getTags()).containsExactly(OpenApiConfig.TAG_FEEDBACK);
        assertThat(operation.getSummary()).isEqualTo("Consulta feedback");
        assertThat(tenantOperation.getTags()).containsExactly(OpenApiConfig.TAG_FEEDBACK);
    }

    private AnnotationConfigApplicationContext contextForProfile(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.register(OpenApiConfig.class);
        context.refresh();
        return context;
    }

    private void assertSpringdocEnabled(String resourceName, boolean expectedValue) {
        Properties properties = yamlProperties(resourceName);

        assertThat(properties).containsEntry("springdoc.api-docs.enabled", expectedValue);
        assertThat(properties).containsEntry("springdoc.swagger-ui.enabled", expectedValue);
    }

    private Properties yamlProperties(String resourceName) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(resourceName));
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    private Operation customize(Object controller, String methodName) throws NoSuchMethodException {
        OperationCustomizer customizer = config.documentedOperations();
        Method method = controller.getClass().getDeclaredMethod(methodName);
        HandlerMethod handlerMethod = new HandlerMethod(controller, method);
        return customizer.customize(new Operation(), handlerMethod);
    }

    private static class ProductSampleController {

        @GetMapping("/api/v1/products")
        void productMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @GetMapping("/api/v1/products/{productId}/forms/{formId}/submissions")
        void submissionMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }
    }

    private static class FeedbackSampleController {

        @GetMapping("/api/v1/feedback")
        void feedbackMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @GetMapping("/api/v1/tenants/{tenantId}/feedback")
        void tenantFeedbackMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }
    }

    @RequestMapping("/api/v1/auth")
    private static class AuthSampleController {

        @PostMapping("/login")
        void authMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }
    }
}
