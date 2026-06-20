# Etapa 17 — Knowledge Graph: layout persistente, props ricas e nós órfãos

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 07 concluída (Knowledge Graph MVP).

## Contexto fixo

A etapa 07 criou `GraphNode`/`GraphEdge` com `metadataJson` genérico. O frontend (`GraphCanvasView.tsx`, `EntityDetails.tsx`, `RelationshipExplorer.tsx`, `OrphanEntityTable.tsx`) usa um shape mais rico, com posição persistente no canvas e uma lista chave/valor estruturada — conforme `docs/trace/00_endpoints_esperados.md` (Seção B.9):

```ts
type KGEntityType = "Tenant" | "Produto" | "Página" | "Asset" | "Formulário" | "Submission" | "Lead" | "Categoria" | "Tag" | "Autor" | "SEO";
type KGNode = { id: string; label: string; type: KGEntityType; status: string; x: number; y: number; props: { k: string; v: string }[] };
type KGEdge = { from: string; to: string; verb: string };
```

## Objetivo

`GraphNode` ganha posição persistente (`x`/`y`) e `props` estruturadas; novo endpoint expõe nós órfãos (sem nenhuma edge); novo endpoint de **preview leve** dá suporte a referências inline dentro de conteúdo (caso WikiDev, ver Tarefa D).

## Tarefas

### A. Ajuste de `GraphNode`

Adicionar colunas `x` (float), `y` (float) — posição no canvas, atualizada quando o usuário arrasta um nó em `GraphCanvasView.tsx` (precisa de um `PUT` ou `PATCH` para isso, ver Tarefa B). Se a coluna `metadataJson` da etapa 07 já cobre um JSON livre, decidir: ou `props` (`{k,v}[]`) é só uma forma de ler/escrever `metadataJson` de forma estruturada (mapear no DTO, sem nova coluna), ou criar uma tabela `graph_node_props` própria — qualquer uma das duas é aceitável, documentar a escolha no código.

### B. Endpoints novos/ajustados

```txt
PATCH /api/v1/products/{productId}/graph/nodes/{nodeId}/position   body: { x: number; y: number }
GET   /api/v1/products/{productId}/graph/orphans
```

`GET /graph/orphans` retorna `KGNode[]` — nós que não aparecem como `sourceNodeId` nem `targetNodeId` de nenhuma `GraphEdge` do produto. Motivo: `OrphanEntityTable.tsx`, hoje sem nenhum tipo formal no frontend — ao implementar este endpoint, também é necessário definir e documentar (atualizar `docs/trace/00_endpoints_esperados.md`) o shape de cada linha da tabela, incluindo o campo `action` (ação sugerida: "Arquivar", "Vincular", "Associar", "Mesclar", "Revisar" — ver `OrphanEntityTable.tsx` no frontend).

### C. Ajuste no DTO de resposta dos endpoints já existentes (etapa 07)

`GET /graph/nodes` e `GET /graph/nodes/{nodeId}` passam a retornar o shape `KGNode` completo (`x`, `y`, `props`), não o shape mínimo da etapa 07 — atualizar os DTOs sem quebrar os endpoints já existentes (é uma adição de campos, não uma mudança incompatível).

### D. Preview leve de nó (referência inline — caso WikiDev)

> Adicionado depois da análise dos contratos de negócio dos primeiros produtos reais (Sprint 11 do frontend, `docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`). A WikiDev precisa de um popup leve ao passar o mouse sobre uma referência dentro do corpo de um artigo (ex.: a palavra "Spring Boot" linkada para o artigo sobre Spring Boot) — isso é diferente da tela de detalhe completa do nó, que é pesada para um tooltip.

```txt
GET /api/v1/products/{productId}/graph/nodes/{nodeId}/preview
```

```ts
type GraphNodePreview = {
  id: string; label: string; type: string; summary: string;
  difficulty?: "beginner" | "intermediate" | "advanced"; thumbnail?: string;
};
```

Implementação: não precisa de tabela nova — `summary`/`difficulty` vêm do `Content` associado ao nó (etapa 10, campos `summary`/`difficultyLevel`, quando o nó referenciar um `Content`) ou de `props`/`metadataJson` para nós que não são `Content` (ex.: um nó `TAG` ou `CATEGORY`). `thumbnail` é opcional e pode vir de um asset associado, se existir.

## Critérios de aceite

- [ ] Posição de um nó pode ser atualizada via `PATCH .../position` e persiste entre consultas.
- [ ] `GET /graph/nodes` retorna `x`/`y`/`props` corretamente.
- [ ] `GET /graph/orphans` retorna só nós sem nenhuma edge.
- [ ] Shape de `OrphanEntityTable` documentado no trace report.
- [ ] `GET .../preview` retorna o shape leve `GraphNodePreview`, populado a partir do `Content` associado quando existir.

## Validação

```bash
curl -X PATCH http://localhost:8080/api/v1/products/<productId>/graph/nodes/<nodeId>/position \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"x":120.5,"y":340.0}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/graph/orphans

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/graph/nodes/<nodeId>/preview
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): knowledge graph com layout persistente e endpoint de nos orfaos"
```
