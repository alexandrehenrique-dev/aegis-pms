# Etapa 26 — Domínio `feedback` (reportar problema)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 06 (Tenant/Product), 11 (`asset`, para o anexo) e 23 (`notification`, opcional — ver Seção E). Adicionada **depois** do checklist final (etapa 22) e das etapas 23/24, mesmo padrão já usado para elas.

## Contexto fixo

`FeedbackModal.tsx` ("Reportar problema", acessível pelo menu de usuário e pela Central de Ajuda) é hoje 100% decorativo: `handleSubmit` só gera um ID fake (`AGS-####`) em memória e mostra um toast — nenhum POST acontece. O botão "Anexar arquivo" também é fake (`fakeFile`, um nome de string simulado, nunca um arquivo real do sistema do usuário). Nenhuma etapa anterior documentou este domínio — não existe `feedback` em nenhuma etapa nem no trace report até esta revisão.

## Objetivo

Persistir o feedback/bug-report submetido pelo usuário, com contexto automático (tela, usuário, tenant, produto) e anexo opcional — reaproveitando o domínio `asset` (etapa 11) para o upload do arquivo, não criando um mecanismo de upload paralelo.

## Tarefas

### A. Entidade

**Feedback**: `id`, `tenantId`, `productId?` (nulável — feedback pode ser reportado fora do contexto de um produto, ex. na tela de seleção de tenant), `createdBySubject`, `category` (`"Bug"|"UX confusa"|"Erro visual"|"Permissão incorreta"|"Informação errada"|"Sugestão"`), `priority` (`"baixa"|"média"|"alta"|"crítica"`), `description`, `screenName?`, `attachmentAssetId?` (referência a um `Asset` da etapa 11, **não** um upload próprio), `status` (`"aberto"|"em_analise"|"resolvido"`, default `"aberto"`), `createdAt`, `updatedAt`.

### B. Fluxo de anexo — reaproveita o upload de assets, não inventa um novo

> Correção do bug relatado: o botão "Anexar arquivo" não abre nenhum seletor de arquivo do sistema — só alterna um nome fake em memória. A causa não é só de UI: também não havia endpoint nenhum documentado para receber o arquivo. A solução **não** é criar um endpoint de upload próprio para feedback — é reaproveitar `POST /api/v1/products/{productId}/assets` (etapa 11), exatamente como qualquer outro domínio que referencia um asset (`download`, `audio`, `video` da etapa 21).

Fluxo (2 chamadas, nunca uma só): (1) frontend faz upload do arquivo escolhido via `<input type="file">` real (correção de UI, ver Sprint 18) para `POST /products/{productId}/assets` — se o feedback for reportado fora do contexto de um produto (`productId` nulo), usar um produto "técnico"/interno reservado para isso (decisão de implementação do GPT, documentar a escolha — ex. um produto de sistema do tenant BYOP) ou relaxar a etapa 11 para aceitar upload sem `productId` quando vier de feedback (decisão alternativa, também documentar); (2) frontend recebe o `assetId` criado e manda no `POST /feedback` como `attachmentAssetId`.

### C. Endpoints

```txt
POST /api/v1/feedback
GET  /api/v1/feedback                      (Super Admin — todos os tenants)
GET  /api/v1/tenants/{tenantId}/feedback    (Super Admin ou Tenant Admin do próprio tenant)
PUT  /api/v1/feedback/{feedbackId}/status   (Super Admin — body: { status })
```

Payloads:

```ts
type CreateFeedbackRequest = {
  productId?: string; category: string; priority: string; description: string;
  screenName?: string; attachmentAssetId?: string;
};
// POST /feedback → Feedback criado, com um id legível tipo "AGS-1234" (não o UUID interno —
// mesma convenção que o frontend mock já usa, só que persistido de verdade agora)

type FeedbackSummary = {
  id: string; category: string; priority: string; description: string; status: string;
  createdBySubject: string; tenantId: string; productId?: string; createdAt: string;
  attachmentAssetId?: string;
};
// GET /feedback, GET /tenants/{tenantId}/feedback → FeedbackSummary[]
```

