package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import br.com.byop.aegis.product.export.exception.ProductExportException;
import br.com.byop.aegis.product.domain.Product;
import br.com.byop.aegis.product.exception.ProductNotFoundException;
import br.com.byop.aegis.product.repository.ProductRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductExportSerializer {

    private static final String AEGIS_EXPORT_VERSION = "1.0";
    private static final String TRIGGER_PRODUCT_DELETED = "PRODUCT_DELETED";
    private static final String KEY_PRODUCT_ID = "product_id";
    private static final String KEY_ID = "id";
    private static final String KEY_FOREIGN_KEY = "foreignKey";
    private static final String ENTRY_MANIFEST = "manifest.json";
    private static final String ENTRY_PRODUCT = "product.json";
    private static final String ENTRY_MODULES = "modules.json";
    private static final String ENTRY_CONTENT = "content/entries.json";
    private static final String ENTRY_PAGES = "pages/pages.json";
    private static final String ENTRY_FORMS = "forms/forms.json";
    private static final String ENTRY_SUBMISSIONS = "forms/submissions.json";
    private static final String ENTRY_ASSETS = "assets/metadata.json";
    private static final String ENTRY_GRAPH_NODES = "knowledge-graph/nodes.json";
    private static final String ENTRY_GRAPH_EDGES = "knowledge-graph/edges.json";
    private static final String ENTRY_USERS = "users/assignments.json";
    private static final String ENTRY_AUDIT = "audit/events.json";

    private final ProductRepository productRepository;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ProductExportSerializer(ProductRepository productRepository, JdbcClient jdbcClient) {
        this(productRepository, jdbcClient, new ObjectMapper(), Clock.systemUTC());
    }

    ProductExportSerializer(ProductRepository productRepository, JdbcClient jdbcClient, ObjectMapper objectMapper, Clock clock) {
        this.productRepository = productRepository;
        this.jdbcClient = jdbcClient;
        this.objectMapper = exportObjectMapper(objectMapper);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProductExportData serialize(UUID productId, String callerSubject, String callerEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        Map<String, Object> productJson = productJson(product);
        Map<String, Object> tenantJson = singleById(ExportTable.TENANTS, product.getTenantId());
        ExportRows rows = exportRows(productId);
        Map<String, Object> entityCounts = entityCounts(rows);
        Instant exportedAt = clock.instant();
        Instant downloadExpiresAt = exportedAt.plusSeconds(604800);
        Map<String, Object> manifest = manifest(productJson, tenantJson, callerSubject, callerEmail, entityCounts, exportedAt, downloadExpiresAt);
        Map<String, byte[]> jsonEntries = jsonEntries(new JsonEntryData(manifest, productJson, rows));
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(exportedAt);

        return new ProductExportData(
                product.getTenantId(),
                product.getId(),
                product.getKey(),
                product.getName(),
                product.getAssetStorageStrategy(),
                "aegis-export-%s-%s.zip".formatted(product.getKey(), timestamp),
                jsonEntries,
                exportAssetFiles(rows.assets),
                manifest,
                entityCounts
        );
    }

    private ExportRows exportRows(UUID productId) {
        ExportRows rows = new ExportRows();
        rows.modules = allByProductId(ExportTable.PRODUCT_MODULES, productId);
        rows.contentEntries = contentEntries(productId);
        rows.pages = pages(productId);
        rows.forms = allByProductId(ExportTable.FORM_DEFINITIONS, productId);
        rows.submissions = submissions(productId);
        rows.assets = allByProductId(ExportTable.ASSETS, productId);
        rows.graphNodes = allByProductId(ExportTable.GRAPH_NODES, productId);
        rows.graphEdges = allByProductId(ExportTable.GRAPH_EDGES, productId);
        rows.assignments = allByProductId(ExportTable.PRODUCT_ASSIGNMENTS, productId);
        rows.auditEvents = allByProductId(ExportTable.AUDIT_EVENTS, productId);
        return rows;
    }

    private Map<String, Object> entityCounts(ExportRows rows) {
        long assetFilesBytes = rows.assets.stream()
                .mapToLong(asset -> ((Number) asset.getOrDefault("sizeBytes", 0L)).longValue())
                .sum();
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("contentEntries", rows.contentEntries.size());
        counts.put("pages", rows.pages.size());
        counts.put("forms", rows.forms.size());
        counts.put("formSubmissions", rows.submissions.size());
        counts.put("assets", rows.assets.size());
        counts.put("assetFilesBytes", assetFilesBytes);
        counts.put("knowledgeGraphNodes", rows.graphNodes.size());
        counts.put("knowledgeGraphEdges", rows.graphEdges.size());
        counts.put("auditEvents", rows.auditEvents.size());
        return counts;
    }

    private Map<String, Object> productJson(Product product) {
        Map<String, Object> productJson = new LinkedHashMap<>();
        productJson.put("id", product.getId());
        productJson.put("key", product.getKey());
        productJson.put("name", product.getName());
        productJson.put("type", product.getType().label());
        productJson.put("defaultLocale", product.getDefaultLocale());
        productJson.put("assetStorageStrategy", product.getAssetStorageStrategy().name().toLowerCase());
        productJson.put("tenantId", product.getTenantId());
        productJson.put("status", product.getStatus().name().toLowerCase());
        productJson.put("createdAt", product.getCreatedAt());
        productJson.put("updatedAt", product.getUpdatedAt());
        return productJson;
    }

    private Map<String, Object> singleById(ExportTable table, UUID id) {
        return jdbcClient.sql(table.byIdSql()).param(KEY_ID, id).query(this::readRow).optional().orElse(Map.of());
    }

    private List<Map<String, Object>> allByProductId(ExportTable table, UUID productId) {
        return jdbcClient.sql(table.byProductSql()).param(KEY_PRODUCT_ID, productId).query(this::readRow).list();
    }

    private List<Map<String, Object>> contentEntries(UUID productId) {
        List<Map<String, Object>> entries = allByProductId(ExportTable.CONTENTS, productId);
        entries.forEach(entry -> entry.put("versions", allByForeignKey(ExportForeignKey.CONTENT_VERSIONS_BY_CONTENT, entry.get(KEY_ID))));
        return entries;
    }

    private List<Map<String, Object>> pages(UUID productId) {
        List<Map<String, Object>> pages = allByProductId(ExportTable.PAGES, productId);
        pages.forEach(page -> page.put("sections", allByForeignKey(ExportForeignKey.PAGE_SECTIONS_BY_PAGE, page.get(KEY_ID))));
        return pages;
    }

    private List<Map<String, Object>> submissions(UUID productId) {
        String sql = """
                select submissions.*
                from form_submissions submissions
                join form_definitions forms on forms.id = submissions.form_id
                where forms.product_id = :product_id
                order by submissions.id
                """;
        return jdbcClient.sql(sql).param(KEY_PRODUCT_ID, productId).query(this::readRow).list();
    }

    private List<Map<String, Object>> allByForeignKey(ExportForeignKey foreignKey, Object value) {
        return jdbcClient.sql(foreignKey.sql()).param(KEY_FOREIGN_KEY, value).query(this::readRow).list();
    }

    private Map<String, Object> readRow(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int index = 1; index <= columnCount; index++) {
            String columnName = resultSet.getMetaData().getColumnLabel(index);
            Object value = normalizeValue(resultSet.getObject(index));
            if (value != null) {
                row.put(toCamelCase(columnName), value);
            }
        }
        return row;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof PGobject postgresJsonObject) {
            return readJson(postgresJsonObject.getValue());
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return value;
    }

    private Object readJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            throw new ProductExportException("Unable to read JSONB column", exception);
        }
    }

    private Map<String, Object> manifest(Map<String, Object> productJson,
                                         Map<String, Object> tenantJson,
                                         String callerSubject,
                                         String callerEmail,
                                         Map<String, Object> entityCounts,
                                         Instant exportedAt,
                                         Instant downloadExpiresAt) {
        Map<String, Object> product = new LinkedHashMap<>(productJson);
        product.put("tenantName", tenantJson.get("name"));
        Map<String, Object> requestedBy = new LinkedHashMap<>();
        requestedBy.put("subject", callerSubject);
        requestedBy.put("email", callerEmail);
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("aegisExportVersion", AEGIS_EXPORT_VERSION);
        manifest.put("exportedAt", exportedAt);
        manifest.put("trigger", TRIGGER_PRODUCT_DELETED);
        manifest.put("product", product);
        manifest.put("requestedBy", requestedBy);
        manifest.put("entityCounts", entityCounts);
        manifest.put("downloadExpiresAt", downloadExpiresAt);
        return manifest;
    }

    private Map<String, byte[]> jsonEntries(JsonEntryData data) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        ExportRows rows = data.rows;
        entries.put(ENTRY_MANIFEST, jsonBytes(data.manifest()));
        entries.put(ENTRY_PRODUCT, jsonBytes(data.productJson()));
        entries.put(ENTRY_MODULES, jsonBytes(rows.modules));
        entries.put(ENTRY_CONTENT, jsonBytes(rows.contentEntries));
        entries.put(ENTRY_PAGES, jsonBytes(rows.pages));
        entries.put(ENTRY_FORMS, jsonBytes(rows.forms));
        entries.put(ENTRY_SUBMISSIONS, jsonBytes(rows.submissions));
        entries.put(ENTRY_ASSETS, jsonBytes(rows.assets));
        entries.put(ENTRY_GRAPH_NODES, jsonBytes(rows.graphNodes));
        entries.put(ENTRY_GRAPH_EDGES, jsonBytes(rows.graphEdges));
        entries.put(ENTRY_USERS, jsonBytes(rows.assignments));
        entries.put(ENTRY_AUDIT, jsonBytes(rows.auditEvents));
        return Map.copyOf(entries);
    }

    private byte[] jsonBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new ProductExportException("Unable to serialize export JSON", exception);
        }
    }

    private List<ExportAssetFile> exportAssetFiles(List<Map<String, Object>> assets) {
        List<ExportAssetFile> files = new ArrayList<>();
        for (Map<String, Object> asset : assets) {
            files.add(new ExportAssetFile(
                    String.valueOf(asset.get("id")),
                    String.valueOf(asset.get("name")),
                    String.valueOf(asset.get("category")).toLowerCase(),
                    String.valueOf(asset.get("storageProvider")).toLowerCase(),
                    String.valueOf(asset.get("storageKey")),
                    ((Number) asset.getOrDefault("sizeBytes", 0L)).longValue()
            ));
        }
        return List.copyOf(files);
    }

    private String toCamelCase(String columnName) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char character : columnName.toCharArray()) {
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private static ObjectMapper exportObjectMapper(ObjectMapper source) {
        return source.copy()
                .registerModule(new JavaTimeModule())
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final class ExportRows {
        private List<Map<String, Object>> modules;
        private List<Map<String, Object>> contentEntries;
        private List<Map<String, Object>> pages;
        private List<Map<String, Object>> forms;
        private List<Map<String, Object>> submissions;
        private List<Map<String, Object>> assets;
        private List<Map<String, Object>> graphNodes;
        private List<Map<String, Object>> graphEdges;
        private List<Map<String, Object>> assignments;
        private List<Map<String, Object>> auditEvents;
    }

    private static final class JsonEntryData {
        private final Map<String, Object> manifest;
        private final Map<String, Object> productJson;
        private final ExportRows rows;

        private JsonEntryData(Map<String, Object> manifest, Map<String, Object> productJson, ExportRows rows) {
            this.manifest = manifest;
            this.productJson = productJson;
            this.rows = rows;
        }

        private Map<String, Object> manifest() {
            return manifest;
        }

        private Map<String, Object> productJson() {
            return productJson;
        }
    }

    private enum ExportTable {
        TENANTS("select * from tenants where id = :id", null),
        PRODUCT_MODULES(null, "select * from product_modules where product_id = :product_id order by id"),
        CONTENTS(null, "select * from contents where product_id = :product_id order by id"),
        PAGES(null, "select * from pages where product_id = :product_id order by id"),
        FORM_DEFINITIONS(null, "select * from form_definitions where product_id = :product_id order by id"),
        ASSETS(null, "select * from assets where product_id = :product_id order by id"),
        GRAPH_NODES(null, "select * from graph_nodes where product_id = :product_id order by id"),
        GRAPH_EDGES(null, "select * from graph_edges where product_id = :product_id order by id"),
        PRODUCT_ASSIGNMENTS(null, "select * from product_assignments where product_id = :product_id order by id"),
        AUDIT_EVENTS(null, "select * from audit_events where product_id = :product_id order by id");

        private final String byIdSql;
        private final String byProductSql;

        ExportTable(String byIdSql, String byProductSql) {
            this.byIdSql = byIdSql;
            this.byProductSql = byProductSql;
        }

        private String byIdSql() {
            return byIdSql;
        }

        private String byProductSql() {
            return byProductSql;
        }
    }

    private enum ExportForeignKey {
        CONTENT_VERSIONS_BY_CONTENT("select * from content_versions where content_id = :foreignKey order by id"),
        PAGE_SECTIONS_BY_PAGE("select * from page_sections where page_id = :foreignKey order by id");

        private final String sql;

        ExportForeignKey(String sql) {
            this.sql = sql;
        }

        private String sql() {
            return sql;
        }
    }
}
