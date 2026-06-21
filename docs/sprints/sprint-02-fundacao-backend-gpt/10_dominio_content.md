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

> **Convenção de markdown (Sprint 13 do frontend):** qualquer campo de texto longo dentro de `snapshotJson` (corpo do artigo, `summary`) é **markdown**, nunca HTML bruto. O backend não precisa converter markdown para HTML (isso é responsabilidade do renderer do frontend), mas deve armazenar a string exatamente como recebida e aplicar a regra de sanitização da Seção C antes de persistir.

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
- **Sanitização de markdown** (não negociável, mesma regra que vale para `pages` na etapa 21): rejeitar/limpar tags HTML fora de uma allowlist mínima (`p, strong, em, ul, ol, li, blockquote, h2, h3, a, br`), bloquear `javascript:`/`data:` em links, nunca aceitar `<script>`/`<iframe>` dentro do markdown. Aplicar ao salvar (`PUT`/`transition`), não só na hora de renderizar.
- **Integração com Knowledge Graph** (ver etapa 07, Seção C.1): ao salvar `bodyMarkdown` contendo referências inline `{{kg-ref:nodeId:Label}}`, chamar o equivalente de `KnowledgeGraphService` para garantir o node do conteúdo e criar as edges — `kg-ref` para `nodeId` inexistente é rejeitado (400) nesta etapa, antes de persistir o conteúdo.
- **Isolamento por produto** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): conteúdo de um produto que não está no escopo do usuário autenticado retorna 404, nunca 403, em qualquer endpoint da Seção B.
- **Module-gating** (`00_padrao_qualidade_e_arquitetura.md`, Seção 9.2): `ContentController` anotado com `@RequireModule(ModuleKey.CONTENT)` — produto com módulo `CONTENT` desabilitado retorna 403 `MODULE_DISABLED`.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `ContentRepository`/`ContentVersionRepository`. Mappers via MapStruct (`ContentMapper`, `ContentVersionMapper`). 100% de cobertura nas classes funcionais, incluindo a lógica de sanitização de markdown (`MarkdownSanitizer`/equivalente) e a máquina de estados (`wfAllowed`).
- Entregar em rodadas:
  1. `Content`, `ContentVersion` (entities) + `ContentRepository`, `ContentVersionRepository` + testes `@DataJpaTest`.
  2. `ContentMapper`, `ContentVersionMapper` (MapStruct) + testes de mapper.
  3. `ContentService` (transições, publicação, sanitização) + testes com mocks — cada transição permitida E cada transição rejeitada da Seção A tem teste próprio; sanitização tem teste para cada caso da allowlist (tag permitida passa, tag fora da lista é removida, `javascript:` é bloqueado).
  4. `ContentController` (endpoints da Seção B) + testes `@WebMvcTest` (incluindo `EDITOR` tentando publicar → 403) + validação via `curl`.

## Critérios de aceite

- [ ] Listar, ver detalhe e editar conteúdo funcionam.
- [ ] Transições permitidas funcionam e geram nova versão.
- [ ] Transições não permitidas (ex.: `Draft → Published`) são rejeitadas com 400.
- [ ] `EDITOR` tentando publicar é rejeitado com 403.
- [ ] Versões de um conteúdo podem ser listadas em ordem cronológica.
- [ ] Conteúdo de produto fora do escopo do usuário retorna 404 (não 403).
- [ ] Produto com módulo `CONTENT` desabilitado retorna 403 `MODULE_DISABLED` em qualquer endpoint desta etapa.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).
- [ ] `ContentRepository`/`ContentVersionRepository` têm Javadoc na interface e em todo método.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

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
