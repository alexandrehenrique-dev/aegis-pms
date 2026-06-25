# SPRINT-RESULTADO.md — Artefato de continuidade entre etapas

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Este arquivo é cumulativo: cada etapa concluída adiciona **uma** entrada nova (template fixo na Seção 12.2), nunca reescreve uma entrada já existente. É o que você cola, junto do `.md` da próxima etapa, em toda conversa nova do GPT a partir da etapa 02 — substitui a memória que aquela conversa não tem.
>
> Evidências detalhadas de cada etapa ficam isoladas em `docs/sprints/backend/results/sprint-N.md`; este arquivo é o resumo consolidado que cada conversa nova recebe. As entradas das etapas 01, 02, 03, 05, 06 e 06.1 foram retroativamente reconstruídas em 2026-06-25 a partir dos respectivos arquivos `results/` — caso algum detalhe não tenha sido registrado lá no momento da execução original, o arquivo `results/sprint-N.md` correspondente prevalece.

## Etapa 01 — Estrutura do repositório e variáveis de ambiente (concluída em 2026-06-21)

**Classes criadas/alteradas:** nenhuma classe Java — etapa de fundação. Arquivos: `docker-compose.yml`, `.env.example`, `.gitignore`, `backend/pom.xml`, `backend/Dockerfile`, estrutura de diretórios (`backend/`, `frontend/`, `infra/docker/`, `infra/keycloak/`, `scripts/`, `docs/`).

**Endpoints confirmados:** nenhum endpoint implementado nesta etapa.

**Decisões de implementação registradas pelo GPT:** frontend oficial React + Vite, porta 5173 (ADR-0011); backend Java 25 / Spring Boot 4.1.x / Spring Security 7 / Spring Data JPA / Flyway / Spring Modulith; PostgreSQL como único banco oficial (nunca H2, nunca MongoDB); arquitetura monólito modular (nunca microsserviços); Keycloak como único provedor de identidade (nunca autenticação própria); prefixo global de API obrigatório `/api/v1`.

**Retrofits pendentes para etapas futuras:** Docker Compose completo, PostgreSQL do Aegis e do Keycloak, Keycloak configurado, projeto Spring Boot funcional, Flyway/Modulith configurados, realm/client/integração OAuth2 Resource Server, primeiro contrato REST sob `/api/v1` — todos resolvidos nas etapas 02-05.

**Cobertura de testes:** não aplicável — nenhuma classe de produção criada nesta etapa. Evidências completas: `results/sprint-1.md`.

## Etapa 02 — PostgreSQL dedicado do Aegis e do Keycloak (concluída em 2026-06-21)

**Classes criadas/alteradas:** nenhuma classe Java — etapa de infraestrutura. Containers `aegis-postgres` e `keycloak-postgres-aegis` (PostgreSQL 16), cada um com volume Docker nomeado e persistente, adicionados ao `docker-compose.yml`.

**Endpoints confirmados:** nenhum endpoint de domínio nesta etapa.

**Decisões de implementação registradas pelo GPT:** dois bancos PostgreSQL totalmente separados (nunca compartilhar schema entre Aegis e Keycloak); nenhum dos dois pode perder dados em `docker compose down` sem `-v`; H2 nunca é uma opção, nem em dev.

**Retrofits pendentes para etapas futuras:** subir o Keycloak conectado ao `keycloak-postgres` (etapa 03).

**Cobertura de testes:** não aplicável — infraestrutura pura, validada via healthcheck dos containers (`Up (healthy)`) e `docker compose down && docker compose up -d` sem `-v`. Evidência consolidada junto da etapa 03, já que não há `results/sprint-2.md` dedicado exclusivamente a esta etapa — ver `results/sprint-2.md` (validação dos containers operacionais) e `results/sprint-4.md` (healthcheck confirmado novamente).

## Etapa 03 — Keycloak persistente, realm/client/roles e export (concluída em 2026-06-21)

**Classes criadas/alteradas:** nenhuma classe Java. Artefato `infra/keycloak/realm/aegis-realm.json` (export do realm). Container `aegis-keycloak` adicionado ao `docker-compose.yml`, conectado ao `keycloak-postgres` da etapa 02.

