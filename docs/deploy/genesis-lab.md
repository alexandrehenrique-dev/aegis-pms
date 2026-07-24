# Deploy do Aegis PMS no Genesis Lab

Este é o runbook canônico do build e deploy do Aegis PMS no Genesis Lab. A SPA
React/Vite continua empacotada no JAR Spring Boot e servida na mesma origem,
conforme o ADR-0009.

## Configuração atual e promoção futura

Hoje a branch implantada é `develop`.

A variável canônica é a variável de repositório do GitHub Actions:

```text
AEGIS_DEPLOY_BRANCH=develop
```

Ela fica em **Settings → Secrets and variables → Actions → Variables**. O
workflow escuta `develop` e `release`, mas só implanta o push cuja branch for
igual a `AEGIS_DEPLOY_BRANCH`.

Quando `release` se tornar o ambiente implantado, a troca será:

```text
AEGIS_DEPLOY_BRANCH=release
```

Não se altera o workflow nem o script. Antes da promoção real, o arquivo externo
`/opt/genesis-lab/services/aegis-pms/secrets/.env.release` precisa existir com a
configuração daquele ambiente. O mecanismo não cria um ambiente de homologação
fictício.

Alterar a variável não dispara um workflow sozinho. Depois da troca, faça push
em `release` ou execute **Actions → CI/CD → Run workflow**, selecionando
`release`. O job de deploy recusa qualquer branch diferente da variável.

`IMAGE_TAG` é configurável, mas o fluxo oficial não a define separadamente: o
script deriva a tag de `AEGIS_DEPLOY_BRANCH`. Assim, hoje gera
`aegis-pms:develop` e futuramente gerará `aegis-pms:release`. Em uma execução
manual especial, `IMAGE_TAG` pode ser exportada explicitamente.

## Arquitetura do deploy

```text
push em develop ou release
        |
        v
GitHub Actions valida branch == AEGIS_DEPLOY_BRANCH
        |
        v
frontend typecheck/lint + backend verify + stack efêmera Bruno
        |
        v
scripts/deploy-aegis.sh no runner genesis-lab
        |
        +--> build Docker da SPA e do backend
        +--> Docker Compose recria aegis-pms-prod
        +--> healthcheck do container e endpoint público
        +--> preflight CORS e login inválido
        +--> rollback automático se a ativação falhar
```

O PostgreSQL do Aegis e o PostgreSQL do Keycloak permanecem externos, dedicados
e persistentes. O deploy só recria o serviço `aegis-backend`.

## Runner self-hosted no Genesis Lab

No GitHub, abra **Settings → Actions → Runners → New self-hosted runner** e
selecione Linux/arquitetura do servidor. No Genesis Lab, execute os comandos de
download e verificação apresentados pelo próprio GitHub em um diretório
dedicado, por exemplo:

```bash
id github-runner >/dev/null 2>&1 \
  || sudo useradd --create-home --shell /bin/bash github-runner
sudo install -d -o github-runner -g github-runner /opt/actions-runner/aegis-pms
sudo -u github-runner -H bash
cd /opt/actions-runner/aegis-pms
# execute aqui download e extração fornecidos pela tela do GitHub
```

Na etapa de configuração, use o token efêmero mostrado pelo GitHub e acrescente
nome e label estáveis:

```bash
./config.sh \
  --url https://github.com/alexandrehenrique-dev/aegis-pms \
  --token <TOKEN_EFEMERO_FORNECIDO_PELO_GITHUB> \
  --name genesis-lab-aegis \
  --labels genesis-lab \
  --work _work \
  --unattended
```

O label `genesis-lab` é obrigatório porque todos os jobs usam:

```yaml
runs-on: [self-hosted, genesis-lab]
```

Instale o runner como serviço com o usuário dedicado, usando o `svc.sh` incluído
no pacote:

```bash
exit
cd /opt/actions-runner/aegis-pms
sudo ./svc.sh install github-runner
sudo ./svc.sh start
sudo ./svc.sh status
```

Pré-requisitos do usuário do serviço:

- acesso ao daemon Docker e ao plugin Docker Compose;
- `git`, `curl`, `awk`, `grep`, `sed` e `seq`;
- Java é preparado pelo workflow e o Maven é baixado pelo wrapper binário
  versionado em `backend/mvnw`; não é necessário instalar Maven nem `unzip`
  globalmente no runner;
- Node 22 é preparado separadamente nos jobs de frontend e Bruno; o Node global
  do servidor não é usado por essas etapas;
- leitura do arquivo
  `/opt/genesis-lab/services/aegis-pms/secrets/.env.develop`;
