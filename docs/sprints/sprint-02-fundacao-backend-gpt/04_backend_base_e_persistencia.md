# Etapa 04 — Backend Spring Boot base e conexão com PostgreSQL/Flyway

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 02 concluída (PostgreSQL do Aegis no ar).

## Contexto fixo

Aegis PMS: monólito modular Spring Boot (não microsserviços). Módulos previstos: `core`, `identity`, `tenant`, `product`, `contract`, `content`, `asset`, `form`, `submission`, `seo`, `analytics`, `knowledgegraph`, `integration`. Banco: PostgreSQL com Flyway controlando migrations, `ddl-auto: validate` (nunca `update`/`create` em ambiente real).

## Objetivo

Criar a aplicação Spring Boot base e conectá-la ao `aegis-postgres`, com a primeira migration Flyway.

## Tarefas

### A. Projeto base

Criar projeto em `backend/` — **Java 25** e **Spring Boot 4.1.x** (linha estável atual; não negociável, ver `00_padrao_qualidade_e_arquitetura.md`), Maven, package base `br.com.byop.aegis`.

> **Atenção — Spring Boot 4.x não é um upgrade trivial de 3.x**: vem com Spring Framework 7, Jakarta EE 11, Hibernate 7.1, Spring Security 7 e Jackson 3 — mais de 100 mudanças que quebram compatibilidade. As que mais afetam esta sprint: (1) Spring Security 7 não tem mais configuração automática "boa o suficiente" por padrão — declarar `SecurityFilterChain` explícito desde a etapa 05, nunca depender de default; (2) `@MockBean`/`@SpyBean` foram removidos dos testes — usar `@MockitoBean`/`@MockitoSpyBean` em todo teste de Service/Controller (Seção 5 do padrão de qualidade); (3) Jackson 3 pode mudar formatação de serialização — não escrever teste comparando JSON como string exata, comparar como objeto/árvore; (4) tudo já é `jakarta.*` (nunca `javax.*` — já é o padrão deste projeto, mas reforçando). Se o GPT gerar código usando qualquer uma dessas APIs antigas, pedir para corrigir antes de aceitar.

Dependências: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `postgresql`, `flyway-core`, `flyway-database-postgresql`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-test`, `spring-security-test`, `mapstruct` + `mapstruct-processor` (annotation processor path).

Plugins: `jacoco-maven-plugin` configurado com `goal check` exigindo 100% de cobertura (LINE e BRANCH) nas classes elegíveis, falhando o build na fase `verify` se não atingir — configuração completa e exclusões (DTOs de transporte puro) em `00_padrao_qualidade_e_arquitetura.md`, Seções 4 e 6. Configurar isso **nesta etapa**, mesmo que ainda não exista classe alguma para medir — assim toda etapa seguinte já nasce com a régua de qualidade ativa, em vez de adicionar depois e descobrir débito de cobertura acumulado.

Estrutura de pacotes:

```txt
backend/src/main/java/br/com/byop/aegis
 ├── AegisApplication.java
 ├── config
 ├── security
 ├── shared
 ├── identity
 ├── tenant
 ├── product
 ├── contract
 ├── content
 ├── asset
 ├── form
 ├── submission
 ├── seo
 ├── analytics
 ├── knowledgegraph
 └── integration
