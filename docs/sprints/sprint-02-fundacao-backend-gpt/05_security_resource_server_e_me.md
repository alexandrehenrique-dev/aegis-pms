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

Classes esperadas: `security/SecurityConfig.java`, `security/AuthenticatedUser.java` (campos: `subject`, `email`, `username`, `name`, `roles`), `security/AuthenticatedUserProvider.java`, `security/JwtRoleConverter.java`.

### B. Endpoint `/api/v1/me`

```txt
GET /api/v1/me
```

Retorna os claims do usuário autenticado a partir do JWT, sem consultar nada do domínio ainda:

```json
{
  "subject": "uuid-keycloak",
  "email": "loki@byop.dev",
  "username": "loki",
  "name": "Loki",
  "roles": ["AEGIS_SUPER_ADMIN"]
}
```

Não aceita body nem `userId` no request — a identidade vem só do token.

## Critérios de aceite

- [ ] Sem token, `/api/v1/me` retorna 401.
- [ ] Com token válido, `/api/v1/me` retorna 200 com os claims.
- [ ] Token inválido ou expirado retorna 401.
- [ ] Backend valida o issuer correto (`KEYCLOAK_ISSUER_URI`).

## Validação

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

(O `grant_type=password` aqui é só para teste manual via curl nesta etapa de backend — não é a estratégia de login da SPA, que será resolvida na Sprint 06 deste plano, fora do GPT.)

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): resource server com keycloak e endpoint /api/v1/me"
```
