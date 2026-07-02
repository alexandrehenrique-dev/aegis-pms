# Padrão de qualidade, arquitetura e entrega — vale para TODAS as etapas (01-26)

> Este arquivo é a fonte da verdade do padrão. Cada etapa de domínio (07, 08, 10-18, 22, 24-26) já traz um resumo deste padrão na própria etapa, porque cada arquivo precisa ser colável isoladamente numa conversa nova do GPT, sem depender de ter colado este arquivo antes. Se houver qualquer divergência entre o resumo numa etapa e este arquivo, **este arquivo prevalece** — ele é mais detalhado de propósito.
>
> Motivo de existir: o backend é gerado aos poucos, em conversas separadas do GPT, por etapa. Sem um padrão explícito e repetido, cada conversa nova do GPT tende a inventar uma convenção um pouco diferente da anterior (nome de método, forma de injeção, estilo de teste). Este documento existe para que **toda classe gerada, em qualquer etapa, em qualquer conversa nova do GPT, saia igual** — mesma forma, mesmo nome de método por camada, mesmo padrão de teste.

## 1. Stack (não negociável)

**Java 25**, Maven, **Spring Boot 4.1.x** (linha estável atual — Spring Framework 7, Jakarta EE 11, Hibernate 7.1, Spring Security 7, Jackson 3). Não aceitar sugestão do GPT de usar uma versão de Java diferente (nem "21+", nem "a LTS mais recente disponível na imagem Docker" — é Java 25, ponto) nem de Spring Boot diferente (nem "3.x para estabilidade" — é a linha 4.1.x, que é a estável atual, não a 3.x antiga). Se a imagem base do `Dockerfile` (etapa 20) não tiver Java 25 disponível, atualizar a imagem, nunca rebaixar a versão do projeto.

**Riscos de migração 3.x → 4.x que o GPT pode errar por hábito** (treinado majoritariamente em exemplos de 3.x):
- **Segurança**: Spring Security 7 não tem mais default "bom o suficiente" — toda etapa que toca segurança (05 em diante) declara `SecurityFilterChain` explícito, nunca depende de auto-configuração implícita.
- **Testes**: `@MockBean`/`@SpyBean` foram **removidos**. Usar `@MockitoBean`/`@MockitoSpyBean` (Spring Boot 4) em todo teste de `Service`/`Controller`. Se o GPT gerar `@MockBean`, é código que não compila — corrigir antes de aceitar.
- **Serialização**: Jackson 3 pode formatar JSON diferente de Jackson 2. Testes de `Controller` comparam **estrutura/objeto** (`JsonPath`, deserializar e comparar campo a campo), nunca string JSON exata.
- **Namespace**: tudo é `jakarta.*` — nunca `javax.*` (isso já valia antes, mas o GPT às vezes regride para `javax.*` em exemplos antigos; rejeitar se acontecer).
- Antes de aceitar qualquer código gerado, perguntar: "isso usa alguma API removida/alterada entre Spring Boot 3.x e 4.x?" — se a resposta não for claramente não, pedir para o GPT confirmar contra a documentação oficial do Spring Boot 4.1 antes de aplicar.

## 1.1 OpenAPI/Swagger por profile

Swagger/OpenAPI é configuração de ambiente e precisa ser testado como tal:

- Swagger fica habilitado somente em `local` e `dev`; `prod` deve manter `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false`.
- A configuração OpenAPI deve preservar Bearer JWT na UI e a ordem canônica das tags.
- Nomes de tags repetidos devem ser constantes semânticas (`TAG_PRODUCTS`, `TAG_AUTH`, etc.), nunca literais espalhados em listas, mapas e testes.
- A lista fixa de tags deve ser centralizada em método/constante única, reutilizada pelos testes para validar ordem sem duplicar o catálogo.
- Não usar ternários ou condicionais que retornem o mesmo valor nos dois ramos (`java:S3923`); remover a condição ou tornar a diferença real e testada.
- Sempre que ativação/desativação de OpenAPI depender de profile, criar teste cobrindo criação dos beans nos profiles permitidos, ausência no profile bloqueado e propriedades `springdoc` dos resources de ambiente.

