# docs/deploy — Deploy, Ambientes e Entrega Contínua

## Propósito

Documentação da **estratégia de deploy** do Aegis: build, release, ambientes, promotion, rollback e CI/CD.

## Conteúdo esperado

- Definição de ambientes: `local → development → staging → production`.
- Pipeline de build do **backend** (`.jar`) e do **frontend SPA** (`dist/`).
- Estratégia de entrega: **o Spring Boot serve o SPA na mesma origem e no mesmo deploy** (ver `adr/ADR-0009`).
- Docker / Docker Compose como deploy inicial reprodutível.
- Health checks, migrations no deploy, rollback e release notes.

## Princípios

- Deploy é processo controlado de entrega, validação, ativação e recuperação — não cópia de arquivos.
- Deploy deve ser repetível, auditável, observável, seguro e reversível quando possível.
- Mudanças de estratégia de deploy exigem ADR.

## Modelo de entrega do frontend (resumo)

```txt
Frontend source → build → dist/ → empacotado no artefato do Spring Boot
Backend source  → build → .jar (serve API + SPA na mesma origem)
```

Vantagens: sem CORS, mesma sessão, deploy único.
Limitação: atualizar o frontend exige rebuild/redeploy do artefato.
Evolução futura: servir o SPA por CDN separada quando houver necessidade de escala.

## Docker Compose Local Oficial

A partir da Sprint 21, o fluxo oficial para subir a stack local completa é:

```bash
docker compose up -d --build
```

Esse comando sobe:

- PostgreSQL dedicado do Aegis (`aegis-postgres`)
- PostgreSQL dedicado do Keycloak (`keycloak-postgres`)
- Keycloak (`aegis-keycloak`)
- MailHog (`aegis-mailhog`)
- Backend Spring Boot com a SPA React embutida (`aegis-backend`)

Volumes persistentes nomeados:

- `aegis_postgres_data`
- `keycloak_aegis_postgres_data`
- `aegis_assets_data`

O volume `aegis_assets_data` é montado em `/app/assets` no container do backend. O bootstrap Java existente continua criando os diretórios necessários com `Files.createDirectories(...)`; no container, esses diretórios são criados dentro do volume Docker.

O build do backend no Compose também constrói a SPA React em um stage Node e copia o `dist/` para o classpath do Spring Boot antes do empacotamento Maven. A imagem final continua contendo apenas JRE e JAR.

O issuer externo do Keycloak permanece `http://localhost:8282/realms/aegis`. O backend containerizado valida tokens contra esse issuer e usa apenas a URL interna/JWKS container-to-container para buscar chaves e chamar APIs administrativas.

Para parar sem remover dados:

```bash
docker compose down
```

Não use `docker compose down -v` em ambientes com dados que precisam ser preservados, pois esse comando remove os volumes nomeados.

## Preparacao de Producao

A Sprint de Integracao 08 preparou os artefatos iniciais de producao e CI/CD em [integration-08](integration-08/README.md). Essa pasta separa o que ja foi implementado no repositorio do que ainda precisa ser executado no servidor `genesis-lab` e na maquina Oracle antes do primeiro deploy real.

O runbook operacional vigente do ambiente já implantado está em
[genesis-lab.md](genesis-lab.md). Ele substitui os comandos manuais iniciais da
Sprint de Integração 08 quando houver divergência.
