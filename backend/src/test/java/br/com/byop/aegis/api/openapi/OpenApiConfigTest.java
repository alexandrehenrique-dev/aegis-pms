package br.com.byop.aegis.api.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    void shouldExposeApiMetadataBearerJwtAndOauth2PkceSecuritySchemes() {

        OpenAPI openAPI = config.aegisOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Aegis PMS API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getServers()).singleElement()
                .satisfies(server -> assertThat(server.getUrl()).isEqualTo("http://localhost:8080"));
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKeys(OpenApiConfig.OAUTH2_PKCE_SCHEME, OpenApiConfig.BEARER_AUTH_SCHEME);
        SecurityScheme oauth2Pkce = openAPI.getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfig.OAUTH2_PKCE_SCHEME);
        assertThat(oauth2Pkce.getType()).isEqualTo(SecurityScheme.Type.OAUTH2);
        assertThat(oauth2Pkce.getFlows().getAuthorizationCode().getAuthorizationUrl())
                .isEqualTo("http://localhost:8282/realms/aegis/protocol/openid-connect/auth");
        assertThat(oauth2Pkce.getFlows().getAuthorizationCode().getTokenUrl())
                .isEqualTo("http://localhost:8282/realms/aegis/protocol/openid-connect/token");
        assertThat(oauth2Pkce.getFlows().getAuthorizationCode().getScopes())
                .containsKeys("openid", "profile", "email");
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
        assertThat(operation.getSecurity())
                .anySatisfy(requirement -> assertThat(requirement).containsKey(OpenApiConfig.OAUTH2_PKCE_SCHEME))
                .anySatisfy(requirement -> assertThat(requirement).containsKey(OpenApiConfig.BEARER_AUTH_SCHEME));
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

    @Test
    void shouldDocumentRemainingHttpMethodsAndSuccessCodes() throws NoSuchMethodException {

        Operation putOperation = customize(new ProductSampleController(), "putMethod");
        Operation patchOperation = customize(new ProductSampleController(), "patchMethod");
        Operation deleteOperation = customize(new ProductSampleController(), "deleteMethod");
        Operation createdOperation = customize(new ProductSampleController(), "createdMethod",
                new Operation().responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                        .addApiResponse("201", new io.swagger.v3.oas.models.responses.ApiResponse())));
        Operation noContentOperation = customize(new ProductSampleController(), "noContentMethod",
                new Operation().responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                        .addApiResponse("204", new io.swagger.v3.oas.models.responses.ApiResponse())));

        assertThat(putOperation.getSummary()).isEqualTo("Atualiza products");
        assertThat(patchOperation.getSummary()).isEqualTo("Atualiza parcialmente products");
        assertThat(deleteOperation.getSummary()).isEqualTo("Remove products");
        assertThat(createdOperation.getResponses()).containsKey("201");
        assertThat(noContentOperation.getResponses()).containsKey("204");
    }

    @Test
    void shouldDocumentFallbackMappingsAndPublicSubmissionSecurity() throws NoSuchMethodException {

        Operation fallbackOperation = customize(new PlainSampleController(), "plainMethod");
        Operation rootOperation = customize(new RootSampleController(), "rootMethod");
        Operation classOnlyOperation = customize(new ClassOnlySampleController(), "classOnlyMethod");
        Operation publicSubmissionOperation = customize(new ProductSampleController(), "publicSubmissionMethod");
        Method deprecatedMethod = PlainSampleController.class.getDeclaredMethod("deprecatedMethod");

        assertThat(fallbackOperation.getTags()).containsExactly(OpenApiConfig.TAG_SYSTEM);
        assertThat(fallbackOperation.getSummary()).isEqualTo("Executa plainMethod");
        assertThat(rootOperation.getSummary()).isEqualTo("Consulta rootMethod");
        assertThat(classOnlyOperation.getDescription()).contains("GET /api/v1/system");
        assertThat(publicSubmissionOperation.getSecurity()).isEmpty();
        assertThat((String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                OpenApiConfig.class,
                "firstMappingValue",
                deprecatedMethod.getAnnotation(Deprecated.class)
        )).isEmpty();
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
        return customize(controller, methodName, new Operation());
    }

    private Operation customize(Object controller, String methodName, Operation operation) throws NoSuchMethodException {
        OperationCustomizer customizer = config.documentedOperations();
        Method method = controller.getClass().getDeclaredMethod(methodName);
        HandlerMethod handlerMethod = new HandlerMethod(controller, method);
        return customizer.customize(operation, handlerMethod);
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

        @PostMapping(path = "/api/v1/products")
        void createdMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @PutMapping(path = "/api/v1/products")
        void putMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @PatchMapping(path = "/api/v1/products")
        void patchMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @DeleteMapping(path = "/api/v1/products")
        void deleteMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @PostMapping(path = "/api/v1/products/{productId}/forms/{formId}/submit")
        void publicSubmissionMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @DeleteMapping(path = "/api/v1/products")
        void noContentMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }
    }

    @RequestMapping
    private static class RootSampleController {

        @GetMapping
        void rootMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }
    }

    private static class PlainSampleController {

        void plainMethod() {
            // Metodo usado apenas para expor anotacoes Spring MVC ao HandlerMethod.
        }

        @Deprecated
        void deprecatedMethod() {
            // Metodo usado apenas para expor anotacao generica ao helper privado.
        }
    }

    @RequestMapping("/api/v1/system")
    private static class ClassOnlySampleController {

        @GetMapping
        void classOnlyMethod() {
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
