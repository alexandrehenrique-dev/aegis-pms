package br.com.byop.aegis.product.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.AssetStorageStrategy;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.domain.ProductModule;
import br.com.byop.aegis.product.domain.ProductTypeKey;
import br.com.byop.aegis.product.dto.ProductModuleSummary;
import br.com.byop.aegis.product.exception.InvalidModuleKeyException;
import br.com.byop.aegis.product.exception.ModuleDependencyMissingException;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.mapper.ProductModuleMapper;
import br.com.byop.aegis.product.repository.ProductModuleRepository;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.security.AuthenticatedUser;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductModuleServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductModuleRepository moduleRepository;

    @Mock
    private ProductModuleMapper moduleMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductModuleService moduleService;

    @Test
    void shouldEnableValidModule() {
        Product product = product();
        ProductModule savedModule = new ProductModule(product, ModuleKey.CONTENT);
        savedModule.enable();
        ProductModuleSummary summary = moduleSummary(product.getId(), ModuleKey.CONTENT, true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.CONTENT)).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenReturn(savedModule);
        when(moduleMapper.toSummary(savedModule)).thenReturn(summary);

        ProductModuleSummary result = moduleService.enableModule(product.getId(), "CONTENT");

        assertThat(result).isEqualTo(summary);
        ArgumentCaptor<ProductModule> moduleCaptor = ArgumentCaptor.forClass(ProductModule.class);
        verify(moduleRepository).save(moduleCaptor.capture());
        assertThat(moduleCaptor.getValue().getProduct()).isEqualTo(product);
        assertThat(moduleCaptor.getValue().getModuleKey()).isEqualTo(ModuleKey.CONTENT);
        assertThat(moduleCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    void shouldRejectUnknownModuleKey() {
        Product product = product();
        UUID productId = product.getId();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> moduleService.enableModule(productId, "UNKNOWN"))
                .isInstanceOf(InvalidModuleKeyException.class)
                .hasMessage("Invalid module key: UNKNOWN");

        verify(moduleRepository, never()).save(any(ProductModule.class));
    }

    @Test
    void shouldRejectWhenProductDoesNotExist() {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.enableModule(productId, "CONTENT"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldRejectWhenProductDoesNotExistOnDisable() {
        UUID productId = UUID.fromString("87878787-8787-8787-8787-878787878787");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.disableModule(productId, "CONTENT"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + productId);
    }

    @Test
    void shouldFailWhenEnablingKnowledgeGraphWithoutContent() {
        Product product = product();
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, ModuleKey.CONTENT))
                .thenReturn(false);

        assertThatThrownBy(() -> moduleService.enableModule(productId, "KNOWLEDGE_GRAPH"))
                .isInstanceOf(ModuleDependencyMissingException.class)
                .satisfies(exception -> {
                    ModuleDependencyMissingException dependencyException = (ModuleDependencyMissingException) exception;
                    assertThat(dependencyException.getModuleKey()).isEqualTo(ModuleKey.KNOWLEDGE_GRAPH);
                    assertThat(dependencyException.getRequires()).isEqualTo(ModuleKey.CONTENT);
                });
    }

    @Test
    void shouldEnableContentAndThenKnowledgeGraph() {
        Product product = product();
        ProductModule content = new ProductModule(product, ModuleKey.CONTENT);
        content.enable();
        ProductModule graph = new ProductModule(product, ModuleKey.KNOWLEDGE_GRAPH);
        graph.enable();
        ProductModuleSummary contentSummary = moduleSummary(product.getId(), ModuleKey.CONTENT, true);
        ProductModuleSummary graphSummary = moduleSummary(product.getId(), ModuleKey.KNOWLEDGE_GRAPH, true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.CONTENT)).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenReturn(content, graph);
        when(moduleMapper.toSummary(content)).thenReturn(contentSummary);

        ProductModuleSummary enabledContent = moduleService.enableModule(product.getId(), "CONTENT");

        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(product.getId(), ModuleKey.CONTENT))
                .thenReturn(true);
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.KNOWLEDGE_GRAPH))
                .thenReturn(Optional.empty());
        when(moduleMapper.toSummary(graph)).thenReturn(graphSummary);

        ProductModuleSummary enabledGraph = moduleService.enableModule(product.getId(), "KNOWLEDGE_GRAPH");

        assertThat(enabledContent.moduleKey()).isEqualTo(ModuleKey.CONTENT);
        assertThat(enabledGraph.moduleKey()).isEqualTo(ModuleKey.KNOWLEDGE_GRAPH);
    }

    @Test
    void shouldFailWhenDisablingContentWithKnowledgeGraphEnabled() {
        Product product = product();
        UUID productId = product.getId();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(productId, ModuleKey.KNOWLEDGE_GRAPH))
                .thenReturn(true);

        assertThatThrownBy(() -> moduleService.disableModule(productId, "CONTENT"))
                .isInstanceOf(ModuleDependencyMissingException.class)
                .satisfies(exception -> {
                    ModuleDependencyMissingException dependencyException = (ModuleDependencyMissingException) exception;
                    assertThat(dependencyException.getModuleKey()).isEqualTo(ModuleKey.KNOWLEDGE_GRAPH);
                    assertThat(dependencyException.getRequires()).isEqualTo(ModuleKey.CONTENT);
                });
    }

    @Test
    void shouldDisableModuleWithoutEnabledDependent() {
        Product product = product();
        ProductModule content = new ProductModule(product, ModuleKey.CONTENT);
        content.enable();
        ProductModuleSummary summary = moduleSummary(product.getId(), ModuleKey.CONTENT, false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(product.getId(), ModuleKey.KNOWLEDGE_GRAPH))
                .thenReturn(false);
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.CONTENT)).thenReturn(Optional.of(content));
        when(moduleRepository.save(content)).thenReturn(content);
        when(moduleMapper.toSummary(content)).thenReturn(summary);

        ProductModuleSummary result = moduleService.disableModule(product.getId(), "CONTENT");

        assertThat(result).isEqualTo(summary);
        assertThat(content.isEnabled()).isFalse();
    }

    @Test
    void shouldDisableNewModuleWhenModuleWasNotConfiguredYet() {
        Product product = product();
        ProductModule savedModule = new ProductModule(product, ModuleKey.ECOMMERCE);
        ProductModuleSummary summary = moduleSummary(product.getId(), ModuleKey.ECOMMERCE, false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.ECOMMERCE)).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenReturn(savedModule);
        when(moduleMapper.toSummary(savedModule)).thenReturn(summary);

        ProductModuleSummary result = moduleService.disableModule(product.getId(), "ECOMMERCE");

        assertThat(result.moduleKey()).isEqualTo(ModuleKey.ECOMMERCE);
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void shouldRecordAuditWhenCallerEnablesModule() {
        Product product = product();
        ProductModule savedModule = new ProductModule(product, ModuleKey.CONTENT);
        savedModule.enable();
        ProductModuleSummary summary = moduleSummary(product.getId(), ModuleKey.CONTENT, true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.CONTENT)).thenReturn(Optional.empty());
        when(moduleRepository.save(any(ProductModule.class))).thenReturn(savedModule);
        when(moduleMapper.toSummary(savedModule)).thenReturn(summary);
        AuthenticatedUser caller = caller();

        moduleService.enableModule(product.getId(), "CONTENT", caller);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        AuditRecordCommand command = auditCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(product.getTenantId());
        assertThat(command.productId()).isEqualTo(product.getId());
        assertThat(command.actorSubject()).isEqualTo("pm-subject");
        assertThat(command.action()).isEqualTo("MODULE_ENABLED");
        assertThat(command.after()).containsEntry("moduleKey", "CONTENT").containsEntry("enabled", true);
    }

    @Test
    void shouldRecordAuditWhenCallerDisablesModule() {
        Product product = product();
        ProductModule content = new ProductModule(product, ModuleKey.CONTENT);
        content.enable();
        ProductModuleSummary summary = moduleSummary(product.getId(), ModuleKey.CONTENT, false);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(moduleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(product.getId(), ModuleKey.KNOWLEDGE_GRAPH))
                .thenReturn(false);
        when(moduleRepository.findByProductIdAndModuleKey(product.getId(), ModuleKey.CONTENT)).thenReturn(Optional.of(content));
        when(moduleRepository.save(content)).thenReturn(content);
        when(moduleMapper.toSummary(content)).thenReturn(summary);
        AuthenticatedUser caller = caller();

        moduleService.disableModule(product.getId(), "CONTENT", caller);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("MODULE_DISABLED");
        assertThat(auditCaptor.getValue().after()).containsEntry("enabled", false);
    }

    private Product product() {
        Product product = new Product(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "maestro-beton",
                "Maestro Beton",
                ProductTypeKey.SITE_INSTITUCIONAL,
                "pt-BR",
                AssetStorageStrategy.LOCAL
        );
        ReflectionTestUtils.setField(product, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return product;
    }

    private ProductModuleSummary moduleSummary(UUID productId, ModuleKey moduleKey, boolean enabled) {
        return new ProductModuleSummary(
                UUID.nameUUIDFromBytes(moduleKey.name().getBytes()),
                productId,
                moduleKey,
                enabled,
                "{}",
                OffsetDateTime.parse("2026-06-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-06-25T10:10:00-03:00")
        );
    }

    private AuthenticatedUser caller() {
        return new AuthenticatedUser("pm-subject", "pm@byop.io", "pm", "Product Manager",
                Set.of("ROLE_PRODUCT_MANAGER"));
    }
}
