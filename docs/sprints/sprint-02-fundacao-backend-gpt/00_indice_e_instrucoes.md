# Sprint 02 — Fundação do backend (para executar com GPT)

## Por que esta sprint é diferente das outras

Todas as outras sprints deste plano (`01`, `03`–`08`) são pensadas para você executar comigo (Claude/Cowork), em modo agent, neste mesmo repositório.

A Sprint 02 é a exceção: ela cobre a fundação inteira do backend (Docker, PostgreSQL duplo, Keycloak, Spring Boot, modelo core, Knowledge Graph MVP) descrita em `docs/implementation/001_aegis_pms_roteiro_fundacao_operacional.md`, e você decidiu executá-la com o **GPT**, não comigo. Por isso ela foi quebrada em vários arquivos pequenos — um por etapa — para colar um de cada vez em conversas do GPT, em vez de um arquivo monolítico gigante.

## Como usar esta pasta

1. Antes de começar, crie a branch desta sprint a partir de `develop` (já deve existir, criada na Sprint 01):
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b sprint/02-fundacao-backend
   ```
2. Abra uma conversa nova no GPT para cada arquivo `01_...md` até `12_...md`, **nesta ordem**. Cole o conteúdo do arquivo inteiro (ele já inclui o contexto fixo necessário — não precisa colar nada antes).
3. Peça ao GPT para gerar os arquivos/código pedidos na etapa, aplique-os no repositório, valide com os comandos da seção "Validação" do próprio arquivo, e só então comite (cada etapa já traz a mensagem de commit sugerida).
4. Ao terminar a última etapa (`12_...md`), faça merge da branch em `develop`:
   ```bash
   git push -u origin sprint/02-fundacao-backend
   git checkout develop
   git merge --no-ff sprint/02-fundacao-backend
   git push origin develop
   ```
5. **Nunca** comite direto em `main`, `release` ou `develop` — sempre dentro de `sprint/02-fundacao-backend`, conforme `AGENTS.md` e `CONTRIBUTING.md` na raiz do repositório.

## Regras que valem para todas as etapas (não negociáveis)

- Keycloak **deve** usar PostgreSQL dedicado e persistente (`keycloak-postgres`, volume `keycloak_postgres_data`) — nunca H2, nunca volume efêmero. Ver ADR-0005.
- O PostgreSQL do Aegis também é dedicado e persistente (`aegis-postgres`, volume `aegis_postgres_data`). Ver ADR-0010.
- O frontend **não é Angular** — é React (ver `docs/adr/ADR-0011-frontend-framework-react.md`). Onde `implementation/001` menciona Angular (Features 017/018), a etapa `09` desta pasta substitui isso por "buildar e servir o React existente em `frontend/`". Não recrie um frontend Angular.
- Toda etapa precisa passar pela validação descrita antes de seguir para a próxima.
- Se o GPT sugerir qualquer mudança na forma como Keycloak ou PostgreSQL persistem dados, pare e não aplique — viola decisão arquitetural já tomada.

## Etapas

| Arquivo | Cobre (Features de implementation/001) | Conteúdo |
|---|---|---|
| `01_estrutura_repo_e_env.md` | 001, 002 | Estrutura do repositório e variáveis de ambiente |
| `02_postgresql_aegis_e_keycloak.md` | 003, 004 | Os dois PostgreSQL dedicados e persistentes |
| `03_keycloak_persistente_e_realm.md` | 005, 006, 007 | Keycloak persistente + realm/client/roles + export |
| `04_backend_base_e_persistencia.md` | 008, 009 | Spring Boot base + conexão com PostgreSQL + Flyway |
| `05_security_resource_server_e_me.md` | 010, 011 | Resource Server com Keycloak + endpoint `/api/v1/me` |
| `06_modelo_core_tenant_product_modulos.md` | 012, 013 | Tenant, Membership, Product, catálogo de módulos |
| `07_knowledge_graph_mvp.md` | 014, 015 | Knowledge Graph MVP (modelo + regras de consistência) |
| `08_health_status.md` | 016 | Health, info, readiness |
| `09_build_frontend_react_no_backend.md` | 017, 018 (substituídas) | Build do React (não Angular) servido pelo Spring Boot |
| `10_dockerfile_e_compose_completo.md` | 019, 020 | Dockerfile do backend + Docker Compose completo |
| `11_seed_inicial_e_grafo.md` | 021, 022 | Seed de tenants/produtos + seed do Knowledge Graph |
| `12_openapi_testes_e_checklist_final.md` | 023, 024, 025 | Swagger, testes mínimos, checklist final do servidor |

Ao final da etapa 12, o backend completo deve estar funcional: `docker compose up -d` sobe tudo, Keycloak e PostgreSQL persistem dados entre restarts, o backend autentica via JWT, o modelo core e o Knowledge Graph MVP funcionam, e o build do frontend React é servido na mesma origem.
