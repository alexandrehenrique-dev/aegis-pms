# ADR-0012 — Separação de tipos de conteúdo (Page vs Content vs futuros)

## Status

ACCEPTED

## Contexto

Produtos diferentes do ecossistema BYOP têm necessidades de conteúdo distintas: o Maestro Beton e a CMSS precisam de sites institucionais navegáveis por URL própria, compostos visualmente em seções (hero, galeria, agenda); a WikiDev, a Loki e a Conecta Talentos precisam de texto corrido com workflow editorial (Draft → Review → Published), pensado para leitura linear e referência cruzada, não para composição visual em seções. Uma sessão de teste manual identificou produtos tentando forçar conteúdo editorial dentro do domínio `pages` (ou vice-versa), gerando páginas sem necessidade de seções e artigos sem workflow adequado.

## Decisão

O Aegis reconhece formalmente **três famílias de conteúdo**, cada uma com domínio e finalidade próprios — não se força tudo em `Content` nem tudo em `Page`:

| Família | Domínio | Quando usar | Exemplos |
|---|---|---|---|
| **Page** (institucional) | `domains/pages` | Site composto por seções/blocos visuais, navegado por URL própria | Home, Sobre, Serviços, Agenda do Maestro Beton; Home/Quem Somos/História da CMSS |
| **Content** (editorial) | `domains/content` | Texto corrido com workflow (Draft→Review→Published), lido e referenciado, não composto em seções visuais | Artigos da WikiDev, manifestos/reflexões/poemas da Loki, posts do blog da Conecta Talentos |
| **Tipos futuros registrados** | domínios próprios quando chegar a hora | Conteúdo transacional/estruturado que não é nem página nem artigo | `JobPosting` (Conecta Talentos), `Book`/`Playlist` (Loki — hoje modelados como `type` extra de Content, aceitável por ora) |

Regra de decisão: **se o conteúdo é navegado como uma URL própria e composto visualmente em seções, é Page; se é lido de forma linear e tem workflow editorial de aprovação, é Content.** Um produto pode ter ambas as famílias simultaneamente (ex.: BYOP institucional + blog).

## Consequências

Positivas:
- Cada domínio evolui sem carregar responsabilidades do outro (workflow editorial não precisa de seções; composição visual não precisa de fluxo de aprovação).
- Decisão de modelagem deixa de ser ad-hoc por produto — há um critério único e reutilizável.
- Editores de produto (Sprint 13, Tarefa M) sabem exigir o domínio certo durante a jornada de criação.

Negativas / trade-offs:
- Produtos com necessidades híbridas (ex.: um artigo que também precisa de uma seção visual de destaque) exigem compor os dois domínios deliberadamente, não um único modelo universal.
- Tipos futuros (`JobPosting`, `Book`/`Playlist`) ficam temporariamente "emprestados" em `Content.type` até justificarem domínio próprio — risco de acúmulo de casos especiais se não forem promovidos a tempo.

## Alternativas Consideradas

- **Um único domínio de conteúdo genérico para tudo**: rejeitado por misturar workflow editorial com composição visual, forçando todo bloco de página a também ter status de revisão e todo artigo a ter sistema de seções, sem necessidade real de nenhum dos dois lados.
- **Domínio próprio por produto**: rejeitado por contrariar Product First (ADR-0001) — o objetivo é um motor reutilizável entre produtos, não implementações particulares.

## Impactos

- **Backend**: domínios `pages` e `content` continuam separados; tipos futuros (`JobPosting`, `Book`/`Playlist`) candidatos a domínio próprio quando o volume justificar.
- **Frontend**: jornada de criação de produto (Sprint 09) e simulações de jornada (Sprint 13, Tarefas L/M) devem direcionar o usuário ao domínio correto desde o início — WikiDev usa `content`+`kg-ref`, nunca `pages` para seu conteúdo principal.
- **Migração**: produtos que hoje usam `pages` para conteúdo que deveria ser `Content` (ou vice-versa) devem ser identificados e ajustados (ver Sprint 13, Tarefa M).

## Links Relacionados

- ADR-0001 (Product First), ADR-0003 (CMS First).
- Sprint 13 — Tarefas L, M (simulação de jornadas que validam esta decisão na prática).
