# Etapa 25 — Templates de produto: esqueleto de páginas no `POST /products`

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 06 (Tenant/Product/Module), 21 (`pages`/`PageSection`) e 23 (`notification`) concluídas — esta etapa cria páginas/seções na criação do produto e dispara uma notificação na etapa de tenant. Adicionada **depois** do checklist final (etapa 22) e da etapa 23, mesmo padrão já usado para elas — surgiu depois, numerada por último.

## Contexto fixo

ADR-0017 formaliza o que a etapa 06 sempre deixou pendente: o `type` escolhido na criação de um produto (`Site Institucional`, `Portal`, `Knowledge Base`, `Portfolio`, `Library/Books/Music`, `Produto SaaS`, e o novo `Custom`) não é só um rótulo que pré-marca módulos — para os tipos cujo módulo `PAGES` é padrão, ele também gera automaticamente um **esqueleto de páginas e seções**, com conteúdo vazio (nunca dado de demonstração inventado). `Custom` é o oposto: produto nasce 100% em branco, nenhum módulo habilitado.

## Objetivo

`POST /products` passa a, depois de criar o `Product`: (1) pré-habilitar os módulos recomendados do `type` (respeitando a validação de dependência já existente, etapa 06), e (2) se o `type` tiver esqueleto de páginas definido, criar as `Page`/`PageSection` correspondentes com conteúdo default vazio por `BlockType`.

## Tarefas

### A. Catálogo de esqueleto por tipo (`ProductTemplateCatalog`)

Classe de configuração (não entidade — é um catálogo fixo, mesmo espírito do catálogo de `BlockType`/`ModuleKey`), mapeando `type` → lista de páginas, cada página com `slug`, `title`, e lista de seções (`type: BlockType`, `label`, `order`):

```txt
"Site Institucional":
  home: hero, feature-grid, event-list, cta-section
  quem-somos: image-text, two-column
  historia: timeline
  agenda: event-list
  galeria: gallery
  apoie: rich-text, faq
  contato: contact

"Portal":
  home: hero, text, card-list (label "Vagas"), card-list (label "Blog")

"Portfolio":
  home: hero, card-list (label "Projetos"), feature-grid (label "Skills"), timeline (label "Experiência"), download (label "Downloads")

"Knowledge Base", "Library/Books/Music", "Produto SaaS": sem páginas — só módulos (Seção C).

"Custom": sem páginas, sem módulos pré-habilitados.
```

Espelha exatamente `pagesByProduct` (Maestro Beton/CMSS para Site Institucional + Galeria, Conecta Talentos para Portal, Alexandre Dev para Portfolio) já documentado/mockado no frontend (`frontend/src/domains/pages/mocks/pages.mocks.ts`, ver ADR-0017) — não inventar uma estrutura diferente.

### B. Conteúdo default por `BlockType` (`BlockDefaults`)

A etapa 21 valida conteúdo por tipo de bloco (Seção C daquela etapa), mas não formaliza um conteúdo **default vazio** por tipo — esta etapa precisa disso para o scaffold. Criar `BlockDefaults` (classe utilitária, `Map<BlockType, JsonNode/Map<String,Object>>` ou equivalente), espelhando exatamente `frontend/src/domains/pages/blockDefaults.ts` (`DEFAULT_BLOCK_CONTENT`) — mesma forma vazia por tipo, ex.: `gallery: { items: [] }`, `event-list: { title: "Agenda" }` (sem `source` ainda), `download: { title: "Downloads", items: [] }`, `hero: { title: "" }` (ou o mínimo que passe a validação da etapa 21 sem ser confundido com dado real). Reaproveitada tanto pelo scaffold (Seção C) quanto por qualquer "adicionar bloco novo" manual no editor de páginas (mesma fonte de verdade, nunca duas listas de default divergentes entre frontend e backend).

### C. Hook em `POST /products`

1. Depois de criar o `Product` (etapa 06): para cada módulo recomendado do `type` (mesma lista de `core/products/moduleDefaults.ts`/`PRODUCT_TYPE_MODULE_DEFAULTS`, espelhada no backend), chamar `ProductModuleService.enable(productId, moduleKey)` **na ordem que resolve dependências primeiro** (ex.: habilitar `CONTENT` antes de `KNOWLEDGE_GRAPH` quando ambos forem recomendados, para a validação de dependência da etapa 06 não rejeitar a própria sequência de scaffold).
2. Se o `type` tiver esqueleto de páginas (Seção A): para cada página do catálogo, criar `Page` (`status: "draft"`) + `PageSection`s na ordem declarada, com `contentJson` = `BlockDefaults.defaultFor(type)` (Seção B) — nunca o conteúdo de demonstração de Maestro Beton/CMSS/Conecta Talentos/Alexandre Dev, que é fiel a esses produtos específicos, não a um produto novo qualquer.
3. `type: "Custom"`: não chama nem a Seção C.1 nem a C.2 — produto fica exatamente como `POST /products` já criava antes desta etapa (sem módulo nenhum habilitado, sem página nenhuma).
4. Toda a operação (criar produto + habilitar módulos + criar páginas) é uma única transação — se qualquer passo falhar, nada é persistido (nunca um produto "pela metade", com módulo habilitado mas sem a página correspondente, ou vice-versa).

