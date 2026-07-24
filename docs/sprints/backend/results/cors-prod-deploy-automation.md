# Correção de CORS em produção e automação do deploy

Executada em 2026-07-24 diretamente na branch `develop`, por exceção explícita
do responsável pelo projeto.

## Objetivo

Corrigir o bloqueio CORS do login em `https://aegis.byop.dev`, tornar o build da
SPA reproduzível, automatizar o deploy no Genesis Lab e deixar a futura troca de
`develop` para `release` controlada por uma única variável.

## Diagnóstico e causa raiz

Antes da alteração:

- `GET /actuator/health`: HTTP 200, `status=UP`;
- login sem `Origin`: alcançava o controller e o Keycloak;
- preflight com `Origin: https://aegis.byop.dev`: HTTP 403;
- login inválido com a mesma origem: HTTP 403, `Invalid CORS request`.

`SecurityConfig` usava `@Value` para injetar
`aegis.app.cors-allowed-origins` como `List<String>`. A lista YAML era exposta
como propriedades indexadas, enquanto o placeholder consultava uma propriedade
escalar. No profile `prod`, isso permitia cair no default local sem erro de
inicialização. O bloqueio ocorria no filtro CORS antes do controller.

## Decisões

- `AegisAppProperties` usa `@ConfigurationProperties(prefix = "aegis.app")`.
- A lista é defensiva, imutável e validada.
- Origens aceitam apenas HTTP(S) explícito, sem caminho ou wildcard.
- Múltiplas origens por variável de ambiente usam valores separados por vírgula.
- `allowCredentials(true)` foi preservado sem aceitar `"*"`.
- Defaults: localhost no ambiente local e `https://aegis.byop.dev` em `prod`.
- `shared.config` foi exposto como `NamedInterface` para preservar o Modulith.
- Um log INFO único registra profiles, base URL, origens e modo, sem secrets.
- O Dockerfile define build arguments Vite de produção e recusa o endpoint
  localhost no bundle.
- `.dockerignore` exclui `.env` e diretórios de secrets do contexto.
- `AEGIS_DEPLOY_BRANCH` é a variável canônica; `IMAGE_TAG` deriva dela.
- O workflow observa `develop` e `release`, mas implanta apenas a branch
  configurada, por push ou acionamento manual. Hoje: `develop`. Futuro:
  `release`.
- O script preserva a imagem anterior e executa rollback automático se health,
  ID da imagem, CORS ou login inválido falharem.
- `gitflow-finish-sprint.sh` ganhou suporte retrocompatível e seguro à exceção
  direta em `develop`, sem merge da branch nela mesma nem exclusão remota.

## Arquivos criados

- `backend/src/main/java/br/com/byop/aegis/shared/config/AegisAppProperties.java`
- `backend/src/main/java/br/com/byop/aegis/shared/config/AegisApplicationConfig.java`
- `backend/src/main/java/br/com/byop/aegis/shared/config/package-info.java`
- `backend/src/test/java/br/com/byop/aegis/shared/config/AegisAppPropertiesTest.java`
- `docs/deploy/genesis-lab.md`
- `scripts/deploy-aegis.sh`
- `docs/sprints/backend/results/cors-prod-deploy-automation.md`

## Arquivos alterados

- `.dockerignore`, `.env.example`, `.github/workflows/ci.yml`, `AGENTS.md`;
- `backend/Dockerfile`, `SecurityConfig`, `application.yml`,
  `application-prod.yml` e `SecurityConfigTest`;
- `frontend/src/shared/services/apiClient.ts`;
- `infra/.env.prod.example` e `infra/docker-compose.prod.yml`;
- `scripts/gitflow-finish-sprint.sh`;
- documentação de deploy e `docs/sprints/backend/SPRINT-RESULTADO.md`.

## Testes e comandos executados

### Backend

Primeira tentativa de `mvn clean verify`: falhou com 391 erros porque o
PostgreSQL local em `localhost:5434` estava parado.

Segunda tentativa, com o banco ativo: falhou com 246 erros de slice tests porque
`SecurityConfig` era importado isoladamente sem registrar
`AegisAppProperties`.

Terceira tentativa: 1 erro no `ModulithArchitectureTest`, pois `shared.config`
ainda não era uma interface pública do módulo.

Após as correções:

```text
Tests run: 1681, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

O teste direcionado de CORS/binding passou: 14 testes, zero falhas.

### Frontend

```text
npm ci                 concluído
npm run typecheck      aprovado
npm run lint           aprovado, zero warnings
npm run build          aprovado em modo API
scan do bundle         endpoint localhost ausente
```

Em 2026-07-24, o lockfile foi atualizado sem mudança de dependências diretas:
`brace-expansion` passou para `1.1.16`/`2.1.2`, `js-yaml` para `4.3.0` e `tar`
para `7.5.21`. `npm audit` retorna zero vulnerabilidades, e o workflow executa
`npm audit --audit-level=high` após o `npm ci`.

Após a instalação do runner no Genesis Lab, o primeiro `backend-verify` revelou
que o job dependia implicitamente de PostgreSQL já ativo em `localhost:5434`.
O workflow agora cria um PostgreSQL 16 efêmero e saudável para o `mvn verify`,
sem acesso ao banco de produção.

Na execução seguinte, o PostgreSQL ficou saudável, mas o passo `Verify` encerrou
com código 127 e `mvn: command not found`: `actions/setup-java` prepara o JDK e
o cache, não instala o Maven. O backend agora inclui o Maven Wrapper 3.9.16 em
`backend/mvnw`, fixa o SHA-256 da distribuição e o workflow usa
`./mvnw -B clean verify`, sem depender de Maven global no host.

O modo inicial `only-script` revelou uma segunda dependência implícita: sem
`unzip`, o script troca o artefato Maven de `.zip` para `.tar.gz`, mas conserva
o SHA-256 configurado para o ZIP. O runner então falhava na validação antes de
iniciar o Maven. A correção definitiva usa o wrapper `bin`, versiona o JAR
oficial 3.3.4 com SHA-256 fixado e mantém o checksum do ZIP do Maven 3.9.16. A
execução não depende mais de Maven ou `unzip` instalados no host.

A correção foi reproduzida em um container Linux `eclipse-temurin:25-jdk` sem
`unzip`: `./mvnw --version` baixou e iniciou o Maven 3.9.16 com sucesso. Em
seguida, `./mvnw -B clean verify` concluiu com 1681 testes, zero falhas/erros,
JaCoCo aprovado e `BUILD SUCCESS`.

Na primeira execução completa no runner, o Maven iniciou corretamente e revelou
uma dependência externa oculta em `MeControllerTest`: o cenário de token
inválido tentava descobrir a configuração OpenID em
`http://localhost:8282/realms/aegis`. O teste web passou a substituir o
`JwtDecoder` e simular `BadJwtException`, comprovando a resposta 401 sem exigir
Keycloak ativo.

