# BUG-SPRINT-09 — Administracao, Knowledge Graph, Analytics e UX operacional

> **Branch de implementacao:** `bugfix/sprint-09-admin-kg-analytics-ux`
>
> **Status:** AGUARDANDO VALIDACAO DO USUARIO
>
> **Data de abertura:** 2026-07-10
>
> **Origem:** rodada de validacao manual pos BUG-SPRINT-08.

## Contexto

Esta sprint corrige regressos e lacunas remanescentes em jornadas administrativas, Knowledge Graph, Analytics, Conteudo, Assets e Auditoria.

O foco principal e remover dados mockados do modo API, respeitar module-gating e permissoes por papel, simplificar fluxos confusos e manter a experiencia responsiva em tema claro e escuro.

## Premissas

1. **Produto continua sendo o centro.** Perfis operacionais (Product Manager, Editor, Viewer) devem navegar por produtos e nao precisar conhecer tenants, mesmo quando participarem de produtos em tenants diferentes.
2. **Tenant Admin e papel administrativo de tenant.** Ele administra o tenant e seus produtos, usuarios e configuracoes, mas nao deve ganhar acesso operacional irrestrito ao conteudo de todos os produtos.
3. **Sem mock em modo API.** Dados demonstrativos podem existir apenas no modo mock/local sem backend. Em modo API, telas devem usar dados reais ou estado vazio honesto.
4. **Module-gating e permissao devem ser coerentes.** Se um modulo nao esta habilitado, a UI nao deve oferecer a acao que a API vai negar.
5. **Documentacao viva.** Decisoes sobre papeis, modulos e jornada administrativa devem atualizar ADRs aplicaveis.

## Bugs e ajustes

| ID | Area | Problema | Criterio de aceite |
|---|---|---|---|
| S09-01 | Auditoria | Detalhe em `/audit/{eventId}` aparenta usar mock estatico e nao refletir o evento real. | A tela carrega o detalhe real pelo id, exibe antes/depois/payload/metadados quando existirem e mostra estado vazio quando o evento nao tiver aquele campo. |
| S09-02 | Produto / modulos | Tipo `Library/Books/Music` nao permite habilitar `Paginas`. | `Paginas` fica disponivel como modulo opcional para esse tipo, sem quebrar defaults existentes. |
| S09-03 | Dashboard / usuarios | Contador de usuarios ativos diverge da lista do produto. | Dashboard e lista usam a mesma regra: somente acessos ativos e relevantes ao contexto exibido. |
| S09-04 | Notificacoes / tutorial | Modal "Novo modulo de paginas" abre repetidamente e pode substituir tutorial real. | Modal/tutorial abre apenas uma vez por usuario/contexto, nao renasce por notificacao antiga do Bruno e nao bloqueia jornadas recorrentes. |
| S09-05 | Conteudo / module-gating | WikiDev sem `Paginas` habilitado ainda oferece criar `Pagina de produto`; a API responde 403. | Tipo `Pagina de produto` so aparece quando `Paginas` estiver habilitado e o usuario tiver permissao. |
| S09-06 | Analytics | Acoes "Investigar" e "Ver detalhe" em cards nao executam navegacao/acao. | Botoes levam para a lista/tela filtrada correta ou ficam desabilitados com motivo claro quando nao houver destino. |
| S09-07 | Analytics / saude | Tela Saude do produto mostra valores com aparencia de mock. | A tela usa metricas reais disponiveis ou estados "sem dados" rastreaveis, sem numeros ficticios. |
| S09-08 | Knowledge Graph | Timeline de relacoes mostra entradas sem origem clara ou mockadas. | Timeline mostra relacoes reais do produto, com labels legiveis, ou estado vazio. |
| S09-09 | Knowledge Graph | Canvas nao possui zoom. | Canvas oferece zoom in/out/reset e mantem navegabilidade com muitos nodes/edges. |
| S09-10 | Knowledge Graph | Orfaos mostra registros mockados. | Orfaos sao derivados de dados reais do produto ou exibem vazio honesto. |
| S09-11 | Knowledge Graph | Insights mostra dados mockados e subtelas quebram. | Insights sao derivados de dados reais ou mostram vazio; acoes nao quebram e tem destino coerente. |
| S09-12 | Conteudo / versoes | Historico em `/content/{id}/versions` mostra versoes mockadas. | Historico usa endpoint real ou estado vazio honesto; botoes sem suporte ficam indisponiveis com explicacao. |
| S09-13 | Tenant Admin / convite | Criacao de Tenant Admin mostra produtos, mas nao o tenant administrado. | Fluxo de convite de Tenant Admin explicita o tenant e nao exige escolher produtos operacionais. |
| S09-14 | Tenant Admin / permissoes | Tenant Admin recebe acesso full operacional a todos os produtos. | Tenant Admin gerencia tenant/produtos/usuarios/configuracoes, mas nao ganha acesso operacional a conteudo/assets/forms/analytics/KG sem papel de produto especifico. |
| S09-15 | Assets | Upload multiplo permite selecionar varios arquivos, mas metadados laterais editam somente o primeiro. | Cada arquivo selecionado pode receber metadados proprios antes do upload. |
| S09-16 | Evento / imagem | Evento com imagem vinculada nao mostra preview nem indicacao de imagem existente. | Drawer de evento mostra preview/estado da imagem atual e permite trocar/remover. |
| S09-17 | Knowledge Graph | Botao "Resolver orfaos" nao faz nada. | Acao tem comportamento definido: arquivar, vincular, revisar ou abrir fluxo guiado; se nao houver selecao, orienta o usuario. |
| S09-18 | UI / viewport | Existe risco de pop-up sair da viewport. | Jornadas com modais/drawers/popovers sao revisadas em tamanhos desktop e menores; qualquer overflow encontrado e corrigido. |

