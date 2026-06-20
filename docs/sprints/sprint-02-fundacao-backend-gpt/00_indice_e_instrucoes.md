# Sprint 02 — Fundação do backend (para executar com GPT)

## Por que esta sprint é diferente das outras

Todas as outras sprints deste plano (`01`, `05`–`11`) são pensadas para você executar comigo (Claude/Cowork), em modo agent, neste mesmo repositório.

A Sprint 02 é a exceção: ela cobre a fundação inteira do backend (Docker, PostgreSQL duplo, Keycloak, Spring Boot, modelo core, Knowledge Graph MVP) descrita em `docs/implementation/001_aegis_pms_roteiro_fundacao_operacional.md`, **mais todos os domínios de produto que o frontend já tem construído e que precisam de endpoint real** (`content`, `pages`, `assets`, `forms`, `analytics`, `users`, `audit`, `settings`, `dashboard`) e o fluxo completo de Super Admin (`tenants` CRUD + `ProductAssignment`) — especificação consolidada em `docs/trace/00_endpoints_esperados.md`. Você decidiu executar tudo isso com o **GPT**, não comigo. Por isso foi quebrado em vários arquivos pequenos — um por etapa — para colar um de cada vez em conversas do GPT, em vez de um arquivo monolítico gigante.

## Como usar esta pasta