- acesso aos diretórios bind-mounted de runtime;
- saída HTTPS para GitHub, registries Docker, Maven e npm.

Em distribuições que usam o grupo `docker`, adicione o usuário e reinicie o
serviço para aplicar a associação:

```bash
sudo usermod -aG docker github-runner
sudo ./svc.sh stop
sudo ./svc.sh start
```

Não execute o runner como root. Restrinja o arquivo `.env.develop` ao usuário ou
grupo do runner e nunca o coloque dentro de `_work`.

No GitHub Actions, configure:

```text
AEGIS_DEPLOY_BRANCH=develop
```

Opcionalmente configure `AEGIS_ENV_FILE` apenas se o caminho real for diferente
do default. Não crie uma variável `IMAGE_TAG` no GitHub: o script a deriva da
branch, garantindo a troca simples.

Para homologar `release` no futuro:

1. prepare o arquivo externo `.env.release`;
2. altere somente `AEGIS_DEPLOY_BRANCH` para `release`;
3. faça push em `release` ou rode manualmente o workflow nessa branch;
4. confirme que o log final mostra `branch=release image=aegis-pms:release`.

## Secrets e variáveis externas

O arquivo atual é:

```text
/opt/genesis-lab/services/aegis-pms/secrets/.env.develop
```

Ele não é versionado, não é copiado para artifacts e nunca deve ser exibido
integralmente em logs. A variável opcional de repositório `AEGIS_ENV_FILE` pode
sobrescrever o caminho; quando ausente, o script deriva o caminho da branch.
Tanto o hardening do Keycloak quanto o deploy usam esse caminho canônico.

As variáveis não sensíveis obrigatórias são:

```dotenv
SPRING_PROFILES_ACTIVE=prod
KEYCLOAK_URL=https://auth.buildyourownpath.io
KEYCLOAK_RESOLVE_IP=10.200.0.1
AEGIS_APP_BASE_URL=https://aegis.byop.dev
AEGIS_APP_CORS_ALLOWED_ORIGINS=https://aegis.byop.dev
```

`KEYCLOAK_URL` é a URL-base pública usada pelo `kcadm` no hardening do realm.
Ela não inclui `/realms/aegis-pms`. O workflow fornece o `AEGIS_ENV_FILE` ao
script de hardening por `--env-file`; não é necessário duplicar URL,
credenciais administrativas ou SMTP em GitHub Actions Variables/Secrets. O
carregamento aceita apenas uma lista explícita de chaves e não executa o
arquivo como shell script. O `kcadm` é executado em um container efêmero da
imagem oficial `quay.io/keycloak/keycloak:26.0`; não é necessário instalar o
CLI do Keycloak no Genesis Lab. O repositório e o arquivo operacional são
montados somente para leitura, e o container é removido ao terminar.

`KEYCLOAK_RESOLVE_IP` é opcional. No Genesis Lab ele direciona somente o
container de hardening para o IP WireGuard `10.200.0.1` por meio de
`docker --add-host`. A URL continua usando `auth.buildyourownpath.io`, portanto
hostname, SNI e validação do certificado TLS são preservados. Não substitua
`KEYCLOAK_URL` por uma URL contendo o IP.

Múltiplas origens usam lista separada por vírgula:

```dotenv
AEGIS_APP_CORS_ALLOWED_ORIGINS=https://aegis.byop.dev,https://outra-origem.example
```

O Compose declara essas variáveis em `environment:`. `--env-file` fornece
valores para interpolação do Compose; ele não é tratado como injeção automática
de todas as chaves no container.

Se for necessário adicionar a origem atual ao arquivo operacional:

```bash
sudo cp \
  /opt/genesis-lab/services/aegis-pms/secrets/.env.develop \
  /opt/genesis-lab/services/aegis-pms/secrets/.env.develop.bak

sudo sh -c \
  'printf "%s\n" "AEGIS_APP_CORS_ALLOWED_ORIGINS=https://aegis.byop.dev" >> /opt/genesis-lab/services/aegis-pms/secrets/.env.develop'
```

Antes de anexar, confirme apenas a existência da chave para evitar duplicidade:

```bash
sudo grep -q '^AEGIS_APP_CORS_ALLOWED_ORIGINS=' \
  /opt/genesis-lab/services/aegis-pms/secrets/.env.develop
```

Esses comandos não exibem as demais variáveis. A alteração operacional só deve
ser feita no servidor depois de confirmar o caminho real.

## Configuração CORS

`AegisAppProperties` faz o binding tipado e imutável de:

- `aegis.app.base-url`;
- `aegis.app.cors-allowed-origins`.

