# BUG-SPRINT-05 — Qualidade Transversal, Conteúdo e Knowledge Graph

**Arquivo:** `BUG-SPRINT-05_qualidade_transversal_conteudo_kg.md`  
**Criado em:** 2026-07-08  
**Branch:** `bugfix/sprint-05-qualidade-conteudo-kg`  
**Status:** aguarda implementação

> **⚠️ CONSTRAINT INVIOLÁVEL**  
> Este arquivo **NÃO deve ser modificado** pelo agente implementador durante commits de implementação.  
> Serve exclusivamente como especificação de referência.

---

## Contexto

Rodada de bug-reporting pós BUG-SPRINT-CONSOLIDADO (03+04).  
Os bugs desta sprint cobrem 5 domínios: infraestrutura Bruno/auditoria, Forms, Páginas/Eventos, Conteúdo/Knowledge Graph e Assets/Mídias. Alguns problemas têm raiz comum (picker de assets com erro); o agente deve resolvê-los de dentro para fora, começando pela raiz.

**Bugs reportados nesta rodada:** 18  
**Bugs mapeados em seções:** G (8) · H (3) · I (3) · J (5) · K (2) · L (1) · M (1) · N (4) · O (1) = 28 itens  
*(bug 5 — hero image — corresponde a J.1; bug 18 — verificação — corresponde a L.1)*

---

## Seção G — Infraestrutura e Auditoria

### G.1 — Tenants fantasmas acumulam a cada deploy porque o teardown nunca executa de fato

> **🔴 PRIORIDADE MÁXIMA** — Este é o bug mais frequente do dia-a-dia: a cada rodada de CI/CD os tenants de teste ficam presos no banco e poluem a lista de Admin. A solução é cirúrgica (uma linha no CI) e não toca nenhum dado real.

**Tela:** Admin > lista de tenants  
**Manifestação:** após cada deploy, dezenas de tenants `TEST-*` (e variantes legadas como `Tenant <uuid>`) acumulam sem serem removidos. O operador precisa limpar manualmente a cada ciclo.

---

#### Causa raiz exata

**O teardown existe, funciona e já está correto — mas nunca é executado de verdade no CI.**

Arquivo: `.github/workflows/ci.yml`, job `bruno-tests`, step `Run Bruno collection`, linha 94:

```yaml
# ATUAL — QUEBRADO:
run: npx @usebruno/cli run --env local
```

O script de teardown (`99-teardown/02-limpar-tenants-de-teste.bru`) usa `fetch()` dentro do `post-response script` para disparar os `DELETE`s. O `fetch()` **só existe no sandbox `"developer"` do Bruno CLI**. O default é o sandbox `"safe"` (QuickJS) — que não tem `fetch`. O resultado é que o teardown roda mas não apaga nada, silenciosamente, sem erro aparente.

**Prova:** o `collection.bru` já define `testTenantPrefix = 'TEST-'` e todos os 10 scripts de criação de tenant na collection já usam `TEST-` — isso foi verificado. A lógica de proteção já está completa no teardown:
- `isTestTenant()`: apaga apenas names que começam com `TEST-` OU que correspondem a padrões legados fechados (`Tenant listener-*`, `Tenant <uuid>`, `Tenant E2E <timestamp>`)  
- `isProtectedTenant()`: cinto-e-suspensório — mesmo que um tenant `TEST-*` passe pelo filtro, se `name === "CLIENTES BETA"` ou `key === "clientes-beta"`, **nunca é apagado**  
- Idempotente: se não há candidatos, não faz nenhuma chamada DELETE

O teardown é seguro. O problema é que ele nunca roda com o sandbox certo.

---

#### Correção 1 — CI: adicionar `--sandbox developer` + steps de teardown dedicados

Arquivo: `.github/workflows/ci.yml` — substituir o job `bruno-tests` inteiro:

```yaml
bruno-tests:
  name: Bruno - local stack smoke
  runs-on: [self-hosted, genesis-lab]
  needs: docker-build
  steps:
    - uses: actions/checkout@v4

    - name: Start local stack
      run: docker compose up -d --build

    - name: Wait for backend health
      run: |
        for i in $(seq 1 60); do
          if curl -fsS http://localhost:8080/actuator/health; then
            exit 0
          fi
          sleep 5
        done
        docker compose logs aegis-backend
        exit 1

    # PRÉ-LIMPEZA: apagar resíduos de runs anteriores que falharam antes do teardown
    - name: Pre-test teardown (limpar resíduos anteriores)
      working-directory: bruno
      run: npx @usebruno/cli run 99-teardown --env local -r --sandbox developer
      continue-on-error: true  # não bloquear se já está limpo

    # SUITE PRINCIPAL: usa --sandbox developer para que o teardown embutido funcione
    - name: Run Bruno collection
      working-directory: bruno
      run: npx @usebruno/cli run --env local --sandbox developer

    # PÓS-LIMPEZA: garantir limpeza mesmo que a suite falhe (always())
    - name: Post-test teardown (garantir limpeza após suite)
      if: always()
      working-directory: bruno
      run: npx @usebruno/cli run 99-teardown --env local -r --sandbox developer

    - name: Stop local stack
      if: always()
      run: docker compose down
```

**Por que três momentos de teardown?**  
- **Pré-suite**: apaga resíduos de runs de CI que falharam _antes_ de chegar no `99-teardown` (ex.: timeout, crash da stack)  
- **Embutido na suite**: o `99-teardown` que já existe dentro do `bru run --env local` agora funciona porque tem `--sandbox developer`  
- **Pós-suite com `if: always()`**: garante limpeza mesmo que a suite falhe no meio — sem este step, uma falha em `16-audit` deixaria os tenants criados até aquele ponto

---

#### Correção 2 — Cleanup pontual de produção (tenants legados pré-existentes)

Para limpar os tenants fantasmas que já existem no ambiente (antes desta correção entrar no CI), o operador deve rodar **uma única vez**:

```bash
# Na máquina com acesso ao ambiente de produção/staging:
cd /caminho/para/aegis-pms/bruno
npx @usebruno/cli run 99-teardown --env prod -r --sandbox developer
```

**O que será apagado:** somente tenants com `name` começando em `TEST-` + variantes legadas (`Tenant <uuid>`, `Tenant listener-*`, `Tenant E2E <timestamp>`)  
**O que NUNCA será apagado:** `CLIENTES BETA`, qualquer tenant sem o marcador `TEST-` e sem padrão legado  
**Confirmação:** o health-check final (`04-health-check-final.bru`) asserta que `CLIENTES BETA` ainda existe após o teardown

> ⚠️ Rodar no env `prod` aponta para o `baseUrl` do `environments/prod.bru`. Conferir que `baseUrl` e `token` estão corretos antes de executar.

---

#### Correção 3 — Proteger o ambiente `prod.bru` de ser usado em testes acidentais

Arquivo: `bruno/environments/prod.bru` — adicionar variável de guarda:

```
vars {
  baseUrl: https://aegis.byop.dev
  tenantName: TEST-BYOP
  _envGuard: production
}
```

Em `collection.bru`, adicionar checagem ao início do `script:pre-request`:
```javascript
// Bloquear criação de tenant sem prefixo TEST- em qualquer ambiente
const prefix = bru.getVar('testTenantPrefix') || 'TEST-';
const tName = bru.getVar('tenantName') || '';
if (tName && !tName.startsWith(prefix)) {
  throw new Error(`[GUARD] tenantName "${tName}" não tem o prefixo "${prefix}". Corrija antes de rodar.`);
}
```

---

#### Garantias de proteção contra tenants reais

| Tenant | Protegido por |
|---|---|
| `CLIENTES BETA` | `isProtectedTenant()` verifica `name` E `key` — dupla verificação |
| Qualquer tenant sem `TEST-` no nome | `isTestTenant()` só marca como candidato quem tem o prefixo ou padrão legado — nome genérico nunca entra |
| Tenant criado pela plataforma (usuário real) | Por definição não terá `TEST-` no nome — nunca elegível |
| Padrões legados (`Tenant <uuid>`) | Já não são criados pela collection atual; cleanup pontual os remove; os novos nunca mais aparecem |

**O teardown é 100% opt-in por origem**: a decisão de apagar foi tomada _na criação_ do tenant (ao colocar `TEST-` no nome), não no momento da exclusão. Nunca haverá risco de apagar um tenant real criado pela plataforma.

---

### G.4 — Graph Canvas: nós com mocks, sem drag, sem zoom e arestas não carregam da API

> **🔴 INTERATIVIDADE CRÍTICA** — O canvas é a principal tela do módulo Knowledge Graph, mas está estático: não é possível arrastar vértices, não tem zoom, e os dados exibidos são fixtures de seed em vez de conteúdo real publicado.

**Tela:** Knowledge Graph > Graph Canvas (screenshot da sessão)  
**Manifestações:**
1. Nós exibidos ("Vigilia", "Clair de Lune - Debussy", "Manifesto do Silêncio", etc.) são seed fixtures — não são conteúdos criados pelo usuário
2. Clicar e arrastar um nó não faz nada — os vértices estão fixos
3. Não existe controle de zoom (aumentar/diminuir)
4. Em API mode, as arestas nunca são carregadas do backend (fallback para mock silencioso)
5. Contadores no painel direito mostram zero para todos os tipos de entidade quando em API mode

---

#### Diagnóstico detalhado

**G.4.1 — Dados mockados**

`knowledgeService.ts` — `listNodes` em API mode chama corretamente `GET /products/{productId}/graph/nodes`. Mas `listEdges` em API mode **não tem endpoint**:

```typescript
async listEdges(productId: string): Promise<ListEdgesResponse> {
  if (IS_API_MODE) warnMissingEndpoint("GET", `.../graph/edges`); // ← só loga no console
  return productEdges(productId); // ← retorna seed data SEMPRE, inclusive em API mode
},
```

Em mock mode, `listNodes` retorna `lokiKgNodes` — fixture com "Vigilia", "Clair de Lune", etc. Os contadores de tipo (Tenant: 0, Produto: 0...) mostram zero porque em API mode os nós vêm do backend mas os tipos não batem com as chaves hardcoded em `kgColor`.

**G.4.2 — Sem drag-and-drop**

`GraphCanvasView.tsx` renderiza cada nó como `<button>` com `style={{ left: n.x, top: n.y }}`. Não há `onMouseDown`, `onMouseMove` nem `onMouseUp`. Os valores `x`/`y` vêm do mock (`knowledge.mocks.ts`) — nós da API não têm coordenadas retornadas.

**Importante:** o backend **já expõe** `PATCH /api/v1/products/{productId}/graph/nodes/{nodeId}/position` (`UpdateGraphNodePositionRequest`) para persistir a posição após arrastar. O frontend nunca chama esse endpoint.

**G.4.3 — Sem zoom**

O canvas é um `<div style={{ width: 864, height: 570 }}>` fixo. Não há `transform: scale(zoom)` nem controles +/−.

**G.4.4 — Nós da API sem posição inicial**

`KGNode.x`/`KGNode.y` são campos específicos dos mocks. O backend (`GraphNodeSummary`) não retorna coordenadas — ao montar com dados reais da API, todos os nós renderizariam em `left: 0, top: 0` sobrepostos (ou `undefined`, quebrando o layout).

---

#### Correções

**G.4.1 — Backend: adicionar `GET /graph/edges`**

Em `KnowledgeGraphController.java`, adicionar endpoint:

```java
@GetMapping("/api/v1/products/{productId}/graph/edges")
public List<GraphEdgeSummary> listEdges(@PathVariable("productId") UUID productId,
                                         Authentication authentication) {
    assertProductAccess(authentication, productId);
    return knowledgeGraphService.listEdges(productId);
}
```

Em `KnowledgeGraphService.java`:
```java
public List<GraphEdgeSummary> listEdges(UUID productId) {
    return graphEdgeRepository.findAllByProductId(productId).stream()
        .map(graphEdgeMapper::toSummary)
        .toList();
}
```

`GraphEdgeSummary.java` (DTO):
```java
public record GraphEdgeSummary(UUID id, UUID from, UUID to, String verb, String edgeType, Integer weight) {}
```

JaCoCo 100% obrigatório no service method.

**G.4.2 — Frontend: conectar `listEdges` à API**

`knowledgeService.ts`:
```typescript
async listEdges(productId: string): Promise<ListEdgesResponse> {
  if (IS_API_MODE) {
    const edges = await apiClient.get<GraphEdgeSummary[]>(`/products/${productId}/graph/edges`);
    // Mapear para o shape KGEdge do frontend
    return edges.map((e) => ({
      from: e.from,
      to: e.to,
      verb: e.verb ?? e.edgeType ?? "RELATED_TO",
      weight: e.weight ?? 0,
    }));
  }
  return productEdges(productId);
},
```

Adicionar `GraphEdgeSummary` ao `contracts/responses.ts`:
```typescript
export type GraphEdgeSummary = {
  id: string; from: string; to: string;
  verb: string; edgeType: string; weight: number;
};
```

**G.4.3 — Auto-layout força-dirigida para nós sem posição**

**Requisito:** o grafo deve distribuir vértices de forma visual e fácil de gerenciar, evitando ao máximo sobreposição entre nós. Arestas devem evidenciar as conexões sem que nós de origem e destino coexistam no mesmo ponto.

Criar `frontend/src/domains/knowledge/utils/kgLayout.ts` com simulação de força (repulsão entre nós + atração por aresta) em vez de grade simples:

```typescript
import type { KGNode } from "../mocks/knowledge.mocks";

interface EdgeRef { from: string; to: string; }

const NODE_W        = 140;
const NODE_H        = 60;
const REPULSION     = 18_000;  // força de afastamento entre quaisquer dois nós
const ATTRACTION    = 0.04;    // força de aproximação por aresta (spring)
const REST_LEN      = 220;     // comprimento ideal de aresta em px
const ITERATIONS    = 180;     // iterações da simulação (aumentar se grafo > 60 nós)
const PADDING       = 40;      // margem mínima com a borda do canvas
const CANVAS_W      = 1600;
const CANVAS_H      = 1000;

export function forceLayout(nodes: KGNode[], edges: EdgeRef[]): KGNode[] {
  if (nodes.length === 0) return nodes;

  // Inicializar em posições aleatórias se ainda não tiverem (ou tiverem x=0,y=0)
  const pos: Record<string, { x: number; y: number }> = {};
  nodes.forEach((n, i) => {
    pos[n.id] = (n.x > 0 || n.y > 0)
      ? { x: n.x, y: n.y }
      : {
          x: PADDING + Math.random() * (CANVAS_W - PADDING * 2),
          y: PADDING + Math.random() * (CANVAS_H - PADDING * 2),
        };
  });

  for (let iter = 0; iter < ITERATIONS; iter++) {
    const force: Record<string, { fx: number; fy: number }> = {};
    nodes.forEach((n) => { force[n.id] = { fx: 0, fy: 0 }; });

    // Repulsão entre pares
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const a = nodes[i].id, b = nodes[j].id;
        const dx = pos[b].x - pos[a].x;
        const dy = pos[b].y - pos[a].y;
        const dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        const f = REPULSION / (dist * dist);
        force[a].fx -= (f * dx) / dist;
        force[a].fy -= (f * dy) / dist;
        force[b].fx += (f * dx) / dist;
        force[b].fy += (f * dy) / dist;
      }
    }

    // Atração pelas arestas (spring)
    for (const e of edges) {
      if (!pos[e.from] || !pos[e.to]) continue;
      const dx = pos[e.to].x - pos[e.from].x;
      const dy = pos[e.to].y - pos[e.from].y;
      const dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
      const f = ATTRACTION * (dist - REST_LEN);
      const fx = (f * dx) / dist;
      const fy = (f * dy) / dist;
      force[e.from].fx += fx;
      force[e.from].fy += fy;
      force[e.to].fx   -= fx;
      force[e.to].fy   -= fy;
    }

    // Aplicar forças com resfriamento progressivo
    const cool = 1 - iter / ITERATIONS;
    nodes.forEach((n) => {
      pos[n.id].x = Math.min(CANVAS_W - NODE_W - PADDING,
                    Math.max(PADDING, pos[n.id].x + force[n.id].fx * cool));
      pos[n.id].y = Math.min(CANVAS_H - NODE_H - PADDING,
                    Math.max(PADDING, pos[n.id].y + force[n.id].fy * cool));
    });
  }

  return nodes.map((n) => ({ ...n, x: Math.round(pos[n.id].x), y: Math.round(pos[n.id].y) }));
}

/** Compatibilidade retroativa — chama forceLayout sem arestas (grade forçada por repulsão) */
export function assignInitialPositions(nodes: KGNode[], edges: EdgeRef[] = []): KGNode[] {
  return forceLayout(nodes, edges);
}
```

Atualizar chamada em `GraphCanvasView.tsx` para passar as arestas:
```tsx
const layoutNodes = useMemo(
  () => forceLayout(kgNodes ?? [], edges ?? []),
  [kgNodes, edges]
);
```

Adicionar botão "Reorganizar" no canvas (ao lado dos controles de zoom) para re-executar o layout a qualquer momento:
```tsx
<button
  onClick={() => {
    const relaid = forceLayout(kgNodes ?? [], edges ?? []);
    setPositions(Object.fromEntries(relaid.map((n) => [n.id, { x: n.x, y: n.y }])));
    relaid.forEach((n) => knowledgeService.updateNodePosition(effectiveProduct.id, n.id, n.x, n.y));
  }}
  className="flex h-7 w-7 items-center justify-center rounded-lg border border-border bg-card text-[9px] shadow-sm hover:bg-muted"
  title="Reorganizar grafo"
>⟳</button>
```

**Nota de performance:** `ITERATIONS = 180` é síncrono e imperceptível para grafos até ~60 nós. Para grafos maiores, envolver em `setTimeout(..., 0)` ou `requestIdleCallback`. Não usar `d3-force` para não adicionar dependência — a simulação acima é suficiente para o escopo atual.

**G.4.4 — Drag-and-drop com persistência**

Adicionar estado de posição local e handlers de mouse em `GraphCanvasView.tsx`:

```tsx
// Estado de posições — inicializado com layoutNodes, atualizado pelo drag
const [positions, setPositions] = useState<Record<string, { x: number; y: number }>>({});
const dragging = useRef<{ nodeId: string; startX: number; startY: number; origX: number; origY: number } | null>(null);

// Inicializar positions quando layoutNodes muda
useEffect(() => {
  if (!layoutNodes) return;
  setPositions(Object.fromEntries(layoutNodes.map((n) => [n.id, { x: n.x, y: n.y }])));
}, [layoutNodes]);

const handleMouseDown = (e: React.MouseEvent, nodeId: string) => {
  e.preventDefault();
  const pos = positions[nodeId] ?? { x: 0, y: 0 };
  dragging.current = { nodeId, startX: e.clientX, startY: e.clientY, origX: pos.x, origY: pos.y };
};

const handleMouseMove = (e: React.MouseEvent) => {
  if (!dragging.current) return;
  const { nodeId, startX, startY, origX, origY } = dragging.current;
  const newX = Math.max(0, origX + (e.clientX - startX) / zoom);
  const newY = Math.max(0, origY + (e.clientY - startY) / zoom);
  setPositions((prev) => ({ ...prev, [nodeId]: { x: newX, y: newY } }));
};

const handleMouseUp = async () => {
  if (!dragging.current) return;
  const { nodeId } = dragging.current;
  dragging.current = null;
  // Persistir posição via PATCH — endpoint já existe no backend
  const pos = positions[nodeId];
  if (pos && IS_API_MODE) {
    try {
      await knowledgeService.updateNodePosition(productId, nodeId, pos.x, pos.y);
    } catch {
      // posição local mantida mesmo se a persistência falhar
    }
  }
};
```

Adicionar `updateNodePosition` ao `knowledgeService.ts`:
```typescript
async updateNodePosition(productId: string, nodeId: string, x: number, y: number): Promise<void> {
  if (IS_API_MODE) {
    await apiClient.patch(`/products/${productId}/graph/nodes/${nodeId}/position`, { x: Math.round(x), y: Math.round(y) });
    return;
  }
  const node = allNodes.find((n) => n.id === nodeId);
  if (node) { node.x = Math.round(x); node.y = Math.round(y); }
},
```

No JSX, envolver o `<div className="relative">` do canvas com os handlers:
```tsx
<div
  className="relative overflow-hidden"
  style={{ width: canvasW, height: canvasH, cursor: dragging.current ? "grabbing" : "default" }}
  onMouseMove={handleMouseMove}
  onMouseUp={handleMouseUp}
  onMouseLeave={handleMouseUp}
>
```

Cada nó: adicionar `onMouseDown` + mudar cursor:
```tsx
<button
  key={n.id}
  onMouseDown={(e) => handleMouseDown(e, n.id)}
  onClick={() => { if (!wasDragged) setSel(isSel ? null : n); }}
  style={{ left: positions[n.id]?.x ?? n.x, top: positions[n.id]?.y ?? n.y, cursor: "grab", ... }}
>
```
*(usar ref para detectar se o mouseup foi após drag significativo — `Math.abs(dx) > 4 || Math.abs(dy) > 4` — e evitar toggle de seleção ao soltar um nó arrastado)*

**G.4.5 — Zoom com transform-origin**

Adicionar estado de zoom e controles:
```tsx
const [zoom, setZoom] = useState(1);
const MIN_ZOOM = 0.4;
const MAX_ZOOM = 2;
const ZOOM_STEP = 0.15;
```

No container do canvas:
```tsx
<div style={{ transform: `scale(${zoom})`, transformOrigin: "top left" }}>
  {/* ... canvas content ... */}
</div>
```

Botões de controle (posição absoluta no canto superior direito do canvas):
```tsx
<div className="absolute right-2 top-2 z-10 flex flex-col gap-1">
  <button
    onClick={() => setZoom((z) => Math.min(MAX_ZOOM, +(z + ZOOM_STEP).toFixed(2)))}
    className="flex h-7 w-7 items-center justify-center rounded-lg border border-border bg-card text-sm shadow-sm hover:bg-muted"
  >+</button>
  <span className="text-center text-[10px] text-muted-foreground">{Math.round(zoom * 100)}%</span>
  <button
    onClick={() => setZoom((z) => Math.max(MIN_ZOOM, +(z - ZOOM_STEP).toFixed(2)))}
    className="flex h-7 w-7 items-center justify-center rounded-lg border border-border bg-card text-sm shadow-sm hover:bg-muted"
  >−</button>
  <button
    onClick={() => setZoom(1)}
    title="Resetar zoom"
    className="flex h-7 w-7 items-center justify-center rounded-lg border border-border bg-card text-[9px] shadow-sm hover:bg-muted"
  >1:1</button>
</div>
```

Suporte a scroll do mouse (wheel):
```tsx
<div
  onWheel={(e) => {
    e.preventDefault();
    setZoom((z) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(z - e.deltaY * 0.001).toFixed(2))));
  }}
>
```

**G.4.6 — Mock mode: substituir seeds por estado vazio com instrução**

Em mock mode, quando `kgNodes` de seed existe mas o produto ativo é Loki (ou qualquer produto real), não exibir os fixtures — exibir `EmptyState` com orientação:

```tsx
// GraphCanvasView.tsx — após carregar os dados
const isEmptyGraph = !kgNodes || kgNodes.length === 0;
// Em mock mode, considerar "vazio" se só tiver seeds sem conteúdo real de sessão
const hasOnlySeedNodes = !IS_API_MODE && kgNodes?.every((n) => SEED_NODE_IDS.has(n.id));

if (isEmptyGraph || hasOnlySeedNodes) {
  return (
    <EmptyState
      title="Grafo vazio"
      description="Nenhuma entidade foi adicionada a este produto ainda. Crie conteúdo e referencie entidades com {{kg-ref:id:Label}} para que apareçam aqui."
    />
  );
}
```

`SEED_NODE_IDS` = `new Set(["tenant", "produto", "pg-home", "asset", "form", "seo", "autor", "cat", "tag", "sub", "lead", ...lokiKgNodes.map(n => n.id), ...wikidevKgNodes.map(n => n.id)])` — qualquer ID não presente neste set é conteúdo criado na sessão.

---

#### Resumo de endpoints e arquivos impactados

| Arquivo | Mudança |
|---|---|
| `KnowledgeGraphController.java` | + `GET /graph/edges` |
| `KnowledgeGraphService.java` | + `listEdges()` + JaCoCo 100% |
| `GraphEdgeSummary.java` | novo DTO |
| `knowledgeService.ts` | `listEdges` → API real; + `updateNodePosition` |
| `GraphCanvasView.tsx` | drag state, zoom state, auto-layout, controles |
| `kgLayout.ts` | novo utilitário `assignInitialPositions` |
| `contracts/responses.ts` | + `GraphEdgeSummary` type |

---

### G.5 — RelationshipExplorer: relações criadas entre posts não aparecem

**Tela:** Knowledge Graph > Relações (tabela vazia)  
**Manifestação:** o usuário criou uma relação entre dois posts via `{{kg-ref:...}}` no corpo do conteúdo. A aresta foi criada (backend confirmou 201). Ao abrir a aba Relações, a tabela está vazia.

**Diagnóstico:**

`RelationshipExplorer.tsx` chama `knowledgeService.listEdges(productId)`. Em API mode, esse método não chama a API — loga um warning e retorna edges do mock local:

```typescript
async listEdges(productId: string): Promise<ListEdgesResponse> {
  if (IS_API_MODE) warnMissingEndpoint("GET", `.../graph/edges`); // ← só log
  return productEdges(productId); // ← retorna seed data SEMPRE, inclusive em API mode
}
```

Os edges do mock têm IDs como `"tenant"`, `"produto"`, `"pg-home"` — que não existem nos nós retornados pela API (UUIDs reais). Por isso, a linha `if (!fn || !tn) return null` filtra 100% das linhas e a tabela fica vazia.

**Correção:** idêntica a G.4.1 e G.4.2. Este bug é corrigido automaticamente ao implementar G.4. Após a implementação, verificar que a aresta criada entre dois posts aparece na tabela com `Origem`, `Relação` e `Destino` corretos.

---

### G.6 — OrphanEntityTable: entidades órfãs são 100% hardcoded

**Tela:** Knowledge Graph > Órfãos ("Imagem antiga.jpg", "Form RSVP 2023", etc.)  
**Manifestação:** lista sempre exibe os mesmos 5 itens independente do produto. Clicar em "Arquivar" / "Vincular" dispara chamadas com IDs falsos (`"or1"`, `"or2"`) → 404 no backend.

**Diagnóstico:**

`OrphanEntityTable.tsx` — linha 12–18:
```tsx
const ROWS: OrphanRow[] = [
  { id: "or1", label: "Imagem antiga.jpg", type: "Asset", ... },
  { id: "or2", label: "Form RSVP 2023", type: "Formulário", ... },
  // ...
];
```
Nenhuma chamada ao backend. `knowledgeService.resolveOrphan(productId, "or1", ...)` em API mode chama `POST /products/{productId}/graph/orphans/or1/resolve` — o backend rejeita com 404 porque não existe nó com ID `"or1"`.

O backend já possui `GET /api/v1/products/{productId}/graph/orphans` que retorna nós sem arestas ativas.

**Correções:**

**G.6.1 — Adicionar `listOrphans` ao `knowledgeService.ts`**
```typescript
async listOrphans(productId: string): Promise<KGNode[]> {
  if (IS_API_MODE) return apiClient.get<KGNode[]>(`/products/${productId}/graph/orphans`);
  const nodes = productNodes(productId);
  const edges = productEdges(productId);
  const connectedIds = new Set(edges.flatMap((e) => [e.from, e.to]));
  return nodes.filter((n) => !connectedIds.has(n.id));
},
```

