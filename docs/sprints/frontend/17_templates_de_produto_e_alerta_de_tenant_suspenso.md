# Sprint 17 — Templates de produto (esqueleto de páginas) e alerta de tenant suspenso

> Pré-requisito: nenhum específico de outra sprint do frontend — mas reaproveita `pagesService`/`DEFAULT_BLOCK_CONTENT` (domínio `pages`, já em produção) e `notificationsService` (Sprint 14, já em produção). Companion desta sprint no backend: `docs/sprints/sprint-02-fundacao-backend-gpt/24_templates_de_produto_e_seed_de_esqueleto.md`.

## Contexto

Uma auditoria de jornadas (`docs/implementation/005_aegis_pms_user_journeys.md`) comparada ao código encontrou dois pontos da visão original do produto nunca implementados:

1. **"Escolher Template" (Journey 02, Tenant Admin) nunca existiu de fato.** `CreateProductForm.tsx:77` tem `<SelectLike label="Template inicial" value="Produto operacional padrão" locked />` — campo travado, sem opções. `CreateProductForm.tsx:49` e `CreateProductModal.tsx:45` mandam `template: "Produto operacional padrão"` hardcoded para `productsService.create`. O que de fato varia (`type`, via `useModuleSelection`) já pré-marca módulos (`core/products/moduleDefaults.ts`), mas nunca gera nenhuma página.
2. **Suspender/reativar um tenant é silencioso** (Journey 01, Super Admin). `tenantsService.update` (`core/tenants/services/tenantsService.ts:34-42`) só sobrescreve `tenant.status`, sem avisar ninguém — apesar do sistema de notificações (Sprint 14) já suportar destinatário `"tenant"`.

ADR-0017 formaliza a solução do primeiro ponto: `type` (não um campo "template" separado) passa a gerar automaticamente um esqueleto de páginas — reaproveitando os 6 contratos de produto reais já levantados (Sprint 11) — para os tipos cujo módulo `Páginas` é padrão (Site Institucional, Portal, Portfolio); os demais tipos só ganham os módulos recomendados; e o novo tipo `Custom` não gera nada, nem página nem módulo.

## Objetivo

1. `type` escolhido na criação de produto gera o esqueleto de páginas certo (ou nenhum, para tipos sem `Páginas`/para `Custom`) — espelhando em mock exatamente o que a etapa 24 do backend vai fazer de verdade.
2. Remover o campo "Template inicial" (morto) e o campo `template` do contrato de criação de produto.
3. Suspender/reativar um tenant cria uma notificação para os usuários daquele tenant.

## Tarefas

### A. Catálogo de esqueleto (mock) — `core/products/productTemplates.ts` (novo arquivo)

Criar o catálogo, espelhando exatamente a tabela da ADR-0017 (mesmas páginas/seções/labels que `frontend/src/domains/pages/mocks/pages.mocks.ts` já usa para Maestro Beton/CMSS/Conecta Talentos/Alexandre Dev — não inventar uma estrutura nova):

