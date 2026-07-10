# BUG-SPRINT-08 — Documento de Execucao

> **Branch de implementacao:** `bugfix/sprint-08-jornadas-editor-permissoes-kg`
>
> **Commit da sprint:** `8a1ca2a fix(sprint-08): ajusta jornadas e permissoes por produto`
>
> **Merge em develop:** `7b8cb47 merge: finaliza bugfix/sprint-08-jornadas-editor-permissoes-kg em develop`
>
> **Status:** finalizada em 2026-07-08
>
> **Objetivo deste arquivo:** manter historico vivo da leva de bugs corrigida antes da proxima rodada. A proxima leva deve ganhar um novo documento antes de qualquer implementacao.

---

## Contexto

Rodada extensa de correcao pos-uso real cobrindo jornadas de produto, permissoes por papel, Knowledge Graph, editor de paginas, preview, formularios, assets, analytics, auditoria, Bruno e qualidade Sonar.

O foco arquitetural da leva foi consolidar a premissa de que **Produto e o centro operacional da experiencia**: PM, Editor e Viewer podem pertencer a produtos em tenants diferentes, mas a jornada deles deve ser guiada pelo produto, nao pelo tenant. Super Admin e Tenant Admin continuam com visoes administrativas proprias.

---

## Status por item

| Item | Status | Observacao |
|---|---|---|
| Jornadas por papel e produto | Concluido | PM, Editor e Viewer passam a ver apenas produtos e rotas coerentes com seus vinculos e permissoes. Fluxos com multiplas roles e multiplos tenants foram ajustados para selecionar produto sem exigir entendimento previo do tenant. |
| Product Manager exibido como Viewer | Concluido | Corrigida a resolucao de papel efetivo por produto para impedir queda indevida para Viewer quando o usuario tem papel operacional mais alto naquele produto. |
| Lista de equipe e tabela de usuarios | Concluido | Corrigido enriquecimento de usuarios, exibicao de nome/email reais e filtragem por produto. Usuarios removidos ou de outros produtos deixam de poluir a jornada ativa. |
| Convite de usuario ja pertencente ao tenant | Concluido | Fluxo permite atribuir produto adicional a usuario ja existente no tenant, respeitando o papel e os modulos do produto. |
| Mensagem opcional de convite | Concluido | Mensagem passou a trafegar no backend, persistir no token de acao e aparecer no template de email do Keycloak. |
| First name e last name | Concluido | Ativacao de convite e convite de usuario exigem dados de nome/sobrenome, evitando perfis incompletos. |
| Permissoes por modulo e settings | Concluido | Telas, botoes e rotas de configuracao respeitam modulo, papel e matriz de permissoes. Editor deixa de acessar configuracoes administrativas e habilitacao de modulo. |
| Produto arquivado/ativo apos reload | Concluido | Status do produto passou a persistir e refletir corretamente apos reload e em login por perfis operacionais. |
| Contagem de modulos | Concluido | Cards e modal de edicao passam a usar a mesma origem de verdade para modulos habilitados, evitando divergencia de contagem. |
| Dependencia Knowledge Graph e Assets | Concluido | Knowledge Graph exige Content e Assets habilitados quando necessario; Assets passa a ser habilitado junto do KG nos cenarios em que o grafo referencia midia. |
| Editor de paginas: eventos | Concluido | Blocos de eventos aceitam eventos selecionados, salvam sem 400 e aparecem no preview. |
| Editor de paginas: video e galerias | Concluido | Normalizacao de URL YouTube, galeria de videos, multiplos itens e preview foram revisados. URLs de share do YouTube sao aceitas e convertidas para embed valido. |
| Preview de pagina | Concluido | Preview recebeu cenarios visuais para componentes mistos, galerias e blocos de midia, com melhor comportamento responsivo. |
| Asset picker | Concluido | Modal passou a mostrar preview de imagem e suportar listas maiores com melhor navegacao e UX. |
| Upload de asset | Concluido | Barra "pronto para envio" foi ajustada para nao sugerir upload automatico antes do clique em enviar. |
| Formularios | Concluido | Validacao de formatos aceitos para upload foi corrigida e centralizada para refletir o que a UI configura. |
| Knowledge Graph sem mocks | Concluido | Busca, overview, canvas e relacoes passaram a priorizar dados reais do produto ativo. Mocks ficaram restritos a mock mode. |
| Knowledge Graph arestas | Concluido | Relacoes entre conteudos sao expostas pelo backend e renderizadas no frontend; canvas deixa de exibir apenas nos soltos. |
| Knowledge Graph interativo | Concluido | Canvas ganhou suporte a posicao/arraste e melhor composicao visual dos vertices. |
| Analytics reports | Concluido | Relatorios deixam de baixar arquivos `.txt` vazios e passam a respeitar formato proposto pela UI. |
| Global Search | Concluido | Busca global foi escopada ao produto atual e removeu resultados mockados/de outros produtos em API mode. |
| Dashboard e timelines | Concluido | Cards, contadores e timeline lateral deixam de exibir eventos de outros produtos quando a jornada esta scoped por produto. |
| Auditoria | Concluido | Tela de auditoria foi convertida para tabela paginada, com filtros reorganizados e contrato backend paginado. |
| Feedback | Concluido | Drawer/popover de feedback foi ajustado para ficar dentro do viewport e download de anexo passou a preservar nome/tipo correto. |
| Login | Concluido | Botao de mostrar senha deixou de acionar o link de esqueci senha. |
| Tutorial/onboarding | Concluido | Modal concorrente deixou de substituir indevidamente a jornada de tutorial. Smoke dedicado adicionado. |
| Bruno auth e teardown | Concluido | Collections passam a usar as credenciais atualizadas, teardown remove tenants de teste legados e temporarios, preservando `CLIENTES BETA` e tenants reais criados pelo operador. |
| Bruno permissoes | Concluido | Adicionados cenarios para permissao por papel, autoexclusao de usuarios temporarios e bloqueios de acesso indevido. |
| ADRs e documentacao viva | Concluido | ADRs 0015, 0016, 0017, 0018 e 0019 foram atualizadas com as decisoes de modulo, KG, templates, super admin e visibilidade por papel. |
| Sonar backend | Concluido | Corrigidos apontamentos de assinatura com parametros demais, `Math.clamp`, regex com backtracking, static imports Mockito e Dockerfile sem usuario root. |