**G.6.2 — `OrphanEntityTable`: substituir `ROWS` por dados reais**
```tsx
export function OrphanEntityTable() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: orphans, loading, error } = useAsyncData(
    () => productId ? knowledgeService.listOrphans(productId) : Promise.resolve([]),
    [productId]
  );
  const [resolved, setResolved] = useState<Set<string>>(new Set());
  const [resolvingRow, setResolvingRow] = useState<string | null>(null);
  const [resolvingAll, setResolvingAll] = useState(false);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  const rows = (orphans ?? []).filter((r) => !resolved.has(r.id));

  const handleResolveRow = async (orphan: KGNode) => {
    setResolvingRow(orphan.id);
    try {
      await knowledgeService.resolveOrphan(productId, orphan.id, "archive");
      setResolved((prev) => new Set(prev).add(orphan.id));
      toast.success("Nó arquivado.", { description: orphan.label });
    } catch {
      toast.error("Não foi possível resolver.", { description: orphan.label });
    } finally {
      setResolvingRow(null);
    }
  };
  // ...
}
```

**G.6.3 — Empty state quando grafo não tem órfãos**
```tsx
{rows.length === 0 && <EmptyState title="Nenhum nó órfão" description="Todas as entidades têm pelo menos uma relação ativa." />}
```

**G.6.4 — Ação sugerida contextual por tipo**
```typescript
function suggestAction(type: string): string {
  const map: Record<string, string> = { Asset: "Arquivar", Formulário: "Vincular", SEO: "Associar", Categoria: "Mesclar" };
  return map[type] ?? "Revisar";
}
```

---

### G.7 — KnowledgeInsights: insights hardcoded + navegação para entidade fixa

**Tela:** Knowledge Graph > Insights (6 cards fixos, "Página Home possui 12 dependências", etc.)  
**Manifestação:** os insights são sempre os mesmos 6 textos pré-escritos no código, não refletem o estado real do grafo. "Abrir entidade" navega sempre para `/knowledge/entities/pg-home` (ID hardcoded).

**Diagnóstico:**

`KnowledgeInsights.tsx` — linha 27–34:
```tsx
const insights: [string, string][] = [
  ["5 assets não estão sendo utilizados.", "média"],
  ["Página Home possui 12 dependências.", "alta"],
  // ... 4 itens hardcoded
];
```
`navigate("/knowledge/entities/pg-home")` — hardcoded em `KnowledgeInsightCard`. O backend **não tem** `GET /graph/insights` — só `POST /graph/insights/review` para marcar revisado.

**Correções:**

**G.7.1 — Backend: `GET /graph/insights` computado**

`KnowledgeGraphController.java`:
```java
@GetMapping("/api/v1/products/{productId}/graph/insights")
public List<GraphInsightSummary> listInsights(@PathVariable("productId") UUID productId,
                                               Authentication authentication) {
    assertProductAccess(authentication, productId);
    return knowledgeGraphService.listInsights(productId);
}
```

`KnowledgeGraphService.java` — `listInsights` deriva insights do estado real do grafo:
```java
public List<GraphInsightSummary> listInsights(UUID productId) {
    List<GraphInsightSummary> insights = new ArrayList<>();
    long orphanCount = graphNodeRepository.countOrphansByProductId(productId);
    if (orphanCount > 0)
        insights.add(build(orphanCount + " entidade(s) sem relações ativas.", "media", null));
    long unlinkedAssets = graphNodeRepository.countUnlinkedAssetsByProductId(productId);
    if (unlinkedAssets > 0)
        insights.add(build(unlinkedAssets + " asset(s) não utilizados.", "media", null));
    // ... outros insights derivados de queries reais
    return insights.stream().map((i) -> withReviewedStatus(productId, i)).toList();
}

private GraphInsightSummary withReviewedStatus(UUID productId, GraphInsightSummary i) {
    String hash = DigestUtils.sha256Hex(i.text());
    boolean reviewed = insightReviewRepository
        .findByProductIdAndTextHash(productId, hash)
        .map(GraphInsightReview::isReviewed).orElse(false);
    return new GraphInsightSummary(hash, i.text(), i.severity(), reviewed, i.relatedEntityId());
}
```

`GraphInsightSummary.java`:
```java
public record GraphInsightSummary(String id, String text, String severity, boolean reviewed, UUID relatedEntityId) {}
```

JaCoCo 100% em `listInsights`.

**G.7.2 — `knowledgeService.ts` — adicionar `listInsights`**
```typescript
async listInsights(productId: string): Promise<GraphInsight[]> {
  if (IS_API_MODE) return apiClient.get<GraphInsight[]>(`/products/${productId}/graph/insights`);
  // Mock: derivar do estado local
  const nodes = productNodes(productId);
  const edges = productEdges(productId);
  const connectedIds = new Set(edges.flatMap((e) => [e.from, e.to]));
  const orphanCount = nodes.filter((n) => !connectedIds.has(n.id)).length;
  if (orphanCount > 0)
    return [{ id: "mock-orphans", text: `${orphanCount} entidade(s) sem relações.`, severity: "media", reviewed: false, relatedEntityId: null }];
  return [];
},
```

`contracts/responses.ts`:
```typescript
export type GraphInsight = {
  id: string; text: string; severity: "alta" | "media" | "baixa"; reviewed: boolean; relatedEntityId: string | null;
};
```

**G.7.3 — `KnowledgeInsights.tsx` — dados reais + navegação dinâmica**
```tsx
export function KnowledgeInsights() {
  const { product } = useCurrentProduct();
  const productId = product?.id ?? "";
  const { data: insights, loading, error } = useAsyncData(
    () => productId ? knowledgeService.listInsights(productId) : Promise.resolve([]),
    [productId]
  );
  const [reviewed, setReviewed] = useState<Set<string>>(new Set());

  if (loading) return <SkeletonLines />;
  if (error) return <PartialErrorWidget />;

  return (
    <>
      <PageHeader title="Knowledge Insights" ... />
      {(insights ?? []).length === 0
        ? <EmptyState title="Grafo saudável" description="Nenhuma lacuna ou inconsistência detectada." />
        : <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {(insights ?? []).map((i) => (
              <KnowledgeInsightCard
                key={i.id}
                insight={i}
                reviewed={reviewed.has(i.id) || i.reviewed}
                onReview={async () => {
                  try {
                    await knowledgeService.markInsightReviewed(productId, i.text);
                    setReviewed((prev) => new Set(prev).add(i.id));
                    toast.success("Insight marcado como revisado.");
                  } catch {
                    toast.error("Não foi possível marcar como revisado.");
                  }
                }}
              />
            ))}
          </div>
      }
    </>
  );
}
```

**G.7.4 — Navegação dinâmica em `KnowledgeInsightCard`**
```tsx
// De: navigate("/knowledge/entities/pg-home")
// Para:
{insight.relatedEntityId && (
  <Button onClick={() => navigate(`/knowledge/entities/${insight.relatedEntityId}`)}>
    Abrir entidade
  </Button>
)}
```

---

### G.8 — KnowledgeOverview: KPIs e timeline hardcoded + vazamento de dados entre produtos

> **🔴 ISOLAMENTO CRÍTICO** — A tela Overview exibe números e eventos de um produto seed (Maestro Beton) enquanto o usuário está em outro produto (Loki). Isso é um vazamento de contexto entre produtos — o usuário vê dados que não pertencem ao seu contexto atual.

**Tela:** Knowledge Graph > Overview (screenshot — "184", "426", "hero-maestro.jpg vinculado à Página Home", etc. aparecem enquanto o produto ativo é Loki)  
**Manifestações:**
1. KPIs (`184`, `426`, `18`, `5`, `3`, `27`, `9`) são strings hardcoded — não refletem o produto ativo
2. Timeline de relações mostra eventos de "Maestro Beton" (`hero-maestro.jpg`, `Formulário Orçamento`, `Lead Camila`) enquanto o usuário está em "Loki"
3. `PartialErrorWidget` é renderizado incondicionalmente na linha 53 — sempre aparece o aviso de erro parcial, mesmo sem erro real
4. `EmptyState`, `SkeletonLines` e `PermissionHint` são renderizados simultaneamente no card "Estados do módulo" — layout quebrado (três estados ao mesmo tempo)

**Diagnóstico:**

`KnowledgeOverview.tsx` — completamente estático:
```tsx
// KPIs — 8 valores hardcoded:
<KPIWidget label="Entidades totais" value="184" ... />
<KPIWidget label="Relações totais" value="426" ... />
// ...

// Timeline — 4 eventos hardcoded de Maestro Beton:
{["hero-maestro.jpg vinculado à Página Home", "Formulário Orçamento conectado à Página Home", ...]}

// PartialErrorWidget SEMPRE renderizado:
<Card><h2>Timeline de relações</h2><KnowledgeTimeline /><PartialErrorWidget /></Card>
//                                                        ↑ incondicional!

// Estados do módulo — 3 estados ao mesmo tempo:
<EmptyState ... />   // ← aparece
<SkeletonLines />    // ← aparece
<PermissionHint />   // ← aparece
```

O backend tem `GET /products/{productId}/graph/nodes` e `GET /products/{productId}/graph/edges` (após G.4). Para o KPI de "Entidades órfãs", usar o `listOrphans` de G.6. Para os demais KPIs computados, adicionar `GET /products/{productId}/graph/stats`.

**Correções:**

**G.8.1 — Backend: `GET /graph/stats`**

`KnowledgeGraphController.java`:
```java
@GetMapping("/api/v1/products/{productId}/graph/stats")
public GraphStatsSummary getStats(@PathVariable("productId") UUID productId,
                                   Authentication authentication) {
    assertProductAccess(authentication, productId);
    return knowledgeGraphService.getStats(productId);
}
```

`GraphStatsSummary.java`:
```java
public record GraphStatsSummary(
    long totalNodes, long totalEdges, long orphanNodes,
    long unlinkedContents, long unusedAssets, long disconnectedForms,
    long recentEdges, long recentChanges
) {}
```

`KnowledgeGraphService.java` — `getStats` executa queries diretas (sem N+1):
```java
public GraphStatsSummary getStats(UUID productId) {
    return new GraphStatsSummary(
        graphNodeRepository.countByProductId(productId),
        graphEdgeRepository.countByProductId(productId),
        graphNodeRepository.countOrphansByProductId(productId),
        // ... etc.
    );
}
```

JaCoCo 100%.

**G.8.2 — Timeline: carregar eventos reais do grafo**

Usar `GET /products/{productId}/graph/edges?recent=true&limit=10` (adicionar parâmetro `recent` ao endpoint G.4.1) ou uma consulta separada. A timeline deve mostrar edges recentes com `createdAt`, `from` e `to` resolvidos para labels.

**G.8.3 — `KnowledgeOverview.tsx` — substituir hardcoded por dados reais**
```tsx
export function KnowledgeOverview() {
  const navigate = useNavigate();
  const { effectiveProduct } = useAuth();
  const productId = effectiveProduct?.id ?? "";

  const { data: stats, loading: loadingStats, error: errorStats } = useAsyncData(
    () => productId ? knowledgeService.getStats(productId) : Promise.resolve(undefined),
    [productId]
  );

  const { data: recentEdges, loading: loadingTimeline, error: errorTimeline } = useAsyncData(
    () => productId ? knowledgeService.listRecentEdges(productId, 5) : Promise.resolve([]),
    [productId]
  );

  const kpiLoading = loadingStats;
  const dash = (v: number | undefined) => v !== undefined ? String(v) : "—";

  return (
    <>
      <PageHeader title="Knowledge Graph" badge={effectiveProduct?.name} ... />
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {kpiLoading
              ? Array(8).fill(0).map((_, i) => <SkeletonLines key={i} />)
              : <>
                  <KPIWidget label="Entidades totais" value={dash(stats?.totalNodes)} detail="nós no grafo" />
                  <KPIWidget label="Relações totais" value={dash(stats?.totalEdges)} detail="arestas ativas" />
                  <KPIWidget label="Entidades órfãs" value={dash(stats?.orphanNodes)} error={(stats?.orphanNodes ?? 0) > 0} detail="sem relações ativas" />
                  <KPIWidget label="Conteúdos sem vínculo" value={dash(stats?.unlinkedContents)} detail="revisar navegação" />
                  <KPIWidget label="Assets sem uso" value={dash(stats?.unusedAssets)} detail="impacto em biblioteca" />
                  <KPIWidget label="Formulários desconectados" value={dash(stats?.disconnectedForms)} error={(stats?.disconnectedForms ?? 0) > 0} detail="sem conteúdo origem" />
                  <KPIWidget label="Relações recentes" value={dash(stats?.recentEdges)} detail="últimas 48h" />
                  <KPIWidget label="Mudanças recentes" value={dash(stats?.recentChanges)} detail="auditáveis" />
                </>
            }
          </div>
        </div>
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Timeline de relações</h2>
          {loadingTimeline
            ? <SkeletonLines />
            : errorTimeline
            ? <PartialErrorWidget />  // ← condicional, não incondicional
            : recentEdges && recentEdges.length > 0
            ? <KnowledgeTimelineReal edges={recentEdges} productName={effectiveProduct?.name} />
            : <EmptyState compact title="Sem relações recentes" description="As relações criadas aparecerão aqui." />
          }
        </Card>
      </div>
    </>
  );
}
```

**G.8.4 — `knowledgeService.ts` — adicionar `getStats` e `listRecentEdges`**
```typescript
async getStats(productId: string): Promise<KGStats> {
  if (IS_API_MODE) return apiClient.get<KGStats>(`/products/${productId}/graph/stats`);
  const nodes = productNodes(productId);
  const edges = productEdges(productId);
  const connectedIds = new Set(edges.flatMap((e) => [e.from, e.to]));
  return {
    totalNodes: nodes.length, totalEdges: edges.length,
    orphanNodes: nodes.filter((n) => !connectedIds.has(n.id)).length,
    unlinkedContents: 0, unusedAssets: 0, disconnectedForms: 0,
    recentEdges: edges.length, recentChanges: 0,
  };
},

async listRecentEdges(productId: string, limit = 5): Promise<KGRecentEdge[]> {
  if (IS_API_MODE) return apiClient.get<KGRecentEdge[]>(`/products/${productId}/graph/edges?recent=true&limit=${limit}`);
  return productEdges(productId).slice(0, limit).map((e) => {
    const fn = allNodes.find((n) => n.id === e.from);
    const tn = allNodes.find((n) => n.id === e.to);
    return { fromLabel: fn?.label ?? e.from, toLabel: tn?.label ?? e.to, verb: e.verb, createdAt: new Date().toISOString() };
  });
},
```