Após o backend passar, o job Bruno expôs dois problemas específicos do runner:
o CLI era iniciado pelo Node 18.19.1 global, sem a Web Crypto API esperada pelo
Bruno 4.0.0, e o MailHog efêmero tentava publicar a porta 8025 já usada no
Genesis Lab. O job passou a configurar Node 22, validar `crypto.subtle` e fixar
o Bruno CLI 4.0.0. Backend e API do MailHog de CI agora usam portas efêmeras
limitadas a `127.0.0.1`, e o Keycloak de CI usa a porta dedicada 18282. O
workflow descobre os endpoints e injeta `baseUrl`, `mailhogBaseUrl` e
`keycloakIssuer` na collection, sem alterar o SMTP interno entre containers.
As capturas de tokens de convite e reset agora fazem polling limitado pela
mensagem do destinatário esperado, pois o envio assíncrono podia fazer a
primeira consulta ao MailHog retornar `items: []`. A execução completa
equivalente ao runner terminou com 337/337 requisições e 650/650 testes
aprovados.

No primeiro deploy automatizado, o hardening foi interrompido antes do `kcadm`
porque `KEYCLOAK_URL` estava referenciada no GitHub, mas o valor existia apenas
no `.env.develop`. A correção definitiva fez o workflow resolver o mesmo
`AEGIS_ENV_FILE` do deploy e fornecê-lo ao hardening com `--env-file`. O script
carrega apenas uma lista permitida de chaves, sem `source`, sem executar o
arquivo e sem imprimir segredos. O realm padrão foi corrigido para
`aegis-pms`, e os exemplos de ambiente foram alinhados com o endpoint público
real. O workflow executa o hardening em um container efêmero da imagem oficial
do Keycloak, com repositório e arquivo operacional montados somente para
leitura, evitando depender de `kcadm` instalado no runner.

### Docker, Compose e scripts

```text
docker build --no-cache -f backend/Dockerfile -t aegis-pms:develop .
BUILD SUCCESS
image ID: sha256:ab865e71a1b7a8ed900e5d2cb89399183a7483ef248c7e654594159d92574e81
docker compose ... config --quiet: aprovado
bash -n scripts/deploy-aegis.sh scripts/gitflow-finish-sprint.sh: aprovado
workflow YAML: sintaticamente válido
git diff --check: aprovado
```

O teste seguro do deploy com árvore suja abortou antes de fetch/build/deploy,
confirmando a proteção contra alterações locais.

## Configuração atual

```text
AEGIS_DEPLOY_BRANCH=develop
IMAGE_TAG=develop (derivada)
SPRING_PROFILES_ACTIVE=prod
AEGIS_APP_BASE_URL=https://aegis.byop.dev
AEGIS_APP_CORS_ALLOWED_ORIGINS=https://aegis.byop.dev
VITE_API_MODE=api
VITE_API_BASE_URL=/api/v1
VITE_KEYCLOAK_URL=https://auth.buildyourownpath.io
VITE_KEYCLOAK_REALM=aegis-pms
VITE_KEYCLOAK_CLIENT_ID=aegis-web
```

O arquivo operacional esperado é
`/opt/genesis-lab/services/aegis-pms/secrets/.env.develop`. Ele não existe na
máquina local de desenvolvimento e não foi alterado.

## Deploy e validação pública

O push de `develop`, feito exclusivamente por
`scripts/gitflow-finish-sprint.sh`, aciona o workflow no runner
`self-hosted/genesis-lab`. O job oficial executa build, recriação, comparação de
IDs, healthcheck, preflight e login inválido. As evidências públicas
pós-deploy são apresentadas no relatório final da execução.

O login real permanece validação manual no navegador. Nenhuma senha real foi
automatizada.

## Riscos e pendências

- Revogar no Keycloak todas as sessões do usuário
  `aegis@buildyourownpath.io` depois da validação manual, pois tokens reais
  apareceram durante o diagnóstico.
- Criar `.env.release` apenas quando o ambiente `release` for realmente
  promovido; o script aborta com clareza enquanto o arquivo não existir.

## Rollback

O script marca a imagem anterior como `aegis-pms:rollback-develop`. Se uma
validação pós-ativação falhar, recria automaticamente `aegis-backend` com essa
imagem e aguarda o healthcheck. O procedimento manual está documentado em
`docs/deploy/genesis-lab.md`.

Rollback de imagem não reverte migrations Flyway.
