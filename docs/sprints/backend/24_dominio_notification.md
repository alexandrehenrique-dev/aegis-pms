# Etapa 24 — Domínio `notification` (onboarding + notificações direcionadas)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 07 (Tenant/Product/Membership), 10 (ProductAssignment) e 15 (Users) concluídas — esta etapa precisa resolver "todos os usuários", "usuários de um tenant" e "usuários específicos" como destinatários. Etapa adicionada pela Sprint 14 do frontend (`docs/sprints/14_onboarding_real_e_sistema_de_notificacoes.md`), depois da fundação e dos domínios de produto já estarem todos especificados — por isso entra numerada depois da etapa 23 (checklist final), mesmo padrão já usado para a etapa 22 (`pages`, adicionada pela Sprint 11).

## Contexto fixo

O frontend tinha dois mecanismos fracos que a Sprint 14 unificou num só: um "modal de boas-vindas" controlado por `localStorage` do navegador (não por usuário, não por backend) e um sino de notificações lendo de um mock estático sem tipo nem estado de leitura real. A decisão de modelo (já tomada, não é uma pergunta aberta): **onboarding é só mais um tipo de notificação**, não um mecanismo separado — toda notificação (onboarding inclusive) é uma `Notification` com **fan-out** para uma ou mais `UserNotificationStatus` (uma linha por destinatário), e o mesmo mecanismo de "aparece como modal a primeira vez, depois só no sino" vale para qualquer tipo.

## Objetivo

CRUD de notificações pelo Super Admin, com fan-out para os destinatários corretos no momento da criação, e os endpoints que o usuário comum usa para saber o que está pendente, marcar como mostrado/lido, e listar o histórico.

## Tarefas

### A. Entidades

**Notification**: `id`, `type` (`"ONBOARDING"|"FEATURE"|"WARNING"|"MAINTENANCE"|"GENERAL"`), `title`, `bodyMarkdown` (texto markdown — mesma convenção/regra de sanitização das etapas 11 e 22: allowlist `p, strong, em, ul, ol, li, blockquote, h2, h3, a, br, span (span só com atributo class, e só um dos 6 valores fixos de cor da Sprint 18 — nunca style nem qualquer outro atributo)`, bloquear `javascript:`/`data:`, nunca `<script>`/`<iframe>`), `presentationMode` (`"MODAL_ONCE"|"BELL_ONLY"`), `createdBySubject`, `createdAt`, `updatedAt`.

**UserNotificationStatus**: `id`, `notificationId`, `userSubject`, `autoShown` (boolean, default `false`), `read` (boolean, default `false`), `readAt?`, `shownAt?`, `createdAt`. Constraint única em `(notificationId, userSubject)` — um destinatário nunca tem duas linhas de status para a mesma notificação.

> Decisão de schema: `UserNotificationStatus` é materializada (uma linha por destinatário resolvido) no momento da criação da notificação, não calculada em tempo de leitura a partir de um critério de destinatário guardado solto (ex.: "todos do tenant X") — isso significa que adicionar um usuário novo a um tenant **depois** de uma notificação `"Usuários de um tenant"` já ter sido criada **não** retroage automaticamente para esse usuário novo. É uma limitação aceita deliberadamente nesta fase (mais simples e mais previsível de consultar) — se isso precisar mudar, é uma decisão de produto nova, não desta etapa.

### B. Endpoints

```txt
GET  /api/v1/notifications/mine
GET  /api/v1/notifications/mine/pending-modal
POST /api/v1/notifications/{notificationId}/mark-shown
POST /api/v1/notifications/{notificationId}/mark-read
POST /api/v1/notifications                                  (Super Admin)
GET  /api/v1/notifications                                  (Super Admin — gestão/auditoria)
```

Payloads:

