# Etapa 30 — Migration de dados reais de homologação e remoção do seed Java

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: todas as etapas 26–29 concluídas (pois esta usa o próximo número de migration Flyway disponível após as delas). Esta é a **última etapa do backend**.
>
> **Motivação:** a etapa 22 criou `DemoSeedService.java` + `DemoSeedRunner.java` com `@Profile("local")` para popular o banco em desenvolvimento. Isso tem dois problemas críticos:
> 1. Nunca roda em homologação nem em produção — os produtos dos clientes beta nunca existem nesses ambientes.
> 2. Não cria páginas, seções nem Knowledge Graph com estrutura real — o sistema aparece completamente vazio para quem abre pela primeira vez.
>
> Esta etapa remove o package `seed` Java inteiramente e cria uma migration SQL que roda em **todos** os ambientes com os dados reais do tenant **CLIENTES BETA** — os produtos dos amigos do Alexandre, que serão usados para homologar o Aegis. Depois é só fazer o invite dos usuários reais via `/invite`.
>
> **Branch:** `feat/etapa-30-seed-homologacao`

---

## A. Remover o package `seed` Java

### A.1 — Apagar os dois arquivos do package

```
backend/src/main/java/br/com/byop/aegis/seed/DemoSeedRunner.java   ← DELETAR
backend/src/main/java/br/com/byop/aegis/seed/DemoSeedService.java  ← DELETAR
```

Verificar que nenhum outro arquivo do projeto importa essas classes:

```bash
grep -r "DemoSeedRunner\|DemoSeedService" backend/src/ --include="*.java"
# esperado: zero resultados após deletar
```

### A.2 — Remover NamedInterfaces criadas exclusivamente para o seed

O `DemoSeedService` consumia interfaces via Spring Modulith. Para cada uma abaixo, verificar se tem outros consumidores além do seed:

```bash
grep -r "TenantSeedService\|ProductSeedService\|KnowledgeGraphSeedService\|NotificationOnboardingService" \
  backend/src/ --include="*.java"
```

**Regra:** se o único caller era `DemoSeedService` → deletar a interface **e** sua implementação `@Service`. Atualizar o `module-info.java` (ou descritor Spring Modulith equivalente) removendo o `@NamedInterface` correspondente.

> `NotificationOnboardingService` pode ter sido reaproveitada pela etapa 25 (onboarding de novos usuários no convite) — verificar antes de deletar.

### A.3 — Remover propriedade de seed do `application-local.yml`

```yaml
# REMOVER completamente este bloco:
aegis:
  seed:
    demo-user-initial-credential: senha123
```

### A.4 — Ajustar testes que dependiam do seed

```bash
grep -r "DemoSeed\|TenantSeedService\|ProductSeedService\|KnowledgeGraphSeedService" \
  backend/src/test/ --include="*.java"
```

Para cada ocorrência: substituir por fixture SQL inline via `@Sql` ou builder de teste no `@BeforeEach`. Nunca mockar o seed — o dado deve entrar pelo mesmo caminho que entraria em produção (a migration).

---

## B. Adicionar valores ao enum `GraphNodeType`

O enum Java `GraphNodeType` (e o tipo PostgreSQL correspondente) precisam de novos valores para os nodes do Loki e WikiDev. Se esses valores já foram adicionados nas etapas 26–29, pular esta seção.

**Verificação:**

```bash
grep -r "TOPIC\|POEM\|MUSIC_REF\|MANIFEST\|BOOK\|PLAYLIST\|TENANT" \
  backend/src/main/java/ --include="*.java" | grep "GraphNodeType"
```

Adicionar em `GraphNodeType.java` os que faltarem:

```java
TOPIC,      // tópico de conhecimento (WikiDev)
POEM,       // poema (Loki)
MUSIC_REF,  // referência musical — faixa/álbum externo (Loki)
MANIFEST,   // manifesto filosófico (Loki)
BOOK,       // livro (Loki)
PLAYLIST,   // playlist (Loki)
TENANT,     // nó raiz de tenant no grafo (pode já existir desde etapa 08)
```

Esses valores serão inseridos no PostgreSQL via `ALTER TYPE` no início da migration (Seção C).

---

## C. Migration `V_NEXT__seed_homologacao.sql`

> **Número da migration:** usar o próximo número disponível após as migrations das etapas 26–29. Verificar com:
> ```bash
> ls backend/src/main/resources/db/migration/ | sort
> ```
> e usar `V{N+1}__seed_homologacao.sql` onde `N` é o número da última migration existente.
>
> **Idempotência:** toda inserção usa `ON CONFLICT ... DO NOTHING`. A migration pode ser re-executada em banco já populado sem erro.
>
> **Verificação prévia obrigatória:** antes de escrever o SQL final, confirmar nomes exatos de colunas lendo:
> ```bash
> cat backend/src/main/resources/db/migration/V2__core_tenant_product_modules.sql
> cat backend/src/main/resources/db/migration/V3__knowledge_graph_mvp.sql
> cat backend/src/main/resources/db/migration/V12__pages_sections_globals_events.sql
> ```

### C.1 — Cabeçalho e enum additions

```sql
-- ============================================================
-- V_NEXT__seed_homologacao.sql
-- Dados reais do tenant de homologação CLIENTES BETA.
-- Idempotente: ON CONFLICT ... DO NOTHING em todas as inserções.
-- Roda em todos os ambientes (dev, staging, produção).
-- ============================================================

-- Enum additions ANTES de qualquer transação de dados (não revertíveis em rollback)
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'TOPIC';
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'POEM';
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'MUSIC_REF';
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'MANIFEST';
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'BOOK';
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'PLAYLIST';
ALTER TYPE graph_node_type ADD VALUE IF NOT EXISTS 'TENANT';
```

### C.2 — Tenant CLIENTES BETA

```sql
INSERT INTO tenants (id, key, name, plan, status, created_at)
VALUES (
  'a0000000-0000-0000-0000-000000000001',
  'clientes-beta',
  'CLIENTES BETA',
  'PRO',
  'ACTIVE',
  NOW()
)
ON CONFLICT (key) DO NOTHING;
```

> **Nota:** se a etapa 22 (seed Java) já persistiu um tenant com key `clientes-beta` em banco de dev, o `ON CONFLICT DO NOTHING` garante que não duplica — mas o UUID pode divergir. Para ambientes de dev que rodaram a etapa 22, limpar o dado antigo antes de aplicar a migration, ou aceitar a coexistência (o seed Java não será mais executado após a remoção do package).

### C.3 — Produtos e módulos

