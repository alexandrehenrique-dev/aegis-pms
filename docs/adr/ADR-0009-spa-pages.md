# ADR-0009 — SPA Servida pelo Spring Boot (Mesma Origem)

## Status

ACCEPTED

## Contexto

O Aegis administra conteúdo; ele **não renderiza páginas públicas**. O painel administrativo (Eirene) é uma **SPA** (Single Page Application). Era preciso decidir como entregar essa SPA: hospedagem separada (CDN/host estático com domínio próprio) ou servida pela própria aplicação backend.

Restrições do MVP: simplicidade de deploy, ausência de equipe de infra dedicada, necessidade de sessão/autenticação consistente com o Keycloak e baixo custo operacional.

## Decisão

A SPA é **desacoplada no código** (projeto/base de código próprios, build próprio) mas **entregue pelo próprio Spring Boot**, na **mesma origem** e no **mesmo deploy**.

Fluxo:

```txt
Frontend source → build → dist/ → empacotado no artefato do backend
Backend (.jar) → serve a API REST + os assets estáticos da SPA na mesma origem
```

O backend faz fallback de rotas não-API para o `index.html` da SPA (client-side routing). A SPA continua consumindo a API REST por contratos JSON.

## Consequências

Positivas:
- **Deploy único** e reprodutível (um artefato, um container).
- **Sem CORS** entre SPA e API (mesma origem).
- **Mesma sessão/cookies**; integração de autenticação Keycloak simplificada.
- Menor custo e menor superfície operacional.

Negativas / trade-offs / limitações:
- Atualizar o frontend exige **rebuild e redeploy** do artefato do backend.
- Frontend e backend compartilham ciclo de release (acoplamento de deploy, não de código).
- Escala de entrega estática limitada à aplicação (sem CDN dedicada no início).

## Alternativas Consideradas

- **Host estático/CDN separada com domínio próprio**: melhor escala e deploy independente do frontend, mas adiciona CORS, gestão de sessão cross-origin, mais infraestrutura e custo — prematuro para o MVP.
- **SSR/Next.js renderizando páginas**: rejeitado porque o Aegis administra conteúdo e não renderiza páginas públicas; frontends externos consomem contratos.

## Evolução Futura

Quando houver necessidade de escala ou de deploy independente do frontend, a SPA pode ser servida por **CDN separada** via novo ADR, mantendo o consumo de contratos por REST e resolvendo CORS/sessão de forma explícita.

## Impactos

- **Backend**: configuração para servir estáticos e fallback de rotas SPA.
- **Frontend (Eirene)**: build gera `dist/` consumido pelo empacotamento do backend.
- **Deploy (Daedalus)**: pipeline empacota frontend + backend em um único artefato/imagem.

## Links Relacionados

- Documento Mestre — Parte V (Estratégia Oficial de Frontend e Entrega da Aplicação) e Parte VI (Deploy).
- ADR-0005 (Keycloak), ADR-0006 (REST First).
