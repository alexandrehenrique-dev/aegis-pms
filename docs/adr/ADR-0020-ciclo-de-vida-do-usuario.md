# ADR-0020 — Ciclo de vida do usuário: remoção, inativação e restauração

## Status

ACCEPTED

## Contexto

A etapa 15 do backend documenta `block` (bloquear usuário: marcar `TenantMembership.status` como inativo) e `resend-invite`, mas não existe nenhum endpoint documentado para **remover** um usuário de um produto, de um tenant, ou revogar o acesso permanentemente. Tampouco existe documentação sobre o que acontece quando o usuário precisa ser restaurado.

Dois cenários não cobertos:

1. **Remoção de um usuário de um produto específico**: o `ProductAssignment` é deletado (`DELETE /products/{productId}/users/{userId}` já existe na etapa 10), mas não há documentação do impacto disso — o usuário continua no Keycloak, continua com `TenantMembership`, e pode ser re-atribuído ao produto sem precisar de um novo convite.

2. **Remoção de um usuário do tenant**: não existe endpoint documentado. A etapa 15 tem `block` que marca o usuário como inativo, mas não há decisão explícita sobre se isso é reversível, o que acontece no Keycloak, e como restaurar o acesso.

A decisão sobre **hard delete vs soft delete** no Keycloak tem implicações diretas de LGPD:
- Hard delete (remover do Keycloak): perde o `subject` (UUID) que é a chave de rastreabilidade em `TenantMembership`, `ProductAssignment` e `AuditEvent` — torna a trilha de auditoria incompleta.
- Soft delete (manter no Keycloak, desabilitar): preserva o `subject` e toda a trilha histórica; conta pode ser reativada sem recriar do zero.

## Decisão

### 1. Remoção de produto (ProductAssignment)

`DELETE /api/v1/products/{productId}/users/{userId}` remove o `ProductAssignment`.

Consequências:
- O usuário perde acesso ao produto imediatamente (próxima request ao produto retorna 404/403 por falta de `ProductAssignment`)
- O usuário continua no Keycloak com a conta ativa
- O usuário continua com `TenantMembership` ativa (acesso a outros produtos do tenant não é afetado)
- Toda a trilha de auditoria do usuário no produto é preservada (`AuditEvent` referencia `userSubject`, não `ProductAssignment.id`)
- Notificação interna (se módulo de notificações disponível): usuário recebe aviso "Seu acesso ao produto X foi removido"

### 2. Remoção de tenant (TenantMembership — soft delete)

**Decisão: soft delete sempre — nunca hard delete do Keycloak.**

Razões:
- `userSubject` (UUID do Keycloak) é a chave de todos os registros históricos (`TenantMembership`, `ProductAssignment`, `AuditEvent`, `ContentVersion`, `Notification`) — deletar do Keycloak torna todos esses registros "órfãos" e a auditoria incompleta
- Se o usuário tem memberships em outros tenants (possível num ambiente multi-tenant), deletar do Keycloak remove o acesso de todos eles
- LGPD: o direito ao esquecimento não se aplica a registros de auditoria de acesso — apenas a dados pessoais desnecessários (nome, email). O `userSubject` (UUID opaco) pode ser mantido como referência de auditoria

**Novos endpoints (etapa 15):**

```
DELETE /api/v1/tenants/{tenantId}/users/{userId}   → soft delete do tenant
POST   /api/v1/tenants/{tenantId}/users/{userId}/restore  → restauração
```

**Sequência de remoção (soft delete de tenant):**

1. Verificar que não é o último `TENANT_ADMIN`/`SUPER_ADMIN` ativo (regra de "não se trancar para fora")
2. Marcar `TenantMembership.status = "removido"` (novo status, distinto de `"bloqueado"`)
3. Revogar todos os `ProductAssignment`s ativos deste usuário neste tenant (`status = "removido"`)
4. Chamar Keycloak Admin API: se o usuário não tem `TenantMembership` ativa em **nenhum** outro tenant, desabilitar a conta (`enabled: false`). Se ainda tem acesso em outro tenant, **não** tocar na conta do Keycloak — o acesso ao Keycloak é global, não por tenant
5. Registrar evento de auditoria: `USER_REMOVED_FROM_TENANT`
6. Notificar o usuário (e-mail + notificação interna): "Seu acesso ao tenant X foi removido"

**Diferença entre `block` e `remove`:**

| Aspecto | `block` | `remove` |
|---|---|---|
| `TenantMembership.status` | `"bloqueado"` | `"removido"` |
| `ProductAssignment` | inalterado (suspenso pelo block implicitamente) | revogados explicitamente |
| Keycloak | inalterado (pode logar se tiver outro tenant) | desabilitado se sem outros tenants |
| Reversível? | Sim (via `unblock`) | Sim (via `restore`) |
| Notificação e-mail? | Não | Sim |
| Caso de uso | Suspensão temporária, auditoria em andamento | Saída definitiva da equipe |

### 3. Restauração de acesso (restore)

`POST /api/v1/tenants/{tenantId}/users/{userId}/restore`:

