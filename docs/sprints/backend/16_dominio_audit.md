# Etapa 16 — Domínio `audit` (trilha de auditoria)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 04 concluída (tabela `audit_events` já existe na migration inicial). Esta etapa expõe os endpoints de leitura e formaliza a gravação — várias etapas anteriores (09, 10, 11, 14) já devem ter gravado eventos aqui; se ainda não gravaram, volte e adicione antes de prosseguir.

## Contexto fixo

Telas `AuditTimeline` (tela cheia e modo `compact` embutido em `UserDetailPanel.tsx`) e `AuditEventDetail` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.6). A tabela `audit_events` já foi criada na migration da etapa 04 — esta etapa formaliza o modelo de gravação (um `AuditService` central, não cada domínio gravando do seu jeito) e os endpoints de leitura.

## Objetivo

Toda ação relevante do sistema (criar/editar/excluir tenant, atribuir produto, convidar usuário, publicar conteúdo, excluir asset, etc.) grava um evento de auditoria consultável.

## Tarefas

### A. Entidade e serviço central

**AuditEvent** (colunas mínimas, ajustar à tabela já existente da etapa 04): `id`, `tenantId`, `actorSubject`, `action`, `targetType`, `targetId`, `targetLabel`, `productId?`, `module?`, `risk` (`baixo`|`medio`|`alto`), `diffJson?` (antes/depois, quando aplicável), `traceId?`, `ip?`, `userAgent?`, `createdAt`.

Criar `AuditService.record(...)` único, injetado em todos os outros serviços que precisam auditar (tenant, product assignment, users, content publish, asset delete, etc.) — não duplicar lógica de gravação em cada domínio. Se as etapas anteriores (09, 10, 11, 14) já gravaram eventos de forma ad-hoc, refatorar para usar este serviço central agora.

### B. Endpoints

```txt
GET /api/v1/tenants/{tenantId}/audit-events
GET /api/v1/tenants/{tenantId}/audit-events/{eventId}
```

Payloads:

```ts
type AuditEvent = {
  actor: string; action: string; target: string;
  tenant: string; module: string; time: string; risk: string;
};
// GET /audit-events → AuditEvent[]
```

`GET /audit-events/{eventId}` retorna o mesmo shape acrescido de `diffJson`, `traceId`, `ip`, `userAgent` — campos técnicos que `AuditEventDetail.tsx` já prevê na UI mesmo que hoje estejam vazios no mock.

### C. Regras de negócio

- Auditoria é **somente leitura** pela API pública — nenhum endpoint de escrita direta; eventos só são criados pelos próprios serviços de domínio via `AuditService`.
- `risk` é calculado pela própria ação (ex.: excluir tenant = `alto`; editar configuração = `medio`; criar conteúdo = `baixo`) — manter uma tabela de mapeamento simples, não um cálculo complexo nesta fase.
- Listagem suporta filtro por `actorSubject`, `productId`, `module`, `risk` (mesmo filtro que `AuditTimeline.tsx` oferece na UI).
- **Isolamento por tenant** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): `tenantId` fora do escopo do usuário autenticado (sem membership ativa, exceto `SUPER_ADMIN`) retorna 404, nunca 403.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `AuditEventRepository` (incluindo os métodos de filtro). Mapper via MapStruct (`AuditEventMapper`). 100% de cobertura nas classes funcionais, incluindo a tabela de mapeamento de `risk` por ação.
- Entregar em rodadas:
  1. `AuditEvent` (entity, ajustada à tabela da etapa 04) + `AuditEventRepository` (com os filtros da Seção C) + testes `@DataJpaTest`.
  2. `AuditEventMapper` (MapStruct) + testes de mapper.
  3. `AuditService` (record central + cálculo de `risk`) + refatoração dos serviços das etapas 09/10/11/14 para usar este service + testes com mocks.
  4. `AuditController` (endpoints da Seção B) + testes `@WebMvcTest` (confirmando que não existe endpoint de escrita) + validação via `curl`.

## Critérios de aceite

- [ ] Ações de outras etapas (criar/editar/excluir tenant, atribuir produto, convidar usuário) geram evento de auditoria visível aqui.
- [ ] Listagem de eventos funciona com os filtros.
- [ ] Detalhe de um evento retorna os campos técnicos completos.
- [ ] Não existe endpoint de escrita direta de auditoria.
- [ ] Tenant fora do escopo do usuário (sem `SUPER_ADMIN`) retorna 404 (não 403).
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).
- [ ] `AuditEventRepository` tem Javadoc na interface e em todo método.

## Validação

> **Entrega via collection Bruno, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os como requests `.bru` na pasta numerada desta etapa em `bruno/` (collection cumulativa, autenticação herdada via header `Authorization: Bearer {{token}}` definido em `collection.bru`) e valide a collection inteira via `npx @usebruno/cli run --env local`.

```bash
# excluir um tenant (etapa 09) e depois verificar que gerou evento de auditoria alto risco
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/tenants/<tenantId>/audit-events
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio audit com servico central de gravacao e endpoints de leitura"
```
