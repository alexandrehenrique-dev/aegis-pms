package br.com.byop.aegis.api.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuração OpenAPI disponível apenas em perfis operacionais não produtivos.
 */
@Configuration
@Profile({"local", "dev"})
public class OpenApiConfig {

    static final String BEARER_AUTH_SCHEME = "bearer-jwt";
    static final String OAUTH2_PKCE_SCHEME = "oauth2-pkce";
    static final String TAG_SYSTEM = "System";
    static final String TAG_AUTH = "Auth";
    static final String TAG_TENANTS = "Tenants";
    static final String TAG_PRODUCTS = "Products";
    static final String TAG_PRODUCT_MODULES = "Product Modules";
    static final String TAG_PRODUCT_ASSIGNMENTS = "Product Assignments";
    static final String TAG_KNOWLEDGE_GRAPH = "Knowledge Graph";
    static final String TAG_CONTENT = "Content";
    static final String TAG_PAGES = "Pages";
    static final String TAG_ASSETS = "Assets";
    static final String TAG_FORMS = "Forms";
    static final String TAG_SUBMISSIONS = "Submissions";
    static final String TAG_ANALYTICS = "Analytics";
    static final String TAG_USERS = "Users";
    static final String TAG_AUDIT = "Audit";
    static final String TAG_SETTINGS = "Settings";
    static final String TAG_DASHBOARD = "Dashboard";
    static final String TAG_NOTIFICATIONS = "Notifications";
    static final String TAG_FEEDBACK = "Feedback";

    private static final String API_VERSION = "v1";
    private static final String API_TITLE = "Aegis PMS API";
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";
    private static final String DEFAULT_KEYCLOAK_ISSUER_URI = "http://localhost:8282/realms/aegis";
    private static final String OPENID_SCOPE = "openid";
    private static final String PROFILE_SCOPE = "profile";
    private static final String EMAIL_SCOPE = "email";
    private static final String HTTP_STATUS_OK = "200";
    private static final String HTTP_STATUS_CREATED = "201";
    private static final String HTTP_STATUS_NO_CONTENT = "204";
    private static final String HTTP_STATUS_BAD_REQUEST = "400";
    private static final String HTTP_STATUS_UNAUTHORIZED = "401";
    private static final String HTTP_STATUS_FORBIDDEN = "403";
    private static final String HTTP_STATUS_NOT_FOUND = "404";

    private static final List<String> ORDERED_TAGS = List.of(
            TAG_SYSTEM,
            TAG_AUTH,
            TAG_TENANTS,
            TAG_PRODUCTS,
            TAG_PRODUCT_MODULES,
            TAG_PRODUCT_ASSIGNMENTS,
            TAG_KNOWLEDGE_GRAPH,
            TAG_CONTENT,
            TAG_PAGES,
            TAG_ASSETS,
            TAG_FORMS,
            TAG_SUBMISSIONS,
            TAG_ANALYTICS,
            TAG_USERS,
            TAG_AUDIT,
            TAG_SETTINGS,
            TAG_DASHBOARD,
            TAG_NOTIFICATIONS,
            TAG_FEEDBACK
    );

    private static final Map<String, String> TAG_DESCRIPTIONS = Map.ofEntries(
            Map.entry(TAG_SYSTEM, "Health, readiness e informações operacionais do backend."),
            Map.entry(TAG_AUTH, "Proxy de autenticação, refresh, logout e recuperação de senha."),
            Map.entry(TAG_TENANTS, "Criação, listagem, atualização e remoção de tenants."),
            Map.entry(TAG_PRODUCTS, "Criação, listagem e consulta de produtos digitais."),
            Map.entry(TAG_PRODUCT_MODULES, "Habilitação e desabilitação de módulos por produto."),
            Map.entry(TAG_PRODUCT_ASSIGNMENTS, "Atribuição de usuários e convites por produto."),
            Map.entry(TAG_KNOWLEDGE_GRAPH, "Nós, relações, órfãos, posição e preview do grafo de conhecimento."),
            Map.entry(TAG_CONTENT, "Conteúdos editoriais, workflow, publicação e versões."),
            Map.entry(TAG_PAGES, "Páginas, seções, eventos e entidades globais de produto."),
            Map.entry(TAG_ASSETS, "Biblioteca de assets, tags, upload, resolução e download."),
            Map.entry(TAG_FORMS, "Definições de formulários, catálogo de campos, publicação e entrega."),
            Map.entry(TAG_SUBMISSIONS, "Envio público e consulta administrativa de submissões."),
            Map.entry(TAG_ANALYTICS, "KPIs, saúde, canais, tendências e relatórios."),
            Map.entry(TAG_USERS, "Usuários de tenant, convites, bloqueio, remoção e restauração."),
            Map.entry(TAG_AUDIT, "Consulta da trilha de auditoria por tenant."),
            Map.entry(TAG_SETTINGS, "Configurações de produto, roles, permissões e segurança."),
            Map.entry(TAG_DASHBOARD, "Resumo agregado do dashboard operacional."),
            Map.entry(TAG_NOTIFICATIONS, "Notificações internas, onboarding e leitura por usuário."),
            Map.entry(TAG_FEEDBACK, "Reporte de problemas, sugestões e gestão de status de feedbacks.")
    );

