# Sprints de Integração Frontend ↔ Backend — Aegis PMS

> Esta pasta documenta as sprints necessárias para integrar o frontend React com o backend Spring Boot, preparar o deploy de produção em `aegis.byop.dev` (genesis-lab, IP `198.162.0.77`) e configurar o pipeline CI/CD via GitHub Actions self-hosted.
>
> **Pré-requisito:** todas as sprints de backend (etapas 01–30) devem estar implementadas antes de executar as sprints de integração. As sprints de frontend (01–23) devem estar implementadas a partir da Sprint de Integração 03 em diante.
>
> **Regra de ouro:** o `docker-compose.yml` local permanece **intocado** — continua subindo Postgres + Keycloak + backend localmente para modo de desenvolvimento. Um novo `docker-compose.prod.yml` cuida exclusivamente do deploy em produção com serviços externos.

---

## Diagnóstico resumido (auditoria pré-integração)

**Problemas críticos encontrados:**

1. **Login 100% mock mesmo em modo API** — `LoginScreen.tsx` autentica contra `mockUsers` sem checar `IS_API_MODE`. Qualquer pessoa com `admin@byop.io/senha123` entra na UI em produção.
2. **Toggle inconsistente** — `apiMode.ts` usa `VITE_API_MODE === 'api'`; `keycloakConfig.ts` usa `=== "real"`. Com `VITE_API_MODE=api` (prod), o painel de contas demo com senha em texto aparece na tela de login de produção.
3. **Placeholders literais `{productId}`/`{contentId}`/`{formId}`** em vários services — chamadas garantidamente quebradas em modo API.
4. **Slug vs UUID** — frontend usa `key`/slug nos paths; backend espera `UUID id`. Cross-cutting em todos os domínios.
5. **`apiClient.ts` fixa `Content-Type: application/json`** — impossibilita upload multipart (assets).
6. **Paths errados** — `notificationsService` chama `/me` (backend: `/mine`); `settingsService` família inteira diverge; `auditService` tem POST que não existe; `productAssignmentsService` chama `/assignments` (backend: `/users`).
7. **Nenhum workflow GitHub Actions** — pipeline CI/CD precisa ser criado do zero.
8. **Keycloak realm tem redirect URIs só de localhost** — produção em `aegis.byop.dev` não funciona.

---

## Ordem de execução

| Sprint | Arquivo | Foco | Pré-requisito |
|---|---|---|---|
| **01** | `01_autenticacao_real.md` | Login/logout/refresh reais, token no `apiClient`, remover credenciais do bundle de prod | — (bloqueia tudo) |
| **02** | `02_contratos_cross_cutting.md` | Unificar slug→UUID, corrigir placeholders literais, mapper de roles, `apiClient` multipart | Sprint 01 |
| **03** | `03_nucleo_navegacao.md` | Products, Tenants, Dashboard, Notifications, `GET /me` | Sprint 02 |
| **04** | `04_pages_content.md` | Pages, Sections, Events, Globals, Content editorial + workflow | Sprint 03 |
| **05** | `05_forms_submissions_assets.md` | Forms builder, Submissions, Assets (upload multipart) | Sprint 03 |
| **06** | `06_users_settings_audit.md` | Users (userId vs email), Settings, Audit, ProductAssignments | Sprint 03 |
| **07** | `07_knowledge_graph_analytics.md` | KG (edges, search), Analytics (telas estáticas → dinâmicas) | Sprint 04 |
| **08** | `08_infra_deploy.md` | `docker-compose.prod.yml`, Caddyfile `aegis.byop.dev`, GitHub Actions CI/CD | Pode correr em paralelo com 04–07 |

---

## Variáveis de ambiente de produção (referência)

```
# genesis-lab / aegis.byop.dev
AEGIS_APP_BASE_URL=https://aegis.byop.dev
AEGIS_APP_DOMAIN=aegis.byop.dev

# Banco (máquina Oracle externa — ainda não comprada)
AEGIS_DB_HOST=<IP_ORACLE>
AEGIS_DB_PORT=5434
AEGIS_DB_NAME=aegis_db
AEGIS_DB_USER=aegis_user
AEGIS_DB_PASSWORD=<SECRET>

# Keycloak (mesmo host Oracle)
KEYCLOAK_INTERNAL_BASE_URL=http://<IP_ORACLE>:8282
KEYCLOAK_ISSUER_URI=https://auth.byop.dev/realms/aegis   # ou subdomínio
KEYCLOAK_ADMIN=<SECRET>
KEYCLOAK_ADMIN_PASSWORD=<SECRET>

# SMTP real
SMTP_HOST=<PROVIDER>
SMTP_PORT=587
SMTP_USER=<SECRET>
SMTP_PASSWORD=<SECRET>
SMTP_FROM=noreply@byop.dev

# Storage
AEGIS_STORAGE_LOCAL_PATH=/data/aegis/assets

# Telegram alerts (opcional)
AEGIS_TELEGRAM_BOT_TOKEN=<SECRET>
AEGIS_TELEGRAM_CHAT_ID=<SECRET>
```

---

## O que NÃO muda

- `docker-compose.yml` local: intocado. Continua subindo Postgres (5434), Keycloak-Postgres (5435), Keycloak (8282), MailHog — tudo localmente.
- `VITE_API_MODE=mock` continua sendo o padrão de dev — mocks funcionam como hoje.
- Frontend e backend continuam funcionando de forma independente: frontend em mock mode sem backend rodando; backend com Bruno sem frontend.
- Testes JaCoCo, testes de backend e Bruno collection continuam passando em cada sprint de domínio antes de avançar para integração.