## 2. Estrutura por camada — nomes e responsabilidade fixos

Todo domínio (`tenant`, `product`, `content`, `asset`, `form`, etc.) segue exatamente esta forma, sem variação de nome entre domínios:

| Camada | Nome de classe | Responsabilidade | Anotações típicas |
|---|---|---|---|
| Entity | `Xxx` (ex.: `Tenant`, `ProductModule`) | Mapeamento JPA puro — sem lógica de negócio complexa, só invariantes do próprio objeto (ex.: validação de campo obrigatório no construtor) | `@Entity`, `@Table` |
| Repository | `XxxRepository` | Interface `JpaRepository<Xxx, UUID>` (ou `JpaRepository<Xxx, Long>` se a etapa especificar `Long`) + métodos de query customizados | `@Repository` (opcional com Spring Data, mas declarar para clareza) |
| DTO/Request/Response | `XxxRequest`, `XxxResponse`, `XxxSummary` (`record`, nunca `class`, quando não tiver lógica) | Transporte de dados na borda da API — **sem lógica**, só campos | `record`, validação via `jakarta.validation` (`@NotBlank`, etc.) |
| Mapper | `XxxMapper` | Conversão `Entity ↔ DTO`, sempre via **MapStruct**, nunca conversão manual (`new XxxResponse(x.getId(), ...)` espalhado pelo service) | `@Mapper(componentModel = "spring")` |
| Service | `XxxService` | Regra de negócio, orquestra repository + mapper, nunca acessa `HttpServletRequest`/anotação web | injeção via **construtor** (nunca `@Autowired` em campo) |
| Controller | `XxxController` | Camada REST — recebe `Request`, chama `Service`, devolve `Response` via `Mapper`; nunca tem regra de negócio dentro do método do controller | `@RestController`, `@RequestMapping` |
| Policy/Validator (quando a etapa tiver) | `XxxPolicy`/`XxxValidator` | Regra de validação/consistência isolada (ex.: `GraphConsistencyPolicy`, já citado na etapa 08) | classe simples, sem anotação Spring obrigatória se for stateless |

Convenção de método por camada (sempre os mesmos nomes, em qualquer domínio):

- `XxxRepository`: usa os métodos padrão do Spring Data (`findById`, `save`, `deleteById`...) + métodos customizados nomeados por convenção de Spring Data Query Methods (`findByTenantIdAndStatus(...)`) sempre que possível, em vez de `@Query` JPQL manual — só usar `@Query` quando o nome do método ficaria absurdo ou a consulta precisar de `JOIN` complexo.
- `XxxMapper`: sempre `toEntity(XxxRequest request): Xxx` e `toResponse(Xxx entity): XxxResponse` (ou `toSummary` quando a etapa distinguir resposta completa de resumida) — nunca `map()`, `convert()`, `from()` ou qualquer outro nome.
- `XxxService`: métodos nomeados pelo verbo de negócio, não pelo verbo HTTP (`create`, `update`, `archive`, `transition` — não `post`, `put`).
- `XxxController`: métodos nomeados pelo verbo HTTP + recurso (`createTenant`, `listProducts`, `enableModule`) — é a única camada onde o nome do método pode espelhar o verbo HTTP.

## 3. Javadoc obrigatório no Repository

Toda interface `XxxRepository` tem:

```java
/**
 * Repositório de {@link Xxx}, escopado por tenant/produto conforme a regra
 * de negócio do domínio. <Uma frase explicando o que esta entidade representa.>
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Busca um tenant pela chave única (slug/key), usada na criação para
     * garantir unicidade global.
     *
     * @param key chave única do tenant
     * @return o tenant encontrado, ou {@link Optional#empty()} se não existir
     */
    Optional<Tenant> findByKey(String key);
}
```

Regras:
- Javadoc na assinatura da interface (o que a entidade representa, qual a regra de escopo — por tenant, por produto, global).
- Javadoc em **todo** método declarado explicitamente na interface (métodos herdados de `JpaRepository` sem override não precisam, só os que a etapa declarar) — incluindo `@param`/`@return` quando o método tiver parâmetro/retorno não óbvio.
- Nunca aceitar um método de repository sem Javadoc, mesmo que o nome pareça autoexplicativo.

