package br.com.byop.aegis.seed;

import br.com.byop.aegis.identity.api.IdentityDemoUserService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.knowledgegraph.api.GraphSeedEdgeCommand;
import br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeCommand;
import br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeReference;
import br.com.byop.aegis.knowledgegraph.api.KnowledgeGraphSeedService;
import br.com.byop.aegis.notification.api.NotificationOnboardingService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductSeedCommand;
import br.com.byop.aegis.product.api.ProductSeedReference;
import br.com.byop.aegis.product.api.ProductSeedService;
import br.com.byop.aegis.tenant.api.TenantSeedCommand;
import br.com.byop.aegis.tenant.api.TenantSeedReference;
import br.com.byop.aegis.tenant.api.TenantSeedService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Seed local idempotente para demonstracao do Aegis PMS.
 */
@Service
@Profile("local")
public class DemoSeedService {

    private static final String ACTIVE = "ACTIVE";
    private static final String SUSPENDED = "SUSPENDED";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    private static final String ROLE_PRODUCT_MANAGER = "PRODUCT_MANAGER";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_VIEWER = "VIEWER";
    private static final String USER_SUPER_ADMIN_SLUG = "super-admin";
    private static final String USER_ADMIN_SLUG = "admin";
    private static final String USER_PM_SLUG = "pm";
    private static final String USER_EDITOR_SLUG = "editor";
    private static final String USER_VIEWER_SLUG = "viewer";
    private static final String CLIENTES_BETA = "clientes-beta";
    private static final String AEGIS_LABS = "aegis-labs";
    private static final String CLIENTE_NORTE = "cliente-norte";
    private static final String PRODUCT_MAESTRO_BETON = "maestro-beton";
    private static final String PRODUCT_ALEXANDRE_DEV = "alexandre-dev";
    private static final String PRODUCT_CMSS = "cmss";
    private static final String PRODUCT_CONECTA_TALENTOS = "conecta-talentos";
    private static final String PRODUCT_AEGIS_CORE = "aegis-core";
    private static final String PRODUCT_AEGIS_DOCS = "aegis-docs";
    private static final String PRODUCT_PORTAL_NORTE = "portal-norte";
    private static final String WIKIDEV = "wikidev";
    private static final String LOKI = "loki";
    private static final String REF_PRODUCT = "PRODUCT";
    private static final String REF_CATEGORY = "CATEGORY";
    private static final String REF_TOPIC = "TOPIC";
    private static final String REF_ARTICLE = "ARTICLE";
    private static final String REF_POEM = "POEM";
    private static final String REF_MUSIC = "MUSIC_REFERENCE";
    private static final String REF_PLAYLIST = "PLAYLIST";
    private static final String NODE_PRODUCT = "PRODUCT";
    private static final String NODE_CATEGORY = "CATEGORY";
    private static final String NODE_TOPIC = "TOPIC";
    private static final String NODE_ARTICLE = "ARTICLE";
    private static final String NODE_POEM = "POEM";
    private static final String NODE_MUSIC = "MUSIC_REFERENCE";
    private static final String NODE_CONTENT = "CONTENT";
    private static final String EDGE_CONTAINS = "CONTAINS";
    private static final String EDGE_RELATED_TO = "RELATED_TO";
    private static final String EDGE_INSPIRED_BY = "INSPIRED_BY";
    private static final String EDGE_PART_OF = "PART_OF";
    private static final String METADATA_SEED = "{\"seed\":\"sprint-22\",\"status\":\"ativo\"}";

    private final IdentityDemoUserService identityDemoUserService;
    private final TenantSeedService tenantSeedService;
    private final ProductSeedService productSeedService;
    private final KnowledgeGraphSeedService knowledgeGraphSeedService;
    private final NotificationOnboardingService notificationOnboardingService;
    private final String localDemoUserInitialCredential;

    public DemoSeedService(IdentityDemoUserService identityDemoUserService, TenantSeedService tenantSeedService,
                           ProductSeedService productSeedService, KnowledgeGraphSeedService knowledgeGraphSeedService,
                           NotificationOnboardingService notificationOnboardingService,
                           @Value("${aegis.seed.demo-user-initial-credential}") String localDemoUserInitialCredential) {
        this.identityDemoUserService = identityDemoUserService;
        this.tenantSeedService = tenantSeedService;
        this.productSeedService = productSeedService;
        this.knowledgeGraphSeedService = knowledgeGraphSeedService;
        this.notificationOnboardingService = notificationOnboardingService;
        this.localDemoUserInitialCredential = localDemoUserInitialCredential;
    }

