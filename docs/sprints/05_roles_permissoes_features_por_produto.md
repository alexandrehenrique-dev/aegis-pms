# Sprint 05 — Roles, permissões e features atribuíveis por produto

> Pré-requisito: Sprint 02 (backend com modelo core) e Sprint 04 (integração) recomendadas, mas a modelagem desta sprint pode começar em paralelo.

## Contexto

Dois sistemas de permissão coexistem hoje, sem estarem conectados:

1. **RBAC de papel** (já existe, mockado): `viewAsRole` com 5 personas (`super_admin`, `tenant_admin`, `product_manager`, `editor`, `viewer`), simulador de papéis (Sprint 13 do Figma Make), `canCreate`, `canSeeUsers`, `canSeeFinancial` calculados em `App.tsx`.
2. **Catálogo de módulos por produto** (já existe, mockado): o array `modules` em `App.tsx` já modela cada módulo (Conteúdo, Assets, Forms, Analytics, SEO, **Knowledge Graph**, Workflow, Versionamento, Auditoria, Integrações) com um estado (`"habilitado" | "desabilitado" | "dependência" | "futuro" | "sem permissão"`), dependências entre módulos, e tier (`MVP`/`V1`/`Futuro`). A tela `modules` ("Catálogo") já existe na UI.

O que falta não é inventar o conceito — é **persistir e ligar os dois**: hoje é um array estático em memória, sem backend, sem ligação real com papel do usuário logado. É exatamente isso que o usuário descreve ao dizer "Knowledge Graph é uma funcionalidade que posso atribuir a um produto, posso modificar um produto, posso remover essa funcionalidade do produto".

## Objetivo

Modelar `ProductModule` como entidade real (produto ↔ módulo do catálogo, com estado habilitado/desabilitado e regras de dependência), e gatear toggle dessa atribuição por permissão de papel.

## Tarefas

### A. Backend

- Nova entidade `ProductModule` (ou `ProductFeature`): `productId`, `moduleKey` (referência ao catálogo fixo de módulos: content, assets, forms, analytics, seo, knowledge, workflow, versioning, audit, integrations), `enabled: boolean`, `enabledAt`, `enabledBy`, respeitando o Artigo X da Constituição (quem/quando criou/alterou).
- Regras de dependência: replicar a coluna "Dependências" já existente na UI (`modules` array: ex. Knowledge Graph depende de "Conteúdo + Assets") como validação de backend — não permitir habilitar um módulo sem suas dependências habilitadas.
- Endpoints mínimos: `GET /api/v1/products/{id}/modules`, `PATCH /api/v1/products/{id}/modules/{moduleKey}` (toggle habilitado/desabilitado).
- Apenas papéis com permissão (`tenant_admin`, `product_manager` — não `editor`/`viewer`) podem chamar o `PATCH`.

### B. Frontend

- Ligar a tela `modules` (Catálogo) aos endpoints reais da seção A em vez do array estático.
- Toggle de módulo desabilitado (cinza/bloqueado) quando o usuário simulado (`viewAsRole`) não tem permissão — reaproveitar `PermissionHint`/`Lock` já existentes na UI.
- Ao desabilitar um módulo que tem dependentes habilitados (ex.: desabilitar "Conteúdo" com Knowledge Graph habilitado), mostrar aviso e bloquear, ou oferecer desabilitar em cascata — decisão de produto a confirmar com o usuário antes de implementar a UI desse caso de borda.

### C. RBAC formal

- Formalizar o RBAC do simulador (`viewAsRole`, `canCreate`, `canSeeUsers`, `canSeeFinancial`) como Role → Permission → Capability real (conforme `implementation/011`, seção "PERMISSÕES": "UI nunca decide permissão. Permissões vêm do Core.").
- O simulador de papéis (ferramenta de QA/demo) deve continuar existindo, mas passa a refletir permissões reais vindas do backend via `/api/v1/me`, não mais um estado local arbitrário.

## Critérios de aceite

- [ ] Knowledge Graph (e qualquer outro módulo do catálogo) pode ser atribuído ou removido de um produto específico via UI, persistindo no backend.
- [ ] Dependências entre módulos são validadas no backend, não só sugeridas na UI.
- [ ] Apenas papéis com permissão conseguem alterar módulos de um produto; demais papéis veem o catálogo somente leitura.
- [ ] `/api/v1/me` retorna permissões reais que alimentam o simulador de papéis em vez de um estado local solto.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/05-roles-permissoes-features
git commit -m "feat(backend): adiciona entidade ProductModule com regras de dependencia"
git commit -m "feat(backend): endpoints de toggle de modulo por produto com gate de permissao"
git commit -m "feat(frontend): liga catalogo de modulos ao backend real"
git commit -m "refactor(permissions): formaliza rbac com permissoes vindas de /api/v1/me"
git push -u origin sprint/05-roles-permissoes-features
git checkout develop && git merge --no-ff sprint/05-roles-permissoes-features && git push origin develop
```
