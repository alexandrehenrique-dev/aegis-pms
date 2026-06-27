# Etapa 19 — Build do frontend React servido pelo Spring Boot (SPA same-origin)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 05 concluída (Resource Server funcionando). Pode ser feita em paralelo com as etapas 06-17 (não depende dos domínios de produto) — só precisa do Resource Server no ar.

## Contexto fixo

**Importante — isto substitui as Features 017/018 do roteiro original (`implementation/001`):** aquele documento descreve criar um frontend Angular. Isso está superado por `docs/adr/ADR-0011-frontend-framework-react.md` — o frontend oficial do Aegis é **React**, já existe em `frontend/` (Vite + Tailwind), e não deve ser recriado em Angular. O que continua válido do roteiro original é a estratégia de servir a SPA pelo Spring Boot na mesma origem, sem CORS, compartilhando sessão/cookies (ver `docs/adr/ADR-0009-spa-pages.md`) — só que aplicada ao build do React em vez do Angular.

## Objetivo

O Spring Boot serve o build de produção do React (`frontend/dist/`) na mesma origem do backend, com fallback de rotas para `index.html`, sem interferir nas rotas de API.

## Tarefas

### A. Build do frontend e cópia para o backend

Já existe um script versionado para isso — use-o em vez de rodar os comandos manualmente:

```bash
cd frontend
npm run build:backend
# builda o frontend (vite build) e copia frontend/dist/ para
# backend/src/main/resources/static/, criando o diretório se necessário
```

Equivalente manual (caso o script não esteja disponível neste ambiente):

```bash
cd frontend && npm install && npm run build && cd ..
rm -rf backend/src/main/resources/static/*
cp -R frontend/dist/* backend/src/main/resources/static/
```

### C. Configuração de fallback de rotas no Spring Boot

Regras:

- `/api/v1/**` nunca cai no fallback da SPA.
- `/actuator/**` nunca cai no fallback da SPA.
- Qualquer outra rota (ex.: `/products`, `/knowledge/graph`) que não exista como arquivo estático deve retornar `index.html`, para o roteador do React (Sprint 01, fora do GPT) assumir a navegação client-side.
- Assets estáticos (`/assets/**`, `.js`, `.css`, etc.) continuam servidos normalmente.

Implementar via um `WebMvcConfigurer`/`Controller` de fallback que não intercepte `/api` nem `/actuator`.

### D. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Sem entidade/repository nesta etapa — a classe com lógica é o `Controller`/`WebMvcConfigurer` de fallback. 100% de cobertura nele mesmo assim: testar que `/api/v1/**` e `/actuator/**` nunca caem no fallback, e que qualquer outra rota cai.
- Entregar em uma rodada só (não há entity/mapper/service aqui): `SpaFallbackController` (ou `WebMvcConfigurer` equivalente) + teste `@WebMvcTest` cobrindo os 4 cenários dos Critérios de aceite.

## Critérios de aceite

- [ ] `npm run build` do frontend gera `dist/` sem erros.
- [ ] Backend serve `index.html` em `/`.
- [ ] Refresh em uma rota não-raiz (ex.: `/algumarota`) também retorna `index.html`, não 404.
- [ ] `/api/v1/me` continua sendo API (401 sem token, JSON com token).
- [ ] `/actuator/health` continua respondendo JSON, não HTML.
- [ ] `mvn clean verify` confirma 100% de cobertura no controller/configurer de fallback (JaCoCo).

## Validação

> **Entrega via collection Bruno, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os como requests `.bru` na pasta numerada desta etapa em `bruno/` (collection cumulativa, autenticação herdada via header `Authorization: Bearer {{token}}` definido em `collection.bru`) e valide a collection inteira via `npx @usebruno/cli run --env local`.

```bash
cd frontend && npm run build && cd ..
rm -rf backend/src/main/resources/static/*
cp -R frontend/dist/* backend/src/main/resources/static/
cd backend
mvn spring-boot:run
```

```bash
curl http://localhost:8080/
curl http://localhost:8080/qualquer-rota-spa
curl http://localhost:8080/api/v1/me
curl http://localhost:8080/actuator/health
```

Esperado: as duas primeiras retornam HTML (o `index.html`); a terceira retorna 401 sem token; a quarta retorna JSON.

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): serve build do frontend react na mesma origem (adr-0009, adr-0011)"
```
