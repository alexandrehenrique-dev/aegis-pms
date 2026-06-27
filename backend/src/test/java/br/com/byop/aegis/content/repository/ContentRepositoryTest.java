package br.com.byop.aegis.content.repository;

import br.com.byop.aegis.content.domain.Content;
import br.com.byop.aegis.content.domain.ContentStatus;
import br.com.byop.aegis.content.domain.DifficultyLevel;
import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.knowledgegraph.domain.GraphNode;
import br.com.byop.aegis.knowledgegraph.domain.GraphNodeType;
import br.com.byop.aegis.knowledgegraph.repository.GraphNodeRepository;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private GraphNodeRepository graphNodeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveContent() {
        Product product = saveProduct("content-save");

        Content saved = contentRepository.saveAndFlush(
                new Content(product.getTenantId(), product.getId(), "Artigo", "article", "pt-BR", "subject-1")
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getTitle()).isEqualTo("Artigo");
        assertThat(saved.getType()).isEqualTo("article");
        assertThat(saved.getLang()).isEqualTo("pt-BR");
        assertThat(saved.getAuthorSubject()).isEqualTo("subject-1");
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(saved.getCurrentVersion()).isEqualTo(1);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyEditAndTrackWorkflowState() {
        Product product = saveProduct("content-edit");
        Content content = contentRepository.saveAndFlush(
                new Content(product.getTenantId(), product.getId(), "Artigo", "article", "pt-BR", "subject-1")
        );

        content.applyEdit(new Content.Edit("Artigo editado", "article", "pt-BR", "corpo", "resumo",
                DifficultyLevel.INTERMEDIATE, "categoria", "topico", "{\"k\":\"v\"}"));
        content.changeStatus(ContentStatus.IN_REVIEW);
        content.bumpVersion();
        content.markPublication("2026-06-26/site");
        contentRepository.saveAndFlush(content);

        Content reloaded = contentRepository.findById(content.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Artigo editado");
        assertThat(reloaded.getBodyMarkdown()).isEqualTo("corpo");
        assertThat(reloaded.getSummary()).isEqualTo("resumo");
        assertThat(reloaded.getDifficultyLevel()).isEqualTo(DifficultyLevel.INTERMEDIATE);
        assertThat(reloaded.getCategory()).isEqualTo("categoria");
        assertThat(reloaded.getTopic()).isEqualTo("topico");
        assertThat(reloaded.getMetadataJson()).isEqualTo("{\"k\":\"v\"}");
        assertThat(reloaded.getStatus()).isEqualTo(ContentStatus.IN_REVIEW);
        assertThat(reloaded.getCurrentVersion()).isEqualTo(2);
        assertThat(reloaded.getPublication()).isEqualTo("2026-06-26/site");
    }

    @Test
    void shouldLinkGraphNode() {
        Product product = saveProduct("content-graph-link");
        Content content = contentRepository.saveAndFlush(
                new Content(product.getTenantId(), product.getId(), "Artigo", "article", "pt-BR", "subject-1")
        );
        GraphNode node = graphNodeRepository.saveAndFlush(new GraphNode(new GraphNode.Creation(
                product.getTenantId(), product.getId(), GraphNodeType.CONTENT, "CONTENT",
                content.getId().toString(), "Artigo", "artigo", "{}"
        )));

        content.linkGraphNode(node.getId());
        contentRepository.saveAndFlush(content);

        assertThat(contentRepository.findById(content.getId()).orElseThrow().getGraphNodeId())
                .isEqualTo(node.getId());
    }

    @Test
    void shouldSupportContractStatusValues() {
        assertThat(ContentStatus.fromContractValue("In Review")).isEqualTo(ContentStatus.IN_REVIEW);
        assertThat(ContentStatus.IN_REVIEW.contractValue()).isEqualTo("In Review");
        assertThatThrownBy(() -> ContentStatus.fromContractValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSupportContractDifficultyValues() {
        assertThat(DifficultyLevel.fromContractValue("advanced")).isEqualTo(DifficultyLevel.ADVANCED);
        assertThat(DifficultyLevel.ADVANCED.contractValue()).isEqualTo("advanced");
        assertThatThrownBy(() -> DifficultyLevel.fromContractValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFindContentScopedByProduct() {
        Product product = saveProduct("content-find");
        Product otherProduct = saveProduct("content-find-other");
        Content content = contentRepository.saveAndFlush(
                new Content(product.getTenantId(), product.getId(), "Artigo", "article", "pt-BR", "subject-1")
        );
        contentRepository.saveAndFlush(
                new Content(otherProduct.getTenantId(), otherProduct.getId(), "Outro", "article", "pt-BR", "subject-2")
        );

        assertThat(contentRepository.findAllByProductId(product.getId())).containsExactly(content);
        assertThat(contentRepository.findByProductIdAndId(product.getId(), content.getId())).contains(content);
        assertThat(contentRepository.findByProductIdAndId(otherProduct.getId(), content.getId())).isEmpty();
    }

    @Test
    void shouldCascadeDeleteContentWhenProductIsDeleted() {
        Product product = saveProduct("content-cascade");
        Content content = contentRepository.saveAndFlush(
                new Content(product.getTenantId(), product.getId(), "Artigo", "article", "pt-BR", "subject-1")
        );

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("contents", content.getId())).isZero();
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }

    private Long countRows(String tableName, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                Long.class,
                id
        );
    }
}
