# ADR-0015 — Módulos de produto como portão de acesso (module-gating)

## Status

ACCEPTED

## Contexto

A etapa 06 (`sprint-02-fundacao-backend-gpt/06_modelo_core_tenant_product_modulos.md`) já modela `ProductModule` (`productId`, `moduleKey`, `enabled`) e expõe `POST/.../modules/{moduleKey}/enable|disable`. Isso permite **ligar/desligar** um módulo num produto, mas uma auditoria de consistência feita antes do início da implementação encontrou que **nenhuma outra etapa de domínio** (`07` Knowledge Graph, `10` content, `11` assets, `12` forms, `13` analytics, `17` Knowledge Graph extras, `21` pages) verifica, nos próprios endpoints, se o módulo correspondente está habilitado no produto antes de processar a requisição.

Na prática, hoje a especificação permite que um produto com `KNOWLEDGE_GRAPH` desabilitado (ex.: `maestro-beton`, etapa 20) continue respondendo normalmente a `POST /products/{id}/graph/nodes` — o toggle existe, mas não é respeitado por ninguém. O mesmo vale para `content`, `pages`, `assets`, `forms` e `analytics`: a única coisa que hoje impede um produto de usar um módulo "desabilitado" é a UI não mostrar o botão — o que viola a regra já registrada na Sprint 05 do frontend ("UI nunca decide permissão. Permissões vêm do Core.").

## Decisão

Toda rota de domínio cujo `moduleKey` correspondente pode ser desabilitado por produto **verifica `ProductModule.enabled` antes de processar a requisição**, retornando `403` com corpo padronizado se o módulo estiver desabilitado — independentemente do papel do usuário (nem `SUPER_ADMIN` contorna um módulo desabilitado; para usar a funcionalidade, primeiro habilita o módulo).

Mecanismo (implementado uma vez, na etapa 06, reaproveitado por todas as etapas de domínio seguintes): anotação `@RequireModule(ModuleKey.X)` em métodos de controller, interceptada por um `ModuleAccessAspect` (Spring AOP) que resolve o `productId` do path da requisição, consulta `ProductModuleRepository.existsByProductIdAndModuleKeyAndEnabledTrue(...)` e lança uma exceção mapeada para `403` (`@ExceptionHandler` devolvendo `{"error": "MODULE_DISABLED", "moduleKey": "FORMS"}`) se não encontrar.

Mapeamento módulo → domínio gateado:

| `moduleKey` | Domínio/etapa gateada |
|---|---|
| `CONTENT` | `10_dominio_content.md` |
| `PAGES` | `21_dominio_pages_secoes_e_blocos.md` |
| `ASSETS` | `11_dominio_assets.md` |
| `FORMS` | `12_dominio_forms.md` (forms e submissions) |
| `ANALYTICS` | `13_dominio_analytics.md` |
| `KNOWLEDGE_GRAPH` | `07_knowledge_graph_mvp.md` + `17_knowledge_graph_extras_layout_e_orphans.md` |

Domínios **não** gateados por módulo (sempre disponíveis para quem tem permissão de papel, independente de toggle de módulo): `tenants`/`ProductAssignment` (09), `users` (14), `audit` (15), `settings`/`dashboard` (16), `notification` (23) — são infraestrutura/fundação do produto, não "funcionalidades" que um tenant escolhe ligar ou desligar.

`KNOWLEDGE_GRAPH` possui dependências explícitas de módulo: só pode ser habilitado quando `CONTENT` **e** `ASSETS` estiverem habilitados no mesmo produto. O grafo nasce de entidades de conteúdo e pode referenciar mídia/arquivos; permitir KG sem Assets deixou jornadas incoerentes (nó visualizando relação, mas sem biblioteca de mídia vinculável). Pela mesma regra, `CONTENT` ou `ASSETS` não podem ser desabilitados enquanto `KNOWLEDGE_GRAPH` estiver ativo. Backend e frontend aplicam a mesma dependência; a UI apenas antecipa o bloqueio, a API continua sendo a autoridade.

A listagem resumida de produtos (`ProductSummary`) expõe os módulos realmente habilitados pelo backend. O frontend deve usar essa lista quando existir para contar/exibir módulos, em vez de recalcular pelo tipo do produto; defaults por tipo são apenas fallback para mocks/offline e para criação de produto antes da persistência.

## Consequências

Positivas:
- O toggle de módulo (Sprint 05 do frontend, etapa 06 do backend) passa a ter efeito real, não só cosmético — fecha o gap entre "UI esconde o botão" e "API de fato bloqueia".
- Mecanismo único (`@RequireModule` + aspecto), implementado uma vez, sem duplicar a checagem em cada controller manualmente.
- Erro padronizado (`MODULE_DISABLED`) permite ao frontend (Sprint 05/07) tratar esse caso de forma genérica, sem parsear mensagem de erro.

Negativas / trade-offs:
- Toda nova etapa de domínio que crie endpoints sob um `moduleKey` gateável precisa lembrar de anotar o controller — se esquecido, o endpoint fica silenciosamente sem gate (mesmo risco que motivou esta ADR). Mitigação: etapa 22 (checklist final) ganha um item específico para isso.
- Aspecto AOP adiciona uma camada de indireção a mais para depurar em caso de erro 403 inesperado — mitigado por log explícito no aspecto (`productId`, `moduleKey`, resultado).

## Alternativas Consideradas

- **Checagem manual no início de cada método de service/controller**: rejeitado — é exatamente o padrão que já existia (nenhuma etapa fazia isso) e que motivou o gap; duplicar a checagem por domínio aumenta a chance de alguém esquecer.
- **Checagem no frontend apenas**: rejeitado — contraria diretamente a regra já formalizada na Sprint 05 ("UI nunca decide permissão. Permissões vêm do Core"); um cliente que chama a API direto (Bruno, integração externa) ignoraria completamente o toggle.
- **Retornar 404 em vez de 403 quando módulo desabilitado**: rejeitado — o produto e o recurso existem; 404 sugeriria erro de URL/recurso inexistente, confundindo depuração. 403 com corpo explícito (`MODULE_DISABLED`) é inequívoco.

## Impactos

- **Backend**: etapa 06 ganha a tarefa de implementar `@RequireModule`/`ModuleAccessAspect`; etapas 07, 10, 11, 12, 13, 17, 21 ganham a tarefa de anotar seus controllers e um cenário de teste (`módulo desabilitado → 403 MODULE_DISABLED`) nos critérios de aceite; etapa 22 (checklist final) ganha um item de auditoria cruzada confirmando que todo domínio gateável está de fato anotado.
- **Frontend**: auditoria de refinamento (Sprint 15) encontrou que isso já é um problema **hoje**, não só uma preocupação futura — `AppShell.tsx` (sidebar) filtra item de navegação só por `roleVisibleNav[viewAsRole]` (papel), sem checar `product.modulesList`; um produto sem Knowledge Graph habilitado ainda mostra o item "Knowledge Graph" na sidebar para quem tem permissão de papel. Corrigido na Sprint 15, Tarefa E — filtro passa a exigir as duas condições (módulo do produto **e** papel do usuário), não uma ou outra. Quando o backend devolve `enabledModules`, essa lista é a fonte de verdade visual para cards, seletor de produto e catálogo.

## Links Relacionados

- ADR-0001 (Product First).
- `docs/sprints/05_roles_permissoes_features_por_produto.md` (toggle de módulo, ainda não executada).
- `docs/sprints/sprint-02-fundacao-backend-gpt/06_modelo_core_tenant_product_modulos.md`, `22_openapi_testes_e_checklist_final.md`.