```ts
export type PageSkeletonSection = { type: BlockType; label: string };
export type PageSkeleton = { slug: string; title: string; sections: PageSkeletonSection[] };
export const PRODUCT_PAGE_SKELETONS: Partial<Record<ProductTypeKey, PageSkeleton[]>> = {
  "Site Institucional": [
    { slug: "home", title: "Home", sections: [{ type: "hero", label: "Hero" }, { type: "feature-grid", label: "Pilares" }, { type: "event-list", label: "Próximos eventos" }, { type: "cta-section", label: "CTA" }] },
    { slug: "quem-somos", title: "Quem Somos", sections: [{ type: "image-text", label: "Apresentação" }, { type: "two-column", label: "Missão e Valores" }] },
    { slug: "historia", title: "História", sections: [{ type: "timeline", label: "Linha do tempo" }] },
    { slug: "agenda", title: "Agenda", sections: [{ type: "event-list", label: "Agenda completa" }] },
    { slug: "galeria", title: "Galeria", sections: [{ type: "gallery", label: "Galeria de fotos" }] },
    { slug: "apoie", title: "Apoie", sections: [{ type: "rich-text", label: "Como apoiar" }, { type: "faq", label: "Perguntas frequentes" }] },
    { slug: "contato", title: "Contato", sections: [{ type: "contact", label: "Fale com a gente" }] },
  ],
  Portal: [
    { slug: "home", title: "Home", sections: [{ type: "hero", label: "Hero" }, { type: "text", label: "Quem Somos" }, { type: "card-list", label: "Vagas" }, { type: "card-list", label: "Blog" }] },
  ],
  Portfolio: [
    { slug: "home", title: "Home", sections: [{ type: "hero", label: "Hero" }, { type: "card-list", label: "Projetos" }, { type: "feature-grid", label: "Skills" }, { type: "timeline", label: "Experiência" }, { type: "download", label: "Downloads" }] },
  ],
  // "Knowledge Base", "Library/Books/Music", "Produto SaaS", "Custom": sem entrada — sem esqueleto de páginas.
};
```

### B. `productsService.create` gera o esqueleto

Em `domains/products/services/productsService.ts`, `create(req)` (linha ~89-104): depois de criar o `ProductSummary`, se `PRODUCT_PAGE_SKELETONS[req.type]` existir, para cada `PageSkeleton`: chamar `pagesService.createPage(created.id, { slug, title, locale: "pt-BR" })`, depois `pagesService.createSection(created.id, page.id, { type, label, content: DEFAULT_BLOCK_CONTENT[type] })` para cada seção, **na ordem declarada** — `DEFAULT_BLOCK_CONTENT` já existente (`domains/pages/blockDefaults.ts`) garante automaticamente que `gallery`/`download`/`audio`/`image`/`image-text` nascem com listas/referências vazias, sem nenhuma lógica especial por bloco. `type: "Custom"` (e os demais tipos sem entrada no catálogo) não chama nada disso — produto fica exatamente como hoje.

### C. Remover o campo morto e o conceito de `template`

1. `CreateProductForm.tsx:77`: remover `<SelectLike label="Template inicial" .../>`.
2. `CreateProductForm.tsx:49` e `CreateProductModal.tsx:45`: remover `template: "Produto operacional padrão"` da chamada a `productsService.create`.
3. `domains/products/contracts/requests.ts:11` (`CreateProductRequest.template: string`): remover o campo do contrato.
4. `core/products/moduleDefaults.ts`: adicionar `"Custom"` a `ProductTypeKey`/`PRODUCT_TYPES`.
5. No lugar do campo removido (`CreateProductForm.tsx`, mesma posição), adicionar um preview somente leitura: se `PRODUCT_PAGE_SKELETONS[type]` existir, listar as páginas que serão criadas ("Este produto nasce com N páginas: Home, Quem Somos..."); se `type === "Custom"` ou sem esqueleto, mostrar "Este produto nasce em branco — nenhuma página, nenhum módulo pré-marcado, mas você pode habilitar o que quiser abaixo" (para `Custom`) ou "Este produto nasce sem páginas — módulos recomendados já vêm pré-marcados" (Knowledge Base/Library/SaaS).

### C.1 Correção de bug — `Custom` não mostra nenhum módulo para habilitar

> Encontrado em teste manual (independente do backend estar de pé ou não — é um bug 100% do mock/frontend): com a Tarefa C.4 como descrita originalmente (`"Custom"` sem entrada em `PRODUCT_TYPE_MODULE_DEFAULTS`), `useModuleSelection` (`domains/products/hooks/useModuleSelection.ts:17`, `moduleOptions = PRODUCT_TYPE_MODULE_DEFAULTS[type] ?? []`) resolve `moduleOptions` para um array vazio — `ModuleCheckboxList` não renderiza **nenhum** checkbox, então não há como o Tenant Admin habilitar módulo nenhum durante a criação de um produto `Custom`, nem manualmente. Isso não é o comportamento desejado: "produto em branco" deve significar **nenhum módulo pré-marcado**, não "nenhum módulo disponível para escolher".

