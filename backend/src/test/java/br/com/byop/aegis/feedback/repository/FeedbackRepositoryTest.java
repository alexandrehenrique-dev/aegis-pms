package br.com.byop.aegis.feedback.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.feedback.domain.Feedback;
import br.com.byop.aegis.feedback.domain.FeedbackCategory;
import br.com.byop.aegis.feedback.domain.FeedbackPriority;
import br.com.byop.aegis.feedback.domain.FeedbackStatus;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveFeedbackWithReadablePublicId() {
        Product product = saveProduct("feedback-save");
        String publicId = nextPublicId();

        Feedback saved = feedbackRepository.saveAndFlush(newFeedback(product, publicId));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPublicId()).isEqualTo(publicId);
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getCreatedBySubject()).isEqualTo("subject-1");
        assertThat(saved.getCategory()).isEqualTo(FeedbackCategory.BUG);
        assertThat(saved.getPriority()).isEqualTo(FeedbackPriority.HIGH);
        assertThat(saved.getDescription()).isEqualTo("Botao X nao responde ao clicar.");
        assertThat(saved.getScreenName()).isEqualTo("/content/list");
        assertThat(saved.getStatus()).isEqualTo(FeedbackStatus.OPEN);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldChangeStatus() {
        Product product = saveProduct("feedback-status");
        Feedback feedback = feedbackRepository.saveAndFlush(newFeedback(product, nextPublicId()));

        feedback.changeStatus(FeedbackStatus.IN_REVIEW);
        feedbackRepository.saveAndFlush(feedback);

        assertThat(feedbackRepository.findById(feedback.getId()))
                .get()
                .extracting(Feedback::getStatus)
                .isEqualTo(FeedbackStatus.IN_REVIEW);
    }

    @Test
    void shouldRejectDuplicatePublicId() {
        Product product = saveProduct("feedback-duplicate");
        String publicId = nextPublicId();
        feedbackRepository.saveAndFlush(newFeedback(product, publicId));
        Feedback duplicate = newFeedback(product, publicId);

        assertThatThrownBy(() -> feedbackRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInvalidStatusAtDatabaseLevel() {
        Product product = saveProduct("feedback-invalid-status");
        UUID feedbackId = UUID.randomUUID();
        String publicId = nextPublicId();
        UUID tenantId = product.getTenantId();
        UUID productId = product.getId();
        String insertInvalidStatus = """
                INSERT INTO feedback (
                    id, public_id, tenant_id, product_id, created_by_subject, category, priority,
                    description, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        assertThatThrownBy(() -> jdbcTemplate.update(
                insertInvalidStatus,
                feedbackId,
                publicId,
                tenantId,
                productId,
                "subject-1",
                "BUG",
                "HIGH",
                "Descricao",
                "INVALID"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldListByTenantAndStatusOrderedByCreatedAtDesc() {
        Product product = saveProduct("feedback-list");
        Product otherProduct = saveProduct("feedback-list-other");
        String olderOpenPublicId = nextPublicId();
        String newerOpenPublicId = nextPublicId();
        String resolvedPublicId = nextPublicId();
        Feedback olderOpen = feedbackRepository.saveAndFlush(newFeedback(product, olderOpenPublicId));
        Feedback newerOpen = feedbackRepository.saveAndFlush(newFeedback(product, newerOpenPublicId));
        Feedback resolved = feedbackRepository.saveAndFlush(newFeedback(product, resolvedPublicId));
        Feedback otherTenant = feedbackRepository.saveAndFlush(newFeedback(otherProduct, nextPublicId()));
        resolved.changeStatus(FeedbackStatus.RESOLVED);
        feedbackRepository.saveAndFlush(resolved);
        setCreatedAt(olderOpen, "2026-07-03T10:00:00Z");
        setCreatedAt(newerOpen, "2026-07-03T11:00:00Z");
        setCreatedAt(resolved, "2026-07-03T12:00:00Z");
        setCreatedAt(otherTenant, "2026-07-03T13:00:00Z");

        assertThat(feedbackRepository.findAllByTenantIdOrderByCreatedAtDesc(product.getTenantId()))
                .extracting(Feedback::getPublicId)
                .containsExactly(resolvedPublicId, newerOpenPublicId, olderOpenPublicId);
        assertThat(feedbackRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(
                product.getTenantId(),
                FeedbackStatus.OPEN
        )).extracting(Feedback::getPublicId).containsExactly(newerOpenPublicId, olderOpenPublicId);
        assertThat(feedbackRepository.findAllByTenantIdOrderByCreatedAtDesc(otherProduct.getTenantId()))
                .containsExactly(otherTenant);
    }

    @Test
    void shouldFindByPublicIdAndGenerateSequenceValues() {
        Product product = saveProduct("feedback-public-id");
        String publicId = nextPublicId();
        Feedback feedback = feedbackRepository.saveAndFlush(newFeedback(product, publicId));

        long firstSequence = feedbackRepository.nextPublicIdSequence();
        long secondSequence = feedbackRepository.nextPublicIdSequence();

        assertThat(feedbackRepository.findByPublicId(publicId)).contains(feedback);
        assertThat(secondSequence).isEqualTo(firstSequence + 1);
    }

    @Test
    void shouldSupportContractValues() {
        assertThat(FeedbackCategory.fromContractValue("Bug")).isEqualTo(FeedbackCategory.BUG);
        assertThat(FeedbackCategory.fromContractValue("permissao incorreta"))
                .isEqualTo(FeedbackCategory.INCORRECT_PERMISSION);
        assertThat(FeedbackCategory.BUG.contractValue()).isEqualTo("Bug");
        assertThat(FeedbackPriority.fromContractValue("crítica")).isEqualTo(FeedbackPriority.CRITICAL);
        assertThat(FeedbackPriority.fromContractValue("critica")).isEqualTo(FeedbackPriority.CRITICAL);
        assertThat(FeedbackPriority.CRITICAL.contractValue()).isEqualTo("crítica");
        assertThat(FeedbackStatus.fromContractValue("em_analise")).isEqualTo(FeedbackStatus.IN_REVIEW);
        assertThat(FeedbackStatus.IN_REVIEW.contractValue()).isEqualTo("em_analise");
    }

    @Test
    void shouldRejectUnsupportedContractValues() {
        String unsupportedCategory = "Suporte";
        String unsupportedPriority = "urgente";

        assertThatThrownBy(() -> FeedbackCategory.fromContractValue(unsupportedCategory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported feedback category");
        assertThatThrownBy(() -> FeedbackPriority.fromContractValue(unsupportedPriority))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported feedback priority");
    }

    private Feedback newFeedback(Product product, String publicId) {
        return new Feedback(new Feedback.Creation(
                publicId,
                product.getTenantId(),
                product.getId(),
                "subject-1",
                FeedbackCategory.BUG,
                FeedbackPriority.HIGH,
                "Botao X nao responde ao clicar.",
                "/content/list",
                null
        ));
    }

    private String nextPublicId() {
        return "AGS-%04d".formatted(feedbackRepository.nextPublicIdSequence());
    }

    private Product saveProduct(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        return productRepository.saveAndFlush(product(tenant, suffix));
    }

    private void setCreatedAt(Feedback feedback, String createdAt) {
        OffsetDateTime timestamp = OffsetDateTime.parse(createdAt);
        jdbcTemplate.update(
                "update feedback set created_at = ?, updated_at = ? where id = ?",
                timestamp,
                timestamp,
                feedback.getId()
        );
    }
}
