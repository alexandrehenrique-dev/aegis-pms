package br.com.byop.aegis.pages.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.pages.domain.BlockType;
import br.com.byop.aegis.pages.domain.Page;
import br.com.byop.aegis.pages.domain.PageSection;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PageSectionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PageSectionRepository sectionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveSection() {
        Page page = savePage("section-save");

        PageSection saved = sectionRepository.saveAndFlush(new PageSection(
                page.getId(), BlockType.HERO, "centered", 0, "{\"title\":\"Ola\"}", "{\"align\":\"center\"}"
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPageId()).isEqualTo(page.getId());
        assertThat(saved.getType()).isEqualTo(BlockType.HERO);
        assertThat(saved.getVariant()).isEqualTo("centered");
        assertThat(saved.getOrder()).isZero();
        assertThat(saved.getContentJson()).isEqualTo("{\"title\":\"Ola\"}");
        assertThat(saved.getSettingsJson()).contains("center");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldApplyEditAndReorder() {
        Page page = savePage("section-edit");
        PageSection section = sectionRepository.saveAndFlush(new PageSection(
                page.getId(), BlockType.TEXT, null, 0, "{\"body\":\"a\"}", null
        ));

        section.applyEdit(new PageSection.Edit(BlockType.RICH_TEXT, "highlight", 1, "{\"body\":\"b\"}", "{\"columns\":2}"));
        sectionRepository.saveAndFlush(section);

        PageSection reloaded = sectionRepository.findById(section.getId()).orElseThrow();
        assertThat(reloaded.getType()).isEqualTo(BlockType.RICH_TEXT);
        assertThat(reloaded.getVariant()).isEqualTo("highlight");
        assertThat(reloaded.getOrder()).isEqualTo(1);
        assertThat(reloaded.getContentJson()).contains("\"b\"");

        reloaded.reorderTo(5);
        sectionRepository.saveAndFlush(reloaded);
        assertThat(sectionRepository.findById(section.getId()).orElseThrow().getOrder()).isEqualTo(5);
    }

    @Test
    void shouldListSectionsOrderedByOrder() {
        Page page = savePage("section-order");
        PageSection second = sectionRepository.saveAndFlush(new PageSection(page.getId(), BlockType.TEXT, null, 1, "{}", null));
        PageSection first = sectionRepository.saveAndFlush(new PageSection(page.getId(), BlockType.HERO, null, 0, "{\"title\":\"x\"}", null));

        assertThat(sectionRepository.findAllByPageIdOrderByOrderAsc(page.getId()))
                .containsExactly(first, second);
        assertThat(sectionRepository.findByPageIdAndId(page.getId(), first.getId())).contains(first);
    }

    @Test
    void shouldFindSectionsReferencingFormId() {
        Page page = savePage("section-form-ref");
        UUID formId = UUID.randomUUID();
        sectionRepository.saveAndFlush(new PageSection(
                page.getId(), BlockType.CONTACT, null, 0, "{\"formId\":\"" + formId + "\"}", null
        ));

        assertThat(sectionRepository.existsByFormIdReference(formId)).isTrue();
        assertThat(sectionRepository.existsByFormIdReference(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldCascadeDeleteSectionsWhenPageIsDeleted() {
        Page page = savePage("section-cascade");
        PageSection section = sectionRepository.saveAndFlush(new PageSection(page.getId(), BlockType.HERO, null, 0, "{\"title\":\"x\"}", null));

        jdbcTemplate.update("DELETE FROM pages WHERE id = ?", page.getId());

        assertThat(countRows("page_sections", section.getId())).isZero();
    }

    private Page savePage(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        Product product = productRepository.saveAndFlush(product(tenant, suffix));
        return pageRepository.saveAndFlush(new Page(product.getTenantId(), product.getId(), "slug-" + suffix, "Titulo", "pt-BR"));
    }

    private Long countRows(String tableName, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                Long.class,
                id
        );
    }
}
