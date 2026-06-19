# AGENTS.md — Protocolo de Agentes de IA do Aegis PMS

Este é o arquivo mestre que qualquer agente de IA (Claude, Codex ou outro) deve ler **antes** de tocar em qualquer arquivo deste repositório. Ele define o fluxo de git obrigatório, convenções de commit e branch, e onde buscar contexto antes de agir.

Repositório: `aegis-pms` (remote `git@github.com:alexandrehenrique-dev/aegis-pms.git`).

Se você é um agente de IA agnóstico de ferramenta (não especificamente Claude), leia também `CONTRIBUTING.md` — ele cobre o mesmo fluxo em formato mais curto e ferramenta-agnóstico.

---

## 1. Antes de qualquer mudança

1. Leia este arquivo até o fim.
2. Leia `docs/README.md` para entender a estrutura documental e qual é a fonte de verdade (`docs/AEGIS_CMS_V1.md`).
3. Leia `docs/implementation/016_aegis_constitution.md`. Nenhuma mudança pode violar a Constituição do Aegis.
4. Verifique `docs/adr/README.md` — se a sua tarefa toca em banco, autenticação, framework, contrato público ou modelo multi-tenant, **já existe um ADR sobre isso**. Não decida de novo o que já foi decidido; siga o ADR vigente.
5. Se a tarefa vier de um arquivo em `docs/sprints/`, execute exatamente o que o arquivo da sprint descreve, na ordem descrita, e pare nos critérios de aceite.

## 2. Regras de Git — obrigatórias, sem excepção

- **Nunca commitar diretamente em `main`, `release` ou `develop`.** Toda mudança nasce em uma branch própria, a partir de `develop`, e chega a `develop` via merge/PR.
- **`develop` deve estar sempre atualizada** em relação ao trabalho concluído. Ao terminar uma tarefa, faça merge (ou abra PR) para `develop` antes de considerar a tarefa encerrada — não deixe trabalho pronto preso em uma branch órfã.
- **`main`** reflete apenas o que já foi lançado em produção. **`release`** reflete o que está estabilizado e pronto para ir para `main` (changesets de release, hardening, congelamento de escopo). Merges para `main`/`release` são decisão humana, não automática de agente.
- Nunca force-push em `main`, `release` ou `develop`.
- Nunca reescreva histórico (`rebase -i`, `commit --amend`, `push --force`) em branches compartilhadas.

### 2.1 Convenção de branches

Formato: `<tipo>/<slug-curto-em-kebab-case>`.

| Tipo | Uso |
|---|---|
| `feature/` | nova funcionalidade ou capacidade nova |
| `bugfix/` | correção de bug em código já mergeado em `develop` |
| `hotfix/` | correção urgente que parte de `main`/`release` (produção) |
| `refactor/` | reestruturação sem mudança de comportamento externo |
| `chore/` | tarefas de manutenção (deps, configs, scripts, `.gitignore`) |
| `docs/` | mudanças apenas em documentação |
| `sprint/` | quando a branch corresponde 1:1 a um arquivo de `docs/sprints/` |

Exemplos: `feature/tenant-creation-flow`, `bugfix/login-redirect-loop`, `docs/adr-0011-react`, `sprint/01-refactor-frontend`.

### 2.2 Convenção de commits

Conventional Commits: `<tipo>(<escopo opcional>): <descrição curta no imperativo>`.

Tipos válidos: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `style`, `perf`, `build`, `ci`.

Exemplos:
```
feat(tenants): adiciona tela de criação de tenant
fix(auth): corrige loop de redirecionamento pós-login
docs(adr): registra ADR-0011 sobre framework de frontend
refactor(frontend): extrai App.tsx monolítico para domains/
chore(git): adiciona .gitignore cobrindo backend, frontend e keycloak
```

Corpo do commit (opcional, recomendado para mudanças não-triviais): explique o *porquê*, não apenas o *o quê* — o diff já mostra o quê.

### 2.3 Pull Requests

- PR sempre de `feature/*`, `bugfix/*`, `refactor/*`, `chore/*`, `docs/*` ou `sprint/*` → `develop`.
- PR de `release/*` → `main` só após validação manual do humano responsável.
- Toda PR descreve: objetivo, sprint/arquivo de referência (se houver), critérios de aceite atendidos, e se algum ADR foi criado/alterado.

## 3. Regras que nenhum agente pode quebrar

Estas regras vêm de decisões já tomadas (ADRs) e não devem ser revisadas por um agente sem decisão humana explícita:

- **Keycloak precisa de banco PostgreSQL dedicado e persistente** (ADR-0005 + Feature 004/005 de `implementation/001`). Nunca mover o Keycloak para o mesmo banco do Aegis, nunca remover o volume persistente do Keycloak em configs de Docker Compose ou `.gitignore`.
- **PostgreSQL do Aegis também é persistente e dedicado** — mesmo cuidado: nenhuma config de deploy pode apagar dados em redeploy (ADR-0010).
- **O frontend é React, não Angular** (ADR-0011). Ignore qualquer instrução de `implementation/001` Feature 017/018 que diga o contrário.
- **A SPA é servida pelo próprio Spring Boot, mesma origem** (ADR-0009) — não introduzir CORS nem hospedagem separada sem novo ADR.
- **O Produto é o centro do sistema** (Constituição, ARTIGO VI) — nenhuma feature deve subordinar Produto a Tenant, Usuário ou Conteúdo.
- **A tela de login atual (UI/UX) não muda** quando o Keycloak real for integrado — ver `docs/sprints/06_keycloak_login_ui_custom.md`. Não expor a tela nativa do Keycloak ao usuário final.

## 4. Onde registrar observações

Toda observação de comportamento de agente, lição aprendida durante uma sprint, ou ajuste de processo descoberto na prática deve ser adicionada à **Seção 5** abaixo, com data — não criar arquivos paralelos de "notas" soltos pelo repositório.

## 5. Observações registradas

- **2026-06-19** — Sessão inicial de alinhamento: consolidação de documentos mestres, resolução da contradição RH no ARTIGO V, regeneração de `WORKTREE.md`, criação de `docs/sprints/`, `AGENTS.md`, `CONTRIBUTING.md`, `.gitignore` e ADR-0011 (React vs Angular). Descoberto que `.git/refs/codex/turn-diffs/checkpoints/` já existe — Codex já foi usado neste repositório antes deste protocolo existir; ver `CONTRIBUTING.md` para uso conjunto Claude + Codex.

---

Qualquer agente que viole as regras da Seção 3 deve parar e reportar ao humano responsável (Alexandre) em vez de prosseguir.