    private static final List<Map.Entry<String, String>> TAG_BY_PATH_FRAGMENT = List.of(
            Map.entry("/api/v1/system", TAG_SYSTEM),
            Map.entry("/api/v1/auth", TAG_AUTH),
            Map.entry("/api/v1/tenants/{tenantId}/feedback", TAG_FEEDBACK),
            Map.entry("/api/v1/tenants/{tenantId}/users", TAG_USERS),
            Map.entry("/api/v1/tenants/{tenantId}/audit-events", TAG_AUDIT),
            Map.entry("/api/v1/tenants/{tenantId}/roles", TAG_SETTINGS),
            Map.entry("/api/v1/tenants/{tenantId}/permission-matrix", TAG_SETTINGS),
            Map.entry("/api/v1/tenants", TAG_TENANTS),
            Map.entry("/api/v1/products/{productId}/modules", TAG_PRODUCT_MODULES),
            Map.entry("/api/v1/products/{productId}/users", TAG_PRODUCT_ASSIGNMENTS),
            Map.entry("/api/v1/products/{productId}/graph", TAG_KNOWLEDGE_GRAPH),
            Map.entry("/api/v1/products/{productId}/content", TAG_CONTENT),
            Map.entry("/api/v1/products/{productId}/pages", TAG_PAGES),
            Map.entry("/api/v1/products/{productId}/events", TAG_PAGES),
            Map.entry("/api/v1/products/{productId}/globals", TAG_PAGES),
            Map.entry("/api/v1/products/{productId}/assets", TAG_ASSETS),
            Map.entry("/api/v1/products/{productId}/asset-tags", TAG_ASSETS),
            Map.entry("/api/v1/assets", TAG_ASSETS),
            Map.entry("/api/v1/products/{productId}/forms/{formId}/submissions", TAG_SUBMISSIONS),
            Map.entry("/api/v1/products/{productId}/forms/submissions", TAG_SUBMISSIONS),
            Map.entry("/api/v1/products/{productId}/forms", TAG_FORMS),
            Map.entry("/api/v1/products/{productId}/analytics", TAG_ANALYTICS),
            Map.entry("/api/v1/products/{productId}/settings", TAG_SETTINGS),
            Map.entry("/api/v1/products", TAG_PRODUCTS),
            Map.entry("/api/v1/users", TAG_USERS),
            Map.entry("/api/v1/dashboard", TAG_DASHBOARD),
            Map.entry("/api/v1/notifications", TAG_NOTIFICATIONS),
            Map.entry("/api/v1/feedback", TAG_FEEDBACK)
    );

    @Value("${keycloak.issuer-uri:" + DEFAULT_KEYCLOAK_ISSUER_URI + "}")
    private String keycloakIssuerUri = DEFAULT_KEYCLOAK_ISSUER_URI;

