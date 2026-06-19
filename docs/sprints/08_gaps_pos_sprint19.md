# Sprint 08 — Auditoria de gaps entre o histórico Figma Make e o código real

> Executar por último, depois das Sprints 01–07, como fechamento.

## Contexto

`docs/implementation/figma-make-sprints-refinamento-v2.md` documenta Sprints 09–19 como concluídas, cada uma com uma seção "Feedback figma" que descreve o que foi de fato verificado. A Sprint 18 daquele histórico afirma ter auditado a jornada de cada uma das 5 personas (Super Admin, Tenant Admin, Product Manager, Editor, Viewer) e marca todas como ✅. A Sprint 03 deste plano já encontrou uma lacuna real dentro de uma jornada marcada como concluída (criação de tenant). Isso é evidência de que o "✅" daquele documento significa "a tela existe e parece coerente", não necessariamente "o fluxo está completo de ponta a ponta" — outras lacunas do mesmo tipo provavelmente existem e não foram listadas nesta análise por não terem sido lidas em detalhe linha a linha.

## Objetivo

Auditar, jornada por jornada, o que `figma-make-sprints-refinamento-v2.md` (Sprint 18) declara contra o comportamento real do frontend já refatorado e integrado (pós Sprints 01–07), e registrar/corrigir qualquer divergência.

## Tarefas

1. Para cada uma das 5 personas (`super_admin`, `tenant_admin`, `product_manager`, `editor`, `viewer`), percorrer manualmente a jornada descrita na Sprint 18 do histórico Figma Make, passo a passo, no app real (não apenas olhar o código).
2. Para cada passo, confirmar: (a) a tela existe, (b) a ação realmente produz o efeito esperado (persistência real, não só transição visual), (c) o passo está acessível pelo caminho de navegação descrito (quick action, menu, breadcrumb) e não só por URL direta.
3. Registrar cada divergência encontrada em uma tabela neste arquivo (seção "Resultado da auditoria", a preencher pelo agente que executar a sprint).
4. Abrir uma branch de correção por divergência relevante (ou agrupadas, se pequenas), seguindo o padrão `bugfix/<slug>`.
5. Atualizar `figma-make-sprints-refinamento-v2.md` com uma nota de rodapé em cada sprint cujo "✅" foi contestado por esta auditoria, apontando para a correção feita.

## Resultado da auditoria

_A preencher durante a execução desta sprint:_

| Persona | Passo da jornada (Sprint 18) | Situação real encontrada | Ação |
|---|---|---|---|
| Super Admin | Criar Tenant | Não existia — corrigido na Sprint 03 deste plano | ✅ Resolvido (Sprint 03) |
| ... | ... | ... | ... |

## Critérios de aceite

- [ ] Todas as 5 jornadas percorridas manualmente, não apenas inspecionadas no código.
- [ ] Tabela de resultado preenchida com pelo menos as 5 jornadas avaliadas.
- [ ] Toda divergência encontrada tem uma branch `bugfix/*` correspondente, mergeada em `develop`.
- [ ] `figma-make-sprints-refinamento-v2.md` atualizado com notas de rodapé onde aplicável.

## Comandos de Git

```bash
git checkout develop && git pull origin develop
git checkout -b sprint/08-auditoria-jornadas
# ... preencher tabela, abrir bugfix/* por divergencia, mergear cada um em develop ...
git commit -m "docs(sprints): registra resultado da auditoria de jornadas pos figma make"
git push -u origin sprint/08-auditoria-jornadas
git checkout develop && git merge --no-ff sprint/08-auditoria-jornadas && git push origin develop
```