    public void seed() {
        Map<String, IdentityUser> users = seedUsers();
        Map<String, TenantSeedReference> tenants = seedTenants();
        Map<String, ProductSeedReference> products = seedProducts(tenants);
        seedMemberships(users, tenants);
        seedOnboarding(users);
        seedAssignments(users, products);
        seedKnowledgeGraph(products);
    }

    private Map<String, IdentityUser> seedUsers() {
        return Map.of(
                USER_SUPER_ADMIN_SLUG, identityDemoUserService.ensureDemoUser(
                        "super-admin@byop.io", "Super Admin", localDemoUserInitialCredential, ROLE_SUPER_ADMIN
                ),
                USER_ADMIN_SLUG, identityDemoUserService.ensureDemoUser("admin@byop.io", "Ana Martins",
                        localDemoUserInitialCredential, ROLE_TENANT_ADMIN),
                USER_PM_SLUG, identityDemoUserService.ensureDemoUser("pm@byop.io", "Marina Costa",
                        localDemoUserInitialCredential, ROLE_PRODUCT_MANAGER),
                USER_EDITOR_SLUG, identityDemoUserService.ensureDemoUser("editor@byop.io", "Rafael Lima",
                        localDemoUserInitialCredential, ROLE_EDITOR),
                USER_VIEWER_SLUG, identityDemoUserService.ensureDemoUser("viewer@byop.io", "Joao Alves",
                        localDemoUserInitialCredential, ROLE_VIEWER)
        );
    }

    private Map<String, TenantSeedReference> seedTenants() {
        return Map.of(
                CLIENTES_BETA, tenantSeedService.ensureTenant(new TenantSeedCommand(
                        CLIENTES_BETA, "CLIENTES BETA", "Pro", ACTIVE
                )),
                AEGIS_LABS, tenantSeedService.ensureTenant(new TenantSeedCommand(
                        AEGIS_LABS, "Aegis Labs", "Enterprise", ACTIVE
                )),
                CLIENTE_NORTE, tenantSeedService.ensureTenant(new TenantSeedCommand(
                        CLIENTE_NORTE, "Cliente Norte", "Starter", SUSPENDED
                ))
        );
    }