## 4. MapStruct (mappers)

Adicionar ao `pom.xml` (etapa 04, na primeira vez que algum domínio precisar de mapper):

```xml
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>1.6.3</version>
</dependency>
```

E no `maven-compiler-plugin`, `annotationProcessorPaths`:

```xml
<path>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct-processor</artifactId>
  <version>1.6.3</version>
</path>
```

Todo `XxxMapper` é uma interface anotada `@Mapper(componentModel = "spring")` — o MapStruct gera a implementação em build time. Nunca escrever um `XxxMapperImpl` manual nem fazer conversão de campo a campo dentro do `Service`/`Controller`.

## 5. Cobertura de testes — 100% das classes funcionais

**Regra**: toda classe com lógica (qualquer comportamento que possa quebrar) precisa de teste cobrindo 100% de linhas e branches. Isso inclui: `Entity` com métodos de comportamento/validação própria, `Repository` com query customizada, `Mapper`, `Service`, `Controller`, `Policy`/`Validator`, classes de configuração com lógica condicional (ex.: o bean de bootstrap de storage da etapa 04).

**Exceção explícita — não exigir teste dedicado**: `record`/classe DTO de transporte puro (`XxxRequest`, `XxxResponse`, `XxxSummary`) sem nenhum método além de getters/campos do `record` — não tem comportamento para quebrar, então não tem o que testar. Se um DTO ganhar lógica (ex.: um método de validação customizada além do `jakarta.validation`), ele deixa de ser "puro" e passa a precisar de teste.

Tipo de teste por camada:
- `Repository`: `@DataJpaTest` (banco em memória ou Testcontainers — usar o que a etapa 02/04 já tiver configurado para teste; nunca testar contra o Postgres de produção).
- `Mapper`: teste unitário simples, sem Spring context — instanciar o mapper gerado (`Mappers.getMapper(XxxMapper.class)`) e verificar o resultado da conversão.
- `Service`: teste unitário com mocks (Mockito puro, `@ExtendWith(MockitoExtension.class)`) do `Repository`/`Mapper` — nunca subir contexto Spring inteiro só para testar uma regra de negócio.
- `Controller`: `@WebMvcTest` + `MockMvc`, com o `Service` mockado via `@MockitoBean` (**nunca** `@MockBean` — removido no Spring Boot 4, ver Seção 1).
- Cenários de erro/borda contam tanto quanto o caminho feliz — "100% dos cenários" no checklist de cada etapa significa: toda regra de negócio descrita na etapa (cada item de "Regras de negócio") tem pelo menos um teste que confirma o comportamento esperado E pelo menos um teste que confirma a rejeição quando a regra é violada.

## 6. JaCoCo

