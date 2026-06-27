# Etapa 19 — Read model de formulários, submit público e endpoint de segurança

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 13 (domínio `form`/`submission`) e 17 (domínio `settings`) concluídas.
>
> **Motivação:** esta etapa fecha três lacunas identificadas na auditoria de retroﬁts do `SPRINT-RESULTADO.md` e na análise dos pacotes vazios do backend:
>
> 1. **Read model de forms (retrofit etapa 13):** `FormSummary.responses`, `lastActivity` e `conversionRate` retornam zeros/nulos porque o módulo `form` não pode acessar `submission` internamente sem criar ciclo no Spring Modulith. Solução: `SubmissionReceivedEvent` publicado pelo módulo `submission` → listener no módulo `form` incrementa contador e atualiza timestamp.
> 2. **Endpoint público de submissão (explicitamente deixado fora da etapa 13):** `POST /api/v1/products/{productId}/forms/{formId}/submit` (sem autenticação) — a constraint do modelo (`submission` só em formulário publicado) já existe desde a etapa 13; faltava o endpoint público.
> 3. **Endpoint de configurações de segurança (gap da etapa 17):** `SecuritySettingsPanel.tsx` chama `settingsService.saveSecurity()` → `PUT /api/v1/products/{productId}/settings/security`. Nunca foi especificado na etapa 17. Esta etapa fecha esse gap com um endpoint simples.
> 4. **Limpeza de pacotes vazios e encoding Maven.**
>
> **Nota sobre pacotes `seo`, `contract`, `config`, `integration`:** todos são diretórios vazios criados estruturalmente pelo GPT em etapas anteriores, sem classes. Não representam domínios a implementar:
> - `seo` → SEO já é parte do domínio `pages` (campos `seoTitle`, `seoDescription`, `seoCanonical`, `seoOgImageAssetId`, `seoNoIndex` na entidade `Page`, etapa 23).
> - `contract`, `config` → cada domínio tem seu próprio subpacote `contract/` e `config/`. Estes raiz são artifacts estruturais.
> - `integration` → webhooks e integrações externas são V2 (fora do ciclo de sprints V1). O `SecuritySettingsPanel` que as exibe é hoje 100% mock e continuará assim até V2.
>
> **Instrução ao GPT:** ao iniciar, **apagar** os quatro diretórios vazios via `Files.deleteIfExists` em um teste de integração ou simplesmente ignorar (compilador Java ignora diretórios vazios). O importante é não criar classes neles.

## A. Read model de formulários

### A.1 Campos novos na entidade `FormDefinition`

```java
// Adicionar em FormDefinition.java
@Column(nullable = false)
private long responseCount = 0L;  // incrementado por evento

@Column
private Instant lastActivityAt;   // atualizado por evento
```

Migration:
```sql
-- V9__form_read_model.sql
ALTER TABLE form_definitions ADD COLUMN response_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE form_definitions ADD COLUMN last_activity_at TIMESTAMPTZ;
```

### A.2 Evento no módulo `submission`

Criar em `submission.api` (NamedInterface existente):

```java
// submission/api/SubmissionReceivedEvent.java
public record SubmissionReceivedEvent(UUID formId, UUID productId, Instant receivedAt) {}
```

Publicar no `SubmissionService.create(...)` **após** persistir a submission:
```java
applicationEventPublisher.publishEvent(
    new SubmissionReceivedEvent(submission.getFormId(), submission.getProductId(), Instant.now())
);
```

### A.3 Listener no módulo `form`

Criar em `form/service/FormResponseCountListener.java`:

```java
@Component
public class FormResponseCountListener {

    private final FormDefinitionRepository formDefinitionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionReceived(SubmissionReceivedEvent event) {
        formDefinitionRepository.findById(event.formId()).ifPresent(form -> {
            form.setResponseCount(form.getResponseCount() + 1);
            form.setLastActivityAt(event.receivedAt());
            formDefinitionRepository.save(form);
        });
    }
}
```

