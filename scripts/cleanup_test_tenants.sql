-- =============================================================================
-- cleanup_test_tenants.sql
-- Remove todos os tenants de teste e seus dados em cascata.
--
-- PRESERVA (seed V17__seed_homologacao.sql):
--   Tenant  : a0000000-0000-0000-0000-000000000001  (CLIENTES BETA)
--   Produtos: b0000000-0000-0000-0000-000000000001  (Maestro Beton)
--             b0000000-0000-0000-0000-000000000002  (Conecta Talentos)
--             b0000000-0000-0000-0000-000000000003  (Alexandre Dev)
--             b0000000-0000-0000-0000-000000000004  (CMSS)
--             b0000000-0000-0000-0000-000000000005  (WikiDev)
--             b0000000-0000-0000-0000-000000000006  (Loki)
--
-- USO:
--   1. Rode o bloco PREVIEW separado para conferir o que será removido.
--   2. Se estiver correto, rode o bloco BEGIN..COMMIT.
--   3. Em caso de dúvida: substitua COMMIT por ROLLBACK.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- PREVIEW — rode isso antes para ver o que vai ser apagado
-- ---------------------------------------------------------------------------
SELECT
    t.id,
    t.key,
    t.name,
    t.status,
    count(p.id) AS produtos
FROM tenants t
LEFT JOIN products p ON p.tenant_id = t.id
WHERE t.id != 'a0000000-0000-0000-0000-000000000001'
GROUP BY t.id, t.key, t.name, t.status
ORDER BY t.name;

-- ---------------------------------------------------------------------------
-- LIMPEZA — wrappado em transação para poder fazer ROLLBACK se necessário
-- ---------------------------------------------------------------------------
BEGIN;

-- Helper: IDs dos tenants que serão removidos
-- (usado nas subqueries abaixo)

-- 1. Knowledge Graph --------------------------------------------------------
DELETE FROM graph_edges
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

DELETE FROM graph_insight_reviews
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

DELETE FROM graph_nodes
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 2. Pages (page_sections tem ON DELETE CASCADE → apagadas junto) -----------
DELETE FROM pages
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 3. Formulários ------------------------------------------------------------
-- form_submissions: ON DELETE CASCADE em form_definitions → apaga junto
DELETE FROM form_definitions
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 4. Conteúdo ---------------------------------------------------------------
-- content_versions: ON DELETE CASCADE em contents → apaga junto
DELETE FROM contents
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 5. Assets -----------------------------------------------------------------
-- asset_tag_assignments: ON DELETE CASCADE em assets e asset_tags → apaga junto
-- asset_usages: ON DELETE CASCADE em assets → apaga junto
-- asset_tags: ON DELETE CASCADE em products → apaga junto com products abaixo
DELETE FROM assets
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 6. Auditoria e exports ----------------------------------------------------
-- audit_events NÃO tem ON DELETE CASCADE → delete explícito
DELETE FROM audit_events
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- export_tokens não tem tenant_id, referenciam product_id
DELETE FROM export_tokens
WHERE product_id IN (
    SELECT id FROM products
    WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001'
);

-- 7. Feedback (ON DELETE CASCADE em tenant_id, mas sendo explícito) ---------
DELETE FROM feedback
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 8. Sub-tabelas de produto -------------------------------------------------
-- product_modules, product_security_settings, product_globals:
--   têm ON DELETE CASCADE em product_id → apagadas junto com products
-- product_assignments: tem tenant_id
DELETE FROM product_assignments
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 9. Tenant memberships -----------------------------------------------------
DELETE FROM tenant_memberships
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 10. Produtos (cascata: product_modules, product_security_settings,
--     product_globals, asset_tags, asset_tag_assignments, asset_usages) -----
DELETE FROM products
WHERE tenant_id != 'a0000000-0000-0000-0000-000000000001';

-- 11. Tenants ---------------------------------------------------------------
DELETE FROM tenants
WHERE id != 'a0000000-0000-0000-0000-000000000001';

-- ---------------------------------------------------------------------------
-- Verificação pós-limpeza
-- ---------------------------------------------------------------------------
SELECT 'tenants_restantes'  AS tabela, count(*) AS total FROM tenants;
SELECT 'produtos_restantes' AS tabela, count(*) AS total FROM products;
SELECT 'memberships_rest.'  AS tabela, count(*) AS total FROM tenant_memberships;

-- Se os números estiverem corretos → COMMIT
-- Se algo estiver errado      → ROLLBACK
COMMIT;
-- ROLLBACK;
