# Etapa 18 — Knowledge Graph: layout persistente, props ricas e nós órfãos

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
POST  /api/v1/products/{productId}/graph/orphans/{nodeId}/resolve   body: { action: string }
POST  /api/v1/products/{productId}/graph/orphans/resolve            body: { ids: string[] }
POST  /api/v1/products/{productId}/graph/insights/review            body: { text: string }
```

`GET /graph/orphans` retorna `KGNode[]` — nós que não aparecem como `sourceNodeId` nem `targetNodeId` de nenhuma `GraphEdge` do produto. Motivo: `OrphanEntityTable.tsx`, hoje sem nenhum tipo formal no frontend — ao implementar este endpoint, também é necessário definir e documentar (atualizar `docs/trace/00_endpoints_esperados.md`) o shape de cada linha da tabela, incluindo o campo `action` (ação sugerida: "Arquivar", "Vincular", "Associar", "Mesclar", "Revisar" — ver `OrphanEntityTable.tsx` no frontend).

> **Endpoints de ação adicionados nesta revisão** (ver ADR-0016): o frontend mock (`knowledgeService.resolveOrphan`/`resolveOrphans`/`markInsightReviewed`) já chama estes três caminhos por convenção REST há tempo, mas nenhuma etapa os havia formalizado — `resolve` (um órfão ou em lote) aplica a `action` escolhida na `OrphanEntityTable` (arquivar/vincular/associar/mesclar/revisar — decisão de cada `action` é responsabilidade desta etapa, documentar o que cada uma faz de fato no backend); `insights/review` marca como revisado um "insight" textual de sugestão de conexão (feature de curadoria, não bloqueia nada se ainda não tiver lógica de sugestão real — pode só persistir `reviewed=true` para o texto/hash informado).

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

**Isolamento por produto e module-gating** (`00_padrao_qualidade_e_arquitetura.md`, Seções 9.2 e 10): os endpoints novos desta etapa (`position`, `orphans`, `preview`) ficam no mesmo `KnowledgeGraphController` da etapa 07 — já anotado com `@RequireModule(ModuleKey.KNOWLEDGE_GRAPH)` e já validando isolamento por produto; não precisa duplicar a anotação por método, só confirmar que os métodos novos estão na mesma classe.

### E. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Esta etapa **não cria entidade nova** — ajusta `GraphNode` (etapa 07) e o `GraphNodeRepository`/`GraphNodeMapper` já existentes (adicionar Javadoc nos métodos novos, se algum for adicionado ao repository). 100% de cobertura nas classes funcionais, incluindo a lógica de `orphans` (query de nós sem edge) e o fallback de `preview` (`Content` vs `props`/`metadataJson`).
- Entregar em rodadas:
  1. Ajuste de `GraphNode` (colunas `x`/`y`, decisão sobre `props`) + ajuste/novo método em `GraphNodeRepository` (query de órfãos) + testes `@DataJpaTest`.
  2. Ajuste de `GraphNodeMapper` (shape `KGNode` completo) + testes de mapper.
  3. Ajuste de `KnowledgeGraphService` (posição, órfãos, preview) + testes com mocks — caso `Content` associado e caso sem `Content` (fallback para `props`) ambos cobertos.
  4. Ajuste de `KnowledgeGraphController` (endpoints da Seção B/D) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] Posição de um nó pode ser atualizada via `PATCH .../position` e persiste entre consultas.
- [ ] `GET /graph/nodes` retorna `x`/`y`/`props` corretamente.
- [ ] `GET /graph/orphans` retorna só nós sem nenhuma edge.
- [ ] Shape de `OrphanEntityTable` documentado no trace report.
- [ ] `GET .../preview` retorna o shape leve `GraphNodePreview`, populado a partir do `Content` associado quando existir.
- [ ] `POST .../orphans/{id}/resolve` e `POST .../orphans/resolve` aplicam a `action` informada e o(s) nó(s) deixam de aparecer em `GET /graph/orphans` quando a ação resolve a orfandade (ex.: "Vincular" cria uma edge).
- [ ] `POST .../insights/review` marca o insight como revisado (idempotente — chamar duas vezes não duplica nem falha).
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -X PATCH http://localhost:8080/api/v1/products/<productId>/graph/nodes/<nodeId>/position \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"x":120.5,"y":340.0}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/graph/orphans

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/graph/nodes/<nodeId>/preview
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): knowledge graph com layout persistente e endpoint de nos orfaos"
```