`contracts/responses.ts`:
```typescript
export type KGStats = { totalNodes: number; totalEdges: number; orphanNodes: number; unlinkedContents: number; unusedAssets: number; disconnectedForms: number; recentEdges: number; recentChanges: number; };
export type KGRecentEdge = { fromLabel: string; toLabel: string; verb: string; createdAt: string; };
```

---

### G.2 — Drawer de Feedback abre fora da viewport

**Tela:** Admin > Feedbacks (print 2)  
**Manifestação:** ao clicar "Ver detalhes" no menu de contexto (`MoreVertical`), o `FeedbackDetailDrawer` aparece fora da área visível.

**Diagnóstico:**

`FeedbackInboxPage.tsx` renderiza `FeedbackDetailDrawer` como filho direto do componente, dentro da árvore de layout da página. O componente `Drawer` (vaul, `direction="right"`) requer que seu conteúdo seja posicionado fixo no viewport — se o componente pai tiver `overflow: hidden` ou `position: relative`, o drawer fica preso nessa caixa.

O `ContextActionMenu` captura `{ x: e.clientX, y: e.clientY }` para posicionar o menu de contexto com base na posição absoluta do clique. Quando "Ver detalhes" é clicado, `setSelected(menu.feedback)` abre o drawer. O comportamento indica que o `DrawerContent` não está sendo renderizado em um portal no `document.body`.

**Correção:**

Verificar `frontend/src/shared/components/ui/drawer.tsx`. O componente vaul deve usar `<Vaul.Root>` com `shouldScaleBackground`. O `DrawerContent` precisa estar envolvido em um portal:

```tsx
// drawer.tsx — garantir que DrawerContent renderiza em portal
import { createPortal } from "react-dom";
// ou usar a prop nativa do vaul: <Drawer.Portal> / <DrawerPortal>
```

Se o vaul já usa portal internamente (padrão), o problema é um `z-index` ou `overflow: hidden` no `GlobalChrome` ou no layout da página. Inspecionar `GlobalChrome.tsx` e o container da `FeedbackInboxPage` para remover qualquer `overflow-hidden` que prenda elementos fixed.

**Teste de regressão:** abrir Feedbacks, clicar em MoreVertical, clicar "Ver detalhes" — drawer deve deslizar da direita sobre o conteúdo, não aparecer deslocado.

---

### G.3 — AuditEventDetail é 100% mock estático

**Tela:** Auditoria > detalhe de evento (print 3)  
**Manifestação:** independente do evento clicado, a tela sempre mostra o mesmo `aud_8f42` com dados hardcoded de "Ana Martins / permissão alterada / Permissions".

**Diagnóstico:**

`frontend/src/domains/audit/pages/AuditEventDetail.tsx` — completamente estático:
```tsx
const EVENT_ID = "aud_8f42"; // ← hardcoded
const payload = {
  actor: "Ana Martins",      // ← hardcoded
  role: "Tenant Admin",       // ← hardcoded
  module: "Permissions",      // ← hardcoded
  resource: "Editor role",    // ← hardcoded
  action: "permissão alterada",
  time: "ontem 18:10",        // ← hardcoded
};
// Antes/Depois: PRE-tags com strings literais
// Nenhum useParams(), nenhuma chamada ao auditService
```

O backend **já possui o endpoint real**:
```
GET /api/v1/tenants/{tenantId}/audit-events/{eventId}
```
Retorna `AuditEventDetail` com campos `diffJson`, `traceId`, `ip`, `userAgent`.

**Correções:**

**G.3.1 — Rota com parâmetro**  
Garantir que o router tem: `<Route path="/audit/:eventId" element={<AuditEventDetail />} />`.  
A `AuditTimeline` já deve navegar para `/audit/{event.id}` ao clicar em um evento (verificar e corrigir se necessário).

**G.3.2 — `auditService.ts` — adicionar `getEvent`**
```typescript
async getEvent(tenantId: string, eventId: string): Promise<AuditEventDetailDto> {
  if (IS_API_MODE) {
    return apiClient.get<AuditEventDetailDto>(
      `/tenants/${tenantId}/audit-events/${eventId}`
    );
  }
  // mock: retorna o primeiro evento do store com os campos extras simulados
  const found = auditStore.find((e) => e.id === eventId);
  if (!found) throw { status: 404, message: `Evento ${eventId} não encontrado.` };
  return {
    ...found,
    diffJson: { before: {}, after: {} },
    traceId: "mock_tr_" + eventId,
    ip: "127.0.0.1",
    userAgent: "mock",
  };
}
```

**G.3.3 — `AuditEventDetail.tsx` — usar params e API**
```tsx
import { useParams } from "react-router";
import { useAuth } from "../../../core/auth/useAuth";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { auditService } from "../services/auditService";

export function AuditEventDetail() {
  const { eventId } = useParams<{ eventId: string }>();
  const { effectiveTenant } = useAuth();
  const tenantId = effectiveTenant?.id ?? "";

  const { data: event, loading, error } = useAsyncData(
    () => (tenantId && eventId ? auditService.getEvent(tenantId, eventId) : Promise.resolve(undefined)),
    [tenantId, eventId]
  );

  if (loading) return <SkeletonLines />;
  if (error || !event) return <PartialErrorWidget />;

  // Antes/Depois: renderizar event.diffJson
  const before = JSON.stringify(event.diffJson?.before ?? {}, null, 2);
  const after  = JSON.stringify(event.diffJson?.after  ?? {}, null, 2);

  // botão "Abrir recurso": derivar rota do campo event.module se possível
  // exportar usa event real, não o payload hardcoded
}
```

**Contrato `AuditEventDetailDto` no frontend:**
```typescript
// audit/contracts/responses.ts — adicionar
export type AuditEventDetailDto = AuditEvent & {
  diffJson: { before: Record<string, unknown>; after: Record<string, unknown> };
  traceId: string | null;
  ip: string | null;
  userAgent: string | null;
};
```

---

## Seção H — Forms

### H.1 — SubmissionTable com formId hardcoded

**Tela:** Forms > Submissions (print 4)  
**Manifestação:** a aba Submissions mostra "Erro parcial: dados de conversão indisponíveis" imediatamente ao abrir.

**Diagnóstico:**

`frontend/src/domains/forms/pages/SubmissionTable.tsx` linha 14:
```tsx
const FORM_ID = "form-contato-comercial"; // ← hardcoded
// ...
formsService.listSubmissions(productId, FORM_ID) // → 404 em API mode
```
Em API mode, `GET /products/{productId}/forms/form-contato-comercial/submissions` retorna 404 porque nenhum formulário tem esse ID. O `PartialErrorWidget` é exibido e bloqueia toda a aba.

Mesmo padrão corrigido no `PublicationPanel.tsx` (F.5, BUG-SPRINT consolidado): o form deve ser selecionado dinamicamente.

Também: linha 92: `navigate("/forms/submissions/1")` — rota hardcoded; deve ser `/forms/submissions/{formId}/{submission.id}` ou similar.

**Correções:**

**H.1.1 — Adicionar seletor de formulário em `SubmissionTable`**
```tsx
// Igual ao padrão de PublicationPanel — seletor dinâmico de form
const { data: forms } = useAsyncData(() => productId ? formsService.listForms(productId) : Promise.resolve([]), [productId]);
const [selectedFormId, setSelectedFormId] = useState<string | null>(null);

useEffect(() => {
  if (!selectedFormId && forms && forms.length > 0) setSelectedFormId(forms[0].id);
}, [forms, selectedFormId]);

const { data: submissionsData, loading, error } = useAsyncData(
  () => (productId && selectedFormId ? formsService.listSubmissions(productId, selectedFormId) : Promise.resolve([])),
  [productId, selectedFormId]
);
```

Renderizar `SelectLike` com lista de formulários no `PageHeader`, igual ao `PublicationPanel`.

**H.1.2 — Corrigir rota de "Abrir" submission**
```tsx
// De: navigate("/forms/submissions/1")
// Para:
navigate(`/forms/${selectedFormId}/submissions/${r.id}`);
```
Garantir que a rota correspondente existe no router.

---

### H.2 — FormBuilder sem campo de nome editável

**Tela:** Forms > Form Builder (print 10)  
**Manifestação:** o formulário é sempre criado com nome "Novo formulário" e não existe UI para alterá-lo.

**Diagnóstico:**

`FormBuilder.tsx`:
- `createForm(productId, { name: "Novo formulário", type: "Contato" })` — nome fixo
- `FormBuilderCanvas` exibe `formName` como título read-only (`<h3>{formName}</h3>`)
- Nenhum `Field` para editar o nome

**Correções:**

**H.2.1 — Campo editável em `FormBuilderCanvas`**
```tsx
// Substituir <h3>{formName}</h3> por:
<Field
  label="Nome do formulário"
  value={formName}
  onChange={onFormNameChange}  // novo prop
/>
```

**H.2.2 — Persistir nome via `saveFormFields`**  
`formsService.saveFormFields` já chama `GET /forms/{id}` e então `PUT /forms/{id}` com `{ name, type, fields }`. Deve receber o `name` atualizado:
```typescript
async saveFormFields(productId: string, formId: string, fields: FormField[], name?: string): Promise<void> {
  if (IS_API_MODE) {
    const form = await apiClient.get<FormDetail>(`/products/${productId}/forms/${formId}`);
    await apiClient.put(`/products/${productId}/forms/${formId}`, {
      name: name ?? form.name,
      type: form.type,
      fields,
    });
    return;
  }
  // mock: atualizar nome no store
  const stored = formsStore.find((f) => f.id === formId);
  if (stored && name) stored.name = name;
  fieldsByFormId[formId] = fields;
}
```

**H.2.3 — Modal de criação com nome**  
Em vez de criar silenciosamente com nome padrão, `FormBuilder.tsx` deve apresentar um modal simples ao montar sem `routeFormId`:
```tsx
// Dialog com Field "Nome do formulário" + SelectLike "Tipo"
// Só chama createForm após o usuário confirmar
```

---

### H.3 — Bad Request ao publicar formulário + UX inadequada

**Tela:** Forms > Form Builder (print 11)  
**Manifestação:** ao clicar "Publicar", exibe `Erro 400 ao chamar /products/.../forms/{uuid}/publish`.

**Diagnóstico:**

O formulário foi criado com `name: "Novo formulário"` (que o backend pode rejeitar como nome padrão proibido) ou sem campos válidos. O `handlePublish` em `FormBuilder.tsx` captura o erro e exibe o toast genérico:
```tsx
toast.error("Falha ao publicar formulário", {
  description: (err as { message?: string }).message ?? "Tente novamente.",
});
```
Mas a mensagem vinda do backend não é humanizada.

Problemas adicionais:
1. `<Button onClick={() => navigate("/forms/preview")}>Preview</Button>` — navega sem passar `formId`, abrindo preview genérico
2. Sem validação de pré-condições antes de publicar (nome e ao menos 1 campo)

**Correções:**

**H.3.1 — Validação no frontend antes de publicar**
```tsx
const handlePublish = async () => {
  if (!formId) return;
  if (!form?.name || form.name === "Novo formulário") {
    toast.error("Defina um nome para o formulário antes de publicar.");
    return;
  }
  if (fields.length === 0) {
    toast.error("Adicione ao menos um campo antes de publicar.");
    return;
  }
  // salvar campos automaticamente antes de publicar
  await formsService.saveFormFields(productId, formId, fields, form?.name);
  // ... rest of publish
};
```

**H.3.2 — Codes de erro no backend**  
`FormService.publish()` deve retornar erros com `code` específico:
```json
{ "code": "form_name_required",  "message": "O formulário deve ter um nome válido antes de ser publicado." }
{ "code": "form_has_no_fields",  "message": "O formulário deve ter ao menos um campo." }
```
Frontend: mapear esses códigos para mensagens em português no `handlePublish`.

**H.3.3 — Corrigir botão Preview**
```tsx
// De: navigate("/forms/preview")
// Para:
navigate(`/forms/${formId}/preview`);
```

---

## Seção I — Páginas e Eventos

### I.1 — Evento selecionado não aparece no preview e não persiste ao reabrir

**Tela:** PageEditor > bloco event-list (prints 7, 8)  
**Manifestação:** ao marcar um evento em `EventSelector`, salvar rascunho e ver o preview, a mensagem é "Nenhum evento selecionado ainda" ou "X evento(s) — reabra o bloco". Ao reabrir o editor, a seleção voltou ao estado inicial.

**Diagnóstico:**

`EditorPanels.tsx` linha 303–310: `EventSelector.onChange` já armazena `selectedEvents` (objetos completos reduzidos) em `content`:
```tsx
onChange={(selectedEventIds, selectedEvents) => onChangeContent({
  selectedEventIds,
  selectedEvents: selectedEvents.map((ev) => ({
    id: ev.id, title: ev.title, date: ev.date, location: ev.location, type: ev.type
  })),
})}
```
`BlockRenderer.tsx` para `event-list` lê `c.selectedEvents` e renderiza os cards.

O problema está no ciclo de persistência: ao recarregar a página do browser (ou ao re-navegar), a `PageEditor` re-fetcha o page do `pagesService`. Em mock mode, o store em memória mantém o estado na sessão mas perde no reload. Em API mode, `PUT /pages/{id}` persiste o section `content` como JSON — o backend deve aceitar o campo dinâmico `selectedEvents`.