Somente origens HTTP(S) explícitas, sem caminho e sem wildcard, são aceitas. A
aplicação mantém `allowCredentials(true)`, suporta múltiplas origens e falha na
inicialização quando a configuração viola a validação. O default local é
`http://localhost:5173`; o default de `prod` é `https://aegis.byop.dev`.

Na inicialização, um único log INFO registra profiles ativos, URL pública,
origens CORS e modo de ambiente. Senhas, tokens, client secrets e credenciais
não entram nesse log.

## Build reproduzível do frontend

O Dockerfile define valores explícitos:

```text
VITE_API_MODE=api
VITE_API_BASE_URL=/api/v1
VITE_KEYCLOAK_URL=https://auth.buildyourownpath.io
VITE_KEYCLOAK_REALM=aegis-pms
VITE_KEYCLOAK_CLIENT_ID=aegis-web
```

Esses valores são build arguments configuráveis, não secrets. O build não
depende de `.env` local, e `.dockerignore` exclui arquivos `.env` do contexto.
Uma verificação no Dockerfile falha se o bundle contiver
`http://localhost:8080/api/v1`.

## Execução manual controlada

No checkout limpo e sincronizado:

```bash
AEGIS_DEPLOY_BRANCH=develop \
AEGIS_ENV_FILE=/opt/genesis-lab/services/aegis-pms/secrets/.env.develop \
./scripts/deploy-aegis.sh
```

O script:

1. valida repositório, branch, árvore limpa, dependências, Docker e arquivos;
2. faz `fetch` e apenas atualização `fast-forward`;
3. recusa commits locais não enviados ou divergência;
4. preserva a imagem atual com tag `rollback-develop`;
5. constrói `aegis-pms:develop` sem cache;
6. valida o Compose e recria somente `aegis-backend`;
7. aguarda o healthcheck;
8. compara o ID da imagem do container com o ID da tag construída;
9. valida `/actuator/health`, preflight e login inválido;
10. restaura a imagem anterior se uma validação pós-ativação falhar.

O script nunca usa `git reset --hard`, nunca carrega o arquivo de secrets como
shell script e nunca imprime seu conteúdo.

## Diagnóstico

```bash
docker ps --filter name=aegis-pms-prod
docker inspect aegis-pms-prod \
  --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}'
docker inspect aegis-pms-prod --format '{{.Image}}'
docker image inspect aegis-pms:develop --format '{{.Id}}'
docker logs --tail 100 aegis-pms-prod
curl -fsS https://aegis.byop.dev/actuator/health
```

Teste preflight:

```bash
curl -i -X OPTIONS \
  https://aegis.byop.dev/api/v1/auth/login \
  -H 'Origin: https://aegis.byop.dev' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type'
```

Teste de alcance do login, sem credencial real:

```bash
curl -i -X POST \
  https://aegis.byop.dev/api/v1/auth/login \
  -H 'Origin: https://aegis.byop.dev' \
  -H 'Content-Type: application/json' \
  --data '{"username":"invalid@example.invalid","password":"invalid"}'
```

O resultado não pode ser `403 Invalid CORS request` nem sucesso.

## Rollback e contingência

Falhas posteriores à recriação acionam rollback automático para
`aegis-pms:rollback-develop`. Para contingência manual:

```bash
IMAGE_TAG=rollback-develop \
docker compose \
  -f infra/docker-compose.prod.yml \
  --env-file /opt/genesis-lab/services/aegis-pms/secrets/.env.develop \
  up -d --force-recreate aegis-backend
```

Depois, valide o ID da imagem e o healthcheck público. Rollback de imagem não
reverte migrations Flyway; mudanças de banco precisam de plano próprio.

Se a automação estiver indisponível, o mesmo `scripts/deploy-aegis.sh` é o
procedimento manual preferido. Só use os comandos Docker isolados acima para
recuperação de uma imagem já construída.

## Checklist de segurança pós-validação

- testar o login real manualmente no navegador, sem registrar a senha;
- confirmar que a chamada é `POST /api/v1/auth/login`;
- confirmar que o frontend usa `/api/v1`;
- revogar no Keycloak todas as sessões atuais do usuário
  `aegis@buildyourownpath.io`, pois tokens reais apareceram no diagnóstico;
- confirmar que novos access/refresh tokens são exigidos após a revogação;
- não copiar tokens, senhas ou o arquivo `.env.develop` para logs ou artifacts.

A revogação é feita no Admin Console do Keycloak em **Users → usuário →
Sessions → Logout all sessions**, depois da validação do login. Ela é uma ação
operacional deliberada e não é automatizada com credenciais administrativas no
repositório.