```sql
-- 6 produtos reais do tenant CLIENTES BETA
-- (verificar nome exato de colunas contra V2 antes de executar)
INSERT INTO products (id, key, name, type, tenant_id, status, default_locale, created_at)
VALUES
  ('b0000000-0000-0000-0000-000000000001', 'maestro-beton',    'Maestro Beton',    'Site Institucional',  'a0000000-0000-0000-0000-000000000001', 'ACTIVE',   'pt-BR', NOW()),
  ('b0000000-0000-0000-0000-000000000002', 'conecta-talentos', 'Conecta Talentos', 'Portal',              'a0000000-0000-0000-0000-000000000001', 'ACTIVE',   'pt-BR', NOW()),
  ('b0000000-0000-0000-0000-000000000003', 'alexandre-dev',    'Alexandre Dev',    'Portfolio',           'a0000000-0000-0000-0000-000000000001', 'ACTIVE',   'pt-BR', NOW()),
  ('b0000000-0000-0000-0000-000000000004', 'cmss',             'CMSS',             'Site Institucional',  'a0000000-0000-0000-0000-000000000001', 'ACTIVE',   'pt-BR', NOW()),
  ('b0000000-0000-0000-0000-000000000005', 'wikidev',          'WikiDev',          'Knowledge Base',      'a0000000-0000-0000-0000-000000000001', 'ARCHIVED', 'pt-BR', NOW()),
  ('b0000000-0000-0000-0000-000000000006', 'loki',             'Loki',             'Library/Books/Music', 'a0000000-0000-0000-0000-000000000001', 'ACTIVE',   'pt-BR', NOW())
ON CONFLICT (key) DO NOTHING;

-- Módulos por produto
-- (verificar nome da tabela e colunas: podem ser product_modules(product_id, module_key, enabled)
--  ou product_module_assignments — ajustar conforme V2)
INSERT INTO product_modules (product_id, module_key, enabled) VALUES
  -- Maestro Beton — Site Institucional
  ('b0000000-0000-0000-0000-000000000001', 'CONTENT',         true),
  ('b0000000-0000-0000-0000-000000000001', 'PAGES',           true),
  ('b0000000-0000-0000-0000-000000000001', 'ASSETS',          true),
  ('b0000000-0000-0000-0000-000000000001', 'FORMS',           true),
  ('b0000000-0000-0000-0000-000000000001', 'SEO',             true),
  ('b0000000-0000-0000-0000-000000000001', 'ANALYTICS',       true),
  ('b0000000-0000-0000-0000-000000000001', 'KNOWLEDGE_GRAPH', false),
  -- Conecta Talentos — Portal
  ('b0000000-0000-0000-0000-000000000002', 'CONTENT',         true),
  ('b0000000-0000-0000-0000-000000000002', 'PAGES',           true),
  ('b0000000-0000-0000-0000-000000000002', 'FORMS',           true),
  ('b0000000-0000-0000-0000-000000000002', 'SEO',             true),
  ('b0000000-0000-0000-0000-000000000002', 'ANALYTICS',       true),
  ('b0000000-0000-0000-0000-000000000002', 'SUBMISSIONS',     false),
  ('b0000000-0000-0000-0000-000000000002', 'JOBS',            false),
  -- Alexandre Dev — Portfolio
  ('b0000000-0000-0000-0000-000000000003', 'PORTFOLIO',       true),
  ('b0000000-0000-0000-0000-000000000003', 'CONTENT',         true),
  ('b0000000-0000-0000-0000-000000000003', 'PAGES',           true),
  ('b0000000-0000-0000-0000-000000000003', 'ASSETS',          true),
  ('b0000000-0000-0000-0000-000000000003', 'SEO',             true),
  ('b0000000-0000-0000-0000-000000000003', 'ANALYTICS',       true),
  -- CMSS — Site Institucional
  ('b0000000-0000-0000-0000-000000000004', 'CONTENT',         true),
  ('b0000000-0000-0000-0000-000000000004', 'PAGES',           true),
  ('b0000000-0000-0000-0000-000000000004', 'ASSETS',          true),
  ('b0000000-0000-0000-0000-000000000004', 'FORMS',           true),
  ('b0000000-0000-0000-0000-000000000004', 'SEO',             true),
  ('b0000000-0000-0000-0000-000000000004', 'ANALYTICS',       true),
  -- WikiDev — Knowledge Base (arquivado mas com grafo)
  ('b0000000-0000-0000-0000-000000000005', 'CONTENT',         true),
  ('b0000000-0000-0000-0000-000000000005', 'KNOWLEDGE_GRAPH', true),
  ('b0000000-0000-0000-0000-000000000005', 'SEO',             true),
  ('b0000000-0000-0000-0000-000000000005', 'ANALYTICS',       true),
  -- Loki — Library/Books/Music
  ('b0000000-0000-0000-0000-000000000006', 'LIBRARY',         true),
  ('b0000000-0000-0000-0000-000000000006', 'BOOKS',           true),
  ('b0000000-0000-0000-0000-000000000006', 'MUSIC',           true),
  ('b0000000-0000-0000-0000-000000000006', 'CONTENT',         true),
  ('b0000000-0000-0000-0000-000000000006', 'KNOWLEDGE_GRAPH', true),
  ('b0000000-0000-0000-0000-000000000006', 'SEO',             true),
  ('b0000000-0000-0000-0000-000000000006', 'ANALYTICS',       true)
ON CONFLICT (product_id, module_key) DO NOTHING;
```

### C.4 — Páginas e seções: Maestro Beton (7 páginas)

> Verificar nomes de colunas contra V12 antes de executar. Colunas prováveis de `pages`: `id, product_id, slug, title, locale, status, version, created_at`. Colunas prováveis de `sections`: `id, page_id, type, label, order_index, content_json, created_at`.

