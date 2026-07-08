-- ============================================================
-- V20__assets_module_test_products.sql
-- K.1.3 (BUG-SPRINT-05) — o produto Loki (b0000000-...-006) nao tinha o
-- modulo ASSETS habilitado, entao qualquer AssetPickerModal aberto nesse
-- produto recebia 403 do backend e exibia "Erro parcial" generico. Loki e o
-- produto usado nos fluxos de verificacao de midia/musica desta sprint
-- (J.2, J.5, I.2), entao precisa do modulo habilitado de ponta a ponta.
-- ============================================================

INSERT INTO product_modules (id, product_id, module_key, enabled, settings_json, created_at, updated_at)
SELECT 'a6000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000006', 'ASSETS', true, '{}'::jsonb, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM products WHERE id = 'b0000000-0000-0000-0000-000000000006')
ON CONFLICT (product_id, module_key) DO UPDATE SET enabled = true;