**Endpoints confirmados:** `GET http://localhost:8282/realms/aegis/.well-known/openid-configuration` (OIDC discovery), validado com `HTTP 200 OK`. Nenhum endpoint REST do backend Aegis criado nesta etapa.

**Decisões de implementação registradas pelo GPT:** realm `aegis`, client `aegis-web` (OpenID Connect, client authentication OFF, standard flow ON, PKCE S256); 5 realm roles globais canônicas (`AEGIS_SUPER_ADMIN`, `AEGIS_TENANT_ADMIN`, `AEGIS_PRODUCT_MANAGER`, `AEGIS_EDITOR`, `AEGIS_VIEWER`); usuário de validação criado com `AEGIS_SUPER_ADMIN`; serviço Docker mantido como `keycloak-postgres` (hostname interno usado em `KC_DB_URL_HOST`).

**Retrofits pendentes para etapas futuras:** `directAccessGrantsEnabled` foi ativado apenas para validação manual via `grant_type=password` — não representa o fluxo oficial da SPA (resolvido pelo proxy de autenticação da etapa 06).

**Cobertura de testes:** não aplicável — infraestrutura pura. Validado via `SELECT count(*) FROM realm` (resultado: 2), persistência confirmada após restart e `docker compose down/up` sem `-v`, e export real do realm. Evidências completas: `results/sprint-2.md` (fechamento) e `results/sprint-3.md` (execução parcial anterior, mesma etapa).

## Etapa 04 — Backend Spring Boot base e conexão com PostgreSQL/Flyway (concluída em 2026-06-21)

**Classes criadas/alteradas:** `br.com.byop.aegis.AegisApplication` e `br.com.byop.aegis.shared.storage.LocalStorageBootstrap`; adicionados também `application.yml`, perfis `local`/`prod` e a migration consolidada `V1__init.sql`.

**Endpoints confirmados:** `GET /actuator/health`, respondendo HTTP 200 com `status: UP`. Nenhum endpoint de domínio foi criado nesta etapa.

**Decisões de implementação registradas pelo GPT:** o schema inicial foi consolidado em uma única migration V1; a tabela `event_publication` segue o formato exigido pelo Spring Modulith 2.0.1; o bootstrap de storage é síncrono no startup, usa Java NIO e interrompe a aplicação quando a raiz não pode ser criada; `ddl-auto` permanece em `validate`.

**Retrofits pendentes para etapas futuras:** criar as subpastas por produto/categoria quando os domínios `product` e `asset` forem implementados; adicionar configuração explícita de `SecurityFilterChain` na etapa 05; evoluir as tabelas mínimas na etapa 06 sem recriar o schema do zero.

**Cobertura de testes:** `mvn clean verify` terminou com `BUILD SUCCESS` e executou o goal `jacoco:check`; como ainda não havia execução de testes gerando `jacoco.exec`, o JaCoCo informou explicitamente que o check foi ignorado por ausência do arquivo de execução. Evidências completas: `results/sprint-4.md`.

## Etapa 05 — Security Resource Server e `/api/v1/me` (concluída em 2026-06-23)

**Classes criadas/alteradas:** `AuthenticatedUser`, `AuthenticatedUserProvider`, `JwtRoleConverter`, `SecurityConfig` (segurança); `MeController`, `MeResponse`, `MeResponseMapper` (API); testes `AuthenticatedUserProviderTest`, `JwtRoleConverterTest`, `MeResponseMapperTest`, `MeControllerTest`, `SecurityConfigTest`; testes complementares de cobertura `AegisApplicationTest`, `LocalStorageBootstrapTest`.

**Endpoints confirmados:** `GET /api/v1/me` — protegido por JWT, retorna `{ subject, email, username, name, role }` com `role` no formato canônico minúsculo (ex.: `super_admin`). `401` sem token e com JWT inválido, `200` com JWT válido.

