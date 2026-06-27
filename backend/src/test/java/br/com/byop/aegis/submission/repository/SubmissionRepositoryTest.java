package br.com.byop.aegis.submission.repository;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.domain.SubmissionStatus;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FormDefinitionRepository formDefinitionRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveSubmission() {
        FormDefinition form = saveForm("submission-save");
        OffsetDateTime date = OffsetDateTime.of(2026, 6, 27, 10, 0, 0, 0, ZoneOffset.UTC);

        Submission saved = submissionRepository.saveAndFlush(newSubmission(form, date, "Ana"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFormId()).isEqualTo(form.getId());
        assertThat(saved.getDate()).isEqualTo(date);
        assertThat(saved.getName()).isEqualTo("Ana");
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getSource()).isEqualTo("site");
        assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.NEW);
        assertThat(saved.getOwnerSubject()).isEqualTo("owner-1");
        assertThat(saved.getScore()).isEqualTo(80);
        assertThat(saved.getAnswersJson()).contains("answers");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindSubmissionsScopedByForm() {
        FormDefinition form = saveForm("submission-find");
        FormDefinition otherForm = saveForm("submission-find-other");
        Submission older = submissionRepository.saveAndFlush(newSubmission(form, OffsetDateTime.parse("2026-06-26T10:00:00Z"), "Ana"));
        Submission newer = submissionRepository.saveAndFlush(newSubmission(form, OffsetDateTime.parse("2026-06-27T10:00:00Z"), "Bia"));
        submissionRepository.saveAndFlush(newSubmission(otherForm, OffsetDateTime.parse("2026-06-28T10:00:00Z"), "Clara"));

        assertThat(submissionRepository.findAllByFormIdOrderByDateDesc(form.getId())).containsExactly(newer, older);
        assertThat(submissionRepository.findByFormIdAndId(form.getId(), newer.getId())).contains(newer);
        assertThat(submissionRepository.findByFormIdAndId(otherForm.getId(), newer.getId())).isEmpty();
    }

    @Test
    void shouldCascadeDeleteSubmissionWhenFormIsDeleted() {
        FormDefinition form = saveForm("submission-cascade");
        Submission submission = submissionRepository.saveAndFlush(
                newSubmission(form, OffsetDateTime.parse("2026-06-27T10:00:00Z"), "Ana")
        );

        jdbcTemplate.update("DELETE FROM form_definitions WHERE id = ?", form.getId());

        assertThat(countRows("form_submissions", submission.getId())).isZero();
    }

    private FormDefinition saveForm(String suffix) {
        Tenant tenant = tenantRepository.saveAndFlush(tenant(suffix));
        Product product = productRepository.saveAndFlush(product(tenant, suffix));
        return formDefinitionRepository.saveAndFlush(new FormDefinition(new FormDefinition.Creation(
                product.getTenantId(), product.getId(), "Contato " + suffix, "lead",
                "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]", "[]"
        )));
    }

    private Submission newSubmission(FormDefinition form, OffsetDateTime date, String name) {
        return new Submission(new Submission.Creation(
                form.getId(), date, name, name.toLowerCase() + "@example.com", "site",
                SubmissionStatus.NEW, "owner-1", 80, "{\"answers\":{\"Email\":\"" + name + "@example.com\"}}"
        ));
    }

    private Long countRows(String tableName, UUID id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?", Long.class, id);
    }
}