```sql
-- Páginas de Maestro Beton
INSERT INTO pages (id, product_id, slug, title, locale, status, version, created_at) VALUES
  ('c1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'home',       'Home',       'pt-BR', 'DRAFT', 1, NOW()),
  ('c1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'quem-somos', 'Quem Somos', 'pt-BR', 'DRAFT', 1, NOW()),
  ('c1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'historia',   'História',   'pt-BR', 'DRAFT', 1, NOW()),
  ('c1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'agenda',     'Agenda',     'pt-BR', 'DRAFT', 1, NOW()),
  ('c1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'galeria',    'Galeria',    'pt-BR', 'DRAFT', 1, NOW()),
  ('c1000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 'apoie',      'Apoie',      'pt-BR', 'DRAFT', 1, NOW()),
  ('c1000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000001', 'contato',    'Contato',    'pt-BR', 'DRAFT', 1, NOW())
ON CONFLICT (product_id, slug) DO NOTHING;

-- Seções da Home — baseadas no mock frontend (pages.mocks.ts, maestro-beton)
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', 'hero',       'Hero',               0, '{"title":"Maestro Beton","subtitle":"Eventos que ficam na memória","ctaLabel":"Solicitar orçamento","ctaUrl":"/contato","theme":"dark"}', NOW()),
  ('d1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001', 'card-list',  'Serviços',           1, '{"heading":"Serviços","source":"manual","limit":6,"items":[{"title":"Casamentos","description":""},{"title":"Eventos corporativos","description":""},{"title":"Shows","description":""}]}', NOW()),
  ('d1000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000001', 'gallery',    'Galeria',            2, '{"heading":"Galeria","columns":3,"items":[]}', NOW()),
  ('d1000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000001', 'event-list', 'Agenda',             3, '{"heading":"Agenda","source":"auto","contentType":"evento","upcoming":true,"limit":6}', NOW()),
  ('d1000000-0000-0000-0000-000000000005', 'c1000000-0000-0000-0000-000000000001', 'contact',    'Contato/Orçamento',  4, '{"heading":"Contato / Orçamento","formId":"","address":""}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Seções Quem Somos
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000010', 'c1000000-0000-0000-0000-000000000002', 'image-text', 'Sobre',   0, '{"image":"","text":"","imagePosition":"left"}', NOW()),
  ('d1000000-0000-0000-0000-000000000011', 'c1000000-0000-0000-0000-000000000002', 'two-column', 'Valores', 1, '{"left":"","right":""}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Seções História
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000020', 'c1000000-0000-0000-0000-000000000003', 'timeline', 'Nossa História', 0, '{"heading":"Nossa História","items":[]}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Seções Agenda
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000030', 'c1000000-0000-0000-0000-000000000004', 'event-list', 'Agenda', 0, '{"heading":"Agenda","source":"auto","contentType":"evento","upcoming":true,"limit":12}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Seções Galeria
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000040', 'c1000000-0000-0000-0000-000000000005', 'gallery', 'Galeria', 0, '{"heading":"Galeria","columns":3,"items":[]}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Seções Apoie
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000050', 'c1000000-0000-0000-0000-000000000006', 'rich-text', 'Por que apoiar?', 0, '{"html":""}', NOW()),
  ('d1000000-0000-0000-0000-000000000051', 'c1000000-0000-0000-0000-000000000006', 'faq',       'Dúvidas',         1, '{"heading":"Dúvidas frequentes","items":[]}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Seções Contato
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d1000000-0000-0000-0000-000000000060', 'c1000000-0000-0000-0000-000000000007', 'contact', 'Contato', 0, '{"heading":"Fale Conosco","formId":"","address":""}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;
```

### C.5 — Páginas e seções: CMSS (6 páginas)

```sql
INSERT INTO pages (id, product_id, slug, title, locale, status, version, created_at) VALUES
  ('c2000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', 'home',       'Home',       'pt-BR', 'DRAFT', 1, NOW()),
  ('c2000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000004', 'quem-somos', 'Quem Somos', 'pt-BR', 'DRAFT', 1, NOW()),
  ('c2000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004', 'historia',   'História',   'pt-BR', 'DRAFT', 1, NOW()),
  ('c2000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'agenda',     'Agenda',     'pt-BR', 'DRAFT', 1, NOW()),
  ('c2000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000004', 'apoie',      'Apoie',      'pt-BR', 'DRAFT', 1, NOW()),
  ('c2000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000004', 'contato',    'Contato',    'pt-BR', 'DRAFT', 1, NOW())
ON CONFLICT (product_id, slug) DO NOTHING;

-- Home CMSS
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d2000000-0000-0000-0000-000000000001', 'c2000000-0000-0000-0000-000000000001', 'hero',         'Hero',             0, '{"title":"CMSS","subtitle":"","ctaLabel":"","ctaUrl":""}', NOW()),
  ('d2000000-0000-0000-0000-000000000002', 'c2000000-0000-0000-0000-000000000001', 'feature-grid', 'Pilares',          1, '{"heading":"Pilares","items":[]}', NOW()),
  ('d2000000-0000-0000-0000-000000000003', 'c2000000-0000-0000-0000-000000000001', 'event-list',   'Próximos eventos', 2, '{"heading":"Próximos eventos","source":"auto","upcoming":true,"limit":4}', NOW()),
  ('d2000000-0000-0000-0000-000000000004', 'c2000000-0000-0000-0000-000000000001', 'cta-section',  'CTA Apoie',        3, '{"heading":"Apoie o CMSS","ctaLabel":"Saiba como","ctaUrl":"/apoie"}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Quem Somos CMSS
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d2000000-0000-0000-0000-000000000010', 'c2000000-0000-0000-0000-000000000002', 'image-text', 'Sobre',          0, '{"image":"","text":"","imagePosition":"left"}', NOW()),
  ('d2000000-0000-0000-0000-000000000011', 'c2000000-0000-0000-0000-000000000002', 'two-column', 'Missão e Valores',1, '{"left":"","right":""}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- História CMSS — 4 marcos placeholder
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d2000000-0000-0000-0000-000000000020', 'c2000000-0000-0000-0000-000000000003', 'timeline', 'Nossa Trajetória', 0,
   '{"heading":"Nossa Trajetória","items":[{"year":"1950","title":"Fundação","description":""},{"year":"1985","title":"Expansão","description":""},{"year":"2010","title":"Era digital","description":""},{"year":"2023","title":"Hoje","description":""}]}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Agenda CMSS
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d2000000-0000-0000-0000-000000000030', 'c2000000-0000-0000-0000-000000000004', 'event-list', 'Agenda', 0, '{"heading":"Agenda","source":"auto","upcoming":true,"limit":12}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Apoie CMSS
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d2000000-0000-0000-0000-000000000040', 'c2000000-0000-0000-0000-000000000005', 'rich-text', 'Por que apoiar?', 0, '{"html":""}', NOW()),
  ('d2000000-0000-0000-0000-000000000041', 'c2000000-0000-0000-0000-000000000005', 'faq',       'Dúvidas',         1, '{"heading":"Dúvidas frequentes","items":[]}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;

-- Contato CMSS
INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d2000000-0000-0000-0000-000000000050', 'c2000000-0000-0000-0000-000000000006', 'contact', 'Contato', 0, '{"heading":"Fale Conosco","formId":"","address":""}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;
```

### C.6 — Páginas e seções: Conecta Talentos (1 página)

