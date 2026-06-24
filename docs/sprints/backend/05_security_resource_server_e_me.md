# Etapa 05 — Resource Server com Keycloak e endpoint `/api/v1/me`

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 03 (Keycloak) e 04 (backend base) concluídas.

## Contexto fixo

Aegis PMS: backend Spring Boot protegido por OAuth2 Resource Server validando JWT emitido pelo Keycloak. Toda rota sob `/api/v1/**` exige token válido; `/actuator/health`, `/actuator/info` e o Swagger local ficam públicos.

## Objetivo

Backend valida JWT do Keycloak e expõe o primeiro endpoint autenticado, `/api/v1/me`.

## Tarefas

### A. Configuração do Resource Server

`application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8282/realms/aegis}
```

Regras de acesso: `/api/v1/**` protegido; `/actuator/health` e `/actuator/info` públicos; Swagger liberado em `local`; CSRF off (API stateless); CORS configurado para `http://localhost:5173` em dev.

> **Spring Boot 4 / Spring Security 7 (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 1)**: declarar o `SecurityFilterChain` de forma **explícita** em `SecurityConfig.java` — todas as regras acima (rotas públicas, CSRF, CORS, resource server) configuradas no próprio bean, nada implícito por auto-configuração. Não existe mais um default "razoável" que cubra o que não foi declarado.

Classes esperadas: `security/SecurityConfig.java`, `security/AuthenticatedUser.java` (campos: `subject`, `email`, `username`, `name`, `authorities` — granted authorities Spring, ex. `ROLE_SUPER_ADMIN`), `security/AuthenticatedUserProvider.java`, `security/JwtRoleConverter.java`. Seguir o padrão de qualidade (Javadoc, 100% cobertura, rodadas) de `00_padrao_qualidade_e_arquitetura.md` — esta etapa não tem `Repository`/`Mapper` (sem entidade própria ainda), então a "rodada" aqui é: 1) classes de segurança + `AuthenticatedUserProvider` com testes unitários; 2) endpoint `/me` (Controller) + teste `@WebMvcTest`.

#### A.1 `JwtRoleConverter` — regra de conversão (ver ADR-0014)

O JWT do Keycloak traz `realm_access.roles` com os nomes criados na etapa 03 (`AEGIS_SUPER_ADMIN`, `AEGIS_TENANT_ADMIN`, `AEGIS_PRODUCT_MANAGER`, `AEGIS_EDITOR`, `AEGIS_VIEWER`). `JwtRoleConverter` implementa a conversão de roles do `Jwt` com a seguinte regra, fixa e não negociável:

1. Ler `realm_access.roles` do claim do JWT.
2. Manter apenas as roles que começam com `AEGIS_` (ignorar roles padrão do Keycloak como `offline_access`, `uma_authorization`).
3. Remover o prefixo `AEGIS_`, manter o restante maiúsculo.
4. Produzir uma `SimpleGrantedAuthority("ROLE_" + nomeSemPrefixo)` — ex. `AEGIS_SUPER_ADMIN` → `ROLE_SUPER_ADMIN`.

Isso é o que alimenta `@PreAuthorize("hasRole('SUPER_ADMIN')")` em todas as etapas de domínio (06, 09, 10, 14, 16, 20, 23) que já escrevem regras de negócio citando `SUPER_ADMIN`/`TENANT_ADMIN`/`PRODUCT_MANAGER`/`EDITOR`/`VIEWER` em prosa — o nome usado em `hasRole(...)` é sempre o nome sem prefixo, maiúsculo, sem `ROLE_` (o Spring Security adiciona o prefixo `ROLE_` sozinho na hora de checar).

### B. Endpoint `/api/v1/me`

```txt
GET /api/v1/me
```

Retorna os claims do usuário autenticado a partir do JWT, sem consultar nada do domínio ainda. **Importante (ADR-0014): `role` é singular e minúsculo — nunca um array, nunca maiúsculo** (é exatamente o formato que `AuthUser.role`/`UserRole` já esperam no frontend, `frontend/src/shared/types/auth.ts`). Se por algum motivo o token trouxer mais de uma role `AEGIS_*` (não deveria, pelo desenho do modelo — um papel por membership), resolver para uma só usando a prioridade `super_admin > tenant_admin > product_manager > editor > viewer`.

```json
{
  "subject": "uuid-keycloak",
  "email": "loki@byop.dev",
  "username": "loki",
  "name": "Loki",
  "role": "super_admin"
}
```

A conversão de `ROLE_SUPER_ADMIN` (authority Spring, usada internamente em `@PreAuthorize`) para `"super_admin"` (string da resposta pública) é responsabilidade do mapper/DTO desta etapa, não do `JwtRoleConverter` — são duas conversões com propósitos diferentes, não confundir.

Não aceita body nem `userId` no request — a identidade vem só do token.

## Critérios de aceite

- [ ] Sem token, `/api/v1/me` retorna 401.
- [ ] Com token válido, `/api/v1/me` retorna 200 com os claims.
- [ ] Token inválido ou expirado retorna 401.
- [ ] Backend valida o issuer correto (`KEYCLOAK_ISSUER_URI`).
- [ ] `SecurityFilterChain` é declarado explicitamente (sem depender de default do Spring Boot 4).
- [ ] `mvn clean verify` confirma 100% de cobertura em `AuthenticatedUserProvider`/`JwtRoleConverter`/Controller do `/me` (JaCoCo).
- [ ] `/me` devolve `role` como string singular minúscula (`"super_admin"`, nunca `"AEGIS_SUPER_ADMIN"` nem array) — ver ADR-0014.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
# sem token
curl -i http://localhost:8080/api/v1/me
# esperado: 401

# obter token (usuário de teste criado na etapa 03)
TOKEN=$(curl -s -X POST http://localhost:8282/realms/aegis/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=aegis-web" \
  -d "grant_type=password" \
  -d "username=loki" \
  -d "password=123456" | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/me
# esperado: 200 com os claims
```

(O `grant_type=password` aqui é só para validação direta desta etapa — não é a estratégia de login da SPA. A **etapa 06** (`06_auth_proxy_smtp_e_convite.md`) cria os endpoints `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` e `POST /api/v1/auth/refresh` que o frontend usa: o backend faz o proxy para o Keycloak internamente, o frontend nunca conhece a URL do Keycloak. A Sprint 06 do frontend (`docs/sprints/frontend/06_keycloak_login_ui_custom.md`) consome esses endpoints proxy, não o Keycloak diretamente.)

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): resource server com keycloak e endpoint /api/v1/me"
```
