# AGENTS.md — Protocolo de Agentes de IA do Aegis PMS

Este é o arquivo mestre que qualquer agente de IA (Claude, Codex ou outro) deve ler **antes** de tocar em qualquer arquivo deste repositório. Ele define o fluxo de git obrigatório, convenções de commit e branch, e onde buscar contexto antes de agir.

Repositório: `aegis-pms` (remote `git@github.com:alexandrehenrique-dev/aegis-pms.git`).

Se você é um agente de IA agnóstico de ferramenta (não especificamente Claude), leia também `CONTRIBUTING.md` — ele cobre o mesmo fluxo em formato mais curto e ferramenta-agnóstico.

---

## 1. Antes de qualquer mudança

1. Leia este arquivo até o fim.
2. Leia `docs/README.md` para entender a estrutura documental e qual é a fonte de verdade (`docs/AEGIS_PMS_V1.md`).
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
- **Organização modular é obrigatória** — antes de criar qualquer nova classe, o agente deve analisar a estrutura do módulo correspondente e posicionar a classe no pacote de responsabilidade adequado. É proibido criar classes diretamente no pacote raiz do módulo ou mover classes entre módulos sem autorização explícita. Toda integração entre módulos deve ocorrer exclusivamente por APIs públicas expostas (`NamedInterface`), nunca por entidades, repositories ou services internos.

## 4. Padrões a evitar — apontamentos recorrentes do SonarQube for IDE

Antes de escrever Java neste repositório, evite os padrões abaixo. Cada um já gerou um apontamento real do SonarQube for IDE em sprints anteriores (ver Sprint 16 — domínio `audit`) e a correção sempre foi reescrever o código, nunca suprimir a regra.