**Decisões de implementação registradas pelo GPT:** conversão de roles `AEGIS_<NOME>` (Keycloak) → `ROLE_<NOME>` (Spring Security) → `<nome>` minúsculo (contrato `/me`) centralizada em `JwtRoleConverter` (ADR-0014); `directAccessGrantsEnabled=true` no client `aegis-web` necessário para validação manual via `grant_type=password` (uso temporário, substituído pelo proxy da etapa 06); `@EnableWebSecurity` explícito necessário para os testes `@WebMvcTest` encontrarem o bean `HttpSecurity`.

**Retrofits pendentes para etapas futuras:** o `grant_type=password` direto contra o Keycloak usado para validação manual nesta etapa não deve ser exposto ao frontend — resolvido pelo auth proxy (BFF) da etapa 06.

**Cobertura de testes:** `mvn clean verify` com Sonar e JaCoCo aprovados (100%). Evidências completas: `results/sprint-5.md`.

## Etapa 06 — Auth Proxy, SMTP e fluxo de convite (concluída em 2026-06-24)

**Classes criadas/alteradas:** `KeycloakTokenClient`, `KeycloakTokenResponse`, `KeycloakAdminClient` (clients); `AuthLoginRequest`, `AuthRefreshRequest`, `AuthLogoutRequest`, `AuthForgotPasswordRequest`, `AuthTokenResponse`, `AuthTokenResponseMapper`, `AuthMessageResponse` (DTOs); `AuthService` (orquestração); `AuthController`, `AuthExceptionHandler`, `AuthErrorResponse` (REST); `InvalidCredentialsException`, `AccountDisabledException`, `RefreshTokenExpiredException`, `KeycloakAuthenticationException` (exceções); `KeycloakProperties`, `RestClientConfig` (config). Endpoints administrativos de usuários (`GET /api/v1/users`, `GET /api/v1/users/{id}`) também entregues nesta etapa, antecipando parte da etapa 14/15. Templates `infra/keycloak/themes/aegis/email/{html,text}/executeActions.ftl` e `password-reset.ftl`, `theme.properties`, serviço `mailhog` no `docker-compose.yml`.

**Endpoints confirmados:** `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` (204), `POST /api/v1/auth/forgot-password` (200 genérico, sem enumeração de e-mail) — todos públicos, liberados em `/api/v1/auth/**` no `SecurityFilterChain`. `GET /api/v1/users` e `GET /api/v1/users/{id}` (protegidos, via Keycloak Admin API) entregues como antecipação.

**Decisões de implementação registradas pelo GPT:** `KeycloakProperties` usa defaults locais para `KEYCLOAK_ISSUER_URI`/`KEYCLOAK_INTERNAL_BASE_URL`/`KEYCLOAK_REALM`/`KEYCLOAK_WEB_CLIENT_ID` (evita falha de placeholder sem valor em dev); SMTP/sender configurado no realm Keycloak e exportado em `aegis-realm.json` (sem senha, conforme padrão) — obrigatório para `execute-actions-email` funcionar; `@PathVariable("id")` declarado explicitamente em todo path variable (evita dependência de metadata `-parameters` do compilador); CSRF desabilitado mantido para API REST stateless baseada em Bearer Token.

**Retrofits pendentes para etapas futuras:** templates `productAssignment.ftl`/`productAccessRevoked.ftl` (necessários pelas etapas 10 e 15) ficaram fora do escopo desta etapa — resolvidos na etapa 06.1. Avaliar padronização de encoding Maven (`UTF-8` explícito no `pom.xml`) para eliminar warnings recorrentes — ainda pendente.

**Cobertura de testes:** `mvn clean verify` com `Tests run: 70, Failures: 0, Errors: 0, Skipped: 0` e `All coverage checks have been met` (JaCoCo 100%). Evidências completas: `results/sprint-6.md`.

## Etapa 06.1 — Hotfix: templates de e-mail de produto (concluída em 2026-06-25)

**Classes criadas/alteradas:** nenhuma classe Java — etapa exclusiva de templates. `infra/keycloak/themes/aegis/email/html/productAssignment.ftl` e `infra/keycloak/themes/aegis/email/html/productAccessRevoked.ftl`.

**Endpoints confirmados:** nenhum — templates consumidos futuramente pelas etapas 10/15 via `JavaMailSender`/`FreeMarkerTemplateUtils`, não via `executeActionsEmail` do Keycloak.