```ts
type NotificationWithStatus = {
  id: string; type: "ONBOARDING" | "FEATURE" | "WARNING" | "MAINTENANCE" | "GENERAL";
  title: string; bodyMarkdown: string; presentationMode: "MODAL_ONCE" | "BELL_ONLY";
  createdAt: string; autoShown: boolean; read: boolean; readAt?: string;
};
// GET /notifications/mine → NotificationWithStatus[] (ordenado por createdAt desc)
// GET /notifications/mine/pending-modal → NotificationWithStatus | 204 No Content
//   (a mais antiga com autoShown=false e presentationMode="MODAL_ONCE"; se não houver nenhuma, 204)

type CreateNotificationRequest = {
  type: "ONBOARDING" | "FEATURE" | "WARNING" | "MAINTENANCE" | "GENERAL";
  title: string; bodyMarkdown: string; presentationMode: "MODAL_ONCE" | "BELL_ONLY";
  target:
    | { type: "ALL" }
    | { type: "TENANT"; tenantId: string }
    | { type: "USERS"; userIds: string[] };
};
// POST /notifications → Notification criada (sem o array de status — é interno)
```

### C. Regras de negócio

- Só `SUPER_ADMIN` pode criar notificação (`POST /notifications`) ou listar todas (`GET /notifications`) — qualquer outro papel recebe 403.
- Ao criar, resolver `target` e criar uma `UserNotificationStatus` (com `autoShown=false`, `read=false`) para cada destinatário resolvido:
  - `{ type: "ALL" }`: todo `userSubject` com pelo menos uma `TenantMembership` ativa em qualquer tenant (não duplicar se o usuário tiver membership em mais de um tenant — um destinatário, uma linha de status).
  - `{ type: "TENANT", tenantId }`: todo `userSubject` com `TenantMembership` ativa naquele tenant.
  - `{ type: "USERS", userIds }`: exatamente os subjects informados — validar que cada um existe (404/400 se algum `userId` não existir, antes de criar qualquer status, para não fazer fan-out parcial).
- `GET /notifications/mine/pending-modal` só considera notificações com `presentationMode: "MODAL_ONCE"` — uma notificação `"BELL_ONLY"` nunca aparece aqui, mesmo que `autoShown=false` (ela simplesmente nunca é "mostrada automaticamente", só existe para `GET /mine`).
- `mark-shown` é idempotente (chamar duas vezes não dá erro, só garante `autoShown=true`); o mesmo vale para `mark-read`.
- `mark-read` **não exige** que `autoShown` já seja `true` — o usuário pode marcar como lida uma notificação que nunca foi mostrada automaticamente (ex.: leu direto pelo sino antes do gate automático rodar, em alguma race condition de UI).
- Onboarding (`type: "ONBOARDING"`) é criado **uma vez** (seed, etapa 21 — ver Seção E) e seu fan-out para um usuário novo acontece automaticamente no momento da criação do usuário (etapa 15, fluxo de convite) — não é o Super Admin que cria a notificação de onboarding manualmente a cada novo usuário.
- **Isolamento por usuário** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): `mark-shown`/`mark-read` e `GET /notifications/mine*` operam **sempre** sobre o `UserNotificationStatus` do próprio chamador (`userSubject` resolvido do token, nunca de um `userId` no body/path) — não existe forma de um usuário marcar como lida/vista a notificação de outro usuário. Se o `notificationId` informado não tiver `UserNotificationStatus` para o chamador, retorna 404 (recurso não existe **para ele**, mesmo que exista para outros destinatários).

### D. Sanitização de markdown

Mesma regra das etapas 11 e 22 — `bodyMarkdown` passa pela mesma função de sanitização (reaproveitar a implementação já existente, não duplicar) antes de persistir, em `POST /notifications`.

### E. Integração com a etapa 15 (convite de usuário) e com o seed (etapa 21)

