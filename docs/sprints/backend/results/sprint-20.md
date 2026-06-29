# Sprint 20 — Build do Frontend React servido pelo Spring Boot

Concluída em 2026-06-29 na branch `sprint/20-build-frontend-react-no-backend`.

## Objetivo

Servir a SPA React pelo próprio Spring Boot, mesma origem, preservando os contratos REST, o React Router, `/api/**`, `/actuator/**` e os assets estáticos. A sprint também corrigiu o apontamento SonarQube for IDE `java:S6856` no fallback SPA.

## Resultado alcançado

O build do Vite foi padronizado para gerar `frontend/dist` e o script `npm run build:backend` copia o resultado para `backend/src/main/resources/static`. O backend serve `index.html` para rotas de cliente e não intercepta APIs, actuator nem arquivos com extensão. A suíte Java, a coleção Bruno e o build frontend ficaram verdes.

## Classes criadas

Nenhuma classe de produção nova foi criada. A entrega reutilizou o módulo `system` existente para o fallback SPA.

## Classes alteradas

- `br.com.byop.aegis.system.controller.SpaFallbackController`
- `br.com.byop.aegis.security.SecurityConfig`
- `br.com.byop.aegis.system.controller.SpaFallbackControllerTest`

## Arquivos criados

- `bruno/20-spa-same-origin/folder.bru`
- `bruno/20-spa-same-origin/spa-root-index.bru`
- `bruno/20-spa-same-origin/spa-rota-client-side.bru`
- `bruno/20-spa-same-origin/api-me-sem-token-401.bru`
- `bruno/20-spa-same-origin/actuator-health-json.bru`
- `docs/sprints/backend/results/sprint-20.md`

## Arquivos alterados

- `frontend/vite.config.ts`
- `frontend/package.json`
- `frontend/package-lock.json`
- `.gitignore`
- `AGENTS.md`
- `docs/api-testing/README.md`
- `docs/sprints/backend/SPRINT-RESULTADO.md`
- `bruno/16-audit/sem-endpoint-de-escrita.bru`

## Arquivos removidos

Nenhum arquivo versionado foi removido.

## Arquivos estáticos gerados

- `frontend/dist/`
- `backend/src/main/resources/static/`

Esses diretórios são artefatos gerados localmente por `npm run build:backend` e permanecem ignorados pelo git. O build versionado fica representado pelo script, pela configuração do Vite e pelo lockfile de dependências.

## Bruno criado/alterado

Criada a pasta `bruno/20-spa-same-origin`:

- `spa-root-index.bru`
- `spa-rota-client-side.bru`
- `api-me-sem-token-401.bru`
- `actuator-health-json.bru`

O cenário `bruno/16-audit/sem-endpoint-de-escrita.bru` foi alinhado ao comportamento real atual da aplicação: `POST` em endpoint somente `GET` retorna 405 com `Allow: GET`, provando ausência de handler de escrita.

## Endpoints e rotas confirmados

Rotas SPA confirmadas como HTML/`index.html`:

- `GET /`
- `GET /dashboard`
- `GET /products`
- `GET /qualquer-rota`
- `GET /qualquer/rota/client-side`

Rotas preservadas fora do fallback:

- `GET /api/v1/me`
- `GET /actuator/health`
- `GET /assets/index.js`

## Decisões de implementação

- O fallback SPA usa `@GetMapping` com padrões finitos de profundidade para rotas client-side.
- O primeiro segmento `api` e `actuator` é excluído do fallback.
- O último segmento com ponto é excluído para não capturar assets como JavaScript, CSS, imagens e fontes.
- `SecurityConfig` passou a proteger todo `/api/**`, não apenas `/api/v1/**`.
- Rotas não-API são públicas para permitir o carregamento da SPA mesma origem.
- `/actuator/**` permanece fora do fallback e é atendido pelo Actuator.
- O build gerado em `frontend/dist` e a cópia para `backend/src/main/resources/static` continuam como artefatos locais ignorados pelo git.

## Regras preservadas

- Contratos REST existentes não foram alterados.
- React Router continua responsável por resolver rotas client-side.
- `/api/**` e `/api/v1/**` continuam fora do fallback.
- `/actuator/**` continua fora do fallback.
- Assets estáticos continuam servidos pelo `ResourceHttpRequestHandler`.
- Nenhum CORS foi introduzido; a SPA permanece same-origin conforme ADR-0009.
- O frontend continua React/Vite, conforme ADR-0011.

## SonarQube for IDE

Corrigido o apontamento `java:S6856` no `SpaFallbackController` com binding explícito:

- `@PathVariable(name = "path", required = false)`
- `@PathVariable(name = "segment", required = false)`

Isso remove a dependência de inferência por nome de parâmetro/flag `-parameters` no contrato do controller. Nenhum `@SuppressWarnings`, `NOSONAR` ou desativação de regra foi utilizado. `AGENTS.md` foi atualizado com a recomendação preventiva para controllers Spring MVC.

Observação: o SonarQube for IDE não possui execução CLI neste ambiente; a correção foi aplicada no padrão exigido pela regra e validada por compilação/testes.

## NPM

`npm audit` inicialmente apontou 4 vulnerabilidades transitivas relacionadas a `react-router`, `vite`, `eslint` e `@eslint/plugin-kit`. Foram aplicados upgrades sem major:

- `react-router`: `7.13.0` -> `7.18.0`
- `vite`: `6.3.5` -> `6.4.3`
- `eslint`: `9.15.0` -> `9.39.4`
- `pnpm.overrides.vite`: `6.3.5` -> `6.4.3`

Após os upgrades, `npm audit` retorna `found 0 vulnerabilities`. `npm run typecheck`, `npm run lint` e `npm run build:backend` executaram com sucesso. O lint manteve apenas 14 warnings preexistentes/não bloqueantes de Fast Refresh/hooks.

## Testes

- `SpaFallbackControllerTest` cobre `/`, `/dashboard`, `/products`, `/qualquer-rota`, rota profunda, `/api/v1/me`, `/actuator/health` e `/assets/index.js`.
- A suíte completa do backend passou com JaCoCo e Spring Modulith.
- A collection Bruno completa passou contra a aplicação real em `localhost:8080`.
- O frontend passou em audit, typecheck, lint e build de produção para o backend.

## Comandos executados

- `cd frontend && npm audit`
- `cd frontend && npm install react-router@7.18.0 vite@6.4.3 eslint@9.39.4 --save-exact`
- `cd frontend && npm pkg set pnpm.overrides.vite=6.4.3`
- `cd frontend && npm run typecheck`
- `cd frontend && npm run lint`
- `cd frontend && npm run build:backend`
- `cd backend && mvn -Dtest=SpaFallbackControllerTest test`
- `cd backend && mvn clean verify`
- `cd backend && mvn spring-boot:run`
- `cd bruno && npx @usebruno/cli run --env local`
- `git status`
- `git diff --check`

## Resultado do `mvn clean verify`

Última execução obrigatória:

- Comando: `mvn clean verify`
- Resultado: `BUILD SUCCESS`
- Tests run: 1003
- Failures: 0
- Errors: 0
- Skipped: 0
- JaCoCo: `All coverage checks have been met`
- Spring Modulith: aprovado via suíte completa

## Resultado do Bruno

Última execução obrigatória:

- Backend local: `mvn spring-boot:run` em `localhost:8080`
- Comando: `cd bruno && npx @usebruno/cli run --env local`
- Status: PASS
- Requests: 167 executados, 167 aprovados
- Tests: 327/327 aprovados
- Duration: 24984 ms

## Bugs ou ajustes reais encontrados

- `@PathVariable` sem binding explícito no `SpaFallbackController` acionava SonarQube for IDE `java:S6856`; corrigido sem supressão.
- `npm audit` apontava vulnerabilidades transitivas em `react-router`, `vite`, `eslint` e `@eslint/plugin-kit`; corrigidas por upgrades sem major e lockfile atualizado.
- A expectativa Bruno de `16-audit/sem-endpoint-de-escrita` estava desalinhada do comportamento real atual da aplicação; ajustada para 405 com `Allow: GET`.
- O comentário da `.gitignore` sobre o build do frontend foi ajustado para refletir que `build:backend` copia o build gerado pelo Vite.

## Critérios de aceite

- Finding `java:S6856` corrigido por binding explícito de `@PathVariable`.
- Nenhuma alteração funcional intencional no fallback além da generalização segura para rotas client-side.
- `npm audit` limpo.
- `npm run build:backend` com sucesso.
- `mvn clean verify` com `BUILD SUCCESS`, 0 falhas e 0 erros.
- JaCoCo aprovado.
- Spring Modulith aprovado.
- Bruno 100% verde.
- `/api/**`, `/api/v1/**`, `/actuator/**` e assets preservados fora do fallback.
- `AGENTS.md` atualizado com padrão preventivo.
- Nenhum `@SuppressWarnings`, `NOSONAR` ou desativação de regra foi utilizado nesta correção.

## Retrofits pendentes

- Reexecutar a análise visual do SonarQube for IDE na IDE local para confirmar a remoção do finding `java:S6856`.