    private Map<String, ProductSeedReference> seedProducts(Map<String, TenantSeedReference> tenants) {
        TenantSeedReference clientesBeta = tenants.get(CLIENTES_BETA);
        TenantSeedReference aegisLabs = tenants.get(AEGIS_LABS);
        TenantSeedReference clienteNorte = tenants.get(CLIENTE_NORTE);
        return Map.of(
                PRODUCT_MAESTRO_BETON, productSeedService.ensureProduct(new ProductSeedCommand(
                        clientesBeta.id(), PRODUCT_MAESTRO_BETON, "Maestro Beton", "Site Institucional", ACTIVE,
                        Set.of(ModuleKey.CONTENT, ModuleKey.PAGES, ModuleKey.ASSETS, ModuleKey.FORMS,
                                ModuleKey.SEO, ModuleKey.ANALYTICS, ModuleKey.MUSIC)
                )),
                PRODUCT_CONECTA_TALENTOS, productSeedService.ensureProduct(new ProductSeedCommand(
                        clientesBeta.id(), PRODUCT_CONECTA_TALENTOS, "Conecta Talentos", "Portal", ACTIVE,
                        Set.of(ModuleKey.CONTENT, ModuleKey.PAGES, ModuleKey.FORMS, ModuleKey.SUBMISSIONS,
                                ModuleKey.JOBS, ModuleKey.SEO, ModuleKey.ANALYTICS, ModuleKey.INTEGRATIONS)
                )),
                PRODUCT_ALEXANDRE_DEV, productSeedService.ensureProduct(new ProductSeedCommand(
                        clientesBeta.id(), PRODUCT_ALEXANDRE_DEV, "Alexandre Dev", "Portfolio", ACTIVE,
                        Set.of(ModuleKey.PORTFOLIO, ModuleKey.CONTENT, ModuleKey.PAGES, ModuleKey.ASSETS,
                                ModuleKey.SEO, ModuleKey.ANALYTICS)
                )),
                PRODUCT_CMSS, productSeedService.ensureProduct(new ProductSeedCommand(
                        clientesBeta.id(), PRODUCT_CMSS, "CMSS", "Site Institucional", ACTIVE,
                        Set.of(ModuleKey.CONTENT, ModuleKey.PAGES, ModuleKey.ASSETS, ModuleKey.FORMS,
                                ModuleKey.SEO, ModuleKey.ANALYTICS)
                )),
                LOKI, productSeedService.ensureProduct(new ProductSeedCommand(
                        clientesBeta.id(), LOKI, "Loki", "Library/Books/Music", ACTIVE,
                        Set.of(ModuleKey.PAGES, ModuleKey.LIBRARY, ModuleKey.BOOKS, ModuleKey.MUSIC,
                                ModuleKey.CONTENT, ModuleKey.SEO, ModuleKey.ANALYTICS, ModuleKey.KNOWLEDGE_GRAPH)
                )),
                WIKIDEV, productSeedService.ensureProduct(new ProductSeedCommand(
                        clientesBeta.id(), WIKIDEV, "WikiDev", "Knowledge Base", ACTIVE,
                        Set.of(ModuleKey.KNOWLEDGE_BASE, ModuleKey.CONTENT, ModuleKey.COMMENTS,
                                ModuleKey.CONTRIBUTORS, ModuleKey.FORMS, ModuleKey.ANALYTICS,
                                ModuleKey.KNOWLEDGE_GRAPH)
                )),
                PRODUCT_AEGIS_CORE, productSeedService.ensureProduct(new ProductSeedCommand(
                        aegisLabs.id(), PRODUCT_AEGIS_CORE, "Aegis Core", "Produto SaaS", ACTIVE,
                        Set.of(ModuleKey.CONTENT, ModuleKey.ANALYTICS, ModuleKey.KNOWLEDGE_GRAPH)
                )),
                PRODUCT_AEGIS_DOCS, productSeedService.ensureProduct(new ProductSeedCommand(
                        aegisLabs.id(), PRODUCT_AEGIS_DOCS, "Aegis Docs", "Knowledge Base", ACTIVE,
                        Set.of(ModuleKey.CONTENT, ModuleKey.ANALYTICS, ModuleKey.KNOWLEDGE_GRAPH)
                )),
                PRODUCT_PORTAL_NORTE, productSeedService.ensureProduct(new ProductSeedCommand(
                        clienteNorte.id(), PRODUCT_PORTAL_NORTE, "Portal Norte", "Portal", ACTIVE,
                        Set.of(ModuleKey.CONTENT, ModuleKey.PAGES, ModuleKey.ANALYTICS)
                ))
        );
    }

    private void seedMemberships(Map<String, IdentityUser> users, Map<String, TenantSeedReference> tenants) {
        tenantSeedService.ensureActiveMembership(tenants.get(CLIENTES_BETA).id(), users.get(USER_SUPER_ADMIN_SLUG).id(), ROLE_SUPER_ADMIN);
        tenantSeedService.ensureActiveMembership(tenants.get(AEGIS_LABS).id(), users.get(USER_SUPER_ADMIN_SLUG).id(), ROLE_SUPER_ADMIN);
        tenantSeedService.ensureActiveMembership(tenants.get(CLIENTE_NORTE).id(), users.get(USER_SUPER_ADMIN_SLUG).id(), ROLE_SUPER_ADMIN);
        tenantSeedService.ensureActiveMembership(tenants.get(CLIENTES_BETA).id(), users.get(USER_ADMIN_SLUG).id(), ROLE_TENANT_ADMIN);
        tenantSeedService.ensureActiveMembership(tenants.get(CLIENTES_BETA).id(), users.get(USER_PM_SLUG).id(), ROLE_PRODUCT_MANAGER);
        tenantSeedService.ensureActiveMembership(tenants.get(CLIENTES_BETA).id(), users.get(USER_EDITOR_SLUG).id(), ROLE_EDITOR);
        tenantSeedService.ensureActiveMembership(tenants.get(CLIENTES_BETA).id(), users.get(USER_VIEWER_SLUG).id(), ROLE_VIEWER);
    }

