# ADR-0011 — Framework de Frontend: React (não Angular)

## Status

ACCEPTED

## Contexto

Dois documentos oficiais do Aegis davam respostas diferentes para "qual framework de frontend usar":

- `docs/implementation/001_aegis_pms_roteiro_fundacao_operacional.md` (Feature 017 e 018) especifica explicitamente **Angular** ("frontend Angular mais atual", `angular.json`, rotas Angular, build copiado para dentro do artefato do Spring Boot).
- `docs/implementation/011_aegis_pms_frontend_architecture_blueprint.md`, escrito depois e mais específico sobre arquitetura de UI, já assume **React** implicitamente (seção "Gerenciamento de Estado" → "Estado Local → React State").
- O frontend que de fato existe no repositório (`frontend/`) foi gerado via Figma Make em **React 18 + Vite + TailwindCSS**, e já passou por 11 sprints de refinamento documentados e validados (`docs/implementation/figma-make-sprints-refinamento-v2.md`, Sprints 09–19): theming completo (claro/escuro/auto), simulador de papéis com 5 personas e gating de permissão, workflow editorial com drag-and-drop, Knowledge Graph com análise de impacto, modal de feedback, microinterações e auditoria de jornada por persona.

Ou seja: a decisão de `001` nunca foi de fato executada — o projeto migrou para React em algum momento entre `001` e `011`/Figma Make, sem que um ADR registrasse essa mudança. Isso é a inconsistência que este ADR resolve.

## Decisão

O frontend oficial do Aegis é **React** (atualmente React 18 + Vite + TypeScript + TailwindCSS), não Angular.

`001` é tratado como **superado** neste ponto específico (Feature 017/018): a estrutura `angular.json` e as instruções Angular ali descritas não devem mais ser seguidas. As demais features de `001` (Docker, PostgreSQL dedicado, Keycloak dedicado, backend Spring Boot, Knowledge Graph MVP) permanecem válidas e não são afetadas por este ADR.

## Justificativa

1. **Não há requisito técnico que favoreça Angular.** ADR-0009 (SPA servida pelo Spring Boot, mesma origem) é agnóstico de framework — tanto React quanto Angular satisfazem build estático + fallback de rotas. `001` declarava Angular sem justificativa técnica registrada (nenhuma comparação, nenhum trade-off documentado), o que sugere uma escolha inicial não revisitada, não uma decisão deliberada.
2. **Já existe trabalho real, validado e funcional em React.** Os Sprints 09–19 não são um esqueleto — implementam e validam (com transcrição de feedback por sprint) autenticação, navegação, theming, simulação de papéis, workflow com drag-and-drop, Knowledge Graph orientado a entidades de negócio e auditoria de UX por jornada de usuário. Reescrever em Angular descartaria esse trabalho sem ganho funcional.
3. **Reescrever não resolve nenhuma lacuna real.** Os problemas reais identificados nesta análise — ausência de tela de criação de tenant, zero integração com backend/Keycloak, zero modelagem de feature por produto — existem independentemente do framework e teriam que ser resolvidos de novo após uma migração, dobrando o esforço sem dobrar o valor.
4. **`011` (mais recente e mais específico que `001`) já havia, na prática, abandonado Angular** ao adotar terminologia e estratégia de estado do React. Este ADR apenas torna explícita uma decisão que já estava implícita.

## Consequências

Positivas:
- Elimina a contradição entre `001` e `011`/código real.
- Preserva os 11 sprints de refinamento já validados no frontend atual.
- Permite que as sprints de refatoração (`docs/sprints/`) foquem em arquitetura modular e integração — não em uma reescrita de framework.

Negativas / trade-offs:
- `001` precisa de uma nota de ressalva nas seções de Feature 017/018 apontando para este ADR (feito).
- Qualquer expectativa anterior de "frontend Angular mais atual" (ex.: em conversas externas ao repositório, propostas comerciais, etc.) precisa ser realinhada manualmente fora deste repositório.

## Alternativas Consideradas

- **Migrar tudo para Angular conforme `001`**: rejeitada. Custo de reescrita completa (App Shell, 9 módulos de domínio, theming, DnD, Knowledge Graph, simulador de papéis) sem nenhum ganho técnico identificado; nenhum requisito do Aegis (multi-tenant, multi-produto, RBAC, contratos JSON) depende de Angular especificamente.
- **Adotar Angular apenas para novos módulos, mantendo React nos existentes**: rejeitada. Duas stacks de frontend simultâneas aumentam custo de manutenção, dobram a curva de aprendizado para agentes de IA e contradizem o ARTIGO XII da Constituição (Simplicidade).

## Impactos

- **Frontend (Eirene):** nenhuma mudança de framework; foco passa a ser refatoração arquitetural (ver `docs/sprints/01_*.md`) — sair do arquivo único `App.tsx` (~1030 linhas) para a estrutura por domínio definida em `011`.
- **Documentação:** `implementation/001` recebe nota de superação nas seções Angular; `WORKTREE.md` e `README.md` devem referenciar este ADR ao descrever o frontend.
- **Agentes de IA:** qualquer sprint ou prompt que mencione "Angular" para o frontend do Aegis deve ser tratado como desatualizado; a stack vigente é React.

## Links Relacionados

- ADR-0009 (SPA servida pelo Spring Boot).
- `docs/implementation/001_aegis_pms_roteiro_fundacao_operacional.md` (Feature 017/018 — superadas neste ponto).
- `docs/implementation/011_aegis_pms_frontend_architecture_blueprint.md` (arquitetura de referência para a refatoração).
- `docs/implementation/figma-make-sprints-refinamento-v2.md` (histórico de sprints já executados no frontend React atual).
- `docs/sprints/01_refactor_frontend_e_setup_git.md` (sprint que executa a refatoração arquitetural).
