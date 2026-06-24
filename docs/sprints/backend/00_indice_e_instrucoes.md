# Sprint 02 — Fundação do backend (para executar com GPT)

## Por que esta sprint é diferente das outras

Todas as outras sprints deste plano (`01`, `05`–`11`) são pensadas para você executar comigo (Claude/Cowork), em modo agent, neste mesmo repositório.

A Sprint 02 é a exceção: ela cobre a fundação inteira do backend (Docker, PostgreSQL duplo, Keycloak, Spring Boot, modelo core, Knowledge Graph MVP) descrita em `docs/implementation/001_aegis_pms_roteiro_fundacao_operacional.md`, **mais todos os domínios de produto que o frontend já tem construído e que precisam de endpoint real** (`content`, `pages`, `assets`, `forms`, `analytics`, `users`, `audit`, `settings`, `dashboard`) e o fluxo completo de Super Admin (`tenants` CRUD + `ProductAssignment`) — especificação consolidada em `docs/trace/00_endpoints_esperados.md`. Você decidiu executar tudo isso com o **GPT**, não comigo. Por isso foi quebrado em vários arquivos pequenos — um por etapa — para colar um de cada vez em conversas do GPT, em vez de um arquivo monolítico gigante.

> **Nota de revisão (etapa 06 inserida)**: após a execução da etapa 05 foi identificado que login/logout, SMTP e o fluxo de convite via e-mail não estavam cobertos por nenhuma etapa. A etapa `06_auth_proxy_smtp_e_convite.md` foi inserida antes do modelo core (que era a antiga etapa 06, agora 07) para cobrir: proxy de autenticação via backend (frontend nunca chama o Keycloak diretamente), configuração do SMTP (MailHog em dev), e templates de e-mail FreeMarker com identidade visual Aegis. A numeração 06–25 foi deslocada para 07–26 — o total de etapas passou de 25 para **26**.

## Como usar esta pasta