```sql
INSERT INTO pages (id, product_id, slug, title, locale, status, version, created_at) VALUES
  ('c3000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'home', 'Home', 'pt-BR', 'DRAFT', 1, NOW())
ON CONFLICT (product_id, slug) DO NOTHING;

INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d3000000-0000-0000-0000-000000000001', 'c3000000-0000-0000-0000-000000000001', 'hero',      'Hero',       0, '{"title":"Conecta Talentos","subtitle":"Encontre sua próxima oportunidade","ctaLabel":"Ver vagas","ctaUrl":"#vagas"}', NOW()),
  ('d3000000-0000-0000-0000-000000000002', 'c3000000-0000-0000-0000-000000000001', 'rich-text', 'Quem Somos', 1, '{"html":""}', NOW()),
  ('d3000000-0000-0000-0000-000000000003', 'c3000000-0000-0000-0000-000000000001', 'card-list', 'Vagas',      2, '{"heading":"Vagas abertas","source":"auto","limit":6,"items":[]}', NOW()),
  ('d3000000-0000-0000-0000-000000000004', 'c3000000-0000-0000-0000-000000000001', 'card-list', 'Blog',       3, '{"heading":"Blog","source":"auto","contentType":"artigo","limit":3}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;
```

### C.7 — Páginas e seções: Alexandre Dev (1 página)

```sql
INSERT INTO pages (id, product_id, slug, title, locale, status, version, created_at) VALUES
  ('c4000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'home', 'Home', 'pt-BR', 'DRAFT', 1, NOW())
ON CONFLICT (product_id, slug) DO NOTHING;

INSERT INTO sections (id, page_id, type, label, order_index, content_json, created_at) VALUES
  ('d4000000-0000-0000-0000-000000000001', 'c4000000-0000-0000-0000-000000000001', 'hero',         'Hero',        0, '{"title":"Alexandre Silva","subtitle":"Desenvolvedor de software","ctaLabel":"Ver projetos","ctaUrl":"#projetos"}', NOW()),
  ('d4000000-0000-0000-0000-000000000002', 'c4000000-0000-0000-0000-000000000001', 'card-list',    'Projetos',    1, '{"heading":"Projetos","source":"manual","items":[]}', NOW()),
  ('d4000000-0000-0000-0000-000000000003', 'c4000000-0000-0000-0000-000000000001', 'feature-grid', 'Skills',      2, '{"heading":"Skills","items":[]}', NOW()),
  ('d4000000-0000-0000-0000-000000000004', 'c4000000-0000-0000-0000-000000000001', 'timeline',     'Experiência', 3, '{"heading":"Experiência","items":[]}', NOW()),
  ('d4000000-0000-0000-0000-000000000005', 'c4000000-0000-0000-0000-000000000001', 'download',     'Downloads',   4, '{"heading":"Downloads","items":[]}', NOW())
ON CONFLICT (page_id, order_index) DO NOTHING;
```

### C.8 — Knowledge Graph: WikiDev

> Verificar nomes de colunas contra V3 (`graph_nodes`, `graph_edges`) antes de executar.
> Colunas prováveis de `graph_nodes`: `id, ref_id, label, type, product_id, status, x, y, summary, difficulty, thumbnail, metadata, created_at`.
> Colunas prováveis de `graph_edges`: `id, from_node_id, to_node_id, verb, weight, product_id, created_at`.

```sql
-- WikiDev — grafo baseado em frontend/src/domains/knowledge/mocks/knowledge.mocks.ts
INSERT INTO graph_nodes (id, ref_id, label, type, product_id, status, x, y, summary, difficulty, created_at) VALUES
  ('e5000000-0000-0000-0000-000000000001', 'spring-boot', 'Spring Boot', 'TOPIC', 'b0000000-0000-0000-0000-000000000005', 'ativo', 200, 100,
   'Framework para criar aplicações Java prontas para produção com configuração mínima.', 'beginner', NOW()),
  ('e5000000-0000-0000-0000-000000000002', 'jpa',         'JPA',         'TOPIC', 'b0000000-0000-0000-0000-000000000005', 'ativo', 400, 100,
   'Especificação Java para mapeamento objeto-relacional.', 'intermediate', NOW()),
  ('e5000000-0000-0000-0000-000000000003', 'docker',      'Docker',      'TOPIC', 'b0000000-0000-0000-0000-000000000005', 'ativo', 300, 300,
   'Plataforma de containerização para empacotar e executar aplicações.', 'beginner', NOW())
ON CONFLICT (ref_id, product_id) DO NOTHING;

INSERT INTO graph_edges (id, from_node_id, to_node_id, verb, weight, product_id, created_at) VALUES
  ('f5000000-0000-0000-0000-000000000001',
    (SELECT id FROM graph_nodes WHERE ref_id='spring-boot' AND product_id='b0000000-0000-0000-0000-000000000005'),
    (SELECT id FROM graph_nodes WHERE ref_id='jpa'         AND product_id='b0000000-0000-0000-0000-000000000005'),
    'INTEGRATES_WITH', 9, 'b0000000-0000-0000-0000-000000000005', NOW()),
  ('f5000000-0000-0000-0000-000000000002',
    (SELECT id FROM graph_nodes WHERE ref_id='spring-boot' AND product_id='b0000000-0000-0000-0000-000000000005'),
    (SELECT id FROM graph_nodes WHERE ref_id='docker'      AND product_id='b0000000-0000-0000-0000-000000000005'),
    'PACKAGED_BY', 6, 'b0000000-0000-0000-0000-000000000005', NOW())
ON CONFLICT DO NOTHING;
```

### C.9 — Knowledge Graph: Loki