Problema adicional: `excludedKeys` inclui `"selectedEventIds"` (para não renderizar como campo de texto), mas isso também o exclui do `fieldableContent` que é enviado ao patch. Verificar se `onChangeContent` recebe a estrutura correta.

**Correções:**

**I.1.1 — Garantir que `selectedEvents` é persistido**  
Em `pagesService.updateSection` (ou equivalente), o conteúdo da seção deve ser serializado incluindo `selectedEvents`. Verificar `pagesService.ts` para garantir que o payload do PUT inclui a seção completa.

**I.1.2 — `BlockRenderer`: hidratar a partir de IDs quando `selectedEvents` estiver vazio**  
Ao montar o `BlockRenderer`, se `selectedEventIds` tiver itens mas `selectedEvents` estiver vazio (state após reload), buscar eventos do `eventsService`:
```tsx
// Dentro do case "event-list" em BlockRenderer:
// BlockRenderer não deve fazer fetches — mover a lógica de hydration
// para um wrapper EventListPreview que faz o fetch e passa os dados
```
Criar `EventListPreview.tsx`:
```tsx
function EventListPreview({ content, productSlug }: { content: Record<string, unknown>; productSlug?: string }) {
  const storedEvents = asArray(content.selectedEvents);
  const storedIds = Array.isArray(content.selectedEventIds) ? content.selectedEventIds as string[] : [];
  const [events, setEvents] = useState(storedEvents);

  useEffect(() => {
    if (storedEvents.length === 0 && storedIds.length > 0 && productSlug) {
      eventsService.listEvents(productSlug)
        .then((all) => setEvents(all.filter((e) => storedIds.includes(e.id)).map((ev) => ({
          id: ev.id, title: ev.title, date: ev.date, location: ev.location, type: ev.type
        }))));
    }
  }, [productSlug, storedIds.join(",")]);

  // renderizar events...
}
```

---

### I.2 — Imagem do evento não aparece no preview do evento nem na sidebar

**Telas:** EventsManagerDrawer preview público + sidebar (prints 8, 9)  
**Manifestação:** ao selecionar uma imagem para o evento via `MediaField`, o preview mostra `[foto do evento]` em vez da imagem real.

**Diagnóstico:**

`EventsManagerDrawer.PublicPreview`:
```tsx
const photoSrc = event.image ? resolveAssetSrc(event.image) : undefined;
// Se resolveAssetSrc retorna undefined → mostra placeholder
```

`resolveAssetSrc` em API mode retorna `${resolveBaseUrl()}/assets/${assetId}/download`.  
O problema é que a rota `/api/v1/assets/{id}/download` exige autenticação (Bearer token), mas a tag `<img src={url}>` do browser **não envia cabeçalhos de autorização**. O servidor retorna 401 e o browser exibe ícone de imagem quebrada.

Em mock mode: `findAssetByIdSync(assetId)` busca por UUID no store de assets. Assets criados em runtime na sessão são armazenados em memória, mas se o ID não for encontrado (seed sem `id` explícito), `resolveAssetSrc` retorna `undefined`.

**Correções:**

**I.2.1 — Backend: endpoint de preview de asset sem auth (ou com token de query)**  
Opção A (recomendada): criar endpoint `GET /api/v1/assets/{id}/preview` que retorna redirect para URL assinada (S3 presigned) ou serve o arquivo sem auth para assets do tenant da sessão autenticada via cookie.

Opção B (simples): permitir query param `?token={jwt}` no `/download`, validado pelo Spring Security via `BearerTokenExtractor` customizado que aceita tanto header quanto query param.

**I.2.2 — `resolveAssetSrc`: incluir token na URL em API mode**
```typescript
export function resolveAssetSrc(assetId: string | undefined): string | undefined {
  if (!assetId) return undefined;
  if (IS_API_MODE) {
    const token = getStoredToken(); // função auxiliar que lê do authStore
    const base = `${resolveBaseUrl()}/assets/${assetId}/download`;
    return token ? `${base}?token=${encodeURIComponent(token)}` : base;
  }
  const asset = findAssetByIdSync(assetId);
  return asset ? `https://placehold.co/800x400?text=${encodeURIComponent(asset.name)}` : undefined;
}
```

**I.2.3 — Mock mode: fallback para assets de sessão**  
`assetsService.ts`: garantir que assets enviados via upload rápido na sessão ficam no store em memória e podem ser recuperados por ID via `findAssetByIdSync`.

---

### I.3 — Preview não renderiza mídias reais (áudio e vídeo ficam como texto)

**Tela:** Preview de páginas (mencionado em bug 12)  
**Manifestação:** blocos de áudio e vídeo no preview mostram `[player de áudio: {assetId}]` e `[player de vídeo: {assetId}]` em vez de players reais.

**Diagnóstico:**

`BlockRenderer.tsx` — blocos `audio` e `video` renderizam placeholders de texto:
```tsx
case "audio":
  return <div>[player de áudio: {c.fileAssetId}]</div>; // ← texto
case "video":
  if (source === "upload")
    return <div>[player de vídeo: {c.fileAssetId}]</div>; // ← texto
  else
    return <div>[embed YouTube: {c.youtubeUrl}]</div>; // ← texto
```
O comentário original era intencional (preview aproximado), mas agora que `resolveAssetSrc` existe e é usada para imagens, pode ser aplicada para outros tipos.

**Correções:**

**I.3.1 — `BlockRenderer`: renderizar players reais**
```tsx
case "audio": {
  const src = resolveAssetSrc(asStr(c.fileAssetId) || undefined);
  const source = asStr(c.source, "upload");
  return (
    <div className="p-6">
      {c.title ? <h3 className="mb-2 text-xl font-semibold">{asStr(c.title)}</h3> : null}
      {source === "upload" ? (
        src
          ? <audio controls src={src} className="w-full" />
          : <div className="rounded-lg border border-border p-3 text-sm text-muted-foreground">[áudio não disponível: {asStr(c.fileAssetId, "nenhum")}]</div>
      ) : (
        // Spotify embed (não suporta <audio> direto; usar iframe ou link)
        <a href={asStr(c.spotifyUrl)} target="_blank" rel="noreferrer" className="text-primary underline">
          Ouvir no Spotify
        </a>
      )}
    </div>
  );
}

case "video": {
  const source = asStr(c.source, "upload");
  const src = source === "upload" ? resolveAssetSrc(asStr(c.fileAssetId) || undefined) : undefined;
  const youtubeEmbedUrl = source === "youtube" ? toYoutubeEmbed(asStr(c.youtubeUrl)) : undefined;
  return (
    <div className="p-6">
      {c.title ? <h3 className="mb-2 text-xl font-semibold">{asStr(c.title)}</h3> : null}
      {source === "upload" ? (
        src
          ? <video controls src={src} className="aspect-video w-full rounded-lg" />
          : <div className="flex aspect-video items-center justify-center rounded-lg border border-border text-sm text-muted-foreground">[vídeo não disponível]</div>
      ) : youtubeEmbedUrl ? (
        <iframe src={youtubeEmbedUrl} className="aspect-video w-full rounded-lg" allowFullScreen />
      ) : (
        <div className="flex aspect-video items-center justify-center rounded-lg border border-border text-sm text-muted-foreground">[URL do YouTube inválida]</div>
      )}
    </div>
  );
}
```

**Função auxiliar:**
```typescript
function toYoutubeEmbed(url: string): string | undefined {
  const match = url.match(/(?:v=|youtu\.be\/)([a-zA-Z0-9_-]{11})/);
  return match ? `https://www.youtube.com/embed/${match[1]}` : undefined;
}
```

Obs.: a renderização de `<audio>` e `<video>` com `src` autenticado tem o mesmo problema de I.2. Aplicar a mesma solução de `resolveAssetSrc` com token.

---

## Seção J — Conteúdo e Knowledge Graph

### J.1 — Hero block: `image.src: null` quebra o ImageFieldEditor

**Tela:** PageEditor > bloco hero (print 5)  
**Manifestação:** o campo de imagem no hero não aparece como picker de arquivo; o campo `image › alt` aparece como campo de texto simples, sem o botão de seleção de arquivo.

**Diagnóstico:**

`EditorPanels.tsx` linha 222–228 já injeta `image: { src: "", alt: "" }` para blocos hero quando `!isPlainObject(base.image)`. MAS: se o backend retornar `image: { src: null, alt: "4" }` (um objeto válido com `src` nulo), `isPlainObject` retorna `true` → a injeção NÃO acontece → `base.image = { src: null, alt: "4" }`.

Na renderização de `nestedObjectFields`, a checagem é:
```tsx
{typeof v.src === "string" ? (
  <ImageFieldEditor ... />  // só para string
) : (
  <ObjectFieldsEditor ... />  // src: null → cai aqui → renderiza só "alt" como Field
)}
```
Resultado: o `ImageFieldEditor` (com `MediaField`) nunca aparece.

**Correção:**

**J.1.1 — Normalizar `image.src` para string vazia quando `null` ou `undefined`**
```tsx
// EditorPanels.tsx — substituir a injeção atual
const fieldableContent = (() => {
  const base = Object.fromEntries(
    Object.entries(content).filter(([k]) => !excludedKeys.has(k))
  );
  if (section.type === "hero") {
    const existingImage = isPlainObject(base.image) ? base.image as Record<string, unknown> : {};
    return {
      ...base,
      image: {
        src: typeof existingImage.src === "string" ? existingImage.src : "",
        alt: typeof existingImage.alt === "string" ? existingImage.alt : "",
      },
    };
  }
  return base;
})();
```
Isso garante que `image.src` seja sempre `string` para blocos hero, independente do que o backend retornou.

**J.1.2 — Backend `BlockDefaults.java` (confirmação do fix E.10.1)**  
Verificar se o fix da sprint anterior foi aplicado:
```java
defaults.put(BlockType.HERO, Map.of(
  KEY_TITLE, "Novo título",
  "image",   Map.of("src", "", "alt", ""),
  "ctas",    List.of()
));
```
Se não foi aplicado, incluir nesta sprint.

---

### J.2 — Vincular música: botão com layout quebrado + aceita somente KG nodes

**Tela:** Conteúdo > Editar (Post/Manifesto/Reflexão/Poema) — Metadados (prints 13, 16)  
**Manifestação:** (a) botão "Buscar entidade musical" exibe ícone grudado no texto; (b) ao clicar, abre picker de KG nodes genéricos em vez de seletores específicos de áudio/música.

**Diagnóstico:**

`ContentArticleEditor.tsx` — `MetadataPanel`:
```tsx
<Button><Link2 size={14} />Buscar entidade musical</Button>
// ↑ ícone sem flex/gap → aparece colado ao texto
```

Além disso, o `EntityPicker` lista nós do grafo de conhecimento (qualquer tipo: Página, Evento, etc.). Conforme as ADRs do sistema:
- A vinculação de música deve suportar: **(1)** asset de áudio já presente nos Assets do produto; **(2)** URL do Spotify (track ou playlist).
- O KG node `MUSIC_REF` é criado como referência secundária, não como o ponto de entrada da seleção.

**Correções:**

**J.2.1 — Corrigir layout do botão**
```tsx
<Button className="flex items-center gap-1.5">
  <Link2 size={14} />
  Buscar entidade musical
</Button>
```

**J.2.2 — Substituir `EntityPicker` por `MusicPicker` dedicado**  
Criar `frontend/src/domains/content/components/MusicPicker.tsx`:
```tsx
type MusicSource = { type: "asset"; assetId: string; name: string } | { type: "spotify"; url: string };