1. Antes de começar, crie a branch desta sprint a partir de `develop` (já deve existir, criada na Sprint 01):
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b sprint/02-fundacao-backend
   ```
2. Leia `00_padrao_qualidade_e_arquitetura.md` uma vez (não precisa colar no GPT, é para você entender o padrão antes de revisar o que o GPT entregar). Depois, abra uma conversa nova no GPT para cada arquivo `01_...md` até `26_...md`, **nesta ordem** (ver tabela "Etapas" abaixo — a numeração do arquivo já é a ordem de execução). Cole o conteúdo do arquivo inteiro (ele já inclui o contexto fixo necessário, incluindo um resumo do padrão de qualidade — não precisa colar nada antes) — **e, a partir da etapa 02, cole também o conteúdo atual de `SPRINT-RESULTADO.md`** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12) na mesma mensagem: é como o GPT, que não tem memória da conversa anterior, sabe o que já foi decidido e construído.
3. Peça ao GPT para gerar os arquivos/código pedidos na etapa, aplique-os no repositório, valide — a partir da etapa 03, isso significa pedir ao GPT o JSON atualizado de `aegis-postman-collection.json` (collection Postman cumulativa, ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11), baixar/salvar esse arquivo (sugestão: `infra/postman/aegis-postman-collection.json`, versionado junto do `aegis-realm.json` da etapa 03) e importar no Postman para testar os requests da etapa. Antes de comitar, peça também o **`SPRINT-RESULTADO.md` completo e atualizado** (arquivo inteiro, nunca um diff — mesmo princípio da collection) com a entrada desta etapa (template fixo na Seção 12.2 do padrão de qualidade) e salve-o em `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` — só então comite (cada etapa já traz a mensagem de commit sugerida, que passa a incluir este arquivo também).
4. Ao terminar a última etapa (`26_...md`), faça merge da branch em `develop`:
   ```bash
   git push -u origin sprint/02-fundacao-backend
   git checkout develop
   git merge --no-ff sprint/02-fundacao-backend
   git push origin develop
   ```
5. **Nunca** comite direto em `main`, `release` ou `develop` — sempre dentro de `sprint/02-fundacao-backend`, conforme `AGENTS.md` e `CONTRIBUTING.md` na raiz do repositório.
6. Se preferir não fazer tudo numa sessão só, dá para parar depois da etapa 09 (fundação pura: infra, auth proxy, tenant/product/module, knowledge graph, health) e fazer merge — etapas 10-18 e 22 (domínios de produto) podem virar uma branch/PR separada, desde que sigam depois na mesma ordem.

## Regras que valem para todas as etapas (não negociáveis)

- Keycloak **deve** usar PostgreSQL dedicado e persistente (serviço `keycloak-postgres`, container `keycloak-postgres-aegis`, volume `keycloak_aegis_postgres_data`, banco `keycloak_aegis`/usuário `keycloak_aegis_user`, porta `5435` no host — sufixo `_aegis`/`-aegis` para nunca colidir com outro Postgres/Keycloak já presente no ambiente) — nunca H2, nunca volume efêmero. Ver ADR-0005.
- O PostgreSQL do Aegis também é dedicado e persistente (`aegis-postgres`, volume `aegis_postgres_data`). Ver ADR-0010.
- O frontend **não é Angular** — é React (ver `docs/adr/ADR-0011-frontend-framework-react.md`). Onde `implementation/001` menciona Angular (Features 017/018), a etapa `18` desta pasta substitui isso por "buildar e servir o React existente em `frontend/`". Não recrie um frontend Angular.
- Os payloads das etapas 09-17 e 21 (domínios de produto) **não são inventados nesta sprint** — são os mesmos contratos TypeScript que `frontend/src/domains/*/contracts/` já define hoje (Sprint 09 do frontend, fora do GPT), mais os achados da Sprint 11 (domínio `pages`, preview leve de nó do grafo, `summary`/`difficultyLevel` em `Content`). Se o GPT sugerir um shape diferente do descrito na etapa, prevaleça o shape da etapa/trace report, não a sugestão do GPT.
- Toda etapa precisa passar pela validação descrita antes de seguir para a próxima.
- **A partir da etapa 03**: validação é entregue como collection Postman cumulativa (`aegis-postman-collection.json`), não só `curl` manual — autenticação via Keycloak automática (pasta "Auth", token capturado por script e herdado por toda a collection). Detalhe completo: `00_padrao_qualidade_e_arquitetura.md`, Seção 11.
- **A partir da etapa 02**: toda conversa nova do GPT recebe, além do `.md` da etapa, o `SPRINT-RESULTADO.md` atual — e devolve, ao final, o mesmo arquivo atualizado com a entrada da etapa que acabou de fazer (classes criadas, endpoints confirmados, decisões que o `.md` da etapa deixava a critério do GPT, retrofits pendentes). É a cadeia de continuidade entre conversas que não compartilham memória — detalhe completo: `00_padrao_qualidade_e_arquitetura.md`, Seção 12.
- Se o GPT sugerir qualquer mudança na forma como Keycloak ou PostgreSQL persistem dados, pare e não aplique — viola decisão arquitetural já tomada.
- **Stack é Java 25**, sem exceção — nunca aceitar "21+" ou qualquer versão menor sugerida pelo GPT.
- **Padrão de qualidade e arquitetura obrigatório em toda etapa que gera classes Java**: cobertura de 100% nas classes funcionais (DTOs de transporte puro ficam fora), JaCoCo configurado e falhando o build abaixo de 100%, Javadoc em toda interface/método de `Repository`, mappers via MapStruct, e entrega sempre em rodadas (entity+repo → mapper → service → controller), nunca tudo de uma vez. Detalhe completo, com exemplos de código: **`00_padrao_qualidade_e_arquitetura.md`** — leia esse arquivo uma vez antes de começar a executar qualquer etapa de domínio; cada etapa também traz um resumo dele, mas o arquivo é a fonte da verdade.

## Etapas

### Preâmbulo (ler antes, não é colado no GPT)

| Arquivo | Conteúdo |
|---|---|
| `00_padrao_qualidade_e_arquitetura.md` | Java 25, estrutura por camada, Javadoc em repository, MapStruct, JaCoCo (100% nas classes funcionais), entrega em rodadas — fonte da verdade do padrão repetido (resumido) em cada etapa abaixo |

### Fundação (Features de `implementation/001`)

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `01_estrutura_repo_e_env.md` | 001, 002 | Estrutura do repositório e variáveis de ambiente |
| `02_postgresql_aegis_e_keycloak.md` | 003, 004 | Os dois PostgreSQL dedicados e persistentes |
| `03_keycloak_persistente_e_realm.md` | 005, 006, 007 | Keycloak persistente + realm/client/roles + export |
| `04_backend_base_e_persistencia.md` | 008, 009 | Spring Boot base + conexão com PostgreSQL + Flyway |
| `05_security_resource_server_e_me.md` | 010, 011 | Resource Server com Keycloak + endpoint `/api/v1/me` |
| `06_auth_proxy_smtp_e_convite.md` | — | **[INSERIDA]** Auth proxy BFF (login/logout/refresh via backend), SMTP + MailHog dev, templates de e-mail FreeMarker Aegis, fluxo ponta a ponta de convite de usuário |
| `07_modelo_core_tenant_product_modulos.md` | 012, 013 | Tenant, Membership, Product, catálogo de módulos |
| `08_knowledge_graph_mvp.md` | 014, 015 | Knowledge Graph MVP (modelo + regras de consistência) |
| `09_health_status.md` | 016 | Health, info, readiness |

### Domínios de produto (especificação: `docs/trace/00_endpoints_esperados.md`)

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `10_tenants_crud_completo_e_product_assignment.md` | Trace, Seção C | CRUD completo de tenant (editar/excluir), escopo por papel, entidade `ProductAssignment` (atribuir produto a usuário) |
| `11_dominio_content.md` | Trace, Seção B.1 + Seção D.3 | Conteúdo, workflow editorial (máquina de estados), versionamento, publicação — agora com `summary`/`difficultyLevel` opcionais |
| `12_dominio_assets.md` | Trace, Seção B.2 | Upload de assets, metadados, tags |
| `13_dominio_forms.md` | Trace, Seção B.3 | Definição de formulários (builder) e submissions |
| `14_dominio_analytics.md` | Trace, Seção B.4 | KPIs, saúde do produto, canais (agregadores sobre os outros domínios) |
| `15_dominio_users.md` | Trace, Seção B.5 | Listagem/convite/detalhe de usuário via Keycloak Admin API (pré-requisito: etapa 06 — SMTP e `KeycloakAdminClient` já devem existir) |
| `16_dominio_audit.md` | Trace, Seção B.6 | Serviço central de auditoria e endpoints de leitura |
| `17_dominio_settings_e_dashboard.md` | Trace, Seções B.7, B.8 | Configurações, matriz de permissões, agregador do dashboard global |
| `18_knowledge_graph_extras_layout_e_orphans.md` | Trace, Seção B.9 + Seção D.2 | Posição persistente (`x`/`y`), `props` estruturadas, endpoint de nós órfãos, endpoint `.../preview` para referência inline (caso WikiDev) |

### Integração final e infraestrutura

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `19_build_frontend_react_no_backend.md` | 017, 018 (substituídas) | Build do React (não Angular) servido pelo Spring Boot |
| `20_dockerfile_e_compose_completo.md` | 019, 020 | Dockerfile do backend + Docker Compose completo (inclui MailHog da etapa 06) |
| `21_seed_inicial_e_grafo.md` | 021, 022 | Seed de tenants/produtos/usuários + seed do Knowledge Graph |
| `22_dominio_pages_secoes_e_blocos.md` | Trace, Seção D.1 (Sprint 11 do frontend) | Domínio `pages`: páginas compostas por seções/blocos tipados (hero, card-list, gallery, contact...) — pré-requisito só a etapa 11, pode ser feita a qualquer momento depois dela, numerada por último por ter sido adicionada depois |
| `23_openapi_testes_e_checklist_final.md` | 023, 024, 025 | Swagger, testes mínimos, checklist final do servidor |

### Domínios adicionais (surgidos depois do checklist final)

| Arquivo | Cobre | Conteúdo |
|---|---|---|
| `24_dominio_notification.md` | Sprint 14 do frontend (`docs/sprints/frontend/14_onboarding_real_e_sistema_de_notificacoes.md`) | Domínio `notification`: onboarding real + notificações direcionadas do Super Admin, com fan-out por destinatário — adicionada **depois** do checklist final (etapa 23) porque surgiu depois; ao terminá-la, não é preciso refazer o checklist inteiro da etapa 23, só confirmar que nada que já passava deixou de passar (`mvn clean verify` no projeto inteiro continua sendo o teste definitivo disso) |
| `25_templates_de_produto_e_seed_de_esqueleto.md` | ADR-0017 + auditoria de jornadas (`docs/implementation/005_aegis_pms_user_journeys.md`) | `POST /products` ganha esqueleto de páginas por `type` (catálogo fechado, 7 valores incluindo `"Custom"`) e pré-habilitação de módulos recomendados; `PUT /tenants/{tenantId}` ganha notificação automática ao mudar status (suspender/reativar) — depende das etapas 07, 22 e 24 já existirem |
| `26_dominio_feedback.md` | Sprint 18 do frontend (botão "Reportar problema" sem backend) | Domínio `feedback`: persiste o bug-report do usuário, com anexo reaproveitando o upload de assets já existente (etapa 12), nunca um mecanismo de upload próprio — depende das etapas 06 e 12 |

Ao final da etapa 23, o backend completo deve estar funcional: `docker compose up -d` sobe tudo (incluindo MailHog), Keycloak e PostgreSQL persistem dados entre restarts, o backend autentica via JWT **e expõe proxy de auth** (login/logout/refresh sem o frontend conhecer o Keycloak), o modelo core e o Knowledge Graph MVP funcionam, **todos os domínios de produto que o frontend já usa (content, pages, assets, forms, analytics, users, audit, settings, dashboard) respondem com os payloads exatos do trace report**, o fluxo de Super Admin (criar tenant → criar produto → atribuir usuário) funciona de ponta a ponta via API, e o build do frontend React é servido na mesma origem. Ao final da etapa 24, além de tudo isso, o sistema de notificações (onboarding + direcionadas) também está funcional. Ao final da etapa 25, criar um produto já entrega o esqueleto de páginas certo para o tipo escolhido (ou nada, se `"Custom"`), e suspender/reativar um tenant avisa os usuários afetados.