Adicionar ao `pom.xml` (etapa 04):

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>check</id>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>CLASS</element>
            <excludes>
              <exclude>**/*Request</exclude>
              <exclude>**/*Response</exclude>
              <exclude>**/*Summary</exclude>
              <exclude>**/*Dto</exclude>
              <exclude>**/*Application</exclude>
            </excludes>
            <limits>
              <limit>
                <counter>LINE</counter>
                <minimum>1.00</minimum>
              </limit>
              <limit>
                <counter>BRANCH</counter>
                <minimum>1.00</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

`mvn clean verify` deve **falhar** se qualquer classe elegível (fora da lista de exclusão) tiver cobertura abaixo de 100%. Ajustar o padrão de exclusão (`excludes`) se a etapa nomear DTOs de outro jeito — o princípio é "exclui só o que é transporte puro", nunca ampliar a exclusão para fugir de escrever teste de uma classe com lógica.

## 7. Entrega em rodadas — nunca tudo de uma vez

Cada etapa que cria entidades/camadas segue esta sequência, uma rodada por vez, cada rodada só avança depois da anterior ter testes passando e cobertura 100%:

1. **Rodada 1 — Entities + Repository + testes de repository.**
2. **Rodada 2 — Mappers (MapStruct) + testes de mapper.**
3. **Rodada 3 — Service (regras de negócio) + testes de service.**
4. **Rodada 4 — Controller/REST + testes de controller + validação manual via `curl` (seção "Validação" da etapa).**

Se a etapa tiver `Policy`/`Validator` próprio (ex.: `GraphConsistencyPolicy` na etapa 08), ele entra na Rodada 3, junto do Service, porque é regra de negócio. Pedir ao GPT explicitamente "rodada N desta etapa" em cada conversa — nunca pedir "implemente a etapa inteira" de uma vez.

## 8. Consistência entre rodadas e entre etapas

Antes de aceitar o código de uma rodada nova (de qualquer etapa), comparar com o que já foi gerado nas rodadas/etapas anteriores: mesmo estilo de injeção (construtor), mesmo nome de método por camada (Seção 2), mesmo padrão de teste (Seção 5), mesma forma de Javadoc (Seção 3). Se o GPT entregar algo estruturalmente diferente do que já existe no repositório (ex.: um service com `@Autowired` em campo quando todos os outros usam construtor), **não aceitar** — pedir para refazer seguindo o padrão já estabelecido nas etapas anteriores, citando o arquivo existente como referência.

## 9. Autorização: papéis e módulos (ADR-0014 e ADR-0015)

Auditoria de consistência feita antes do início da implementação encontrou divergências de nomenclatura de papéis entre Keycloak/backend/frontend, e a ausência de qualquer checagem de módulo habilitado nos endpoints de domínio. As duas ADRs abaixo resolvem isso — este resumo é o que toda etapa precisa respeitar; o detalhe completo (contexto, alternativas consideradas) está nas ADRs.

### 9.1 Papéis (ADR-0014) — uma tabela, três formatos, nunca inventar um quarto

| Camada | Formato | Exemplo |
|---|---|---|
| Keycloak (realm role, etapa 03) | `AEGIS_<NOME>` | `AEGIS_SUPER_ADMIN` |
| Spring Security (`@PreAuthorize`/`hasRole`, depois do `JwtRoleConverter` da etapa 05) | `ROLE_<NOME>` (o `hasRole('X')` do Spring já adiciona o `ROLE_`, então no código se escreve só `hasRole('SUPER_ADMIN')`) | `ROLE_SUPER_ADMIN` |
| Regra de negócio em prosa/domínio (`TenantMembership.role`, `ProductAssignment.role`) | `<NOME>` maiúsculo, sem prefixo | `SUPER_ADMIN` |
| `GET /api/v1/me` e contrato de API pública (consumido pelo frontend, `UserRole`) | `<nome>` minúsculo, singular, nunca array | `super_admin` |

Os 5 papéis, sempre nesta ordem de prioridade quando uma resolução precisar escolher um só: `super_admin > tenant_admin > product_manager > editor > viewer`. Nunca criar um sexto papel ou um nome alternativo (ex.: "CONTENT_EDITOR") sem atualizar a ADR-0014 primeiro.

### 9.2 Módulos como portão de acesso (ADR-0015)

`ProductModule.enabled` (etapa 07) não é só um dado consultável pela UI — é um **portão real** que os próprios endpoints de domínio verificam antes de processar qualquer requisição. Mecanismo: anotação `@RequireModule(ModuleKey.X)` no método do controller + `ModuleAccessAspect` (Spring AOP, implementado na etapa 07) que verifica `ProductModule.enabled=true` para o `productId` do path; se desabilitado, `403` com corpo `{"error": "MODULE_DISABLED", "moduleKey": "X"}` — vale até para `SUPER_ADMIN`.

Domínios gateados por módulo (etapa → `@RequireModule`): `08`/`18` → `KNOWLEDGE_GRAPH`; `11` → `CONTENT`; `12` → `ASSETS`; `13` → `FORMS`; `14` → `ANALYTICS`; `22` → `PAGES`. Domínios **não** gateados (fundação, sempre disponíveis): `09` (health/status), `10` (tenants/ProductAssignment), `15` (users), `16` (audit), `17` (settings/dashboard), `24` (notification), `26` (feedback — reportar problema não depende de nenhum módulo do produto).

Toda etapa de domínio gateada por módulo adiciona, nos próprios critérios de aceite, o cenário "módulo desabilitado para o produto → 403 `MODULE_DISABLED`" como teste obrigatório de Rodada 4 (controller).

## 11. Entrega de validação: collection Bruno cumulativa, não só curl

> **Migração Postman → Bruno**: até a Sprint 11 (inclusive), a validação manual era entregue como um único arquivo `postman/aegis-postman-collection.json` (Postman Collection Format v2.1). A partir da Sprint Técnica de migração (ver `SPRINT-RESULTADO.md`), a collection cumulativa passou a ser **Bruno** (`bruno/`, formato `.bru` nativo, um arquivo por request, 100% git-versionado, sem dependência de aplicativo desktop nem de `pm.*`/`postman.*`). A pasta `postman/` e o arquivo `aegis-postman-collection.json` foram removidos do repositório; toda referência a eles nas etapas abaixo é histórica — qualquer trabalho novo usa exclusivamente Bruno.

Toda etapa traz, na própria seção "Validação", um bloco de `curl` — isso continua existindo e continua sendo a **especificação exata** de cada request (método, path, headers, body), não muda. O que muda é a forma de **entregar** a validação: em vez de só rodar os `curl`s manualmente uma vez e descartar, cada etapa adiciona os mesmos requests a uma **collection Bruno cumulativa única**, em `bruno/` (um arquivo `.bru` por request, versionado no git como qualquer arquivo de código), que cresce uma pasta numerada por etapa e é validada via `npx @usebruno/cli run --env local` ao final de cada etapa.

### 11.1 Estrutura da collection (criada na Sprint Técnica de migração, estendida a partir daí)

- **`bruno/bruno.json`**: manifesto da collection (`name`, `type: collection`).
- **`bruno/environments/{local,dev,homolog,prod}.bru`**: uma variável `baseUrl` por ambiente (`local` aponta para `http://localhost:8080`; `dev`/`homolog`/`prod` ficam com o valor vazio até o ambiente existir). Toda URL de request usa `{{baseUrl}}/...`, nunca o host hardcoded.
- **`bruno/collection.bru`**: variáveis compartilhadas (`username`, `password`, `tenantKey`, `productKey`, `inviteEmail`, etc., inicializadas em `script:pre-request` só se ainda não tiverem valor) e o header `Authorization: Bearer {{token}}`, herdado por toda request da collection (usar `headers`, não o bloco `auth:bearer`, que tem um bug conhecido de não repropagar variáveis definidas em runtime entre requests no modo `inherit` do Bruno CLI).
- **Pasta `00-auth` (criada na Sprint Técnica de migração)**: um request `POST {{keycloakIssuer}}/protocol/openid-connect/token` por usuário de teste necessário — body `form-urlencoded` com `grant_type=password`, `client_id=aegis-web`, `username=<usuário>`, `password=<senha de teste>`. Rodar qualquer um desses requests troca o "usuário atual" da sessão de teste.
- **Script de captura automática do token** (`script:post-response` do request de login):
  ```js
  bru.setVar('token', res.body.access_token);
  bru.setVar('refreshToken', res.body.refresh_token);
  ```
  Isso elimina copiar/colar token manualmente — rodar o request de `00-auth` já deixa `{{token}}` pronto para todo o resto da collection.
- **Uma pasta numerada por etapa de domínio** (`00-auth`, `01-tenants`, `02-products`, `03-product-modules`, `05-knowledge-graph`, ..., `12-assets`), cada uma com um request por `curl` documentado na etapa correspondente — mesmo método, path, body; o bloco `docs` do request (objetivo, payload, resultado esperado, variáveis usadas/produzidas) cita a regra de negócio que aquele request valida, para a collection servir como documentação executável, não só uma lista de chamadas soltas.

### 11.2 O que o GPT entrega ao final de cada etapa

Além do código da etapa, o GPT entrega os arquivos `.bru` **novos/alterados** da pasta da etapa (Bruno é git-nativo — cada request é um arquivo próprio, então a entrega é o diff desses arquivos, nunca um JSON único reenviado por completo) e confirma que `npx @usebruno/cli run --env local`, executado a partir de `bruno/`, passa 100% (todas as pastas, na mesma execução, já que variáveis de runtime como tokens e IDs capturados só persistem dentro de uma única invocação do CLI). Etapas que gateiam por módulo ou têm regra de isolamento por tenant (Seções 9 e 10 deste padrão) incluem, na mesma pasta, ao menos um request que prova o caminho de **rejeição** (403 `MODULE_DISABLED`, 404 cross-tenant) — não só o caminho feliz, mesmo princípio já exigido dos testes automatizados (Seção 5).

A pasta `00-auth` nunca precisa ser refeita nas etapas seguintes — só as pastas de domínio são adicionadas incrementalmente, sempre por cima da mesma collection (a collection de uma etapa nunca substitui a estrutura já validada nas etapas anteriores, só soma). Ver `docs/api-testing/README.md` para o guia completo de uso, estrutura de pastas e padrão obrigatório ao adicionar requests em sprints futuras.

## 10. Isolamento entre tenants e produtos — obrigatório em todo domínio, não só nos que já mencionam

As etapas 07, 08, 10, 12 e 17 já aplicam a regra "recurso de outro tenant/produto retorna 404, nunca 403 (não revelar existência)" explicitamente. Uma auditoria de consistência encontrou que essa regra **não estava repetida** nas etapas 11 (content), 13 (forms), 14 (analytics), 15 (users), 16 (audit), 18 (Knowledge Graph extras), 22 (pages) e 24 (notification) — o que não significa que a regra não vale para elas; significa que ela precisa ser explícita em **toda** etapa, não só nas que já a mencionavam por acaso.

**Regra, válida para toda entidade que pertence a um tenant ou produto (direta ou transitivamente, ex.: uma seção pertence a uma página que pertence a um produto):**

1. Toda consulta por ID (`GET/PUT/PATCH/DELETE /.../{id}`) verifica, no `Service` (nunca só no `Controller`, nunca confiando em o frontend mandar o `tenantId`/`productId` certo), que o recurso pertence ao tenant/produto que o usuário autenticado tem permissão de acessar — usando a membership/atribuição do token (`AuthenticatedUser`), nunca um parâmetro que o client poderia manipular.
2. Se o recurso existe mas pertence a outro tenant/produto: **404**, nunca 403 — não revelar que o recurso existe para quem não tem acesso a ele.
3. Toda etapa de domínio (11, 13, 14, 15, 16, 18, 22, 24 incluídas, sem exceção) adiciona um cenário de teste explícito desta regra na Rodada 3 (Service, com mocks) e repete como critério de aceite — não é opcional só porque a etapa não cita a palavra "tenant" no nome.
4. Esta regra é independente do module-gating (Seção 9.2): module-gating bloqueia porque o **produto** não tem o módulo ligado; isolamento bloqueia porque o recurso **não pertence** ao escopo do usuário. Um endpoint pode (e geralmente vai) precisar das duas checagens, nesta ordem: módulo habilitado → depois, recurso pertence ao escopo do usuário.

**Regra adicional — conteúdo de produto para SUPER_ADMIN (ADR-0018):**

5. Para domínios de **conteúdo de produto** (content, pages, assets, forms, analytics, knowledge graph — etapas 08, 11, 12, 13, 14, 18 e 22), a verificação de acesso usa o `ProductAccessResolver` implementado na etapa 07 **antes** do module-gating:
   - `SUPER_ADMIN` com `ProductAssignment` ativo para o produto → passa (usa papel do assignment)
   - `SUPER_ADMIN` sem `ProductAssignment` → **403** `PRODUCT_CONTENT_ACCESS_DENIED` — diferente do 404 cross-tenant, porque o SUPER_ADMIN sabe que o produto existe
   - `TENANT_ADMIN` com `TenantMembership` no tenant do produto → passa
   - `PRODUCT_MANAGER | EDITOR | VIEWER` com `ProductAssignment` → passa
   - Qualquer outro caso → 404
6. Domínios de **infraestrutura** (health/status, tenants, users, settings, audit, notifications — etapas 09, 10, 15, 16, 17 e 24) **não** usam `ProductAccessResolver` — seguem apenas a regra geral dos itens 1-4 acima.
7. `ProductAccessResolver` centraliza esta lógica. **Nunca duplicar** a lógica de acesso de conteúdo em cada Service — sempre delegar ao `ProductAccessResolver` e cobrir os 5 cenários (SUPER_ADMIN com/sem assignment, TENANT_ADMIN, PRODUCT_MANAGER, cross-tenant) nos testes da Rodada 3 da etapa de domínio correspondente.

## 12. Artefato de continuidade entre etapas: `SPRINT-RESULTADO.md`

Cada etapa é colada numa conversa **nova** do GPT — sem memória do que foi decidido nas etapas anteriores. Isso já causou retrabalho: decisões que uma etapa explicitamente deixava "a cargo do GPT, documentar a escolha" (ex.: ordem de habilitação de módulos com dependência, formato exato do `id` legível do feedback, se o provisionamento de pasta é síncrono ou assíncrono) ficavam presas só naquela conversa, perdidas ao abrir a próxima. A partir desta revisão, todo esse conhecimento passa por um único arquivo cumulativo, versionado junto da pasta de etapas: `docs/sprints/backend/SPRINT-RESULTADO.md`.

### 12.1 Regra de uso (você, fora do GPT)

- **Ao colar uma etapa nova numa conversa nova do GPT, cole também o conteúdo atual de `SPRINT-RESULTADO.md` junto** (a partir da etapa 02 — a etapa 01 é a primeira, ainda não há nada para herdar). Isso substitui a memória que o GPT não tem.
- Ao final de cada etapa, antes de comitar, peça ao GPT o **arquivo `SPRINT-RESULTADO.md` completo e atualizado** (igual ao padrão já estabelecido para a collection Postman, Seção 11.2 — sempre o arquivo inteiro, nunca um diff) e salve-o, sobrescrevendo o anterior.

### 12.2 Template fixo de cada entrada (uma por etapa, nunca reescrever entradas de etapas já concluídas)

```md
## Etapa NN — <título da etapa> (concluída em <data>)

**Classes criadas/alteradas:** lista exata (entity, repository, mapper, service, controller, policy) — nomes reais, não genéricos, para a próxima etapa saber o que já existe e nunca duplicar ou renomear por conta própria.

**Endpoints confirmados:** método + path exatamente como implementado. Se algum divergiu do que o `.md` da etapa pedia, anotar a divergência e o motivo aqui — não deixar a divergência só na memória da conversa que já vai se perder.

**Decisões de implementação registradas pelo GPT:** toda vez que o `.md` da etapa dizia algo como "decisão de implementação do GPT, documentar a escolha" — a escolha real feita, com uma frase de motivo. Isto é o item mais importante desta seção — é exatamente o tipo de decisão que se perde entre conversas se não for escrita aqui.

**Retrofits pendentes para etapas futuras:** todo "se a etapa X já existir, chame Y; senão, retrofit a fazer depois" (padrão já usado nas etapas 14/23, 09/24) que **não** pôde ser resolvido agora porque a etapa X ainda não tinha sido executada — para a etapa X, quando chegar a vez dela, saber que precisa voltar e completar isto.

**Cobertura de testes:** confirmação de que `mvn clean verify`/`jacoco:check` passou em 100% para as classes desta etapa.
```

### 12.3 O que NÃO entra neste arquivo

Código-fonte completo, payloads de exemplo já documentados no próprio `.md` da etapa, e qualquer coisa já coberta pela collection Postman (Seção 11) — `SPRINT-RESULTADO.md` é sobre **decisões e estado**, não duplicar o que já está em outro artefato. Etapas que não tiveram nenhuma decisão delegada ao GPT podem ter uma entrada curta (1-2 linhas em "Decisões") — não inventar conteúdo para preencher a seção.
