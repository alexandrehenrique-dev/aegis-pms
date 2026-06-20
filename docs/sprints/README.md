# docs/sprints — Sprints executáveis do Aegis PMS

## O que é esta pasta

Cada arquivo aqui é uma sprint **autocontida e copiável**: você pode abrir o arquivo, copiar o conteúdo inteiro e colar em um agente para que ele execute a sprint do início ao fim, incluindo geração de código, validação e, quando aplicável, comandos de git.

**Divisão de execução entre agentes neste plano:**
- **Sprint 01, Sprint 09, Sprints 05–08, Sprint 10, Sprint 11, Sprint 12, Sprint 13 e Sprint 14** → executadas por você com **Claude/Cowork em modo agent**, com acesso direto a este repositório. (Sprints 03 e 04 estão obsoletas — ver nota em "Ordem recomendada".)
- **Sprint 02** → executada por você com o **GPT**, colando manualmente um arquivo por vez da pasta [`sprint-02-fundacao-backend-gpt/`](./sprint-02-fundacao-backend-gpt/00_indice_e_instrucoes.md) (23 etapas: 8 de fundação, 9 de domínios de produto — content/assets/forms/analytics/users/audit/settings/dashboard/tenants+ProductAssignment —, 1 do domínio `pages` (Sprint 11 do frontend), 4 de integração final/infra (build, docker compose, seed e checklist), e 1 do domínio `notification` (Sprint 14 do frontend, adicionada depois do checklist final)). Veja o motivo dessa divisão no índice daquela pasta.

Isto é diferente de `docs/implementation/figma-make-sprints-refinamento-v2.md`, que documenta sprints **já executadas** (Sprints 09–19) durante a geração do frontend via Figma Make. As sprints desta pasta usam numeração própria (01, 02, 03...) para não colidir com aquele histórico — elas tratam do que falta **depois** de Figma Make: arquitetura, integração real com backend, Keycloak, permissões por produto e o que o `implementation/001` ainda não foi executado.

## Como usar cada arquivo

1. Leia `AGENTS.md` e `CONTRIBUTING.md` na raiz do repo antes de rodar qualquer sprint — elas definem o fluxo de git que toda sprint deve seguir.
2. Abra o arquivo da sprint, copie o conteúdo inteiro.
3. Cole no agente de sua escolha junto com acesso ao repositório (ou cole o conteúdo dos arquivos que a sprint pede para o agente ler, se o agente não tiver acesso direto ao repo).
4. O agente deve seguir a sprint na ordem: Contexto → Objetivo → Tarefas → Critérios de aceite → Comandos de git.
5. Não avance para a próxima sprint sem fechar os critérios de aceite da atual.

## Ordem recomendada

> **Atualização:** as Sprints 03 e 04 foram escritas contra um `App.tsx` monolítico que não existe mais desde a Sprint 01 (já refatorada). A **Sprint 09** as substitui por completo — mesmo diagnóstico, mas desenhada contra a estrutura real do repositório (`src/app`, `src/core`, `src/domains`, `src/shared`), e já cobrindo o fluxo do Super Admin (criar tenant → criar produto → atribuir a um usuário) que nenhuma das duas cobria. **Execute a Sprint 09 no lugar de 03 e 04**; elas permanecem no repositório só como registro histórico do diagnóstico original. Veja a análise completa de endpoints e a justificativa desta reordenação em [`docs/trace/00_endpoints_esperados.md`](../trace/00_endpoints_esperados.md).

