-- ============================================================
-- V17__seed_homologacao.sql
-- Dados reais do tenant de homologacao CLIENTES BETA.
-- Idempotente: as insercoes usam constraints reais do schema atual.
-- ============================================================

ALTER TABLE product_security_settings
    ADD COLUMN IF NOT EXISTS telegram_alert_bot_token VARCHAR(128),
    ADD COLUMN IF NOT EXISTS telegram_alert_chat_id VARCHAR(64);

INSERT INTO tenants (id, key, name, plan, status, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'clientes-beta',
    'CLIENTES BETA',
    'PRO',
    'ACTIVE',
    NOW(),
    NOW()
)
ON CONFLICT (key) DO NOTHING;

INSERT INTO products (id, tenant_id, key, name, type, status, default_locale, asset_storage_strategy, created_at, updated_at)
VALUES
    ('b0000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'maestro-beton', 'Maestro Beton', 'SITE_INSTITUCIONAL', 'ACTIVE', 'pt-BR', 'LOCAL', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'conecta-talentos', 'Conecta Talentos', 'PORTAL', 'ACTIVE', 'pt-BR', 'LOCAL', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'alexandre-dev', 'Alexandre Dev', 'PORTFOLIO', 'ACTIVE', 'pt-BR', 'LOCAL', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000004', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'cmss', 'CMSS', 'SITE_INSTITUCIONAL', 'ACTIVE', 'pt-BR', 'LOCAL', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000005', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'wikidev', 'WikiDev', 'KNOWLEDGE_BASE', 'ARCHIVED', 'pt-BR', 'LOCAL', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000006', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'loki', 'Loki', 'LIBRARY_BOOKS_MUSIC', 'ACTIVE', 'pt-BR', 'LOCAL', NOW(), NOW())
ON CONFLICT (tenant_id, key) DO NOTHING;