1. Verificar que `TenantMembership.status` é `"removido"` ou `"bloqueado"` (senão 400)
2. Marcar `TenantMembership.status = "ativo"`
3. Reativar `ProductAssignment`s do usuário neste tenant? **Não automaticamente** — os assignments foram removidos explicitamente; o admin precisa re-atribuir produtos manualmente. A restoration só recupera o acesso ao tenant, não aos produtos
4. Se a conta Keycloak estava desabilitada: reabilitá-la (`enabled: true`)
5. Chamar `execute-actions-email` com `["UPDATE_PASSWORD"]` — forçar redefinição de senha na primeira entrada após a restauração (segurança — não reutilizar a senha antiga)
6. Registrar evento de auditoria: `USER_RESTORED_TO_TENANT`
7. Notificar o usuário (e-mail): "Seu acesso ao tenant X foi restaurado. Clique no link abaixo para definir uma nova senha."

### 4. Exclusão de account do Keycloak (hard delete — raramente usado)

Hard delete (`DELETE /admin/realms/aegis/users/{keycloakId}`) só deve acontecer como resposta a uma **solicitação de direito ao esquecimento (LGPD Art. 18)**, nunca como operação normal de remoção de usuário. Este fluxo está fora do escopo desta ADR e do Aegis PMS v1 — quando necessário, deve ser tratado como processo manual administrativo com documentação de compliance.

### 5. Notificação de e-mail ao adicionar usuário existente a novo produto

Cenário: `POST /products/{productId}/users` com `userId` (usuário já existente no tenant, não um convite novo). O usuário não recebe um e-mail de "definir senha" (já tem conta), mas precisa ser notificado de que tem acesso a um novo produto.

Fluxo:
1. `ProductAssignment` é criado normalmente
2. Enviar e-mail informativo via Keycloak (ou diretamente via SMTP da etapa 06) para o e-mail do usuário: "Você foi adicionado ao produto X no tenant Y. Acesse [link do produto]"
3. Criar notificação interna (se módulo de notificações ativo): `"Você agora tem acesso ao produto X"`
4. Nenhum `required_actions` é disparado — o usuário já tem senha e acesso ao Keycloak

Este e-mail usa um template FreeMarker separado: `infra/keycloak/themes/aegis/email/html/productAssignment.ftl`. Diferente do `executeActions.ftl` (que tem link de ação), este é puramente informativo: "Olá, você foi adicionado ao produto X. Entre em aegis.app para acessá-lo."

## Consequências

Positivas:
- Trilha de auditoria nunca fica incompleta — `userSubject` sempre permanece referenciável
- Restauração é possível sem recriar conta do zero — operação simples de reativar
- LGPD: dados pessoais são preservados para fins de auditoria; direito ao esquecimento tratado como processo separado e documentado
- Usuário recebe feedback de cada ação (remoção, restauração, nova atribuição de produto)

Negativas / trade-offs:
- Keycloak pode ter usuários `enabled: false` acumulando ao longo do tempo — gerenciamento de "usuários inativos" pode se tornar necessário no futuro
- Restauração não recupera automaticamente os ProductAssignments — admin precisa re-atribuir, o que pode ser trabalhoso em produtos com muitos assignments

## Alternativas Consideradas

- **Hard delete do Keycloak ao remover do tenant**: rejeitado — perde trilha de auditoria e afeta outros tenants do mesmo usuário
- **Soft delete apenas no local (sem tocar Keycloak)**: rejeitado — usuário poderia continuar autenticando com sucesso e só seria bloqueado por falta de `TenantMembership` — aceitável para `block`, mas não para `remove` permanente onde a expectativa é de que a conta esteja inacessível
- **Restauração recupera automaticamente ProductAssignments**: rejeitado — o admin que fez a remoção pode não querer restaurar todos os accesses anteriores; re-atribuição explícita é mais segura e rastreável

## Impactos

- **Backend**: etapa 15 adiciona: `DELETE /tenants/{tenantId}/users/{userId}` (soft delete), `POST /tenants/{tenantId}/users/{userId}/restore`, template de e-mail `productAssignment.ftl` em `infra/keycloak/themes/aegis/email/`; etapa 10 adiciona: chamada de notificação quando `userId` é usado em `POST /products/{productId}/users` (não só para `inviteEmail`).
- **Frontend**: Sprint 19 adiciona: botão "Remover usuário" (com confirmação modal) e "Restaurar acesso" no `UserDetailPanel`; diferencia visualmente usuários `removidos` de `bloqueados` na `UserTable`.
- **Seed** (etapa 21): incluir ao menos um `TenantMembership` com status `"removido"` para validar que o endpoint de restore funciona.

## Links Relacionados

- ADR-0014 (modelo canônico de papéis).
- ADR-0018 (escopo do SUPER_ADMIN).
- `docs/sprints/backend/15_dominio_users.md` (endpoints de remove/restore).
- `docs/sprints/backend/10_tenants_crud_completo_e_product_assignment.md` (notificação no productAssignment com userId).
- `docs/sprints/backend/06_auth_proxy_smtp_e_convite.md` (SMTP, base para o e-mail informativo).