- **Nunca usar `record`, `var`, `yield`, `sealed` ou `permits` como nome de método/campo/variável** (`java:S6213`, restricted identifiers). Mesmo sendo sintaticamente válido em Java, escolha outro nome (ex.: `recordEvent` em vez de `record`).
- **Lambda passada para `assertThrows`/`assertThatThrownBy` deve conter exatamente UMA chamada que possa lançar exceção** (`java:S5778`). Se a expressão dentro da lambda tem mais de uma invocação de método (ex.: `() -> service().metodo(parametro())`), extraia cada chamada auxiliar (`service()`, `parametro()`, `objeto.getId()`, etc.) para uma variável local **antes** do `assertThatThrownBy`, deixando só a chamada que de fato deve lançar a exceção dentro da lambda.
- **Nunca retornar `null` de um método cujo tipo de retorno é `Map`/`List`/`Set`/coleção** (`java:S1168`). Retorne a coleção vazia equivalente (`Map.of()`, `List.of()`, `Set.of()`) — avalie o impacto no contrato/payload antes de aplicar, já que isso pode mudar `null` para `{}`/`[]` na resposta JSON.
- **Nunca deixar a palavra "TODO" (ou variações que o regex do Sonar capture, como "Todo" no início de frase em português) dentro de um comentário/Javadoc sem implementar a tarefa** (`java:S1135`). Se for uma limitação deliberada e documentada (ex.: um campo que fica `null` nesta sprint por decisão arquitetural), escreva a justificativa como Javadoc normal, nunca com o marcador `TODO`, e evite a palavra "Todo" como primeira palavra de frase em comentários (o scanner não distingue "Todo" pronome de "TODO" marcador).
- **Nunca repetir o mesmo literal de string 3+ vezes no mesmo arquivo** (`java:S1192`), inclusive chaves de `Map.of(...)` (ex.: `"status"` repetido em vários `Map.of("status", ...)`). Extraia para uma constante `private static final String` com nome semântico (ex.: `DIFF_KEY_STATUS`), mesmo quando os valores associados à chave variam.
- **Seeds locais devem evitar nomes sensíveis genéricos em constantes** (`java:S2068`). Não use nomes como `PASSWORD`, `SECRET` ou `TOKEN` para valores de demonstração local; prefira nomes semânticos explícitos (ex.: `LOCAL_DEMO_USER_INITIAL_CREDENTIAL`) e, quando fizer sentido, uma property de profile `local` com default documentado. Credenciais de seed local devem ficar restritas ao profile `local`, nunca a produção. Literais repetidos de seed — slugs de produto, papéis, tenants, usuários e módulos — devem ser extraídos para constantes semânticas de domínio (ex.: `PRODUCT_MAESTRO_BETON`, `USER_EDITOR_SLUG`) em vez de constantes genéricas.
- **Nunca chamar método `@Transactional` do próprio bean via `this` ou chamada direta interna** (`java:S6809`). Em Spring, self-invocation ignora o proxy transacional; quando uma operação transacional precisar ser reutilizada, extraia para outro service do mesmo módulo ou reorganize o fluxo para que a chamada transacional entre por um bean injetado.
- **Quando múltiplos testes exercitam a mesma lógica variando apenas entrada e saída esperada, use `@ParameterizedTest` com `@MethodSource` ou `@CsvSource`** (`java:S5976`). Preserve nomes de cenários claros nos argumentos em vez de duplicar métodos quase idênticos.
- **Não use relógio do sistema em testes** (`java:S8692`). Prefira timestamps fixos com `Instant.parse(...)`/`OffsetDateTime.parse(...)` ou injete `Clock.fixed(...)`; se o código de produção depende do tempo atual, exponha uma sobrecarga/helper testável que receba a referência temporal fixa.
- **Não mantenha campos privados mortos em testes ou produção** (`java:S1068`). Se um campo/constante deixou de ser usado após refatoração, remova-o na mesma alteração.
- **Classes utilitárias devem ter construtor privado; classes com estado devem ser services/beans normais** (`java:S6829`). Se a classe mantém estado ou depende de `Clock`, repository, cache ou outro colaborador, modele como bean instanciável; use construtor privado apenas quando a classe for realmente um agrupador de métodos/constantes estáticos. Beans Spring com múltiplos construtores devem marcar explicitamente o construtor de injeção com `@Autowired`, especialmente para compatibilidade com Spring Boot 4 / Spring Framework 7. Evite construtores alternativos em beans quando não forem necessários; para testes, prefira factories/helpers estáticos fora do bean ou construtores package-private apenas quando eles não confundirem a injeção Spring. Classes com estado de runtime não devem ser tratadas como utilitárias apenas para silenciar o Sonar.
- **Spring MVC deve declarar binding explícito para toda variável de rota** (`java:S6856`). Toda variável declarada em `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc. deve ser vinculada com nome explícito em `@PathVariable`, por exemplo `@PathVariable("productId") UUID productId` ou `@PathVariable(name = "path", required = false) String path`. Nunca depender apenas do nome do parâmetro ou da flag `-parameters`; isso torna o contrato REST mais claro, elimina o apontamento do Sonar e evita ambiguidade em controllers.
- **Nunca usar `@SuppressWarnings`, `//NOSONAR` ou desabilitar regra para resolver um apontamento real.** A correção é sempre reescrever o código (renomear, extrair constante/variável, mudar `null` por coleção vazia, etc.) — suprimir só é aceitável para falso positivo comprovado, e mesmo assim exige justificativa explícita do humano responsável antes de aplicar.
- **Ao corrigir qualquer apontamento do Sonar, rode `mvn clean verify` completo depois** — alterações de assinatura de método (ex.: renomear `record` → `recordEvent`) ou de valor de retorno (`null` → coleção vazia) tendem a quebrar testes existentes que ainda esperam o comportamento antigo; corrija os testes na mesma tacada, nunca deixe `mvn verify` vermelho "para depois".
- **Use a asserção AssertJ mais específica disponível para o tipo do valor** (`java:S5838`, entre outras). Para verificar uma entrada de `Map`, prefira `assertThat(map).containsEntry(key, value)` a `assertThat(map.get(key)).isEqualTo(value)` — a primeira produz uma mensagem de falha melhor (mostra o mapa inteiro, não só o valor extraído) e é o padrão que o Sonar espera. O mesmo princípio vale para outras asserções especializadas do AssertJ (`containsExactly`, `hasSize`, etc.) em vez de compor a verificação manualmente a partir de acessores genéricos.
- **Extraia para variável local qualquer construção de coleção/objeto usada como argumento dentro de uma lambda de `assertThatThrownBy`** (`java:S5778`, mesmo princípio já citado acima) — inclusive chamadas aparentemente inofensivas como `Map.of(...)`/`List.of(...)` construídas inline como argumento da chamada sob teste. O Sonar conta qualquer invocação de método dentro da lambda, não só a chamada "principal"; `assertThatThrownBy(() -> service.metodo(Map.of("k", "v")))` já conta como duas invocações (`Map.of` e `metodo`). Extraia sempre: `Map<String, Object> content = Map.of("k", "v"); assertThatThrownBy(() -> service.metodo(content))...`.
- **Ao aplicar `java:S1168` (nunca retornar `null` de um método que devolve `Map`/coleção) em um metodo que decide se um valor "ausente" deve virar `null` no banco/JSON (ex.: uma coluna JSONB opcional), não empurre a decisão de nulidade para dentro do metodo que monta a coleção** — em vez disso, faça o metodo sempre devolver uma coleção não nula (nunca aceitando/retornando `null` na sua própria assinatura) e mova o `if (valorOriginal == null)` para uma variável local no *call site*, **fora** de qualquer declaração de método (uma expressão ternária atribuída a uma variável local não é um "método que retorna null" para o Sonar). Isso resolve o apontamento sem mudar o dado persistido/serializado quando o valor de origem já era `null` (ver `PageService`/`SectionContentValidationService`, Sprint 23).
- **Em configurações OpenAPI/Swagger, nomes de tags repetidos devem ser constantes semânticas e a lista fixa de tags deve existir em um único ponto canônico** (`java:S1192`). Não repetir literais como `"Products"`/`"Auth"` em `List.of(...)`, `Map.entry(...)`, testes e classificação por path; use constantes nomeadas (`TAG_PRODUCTS`, `TAG_AUTH`, etc.) e exponha um helper único para testes verificarem ordem sem duplicar a lista.
- **Não criar ternários/condicionais cujos dois ramos retornam o mesmo valor** (`java:S3923`). Se a ramificação não muda comportamento, remova a condição; se deveria mudar, escreva a diferença explicitamente e cubra com teste.
- **Quando Swagger/OpenAPI for ativado/desativado por profile, teste a configuração por profile**. No Aegis, Swagger deve criar os beans apenas em `local`/`dev`, permanecer indisponível em `prod`, preservar o security scheme Bearer JWT e manter a ordem das tags. Em testes Spring Boot 4, use `@MockitoBean`/`@MockitoSpyBean` quando precisar mockar beans; nunca `@MockBean`/`@SpyBean`.

