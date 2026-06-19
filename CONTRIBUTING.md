# CONTRIBUTING.md — Guia rápido, agnóstico de agente

Este arquivo existe para quem (humano ou agente de IA) for contribuir usando **qualquer ferramenta** — Claude, Codex, ou outra — sem precisar reaprender o processo a cada troca de ferramenta. A fonte completa das regras é `AGENTS.md`; este arquivo é o resumo prático.

## Regra de uma frase

> Nunca commite direto em `main`, `release` ou `develop`; trabalhe em uma branch própria a partir de `develop`, mantenha `develop` atualizada, e siga a convenção de nomes/commits abaixo.

## Fluxo (qualquer ferramenta)

```
1. git checkout develop && git pull
2. git checkout -b <tipo>/<slug>          # ver tabela de tipos no AGENTS.md
3. ... fazer a mudança ...
4. git add -A
5. git commit -m "<tipo>(<escopo>): <mensagem no imperativo>"
6. git push -u origin <tipo>/<slug>
7. abrir PR para develop
```

Tipos de branch: `feature/`, `bugfix/`, `hotfix/`, `refactor/`, `chore/`, `docs/`, `sprint/`.
Tipos de commit (Conventional Commits): `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `style`, `perf`, `build`, `ci`.

## Antes de codificar, ler nesta ordem

1. `docs/README.md` — mapa da documentação.
2. `docs/implementation/016_aegis_constitution.md` — o que o Aegis nunca pode se tornar.
3. `docs/adr/README.md` — decisões já tomadas (não redecida o que já foi decidido).
4. Se a tarefa vier de `docs/sprints/<arquivo>.md` — esse arquivo é a fonte da verdade da tarefa; execute na ordem nele descrita.

## Trabalhando com mais de um agente (Claude + Codex, por exemplo)

- O protocolo de git e commits é **o mesmo para qualquer agente** — não existe variante "modo Codex" ou "modo Claude" das regras de branch/commit.
- Antes de iniciar uma sessão com um agente diferente do último usado, rode `git log --oneline -10` e `git status` para entender o que a ferramenta anterior deixou pendente — não assuma que o outro agente fez merge ou push.
- Este repositório já tem histórico de uso do Codex (`.git/refs/codex/turn-diffs/checkpoints/...`). Esses refs são artefatos internos do Codex (checkpoints de turno) — não dependem de ação manual, mas também não devem ser apagados ou usados como branch de trabalho; ignore-os ao navegar branches.
- Se dois agentes trabalharem na mesma sprint em paralelo, cada um deve abrir sua própria branch (`sprint/01-refactor-frontend-claude` vs `sprint/01-refactor-frontend-codex`, por exemplo) — nunca dois agentes commitando na mesma branch sem coordenação humana.
- Ao entregar um resultado, deixe claro no PR/commit qual ferramenta foi usada (ex.: rodapé `Co-authored-by` ou nota no corpo do commit) — isso ajuda a auditoria e a continuidade entre sessões.

## O que nunca decidir de novo sem um humano

- Trocar o frontend de React para outra coisa (ADR-0011).
- Mover Keycloak ou PostgreSQL para bancos não-dedicados, ou remover persistência deles (ADR-0005, ADR-0010, `implementation/001` Features 004/005).
- Servir a SPA fora da mesma origem do backend (ADR-0009).
- Mudar a UI/UX da tela de login ao integrar o Keycloak real (ver `docs/sprints/06_keycloak_login_ui_custom.md`).

Para o detalhamento completo de cada regra, ver `AGENTS.md`.