INSERT INTO product_modules (id, product_id, module_key, enabled, settings_json, created_at, updated_at)
SELECT id::uuid, product_id::uuid, module_key, enabled, settings_json, created_at, updated_at
FROM (VALUES
    ('a1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'CONTENT', true, '{}'::jsonb, NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'PAGES', true, '{}'::jsonb, NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'ASSETS', true, '{}'::jsonb, NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'FORMS', true, '{}'::jsonb, NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'SEO', true, '{}'::jsonb, NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 'ANALYTICS', true, '{}'::jsonb, NOW(), NOW()),
    ('a1000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000001', 'KNOWLEDGE_GRAPH', true, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'CONTENT', true, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'PAGES', true, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'FORMS', true, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002', 'SEO', true, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000002', 'ANALYTICS', true, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000002', 'SUBMISSIONS', false, '{}'::jsonb, NOW(), NOW()),
    ('a2000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000002', 'JOBS', false, '{}'::jsonb, NOW(), NOW()),
    ('a3000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'PORTFOLIO', true, '{}'::jsonb, NOW(), NOW()),
    ('a3000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003', 'CONTENT', true, '{}'::jsonb, NOW(), NOW()),
    ('a3000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'PAGES', true, '{}'::jsonb, NOW(), NOW()),
    ('a3000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000003', 'ASSETS', true, '{}'::jsonb, NOW(), NOW()),
    ('a3000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000003', 'SEO', true, '{}'::jsonb, NOW(), NOW()),
    ('a3000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000003', 'ANALYTICS', true, '{}'::jsonb, NOW(), NOW()),
    ('a4000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'CONTENT', true, '{}'::jsonb, NOW(), NOW()),
    ('a4000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000004', 'PAGES', true, '{}'::jsonb, NOW(), NOW()),
    ('a4000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004', 'ASSETS', true, '{}'::jsonb, NOW(), NOW()),
    ('a4000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'FORMS', true, '{}'::jsonb, NOW(), NOW()),
    ('a4000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000004', 'SEO', true, '{}'::jsonb, NOW(), NOW()),
    ('a4000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000004', 'ANALYTICS', true, '{}'::jsonb, NOW(), NOW()),
    ('a5000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000005', 'CONTENT', true, '{}'::jsonb, NOW(), NOW()),
    ('a5000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000005', 'KNOWLEDGE_GRAPH', true, '{}'::jsonb, NOW(), NOW()),
    ('a5000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000005', 'SEO', true, '{}'::jsonb, NOW(), NOW()),
    ('a5000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000005', 'ANALYTICS', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000006', 'LIBRARY', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000006', 'BOOKS', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000006', 'MUSIC', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000006', 'CONTENT', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000006', 'KNOWLEDGE_GRAPH', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'SEO', true, '{}'::jsonb, NOW(), NOW()),
    ('a6000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000006', 'ANALYTICS', true, '{}'::jsonb, NOW(), NOW())
) AS seed(id, product_id, module_key, enabled, settings_json, created_at, updated_at)
WHERE EXISTS (SELECT 1 FROM products WHERE id = 'b0000000-0000-0000-0000-000000000001')
ON CONFLICT (product_id, module_key) DO NOTHING;

INSERT INTO pages (id, tenant_id, product_id, slug, title, locale, status, version, seo_no_index, created_at, updated_at)
SELECT id::uuid, tenant_id::uuid, product_id::uuid, slug, title, locale, status, version, seo_no_index,
       created_at, updated_at
FROM (VALUES
    ('c1000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'home', 'Home', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'quem-somos', 'Quem Somos', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'historia', 'História', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000004', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'agenda', 'Agenda', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000005', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'galeria', 'Galeria', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000006', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'apoie', 'Apoie', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c1000000-0000-0000-0000-000000000007', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'contato', 'Contato', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c2000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000004', 'home', 'Home', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c2000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000004', 'quem-somos', 'Quem Somos', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c2000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000004', 'historia', 'História', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c2000000-0000-0000-0000-000000000004', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000004', 'agenda', 'Agenda', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c2000000-0000-0000-0000-000000000005', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000004', 'apoie', 'Apoie', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c2000000-0000-0000-0000-000000000006', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000004', 'contato', 'Contato', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c3000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000002', 'home', 'Home', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW()),
    ('c4000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000003', 'home', 'Home', 'pt-BR', 'DRAFT', 1, false, NOW(), NOW())
) AS seed(id, tenant_id, product_id, slug, title, locale, status, version, seo_no_index, created_at, updated_at)
WHERE EXISTS (SELECT 1 FROM products WHERE id = 'b0000000-0000-0000-0000-000000000001')
ON CONFLICT (product_id, slug) DO NOTHING;

INSERT INTO page_sections (id, page_id, type, variant, section_order, content_json, settings_json, created_at, updated_at)
SELECT id::uuid, page_id::uuid, type, variant, section_order, content_json, settings_json, created_at, updated_at
FROM (VALUES
    ('d1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'HERO', null, 0, '{"title":"Maestro Beton","subtitle":"Eventos que ficam na memória","ctaLabel":"Solicitar orçamento","ctaUrl":"/contato","theme":"dark"}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'CARD_LIST', null, 1, '{"heading":"Serviços","source":"manual","limit":6,"items":[{"title":"Casamentos","description":""},{"title":"Eventos corporativos","description":""},{"title":"Shows","description":""}]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000001', 'GALLERY', null, 2, '{"heading":"Galeria","columns":3,"items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000001', 'EVENT_LIST', null, 3, '{"heading":"Agenda","source":"auto","contentType":"evento","upcoming":true,"limit":6}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000005', 'c1000000-0000-0000-0000-000000000001', 'CONTACT', null, 4, '{"heading":"Contato / Orçamento","formId":"","address":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000010', 'c1000000-0000-0000-0000-000000000002', 'IMAGE_TEXT', null, 0, '{"image":"","text":"","imagePosition":"left"}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000011', 'c1000000-0000-0000-0000-000000000002', 'TWO_COLUMN', null, 1, '{"left":"","right":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000020', 'c1000000-0000-0000-0000-000000000003', 'TIMELINE', null, 0, '{"heading":"Nossa História","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000030', 'c1000000-0000-0000-0000-000000000004', 'EVENT_LIST', null, 0, '{"heading":"Agenda","source":"auto","contentType":"evento","upcoming":true,"limit":12}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000040', 'c1000000-0000-0000-0000-000000000005', 'GALLERY', null, 0, '{"heading":"Galeria","columns":3,"items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000050', 'c1000000-0000-0000-0000-000000000006', 'RICH_TEXT', null, 0, '{"html":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000051', 'c1000000-0000-0000-0000-000000000006', 'FAQ', null, 1, '{"heading":"Dúvidas frequentes","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d1000000-0000-0000-0000-000000000060', 'c1000000-0000-0000-0000-000000000007', 'CONTACT', null, 0, '{"heading":"Fale Conosco","formId":"","address":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001', 'HERO', null, 0, '{"title":"CMSS","subtitle":"","ctaLabel":"","ctaUrl":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000001', 'FEATURE_GRID', null, 1, '{"heading":"Pilares","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000003', 'c2000000-0000-0000-0000-000000000001', 'EVENT_LIST', null, 2, '{"heading":"Próximos eventos","source":"auto","upcoming":true,"limit":4}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000004', 'c2000000-0000-0000-0000-000000000001', 'CTA_SECTION', null, 3, '{"heading":"Apoie o CMSS","ctaLabel":"Saiba como","ctaUrl":"/apoie"}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000010', 'c2000000-0000-0000-0000-000000000002', 'IMAGE_TEXT', null, 0, '{"image":"","text":"","imagePosition":"left"}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000011', 'c2000000-0000-0000-0000-000000000002', 'TWO_COLUMN', null, 1, '{"left":"","right":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000020', 'c2000000-0000-0000-0000-000000000003', 'TIMELINE', null, 0, '{"heading":"Nossa Trajetória","items":[{"year":"1950","title":"Fundação","description":""},{"year":"1985","title":"Expansão","description":""},{"year":"2010","title":"Era digital","description":""},{"year":"2023","title":"Hoje","description":""}]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000030', 'c2000000-0000-0000-0000-000000000004', 'EVENT_LIST', null, 0, '{"heading":"Agenda","source":"auto","upcoming":true,"limit":12}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000040', 'c2000000-0000-0000-0000-000000000005', 'RICH_TEXT', null, 0, '{"html":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000041', 'c2000000-0000-0000-0000-000000000005', 'FAQ', null, 1, '{"heading":"Dúvidas frequentes","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d2000000-0000-0000-0000-000000000050', 'c2000000-0000-0000-0000-000000000006', 'CONTACT', null, 0, '{"heading":"Fale Conosco","formId":"","address":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d3000000-0000-0000-0000-000000000001', 'c3000000-0000-0000-0000-000000000001', 'HERO', null, 0, '{"title":"Conecta Talentos","subtitle":"Encontre sua próxima oportunidade","ctaLabel":"Ver vagas","ctaUrl":"#vagas"}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d3000000-0000-0000-0000-000000000002', 'c3000000-0000-0000-0000-000000000001', 'RICH_TEXT', null, 1, '{"html":""}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d3000000-0000-0000-0000-000000000003', 'c3000000-0000-0000-0000-000000000001', 'CARD_LIST', null, 2, '{"heading":"Vagas abertas","source":"auto","limit":6,"items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d3000000-0000-0000-0000-000000000004', 'c3000000-0000-0000-0000-000000000001', 'CARD_LIST', null, 3, '{"heading":"Blog","source":"auto","contentType":"artigo","limit":3}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d4000000-0000-0000-0000-000000000001', 'c4000000-0000-0000-0000-000000000001', 'HERO', null, 0, '{"title":"Alexandre Silva","subtitle":"Desenvolvedor de software","ctaLabel":"Ver projetos","ctaUrl":"#projetos"}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d4000000-0000-0000-0000-000000000002', 'c4000000-0000-0000-0000-000000000001', 'CARD_LIST', null, 1, '{"heading":"Projetos","source":"manual","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d4000000-0000-0000-0000-000000000003', 'c4000000-0000-0000-0000-000000000001', 'FEATURE_GRID', null, 2, '{"heading":"Skills","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d4000000-0000-0000-0000-000000000004', 'c4000000-0000-0000-0000-000000000001', 'TIMELINE', null, 3, '{"heading":"Experiência","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW()),
    ('d4000000-0000-0000-0000-000000000005', 'c4000000-0000-0000-0000-000000000001', 'DOWNLOAD', null, 4, '{"heading":"Downloads","items":[]}'::jsonb, '{}'::jsonb, NOW(), NOW())
) AS seed(id, page_id, type, variant, section_order, content_json, settings_json, created_at, updated_at)
WHERE EXISTS (SELECT 1 FROM pages WHERE id = 'c1000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO graph_nodes (id, tenant_id, product_id, node_type, ref_type, ref_id, label, slug, metadata_json, x, y, created_at, updated_at)
SELECT id::uuid, tenant_id::uuid, product_id::uuid, node_type, ref_type, ref_id, label, slug, metadata_json,
       x, y, created_at, updated_at
FROM (VALUES
    ('e5000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000005', 'TOPIC', 'TOPIC', 'spring-boot', 'Spring Boot', 'spring-boot', '{"summary":"Framework para criar aplicações Java prontas para produção com configuração mínima.","difficulty":"beginner","status":"ativo"}'::jsonb, 200, 100, NOW(), NOW()),
    ('e5000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000005', 'TOPIC', 'TOPIC', 'jpa', 'JPA', 'jpa', '{"summary":"Especificação Java para mapeamento objeto-relacional.","difficulty":"intermediate","status":"ativo"}'::jsonb, 400, 100, NOW(), NOW()),
    ('e5000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000005', 'TOPIC', 'TOPIC', 'docker', 'Docker', 'docker', '{"summary":"Plataforma de containerização para empacotar e executar aplicações.","difficulty":"beginner","status":"ativo"}'::jsonb, 300, 300, NOW(), NOW()),
    ('e6000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'POEM', 'POEM', 'vigilia', 'Vigília', 'vigilia', '{"status":"ativo"}'::jsonb, 200, 100, NOW(), NOW()),
    ('e6000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'MUSIC_REF', 'MUSIC_REF', 'clair-de-lune', 'Clair de Lune - Debussy', 'clair-de-lune', '{"status":"ativo"}'::jsonb, 400, 100, NOW(), NOW()),
    ('e6000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'MANIFEST', 'MANIFEST', 'manifesto-silencio', 'Manifesto do Silêncio', 'manifesto-silencio', '{"status":"ativo"}'::jsonb, 200, 300, NOW(), NOW()),
    ('e6000000-0000-0000-0000-000000000004', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'PLAYLIST', 'PLAYLIST', 'playlist-intros', 'Playlist: Introspecção', 'playlist-intros', '{"status":"ativo"}'::jsonb, 400, 300, NOW(), NOW()),
    ('e6000000-0000-0000-0000-000000000005', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'BOOK', 'BOOK', 'fragmentos', 'Fragmentos', 'fragmentos', '{"status":"ativo"}'::jsonb, 300, 500, NOW(), NOW()),
    ('e1000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'PRODUCT', 'PRODUCT', 'mb-produto', 'Maestro Beton', 'mb-produto', '{"status":"ativo"}'::jsonb, 300, 200, NOW(), NOW()),
    ('e1000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'PAGE', 'PAGE', 'mb-pagina-home', 'Página Home', 'mb-pagina-home', '{"status":"ativo"}'::jsonb, 200, 100, NOW(), NOW()),
    ('e1000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'FORM', 'FORM', 'mb-form-orcamento', 'Form: Orçamento', 'mb-form-orcamento', '{"status":"ativo"}'::jsonb, 400, 100, NOW(), NOW()),
    ('e1000000-0000-0000-0000-000000000004', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'CATEGORY', 'CATEGORY', 'mb-cat-inst', 'Institucional', 'mb-cat-inst', '{"status":"ativo"}'::jsonb, 300, 400, NOW(), NOW()),
    ('e1000000-0000-0000-0000-000000000005', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'CONTENT', 'AUTHOR', 'mb-autor-marina', 'Marina Costa', 'mb-autor-marina', '{"status":"ativo"}'::jsonb, 100, 300, NOW(), NOW())
) AS seed(id, tenant_id, product_id, node_type, ref_type, ref_id, label, slug, metadata_json, x, y, created_at, updated_at)
WHERE EXISTS (SELECT 1 FROM products WHERE id = 'b0000000-0000-0000-0000-000000000001')
ON CONFLICT (product_id, ref_type, ref_id) DO NOTHING;

INSERT INTO graph_edges (id, tenant_id, product_id, source_node_id, target_node_id, edge_type, weight, metadata_json, created_at, updated_at)
SELECT id::uuid, tenant_id::uuid, product_id::uuid, source_node_id::uuid, target_node_id::uuid, edge_type,
       weight, metadata_json, created_at, updated_at
FROM (VALUES
    ('f5000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000005', 'e5000000-0000-0000-0000-000000000001', 'e5000000-0000-0000-0000-000000000002', 'INTEGRATES_WITH', 9, '{}'::jsonb, NOW(), NOW()),
    ('f5000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000005', 'e5000000-0000-0000-0000-000000000001', 'e5000000-0000-0000-0000-000000000003', 'PACKAGED_BY', 6, '{}'::jsonb, NOW(), NOW()),
    ('f6000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'e6000000-0000-0000-0000-000000000001', 'e6000000-0000-0000-0000-000000000002', 'INSPIRED_BY', 10, '{}'::jsonb, NOW(), NOW()),
    ('f6000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'e6000000-0000-0000-0000-000000000003', 'e6000000-0000-0000-0000-000000000004', 'INSPIRED_BY', 8, '{}'::jsonb, NOW(), NOW()),
    ('f6000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000006', 'e6000000-0000-0000-0000-000000000001', 'e6000000-0000-0000-0000-000000000005', 'PART_OF', 5, '{}'::jsonb, NOW(), NOW()),
    ('f1000000-0000-0000-0000-000000000001', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002', 'CONTAINS', 1, '{}'::jsonb, NOW(), NOW()),
    ('f1000000-0000-0000-0000-000000000002', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000003', 'CONTAINS', 1, '{}'::jsonb, NOW(), NOW()),
    ('f1000000-0000-0000-0000-000000000003', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000004', 'BELONGS_TO', 1, '{}'::jsonb, NOW(), NOW()),
    ('f1000000-0000-0000-0000-000000000004', (SELECT id FROM tenants WHERE key = 'clientes-beta'), 'b0000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000005', 'WRITTEN_BY', 1, '{}'::jsonb, NOW(), NOW())
) AS seed(id, tenant_id, product_id, source_node_id, target_node_id, edge_type, weight, metadata_json, created_at, updated_at)
WHERE EXISTS (SELECT 1 FROM graph_nodes WHERE id = 'e1000000-0000-0000-0000-000000000001')
ON CONFLICT (source_node_id, target_node_id, edge_type) DO NOTHING;