```sql
-- Loki — grafo filosófico/musical baseado em knowledge.mocks.ts (lokiKgNodes/lokiKgEdges)
INSERT INTO graph_nodes (id, ref_id, label, type, product_id, status, x, y, created_at) VALUES
  ('e6000000-0000-0000-0000-000000000001', 'vigilia',            'Vigília',                'POEM',      'b0000000-0000-0000-0000-000000000006', 'ativo', 200, 100, NOW()),
  ('e6000000-0000-0000-0000-000000000002', 'clair-de-lune',      'Clair de Lune — Debussy','MUSIC_REF', 'b0000000-0000-0000-0000-000000000006', 'ativo', 400, 100, NOW()),
  ('e6000000-0000-0000-0000-000000000003', 'manifesto-silencio', 'Manifesto do Silêncio',  'MANIFEST',  'b0000000-0000-0000-0000-000000000006', 'ativo', 200, 300, NOW()),
  ('e6000000-0000-0000-0000-000000000004', 'playlist-intros',    'Playlist: Introspecção', 'PLAYLIST',  'b0000000-0000-0000-0000-000000000006', 'ativo', 400, 300, NOW()),
  ('e6000000-0000-0000-0000-000000000005', 'fragmentos',         'Fragmentos',             'BOOK',      'b0000000-0000-0000-0000-000000000006', 'ativo', 300, 500, NOW())
ON CONFLICT (ref_id, product_id) DO NOTHING;

INSERT INTO graph_edges (id, from_node_id, to_node_id, verb, weight, product_id, created_at) VALUES
  ('f6000000-0000-0000-0000-000000000001',
    (SELECT id FROM graph_nodes WHERE ref_id='vigilia'            AND product_id='b0000000-0000-0000-0000-000000000006'),
    (SELECT id FROM graph_nodes WHERE ref_id='clair-de-lune'      AND product_id='b0000000-0000-0000-0000-000000000006'),
    'INSPIRED_BY', 10, 'b0000000-0000-0000-0000-000000000006', NOW()),
  ('f6000000-0000-0000-0000-000000000002',
    (SELECT id FROM graph_nodes WHERE ref_id='manifesto-silencio' AND product_id='b0000000-0000-0000-0000-000000000006'),
    (SELECT id FROM graph_nodes WHERE ref_id='playlist-intros'    AND product_id='b0000000-0000-0000-0000-000000000006'),
    'INSPIRED_BY', 8, 'b0000000-0000-0000-0000-000000000006', NOW()),
  ('f6000000-0000-0000-0000-000000000003',
    (SELECT id FROM graph_nodes WHERE ref_id='vigilia'            AND product_id='b0000000-0000-0000-0000-000000000006'),
    (SELECT id FROM graph_nodes WHERE ref_id='fragmentos'         AND product_id='b0000000-0000-0000-0000-000000000006'),
    'PART_OF', 5, 'b0000000-0000-0000-0000-000000000006', NOW())
ON CONFLICT DO NOTHING;
```

### C.10 — Knowledge Graph: Maestro Beton (grafo institucional)

```sql
-- Maestro Beton — grafo institucional baseado em kgNodes/kgEdges do mock
INSERT INTO graph_nodes (id, ref_id, label, type, product_id, status, x, y, created_at) VALUES
  ('e1000000-0000-0000-0000-000000000001', 'mb-produto',        'Maestro Beton',  'PRODUCT',  'b0000000-0000-0000-0000-000000000001', 'ativo', 300, 200, NOW()),
  ('e1000000-0000-0000-0000-000000000002', 'mb-pagina-home',    'Página Home',    'PAGE',     'b0000000-0000-0000-0000-000000000001', 'ativo', 200, 100, NOW()),
  ('e1000000-0000-0000-0000-000000000003', 'mb-form-orcamento', 'Form: Orçamento','FORM',     'b0000000-0000-0000-0000-000000000001', 'ativo', 400, 100, NOW()),
  ('e1000000-0000-0000-0000-000000000004', 'mb-cat-inst',       'Institucional',  'CATEGORY', 'b0000000-0000-0000-0000-000000000001', 'ativo', 300, 400, NOW()),
  ('e1000000-0000-0000-0000-000000000005', 'mb-autor-marina',   'Marina Costa',   'AUTHOR',   'b0000000-0000-0000-0000-000000000001', 'ativo', 100, 300, NOW())
ON CONFLICT (ref_id, product_id) DO NOTHING;

INSERT INTO graph_edges (id, from_node_id, to_node_id, verb, weight, product_id, created_at) VALUES
  ('f1000000-0000-0000-0000-000000000001',
    (SELECT id FROM graph_nodes WHERE ref_id='mb-produto'        AND product_id='b0000000-0000-0000-0000-000000000001'),
    (SELECT id FROM graph_nodes WHERE ref_id='mb-pagina-home'    AND product_id='b0000000-0000-0000-0000-000000000001'),
    'CONTAINS', NULL, 'b0000000-0000-0000-0000-000000000001', NOW()),
  ('f1000000-0000-0000-0000-000000000002',
    (SELECT id FROM graph_nodes WHERE ref_id='mb-pagina-home'    AND product_id='b0000000-0000-0000-0000-000000000001'),
    (SELECT id FROM graph_nodes WHERE ref_id='mb-form-orcamento' AND product_id='b0000000-0000-0000-0000-000000000001'),
    'CONTAINS', NULL, 'b0000000-0000-0000-0000-000000000001', NOW()),
  ('f1000000-0000-0000-0000-000000000003',
    (SELECT id FROM graph_nodes WHERE ref_id='mb-produto'        AND product_id='b0000000-0000-0000-0000-000000000001'),
    (SELECT id FROM graph_nodes WHERE ref_id='mb-cat-inst'       AND product_id='b0000000-0000-0000-0000-000000000001'),
    'BELONGS_TO', NULL, 'b0000000-0000-0000-0000-000000000001', NOW()),
  ('f1000000-0000-0000-0000-000000000004',
    (SELECT id FROM graph_nodes WHERE ref_id='mb-pagina-home'    AND product_id='b0000000-0000-0000-0000-000000000001'),
    (SELECT id FROM graph_nodes WHERE ref_id='mb-autor-marina'   AND product_id='b0000000-0000-0000-0000-000000000001'),
    'WRITTEN_BY', NULL, 'b0000000-0000-0000-0000-000000000001', NOW())
ON CONFLICT DO NOTHING;
```

---

## D. Retrofits fechados nesta sprint

Os itens abaixo estão em aberto no `SPRINT-RESULTADO.md` e **não dependem de etapas futuras**.

### D.1 — Module enable/disable não auditado (registrado na etapa 16)

Em `ProductModuleService.enable(...)` e `ProductModuleService.disable(...)`:

```java
auditService.record(
    AuditEvent.builder()
        .actor(authenticatedUser.sub())
        .action("MODULE_" + (enabling ? "ENABLED" : "DISABLED"))
        .target(moduleKey.name())
        .productId(productId)
        .module("product")
        .build()
);
```

### D.2 — Domínio `form` nunca chama `AuditService` (registrado na etapa 23)

Em `SubmissionService.create(...)`:

```java
auditService.record(
    AuditEvent.builder()
        .actor("system")          // submit público — sem usuário autenticado
        .action("FORM_SUBMISSION_RECEIVED")
        .target(formId.toString())
        .productId(productId)
        .module("form")
        .build()
);
```

Verificar que `AuditService` está exposta como `@NamedInterface` em `audit.api` e que o módulo `form` pode acessá-la. Se não estiver, expor nesta etapa.