| # | Arquivo | Resolve |
|---|---|---|
| 01 | `01_refactor_frontend_e_setup_git.md` | Decisão de stack (React, ADR-0011), refatoração do `App.tsx` monolítico para arquitetura por domínio, remoção de artefatos do Figma Make, criação das branches `release`/`develop`, primeiro push do frontend |
| 02 | `02_fundacao_backend.md` → redireciona para [`sprint-02-fundacao-backend-gpt/`](./sprint-02-fundacao-backend-gpt/00_indice_e_instrucoes.md) (23 etapas) | Executa `implementation/001` **via GPT**: Docker Compose, PostgreSQL dedicado, Keycloak dedicado, Spring Boot modular, Knowledge Graph MVP (+ preview leve de nó), **todos os domínios de produto do trace report** (content, pages, assets, forms, analytics, users, audit, settings, dashboard, notification), tenants CRUD + `ProductAssignment`, e build do React (não Angular) servido pelo Spring Boot |
| 09 | `09_servicos_integracao_fluxo_super_admin_e_build.md` | **Execute aqui, no lugar de 03/04.** Camada `services/`+`contracts/` em todos os domínios, fluxo Super Admin (criar tenant → criar produto → atribuir a usuário) com navegação de volta, preparação para Keycloak na porta 8282, script de build para o backend servir o SPA |
| 10 | `10_refinamento_acoes_pendentes_ui.md` | Auditoria de 140+ botões sem `onClick` e campos não editáveis em todos os domínios; corrige os primitivos compartilhados (`SelectLike`/`Field`) e liga cada ação a algo real (service, navegação, drawer, toggle) — depende dos services da Sprint 09 |
| 11 | `11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md` | Validação dos fluxos do PMS contra 6 contratos de produto reais: seleção real de módulos na criação de produto, domínio `pages` (seções/blocos), correção do Knowledge Graph para referência inline (caso WikiDev), e mocks fiéis para os 6 produtos |
| 12 | `12_correcoes_pos_teste_editor_blocos_e_grafo.md` | Bugs encontrados testando a Sprint 11 rodando: logging de chamadas de API, fluxo "Novo conteúdo" abrindo a tela errada, `two-column` sem imagem (diverge da etapa 21 do backend), CRUD de itens em blocos de lista (galeria, cards, FAQ, formulários), preview mostrando mock fixo, `edgeType` inválido na referência inline do grafo, menu mobile e upload de arquivos (PDF) — depende da Sprint 11 já estar em execução |
| 13 | `13_engine_de_blocos_entidades_globais_e_responsividade.md` | Decisões de modelo resolvidas (separação Page/Content, navbar/footer como entidades globais, motor genérico de sub-blocos, permissões por widget, catálogo de blocos via service) + markdown, picker de mídia, bloco de música/redes sociais, drag-and-drop, entrega de formulário multicanal, Central de Ajuda escopada por módulo, e varredura responsiva ampla — depende da Sprint 12 |
| 14 | `14_onboarding_real_e_sistema_de_notificacoes.md` | Substitui o modal de boas-vindas baseado em `localStorage` (`DemoWelcomeModal.tsx`) por onboarding real persistido por usuário; cria o fluxo de notificações direcionadas do Super Admin (criar notificação, escolher tipo/destinatários, fan-out) a partir de `/select-tenant`; remodela o sino de notificações para consumir dados reais. Independente das demais, mas reaproveita o markdown já entregue pela Sprint 13 |
| ~~03~~ | ~~`03_jornadas_criacao_tenant_e_produto.md`~~ | ⚠️ Obsoleta — substituída pela Sprint 09. Mantida só como histórico do diagnóstico original |
| ~~04~~ | ~~`04_integracao_frontend_backend_contratos.md`~~ | ⚠️ Obsoleta — substituída pela Sprint 09. Mantida só como histórico do diagnóstico original |
| 05 | `05_roles_permissoes_features_por_produto.md` | Sistema de roles/permissões onde funcionalidades (ex.: Knowledge Graph) podem ser atribuídas/removidas por produto |
| 06 | `06_keycloak_login_ui_custom.md` | Integração com Keycloak real preservando a UI/UX atual da tela de login (sem expor a tela nativa do Keycloak) |
| 07 | `07_modo_mock_vs_real_via_env.md` | Frontend funciona standalone (mock) sem backend; com `.env`/flag de build, passa a consumir a API real |
| 08 | `08_gaps_pos_sprint19.md` | Lacunas que o histórico Figma Make (Sprints 09–19) marcou como concluídas mas não estão — ex.: jornada do Super Admin afirma "Criar Tenant" ✅ sem a tela existir |

## Regras válidas para todas as sprints

- Nunca commitar direto em `main`, `release` ou `develop` (ver `AGENTS.md` §2).
- Nunca mover Keycloak/PostgreSQL para bancos não dedicados nem remover persistência.
- Nunca reintroduzir Angular (ADR-0011) nem reescrever o frontend do zero.
- Toda sprint que altera arquitetura ou decisão relevante deve referenciar ou criar um ADR em `docs/adr/`.