## 4.1 Padrões a evitar — frontend (ESLint/TypeScript, `frontend/`)

`npm run lint` (ESLint) e `npm run typecheck` (`tsc -b --noEmit`) devem terminar com **zero erros e zero warnings** antes de qualquer commit de sprint frontend — não é só o `build` que precisa passar. Um warning não tratado nesta sprint tende a virar 14 warnings acumulados na próxima (caso real: Sprint 21, ver `docs/sprints/frontend/results/` se existir, ou `SPRINT-RESULTADO`/histórico do frontend). A correção é sempre reescrever o código, nunca `// eslint-disable`.

- **`react-refresh/only-export-components`** — dispara sempre que um arquivo `.tsx`/`.ts` exporta um componente React **e também** um valor que não é componente (hook, `createContext`, constante, função utilitária, `cva(...)` de variantes shadcn/ui). Corrija **separando o valor não-componente para um arquivo irmão**, nunca com `eslint-disable`:
  - Contexto + Provider + hook (`useX`): 3 arquivos — `xContextDefinition.ts` (só `createContext` + tipo do valor, nenhum componente), `XContext.tsx` (só o `Provider`, importando o contexto do arquivo de definição) e `useX.ts` (só o hook, importando o mesmo contexto). Ver `core/auth/authContextDefinition.ts` + `core/auth/AuthContext.tsx` + `core/auth/useAuth.ts` (Sprint 21) como modelo — o mesmo padrão foi aplicado a `ViewAsRoleContext`, `FeedbackModalContext` e `ThemeProvider`/`useTheme`.
  - Constante/função utilitária usada por um componente (ex.: `fade` em `Primitives.tsx`, `getPasswordStrength` em `AuthChrome.tsx`): mova para um arquivo próprio (`shared/components/motion.ts`, `core/auth/passwordStrength.ts`) e importe de volta no arquivo do componente. Se o valor só é usado **dentro** do próprio arquivo e nunca por outro módulo, o mais simples é parar de exportá-lo (remover o `export`) em vez de criar um arquivo novo.
  - Componentes shadcn/ui gerados (`shared/components/ui/*.tsx`) que misturam um componente com um `cva(...)` de variantes (`button.tsx`+`buttonVariants`, `badge.tsx`+`badgeVariants`, `toggle.tsx`+`toggleVariants`, `navigation-menu.tsx`+`navigationMenuTriggerStyle`) ou um hook de contexto (`sidebar.tsx`+`useSidebar`, `form.tsx`+`useFormField`): mova o valor não-componente para `<nome>-variants.ts` ou `<nome>-context.ts` no mesmo diretório `ui/`, e atualize os poucos consumidores internos (`import ... from "./button"` → `from "./button-variants"`). Ao adicionar um novo componente via `npx shadcn add`, se o CLI gerar o arquivo já misturado, aplique esta mesma separação antes de commitar.
