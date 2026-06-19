# docs/sprints — Sprints executáveis do Aegis PMS

## O que é esta pasta

Cada arquivo aqui é uma sprint **autocontida e copiável**: você pode abrir o arquivo, copiar o conteúdo inteiro e colar em um agente para que ele execute a sprint do início ao fim, incluindo geração de código, validação e, quando aplicável, comandos de git.

**Divisão de execução entre agentes neste plano:**
- **Sprint 01 e Sprints 03–08** → executadas por você com **Claude/Cowork em modo agent**, com acesso direto a este repositório.
- **Sprint 02** → executada por você com o **GPT**, colando manualmente um arquivo por vez da pasta [`sprint-02-fundacao-backend-gpt/`](./sprint-02-fundacao-backend-gpt/00_indice_e_instrucoes.md) (12 etapas, da fundação do Docker Compose até servir o React pelo Spring Boot). Veja o motivo dessa divisão no índice daquela pasta.

Isto é diferente de `docs/implementation/figma-make-sprints-refinamento-v2.md`, que documenta sprints **já executadas** (Sprints 09–19) durante a geração do frontend via Figma Make. As sprints desta pasta usam numeração própria (01, 02, 03...) para não colidir com aquele histórico — elas tratam do que falta **depois** de Figma Make: arquitetura, integração real com backend, Keycloak, permissões por produto e o que o `implementation/001` ainda não foi executado.

## Como usar cada arquivo

1. Leia `AGENTS.md` e `CONTRIBUTING.md` na raiz do repo antes de rodar qualquer sprint — elas definem o fluxo de git que toda sprint deve seguir.
2. Abra o arquivo da sprint, copie o conteúdo inteiro.
3. Cole no agente de sua escolha junto com acesso ao repositório (ou cole o conteúdo dos arquivos que a sprint pede para o agente ler, se o agente não tiver acesso direto ao repo).
4. O agente deve seguir a sprint na ordem: Contexto → Objetivo → Tarefas → Critérios de aceite → Comandos de git.
5. Não avance para a próxima sprint sem fechar os critérios de aceite da atual.

## Ordem recomendada

| # | Arquivo | Resolve |
|---|---|---|
| 01 | `01_refactor_frontend_e_setup_git.md` | Decisão de stack (React, ADR-0011), refatoração do `App.tsx` monolítico para arquitetura por domínio, remoção de artefatos do Figma Make, criação das branches `release`/`develop`, primeiro push do frontend |
| 02 | `02_fundacao_backend.md` → redireciona para [`sprint-02-fundacao-backend-gpt/`](./sprint-02-fundacao-backend-gpt/00_indice_e_instrucoes.md) (12 etapas) | Executa `implementation/001` **via GPT**: Docker Compose, PostgreSQL dedicado, Keycloak dedicado, Spring Boot modular, Knowledge Graph MVP, e build do React (não Angular) servido pelo Spring Boot |
| 03 | `03_jornadas_criacao_tenant_e_produto.md` | Tela de criação de **tenant** (inexistente hoje) e revisão da criação de **produto** (já existe, mas mockada) |
| 04 | `04_integracao_frontend_backend_contratos.md` | Camada de serviços/contratos do frontend consumindo a API real do backend, substituindo os dados mockados |
| 05 | `05_roles_permissoes_features_por_produto.md` | Sistema de roles/permissões onde funcionalidades (ex.: Knowledge Graph) podem ser atribuídas/removidas por produto |
| 06 | `06_keycloak_login_ui_custom.md` | Integração com Keycloak real preservando a UI/UX atual da tela de login (sem expor a tela nativa do Keycloak) |
| 07 | `07_modo_mock_vs_real_via_env.md` | Frontend funciona standalone (mock) sem backend; com `.env`/flag de build, passa a consumir a API real |
| 08 | `08_gaps_pos_sprint19.md` | Lacunas que o histórico Figma Make (Sprints 09–19) marcou como concluídas mas não estão — ex.: jornada do Super Admin afirma "Criar Tenant" ✅ sem a tela existir |

## Regras válidas para todas as sprints

- Nunca commitar direto em `main`, `release` ou `develop` (ver `AGENTS.md` §2).
- Nunca mover Keycloak/PostgreSQL para bancos não dedicados nem remover persistência.
- Nunca reintroduzir Angular (ADR-0011) nem reescrever o frontend do zero.
- Toda sprint que altera arquitetura ou decisão relevante deve referenciar ou criar um ADR em `docs/adr/`.
