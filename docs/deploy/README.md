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