- **`react-hooks/exhaustive-deps`** — nunca alargue o array de dependências só para calar o warning nem envolva a função inteira em `useMemo`/`useCallback` sem revisar as deps reais. Quando um `useMemo`/`useCallback` referencia uma função ou valor recalculado a cada render (ex.: um `const x = a ?? b` fora de `useMemo`, ou um setter de contexto não memoizado), o fix correto é: (1) envolver o valor derivado no seu próprio `useMemo` com as deps reais, (2) envolver a função no seu próprio `useCallback` com as deps reais, e só então (3) referenciá-los no `useMemo`/`useCallback` externo, agora com identidade estável entre renders. Ver `core/auth/AuthContext.tsx` (Sprint 21) — `effectiveTenant`/`tenantProducts`/`effectiveProduct` viraram `useMemo` e `login`/`logout`/`switchTenant`/`switchProduct`/`updateProduct`/`removeProduct`/`toggleFavorite` viraram `useCallback` antes de entrarem nas deps do `useMemo` do `value` do contexto.
- **Antes de abrir PR de uma sprint frontend**, rode `npm run lint` e `npm run typecheck` como parte do "Padrão de qualidade e entrega", exatamente como `mvn clean verify` é obrigatório no backend (Seção 4 acima) — nenhum dos dois é opcional só porque `npm run build` passou (o Vite build não roda ESLint).

## 5. Onde registrar observações

Toda observação de comportamento de agente, lição aprendida durante uma sprint, ou ajuste de processo descoberto na prática deve ser adicionada à **Seção 6** abaixo, com data — não criar arquivos paralelos de "notas" soltos pelo repositório.

## 6. Observações registradas