> **Por que `@TransactionalEventListener` com `AFTER_COMMIT`?** Garante que o evento só é processado se a submission foi persistida com sucesso — evita incrementar contador para submissions que falharam no rollback.

### A.4 Atualizar `FormMapper` e DTOs

Mapear os novos campos em `FormSummary` e `FormDetail`:

```java
// FormSummary já tem:
long responses;       // antes fixo em 0 — agora vem de FormDefinition.responseCount
String lastActivity;  // antes string vazia — agora formata FormDefinition.lastActivityAt
String conversionRate;// manter como "—" por ora (requer analytics.api — V2)
```

`lastActivity`: se `lastActivityAt == null` → `"Nenhuma resposta ainda"`. Se não nulo → formato relativo pt-BR (ex.: "há 2 horas", "há 3 dias") usando `Duration.between(lastActivityAt, Instant.now())` — implementar sem biblioteca externa, com um método utilitário privado.

## B. Endpoint público de submissão

```txt
POST /api/v1/products/{productId}/forms/{formId}/submit
```

**Público** — sem `Authorization` header. Adicionar ao `SecurityFilterChain`:
```java
.requestMatchers(HttpMethod.POST, "/api/v1/products/*/forms/*/submit").permitAll()
```

**Request:** mesmo shape de `CreateSubmissionRequest` — `answersJson` como object/string, `assetId` opcional.

**Validações (já existem no `SubmissionService`, reusar):**
- Formulário deve estar `PUBLISHED` → 422 `FORM_NOT_PUBLISHED`
- `formId` deve pertencer ao `productId` → 404
- Campos obrigatórios (`required: true`) devem ter resposta → 422 `MISSING_REQUIRED_FIELD`

**Response:** `201 Created` com `SubmissionSummary`.

**Rate limiting simples** (sem library): no máximo 10 submissions por IP por hora por formulário. Implementar via `ConcurrentHashMap<String, AtomicInteger>` em memória com TTL manual (`Instant.now()` de reset). Se ultrapassado → 429 `TOO_MANY_SUBMISSIONS`.

> Nota: rate limiting em memória é adequado para V1. Em multi-instância futura, substituir por Redis (V2).

**Bruno:** adicionar na pasta `13-forms` um request `POST .../submit` sem `Authorization` header, com corpo de resposta completo e um cenário de formulário não publicado (422).

## C. Endpoint de configurações de segurança

```txt
GET  /api/v1/products/{productId}/settings/security
PUT  /api/v1/products/{productId}/settings/security
```

**Entidade:** `ProductSecuritySettings` (1:1 com `Product`, lazy-created na primeira leitura):

```java
@Entity @Table(name = "product_security_settings")
public class ProductSecuritySettings {
    @Id UUID productId;              // FK para products.id
    String webhookUrl;               // nullable
    String webhookSecret;            // nullable, nunca retornado no GET (omitir no mapper)
    boolean analyticsEnabled;        // default false
    String analyticsProviderKey;     // nullable
    boolean emailDeliveryEnabled;    // default false — Email Provider
    Instant updatedAt;
}
```

Migration:
```sql
-- V9 (mesmo arquivo, adicionar):
CREATE TABLE product_security_settings (
    product_id              UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    webhook_url             VARCHAR(1000),
    webhook_secret          VARCHAR(500),
    analytics_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    analytics_provider_key  VARCHAR(500),
    email_delivery_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at              TIMESTAMPTZ
);
```

**Response do GET:**
```ts
type ProductSecuritySettingsResponse = {
  webhookUrl: string | null;
  // webhookSecret NUNCA é retornado (segredo)
  analyticsEnabled: boolean;
  analyticsProviderKey: string | null;
  emailDeliveryEnabled: boolean;
  updatedAt: string | null;
  // Status derivados para o frontend (sem persistência própria):
  webhookStatus: "connected" | "disconnected";   // "connected" se webhookUrl != null
  analyticsStatus: "connected" | "attention" | "disconnected";
  emailStatus: "connected" | "disconnected";
};
```