### D. Regras de negócio

- `POST /feedback` aceita qualquer papel autenticado — reportar problema não é uma ação restrita por permissão (qualquer usuário pode reportar um bug).
- `GET /feedback` (todos os tenants) é só `SUPER_ADMIN`; `GET /tenants/{tenantId}/feedback` aceita `SUPER_ADMIN` ou `TENANT_ADMIN` do próprio tenant — qualquer outro papel/tenant alheio recebe 403/404 conforme a regra de isolamento padrão (Seção 10 do padrão de qualidade).
- `attachmentAssetId`, se informado, precisa existir e pertencer ao mesmo `tenantId` do feedback — mesma regra de isolamento já usada para qualquer referência de asset em outro domínio.
- `id` legível (`AGS-####`) é gerado pelo backend na criação — sequencial ou aleatório de 4 dígitos (decisão de implementação, documentar a escolha); nunca o `UUID` interno é exposto como identificador principal na resposta de criação (mantém a expectativa visual que o frontend mock já criou).
- **(Opcional, ver Seção E)** Ao criar um feedback com `priority: "crítica"`, considerar notificar o(s) `SUPER_ADMIN` via o domínio `notification` (etapa 23) — registrar como melhoria futura se não for feito nesta etapa, não bloquear a entrega por isso.

### E. Integração opcional com `notification` (etapa 23)

Se a etapa 23 já estiver implementada: ao criar um `Feedback` com `priority: "crítica"`, chamar `NotificationService.create(...)` com `target: { type: "USERS", userIds: [...super admins...] }` (resolver a lista de Super Admins via `TenantMembership`), `type: "WARNING"`, `presentationMode: "BELL_ONLY"`. Se a etapa 23 ainda não existir quando esta etapa for executada, pular esta seção — é um retrofit a fazer depois, não bloqueia a entrega do domínio `feedback`.

## Critérios de aceite

- [ ] `POST /feedback` funciona para qualquer papel autenticado e persiste o registro.
- [ ] Resposta de criação devolve um `id` legível (`AGS-####`), não o UUID interno.
- [ ] `attachmentAssetId` válido (mesmo tenant) é aceito; de outro tenant é rejeitado.
- [ ] `GET /feedback` (todos os tenants) só funciona para `SUPER_ADMIN`.
- [ ] `GET /tenants/{tenantId}/feedback` funciona para `SUPER_ADMIN`/`TENANT_ADMIN` do próprio tenant; tenant alheio retorna 404/403 conforme a regra de isolamento.
- [ ] `PUT .../status` só funciona para `SUPER_ADMIN`.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).

### F. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `FeedbackRepository`. Mapper via MapStruct (`FeedbackMapper`). 100% de cobertura nas classes funcionais.
- Entregar em rodadas:
  1. `Feedback` (entity) + `FeedbackRepository` + testes `@DataJpaTest`.
  2. `FeedbackMapper` (MapStruct) + testes de mapper.
  3. `FeedbackService` (regras da Seção D, geração do `id` legível, validação de `attachmentAssetId` cross-tenant) + testes com mocks.
  4. `FeedbackController` (endpoints da Seção C) + testes `@WebMvcTest` (incluindo 403 para papel sem permissão de listagem) + validação via collection Postman (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11).

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11).

```bash
curl -X POST http://localhost:8080/api/v1/products/<productId>/assets \
  -H "Authorization: Bearer $TOKEN" -F "file=@./screenshot.png" -F "friendlyName=Screenshot do bug"
# anota o assetId retornado

curl -X POST http://localhost:8080/api/v1/feedback \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"productId":"<productId>","category":"Bug","priority":"alta","description":"Botão X não responde ao clicar.","screenName":"/content/list","attachmentAssetId":"<assetId>"}'

curl -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" http://localhost:8080/api/v1/feedback
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio feedback com anexo reaproveitando upload de assets"
```