---

## Decisoes consolidadas

1. **Produto guia a jornada de usuarios operacionais.** PM, Editor e Viewer podem pertencer a produtos em tenants diferentes, mas a UI deve apresentar produtos e capacidades, nao exigir selecao mental de tenant.
2. **Super Admin e Tenant Admin mantem jornadas administrativas.** Super Admin tem visao global, Tenant Admin tem visao de tenant; ambos podem tambem operar como PM em produto, mas a UI precisa separar contexto administrativo de contexto operacional.
3. **Permissao nao e so backend.** Rotas, menus, botoes, cards e modais tambem devem refletir a matriz de permissoes e os modulos habilitados.
4. **Mock mode e API mode nao podem se misturar.** Dados mockados sao validos apenas sem backend; com API ativa, busca, dashboard, KG, analytics e preview devem refletir dados reais do produto atual.
5. **Knowledge Graph depende de dados navegaveis reais.** Nos e arestas devem vir do produto ativo, relacoes devem ser listaveis e o canvas deve permitir leitura visual util.
6. **Bruno e contrato vivo.** Collections precisam validar permissoes, teardown e limpeza de usuarios/tenants temporarios como parte do fluxo normal.
7. **Documentacao viva acompanha decisao.** Qualquer ajuste que muda jornada, permissoes, contrato publico ou modelo multi-tenant atualiza ADR/documentacao na mesma leva.

---

## Verificacao executada

- `cd backend && mvn clean verify` — passou.
- JaCoCo — todos os gates atendidos.
- Backend — `1671` testes, `0` falhas, `0` erros, `0` skipped.
- `cd frontend && npm run lint` — passou.
- `cd frontend && npm run typecheck` — passou.
- `cd frontend && npm run build` — passou.
- Smoke frontend (`node tests/run-all.mjs`) — passou com `23/23` scripts.
- Bruno local completo — passou com `337/337` requests e `650/650` testes.
- `git diff --check` — passou.
- Varredura especifica Sonar nos padroes apontados — sem ocorrencias qualificadas restantes.

---

## Fechamento

A leva foi commitada em `bugfix/sprint-08-jornadas-editor-permissoes-kg`, mesclada em `develop` pelo script `scripts/gitflow-finish-sprint.sh`, enviada ao remoto e a branch remota foi removida pelo proprio script.

Novos bugs encontrados apos esta data devem ser registrados em um novo arquivo nesta pasta antes da implementacao, preservando este documento como historico da sprint ja encerrada.