export function MusicPicker({ productId, onSelect }: { productId: string; onSelect: (src: MusicSource) => void }) {
  const [tab, setTab] = useState<"asset" | "spotify">("asset");
  const [spotifyUrl, setSpotifyUrl] = useState("");
  // aba "asset": usa AssetPickerModal com typeFilter="áudio"
  // aba "spotify": campo de texto para URL + validação básica de URL spotify
  return (
    <div className="w-80">
      {/* Tabs: Asset | Spotify */}
      {/* aba asset: <AssetPickerModal inline ou listagem filtrada> */}
      {/* aba spotify: <Field label="URL do Spotify" /> + botão confirmar */}
    </div>
  );
}
```

**J.2.3 — Atualizar `MetadataPanel` para usar `MusicPicker`**
```tsx
const handleSelectMusic = (src: MusicSource) => {
  if (src.type === "asset") {
    onPatchMetadata({ musicReferenceId: src.assetId, musicReferenceLabel: src.name, musicSource: "asset" });
  } else {
    onPatchMetadata({ musicReferenceId: src.url, musicReferenceLabel: src.url, musicSource: "spotify" });
  }
};
```

---

### J.3 — EntityPicker retorna mocks/seed em vez de conteúdo real publicado

**Tela:** Conteúdo > Editar > "Vincular a outro conteúdo" (prints 14)  
**Manifestação:** o modal "Vincular a outro conteúdo" mostra entidades de seed ("Vigilia", "Clair de Lune - Debussy", "Manifesto do Silêncio") que são fixtures do produto Loki, não conteúdo real publicado do produto ativo.

**Diagnóstico:**

`EntityPicker.tsx` chama `knowledgeService.searchNodes(q, productId)`.

Em **mock mode**: filtra o store de nós de seed (`lokiKgNodes`, `kgNodes`, etc.) por produto → retorna fixtures.

Em **API mode**: `GET /products/{productId}/graph/nodes?q={q}` — se o KG ainda não foi populado com `ensureNodeForContent` (feature nova, conteúdo antigo nunca passou por ela), a busca retorna vazio → picker mostra "Nenhuma entidade encontrada".

O contrato correto: ao vincular a outro conteúdo, o autor deve ver **conteúdo publicado do mesmo produto** como candidatos linkáveis.

**Correções:**

**J.3.1 — Fallback para conteúdo publicado quando KG vazio**  
`knowledgeService.searchNodes`: quando a busca retorna 0 resultados em API mode, o chamador deve exibir uma mensagem orientadora. Mas o melhor é popular o KG automaticamente.

**J.3.2 — Seed de KG para conteúdo existente**  
Quando `KnowledgeOverview` ou `EntitySearch` montam pela primeira vez E o KG tem 0 nós E o produto tem conteúdo publicado, disparar:
```typescript
// knowledgeService — novo método
async seedNodesFromContent(productId: string, contents: ContentRow[]): Promise<void> {
  for (const c of contents.filter((x) => x.status === "Published")) {
    await this.ensureNodeForContent(productId, c.id, c.title, "Página");
  }
}
```
Chamar em `KnowledgeOverview.tsx` após carregar `listNodes` e detectar lista vazia.

**J.3.3 — `EntityPicker`: mostrar conteúdo publicado como alternativa**  
Quando KG retorna vazio, exibir aviso + botão para navegar ao módulo KG:
```tsx
{results.length === 0 && q.length === 0 && (
  <p className="p-2 text-xs text-muted-foreground">
    Nenhuma entidade no grafo ainda. Conteúdos publicados aparecem automaticamente após a primeira edição.
  </p>
)}
```

---

### J.4 — "Enviar para revisão" sem try/catch, sem toast, botão sempre visível

**Tela:** Conteúdo > Editar (print 15)  
**Manifestação:** ao clicar "Enviar para revisão", a tela não muda e nenhum toast aparece.

**Diagnóstico:**

`ContentEditor.tsx`:
```tsx
const handleSubmitForReview = async () => {
  if (!id) return;
  await contentService.submitForReview(id, productId); // ← sem try/catch
  setContent((prev) => (prev ? { ...prev, status: "In Review" } : prev));
  toast.success("Enviado para revisão.", { ... });
  navigate("/content/workflow");
};
```
Se `submitForReview` lança (ex.: 400 porque `content.status` já é `"Published"` — transição inválida), o erro não é capturado → toast e navigate nunca executam → o usuário fica preso na tela sem feedback.

No print: o badge mostra `Published` mas o botão "Enviar para revisão" ainda aparece → a transição é inválida.

**Correções:**

**J.4.1 — Adicionar try/catch**
```tsx
const handleSubmitForReview = async () => {
  if (!id) return;
  try {
    await contentService.submitForReview(id, productId);
    setContent((prev) => (prev ? { ...prev, status: "In Review" } : prev));
    toast.success("Enviado para revisão.", { description: "A equipe editorial será notificada." });
    navigate("/content/workflow");
  } catch (err: unknown) {
    const code = (err as { code?: string }).code;
    const message = code === "invalid_transition"
      ? "Esta transição não é permitida para o status atual do conteúdo."
      : ((err as { message?: string }).message ?? "Tente novamente.");
    toast.error("Não foi possível enviar para revisão.", { description: message });
  }
};
```

**J.4.2 — Botão contextual por status**  
No `ContentEditor.tsx`, substituir o botão fixo por ações condicionais:
```tsx
// Mapeamento status → ação principal
const primaryAction = content.status === "Draft"
  ? { label: "Enviar para revisão", action: handleSubmitForReview }
  : content.status === "In Review"
  ? null // Workflow Board gerencia revisão
  : null; // Published / Archived: sem ação primária no editor
```
Quando `primaryAction === null`, esconder o botão ou exibir apenas "Preview".

**J.4.3 — Backend: código de erro na resposta de transição inválida**  
`ContentTransitionService.transition()` deve retornar `code: "invalid_transition"` no corpo 400 para que o frontend humanize a mensagem.

---

### J.5 — Imagem de capa: picker com erro persistente, sem thumbnail após seleção

**Tela:** Conteúdo > Editar > Metadados > "Selecionar imagem" (print 17)  
**Manifestação:** (a) `AssetPickerModal` abre com "Erro parcial: dados de conversão indisponíveis" e o erro persiste mesmo após fechar e reabrir; (b) ao selecionar uma imagem, nenhum thumbnail aparece no campo, apenas o nome do arquivo.

**Diagnóstico:**

`ContentArticleEditor.MetadataPanel` → `<MediaField label="Imagem de capa" typeFilter="imagem" onChange={(v) => onPatchMetadata({ coverImage: v })} />`

O picker usa `AssetPickerModal` que chama `assetsService.listAssets(productId)`. Em API mode, se esse endpoint falha (ex.: produto sem módulo Assets habilitado, ou erro transiente), `PartialErrorWidget` é exibido dentro do modal. O modal não distingue "Assets indisponíveis" de "dados de conversão" — a mesma widget é usada em todo lugar, gerando mensagem enganosa.

`MediaField.tsx`: após seleção, exibe `displayName` (string) mas **não há `<img>` de thumbnail**. O usuário não tem feedback visual da imagem escolhida.

**Correções:**

**J.5.1 — `AssetPickerModal`: mensagem específica quando assetsService falha**
```tsx
// Em vez de PartialErrorWidget genérico:
if (error) return (
  <div className="p-4 text-sm text-muted-foreground">
    Não foi possível carregar os assets. Verifique se o módulo Assets está habilitado neste produto.
    <Button onClick={refetch} className="mt-2">Tentar novamente</Button>
  </div>
);
```

**J.5.2 — `MediaField`: adicionar thumbnail para imagens**
```tsx
// MediaField — quando typeFilter === "imagem" e value está preenchido
const thumbSrc = typeFilter === "imagem" && value ? resolveAssetSrc(value) : undefined;
// No JSX, acima do displayName:
{thumbSrc && (
  <img src={thumbSrc} alt="preview" className="mr-2 h-10 w-10 rounded object-cover" />
)}
```

**J.5.3 — Limpar asset de upload ao cancelar**  
Se o usuário clicou "Upload rápido" dentro do `AssetPickerModal` e depois clicou "Cancelar", o asset foi enviado ao servidor mas não foi vinculado ao conteúdo. O modal deve rastrear se fez um upload nesta sessão e, ao cancelar, chamar `DELETE /products/{productId}/assets/{uploadedId}`.
```tsx
// AssetPickerModal — estado
const [sessionUploadId, setSessionUploadId] = useState<string | null>(null);
// Ao fazer upload rápido, salvar ID
// Ao cancelar: if (sessionUploadId) await assetsService.deleteAsset(productId, sessionUploadId);
```

---

## Seção K — Assets: Raiz Transversal

### K.1 — AssetPickerModal com "Erro parcial" em múltiplos contextos

**Contexto:** raiz dos bugs J.5, I.2 (imagem do evento), G.2 (qualquer picker de mídia)

**Diagnóstico:**

`assetsService.listAssets(productId)` em API mode pode falhar por:
1. Produto sem módulo Assets habilitado → backend retorna 403 `MODULE_DISABLED`
2. Token expirado → 401
3. Produto com `id = "b0000000-..."` seed → sem assets reais no banco

O `AssetPickerModal` usa `useAsyncData(() => assetsService.listAssets(...), [...])` e em caso de erro exibe `PartialErrorWidget` com mensagem genérica "dados de conversão indisponíveis" (mensagem copiada de outro widget, inadequada para este contexto).

**Correções:**

**K.1.1 — Mapear erro por código**
```typescript
// assetsService.ts — em API mode, capturar e enriquecer erros:
async listAssets(productId: string): Promise<AssetSummary[]> {
  if (IS_API_MODE) {
    try {
      return await apiClient.get<AssetSummary[]>(`/products/${productId}/assets`);
    } catch (err: unknown) {
      const status = (err as { status?: number }).status;
      if (status === 403) throw { status: 403, code: "module_disabled", message: "Módulo Assets não habilitado neste produto." };
      throw err;
    }
  }
  // ...
}
```

**K.1.2 — `AssetPickerModal`: mensagem diferenciada por código de erro**  
Verificar `error.code` e exibir texto específico em vez de `PartialErrorWidget`.

**K.1.3 — Habilitar módulo Assets nos produtos de teste**  
No seed SQL (`migration/V30__dados_reais.sql` ou equivalente), garantir que os produtos de teste têm o módulo `ASSETS` habilitado para que o picker funcione end-to-end.

---

### K.2 — Imagens não carregam no preview de páginas (autenticação no /download)

**Contexto:** raiz dos bugs de imagem em preview (hero, image, image-text, gallery, cover)

**Diagnóstico:**

`resolveAssetSrc(assetId)` em API mode retorna `${resolveBaseUrl()}/assets/${assetId}/download`.  
A URL é inserida em `<img src={...}>`, mas o browser não envia o `Authorization: Bearer {token}` nesta requisição → o endpoint retorna 401 → imagem quebrada.

**Correções:**

**K.2.1 — Backend: endpoint `/preview` público-por-sessão**  
Adicionar `GET /api/v1/assets/{id}/preview` que usa a sessão HTTP (cookie `JSESSIONID` ou similar) ao invés do Bearer header. Redireciona para a URL do arquivo no storage local/S3.

OU:

**K.2.2 — Frontend: fetch com token + blob URL**  
Criar hook `useAuthenticatedImage(assetId)` que faz `fetch` com o token e gera uma blob URL para uso em `<img>`:
```typescript
function useAuthenticatedImage(assetId: string | undefined): string | undefined {
  const [blobUrl, setBlobUrl] = useState<string | undefined>();
  useEffect(() => {
    if (!assetId || !IS_API_MODE) return;
    const token = getStoredToken();
    fetch(`${resolveBaseUrl()}/assets/${assetId}/download`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.blob())
      .then((blob) => setBlobUrl(URL.createObjectURL(blob)))
      .catch(() => setBlobUrl(undefined));
    return () => { if (blobUrl) URL.revokeObjectURL(blobUrl); };
  }, [assetId]);
  return blobUrl;
}
```

Usar este hook em `BlockRenderer` para todos os casos de imagem. Em mock mode, continuar usando `resolveAssetSrc` (que já retorna placeholder via `placehold.co`).

**K.2.3 — Mock mode: garantir seed assets com URL resolvível**  
`assetsService.findAssetByIdSync`: aceitar busca por `name` (para seeds antigos) E por `id` (para assets de sessão). Retornar sempre `{ name, friendlyName }` para que o placeholder `placehold.co?text=...` seja legível.

---

## Seção L — Verificação Abrangente

### L.1 — Agent deve simular fluxos reais e corrigir inconsistências visuais

**Manifestação:** vários componentes têm layout quebrado ou estados incompletos não detectáveis por inspeção estática.

**Instrução para o agente:**

Após implementar todas as correções das seções G–K, o agente deve **simular os seguintes fluxos em modo API** (com instância local rodando) e corrigir qualquer problema encontrado:

1. **Fluxo Forms completo**: criar formulário (com nome custom) → adicionar campos → salvar rascunho → publicar → verificar em PublicationPanel → verificar em Submissions
2. **Fluxo Evento**: criar evento com imagem no EventsManagerDrawer → selecionar no bloco event-list → salvar página → verificar no preview
3. **Fluxo Conteúdo**: criar Post → vincular imagem de capa → vincular música (asset áudio) → escrever body com kg-ref → enviar para revisão → verificar no Workflow Board
4. **Fluxo Assets**: upload de imagem → usar em hero → verificar no preview da página
5. **Fluxo Auditoria**: gerar evento (ex.: criar produto) → navegar para Auditoria → clicar no evento → verificar detalhe com diffJson real
6. **Fluxo Feedback**: criar feedback → abrir detalhe via drawer → verificar que drawer abre corretamente no viewport
7. **Responsividade**: verificar cada tela nas larguras 375px, 768px, 1280px. Corrigir qualquer overflow, sobreposição ou botão inacessível.

Para cada problema visual encontrado: **criar issue inline como comentário `// VISUAL-BUG: ...`** no arquivo afetado, **e corrigir imediatamente** sem criar nova tarefa pendente.

---

## Seção M — Gestão de Usuários: Hierarquia de Exclusão

### M.1 — Hierarquia de deleção/remoção de usuários: backend sem guarda + frontend sem botão

**Tela:** Admin > Usuários (visão tenant) · Configurações > Equipe (visão produto)  
**Gravidade:** 🔴 GRAVE — TENANT_ADMIN pode remover SUPER_ADMIN sem restrição; usuário removido não recebe email