### D. Notificação de tenant suspenso/reativado (retrofit na etapa 09)

> Auditoria de jornada (`docs/implementation/005_aegis_pms_user_journeys.md`, Journey 01) encontrou que mudar o status de um tenant (`PUT /tenants/{tenantId}`, etapa 09) é hoje silencioso — ninguém é avisado. Corrigido aqui, reaproveitando o domínio `notification` (etapa 23) já existente, sem mecanismo novo.

Em `TenantService.update(...)` (etapa 09): se o `status` mudou de `"ativo"` para `"suspenso"` (ou vice-versa), chamar `NotificationService.create(...)` com `target: { type: "TENANT", tenantId }`, `type: "WARNING"` (suspensão) ou `"GENERAL"` (reativação), `presentationMode: "BELL_ONLY"` (não interrompe o usuário com modal — é informativo, ver sino), e um `bodyMarkdown` padrão (ex.: "Este tenant foi suspenso pelo administrador da plataforma. Contate o suporte para mais informações." / "Este tenant foi reativado."). `createdBySubject` é o Super Admin que fez a alteração.

## Critérios de aceite

- [ ] Criar produto `"Site Institucional"` gera as 7 páginas do esqueleto (Seção A), cada seção com o `BlockType` certo e conteúdo vazio (`BlockDefaults`).
- [ ] `gallery`/`download`/`audio`/`image`/`image-text` nascem sempre com listas/referências vazias — nunca um asset inventado.
- [ ] Criar produto `"Knowledge Base"`/`"Library/Books/Music"`/`"Produto SaaS"` habilita os módulos recomendados, mas não cria nenhuma página.
- [ ] Criar produto `"Custom"` não habilita nenhum módulo nem cria nenhuma página — produto nasce vazio.
- [ ] Habilitar módulo com dependência (ex. `KNOWLEDGE_GRAPH`) durante o scaffold nunca é rejeitado pela validação da etapa 06 — a ordem de habilitação resolve a dependência primeiro.
- [ ] Falha em qualquer parte do scaffold (módulo ou página) não deixa o produto criado parcialmente — tudo ou nada.
- [ ] Suspender um tenant cria uma `Notification`/`UserNotificationStatus` (`BELL_ONLY`) para os usuários daquele tenant; reativar cria outra equivalente.

### E. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Esta etapa **não cria entidade nova** (reaproveita `Page`/`PageSection` da etapa 21, `ProductModule` da etapa 06, `Notification` da etapa 23) — as classes novas são `ProductTemplateCatalog`, `BlockDefaults` e a extensão de `ProductService`/`TenantService`. 100% de cobertura nas classes funcionais, incluindo cada tipo do catálogo (Seção A) com pelo menos um teste confirmando as páginas/módulos certos, e `"Custom"` com teste confirmando que nada é criado.
- Entregar em rodadas:
  1. `ProductTemplateCatalog` + `BlockDefaults` (classes de configuração, sem repository) + testes unitários puros (sem Spring context) confirmando o conteúdo de cada entrada.
  2. Extensão de `ProductService.create(...)` (scaffold de módulos + páginas, Seção C) + testes com mocks de `ProductModuleService`/`PageService` — cada `type` do catálogo com teste próprio, incluindo `"Custom"`.
  3. Extensão de `TenantService.update(...)` (notificação de status, Seção D) + teste confirmando a notificação criada com o `target`/`type` certos, e que `update` sem mudança de status não cria notificação nenhuma.
  4. Teste de integração (`@SpringBootTest`) de `POST /products` ponta a ponta para `"Site Institucional"` e `"Custom"`, confirmando o resultado via `GET /products/{id}/pages` e `GET /products/{id}` (módulos).

## Validação

> **Entrega via collection Bruno, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os como requests `.bru` na pasta numerada desta etapa em `bruno/` (collection cumulativa, autenticação herdada via header `Authorization: Bearer {{token}}` definido em `collection.bru`) e valide a collection inteira via `npx @usebruno/cli run --env local`.

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenant-id>","key":"novo-site","name":"Novo Site","type":"Site Institucional","defaultLocale":"pt-BR"}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/pages
# esperado: 7 páginas (home, quem-somos, historia, agenda, galeria, apoie, contato)

curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tenantId":"<tenant-id>","key":"produto-custom","name":"Produto Custom","type":"Custom","defaultLocale":"pt-BR"}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/pages
# esperado: []

curl -X PUT http://localhost:8080/api/v1/tenants/<tenantId> \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"BYOP","plan":"Pro","status":"suspenso"}'

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/notifications/mine
# esperado: inclui a notificação de suspensão para usuários do tenant
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): templates de produto com esqueleto de paginas e notificacao de tenant suspenso"
```