    private void seedAssignments(Map<String, IdentityUser> users, Map<String, ProductSeedReference> products) {
        products.values().forEach(product -> productSeedService.ensureAssignment(
                product.id(), users.get(USER_SUPER_ADMIN_SLUG).id(), ROLE_PRODUCT_MANAGER
        ));
        ensureProductManagerAssignments(users.get(USER_PM_SLUG), products);
        productSeedService.ensureAssignment(products.get(PRODUCT_MAESTRO_BETON).id(), users.get(USER_EDITOR_SLUG).id(), ROLE_EDITOR);
        productSeedService.ensureAssignment(products.get(PRODUCT_MAESTRO_BETON).id(), users.get(USER_VIEWER_SLUG).id(), ROLE_VIEWER);
    }

    private void seedOnboarding(Map<String, IdentityUser> users) {
        users.values().forEach(user -> notificationOnboardingService.assignOnboarding(user.id()));
    }

    private void ensureProductManagerAssignments(IdentityUser user, Map<String, ProductSeedReference> products) {
        productSeedService.ensureAssignment(products.get(PRODUCT_MAESTRO_BETON).id(), user.id(), ROLE_PRODUCT_MANAGER);
        productSeedService.ensureAssignment(products.get(PRODUCT_ALEXANDRE_DEV).id(), user.id(), ROLE_PRODUCT_MANAGER);
        productSeedService.ensureAssignment(products.get(PRODUCT_CMSS).id(), user.id(), ROLE_PRODUCT_MANAGER);
        productSeedService.ensureAssignment(products.get(WIKIDEV).id(), user.id(), ROLE_PRODUCT_MANAGER);
    }

    private void seedKnowledgeGraph(Map<String, ProductSeedReference> products) {
        seedWikiDevGraph(products.get(WIKIDEV));
        seedLokiGraph(products.get(LOKI));
    }

    private void seedWikiDevGraph(ProductSeedReference product) {
        GraphSeedNodeReference wikiDev = ensureNode(product, NODE_PRODUCT, REF_PRODUCT, WIKIDEV, "WikiDev", 0, 0);
        GraphSeedNodeReference programacao = ensureNode(product, NODE_CATEGORY, REF_CATEGORY, "programacao", "Categoria Programacao", 200, 0);
        GraphSeedNodeReference java = ensureNode(product, NODE_TOPIC, REF_TOPIC, "java", "Topico Java", 400, 0);
        GraphSeedNodeReference springBoot = ensureNode(product, NODE_ARTICLE, REF_ARTICLE, "spring-boot", "Artigo Spring Boot", 600, -120);
        GraphSeedNodeReference jpa = ensureNode(product, NODE_ARTICLE, REF_ARTICLE, "jpa", "Artigo JPA", 600, 120);

        ensureEdge(product, wikiDev, programacao, EDGE_CONTAINS);
        ensureEdge(product, programacao, java, EDGE_CONTAINS);
        ensureEdge(product, java, springBoot, EDGE_CONTAINS);
        ensureEdge(product, springBoot, jpa, EDGE_RELATED_TO);
    }

    private void seedLokiGraph(ProductSeedReference product) {
        GraphSeedNodeReference loki = ensureNode(product, NODE_PRODUCT, REF_PRODUCT, LOKI, "Loki", 0, 0);
        GraphSeedNodeReference poema = ensureNode(product, NODE_POEM, REF_POEM, "poema-do-limiar", "Poema do Limiar", 180, -120);
        GraphSeedNodeReference musica = ensureNode(product, NODE_MUSIC, REF_MUSIC, "musica-ecos", "Musica Ecos", 360, 0);
        GraphSeedNodeReference playlist = ensureNode(product, NODE_CONTENT, REF_PLAYLIST, "playlist-atravessias", "Playlist Atravessias", 540, 120);

        ensureEdge(product, loki, poema, EDGE_CONTAINS);
        ensureEdge(product, poema, musica, EDGE_INSPIRED_BY);
        ensureEdge(product, musica, playlist, EDGE_PART_OF);
    }

    private GraphSeedNodeReference ensureNode(ProductSeedReference product, String nodeType, String refType,
                                              String refId, String label, double x, double y) {
        return knowledgeGraphSeedService.ensureNode(new GraphSeedNodeCommand(
                product.id(),
                nodeType,
                refType,
                refId,
                label,
                refId,
                METADATA_SEED,
                x,
                y
        ));
    }

    private void ensureEdge(ProductSeedReference product, GraphSeedNodeReference source,
                            GraphSeedNodeReference target, String edgeType) {
        knowledgeGraphSeedService.ensureEdge(new GraphSeedEdgeCommand(
                product.id(),
                source.id(),
                target.id(),
                edgeType
        ));
    }
}