1. Antes de começar, crie a branch desta sprint a partir de `develop` (já deve existir, criada na Sprint 01):
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b sprint/02-fundacao-backend
   ```
2. Abra uma conversa nova no GPT para cada arquivo `01_...md` até `22_...md`, **nesta ordem** (ver tabela "Etapas" abaixo — a numeração do arquivo já é a ordem de execução). Cole o conteúdo do arquivo inteiro (ele já inclui o contexto fixo necessário — não precisa colar nada antes).
3. Peça ao GPT para gerar os arquivos/código pedidos na etapa, aplique-os no repositório, valide com os comandos da seção "Validação" do próprio arquivo, e só então comite (cada etapa já traz a mensagem de commit sugerida).
4. Ao terminar a última etapa (`22_...md`), faça merge da branch em `develop`:
   ```bash
   git push -u origin sprint/02-fundacao-backend
   git checkout develop
   git merge --no-ff sprint/02-fundacao-backend
   git push origin develop
   ```
5. **Nunca** comite direto em `main`, `release` ou `develop` — sempre dentro de `sprint/02-fundacao-backend`, conforme `AGENTS.md` e `CONTRIBUTING.md` na raiz do repositório.
6. Se preferir não fazer tudo numa sessão só, dá para parar depois da etapa 08 (fundação pura: infra, auth, tenant/product/module, knowledge graph, health) e fazer merge — etapas 09-17 e 21 (domínios de produto) podem virar uma branch/PR separada, desde que sigam depois na mesma ordem.

## Regras que valem para todas as etapas (não negociáveis)

- Keycloak **deve** usar PostgreSQL dedicado e persistente (`keycloak-postgres`, volume `keycloak_postgres_data`) — nunca H2, nunca volume efêmero. Ver ADR-0005.
- O PostgreSQL do Aegis também é dedicado e persistente (`aegis-postgres`, volume `aegis_postgres_data`). Ver ADR-0010.
- O frontend **não é Angular** — é React (ver `docs/adr/ADR-0011-frontend-framework-react.md`). Onde `implementation/001` menciona Angular (Features 017/018), a etapa `18` desta pasta substitui isso por "buildar e servir o React existente em `frontend/`". Não recrie um frontend Angular.
- Os payloads das etapas 09-17 e 21 (domínios de produto) **não são inventados nesta sprint** — são os mesmos contratos TypeScript que `frontend/src/domains/*/contracts/` já define hoje (Sprint 09 do frontend, fora do GPT), mais os achados da Sprint 11 (domínio `pages`, preview leve de nó do grafo, `summary`/`difficultyLevel` em `Content`). Se o GPT sugerir um shape diferente do descrito na etapa, prevaleça o shape da etapa/trace report, não a sugestão do GPT.
- Toda etapa precisa passar pela validação descrita antes de seguir para a próxima.
- Se o GPT sugerir qualquer mudança na forma como Keycloak ou PostgreSQL persistem dados, pare e não aplique — viola decisão arquitetural já tomada.

## Etapas

### Fundação (Features de `implementation/001`)

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `01_estrutura_repo_e_env.md` | 001, 002 | Estrutura do repositório e variáveis de ambiente |
| `02_postgresql_aegis_e_keycloak.md` | 003, 004 | Os dois PostgreSQL dedicados e persistentes |
| `03_keycloak_persistente_e_realm.md` | 005, 006, 007 | Keycloak persistente + realm/client/roles + export |
| `04_backend_base_e_persistencia.md` | 008, 009 | Spring Boot base + conexão com PostgreSQL + Flyway |
| `05_security_resource_server_e_me.md` | 010, 011 | Resource Server com Keycloak + endpoint `/api/v1/me` |
| `06_modelo_core_tenant_product_modulos.md` | 012, 013 | Tenant, Membership, Product, catálogo de módulos |
| `07_knowledge_graph_mvp.md` | 014, 015 | Knowledge Graph MVP (modelo + regras de consistência) |
| `08_health_status.md` | 016 | Health, info, readiness |

### Domínios de produto (especificação: `docs/trace/00_endpoints_esperados.md`)

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `09_tenants_crud_completo_e_product_assignment.md` | Trace, Seção C | CRUD completo de tenant (editar/excluir), escopo por papel, entidade `ProductAssignment` (atribuir produto a usuário) |
| `10_dominio_content.md` | Trace, Seção B.1 + Seção D.3 | Conteúdo, workflow editorial (máquina de estados), versionamento, publicação — agora com `summary`/`difficultyLevel` opcionais |
| `11_dominio_assets.md` | Trace, Seção B.2 | Upload de assets, metadados, tags |
| `12_dominio_forms.md` | Trace, Seção B.3 | Definição de formulários (builder) e submissions |
| `13_dominio_analytics.md` | Trace, Seção B.4 | KPIs, saúde do produto, canais (agregadores sobre os outros domínios) |
| `14_dominio_users.md` | Trace, Seção B.5 | Listagem/convite/detalhe de usuário via Keycloak Admin API |
| `15_dominio_audit.md` | Trace, Seção B.6 | Serviço central de auditoria e endpoints de leitura |
| `16_dominio_settings_e_dashboard.md` | Trace, Seções B.7, B.8 | Configurações, matriz de permissões, agregador do dashboard global |
| `17_knowledge_graph_extras_layout_e_orphans.md` | Trace, Seção B.9 + Seção D.2 | Posição persistente (`x`/`y`), `props` estruturadas, endpoint de nós órfãos, endpoint `.../preview` para referência inline (caso WikiDev) |

### Integração final e infraestrutura

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `18_build_frontend_react_no_backend.md` | 017, 018 (substituídas) | Build do React (não Angular) servido pelo Spring Boot |
| `19_dockerfile_e_compose_completo.md` | 019, 020 | Dockerfile do backend + Docker Compose completo |
| `20_seed_inicial_e_grafo.md` | 021, 022 | Seed de tenants/produtos/usuários + seed do Knowledge Graph |
| `21_dominio_pages_secoes_e_blocos.md` | Trace, Seção D.1 (Sprint 11 do frontend) | Domínio `pages`: páginas compostas por seções/blocos tipados (hero, card-list, gallery, contact...) — pré-requisito só a etapa 10, pode ser feita a qualquer momento depois dela, numerada por último por ter sido adicionada depois |
| `22_openapi_testes_e_checklist_final.md` | 023, 024, 025 | Swagger, testes mínimos, checklist final do servidor |

Ao final da etapa 22, o backend completo deve estar funcional: `docker compose up -d` sobe tudo, Keycloak e PostgreSQL persistem dados entre restarts, o backend autentica via JWT, o modelo core e o Knowledge Graph MVP funcionam, **todos os domínios de produto que o frontend já usa (content, pages, assets, forms, analytics, users, audit, settings, dashboard) respondem com os payloads exatos do trace report**, o fluxo de Super Admin (criar tenant → criar produto → atribuir usuário) funciona de ponta a ponta via API, e o build do frontend React é servido na mesma origem.