**Sintoma observado:**
- `UserTable.tsx` não tem botão "Remover" em nenhuma linha — a ação não existe na UI (print: apenas "Abrir" e "Permissões")
- `TenantUserService.removeUser()` não verifica hierarquia de papéis: chamada `DELETE /tenants/{tenantId}/users/{userId}` por TENANT_ADMIN para um SUPER_ADMIN não é bloqueada
- O template `userRemoved.ftl` existe (criado na tarefa #87) mas **nunca é chamado** em `removeUser()`

**Matriz de permissões (requisito refinado):**

| Actor | Pode fazer |
|---|---|
| `SUPER_ADMIN` | Deletar qualquer usuário de qualquer tenant via `DELETE /tenants/{tenantId}/users/{userId}`. Pode também **auto-revogar** seu próprio papel de `PRODUCT_MANAGER` em qualquer produto (via `DELETE /products/{productId}/assignments/{userId}` com o próprio `userId`). |
| `TENANT_ADMIN` | Deletar qualquer usuário do seu tenant, **exceto** outro `SUPER_ADMIN` (neste caso, só pode revogar o papel de produto via assignments). Não pode deletar outro `TENANT_ADMIN` do mesmo nível. |
| `PRODUCT_MANAGER` | Revogar acesso de `EDITOR` ou `VIEWER` do seu produto (`DELETE /products/{productId}/assignments/{userId}`). Email obrigatório ao usuário removido. Não tem acesso ao endpoint de tenant. |
| `EDITOR` / `VIEWER` | Sem ação de remoção. |

**Regra especial — SUPER_ADMIN como membro de produto:** Se um `SUPER_ADMIN` possui uma `ProductAssignment` como `PRODUCT_MANAGER`, `EDITOR` ou `VIEWER` em um produto, o `TENANT_ADMIN` pode revogar essa atribuição de produto (`DELETE .../assignments/{userId}`), mas **nunca** chamar o endpoint de exclusão de tenant (`DELETE .../tenants/{tenantId}/users/{userId}`) para esse usuário.

**Exceção CLIENTES BETA:** Quando o criador do produto for `SUPER_ADMIN` e o tenant for `CLIENTES BETA`, o sistema deve auto-atribuir o próprio criador como `PRODUCT_MANAGER` (sem exigir campo de PM externo na modal). O SUPER_ADMIN pode posteriormente auto-revogar esse papel e atribuir outro PM.

**Correção Backend — `TenantUserService.removeUser()`:**

```java
// Adicionar ANTES de tenantUserAccessService.remove(...)
RoleHierarchy callerLevel  = RoleHierarchy.of(caller.highestRole());
RoleHierarchy targetLevel  = RoleHierarchy.of(current.role());
if (!callerLevel.canActOn(targetLevel)) {
    throw new ForbiddenException(
        "Caller com papel " + caller.highestRole() +
        " não pode remover usuário com papel " + current.role()
    );
}

// Adicionar APÓS remoção bem-sucedida:
notificationService.send(userId, "userRemoved", Map.of(
    "tenantName", tenant.getName(),
    "removedBy",  caller.name()
));
```

**Arquivo:** `backend/.../user/service/TenantUserService.java`  
Criar ou reutilizar `RoleHierarchy` enum (verificar se já existe no pacote `core` ou `shared`; se não, criar com método `canActOn(RoleHierarchy other): boolean`).  
**JaCoCo 100%:** mínimo 5 casos — SA remove Editor ✓, TA remove Editor ✓, TA tenta remover SA → 403 ✓, PM tenta endpoint de tenant → 403 ✓, remoção bem-sucedida dispara email ✓.

**Correção Backend — `ProductAssignmentService.removeAssignment()`:**
- Validar que `PRODUCT_MANAGER` só pode remover `EDITOR` ou `VIEWER` — não outro `PRODUCT_MANAGER`
- Adicionar chamada de notificação `userRemovedFromProduct` após remoção (reutilizar `userRemoved.ftl` ou criar template específico)

**Correção Frontend — `UserTable.tsx`:**

```tsx
// Linha ~234, dentro do <div className="flex gap-1"> na coluna Ações:

{/* Visão tenant — SUPER_ADMIN e TENANT_ADMIN */}
{isTenantWideView && canRemoveFromTenant(viewAsRole, u.role) && (
  <Button onClick={() => setConfirmRemove(u)}>Remover</Button>
)}

{/* Visão produto — PRODUCT_MANAGER remove EDITOR/VIEWER */}
{!isTenantWideView && canRemoveFromProduct(viewAsRole, u.role) && (
  <Button onClick={() => setConfirmRemoveFromProduct(u)}>Remover do produto</Button>
)}
```

Criar `frontend/src/shared/utils/roleHierarchy.ts`:
```typescript
const RANK: Record<string, number> = {
  super_admin: 5, tenant_admin: 4,
  product_manager: 3, editor: 2, viewer: 1,
};
export function canRemoveFromTenant(callerRole: string, targetRole: string): boolean {
  if (callerRole === "super_admin") return true;
  if (callerRole === "tenant_admin") return RANK[targetRole] < RANK["tenant_admin"];
  return false;
}
export function canRemoveFromProduct(callerRole: string, targetRole: string): boolean {
  if (callerRole === "product_manager") return RANK[targetRole] < RANK["product_manager"];
  return false;
}
```

Usar `ConfirmDialog` existente antes de qualquer chamada destrutiva.  
**Endpoints:** `DELETE /api/v1/tenants/{tenantId}/users/{userId}` (admin) · `DELETE /api/v1/products/{productId}/assignments/{userId}` (PM).

---

## Seção N — Produtos: Datas, Dashboard e PM Obrigatório

### N.1 — Datas ISO raw exibidas nos cards e na lista de produtos

**Tela:** Admin > Produtos (grid e lista)  
**Gravidade:** 🟡 MÉDIO — degradação visual evidente, impacta percepção de qualidade

**Sintoma observado (print):** Todos os cards mostram `2026-07-05T00:07:38.599528Z` literalmente em vez de data/hora legível.

**Causa raiz:**
- `ProductCard.tsx` linha 76: `<p ...>{p.last}</p>` — string ISO passada diretamente ao JSX
- `ProductsList.tsx` linha 53: `{p.type} · {p.last}` — mesma string na visão lista
- Nenhum utilitário de formatação de data existe em `shared/utils/`

**Correção — criar `frontend/src/shared/utils/formatDate.ts`:**
```typescript
/** Formata ISO UTC para data/hora no fuso local do browser, ex.: "05/07/2026, 00:07" */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    }).format(new Date(iso));
  } catch {
    return iso;
  }
}
```

Substituir em ambos os arquivos:
```tsx
// ProductCard.tsx linha 76:
<p className="mt-4 text-sm text-muted-foreground">{formatDateTime(p.last)}</p>

// ProductsList.tsx linha 53:
<p className="text-sm text-muted-foreground">{p.type} · {formatDateTime(p.last)}</p>
```

---

### N.2 — Dashboard Global não se atualiza após criação de produto

**Tela:** Dashboard Global  
**Gravidade:** 🟡 MÉDIO — dados stale imediatamente após ação do usuário; contador de produtos, timeline e KPIs não refletem o produto recém-criado

**Sintoma observado (print):** Após criar "Novo Produto" e retornar ao Dashboard, "Produtos ativos" ainda mostra 5, a timeline e alertas não mencionam o novo produto.

**Causa raiz (`DashboardGlobal.tsx` linha 28):**
```typescript
const { data: summary, loading, error } = useAsyncData(
  () => dashboardService.getSummary(),
  [] // ← deps vazio — nunca re-fetcha após montagem inicial
);
```
`handleProductCreated` (linha 39) navega para o produto criado mas não invalida o summary. `activeProducts` usa `tenantProducts` (atualiza via AuthContext), mas todos os KPIs do summary ficam stale.

**Correção (`DashboardGlobal.tsx`):**
```typescript
const [summaryKey, setSummaryKey] = useState(0);

const { data: summary, loading, error } = useAsyncData(
  () => dashboardService.getSummary(),
  [summaryKey], // ← re-fetcha ao incrementar
);

const handleProductCreated = (created: ProductSummary) => {
  setShowCreateProduct(false);
  setSummaryKey((k) => k + 1); // ← invalida o cache do summary
  if (!authUser) return;
  const product: ProductOption = { ... };
  selectProduct(product);
  navigate(getPostLoginLandingPath(authUser.role, product));
};
```

---

### N.3 — CreateProductModal não coleta nem valida Product Manager obrigatório

**Tela:** Dashboard Global → "Criar produto" · Produtos → "Novo produto"  
**Gravidade:** 🔴 GRAVE — regra de negócio violada: produto pode nascer sem PM designado; quando criador é SUPER_ADMIN, o backend auto-atribui SA como PM, conflitando com ADR-0018

**Regra de negócio:** Todo produto deve ter obrigatoriamente um Product Manager no momento da criação.

**Sintoma:** `CreateProductModal.tsx` não tem campo de email do PM. Não há validação de PM na modal nem no `CreateProductRequest` (TS). O backend (`ProductService.createProduct()` linha 94) auto-atribui o `caller.subject()` como `PRODUCT_MANAGER` — incorreto quando caller é SUPER_ADMIN.

**Exceção CLIENTES BETA:** Quando o criador for SUPER_ADMIN e o tenant for CLIENTES BETA, o campo de PM deve ser **pré-preenchido com o email do próprio SUPER_ADMIN** (editável). O SUPER_ADMIN pode depois auto-revogar esse papel via M.1.

**Correção Frontend — `CreateProductModal.tsx`:**
```tsx
const [pmEmail, setPmEmail] = useState(
  // Pré-preencher com próprio email se CLIENTES BETA (ou padrão vazio)
  tenantName.toLowerCase() === "clientes beta" ? (authUser?.email ?? "") : ""
);
const pmEmailErr = pmEmail && !pmEmail.includes("@")
  ? "Email inválido"
  : !pmEmail
  ? "Product Manager é obrigatório"
  : undefined;

// No formulário, após SelectLike de Tipo:
<Field
  label="Email do Product Manager *"
  value={pmEmail}
  onChange={setPmEmail}
  type="email"
  error={touched.pmEmail ? pmEmailErr : undefined}
  onBlur={() => setTouched((t) => ({ ...t, pmEmail: true }))}
/>

// Na validação:
const hasErrors = !!nameErr || !!slugErr || !!pmEmailErr || checkingSlug;

// No handleCreate, incluir pmEmail no payload:
await productsService.create({ ..., pmEmail });
```

**Correção Backend — `CreateProductRequest.java`:**
```java
public record CreateProductRequest(
    @NotNull  UUID   tenantId,
    @NotBlank String key,
    @NotBlank String name,
    @NotBlank String type,
    @NotBlank String defaultLocale,
    AssetStorageStrategy assetStorageStrategy,
    @NotBlank @Email String productManagerEmail  // ← campo obrigatório
) {
    public CreateProductCommand toCommand() {
        return new CreateProductCommand(
            tenantId, key, name, type, defaultLocale,
            assetStorageStrategy, productManagerEmail
        );
    }
}
```

**Correção Backend — `ProductService.createProduct()`:**
```java
// Substituir auto-atribuição do caller por atribuição do PM designado:
String pmSubject = identityUserService
    .findSubjectByEmail(command.productManagerEmail(), command.tenantId())
    .orElseThrow(() -> new ValidationException(
        "Usuário com email '" + command.productManagerEmail() + "' não encontrado neste tenant. " +
        "Convide o usuário primeiro ou informe outro email."
    ));

assignmentRepository.save(new ProductAssignment(
    product, pmSubject, ProductAssignmentRole.PRODUCT_MANAGER
));
```

Se o email não existir no tenant → retornar 422 com mensagem clara; a UI deve exibir o erro no campo de PM.  
**JaCoCo 100%:** PM existente no tenant ✓, PM não encontrado → 422 ✓, PM = o próprio caller (CLIENTES BETA) ✓.

---

### N.4 — Módulos da criação não são propagados: sidebar e EditModal mostram defaults do tipo

**Tela:** Dashboard Global → "Criar produto" → sidebar do produto + modal "Editar" (print: 6 módulos marcados quando só 1 foi selecionado)  
**Gravidade:** 🔴 GRAVE — o produto nasce com configuração de módulos diferente da que o usuário escolheu; menus de Forms, Assets etc. aparecem no sidebar sem que o produto os tenha habilitado

**Sintoma:** Usuário cria produto "Site Institucional" desmarcando módulos manualmente. Após criação:
- O sidebar mostra todos os itens de navegação do tipo (Forms, Assets, Conteúdo...)
- O modal "Editar" re-abre com todos os defaults do tipo marcados (print: Páginas ✅ Conteúdo ✅ Assets ✅ Forms ✅ SEO ✅ Analytics ✅)
- A legenda "Mesma seleção da criação" é falsa — mostra os defaults do tipo, não a seleção real

**Causa raiz técnica — `DashboardGlobal.tsx` linha 42:**
```tsx
// ATUAL — módulos perdidos:
const product: ProductOption = {
  id: created.id ?? created.name, name: created.name,
  type: created.type, status: created.status,
  modules: created.modules,
  // ← modulesList: created.modulesList AUSENTE
};
selectProduct(product);
```

`productsService.create()` mock retorna corretamente `modulesList: req.initialModules` (só os módulos escolhidos). Mas o `ProductOption` construído em `handleProductCreated` não repassa esse campo. Consequência:

- `resolveEnabledModules({ type: "Site Institucional", modulesList: undefined })` → fallback para os 6 defaults do tipo → sidebar exibe todos os módulos
- `useModuleSelection("Site Institucional", undefined)` no `EditProductModal` → inicializa com 6 defaults marcados

**Correção técnica (`DashboardGlobal.tsx` e `ProductSelectScreen.tsx`):**
```tsx
const product: ProductOption = {
  id: created.id ?? created.name, name: created.name,
  type: created.type, status: created.status,
  modules: created.modules,
  modulesList: created.modulesList,   // ← adicionar
};
selectProduct(product);
// Também chamar addProduct para que `tenantProducts` reflita o novo produto:
addProduct(effectiveTenant.id, product);
```

**Causa raiz de UX — type defaults pré-marcados sem aviso de divergência:**

Quando o usuário cria com tipo "Site Institucional", todos os módulos padrão desse tipo ficam marcados. Se o usuário desmarcar alguns, não há nenhum feedback indicando que o produto deixou de ser um "Site Institucional" padrão. O resultado é confusão sobre o que o produto realmente tem habilitado.

**Decisão de design (escolher UMA das opções abaixo):**

| Opção | Descrição | Prós/Contras |
|---|---|---|
| **A — Auto-switch para Custom** (recomendada) | Quando o usuário desmarca/marca qualquer módulo que desvie dos defaults do tipo selecionado, o campo Tipo muda automaticamente para "Custom" | Mais intuitivo — o tipo sempre reflete a configuração real; pode surpreender o usuário |
| **B — Aviso de divergência** | Manter o tipo mas exibir badge/tooltip "Configuração personalizada — difere do padrão Site Institucional" | Menos disruptivo; mas o tipo mentiroso permanece no banco |
| **C — Bloquear desmarcação** | Módulos defaults do tipo ficam bloqueados (disabled checkbox); para ter controle total, o usuário deve escolher "Custom" | Mais simples de implementar; menos flexível |

**Implementação da opção A (recomendada) em `useModuleSelection.ts`:**
```typescript
const setType = (next: string) => {
  const nextType = next as ProductTypeKey;
  setTypeState(nextType);
  // Resetar para defaults do tipo ao trocar explicitamente
  setSelectedModules(new Set(
    (PRODUCT_TYPE_MODULE_DEFAULTS[nextType] ?? []).filter((m) => m.default).map((m) => m.key)
  ));
};

const toggleModule = (key: string) => {
  setSelectedModules((prev) => {
    const next = new Set(prev);
    if (next.has(key)) { next.delete(key); if (key === KNOWLEDGE_GRAPH_DEPENDENCY) next.delete("Knowledge Graph"); }
    else { next.add(key); }

    // Auto-switch para Custom se a seleção diverge dos defaults do tipo atual
    const typeDefaults = new Set(
      (PRODUCT_TYPE_MODULE_DEFAULTS[type] ?? []).filter((m) => m.default).map((m) => m.key)
    );
    const isDivergent =
      next.size !== typeDefaults.size ||
      [...next].some((k) => !typeDefaults.has(k));
    if (isDivergent && type !== "Custom") setTypeState("Custom");

    return next;
  });
};
```

Ao trocar o tipo para "Custom" automaticamente, exibir `toast.info("Tipo alterado para Custom — você personalizou os módulos.")` para que o usuário perceba a mudança.

---

---

## Seção O — Identidade e Ativação de Conta

### O.1 — 🚨 PRIORIDADE ALTÍSSIMA — Conta bloqueada após ativação por falta de nome/sobrenome no Keycloak

**Tela:** `/invite?token=...` → LoginScreen  
**Gravidade:** 🔴🔴 CRÍTICO — usuário convidado completa o fluxo de ativação e **não consegue logar**. A plataforma está inacessível para qualquer novo editor/viewer convidado.

**Reprodução:**
1. TENANT_ADMIN convida `teste@gmail.com` como Editor
2. Usuário recebe email, clica no link de ativação
3. `InviteScreen` → usuário define senha e clica "Ativar conta"
4. Redirecionado para login
5. Login retorna: **"Esta conta está bloqueada. Entre em contato com o administrador."**

**Causa raiz — cadeia completa:**

**1. `InviteScreen.tsx` — sem campos de nome:**
```tsx
// Formulário atual — apenas senha + confirmação + termos
// firstName e lastName NÃO são coletados em nenhum momento da ativação
<input type="password" value={pwd} ... />
<input type="password" value={confirm} ... />
<input type="checkbox" checked={terms} ... />
```

**2. `KeycloakAdminClient.createUserForInvitation()` (linha 257-278):**
```java
// Se o convite foi enviado com name = "teste" (uma palavra):
firstName("teste")  // → "teste"   (tudo antes do espaço)
lastName("teste")   // → null      (nenhum espaço encontrado → null)

// Se o convite foi enviado sem nome (name = ""):
firstName("")  // → null
lastName("")   // → null
```
Keycloak recebe `lastName: null` → perfil incompleto

**3. `AuthActivationService.activate()` (linha 73-84):**
```java
public AuthMessageResponse activate(UUID tokenId, String password) {
    passwordPolicy.assertStrong(password);
    AuthActionToken token = tokenService.consumeInvite(tokenId);
    updatePassword(token.getKeycloakId(), password);
    keycloakAdminClient.setUserEnabled(token.getKeycloakId(), true);
    keycloakAdminClient.clearRequiredActions(token.getKeycloakId());
    // ← NUNCA atualiza firstName/lastName no Keycloak
    // ← Se realm tiver VERIFY_PROFILE required action, o Keycloak
    //    bloqueará o login mesmo com enabled=true
}
```

**4. `LoginScreen.tsx` linha 54:**
```typescript
blocked: "Esta conta está bloqueada. Entre em contato com o administrador.",
// Keycloak retorna: "error_description=Account is not fully set up"
// → interpretado como "bloqueado" mesmo que a causa seja perfil incompleto
```

**Correção Frontend — `InviteScreen.tsx`:**

Adicionar campos obrigatórios de nome e sobrenome antes do campo de senha:
```tsx
const [firstName, setFirstName] = useState(() => {
  // Pré-preencher com o primeiro token do nome do convite
  const parts = (invite?.userName ?? "").trim().split(" ");
  return parts[0] ?? "";
});
const [lastName, setLastName] = useState(() => {
  const parts = (invite?.userName ?? "").trim().split(" ");
  return parts.slice(1).join(" ");
});

const firstNameErr  = !firstName.trim() ? "Nome é obrigatório" : undefined;
const lastNameErr   = !lastName.trim()  ? "Sobrenome é obrigatório" : undefined;
const hasProfileErr = !!firstNameErr || !!lastNameErr;

// No formulário (antes dos campos de senha):
<label className="block">
  <span className="mb-1 block text-sm font-medium">Nome *</span>
  <input type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)}
    placeholder="Alexandre" className="..." />
  {firstNameErr && <p className="mt-1 text-xs text-destructive">{firstNameErr}</p>}
</label>
<label className="block">
  <span className="mb-1 block text-sm font-medium">Sobrenome *</span>
  <input type="text" value={lastName} onChange={(e) => setLastName(e.target.value)}
    placeholder="Henrique" className="..." />
  {lastNameErr && <p className="mt-1 text-xs text-destructive">{lastNameErr}</p>}
</label>

// Em handleActivate, rejeitar se perfil incompleto e passar os dados:
if (hasProfileErr) { setError("Nome e sobrenome são obrigatórios."); return; }
authActivationService.activateAccount(token, pwd, firstName.trim(), lastName.trim())
```

**Correção Backend — `AuthActivationService.activate()`:**
```java
// Assinatura atual:
public AuthMessageResponse activate(UUID tokenId, String password)

// Nova assinatura (adicionar firstName e lastName):
public AuthMessageResponse activate(UUID tokenId, String password,
                                    String firstName, String lastName) {
    passwordPolicy.assertStrong(password);
    AuthActionToken token = tokenService.consumeInvite(tokenId);
    updatePassword(token.getKeycloakId(), password);
    // ← NOVO: atualizar perfil com nome obrigatório
    keycloakAdminClient.updateUserProfile(token.getKeycloakId(), firstName, lastName);
    keycloakAdminClient.setUserEnabled(token.getKeycloakId(), true);
    keycloakAdminClient.clearRequiredActions(token.getKeycloakId());
    eventPublisher.publishEvent(new IdentityUserInviteActivatedEvent(token.getKeycloakId()));
    audit(token, "USER_INVITE_ACTIVATED");
    return new AuthMessageResponse(ACTIVATE_MESSAGE);
}
```

**Correção Backend — `KeycloakAdminClient`:**

Adicionar método `updateUserProfile(String keycloakId, String firstName, String lastName)`:
```java
public void updateUserProfile(String keycloakId, String firstName, String lastName) {
    String accessToken = adminAccessToken();
    restClient.put()
        .uri(userByIdEndpoint(keycloakId))
        .headers(h -> h.setBearerAuth(accessToken))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("firstName", firstName, "lastName", lastName))
        .retrieve()
        .toBodilessEntity();
    log.debug("updateUserProfile: keycloakId='{}' firstName='{}' lastName='{}'",
              keycloakId, firstName, lastName);
}
```

**Correção Backend — DTO de ativação:**

Se o endpoint recebe JSON, atualizar `AuthActivateRequest` (criar se não existir):
```java
public record AuthActivateRequest(
    @NotBlank String password,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}
```

**Correção Frontend — `LoginScreen.tsx`:**

Distinguir "truly blocked" vs "profile incomplete":
```typescript
// Adicionar caso para Keycloak "Account is not fully set up":
"account_not_fully_set_up": "Seu perfil está incompleto. Acesse o link de convite novamente para finalizar o cadastro.",
// Manter "blocked" apenas para contas realmente desativadas pelo admin.
```

**Correção Frontend — `InviteUserDrawer.tsx`:**

A tela de convite coleta nome (`userName`) — garantir que **primeiro nome e sobrenome** sejam campos separados e obrigatórios, não um campo único que pode vir com palavra única. Validar `!name.includes(" ")` antes de enviar.

**Validação obrigatória no convite:**
```tsx
const nameErr = !name.trim().includes(" ")
  ? "Informe nome e sobrenome (ex.: Alexandre Henrique)"
  : undefined;
```

**JaCoCo 100%** em `AuthActivationService.activate()` após mudança — mínimo 4 casos: ativação com nome completo ✓, ativação com firstName vazio → rejeita ✓, ativação com lastName vazio → rejeita ✓, perfil atualizado no Keycloak antes de habilitar ✓.

---

## Seção Z — Regras de Ouro

Regras invioláveis. O agente deve verificar cada commit contra esta lista.

1. **Nenhum ID hardcoded.** Qualquer `FORM_ID = "..."`, `EVENT_ID = "..."`, `tenant.id = "..."` fixo no código de produção é um bug imediato.

2. **Nenhum dado estático onde existe endpoint.** Se o backend expõe um `GET /.../audit-events/{id}`, a tela deve chamar esse endpoint — nunca simular o dado localmente.

3. **Toda chamada assíncrona tem try/catch visível.** Handlers de ação (`handleSubmit`, `handlePublish`, `handleSubmitForReview`) devem capturar erros e exibir toast com mensagem humanizada.

4. **Imagens em `<img src={...}>` precisam de autenticação resolvida.** Nunca inserir uma URL de endpoint protegido diretamente em `src` de `<img>`. Usar blob URL autenticada ou endpoint de preview não-auth.

5. **Todo recurso carregado em picker deve tratar erro com mensagem específica.** `PartialErrorWidget` genérico é proibido em modais — usar mensagem contextual com botão "Tentar novamente".

6. **Toda seleção persistida deve ser hidratável após reload.** Se um bloco guarda apenas IDs (ex.: `selectedEventIds`), o renderer deve ser capaz de re-buscar os dados completos ao montar.

7. **Nenhum dado de seed britânico ("Maestro Beton", "Loki", "WikiDev") em defaults de estado.** Qualquer `placeholder` ou `default` de campo que mencione produto específico deve ser substituído por vazio ou genérico (`"Seu produto"`, `""`).

8. **Botões de ação de workflow devem ser condicionais ao status atual.** "Enviar para revisão" só aparece em Draft; "Publicar" só em In Review; etc.

9. **Teardown Bruno sempre usa prefixo `TEST-`.** Qualquer script que cria tenant, produto ou usuário em testes deve nomear com `TEST-` para ser elegível à limpeza automática.

10. **JaCoCo 100%** em qualquer novo método de serviço backend introduzido nesta sprint (ex.: `AuditEventQueryService.getEvent`). Sem exceção.

---

## Ordem de Execução Recomendada

```
0. O.1 🚨 PRIORIDADE ALTÍSSIMA — InviteScreen + AuthActivationService + KeycloakAdminClient (nome/sobrenome obrigatórios na ativação)
1. K.1 (AssetPickerModal — raiz de múltiplos bugs)
2. K.2 (autenticação de imagens — raiz transversal)
3. G.3 (AuditEventDetail — mock total → API real)
4. H.1 (SubmissionTable — formId hardcoded)
5. H.2 + H.3 (FormBuilder nome + erro de publicação)
6. J.1 (hero image.src: null → normalização)
7. J.4 (submit for review — try/catch + contextual)
8. J.5 (cover image picker + thumbnail)
9. I.1 (event selecionado — hydration após reload)
10. I.2 + I.3 (imagem/mídia do evento no preview)
11. J.2 (vincular música — MusicPicker)
12. J.3 (EntityPicker — seed KG + fallback)
13. G.1 (Bruno teardown — verificar prefixos)
14. G.2 (Feedback drawer — viewport fix)
15. N.3 (CreateProductModal — campo PM obrigatório + backend CreateProductRequest)
16. N.1 (formatDateTime — ProductCard e ProductsList)
17. N.2 (DashboardGlobal — summaryKey refresh após criação de produto)
18. M.1 (UserTable — botão Remover + guards de hierarquia backend + email notificação)
19. N.4 (useModuleSelection — auto-switch para Custom + propagação de modulesList em handleProductCreated)
20. L.1 (verificação abrangente de fluxos)
```

---

## Resumo de Arquivos Afetados

### Frontend
| Arquivo | Bugs |
|---|---|
| `domains/audit/pages/AuditEventDetail.tsx` | G.3 |
| `domains/audit/services/auditService.ts` | G.3 |
| `domains/content/components/ContentArticleEditor.tsx` | J.2, J.5 |
| `domains/content/components/MusicPicker.tsx` | J.2 (novo) |
| `domains/content/pages/ContentEditor.tsx` | J.4 |
| `domains/content/services/contentService.ts` | J.3 |
| `domains/forms/pages/FormBuilder.tsx` | H.2, H.3 |
| `domains/forms/pages/SubmissionTable.tsx` | H.1 |
| `domains/forms/services/formsService.ts` | H.2 |
| `domains/knowledge/services/knowledgeService.ts` | J.3 |
| `domains/pages/components/BlockRenderer.tsx` | I.1, I.3, K.2 |
| `domains/pages/components/EditorPanels.tsx` | J.1 |
| `domains/pages/components/EventsManagerDrawer.tsx` | I.2 |
| `shared/components/MediaField.tsx` | J.5 |
| `shared/components/ui/drawer.tsx` | G.2 |
| `shared/utils/resolveAssetSrc.ts` | I.2, K.2 |
| `domains/assets/components/AssetPickerModal.tsx` | K.1, J.5 |
| `domains/assets/services/assetsService.ts` | K.1 |
| `domains/feedback/components/FeedbackDetailDrawer.tsx` | G.2 (verificar) |
| `domains/knowledge/components/EntityPicker.tsx` | J.3 |

### Bruno
| Arquivo | Bug |
|---|---|
| `bruno/01-tenants/*.bru` e similares | G.1 (verificar prefixo TEST-) |
| `bruno/99-teardown/02-limpar-tenants-de-teste.bru` | G.1 (integrar a CI) |

### Backend
| Arquivo | Bug |
|---|---|
| `audit/controller/AuditEventController.java` | G.3 (endpoint já existe — verificar) |
| `audit/service/AuditEventQueryService.java` | G.3 (getEvent — verificar) |
| `form/service/FormService.java` | H.3 (códigos de erro) |
| `content/service/ContentService.java` ou transition | J.4 (código invalid_transition) |
| `asset/controller/AssetController.java` | K.2 (endpoint /preview) |
| `pages/domain/BlockDefaults.java` | J.1.2 (hero defaults completos) |
