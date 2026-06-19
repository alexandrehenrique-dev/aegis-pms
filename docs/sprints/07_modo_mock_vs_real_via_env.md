# Sprint 07 — Modo standalone (mock) vs. integrado (.env/build)

> Pode ser executada em paralelo com a Sprint 04 — na prática, define o mecanismo de toggle que a Sprint 04 consome.

## Contexto

Requisito explícito do usuário: o frontend deve continuar funcionando **sem backend** (modo atual, mockado) por padrão, mas ao configurar um `.env` ou fazer um build com a flag adequada, passa a consumir o backend real.

## Objetivo

Uma única variável de ambiente decide o modo, sem precisar de dois código-fontes nem branches separadas.

## Tarefas

1. Definir `VITE_API_MODE` com valores `mock` (padrão) ou `real`. Quando não definida, assumir `mock` — isso garante que `npm run dev` sem nenhum `.env` continua funcionando exatamente como hoje, standalone.
2. Em `core/config`, criar `getApiMode()` lendo `import.meta.env.VITE_API_MODE`.
3. Em cada `services/<dominio>Service.ts` (Sprint 04), implementar as duas variantes por trás da mesma interface:
   ```ts
   interface ProductsService {
     list(): Promise<Product[]>;
     create(input: CreateProductInput): Promise<Product>;
   }
   ```
   `mockProductsService` (lê de `mocks/`) e `realProductsService` (chama `apiClient`) implementam a mesma interface; um factory escolhe qual instanciar com base em `getApiMode()`.
4. Criar `.env.example` na raiz de `frontend/` documentando `VITE_API_MODE` e `VITE_API_BASE_URL` (usada só quando `real`).
5. Build de produção (`npm run build`) deve permitir as duas variantes: `vite build` (padrão, lê `.env`) e um modo explícito documentado no README do frontend (ex.: `VITE_API_MODE=real VITE_API_BASE_URL=https://... npm run build`).
6. Indicador visual discreto (ex.: badge no rodapé do AppShell, visível só em `dev`) mostrando se o app está em modo mock ou real — ajuda QA e evita confusão.

## Critérios de aceite

- [ ] Clonar o repositório, rodar `npm install && npm run dev` sem nenhum `.env` → app funciona 100% mockado, como hoje.
- [ ] Criar `.env` com `VITE_API_MODE=real` e `VITE_API_BASE_URL` apontando para o backend da Sprint 02 → app passa a consumir dados reais, sem mudança de código.
- [ ] Nenhum componente de UI sabe se está em modo mock ou real — a decisão fica inteiramente em `services/`.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/07-mock-vs-real-env
git commit -m "feat(config): adiciona VITE_API_MODE com fallback para mock"
git commit -m "feat(services): implementa variantes mock/real por interface comum"
git commit -m "docs(frontend): adiciona .env.example e instrucoes de build real vs mock"
git push -u origin sprint/07-mock-vs-real-env
git checkout develop && git merge --no-ff sprint/07-mock-vs-real-env && git push origin develop
```