- **2026-06-19** — Sessão inicial de alinhamento: consolidação de documentos mestres, resolução da contradição RH no ARTIGO V, regeneração de `WORKTREE.md`, criação de `docs/sprints/`, `AGENTS.md`, `CONTRIBUTING.md`, `.gitignore` e ADR-0011 (React vs Angular). Descoberto que `.git/refs/codex/turn-diffs/checkpoints/` já existe — Codex já foi usado neste repositório antes deste protocolo existir; ver `CONTRIBUTING.md` para uso conjunto Claude + Codex.
- **2026-06-27** — Sprint 16 (domínio `audit`): rodada de correção de SonarQube for IDE revelou os 6 padrões agora documentados na Seção 4 (S6213, S5778, S1168, S1135, S1192, proibição de `@SuppressWarnings`). Nenhum foi corrigido com supressão; todos via reescrita + ajuste dos testes afetados, confirmado com `mvn clean verify` (BUILD SUCCESS, JaCoCo e Modulith aprovados).
- **2026-07-02** — Sprint 23 (domínio `pages`), rodada dedicada de correção pós-implementação em 5 arquivos (`PageService`, `PageServiceTest`, `ProductGlobalsServiceTest`, `SectionContentValidationService`, `SectionContentValidationServiceTest`): `java:S1168` corrigido em `SectionContentValidationService.sanitizeContent` e `PageService.readMap` sem alterar o payload/persistência (técnica documentada na Seção 4 — nulidade decidida no call site, não dentro do método que retorna a coleção); `java:S1135` era real (não um falso positivo desta vez) — a palavra "Todo" abria a primeira frase de um Javadoc, reescrito para não começar a frase com essa palavra; `java:S5778` corrigido em ~19 ocorrências que tinham `Map.of(...)`/`List.of(...)`/`new XyzRequest(...)`/uma chamada de helper construídos inline dentro da lambda de `assertThatThrownBy`, todas extraídas para variável local antes da asserção; `java:S5838` corrigido trocando `assertThat(map.get(key)).isEqualTo(value)` por `assertThat(map).containsEntry(key, value)`; `java:S5976` resolvido consolidando 3 testes de `SectionContentValidationServiceTest` (variações de `href` inválida do bloco `social-links`) em um único `@ParameterizedTest`/`@MethodSource` com nomes de cenário preservados. Nenhuma alteração de contrato REST, endpoint, migration ou regra de negócio; `mvn clean verify` e a collection Bruno cumulativa (`23-dominio-pages-secoes-e-blocos`) revalidados 100% depois da rodada.
- **2026-07-02** — Sprint 24 (OpenAPI/Swagger), rodada de correção Sonar em `OpenApiConfig`: `java:S1192` corrigido extraindo todas as tags OpenAPI para constantes semânticas e centralizando a lista ordenada em helper único reutilizado pelos testes; `java:S3923` corrigido removendo condicional redundante de status HTTP; testes adicionados para profiles `local`/`dev`/`prod`, Bearer JWT e ordem das tags. Padrão preventivo registrado na Seção 4 e no padrão de qualidade backend.
- **2026-07-03** — Sprint 30 (seed homologação e Telegram): regra de produto confirmada pelo humano — feedback interno do Aegis (`POST /feedback`) e submissions de produtos externos são jornadas diferentes. O primeiro usa somente o Telegram global `aegis.telegram.alert.*`; o segundo usa canais/configuração de produto/formulário. Não misturar esses fluxos em código, docs, Bruno ou frontend.
- **2026-07-03** — Follow-up da Sprint 19 (frontend), fechamento de 3 gaps encontrados numa auditoria pós-sprint (`CreateTenantWizardModal.tsx`, `productsService.ts`, `validation.ts`): plano do tenant era hardcoded `"Starter"` (sem campo editável); unicidade de slug de produto nunca era checada via API no blur (`productsService.checkSlugAvailable` adicionado); validação XOR `userId`/`inviteEmail` na atribuição de produto não existia (`assignmentXorError` adicionado a `shared/utils/validation.ts`, reutilizável por qualquer form de atribuição futuro). Ao escrever o smoke test novo (`tests/tenant-wizard.mjs`) e rodar `tests/run-all.mjs` para validar que nada quebrou, `users.mjs` falhou por um bug pré-existente e não relacionado às mudanças desta rodada: a Sprint 19 trocou o rótulo do botão do `ProductCard` de "Abrir"/"Abrir produto" para "Gerenciar" quando o papel é `super_admin` (mesma rota — só o texto muda, ver `ProductCard.tsx` `isSuperAdmin`), mas `tests/users.mjs` (que loga como `super-admin@byop.io`) nunca foi atualizado para aceitar o novo rótulo — ficou quebrado desde a Sprint 19 sem que ninguém notasse, porque esses smoke tests rodam manualmente, não em CI. Corrigido trocando o seletor para `/^(Abrir|Gerenciar)/`; os outros scripts que usam "Abrir" (`content.mjs`, `assets.mjs`, `forms.mjs`) logam como `editor` e o botão ali é de linha de tabela (conteúdo/asset), não do `ProductCard` — não afetados. Lição: qualquer mudança de rótulo de botão condicionada a papel deve ser seguida de um `grep` nos scripts de `tests/` pelo texto antigo, já que nada os roda automaticamente hoje — plano de colocar `tests/run-all.mjs` na pipeline de deploy (ver `tests/README.md`) deve reduzir esse tipo de regressão silenciosa no futuro.
- **2026-07-24** — Correção de CORS e automação do Genesis Lab: injetar uma lista YAML com `@Value("${aegis.app.cors-allowed-origins:...}")` não é equivalente a binding estruturado — listas YAML são expostas como propriedades indexadas, enquanto o placeholder consulta uma propriedade escalar e pode cair silenciosamente no default. Configurações de lista da aplicação devem usar `@ConfigurationProperties`, coleção defensiva e teste de binding por profile e variável de ambiente. O deploy passou a centralizar a branch em `AEGIS_DEPLOY_BRANCH`, derivar a tag de imagem e validar imagem/health/CORS/login antes de concluir.
- **2026-07-24** — Runner self-hosted do Genesis Lab: `actions/setup-java` instala e seleciona o JDK, mas não disponibiliza o executável `mvn`. Jobs Maven não devem depender de uma instalação global no host; o backend passou a versionar o Maven Wrapper, fixar a versão e o checksum da distribuição e executar `./mvnw`. O PostgreSQL de testes permanece efêmero e isolado por service container.

---

Qualquer agente que viole as regras da Seção 3 deve parar e reportar ao humano responsável (Alexandre) em vez de prosseguir.
