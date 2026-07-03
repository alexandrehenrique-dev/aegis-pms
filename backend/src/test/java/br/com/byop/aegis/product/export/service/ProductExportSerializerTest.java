package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.core.RepositoryTestSupport;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.export.exception.ProductExportException;
import br.com.byop.aegis.product.repository.ProductRepository;
import br.com.byop.aegis.tenant.domain.Tenant;
import br.com.byop.aegis.tenant.repository.TenantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductExportSerializerTest extends RepositoryTestSupport {

    private static final Instant EXPORTED_AT = Instant.parse("2026-07-03T12:00:00Z");
    private static final String CALLER_SUBJECT = "keycloak-subject";
    private static final String CALLER_EMAIL = "owner@byop.dev";

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeProductExportWithDomainJsonArraysAndManifestCounts() throws Exception {
        Tenant tenant = tenantRepository.saveAndFlush(tenant("export-serializer"));
        Product product = productRepository.saveAndFlush(product(tenant, "export-serializer"));
        seedDomainRows(tenant.getId(), product.getId());
        ProductExportSerializer serializer = new ProductExportSerializer(
                productRepository,
                jdbcClient,
                objectMapper,
                Clock.fixed(EXPORTED_AT, ZoneOffset.UTC)
        );

        ProductExportData data = serializer.serialize(product.getId(), CALLER_SUBJECT, CALLER_EMAIL);

        assertThat(data.tenantId()).isEqualTo(tenant.getId());
        assertThat(data.productId()).isEqualTo(product.getId());
        assertThat(data.filename()).isEqualTo("aegis-export-product-export-serializer-20260703-120000.zip");
        assertThat(data.jsonEntries()).containsKeys(
                "manifest.json",
                "product.json",
                "modules.json",
                "content/entries.json",
                "pages/pages.json",
                "forms/forms.json",
                "forms/submissions.json",
                "assets/metadata.json",
                "knowledge-graph/nodes.json",
                "knowledge-graph/edges.json",
                "users/assignments.json",
                "audit/events.json"
        );
        assertThat(objectMapper.readTree(data.jsonEntries().get("modules.json")).isArray()).isTrue();
        JsonNode manifest = objectMapper.readTree(data.jsonEntries().get("manifest.json"));
        assertThat(manifest.path("aegisExportVersion").asText()).isEqualTo("1.0");
        assertThat(manifest.path("requestedBy").path("email").asText()).isEqualTo(CALLER_EMAIL);
        assertThat(manifest.path("entityCounts").path("contentEntries").asInt()).isEqualTo(1);
        assertThat(manifest.path("entityCounts").path("assetFilesBytes").asLong()).isEqualTo(3L);
        assertThat(manifest.toString()).doesNotContain("null");
        JsonNode content = objectMapper.readTree(data.jsonEntries().get("content/entries.json"));
        assertThat(content.get(0).path("versions").isArray()).isTrue();
        JsonNode pages = objectMapper.readTree(data.jsonEntries().get("pages/pages.json"));
        assertThat(pages.get(0).path("sections").isArray()).isTrue();
        assertThat(data.assets()).hasSize(1);
        assertThat(data.assets().getFirst().storageKey()).isEqualTo("aegis/pms/export/image/logo.png");
    }

    @Test
    void shouldRejectMissingProduct() {
        ProductExportSerializer serializer = new ProductExportSerializer(
                productRepository,
                jdbcClient,
                objectMapper,
                Clock.fixed(EXPORTED_AT, ZoneOffset.UTC)
        );
        UUID productId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        assertThatThrownBy(() -> serializer.serialize(productId, CALLER_SUBJECT, CALLER_EMAIL))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldWrapInvalidJsonValue() {
        ProductExportSerializer serializer = new ProductExportSerializer(
                productRepository,
                jdbcClient,
                objectMapper,
                Clock.fixed(EXPORTED_AT, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> invoke(serializer, "readJson", "{"))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Unable to read JSONB column");
    }

    @Test
    void shouldNormalizeOffsetDateTimeToInstant() throws Exception {
        ProductExportSerializer serializer = new ProductExportSerializer(
                productRepository,
                jdbcClient,
                objectMapper,
                Clock.fixed(EXPORTED_AT, ZoneOffset.UTC)
        );
        OffsetDateTime value = OffsetDateTime.parse("2026-07-03T09:00:00-03:00");

        Object normalized = invokeObjectMethod(serializer, "normalizeValue", value);

        assertThat(normalized).isEqualTo(EXPORTED_AT);
    }

    @Test
    void shouldNormalizePostgresJsonObject() throws Exception {
        ProductExportSerializer serializer = new ProductExportSerializer(
                productRepository,
                jdbcClient,
                objectMapper,
                Clock.fixed(EXPORTED_AT, ZoneOffset.UTC)
        );
        PGobject value = postgresJsonObject("{\"enabled\":true}");

        Object normalized = invokeObjectMethod(serializer, "normalizeValue", value);

        assertThat(normalized).isEqualTo(Map.of("enabled", true));
    }

    @Test
    void shouldWrapJsonSerializationFailure() {
        ProductExportSerializer serializer = new ProductExportSerializer(
                productRepository,
                jdbcClient,
                new FailingWriteObjectMapper(),
                Clock.fixed(EXPORTED_AT, ZoneOffset.UTC)
        );

        Object value = new Object();

        assertThatThrownBy(() -> invokeObjectMethod(serializer, "jsonBytes", value))
                .isInstanceOf(ProductExportException.class)
                .hasMessage("Unable to serialize export JSON");
    }

    private Object invoke(ProductExportSerializer serializer, String methodName, Object value) throws Exception {
        Method method = ProductExportSerializer.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        try {
            return method.invoke(serializer, value);
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (target instanceof Exception targetException) {
                throw targetException;
            }
            throw exception;
        }
    }

    private Object invokeObjectMethod(ProductExportSerializer serializer, String methodName, Object value) throws Exception {
        Method method = ProductExportSerializer.class.getDeclaredMethod(methodName, Object.class);
        method.setAccessible(true);
        try {
            return method.invoke(serializer, value);
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (target instanceof Exception targetException) {
                throw targetException;
            }
            throw exception;
        }
    }

    private PGobject postgresJsonObject(String json) throws SQLException {
        PGobject value = new PGobject();
        value.setType("jsonb");
        value.setValue(json);
        return value;
    }

    private void seedDomainRows(UUID tenantId, UUID productId) {
        UUID contentId = UUID.randomUUID();
        UUID pageId = UUID.randomUUID();
        UUID formId = UUID.randomUUID();
        UUID firstNodeId = UUID.randomUUID();
        UUID secondNodeId = UUID.randomUUID();
        OffsetDateTime now = EXPORTED_AT.atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("""
                insert into product_modules (id, product_id, module_key, enabled, settings_json, created_at, updated_at)
                values (?, ?, 'CONTENT', true, '{"theme":"clean"}'::jsonb, ?, ?)
                """, UUID.randomUUID(), productId, now, now);
        jdbcTemplate.update("""
                insert into graph_nodes (id, tenant_id, product_id, node_type, ref_type, ref_id, label, slug, metadata_json, x, y, created_at, updated_at)
                values (?, ?, ?, 'TOPIC', 'topic', 'one', 'One', 'one', '{}'::jsonb, 0, 0, ?, ?),
                       (?, ?, ?, 'TOPIC', 'topic', 'two', 'Two', 'two', '{}'::jsonb, 1, 1, ?, ?)
                """, firstNodeId, tenantId, productId, now, now, secondNodeId, tenantId, productId, now, now);
        jdbcTemplate.update("""
                insert into graph_edges (id, tenant_id, product_id, source_node_id, target_node_id, edge_type, weight, metadata_json, created_at, updated_at)
                values (?, ?, ?, ?, ?, 'RELATED_TO', 1, '{}'::jsonb, ?, ?)
                """, UUID.randomUUID(), tenantId, productId, firstNodeId, secondNodeId, now, now);
        jdbcTemplate.update("""
                insert into contents (id, tenant_id, product_id, title, type, lang, author_subject, status, current_version, metadata_json, created_at, updated_at)
                values (?, ?, ?, 'Artigo', 'artigo', 'pt-BR', 'author', 'DRAFT', 1, '{"featured":true}'::jsonb, ?, ?)
                """, contentId, tenantId, productId, now, now);
        jdbcTemplate.update("""
                insert into content_versions (id, content_id, version_label, snapshot_json, created_by_subject, created_at)
                values (?, ?, 'v1', '{"title":"Artigo"}'::jsonb, 'author', ?)
                """, UUID.randomUUID(), contentId, now);
        jdbcTemplate.update("""
                insert into pages (id, tenant_id, product_id, slug, title, locale, status, version, seo_no_index, created_at, updated_at)
                values (?, ?, ?, 'home', 'Home', 'pt-BR', 'DRAFT', 1, false, ?, ?)
                """, pageId, tenantId, productId, now, now);
        jdbcTemplate.update("""
                insert into page_sections (id, page_id, type, section_order, content_json, created_at, updated_at)
                values (?, ?, 'hero', 1, '{"title":"Hero"}'::jsonb, ?, ?)
                """, UUID.randomUUID(), pageId, now, now);
        jdbcTemplate.update("""
                insert into form_definitions (id, tenant_id, product_id, name, type, status, fields_json, delivery_channels_json, created_at, updated_at)
                values (?, ?, ?, 'Contato', 'contact', 'DRAFT', '[]'::jsonb, '[]'::jsonb, ?, ?)
                """, formId, tenantId, productId, now, now);
        jdbcTemplate.update("""
                insert into form_submissions (id, form_id, date, name, email, source, status, answers_json, created_at)
                values (?, ?, ?, 'Ana', 'ana@example.com', 'site', 'NEW', '{}'::jsonb, ?)
                """, UUID.randomUUID(), formId, now, now);
        jdbcTemplate.update("""
                insert into assets (id, tenant_id, product_id, name, mime_type, category, size_bytes, status, storage_provider, storage_key, uploaded_by_subject, created_at, updated_at)
                values (?, ?, ?, 'logo.png', 'image/png', 'IMAGE', 3, 'ACTIVE', 'LOCAL', 'aegis/pms/export/image/logo.png', 'author', ?, ?)
                """, UUID.randomUUID(), tenantId, productId, now, now);
        jdbcTemplate.update("""
                insert into product_assignments (id, tenant_id, product_id, user_subject, role, status, created_at, updated_at)
                values (?, ?, ?, 'author', 'PRODUCT_MANAGER', 'ASSIGNED', ?, ?)
                """, UUID.randomUUID(), tenantId, productId, now, now);
        jdbcTemplate.update("""
                insert into audit_events (id, tenant_id, product_id, actor_subject, action, target_type, target_id, target_label, risk, created_at)
                values (?, ?, ?, 'author', 'CONTENT_CREATED', 'Content', 'content', 'Artigo', 'BAIXO', ?)
                """, UUID.randomUUID(), tenantId, productId, now);
    }

    private static final class FailingWriteObjectMapper extends ObjectMapper {

        @Override
        public ObjectMapper copy() {
            return this;
        }

        @Override
        public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
            throw new BrokenJsonProcessingException();
        }
    }

    private static final class BrokenJsonProcessingException extends JsonProcessingException {

        private BrokenJsonProcessingException() {
            super("broken");
        }
    }
}