### D.3 — `traceId`, `ip`, `userAgent` sempre nulos nos eventos de auditoria (registrado na etapa 16)

Criar `AuditRequestContextInterceptor` (implementa `HandlerInterceptor`) + `AuditContextHolder` (`ThreadLocal<AuditContext>`):

```java
@Component
public class AuditRequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String traceId = req.getHeader("X-Trace-Id");
        if (traceId == null) traceId = UUID.randomUUID().toString();
        AuditContextHolder.set(new AuditContext(
            traceId,
            req.getRemoteAddr(),
            req.getHeader("User-Agent")
        ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        AuditContextHolder.clear();
    }
}
```

Registrar em `WebMvcConfigurer.addInterceptors(...)`. `AuditService.record(...)` lê do `AuditContextHolder` para preencher os campos antes de persistir.

### D.4 — Dispatch de feedback para Telegram (retrofit da etapa 27, Seção F.1)

A etapa 27 documentou este gap: feedbacks ficam apenas no banco; não há notificação proativa quando alguém reporta um problema. Esta seção fecha isso.

**D.4.1 — Campo Telegram nas settings de produto**

Adicionar dois campos opcionais ao `ProductSettings` (entidade/tabela da etapa 17):

```java
// em ProductSettings.java
@Column(name = "telegram_alert_bot_token", length = 128)
private String telegramAlertBotToken;   // token do bot (nunca expor em GET — retornar mascarado "••••xxxx")

@Column(name = "telegram_alert_chat_id", length = 64)
private String telegramAlertChatId;     // chat ou group ID do canal de alertas
```

Migration associada (incluir no mesmo `V_NEXT__seed_homologacao.sql` ou em script separado — verificar se a tabela `product_settings` existe antes de alterar):

```sql
ALTER TABLE product_settings
  ADD COLUMN IF NOT EXISTS telegram_alert_bot_token VARCHAR(128),
  ADD COLUMN IF NOT EXISTS telegram_alert_chat_id   VARCHAR(64);
```

Endpoints afetados (extensão do `GET/PUT /api/v1/products/{productId}/settings` da etapa 17):
- `GET` → retorna `{ ..., telegramAlert: { chatId: "...", botTokenMasked: "••••aBcD" } | null }`
- `PUT` → aceita `{ ..., telegramAlert: { chatId: "...", botToken: "..." } }` — salva ambos; `botToken` nunca é retornado em claro.

> Usar os mesmos campos `chatId`/`botToken` que `FormDeliveryPolicy.java` já valida: é o mesmo mecanismo, só a origem dos valores é diferente (settings de produto em vez de delivery-channel do form).

**D.4.2 — `TelegramFeedbackNotifier` (novo componente no módulo `feedback`)**

```java
@Component
public class TelegramFeedbackNotifier {

    private final ProductSettingsRepository productSettingsRepo;
    private final AssetRepository assetRepo;              // para montar URL do anexo
    private final RestClient restClient;

    /** Chamado APÓS a persistência do Feedback. Falha silenciosa — não lança exceção. */
    public void notify(Feedback feedback) {
        if (feedback.getProductId() == null) return;

        productSettingsRepo.findByProductId(feedback.getProductId())
            .filter(s -> s.getTelegramAlertBotToken() != null && s.getTelegramAlertChatId() != null)
            .ifPresent(settings -> sendToTelegram(feedback, settings));
    }

    private void sendToTelegram(Feedback feedback, ProductSettings settings) {
        try {
            String text = buildMessage(feedback);
            String url = "https://api.telegram.org/bot%s/sendMessage".formatted(settings.getTelegramAlertBotToken());
            restClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", settings.getTelegramAlertChatId(), "text", text, "parse_mode", "HTML"))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Telegram dispatch falhou para feedback {} — {}", feedback.getId(), ex.getMessage());
            // Nunca propaga: o Feedback já está persistido, o dispatch é best-effort
        }
    }

    private String buildMessage(Feedback feedback) {
        String assetInfo = "";
        if (feedback.getAttachmentAssetId() != null) {
            // URL de download relativo — o destinatário pode colar no browser com o token correto
            assetInfo = "\n📎 Anexo: <code>/api/v1/assets/%s/download</code>".formatted(feedback.getAttachmentAssetId());
        }
        String desc = feedback.getDescription().length() > 200
            ? feedback.getDescription().substring(0, 200) + "…"
            : feedback.getDescription();
        return """
            🐛 <b>Feedback Aegis — %s</b>
            Categoria: %s | Prioridade: <b>%s</b>
            Produto: %s | Tela: %s
            Usuário: %s
            
            %s%s
            """.formatted(
                feedback.getId(),
                feedback.getCategory(), feedback.getPriority(),
                feedback.getProductId(), feedback.getScreenName() != null ? feedback.getScreenName() : "—",
                feedback.getCreatedBySubject(),
                desc, assetInfo
            );
    }
}
```

**D.4.3 — Wiring em `FeedbackService.create(...)`**

```java
// Após persistir o Feedback:
telegramFeedbackNotifier.notify(savedFeedback);
```

**D.4.4 — Testes**

- `TelegramFeedbackNotifierTest`: mock de `RestClient`; verificar que a mensagem contém o ID legível, categoria, prioridade, e a URL do asset quando `attachmentAssetId` está preenchido.
- Verificar que uma exceção do `RestClient` **não propaga** para o caller (feedback ainda salvo).
- Verificar que `notify` é no-op quando `productId` é nulo ou settings não tem token configurado.

### D.5 — Swagger UI dando 401 em todas as requisições após "Authorize"

**Causa raiz:** `OpenApiConfig` usa `SecurityScheme.Type.HTTP` com `scheme("bearer")` — isso gera apenas um campo de texto no Swagger UI onde o usuário precisa colar um JWT manualmente. Não existe nenhum fluxo OAuth2 configurado, então quando o usuário clica em "Authorize", o Swagger não sabe como obter um token do Keycloak — e consequentemente não inclui o header `Authorization` nas requisições.

Problema secundário: `SecurityConfig.cors` só libera `http://localhost:5173`. Embora o Swagger UI esteja na mesma origem da API (`http://localhost:8080`), o fluxo OAuth2 Authorization Code requer que o redirect de volta ao Swagger (`/swagger-ui/oauth2-redirect.html`) esteja registrado no Keycloak — e que o cliente Keycloak tenha `Web Origins` incluindo `http://localhost:8080`.

**D.5.1 — `OpenApiConfig.java`: adicionar OAuth2 Authorization Code + PKCE**

