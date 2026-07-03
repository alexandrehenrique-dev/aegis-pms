# Aegis PMS — Frontend

Frontend do Aegis PMS: plataforma multi-produto, multi-tenant e orientada a contratos para administração de produtos digitais.

## Stack

- [React 18](https://react.dev/)
- [Vite 6](https://vitejs.dev/)
- [TailwindCSS 4](https://tailwindcss.com/)
- [Radix UI](https://www.radix-ui.com/) + [MUI](https://mui.com/) (componentes base)
- [motion](https://motion.dev/) (animações)
- [react-dnd](https://react-dnd.github.io/react-dnd/) (drag-and-drop, ex.: quadro de workflow editorial)
- [react-router](https://reactrouter.com/) v7 (roteamento)

## Como rodar

```bash
npm install
npm run dev      # servidor de desenvolvimento
npm run build    # build de produção em dist/
npm run typecheck # checagem de tipos (não bloqueia o build)
```

## Estrutura de pastas

```
src/
├── app/
│   ├── providers/   # ThemeProvider, AuthProvider, etc.
│   ├── routes/      # definição de rotas React Router
│   ├── layouts/     # AppShell (Header+Sidebar+Workspace), AuthLayout
│   ├── guards/       # PermGate, RequireAuth, RequireRole
│   └── bootstrap/    # ponto de entrada chamado por main.tsx
│
├── core/
│   ├── auth/            # login, seleção de tenant/produto, convite, recuperação de senha
│   ├── permissions/      # roles, capabilities, simulador de papéis
│   ├── tenants/          # estado do tenant atual, switcher
│   ├── products/         # estado do produto atual, switcher
│   ├── notifications/     # toasts (sonner), feedback modal
│   ├── analytics/         # telemetria de eventos
│   └── config/            # leitura de env/flags
│
├── domains/
│   ├── dashboard/
│   ├── products/
│   ├── content/
│   ├── assets/
│   ├── forms/
│   ├── analytics/
│   ├── knowledge/
│   ├── settings/
│   ├── users/
│   └── audit/
│       # cada domínio segue: pages/ components/ services/ hooks/ store/ routes/ contracts/ mocks/ tests/
│
├── shared/
│   ├── components/  # shadcn/ui e componentes reutilizáveis entre domínios
│   ├── hooks/
│   ├── utils/
│   ├── constants/
│   ├── types/
│   └── validations/
│
└── mocks/           # dados mockados cross-domain (timeline, busca global, etc.)
```

Mais contexto arquitetural em `docs/implementation/011_aegis_pms_frontend_architecture_blueprint.md` na raiz do repositório.

## Padrão de qualidade (obrigatório antes de commitar)

```bash
npm run lint       # ESLint — deve terminar com 0 erros e 0 warnings
npm run typecheck  # tsc -b --noEmit — deve terminar sem erros
npm run build      # não roda ESLint sozinho; não substitui o lint acima
```

`npm run build` **não** roda ESLint — passar no build não significa que o lint está limpo. Os três comandos são gates independentes de toda sprint frontend.

Padrões recorrentes de warning do ESLint (`react-refresh/only-export-components`, `react-hooks/exhaustive-deps`) e como corrigi-los sem suprimir a regra estão documentados em `AGENTS.md`, Seção 4.1, na raiz do repositório.
