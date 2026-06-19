# Etapa 12 — OpenAPI/Swagger, testes mínimos e checklist final do servidor

> Cole este arquivo inteiro numa conversa nova do GPT. Última etapa — pré-requisito: todas as etapas 01-11 concluídas.

## Contexto fixo

Última etapa da Sprint 02: documentar a API, garantir uma cobertura mínima de testes nas regras críticas, e validar com um checklist único que toda a fundação operacional está de fato funcionando de ponta a ponta.

## Objetivo

Swagger funcionando em dev, suíte de testes mínima passando, e checklist final do servidor 100% marcado.

## Tarefas

### A. OpenAPI/Swagger

- Habilitado em `local`/`dev`, desabilitado em `prod`.
- Bearer JWT configurado na UI do Swagger (permite colar o token e testar endpoints autenticados).
- Tags por módulo: `System`, `Auth`, `Tenants`, `Products`, `Product Modules`, `Knowledge Graph`.
- Acessível em `http://localhost:8080/swagger-ui/index.html`.

### B. Testes mínimos

Classes: `TenantServiceTest`, `ProductServiceTest`, `ProductModuleServiceTest`, `KnowledgeGraphServiceTest`, `GraphConsistencyPolicyTest`, `AuthenticatedUserProviderTest`, e um smoke test de security.

Cenários obrigatórios: criar tenant; criar produto; bloquear produto de tenant alheio; habilitar módulo válido; rejeitar módulo inválido; criar node; criar edge válida; rejeitar edge com node inexistente; rejeitar edge cross-tenant; listar neighbors; `/api/v1/me` sem token retorna 401.

### C. Checklist final do servidor funcional

Confirmar, nesta ordem, tudo o que foi construído nas etapas 01-11:

**Infra**: `docker compose up -d` sobe tudo · `aegis-postgres` healthy · `keycloak-postgres` healthy · `keycloak` acessível · `backend` acessível · volumes existem · dados persistem após restart.

**Keycloak**: realm `aegis` existe · client `aegis-web` existe · usuário teste existe e persiste após restart · token pode ser emitido · OpenID config acessível.

**Backend**: `/actuator/health` UP · `/api/v1/me` sem token = 401, com token = 200 · Flyway criou as tabelas · Swagger abre em local · API usa `/api/v1`.

**Core**: tenant pode ser criado · produto pode ser criado · produto pode habilitar módulo · listagem respeita membership · produto alheio não é acessível.

**Knowledge Graph**: node pode ser criado · edge pode ser criada · neighbor pode ser consultado · edge inválida é bloqueada · cross-tenant é bloqueado · seed inicial funciona.

**Frontend (React, via etapa 09)**: build gera `dist/` · backend serve a SPA · refresh de rota SPA funciona · API não cai no fallback da SPA.

## Critérios de aceite

- [ ] `mvn clean test` passa sem falhas.
- [ ] Swagger funcional em `local`, desabilitado em `prod`.
- [ ] Todo item do checklist acima confirmado manualmente.

## Validação

```bash
cd backend
mvn clean test
```

```txt
http://localhost:8080/swagger-ui/index.html
```

Percorrer o checklist da seção C item a item.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): swagger, testes minimos e validacao final do servidor funcional"
```

## Ao terminar esta etapa

Volte para `00_indice_e_instrucoes.md` e siga os passos finais de `git push`/merge da branch `sprint/02-fundacao-backend` em `develop`. A partir daqui, a Sprint 03 em diante volta a ser executada com Claude/Cowork.