## Atualizacoes documentais esperadas

- ADR-0015, se a regra de modulos para `Library/Books/Music` ou dependencias de KG for ajustada.
- ADR-0019, se a visibilidade de Tenant Admin/Super Admin/roles operacionais for refinada.
- Este arquivo deve ser atualizado ao final com status, comandos de validacao e decisoes efetivamente aplicadas.

## Validacao obrigatoria antes de encerrar

- Backend: `mvn clean verify` com JaCoCo 100%.
- Frontend: `npm run lint`, `npm run typecheck`, `npm run build`.
- Bruno/smoke aplicavel: validar que as colecoes e jornadas criticas continuam verdes.
- UX: revisar tema dark/light e responsividade das telas alteradas.
- Dados mockados: buscar literais de negocio restantes nas telas tocadas e garantir que nao vazem em modo API.

## Resultado da implementacao

Status em 2026-07-10: implementado e aguardando validacao manual do usuario.

Correcoes aplicadas:

- Auditoria passou a consumir o detalhe real do evento, com estados vazios quando antes/depois/payload nao existirem.
- `Library/Books/Music` recebeu `Paginas` como modulo opcional.
- Contadores de usuarios ativos foram alinhados ao escopo do produto e aos usuarios com acesso ativo.
- A notificacao/tutorial de "Novo modulo de paginas" deixou de abrir repetidamente; o onboarding agora inicia como tutorial controlado por contexto.
- Criacao de conteudo respeita module-gating: `Pagina de produto` nao aparece quando `Paginas` nao esta habilitado.
- Cards de Analytics ganharam destinos reais ou estados honestos sem numeros ficticios.
- Knowledge Graph passou a usar dados reais/estados vazios para timeline, orfaos, insights e canvas, com zoom no canvas.
- Historico de versoes passou a usar dados reais/estado vazio, sem lista mockada.
- Tenant Admin foi restringido a governanca do tenant, sem acesso operacional automatico a conteudo de todos os produtos.
- Upload multiplo de assets permite metadados por arquivo.
- Drawer de eventos indica imagem vinculada e permite trocar/remover.
- Smoke tests foram ajustados para refletir as novas jornadas de onboarding e Knowledge Graph.
- Apontamento Sonar `java:S5976` em `ProductAccessResolverTest` foi corrigido com `@ParameterizedTest`.

Documentacao viva atualizada:

- `docs/adr/ADR-0017-templates-de-produto-e-esqueleto-de-paginas.md`
- `docs/adr/ADR-0018-escopo-super-admin-plataforma.md`
- `docs/adr/ADR-0019-visibilidade-de-produtos-por-papel.md`

Validacoes executadas:

- `cd backend && mvn -Dtest=ProductAccessResolverTest test` — passou.
- `cd frontend && node tests/run-all.mjs` — 23/23 jornadas passaram.
- `cd frontend && npm run lint` — passou.
- `cd frontend && npm run typecheck` — passou.
- `cd frontend && npm run build` — passou, com apenas o aviso conhecido de chunk grande do Vite.
- `cd backend && mvn clean verify` — passou; 1671 testes, JaCoCo com todos os checks atendidos.
- `git diff --check` — passou.
