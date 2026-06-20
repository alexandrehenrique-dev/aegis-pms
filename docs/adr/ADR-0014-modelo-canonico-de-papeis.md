# ADR-0014 — Modelo canônico de papéis (Keycloak ↔ Backend ↔ Frontend)

## Status

ACCEPTED

## Contexto

Uma auditoria de consistência feita antes do início da implementação do backend (Sprint 02, etapas do GPT) encontrou três representações divergentes do mesmo conjunto de papéis:

1. **Etapa 03** (`sprint-02-fundacao-backend-gpt/03_keycloak_persistente_e_realm.md`) listava as roles do realm Keycloak como `AEGIS_SUPER_ADMIN`, `TENANT_ADMIN`, `PRODUCT_MANAGER`, `CONTENT_EDITOR`, `CONTENT_REVIEWER`, `VIEWER` — 6 roles, prefixo `AEGIS_` presente só na primeira, e dois nomes (`CONTENT_EDITOR`/`CONTENT_REVIEWER`) que não existem em nenhuma outra etapa.
2. **Todas as demais etapas** (06, 09, 10, 14, 16, 20, 23) já usam, em regras de negócio e seeds, um conjunto diferente e consistente entre si: `SUPER_ADMIN`, `TENANT_ADMIN`, `PRODUCT_MANAGER`, `EDITOR`, `VIEWER` — 5 papéis, sem prefixo.
3. **O frontend** (`frontend/src/shared/types/auth.ts`) define `UserRole` como `"super_admin" | "tenant_admin" | "product_manager" | "editor" | "viewer"` — minúsculo, singular (`AuthUser.role`, não um array).
4. A etapa 05 (`/api/v1/me`) devolvia `"roles": ["AEGIS_SUPER_ADMIN"]` — formato bruto do Keycloak, em array, plural — incompatível com o shape que o frontend já consome (`role`, singular, minúsculo).

Sem uma decisão explícita, a implementação real do backend teria criado 6 roles erradas no Keycloak, deixado o `JwtRoleConverter` sem regra de conversão definida, e devolvido um `/me` que o frontend não conseguiria consumir sem um adaptador improvisado.

## Decisão

Existe **um único catálogo de 5 papéis**, com três representações fixas e uma regra de conversão única entre elas:

| Camada | Formato | Valores |
|---|---|---|
| Keycloak (realm role) | `AEGIS_<NOME>`, maiúsculo | `AEGIS_SUPER_ADMIN`, `AEGIS_TENANT_ADMIN`, `AEGIS_PRODUCT_MANAGER`, `AEGIS_EDITOR`, `AEGIS_VIEWER` |
| Backend (Spring Security authority / `@PreAuthorize`) | `ROLE_<NOME>`, maiúsculo, sem prefixo `AEGIS_` | `ROLE_SUPER_ADMIN`, `ROLE_TENANT_ADMIN`, `ROLE_PRODUCT_MANAGER`, `ROLE_EDITOR`, `ROLE_VIEWER` |
| Backend (regra de negócio em prosa/código de domínio, ex. `TenantMembership.role`) | `<NOME>`, maiúsculo | `SUPER_ADMIN`, `TENANT_ADMIN`, `PRODUCT_MANAGER`, `EDITOR`, `VIEWER` |
| API pública (`GET /api/v1/me`) e Frontend (`UserRole`) | `<nome>`, minúsculo, singular | `super_admin`, `tenant_admin`, `product_manager`, `editor`, `viewer` |

Regra de conversão, em `JwtRoleConverter` (etapa 05): extrair as roles do claim `realm_access.roles` do JWT, manter apenas as que começam com `AEGIS_`, remover o prefixo e mapear para `GrantedAuthority("ROLE_" + nome)` — isso alimenta `hasRole(...)`/`@PreAuthorize` em todas as etapas que já escrevem regras como `hasRole('SUPER_ADMIN')`.

`AuthenticatedUser` carrega o conjunto de authorities (para granularidade interna), mas o **DTO de resposta de `/api/v1/me`** resolve isso para **um único campo `role` (singular, minúsculo)** — nunca um array — usando a prioridade abaixo se por algum motivo o token trouxer mais de uma role (não deveria acontecer pelo desenho do modelo, mas a prioridade existe como salvaguarda):

```
super_admin > tenant_admin > product_manager > editor > viewer
```

## Consequências

Positivas:
- Elimina ambiguidade: existe exatamente uma forma de escrever cada papel em cada camada, documentada uma única vez.
- `/api/v1/me` passa a devolver exatamente o shape que `AuthUser.role` já espera, sem adaptador no frontend.
- Toda etapa que já escreve `SUPER_ADMIN`/`EDITOR` em prosa (06, 09, 10, 14, 16, 20, 23) continua correta sem alteração — só a etapa 03 (que estava errada) e a 05 (que estava subespecificada) precisam mudar.

Negativas / trade-offs:
- Qualquer papel novo no futuro precisa ser adicionado nas quatro camadas da tabela, nunca só numa — risco de drift se a regra não for seguida.
- A prioridade de resolução (super_admin > ... > viewer) é uma decisão arbitrária para o caso defensivo de múltiplas roles; se o produto algum dia precisar de múltiplos papéis simultâneos por usuário, esta ADR precisa ser revisada (hoje o modelo assume um papel por membership/atribuição, nunca soma de papéis).

## Alternativas Consideradas

- **Backend e frontend usarem o mesmo formato maiúsculo do Keycloak**: rejeitado — exigiria reescrever `UserRole` e todo o RBAC do frontend já implementado (`core/permissions/roles.ts`), que é código real em produção, não um mock a ajustar.
- **`/me` devolver `roles: string[]` (manter array)**: rejeitado — o modelo de domínio (`TenantMembership.role`, `ProductAssignment.role`) já é um papel por vínculo, nunca uma lista; expor array no contrato público sugeriria multiplicidade que não existe e forçaria o frontend a escolher um índice arbitrariamente.

## Impactos

- **Backend**: etapa 03 (lista de roles do realm corrigida para 5, todas com prefixo `AEGIS_`), etapa 05 (`JwtRoleConverter` com a regra de conversão explícita, `/me` devolve `role` singular minúsculo), `00_padrao_qualidade_e_arquitetura.md` (nova seção de referência).
- **Frontend**: nenhuma mudança de código — `UserRole`/`AuthUser.role` já estavam corretos; esta ADR só formaliza o contrato que o backend precisa cumprir.
- **Seed** (etapa 20): usuários de teste continuam usando os nomes em prosa (`SUPER_ADMIN`, `TENANT_ADMIN`...) ao descrever memberships — sem mudança, já estavam no formato certo da camada de domínio.

## Links Relacionados

- ADR-0005 (Keycloak como IAM).
- `frontend/src/shared/types/auth.ts` (`UserRole`, `AuthUser`).
- `docs/sprints/sprint-02-fundacao-backend-gpt/03_keycloak_persistente_e_realm.md`, `05_security_resource_server_e_me.md`.