Correção: `PRODUCT_TYPE_MODULE_DEFAULTS["Custom"]` recebe uma entrada com **todos** os módulos do catálogo (mesma lista de `ALL_MODULE_KEYS`, `core/products/moduleDefaults.ts:129`), cada um com `default: false`:

```ts
Custom: ALL_MODULE_KEYS.map((key) => ({ key, default: false })),
```

Resultado: `ModuleCheckboxList` mostra o catálogo completo de módulos para `Custom`, todos desmarcados — o Tenant Admin pode marcar manualmente o que quiser ainda na tela de criação (em vez de precisar ir à tela de Catálogo de Módulos depois), e se não marcar nada, o produto nasce de fato sem módulo nenhum habilitado — preservando a intenção original da ADR-0017 sem deixar a UI sem opção nenhuma. Atualizar a `Tarefa C.4` acima para refletir isso (a versão "sem entrada no Record" estava errada).

### D. Notificação ao suspender/reativar tenant

Em `core/tenants/services/tenantsService.ts`, `update(id, req)` (linha ~34-42): capturar `previousStatus = tenant.status` **antes** de sobrescrever; depois de aplicar o patch, se `previousStatus !== req.status`, chamar `notificationsService.create({ type: req.status === "suspenso" ? "WARNING" : "GENERAL", title: req.status === "suspenso" ? "Tenant suspenso" : "Tenant reativado", bodyMarkdown: <texto padrão, ver Seção D da etapa 24 do backend>, presentationMode: "BELL_ONLY", recipients: { mode: "tenant", tenantId: id } })`. `update` sem mudança de status não chama nada.

## Critérios de aceite

- [ ] Criar produto `"Site Institucional"` gera as 7 páginas do esqueleto (verificável em `/products/{id}/pages` no mock), cada seção com o `BlockType` certo e conteúdo vazio.
- [ ] Criar produto `"Portal"`/`"Portfolio"` gera a página Home com as seções certas.
- [ ] Criar produto `"Knowledge Base"`/`"Library/Books/Music"`/`"Produto SaaS"` não gera nenhuma página.
- [ ] Criar produto `"Custom"` não gera nenhuma página e não pré-marca nenhum módulo.
- [ ] Selecionar `type: "Custom"` na criação de produto mostra a lista completa de módulos no `ModuleCheckboxList`, todos desmarcados — nunca uma lista vazia (bug corrigido na Tarefa C.1). Marcar manualmente um módulo e confirmar a criação habilita só esse módulo.
- [ ] Campo "Template inicial" não existe mais em nenhuma tela; `CreateProductRequest` não tem mais o campo `template`.
- [ ] Preview do esqueleto aparece corretamente para cada `type`, incluindo o texto específico de `Custom`.
- [ ] Suspender um tenant cria uma notificação `WARNING`/`BELL_ONLY` para os usuários daquele tenant; reativar cria uma `GENERAL` equivalente; salvar sem mudar o status não cria nada.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/17-templates-de-produto

git commit -m "feat(products): catalogo de esqueleto de paginas por tipo de produto"
git commit -m "feat(products): create gera paginas/secoes do esqueleto via pagesService"
git commit -m "refactor(products): remove campo template morto, adiciona tipo Custom"
git commit -m "fix(products): custom mostra catalogo completo de modulos desmarcados em vez de lista vazia"
git commit -m "feat(products): preview do esqueleto na tela de criacao de produto"
git commit -m "feat(tenants): notificacao automatica ao suspender/reativar tenant"

git push -u origin sprint/17-templates-de-produto
```

Ao final, finalize a sprint no gitflow:

```bash
./scripts/gitflow-finish-sprint.sh sprint/17-templates-de-produto
```