```

### B. Configuração de datasource e Flyway

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${AEGIS_DB_HOST:localhost}:${AEGIS_DB_PORT:5432}/${AEGIS_DB_NAME:aegis_pms}
    username: ${AEGIS_DB_USER:aegis_user}
    password: ${AEGIS_DB_PASSWORD:aegis_password}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Criar também `application-local.yml` e `application-prod.yml` (perfis).

### C. Primeira migration

`V1__init.sql` (ou equivalente Flyway) criando tabelas mínimas: `tenants`, `tenant_memberships`, `products`, `product_modules`, `audit_events`. (A definição completa de colunas vem na etapa 06 — aqui pode-se criar a tabela mínima ou já completa, à escolha do GPT, desde que a etapa 06 não precise recriar do zero.)

### D. Provisionamento automático da pasta raiz de storage local (Sprint 13 do frontend)

A etapa 11 (`asset`) vai gravar arquivos em `${AEGIS_STORAGE_LOCAL_PATH}/aegis/pms/...` (ver etapa 01 e etapa 11 Seção D.1 para o caminho completo) quando um produto usar `assetStorageStrategy: "local"`. Essa pasta raiz — incluindo o namespace fixo `aegis/pms` — precisa existir **antes** de qualquer upload — e não pode depender de alguém criar manualmente no servidor (o caminho muda entre Linux/macOS/Windows e entre dev local/produção).

1. Adicionar um `ApplicationRunner`/`@PostConstruct` em `shared/` (ou `config/StorageBootstrapConfig.java`) que, na subida do backend, executa `Files.createDirectories(Path.of(${AEGIS_STORAGE_LOCAL_PATH}, "aegis", "pms"))`. Usar `java.nio.file.Files`/`Path`, **nunca** comando de shell (`mkdir -p` via `ProcessBuilder`, etc.) — `Files.createDirectories` já é multiplataforma por natureza (funciona igual em Linux, macOS e Windows, incluindo caminhos com `\` ou `/`), e não falha se a pasta já existir. Montar o caminho com `Path.of(a, b, c)` (várias partes), não concatenação de string com `/` — assim o separador correto do SO é sempre usado.
2. O mesmo bean garante que a raiz `{AEGIS_STORAGE_LOCAL_PATH}/aegis/pms` existe e tem permissão de escrita (a etapa 06/11 cria as subpastas **por produto/categoria** dentro dela; aqui é só a raiz do namespace) — registrar no log de startup o caminho absoluto resolvido, para facilitar debug em produção (“Storage local pronto em: /caminho/absoluto/aegis/pms”).
3. Se a pasta não puder ser criada (permissão negada, disco somente leitura), a aplicação **deve falhar a subida com uma mensagem clara** (`IllegalStateException` com o caminho e a causa) — nunca subir "pela metade" sem storage funcional, porque isso só apareceria como erro confuso no primeiro upload, bem depois.

## Critérios de aceite

- [ ] `mvn clean test` passa.
- [ ] `java -version` (dentro do ambiente do projeto) confirma Java 25.
- [ ] `mvn spring-boot:run` sobe a aplicação.
- [ ] `/actuator/health` responde `{"status":"UP"}`.
- [ ] Backend conecta ao `aegis-postgres` e Flyway cria as tabelas.
- [ ] `ddl-auto` é `validate`, nunca `update` ou `create`.
- [ ] `mvn clean verify` executa o `jacoco:check` sem erro (nesta etapa, sem classe elegível ainda, então passa trivialmente — confirma só que o plugin está configurado e roda).
- [ ] `mapstruct`/`mapstruct-processor` resolvidos no build (`mvn dependency:tree | grep mapstruct`).
- [ ] Ao subir o backend (sem Docker, direto com `mvn spring-boot:run`) num caminho de `AEGIS_STORAGE_LOCAL_PATH` que ainda não existe, a pasta é criada automaticamente, sem erro.
- [ ] Apontar `AEGIS_STORAGE_LOCAL_PATH` para um caminho sem permissão de escrita faz a subida falhar com mensagem clara, não silenciosamente.

## Validação

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Em outro terminal:

```bash
curl http://localhost:8080/actuator/health
docker exec -it aegis-postgres psql -U aegis_user -d aegis_pms -c "\dt"
```

Esperado: `tenants`, `tenant_memberships`, `products`, `product_modules`, `audit_events`, `flyway_schema_history`.

```bash
# confirmar criação automática da pasta de storage (ajustar caminho conforme o SO)
rm -rf ./data/assets   # ou %TEMP%\aegis\assets no Windows, se for o caminho configurado
mvn spring-boot:run
ls -la ./data/assets/aegis/pms   # esperado: pasta criada automaticamente pelo startup do backend
```

```bash
java -version   # esperado: 25
mvn clean verify   # confirma jacoco:check configurado e passando
mvn dependency:tree | grep -i mapstruct   # confirma mapstruct + mapstruct-processor resolvidos
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): spring boot base (java 25) com flyway, jacoco, mapstruct e bootstrap automatico da pasta de storage"
```
