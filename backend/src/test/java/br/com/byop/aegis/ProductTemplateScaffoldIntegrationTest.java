package br.com.byop.aegis;

import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.pages.repository.PageRepository;
import br.com.byop.aegis.pages.repository.PageSectionRepository;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.command.CreateProductCommand;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.dto.ProductSummary;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.service.ProductService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.tenant.command.CreateTenantCommand;
import br.com.byop.aegis.tenant.dto.TenantSummary;
import br.com.byop.aegis.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ponta a ponta (Spring context real + Postgres real, Etapa 26): {@code
 * ProductService.createProduct} cria o produto, habilita os modulos
 * recomendados e — via {@code ProductCreatedEvent} + {@code
 * ProductPageScaffoldService}, ambos reais — cria o esqueleto de paginas/secoes
 * dentro da mesma transacao, sem tocar HTTP/seguranca (sem convencao
 * estabelecida de teste MockMvc autenticado neste repositorio ainda).
 */
@SpringBootTest
class ProductTemplateScaffoldIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductModuleRepository moduleRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PageSectionRepository sectionRepository;

    @Test
    void shouldScaffoldSevenPagesAndRecommendedModulesForSiteInstitucional() {
        String suffix = UUID.randomUUID().toString();
        TenantSummary tenant = tenantService.createTenant(caller("creator-" + suffix),
                new CreateTenantCommand("tenant-" + suffix, "Tenant " + suffix, "FREE", null));

        ProductSummary product = productService.createProduct(caller("creator-" + suffix), new CreateProductCommand(
                tenant.id(), "site-" + suffix, "Site " + suffix, "Site Institucional", "pt-BR",
                AssetStorageStrategy.LOCAL
        ));

        List<ProductModule> modules = moduleRepository.findAllByProductId(product.id());
        assertThat(modules).extracting(ProductModule::getModuleKey)
                .containsExactlyInAnyOrder(ModuleKey.PAGES, ModuleKey.CONTENT, ModuleKey.ASSETS, ModuleKey.FORMS,
                        ModuleKey.SEO, ModuleKey.ANALYTICS);
        assertThat(modules).allSatisfy(module -> assertThat(module.isEnabled()).isTrue());

        List<Page> pages = pageRepository.findAllByProductId(product.id());
        assertThat(pages).hasSize(7)
                .extracting(Page::getSlug)
                .containsExactlyInAnyOrder("home", "quem-somos", "historia", "agenda", "galeria", "apoie", "contato");
        assertThat(pages).allSatisfy(page -> assertThat(page.getStatus().contractValue()).isEqualTo("draft"));

        Page home = pages.stream().filter(page -> "home".equals(page.getSlug())).findFirst().orElseThrow();
        List<PageSection> homeSections = sectionRepository.findAllByPageIdOrderByOrderAsc(home.getId());
        assertThat(homeSections).hasSize(4)
                .allSatisfy(section -> assertThat(section.getContentJson()).isNotBlank());
    }

    @Test
    void shouldCreateEmptyProductForCustomType() {
        String suffix = UUID.randomUUID().toString();
        TenantSummary tenant = tenantService.createTenant(caller("creator-" + suffix),
                new CreateTenantCommand("tenant-" + suffix, "Tenant " + suffix, "FREE", null));

        ProductSummary product = productService.createProduct(caller("creator-" + suffix), new CreateProductCommand(
                tenant.id(), "custom-" + suffix, "Custom " + suffix, "Custom", "pt-BR", AssetStorageStrategy.LOCAL
        ));

        assertThat(moduleRepository.findAllByProductId(product.id())).isEmpty();
        assertThat(pageRepository.findAllByProductId(product.id())).isEmpty();
    }

    private AuthenticatedUser caller(String subject) {
        return new AuthenticatedUser(subject, subject + "@byop.dev", subject, subject, Set.of("ROLE_TENANT_ADMIN"));
    }
}