    /**
     * Metadados canônicos da API REST administrativa.
     *
     * @return especificação OpenAPI base
     */
    @Bean
    public OpenAPI aegisOpenApi() {
        SecurityScheme bearerJwt = new SecurityScheme()
                .name(BEARER_AUTH_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Cole um access token JWT emitido pelo Keycloak usando o formato Bearer.");
        SecurityScheme oauth2Pkce = new SecurityScheme()
                .name(OAUTH2_PKCE_SCHEME)
                .type(SecurityScheme.Type.OAUTH2)
                .description("Fluxo Authorization Code com PKCE para autenticar no Keycloak pelo Swagger UI.")
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl(keycloakIssuerUri + "/protocol/openid-connect/auth")
                                .tokenUrl(keycloakIssuerUri + "/protocol/openid-connect/token")
                                .scopes(new Scopes()
                                        .addString(OPENID_SCOPE, "OpenID Connect")
                                        .addString(PROFILE_SCOPE, "Perfil do usuário")
                                        .addString(EMAIL_SCOPE, "E-mail do usuário"))));

        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .version(API_VERSION)
                        .description("API REST administrativa do Aegis PMS para operar tenants, produtos, "
                                + "conteúdo, páginas, assets, formulários, usuários, auditoria e configurações. "
                                + "No Swagger UI local, prefira o scheme oauth2-pkce para login via Keycloak; "
                                + "bearer-jwt fica disponível para colar tokens manualmente."))
                .servers(List.of(new Server()
                        .url(LOCAL_SERVER_URL)
                        .description("Servidor local de desenvolvimento")))
                .components(new Components()
                        .addSecuritySchemes(OAUTH2_PKCE_SCHEME, oauth2Pkce)
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, bearerJwt))
                .security(List.of(
                        new SecurityRequirement().addList(OAUTH2_PKCE_SCHEME,
                                List.of(OPENID_SCOPE, PROFILE_SCOPE, EMAIL_SCOPE)),
                        new SecurityRequirement().addList(BEARER_AUTH_SCHEME)))
                .tags(orderedTags());
    }

    /**
     * Complementa a documentação gerada pelo Springdoc para manter todos os endpoints
     * com summary, description, responses, tags e exemplos de erro padronizados.
     *
     * @return customizador aplicado a cada operação descoberta
     */
    @Bean
    public OperationCustomizer documentedOperations() {
        return (operation, handlerMethod) -> {
            String path = mappingPath(handlerMethod);
            String httpMethod = mappingHttpMethod(handlerMethod);
            String tag = tagFor(path);
            operation.setTags(List.of(tag));
            operation.setSummary(summaryFor(httpMethod, path, handlerMethod));
            operation.setDescription(descriptionFor(httpMethod, path, tag));
            operation.setResponses(responsesFor(operation.getResponses()));
            if (requiresBearerToken(path, tag)) {
                operation.addSecurityItem(new SecurityRequirement().addList(OAUTH2_PKCE_SCHEME,
                        List.of(OPENID_SCOPE, PROFILE_SCOPE, EMAIL_SCOPE)));
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
            } else {
                operation.setSecurity(List.of());
            }
            return operation;
        };
    }

    private static List<Tag> orderedTags() {
        return ORDERED_TAGS.stream()
                .map(name -> new Tag().name(name).description(TAG_DESCRIPTIONS.get(name)))
                .toList();
    }

    private static String mappingPath(HandlerMethod handlerMethod) {
        String classPrefix = firstMappingValue(handlerMethod.getBeanType().getAnnotation(RequestMapping.class));
        Method method = handlerMethod.getMethod();
        String methodPath = firstMappingValue(method.getAnnotation(GetMapping.class));
        if (methodPath.isBlank()) {
            methodPath = firstMappingValue(method.getAnnotation(PostMapping.class));
        }
        if (methodPath.isBlank()) {
            methodPath = firstMappingValue(method.getAnnotation(PutMapping.class));
        }
        if (methodPath.isBlank()) {
            methodPath = firstMappingValue(method.getAnnotation(PatchMapping.class));
        }
        if (methodPath.isBlank()) {
            methodPath = firstMappingValue(method.getAnnotation(DeleteMapping.class));
        }
        return normalizePath(classPrefix, methodPath);
    }

    private static String mappingHttpMethod(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        if (method.isAnnotationPresent(GetMapping.class)) {
            return "GET";
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return "POST";
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return "PUT";
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return "PATCH";
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE";
        }
        return "HTTP";
    }

    private static String firstMappingValue(Annotation annotation) {
        if (annotation == null) {
            return "";
        }
        if (annotation instanceof RequestMapping requestMapping) {
            return firstValue(requestMapping.value(), requestMapping.path());
        }
        if (annotation instanceof GetMapping mapping) {
            return firstValue(mapping.value(), mapping.path());
        }
        if (annotation instanceof PostMapping mapping) {
            return firstValue(mapping.value(), mapping.path());
        }
        if (annotation instanceof PutMapping mapping) {
            return firstValue(mapping.value(), mapping.path());
        }
        if (annotation instanceof PatchMapping mapping) {
            return firstValue(mapping.value(), mapping.path());
        }
        if (annotation instanceof DeleteMapping mapping) {
            return firstValue(mapping.value(), mapping.path());
        }
        return "";
    }

    private static String firstValue(String[] values, String[] paths) {
        if (values.length > 0) {
            return values[0];
        }
        if (paths.length > 0) {
            return paths[0];
        }
        return "";
    }

    private static String normalizePath(String classPrefix, String methodPath) {
        if (classPrefix.isBlank()) {
            return methodPath;
        }
        if (methodPath.isBlank()) {
            return classPrefix;
        }
        return (classPrefix + "/" + methodPath).replaceAll("/{2,}", "/");
    }

    private static String tagFor(String path) {
        return TAG_BY_PATH_FRAGMENT.stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(TAG_SYSTEM);
    }

    private static boolean requiresBearerToken(String path, String tag) {
        return !TAG_AUTH.equals(tag) && !TAG_SYSTEM.equals(tag) && !path.endsWith("/submit");
    }

    private static String summaryFor(String httpMethod, String path, HandlerMethod handlerMethod) {
        return switch (httpMethod) {
            case "GET" -> "Consulta " + resourceName(path, handlerMethod);
            case "POST" -> "Executa criação ou ação em " + resourceName(path, handlerMethod);
            case "PUT" -> "Atualiza " + resourceName(path, handlerMethod);
            case "PATCH" -> "Atualiza parcialmente " + resourceName(path, handlerMethod);
            case "DELETE" -> "Remove " + resourceName(path, handlerMethod);
            default -> "Executa " + handlerMethod.getMethod().getName();
        };
    }

    private static String descriptionFor(String httpMethod, String path, String tag) {
        return httpMethod + " " + path + ". " + TAG_DESCRIPTIONS.get(tag)
                + " Respeita autenticação Bearer JWT, isolamento multi-tenant e contratos REST em /api/v1.";
    }

    private static String resourceName(String path, HandlerMethod handlerMethod) {
        String[] parts = path.split("/");
        List<String> usefulParts = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank() && !part.startsWith("{") && !"api".equals(part) && !"v1".equals(part)) {
                usefulParts.add(part.replace('-', ' '));
            }
        }
        if (usefulParts.isEmpty()) {
            return handlerMethod.getMethod().getName();
        }
        return usefulParts.getLast().toLowerCase();
    }

    private static ApiResponses responsesFor(ApiResponses existingResponses) {
        ApiResponses responses = existingResponses == null ? new ApiResponses() : existingResponses;
        String successCode = successCodeFor(responses);
        responses.addApiResponse(successCode, new ApiResponse().description(successDescriptionFor(successCode)));
        responses.addApiResponse(HTTP_STATUS_BAD_REQUEST,
                errorResponse("Requisição inválida ou regra de negócio rejeitada.", "VALIDATION_ERROR"));
        responses.addApiResponse(HTTP_STATUS_UNAUTHORIZED,
                errorResponse("Token ausente, inválido ou expirado.", "UNAUTHORIZED"));
        responses.addApiResponse(HTTP_STATUS_FORBIDDEN,
                errorResponse("Usuário autenticado sem permissão ou módulo desabilitado.", "FORBIDDEN"));
        responses.addApiResponse(HTTP_STATUS_NOT_FOUND,
                errorResponse("Recurso não encontrado no escopo do usuário autenticado.", "NOT_FOUND"));
        return responses;
    }

    private static String successCodeFor(ApiResponses responses) {
        if (responses.containsKey(HTTP_STATUS_CREATED)) {
            return HTTP_STATUS_CREATED;
        }
        if (responses.containsKey(HTTP_STATUS_NO_CONTENT)) {
            return HTTP_STATUS_NO_CONTENT;
        }
        return HTTP_STATUS_OK;
    }

    private static String successDescriptionFor(String code) {
        return switch (code) {
            case HTTP_STATUS_CREATED -> "Recurso criado com sucesso.";
            case HTTP_STATUS_NO_CONTENT -> "Operação concluída sem corpo de resposta.";
            default -> "Operação concluída com sucesso.";
        };
    }

    static List<String> orderedTagNames() {
        return ORDERED_TAGS;
    }

    private static ApiResponse errorResponse(String description, String errorCode) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().addExamples("erro", new Example().value(Map.of(
                                "error", errorCode,
                                "message", description
                        )))));
    }
}
