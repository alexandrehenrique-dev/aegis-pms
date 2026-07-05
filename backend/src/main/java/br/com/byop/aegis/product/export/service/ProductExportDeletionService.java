package br.com.byop.aegis.product.export.service;

import br.com.byop.aegis.product.api.ProductExportStoragePort;
import br.com.byop.aegis.product.export.dto.ExportAssetFile;
import br.com.byop.aegis.product.export.dto.ProductExportData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class ProductExportDeletionService {

    private static final String PARAM_PRODUCT_ID = "productId";

    private final JdbcClient jdbcClient;
    private final ProductExportStoragePort storagePort;

    public ProductExportDeletionService(JdbcClient jdbcClient, ProductExportStoragePort storagePort) {
        this.jdbcClient = jdbcClient;
        this.storagePort = storagePort;
    }

    @Transactional
    public void deleteExportedProduct(ProductExportData data) {
        log.debug("deleteExportedProduct: productId='{}'", data.productId());
        data.assets().forEach(this::deleteAssetFile);
        deleteProductRows(data.productId());
        log.info("deleteExportedProduct: produto excluido apos exportacao productId='{}'", data.productId());
    }

    /**
     * Mesma exclusão de {@link #deleteExportedProduct}, mas sem tentar apagar os
     * arquivos de asset no armazenamento — usado quando o backend de storage não
     * está configurado (ex.: S3 sem bucket) e portanto nunca teve os arquivos
     * gravados de fato; tentar excluí-los resultaria em erro ou seria um no-op
     * enganoso.
     */
    @Transactional
    public void deleteExportedProductSkippingAssetFiles(ProductExportData data) {
        log.debug("deleteExportedProductSkippingAssetFiles: productId='{}'", data.productId());
        deleteProductRows(data.productId());
        log.info("deleteExportedProductSkippingAssetFiles: produto excluido sem backup productId='{}'", data.productId());
    }

    private void deleteProductRows(UUID productId) {
        delete("delete from audit_events where product_id = :productId", productId);
        delete("delete from form_submissions where form_id in (select id from form_definitions where product_id = :productId)", productId);
        delete("delete from form_definitions where product_id = :productId", productId);
        delete("delete from page_sections where page_id in (select id from pages where product_id = :productId)", productId);
        delete("delete from pages where product_id = :productId", productId);
        delete("delete from product_globals where product_id = :productId", productId);
        delete("delete from events where product_id = :productId", productId);
        delete("delete from content_versions where content_id in (select id from contents where product_id = :productId)", productId);
        delete("delete from contents where product_id = :productId", productId);
        delete("delete from graph_edges where product_id = :productId", productId);
        delete("delete from graph_insight_reviews where product_id = :productId", productId);
        delete("delete from graph_nodes where product_id = :productId", productId);
        delete("delete from asset_tag_assignments where asset_id in (select id from assets where product_id = :productId)", productId);
        delete("delete from asset_usages where asset_id in (select id from assets where product_id = :productId)", productId);
        delete("delete from asset_tags where product_id = :productId", productId);
        delete("delete from assets where product_id = :productId", productId);
        delete("delete from product_modules where product_id = :productId", productId);
        delete("delete from product_assignments where product_id = :productId", productId);
        delete("delete from products where id = :productId", productId);
    }

    private void deleteAssetFile(ExportAssetFile asset) {
        storagePort.deleteAsset(asset.storageProvider(), asset.storageKey());
    }

    private void delete(String sql, UUID productId) {
        jdbcClient.sql(sql).param(PARAM_PRODUCT_ID, productId).update();
    }
}