`webhookStatus` é derivado no service (não persistido): `webhookUrl != null` → `"connected"`, caso contrário `"disconnected"`. Idem para os outros.

**Autorização:** apenas `SUPER_ADMIN` e `TENANT_ADMIN` podem ler/escrever — `PRODUCT_MANAGER`, `EDITOR`, `VIEWER` recebem 403.

**Nota:** `settingsService.saveSecurity()` no frontend chama `PUT /api/v1/admin/settings/security` — este path usa prefixo `/admin/...` que nunca foi padrão do Aegis (todos os outros usam `/api/v1/products/{productId}/...`). Ao ligar o frontend à API real (Sprint 07 do frontend), corrigir o path no `settingsService.ts`.

## D. Limpeza: pom.xml encoding

Adicionar ao `pom.xml` do backend, dentro de `<properties>`:
```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
```

Isso elimina os warnings de encoding Maven presentes desde a etapa 06.

## E. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. 100% de cobertura nas classes funcionais. Entregar em rodadas:
  1. Migration `V9__form_read_model.sql` + campos novos em `FormDefinition` + `SubmissionReceivedEvent` em `submission.api` + publicação do evento em `SubmissionService` + testes `@DataJpaTest`.
  2. `FormResponseCountListener` + testes com `@SpringBootTest` e `ApplicationEventPublisher` real (verificar que `responseCount` incrementa e `lastActivityAt` é preenchido após publish do evento).
  3. `FormMapper` atualizado (campos `responses`, `lastActivity`) + testes de mapper com instâncias de `FormDefinition` com e sem `lastActivityAt`.
  4. `ProductSecuritySettings` + `ProductSecuritySettingsRepository` (Javadoc) + `ProductSecuritySettingsMapper` + `ProductSecuritySettingsService` + testes com mocks.
  5. Endpoint público `POST .../submit` em `SubmissionController` (ou controller separado `PublicSubmissionController`) + rate limiter + testes `@WebMvcTest` (sem auth) + testes de rate limit (11ª chamada → 429).
  6. `ProductSecuritySettingsController` (`GET`/`PUT`) + testes `@WebMvcTest` + Bruno atualizado.

## F. Critérios de aceite

- [ ] `POST /api/v1/products/{productId}/forms/{formId}/submit` responde 201 **sem** header `Authorization`.
- [ ] Submit em formulário não publicado retorna 422 `FORM_NOT_PUBLISHED`.
- [ ] Após a 11ª submissão do mesmo IP em 1h, retorna 429 `TOO_MANY_SUBMISSIONS`.
- [ ] `GET /api/v1/products/{productId}/forms` retorna `responses > 0` e `lastActivity` não nulo após pelo menos uma submission recebida.
- [ ] `GET /api/v1/products/{productId}/settings/security` retorna o objeto de segurança com `webhookStatus`.
- [ ] `PUT /api/v1/products/{productId}/settings/security` persiste `webhookUrl` e `analyticsEnabled`.
- [ ] `webhookSecret` **nunca** aparece na resposta do `GET` (verificar via `@WebMvcTest` que o campo não existe no JSON serializado).
- [ ] `EDITOR` recebendo `GET /settings/security` → 403.
- [ ] `mvn clean verify` com `BUILD SUCCESS` e JaCoCo aprovado (100%).
- [ ] Spring Modulith aprovado — sem dependência direta de `submission` em `form`; apenas via `submission.api.SubmissionReceivedEvent`.
- [ ] Bruno executando: **todos requests aprovados** (incluindo novos na pasta `13-forms` e nova pasta `17-settings-security`).
- [ ] `pom.xml` sem warnings de encoding (`mvn clean compile` sem linhas `[WARNING] Using platform encoding`).

## G. Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): read model de forms, submit publico, config seguranca e cleanup"
```