**Decisões de implementação registradas pelo GPT:** templates mantidos em `infra/keycloak/themes/aegis/email/html/` (mesmo diretório dos templates da etapa 06), não em `backend/src/main/resources/templates/email/` — centraliza todo template de e-mail do tema Aegis num único lugar, já que a etapa 10 vai apenas renderizá-los, não depende do Keycloak para isso.

**Retrofits pendentes para etapas futuras:** a etapa 10 (`POST /products/{productId}/users` com `userId`) e a etapa 15 (remoção de `ProductAssignment`) precisam implementar o envio efetivo via `JavaMailSender` apontando para estes dois arquivos `.ftl`; validação funcional via MailHog só é possível quando a etapa 10 existir.

**Cobertura de testes:** não aplicável — sem código Java, sem testes, sem migration, conforme padrão de entrega da própria etapa. Evidências completas: `results/sprint-6.1.md`.

## Etapa 07 — Modelo Core: Tenant, Membership, Product e Catálogo de Módulos (concluída em 2026-06-25)

**Classes criadas/alteradas:** entidades `Tenant`, `TenantMembership`, `Product`, `ProductModule` e `ProductAssignment`; repositories `TenantRepository`, `TenantMembershipRepository`, `ProductRepository`, `ProductModuleRepository` e `ProductAssignmentRepository`; enums de status, papéis, tipos de produto, módulos e storage; DTOs/records `TenantSummary`, `ProductSummary`, `ProductDetail` e `ProductModuleSummary`; mappers MapStruct `TenantMapper`, `ProductMapper` e `ProductModuleMapper`; services `TenantService`, `ProductService` e `ProductModuleService`; `ProductAccessResolver`; controllers `TenantController` e `ProductController`; contracts `CreateTenantRequest` e `CreateProductRequest`; `CoreExceptionHandler`/`CoreErrorResponse`; `RequireModule`, `ModuleAccessAspect`, `ModuleDisabledException` e `ModuleProductIdMissingException`. Ajustados também `KeycloakProperties`/`KeycloakAdminClient`/`application.yml`/`.env.example` para externalizar o path da Admin API.

**Endpoints confirmados:** `GET/POST /api/v1/tenants`; `GET/POST /api/v1/products`; `GET /api/v1/products/{productId}`; `POST /api/v1/products/{productId}/modules/{moduleKey}/enable`; `POST /api/v1/products/{productId}/modules/{moduleKey}/disable`.

**Decisões de implementação registradas pelo GPT:** modelo core implementado em `br.com.byop.aegis.core`; `ProductAssignment` automático para criador do produto com role `product_manager` e status `atribuido`; listagem de produtos filtrada por papel no service; `ProductAccessResolver` centralizado para etapas futuras; dependência de módulos mantida em mapa (`KNOWLEDGE_GRAPH -> CONTENT`); `@RequireModule` exige método anotado com `@PathVariable("productId") UUID productId`; módulo desabilitado bloqueia todos os papéis, inclusive `SUPER_ADMIN`; `assetStorageStrategy` default permanece no service; path `/admin/realms/` externalizado para `keycloak.admin-realms-path`.

**Retrofits pendentes para etapas futuras:** aplicar `ProductAccessResolver` e `@RequireModule` nos domínios de conteúdo, páginas, assets, forms, analytics e knowledge graph; implementar provisionamento real do storage local/S3 conforme etapa de assets caso ainda não esteja completo; evoluir regras próprias do módulo `ECOMMERCE` apenas em sprint futura dedicada; manter collection Postman cumulativa atualizada.

**Cobertura de testes:** `mvn clean verify` com `BUILD SUCCESS`, 179 testes, 0 falhas, 0 erros, 0 ignorados e JaCoCo aprovado (`All coverage checks have been met`). Collection Postman da Sprint 07 validada manualmente. Alertas SonarQube for IDE removidos; nenhum pacote/import `br.com.aegis.pms`. Base package oficial mantido em `br.com.byop.aegis`. Evidências completas: `results/sprint-7.md`.
