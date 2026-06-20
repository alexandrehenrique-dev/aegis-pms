# Etapa 10 — Domínio `content` (editorial, workflow, versões, publicação)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída (Product já existe). Usa o package `content` já previsto na etapa 04.

## Contexto fixo

O frontend tem telas inteiras de conteúdo (`EditorialDashboard`, `ContentDataGrid`, `WorkflowBoard`, `ContentEditor`, `PublishPanel`, `VersionsPage`, `VersionCompareView`) rodando 100% sobre mock — nenhum endpoint de conteúdo existe ainda no backend. Os payloads abaixo são os mesmos tipos TypeScript que `frontend/src/domains/content/contracts/responses.ts` já define e que `domains/content/services/contentService.ts` (mock) já implementa hoje — não são suposições, é a especificação literal de `docs/trace/00_endpoints_esperados.md` (Seção B.1).

## Objetivo

CRUD de conteúdo, máquina de estados de workflow editorial, versionamento e publicação, todos escopados por produto.

## Tarefas

### A. Entidades

**Content**: `id`, `tenantId`, `productId`, `title`, `type`, `lang`, `authorSubject`, `status` (`WFStatus`, ver abaixo), `publication`, `currentVersion`, `summary?` (string, opcional), `difficultyLevel?` (`"beginner"|"intermediate"|"advanced"`, opcional), `createdAt`, `updatedAt`.

> `summary`/`difficultyLevel` foram adicionados depois da análise de `docs/trace/00_endpoints_esperados.md` (Sprint 11 do frontend) — são campos de primeira classe, não metadata solta, porque produtos de base de conhecimento (ex. WikiDev) precisam filtrar/ordenar artigos por dificuldade e mostrar resumo em previews leves (ver etapa 17, endpoint `.../graph/nodes/{nodeId}/preview`). Para outros tipos de produto, ficam `null`.

**ContentVersion**: `id`, `contentId`, `versionLabel`, `snapshotJson` (conteúdo serializado no momento da versão), `createdBySubject`, `createdAt`.

`WFStatus = "Draft" | "In Review" | "Published" | "Archived"`.

Transições permitidas (espelhando `wfAllowed` já modelado em `frontend/src/domains/content/mocks/content.mocks.ts` — não inventar uma máquina de estados diferente):
- `Draft → In Review`
- `In Review → Draft` (devolvida) ou `In Review → Published`
- `Published → Archived`
- `Archived → Draft` (reabertura)

Qualquer outra transição (ex.: `Draft → Published` direto) é rejeitada com 400.

### B. Endpoints

```txt
GET  /api/v1/products/{productId}/content
GET  /api/v1/products/{productId}/content/{contentId}
PUT  /api/v1/products/{productId}/content/{contentId}
POST /api/v1/products/{productId}/content/{contentId}/transition
GET  /api/v1/products/{productId}/content/{contentId}/versions
POST /api/v1/products/{productId}/content/{contentId}/publish
GET  /api/v1/products/{productId}/content/edit-events
GET  /api/v1/products/{productId}/content/workflow-items
```

Payloads:

```ts
type ContentRow = {
  title: string; type: string; lang: string; author: string;
  status: string; updatedAt: string; publication: string; version: string;
  summary?: string; difficultyLevel?: "beginner" | "intermediate" | "advanced";
};
// GET /content → ContentRow[]

type TransitionRequest = { from: "Draft"|"In Review"|"Published"|"Archived"; to: "Draft"|"In Review"|"Published"|"Archived"; comment?: string };
// POST /transition

type WFItem = { id: string; title: string; type: string; lang: string; author: string; status: string; version: string };
// GET /workflow-items → WFItem[] (estado inicial do Kanban de WorkflowBoard.tsx)
```

`GET /edit-events` retorna `string[]` (texto livre, ex. "Editor criou rascunho da página Agenda.") — timeline de atividade editorial simples; não precisa de entidade própria agora, pode ser derivado de `ContentVersion`/auditoria.

### C. Regras de negócio

- Só quem tem papel `SUPER_ADMIN`, `TENANT_ADMIN` ou `PRODUCT_MANAGER` pode publicar (`transition` para `Published`) — `EDITOR`/`VIEWER` podem mover até `In Review`, nunca além.
- Toda transição grava uma `ContentVersion` nova.
- `publish` é um atalho para `transition` com `to: "Published"` que também atualiza `publication` (data/canal).

## Critérios de aceite

- [ ] Listar, ver detalhe e editar conteúdo funcionam.
- [ ] Transições permitidas funcionam e geram nova versão.
- [ ] Transições não permitidas (ex.: `Draft → Published`) são rejeitadas com 400.
- [ ] `EDITOR` tentando publicar é rejeitado com 403.
- [ ] Versões de um conteúdo podem ser listadas em ordem cronológica.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/content

curl -X POST http://localhost:8080/api/v1/products/<productId>/content/<contentId>/transition \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"from":"Draft","to":"In Review"}'

# esperado 400: transicao invalida
curl -X POST http://localhost:8080/api/v1/products/<productId>/content/<contentId>/transition \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"from":"Draft","to":"Published"}'
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio content com workflow editorial e versionamento"
```
