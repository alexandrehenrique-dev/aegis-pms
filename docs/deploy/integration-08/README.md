# Sprint de Integracao 08 - Infra, Deploy e CI/CD

Esta pasta registra a entrega da sprint `docs/sprints/integration/08_infra_deploy.md`.

O objetivo desta sprint e preparar o repositorio para uma implantacao posterior no `genesis-lab`, sem executar a implantacao real antes dos testes totais. A separacao e:

- Implementado no repositorio: compose de producao, exemplo de variaveis, Caddyfile, profile `prod`, CORS parametrizado, workflow GitHub Actions, Dockerfile ajustado, hardening versionado do realm Keycloak e script operacional.
- Preparacao de servidor: criacao de diretorios, secrets reais, instalacao do Caddy, runner self-hosted, Docker/Compose, firewall, DNS/TLS e bancos externos.

Arquivos desta pasta:

- [server-checklist.md](server-checklist.md): checklist do que precisa existir no `genesis-lab` e na maquina Oracle.
- [repository-changes.md](repository-changes.md): mudancas reais feitas no repositorio.
- [environment-variables.md](environment-variables.md): variaveis de ambiente e secrets necessarios.
- [deployment-runbook.md](deployment-runbook.md): ordem recomendada de implantacao.
- [validation-plan.md](validation-plan.md): plano de validacao de build, API, SPA, auth, banco, migrations e health checks.

Decisoes preservadas:

- SPA React/Vite servida pelo Spring Boot na mesma origem.
- Sem CORS em producao para deploy normal; CORS fica parametrizado apenas como protecao operacional.
- PostgreSQL do Aegis e PostgreSQL do Keycloak permanecem dedicados e persistentes.
- Keycloak continua externo ao container de aplicacao em producao.
- `docker-compose.yml` local permanece intocado.
- Frontend nao recebe deploy separado.
