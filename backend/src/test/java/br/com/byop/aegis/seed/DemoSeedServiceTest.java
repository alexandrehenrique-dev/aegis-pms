package br.com.byop.aegis.seed;

import br.com.byop.aegis.identity.api.IdentityDemoUserService;
import br.com.byop.aegis.identity.api.IdentityUser;
import br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeReference;
import br.com.byop.aegis.knowledgegraph.api.KnowledgeGraphSeedService;
import br.com.byop.aegis.notification.api.NotificationOnboardingService;
import br.com.byop.aegis.product.api.ProductSeedReference;
import br.com.byop.aegis.product.api.ProductSeedService;
import br.com.byop.aegis.tenant.api.TenantSeedReference;
import br.com.byop.aegis.tenant.api.TenantSeedService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoSeedServiceTest {

    private static final String LOCAL_DEMO_USER_INITIAL_CREDENTIAL = "senha123";

    @Test
    void shouldRunOnlyWithLocalProfile() {
        Profile profile = DemoSeedService.class.getAnnotation(Profile.class);

        assertThat(profile.value()).containsExactly("local");
    }

    @Test
    void shouldSeedDemoCatalogThroughPublicPorts() {
        IdentityDemoUserService identity = mock(IdentityDemoUserService.class);
        TenantSeedService tenants = mock(TenantSeedService.class);
        ProductSeedService products = mock(ProductSeedService.class);
        KnowledgeGraphSeedService graph = mock(KnowledgeGraphSeedService.class);
        NotificationOnboardingService onboarding = mock(NotificationOnboardingService.class);
        stubUsers(identity);
        stubTenants(tenants);
        stubProducts(products);
        stubGraphNodes(graph);
        DemoSeedService service = new DemoSeedService(identity, tenants, products, graph, onboarding,
                LOCAL_DEMO_USER_INITIAL_CREDENTIAL);

        service.seed();

        verify(identity).ensureDemoUser("super-admin@byop.io", "Super Admin",
                LOCAL_DEMO_USER_INITIAL_CREDENTIAL, "SUPER_ADMIN");
        verify(tenants, atLeast(7)).ensureActiveMembership(any(UUID.class), anyString(), anyString());
        verify(products, atLeast(15)).ensureAssignment(any(UUID.class), anyString(), anyString());
        verify(graph, atLeast(9)).ensureNode(any());
        verify(graph, atLeast(7)).ensureEdge(any());
        verify(onboarding, times(5)).assignOnboarding(anyString());
    }

    private void stubUsers(IdentityDemoUserService identity) {
        when(identity.ensureDemoUser(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> new IdentityUser(
                        invocation.getArgument(0),
                        invocation.getArgument(0),
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        null
                ));
    }

    private void stubTenants(TenantSeedService tenants) {
        AtomicInteger index = new AtomicInteger();
        when(tenants.ensureTenant(any())).thenAnswer(invocation -> new TenantSeedReference(
                uuid(index.incrementAndGet()),
                invocation.getArgument(0, br.com.byop.aegis.tenant.api.TenantSeedCommand.class).key(),
                invocation.getArgument(0, br.com.byop.aegis.tenant.api.TenantSeedCommand.class).name()
        ));
    }

    private void stubProducts(ProductSeedService products) {
        AtomicInteger index = new AtomicInteger(100);
        when(products.ensureProduct(any())).thenAnswer(invocation -> new ProductSeedReference(
                uuid(index.incrementAndGet()),
                invocation.getArgument(0, br.com.byop.aegis.product.api.ProductSeedCommand.class).tenantId(),
                invocation.getArgument(0, br.com.byop.aegis.product.api.ProductSeedCommand.class).key(),
                invocation.getArgument(0, br.com.byop.aegis.product.api.ProductSeedCommand.class).name()
        ));
    }

    private void stubGraphNodes(KnowledgeGraphSeedService graph) {
        AtomicInteger index = new AtomicInteger(200);
        when(graph.ensureNode(any())).thenAnswer(invocation -> new GraphSeedNodeReference(
                uuid(index.incrementAndGet()),
                invocation.getArgument(0, br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeCommand.class).productId(),
                invocation.getArgument(0, br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeCommand.class).refType(),
                invocation.getArgument(0, br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeCommand.class).refId(),
                invocation.getArgument(0, br.com.byop.aegis.knowledgegraph.api.GraphSeedNodeCommand.class).label()
        ));
    }

    private UUID uuid(int value) {
        return new UUID(0L, value);
    }
}