1. Etapa 15 (`UserService`, fluxo de convite/criação de usuário): depois de criar a `TenantMembership` do novo usuário, chamar `NotificationService.assignOnboarding(userSubject)` — cria a `UserNotificationStatus` (`autoShown=false`, `read=false`) ligando o usuário novo à notificação `ONBOARDING` já existente (criada no seed/migration, não recriada a cada usuário).
2. Etapa 21 (seed): garantir que a notificação `ONBOARDING` exista (via migration Flyway, não `CommandLineRunner` — é dado técnico/fixo, não dado de demonstração) com um `bodyMarkdown` inicial razoável (pode ser o mesmo texto-guia já escrito no mock do frontend pela Sprint 14) e fazer o fan-out para os 5 usuários de teste já seedados.

## Critérios de aceite

- [ ] Criar notificação com `target: { type: "ALL" }` gera uma `UserNotificationStatus` para cada usuário com membership ativa, sem duplicar para quem está em mais de um tenant.
- [ ] Criar notificação com `target: { type: "USERS", userIds: [...] }` com um `userId` inexistente é rejeitada (400/404) e não cria nenhum status (sem fan-out parcial).
- [ ] `GET /notifications/mine/pending-modal` retorna 204 quando não há nada pendente, e a notificação correta (mais antiga, `MODAL_ONCE`, `autoShown=false`) quando há.
- [ ] `mark-shown`/`mark-read` são idempotentes.
- [ ] Notificação `BELL_ONLY` nunca aparece em `pending-modal`.
- [ ] Usuário não `SUPER_ADMIN` tentando `POST`/`GET /notifications` (admin) recebe 403.
- [ ] Usuário novo criado via convite (etapa 15) já nasce com `UserNotificationStatus` de onboarding pendente.
- [ ] `bodyMarkdown` passa pela mesma sanitização das etapas 11/22 (tag fora da allowlist é removida, `javascript:` é bloqueado).
- [ ] `mark-shown`/`mark-read` de um `notificationId` sem `UserNotificationStatus` para o chamador retorna 404 — usuário não consegue marcar notificação de outro usuário.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
# super admin cria notificacao para todos
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN_SUPER_ADMIN" -H "Content-Type: application/json" \
  -d '{"type":"FEATURE","title":"Novo módulo de páginas","bodyMarkdown":"Agora você pode **compor páginas** por seções.","presentationMode":"MODAL_ONCE","target":{"type":"ALL"}}'

# usuario comum consulta o que esta pendente
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/notifications/mine/pending-modal

curl -X POST http://localhost:8080/api/v1/notifications/<notificationId>/mark-shown \
  -H "Authorization: Bearer $TOKEN"

# esperado 204 agora, ja foi mostrada
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/notifications/mine/pending-modal

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/notifications/mine

# esperado 403: papel nao super admin criando notificacao
curl -i -X POST http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN_EDITOR" -H "Content-Type: application/json" \
  -d '{"type":"GENERAL","title":"x","bodyMarkdown":"x","presentationMode":"BELL_ONLY","target":{"type":"ALL"}}'
```

### Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `NotificationRepository`/`UserNotificationStatusRepository` (incluindo as queries de fan-out e de `pending-modal`). Mappers via MapStruct (`NotificationMapper`). 100% de cobertura nas classes funcionais, incluindo a lógica de resolução de destinatários (`ALL`/`TENANT`/`USERS`) e a sanitização de markdown reaproveitada.
- Entregar em rodadas:
  1. `Notification`, `UserNotificationStatus` (entities) + `NotificationRepository`, `UserNotificationStatusRepository` (com a constraint única e as queries de filtro) + testes `@DataJpaTest`.
  2. `NotificationMapper` (MapStruct) + testes de mapper.
  3. `NotificationService` (criação + fan-out por tipo de `target`, `pending-modal`, `mark-shown`/`mark-read`, integração com a etapa 15) + testes com mocks — cada tipo de `target` (`ALL`/`TENANT`/`USERS`) com teste próprio, incluindo o caso de `userId` inexistente.
  4. `NotificationController` (endpoints da Seção B) + testes `@WebMvcTest` (incluindo o 403 para papel não autorizado) + validação via `curl`.

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio notification com fan-out, onboarding integrado e endpoints de leitura/admin"
```
