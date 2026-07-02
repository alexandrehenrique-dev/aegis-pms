package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.pages.domain.Event;
import br.com.byop.aegis.pages.domain.EventAccessType;
import br.com.byop.aegis.pages.domain.EventVisibility;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveEvent() {
        Product product = saveProduct("event-save");
        LocalDateTime datetime = LocalDateTime.parse("2026-07-12T16:00");

        Event saved = eventRepository.saveAndFlush(new Event(
                product.getTenantId(), product.getId(), "Show ao vivo", datetime, "Teatro Municipal",
                EventAccessType.PUBLIC, EventVisibility.PUBLIC
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(product.getTenantId());
        assertThat(saved.getProductId()).isEqualTo(product.getId());
        assertThat(saved.getTitle()).isEqualTo("Show ao vivo");
        assertThat(saved.getDatetime()).isEqualTo(datetime);
        assertThat(saved.getLocation()).isEqualTo("Teatro Municipal");
        assertThat(saved.getType()).isEqualTo(EventAccessType.PUBLIC);
        assertThat(saved.getVisibility()).isEqualTo(EventVisibility.PUBLIC);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyEdit() {
        Product product = saveProduct("event-edit");
        Event event = eventRepository.saveAndFlush(new Event(
                product.getTenantId(), product.getId(), "Show", LocalDateTime.parse("2026-07-12T16:00"),
                "Local", EventAccessType.PUBLIC, EventVisibility.PUBLIC
        ));
        UUID imageAssetId = UUID.randomUUID();

        event.applyEdit(new Event.Edit("Show remarcado", LocalDateTime.parse("2026-08-01T20:30"), "Novo local",
                EventAccessType.PRIVATE, EventVisibility.PUBLIC_SUMMARY, "descricao", imageAssetId));
        eventRepository.saveAndFlush(event);

        Event reloaded = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Show remarcado");
        assertThat(reloaded.getDatetime()).isEqualTo(LocalDateTime.parse("2026-08-01T20:30"));
        assertThat(reloaded.getLocation()).isEqualTo("Novo local");
        assertThat(reloaded.getType()).isEqualTo(EventAccessType.PRIVATE);
        assertThat(reloaded.getVisibility()).isEqualTo(EventVisibility.PUBLIC_SUMMARY);
        assertThat(reloaded.getDescription()).isEqualTo("descricao");
        assertThat(reloaded.getImageAssetId()).isEqualTo(imageAssetId);
    }

    @Test
    void shouldListEventsOrderedByDatetime() {
        Product product = saveProduct("event-order");
        Event later = eventRepository.saveAndFlush(new Event(
                product.getTenantId(), product.getId(), "Depois", LocalDateTime.parse("2026-09-01T10:00"),
                "Local", EventAccessType.PUBLIC, EventVisibility.PUBLIC
        ));
        Event earlier = eventRepository.saveAndFlush(new Event(
                product.getTenantId(), product.getId(), "Antes", LocalDateTime.parse("2026-08-01T10:00"),
                "Local", EventAccessType.PUBLIC, EventVisibility.PUBLIC
        ));

        assertThat(eventRepository.findAllByProductIdOrderByDatetimeAsc(product.getId()))
                .containsExactly(earlier, later);
    }

    @Test
    void shouldFindEventScopedByProduct() {
        Product product = saveProduct("event-find");
        Product otherProduct = saveProduct("event-find-other");
        Event event = eventRepository.saveAndFlush(new Event(
                product.getTenantId(), product.getId(), "Show", LocalDateTime.parse("2026-07-12T16:00"),
                "Local", EventAccessType.PUBLIC, EventVisibility.PUBLIC
        ));

        assertThat(eventRepository.findByProductIdAndId(product.getId(), event.getId())).contains(event);
        assertThat(eventRepository.findByProductIdAndId(otherProduct.getId(), event.getId())).isEmpty();
    }

    @Test
    void shouldSupportEventAccessTypeContractValues() {
        assertThat(EventAccessType.fromContractValue("private")).isEqualTo(EventAccessType.PRIVATE);
        assertThat(EventAccessType.PRIVATE.contractValue()).isEqualTo("private");
        assertThatThrownBy(() -> EventAccessType.fromContractValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSupportEventVisibilityContractValues() {
        assertThat(EventVisibility.fromContractValue("public-summary")).isEqualTo(EventVisibility.PUBLIC_SUMMARY);
        assertThat(EventVisibility.PUBLIC_SUMMARY.contractValue()).isEqualTo("public-summary");
        assertThatThrownBy(() -> EventVisibility.fromContractValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCascadeDeleteEventWhenProductIsDeleted() {
        Product product = saveProduct("event-cascade");
        Event event = eventRepository.saveAndFlush(new Event(
                product.getTenantId(), product.getId(), "Show", LocalDateTime.parse("2026-07-12T16:00"),
                "Local", EventAccessType.PUBLIC, EventVisibility.PUBLIC
        ));

        jdbcTemplate.update("DELETE FROM products WHERE id = ?", product.getId());

        assertThat(countRows("events", event.getId())).isZero();
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