Injetar a URL do issuer e adicionar o scheme OAuth2 ao lado do Bearer existente (mantém retrocompatibilidade com Bruno/curl que usam o Bearer manual):

```java
// Adicionar injeção no topo da classe:
@Value("${keycloak.issuer-uri}")
private String keycloakIssuerUri;
```

No bean `aegisOpenApi()`, adicionar o scheme OAuth2 **antes** do `bearerJwt` existente:

```java
SecurityScheme oauth2Pkce = new SecurityScheme()
    .name("oauth2-pkce")
    .type(SecurityScheme.Type.OAUTH2)
    .description("Login via Keycloak — clique em Authorize, autentique e o token será injetado automaticamente.")
    .flows(new OAuthFlows()
        .authorizationCode(new OAuthFlow()
            .authorizationUrl(keycloakIssuerUri + "/protocol/openid-connect/auth")
            .tokenUrl(keycloakIssuerUri + "/protocol/openid-connect/token")
            .scopes(new Scopes()
                .addString("openid",  "OpenID Connect")
                .addString("profile", "Nome e dados básicos do usuário")
                .addString("email",   "E-mail do usuário")
            )
        )
    );
```

Registrar ambos os schemes e aplicar ambos como `SecurityRequirement` global:

```java
return new OpenAPI()
    .info(/* ... igual ao atual ... */)
    .servers(/* ... igual ao atual ... */)
    .components(new Components()
        .addSecuritySchemes("oauth2-pkce",    oauth2Pkce)    // ← NOVO
        .addSecuritySchemes(BEARER_AUTH_SCHEME, bearerJwt))  // ← já existia
    .security(List.of(
        new SecurityRequirement().addList("oauth2-pkce", List.of("openid", "profile", "email")),
        new SecurityRequirement().addList(BEARER_AUTH_SCHEME)
    ))
    .tags(orderedTags());
```

No `documentedOperations()`, atualizar `requiresBearerToken` → adicionar ambos quando proteção é necessária:

```java
if (requiresBearerToken(path, tag)) {
    operation.addSecurityItem(new SecurityRequirement()
        .addList("oauth2-pkce", List.of("openid", "profile", "email")));
    operation.addSecurityItem(new SecurityRequirement()
        .addList(BEARER_AUTH_SCHEME));
} else {
    operation.setSecurity(List.of());
}
```

Imports adicionais necessários:

```java
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import org.springframework.beans.factory.annotation.Value;
```

**D.5.2 — `application-local.yml`: configuração do Swagger UI OAuth2**

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    oauth2-redirect-url: http://localhost:8080/swagger-ui/oauth2-redirect.html
    oauth:
      client-id: aegis-app          # mesmo clientId já criado na etapa 03
      client-secret: ""             # public client — sem secret
      scopes: "openid profile email"
      use-pkce-with-authorization-code-grant: true
      app-name: "Aegis PMS"
```

> `client-secret` vazio porque `aegis-app` é um public client (PKCE substitui o secret). Se em algum momento virar confidential client, preencher com a env var `${SWAGGER_OAUTH_CLIENT_SECRET:}`.

**D.5.3 — Keycloak: registrar redirect URI do Swagger (retrofit na etapa 03)**

No Keycloak Admin (`http://localhost:8282/admin`) → realm `aegis` → client `aegis-app`:

1. **Valid Redirect URIs**: adicionar `http://localhost:8080/swagger-ui/oauth2-redirect.html`
2. **Web Origins**: adicionar `http://localhost:8080` (Keycloak precisa devolver CORS headers para o Swagger UI que roda na mesma origem da API)
3. Confirmar que **Standard Flow Enabled** = ON (Authorization Code grant)
4. Confirmar que **PKCE Code Challenge Method** = S256 (ou deixar sem restrição para aceitar PKCE automaticamente)

Documentar no export do realm (`infra/keycloak/realm-export.json`) após a mudança, para o `docker compose up` já subir com essa configuração (ver etapa 03, Seção D — export automático do realm).

**D.5.4 — Verificação após a correção**

```bash
# 1. Subir o ambiente local
docker compose up -d

# 2. Abrir http://localhost:8080/swagger-ui — deve aparecer dois botões "Authorize":
#    · oauth2-pkce  → clicando abre popup do Keycloak
#    · bearer-jwt   → campo de texto para colar token manualmente

# 3. Clicar em "Authorize" → oauth2-pkce → autenticar com um usuário do realm aegis
#    → após login no Keycloak, o popup fecha automaticamente

# 4. Executar GET /api/v1/me — deve retornar 200 com os dados do usuário autenticado
#    (não mais 401)

# 5. Executar GET /api/v1/tenants — deve retornar 200 para SUPER_ADMIN, 403 para outros papéis
```

> **Nota de UX:** com dois schemes listados, o Swagger UI mostra ambos no diálogo "Authorize". O usuário deve autorizar apenas o `oauth2-pkce` (clicando no botão específico dele) ou apenas o `bearer-jwt` — não ambos ao mesmo tempo, para evitar conflito de header. Documentar isso no bloco `description` do `aegisOpenApi()` ou como um aviso na `info.description`.

---

## E. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. 100% de cobertura nas classes novas.
- Entregar em rodadas:
  1. Remoção do package `seed` + remoção de NamedInterfaces de seed + ajuste de testes que dependiam deles + `mvn verify` BUILD SUCCESS (verificar que nenhum teste quebrou com a remoção).
  2. `GraphNodeType` adições (Java enum + SQL `ALTER TYPE`) + testes de serialização/desserialização dos novos valores.
  3. Migration `V_NEXT__seed_homologacao.sql` (execute em banco limpo via `mvn flyway:migrate` ou `spring.flyway.enabled=true`). Verificar com:
     ```bash
     psql -U aegis_user -d aegis_db -c "SELECT key, name, plan FROM tenants WHERE key='clientes-beta';"
     psql -U aegis_user -d aegis_db -c "SELECT key, name, type FROM products WHERE tenant_id='a0000000-0000-0000-0000-000000000001';"
     psql -U aegis_user -d aegis_db -c "SELECT slug, title FROM pages WHERE product_id='b0000000-0000-0000-0000-000000000001' ORDER BY slug;"
     psql -U aegis_user -d aegis_db -c "SELECT label FROM graph_nodes WHERE product_id='b0000000-0000-0000-0000-000000000006';"
     ```
  4. Teste de integração `@SpringBootTest` + `@Sql` confirmando: os 6 produtos existem via `GET /products`, Maestro Beton tem 7 páginas, Loki tem 5 graph nodes.
  5. Retrofits D.1, D.2, D.3 + testes.
  6. Retrofit D.4 (Telegram para feedback): migration dos campos `telegram_alert_*`, `TelegramFeedbackNotifier` + testes (mock RestClient), extensão de `GET/PUT /settings` + teste de que token nunca retorna em claro.
  7. Retrofit D.5 (Swagger OAuth2): `OpenApiConfig` com scheme `oauth2-pkce`, `application-local.yml` com `springdoc.swagger-ui.oauth.*`, Keycloak redirect URI registrado → verificar `GET /me` respondendo 200 no Swagger UI após login PKCE.
  8. Bruno collection atualizada (pasta `30-seed-homologacao/`): `GET /products?tenantId=a0000000-...`, `GET /products/b0000000-...-001/pages`, `GET /products/b0000000-...-006/graph/nodes`, `PUT /products/{id}/settings` com `telegramAlert`. Rodar `npx @usebruno/cli run --env local` — todos aprovados.

---

## F. Critérios de aceite

- [ ] Package `br.com.byop.aegis.seed` não existe mais. `grep -r "DemoSeed" src/` → zero resultados.
- [ ] `application-local.yml` sem `aegis.seed.*`.
- [ ] `mvn clean verify` BUILD SUCCESS com JaCoCo 100% aprovado.
- [ ] `GET /api/v1/tenants` retorna tenant com `key: "clientes-beta"` e `name: "CLIENTES BETA"`.
- [ ] `GET /api/v1/products` (CLIENTES BETA scope) retorna 6 produtos.
- [ ] `GET /api/v1/products/b0000000-...-001/pages` → 7 páginas (home, quem-somos, historia, agenda, galeria, apoie, contato).
- [ ] `GET /api/v1/products/b0000000-...-004/pages` → 6 páginas (CMSS).
- [ ] `GET /api/v1/products/b0000000-...-002/pages` → 1 página home (Conecta Talentos).
- [ ] `GET /api/v1/products/b0000000-...-003/pages` → 1 página home (Alexandre Dev).
- [ ] `GET /api/v1/products/b0000000-...-005/graph/nodes` → 3 nodes (WikiDev: Spring Boot, JPA, Docker).
- [ ] `GET /api/v1/products/b0000000-...-006/graph/nodes` → 5 nodes (Loki: Vigília, Clair de Lune, Manifesto, Playlist, Fragmentos).
- [ ] Migration é idempotente: rodar duas vezes seguidas sem erro.
- [ ] Eventos de auditoria de `MODULE_ENABLED`, `FORM_SUBMISSION_RECEIVED` aparecem em `GET /audit/events`.
- [ ] Campos `traceId`, `ip`, `userAgent` não são `null` nos eventos de auditoria.
- [ ] `PUT /api/v1/products/{id}/settings` aceita `telegramAlert.botToken` + `telegramAlert.chatId` e persiste.
- [ ] `GET /api/v1/products/{id}/settings` retorna `telegramAlert.botTokenMasked` (ex: `"••••aBcD"`) — nunca o token em claro.
- [ ] Envio de feedback com produto que tem Telegram configurado dispara mensagem ao bot (verificar via Telegram ou via mock de `RestClient` no teste de integração).
- [ ] Envio de feedback com `attachmentAssetId` inclui a URL `/api/v1/assets/{id}/download` na mensagem Telegram.
- [ ] Falha no Telegram (timeout, token inválido) não reverte o `Feedback` persistido — o `POST /feedback` retorna 201 normalmente.
- [ ] Swagger UI em `http://localhost:8080/swagger-ui` exibe dois schemes: `oauth2-pkce` e `bearer-jwt`.
- [ ] Clicar em "Authorize → oauth2-pkce" abre popup do Keycloak; após login, popup fecha e Swagger UI passa a incluir `Authorization: Bearer <token>` em todas as requisições protegidas.
- [ ] `GET /api/v1/me` no Swagger UI retorna 200 após autenticação OAuth2 (não mais 401).
- [ ] `GET /api/v1/tenants` retorna 403 para Editor/Viewer (role incorreta) e 200 para SUPER_ADMIN — autenticação funcionando, autorização por papel também.
- [ ] `realm-export.json` atualizado com `http://localhost:8080/swagger-ui/oauth2-redirect.html` na lista de redirect URIs válidos e `http://localhost:8080` em Web Origins do client `aegis-app`.
- [ ] Spring Modulith aprovado (sem violação de módulo nova).
- [ ] `npx @usebruno/cli run --env local` — todos os requests aprovados (collection cumulativa incluindo a pasta `30-seed-homologacao/`).

---

## G. Validação Bruno

```bash
# Tenant beta
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants
# esperado: inclui { key: "clientes-beta", name: "CLIENTES BETA", plan: "PRO" }

# Produtos do tenant beta
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/products?tenantId=a0000000-0000-0000-0000-000000000001"
# esperado: 6 produtos (maestro-beton, conecta-talentos, alexandre-dev, cmss, wikidev, loki)

# Páginas do Maestro Beton
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/products/b0000000-0000-0000-0000-000000000001/pages
# esperado: 7 páginas em status DRAFT

# KG do Loki
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/products/b0000000-0000-0000-0000-000000000006/graph/nodes
# esperado: 5 nodes (Vigília POEM, Clair de Lune MUSIC_REF, Manifesto MANIFEST, Playlist PLAYLIST, Fragmentos BOOK)

# Idempotência — rodar a migration uma segunda vez não deve falhar
# (verificar logs do Flyway: "Migration V_NEXT already applied")
```

---

## H. Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, atualizar `SPRINT-RESULTADO.md` com: package seed removido, migration V_NEXT aplicada, UUIDs fixos (tabela de referência para facilitar scripts futuros), GraphNodeType valores adicionados, retrofits D.1/D.2/D.3 fechados. Esta é a última etapa — o artefato deve registrar o número final de testes e requests Bruno do ciclo completo.

---

## I. Commit sugerido

```bash
# Remoções
git rm -r backend/src/main/java/br/com/byop/aegis/seed/

# Adições
git add backend/src/main/resources/db/migration/V_NEXT__seed_homologacao.sql \
        backend/src/main/java/br/com/byop/aegis/knowledgegraph/GraphNodeType.java \
        backend/src/main/java/br/com/byop/aegis/audit/AuditRequestContextInterceptor.java \
        backend/src/main/java/br/com/byop/aegis/audit/AuditContextHolder.java \
        backend/src/main/java/br/com/byop/aegis/audit/AuditContext.java

git commit -m "feat(backend): seed real de homologacao via migration SQL, remocao do seed Java e retrofits de auditoria"
```
