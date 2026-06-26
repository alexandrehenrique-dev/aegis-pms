# Etapa 13 — Domínio `form`/`submission` (formulários, builder, submissions)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída. Usa os packages `form` e `submission` já previstos na etapa 04.

## Contexto fixo

Telas `FormsDashboard`, `FormsList`, `FormBuilder`, `FormPreviewFrame`, `SubmissionTable`, `SubmissionDetails`, `BasicFormAnalytics`, `PublicationPanel` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.3).

## Objetivo

CRUD de definição de formulário (campos, ordem, obrigatoriedade) e de submissions recebidas, com publicação.

## Tarefas

### A. Entidades

**FormDefinition**: `id`, `tenantId`, `productId`, `name`, `type`, `status`, `fieldsJson` (array de campos: label, tipo, obrigatório, placeholder, validação, condição de visibilidade — espelhando o painel de propriedades de `FormBuilder.tsx`), `deliveryChannelsJson` (ver Seção D), `publication`, `createdAt`, `updatedAt`.

**Submission**: `id`, `formId`, `date`, `name`, `email`, `source`, `status`, `ownerSubject`, `score`, `answersJson`, `createdAt`.

> Campo do tipo `Upload` em `fieldsJson` (já citado no catálogo `field-types`) deve aceitar uma propriedade `acceptedFileTypes: string[]` (ex.: `["application/pdf"]`) — usado pelo formulário de candidatura (currículo em PDF) e por qualquer outro upload dentro de um formulário. O arquivo enviado pelo respondente vira um `Asset` (etapa 11), e `answersJson` guarda o `assetId`, não o arquivo em si.

### B. Endpoints

```txt
GET  /api/v1/products/{productId}/forms
GET  /api/v1/products/{productId}/forms/{formId}
PUT  /api/v1/products/{productId}/forms/{formId}
GET  /api/v1/products/{productId}/forms/field-types
GET  /api/v1/products/{productId}/forms/{formId}/submissions
GET  /api/v1/products/{productId}/forms/submissions
GET  /api/v1/products/{productId}/forms/{formId}/submissions/{submissionId}
POST /api/v1/products/{productId}/forms/{formId}/publish
```

Payloads:

```ts
type FormSummary = {
  name: string; type: string; status: string; responses: string;
  conversion: string; lastActivity: string; publication: string;
};
// GET /forms → FormSummary[]

// GET /field-types → string[] (catálogo fixo do sistema: "Texto", "Email", "Telefone", "Upload", "Consentimento LGPD"...)

type SubmissionSummary = {
  date: string; name: string; email: string; source: string;
  status: string; owner: string; score: string;
};
// GET /submissions → SubmissionSummary[]
```

### C. Regras de negócio

- `field-types` é um catálogo fixo do sistema (enum no backend), não por tenant — existe como endpoint só para o frontend não hardcodar a lista.
- `publish` exige ao menos um campo obrigatório definido e valida que não há dois campos com o mesmo `label` (consistência mínima do formulário).
- Submission sempre referencia um `formId` publicado — não é possível submeter para um formulário em rascunho (validação relevante quando o endpoint público de submissão existir, fora do escopo desta etapa, mas a constraint do modelo já deve existir).
- **Isolamento por produto** (`00_padrao_qualidade_e_arquitetura.md`, Seção 10): formulário/submission de um produto fora do escopo do usuário retorna 404, nunca 403.
- **Module-gating** (Seção 9.2 do padrão): `FormController`/`SubmissionController` anotados com `@RequireModule(ModuleKey.FORMS)` — produto com módulo `FORMS` desabilitado retorna 403 `MODULE_DISABLED`.

### D. Entrega de respostas por múltiplos canais (Sprint 13 do frontend)

Hoje a única "entrega" de uma submission é a notificação interna do produto. A Sprint 13 do frontend adiciona configuração de **para onde mandar** cada resposta — o cliente pode querer e-mail, WhatsApp, Telegram e/ou webhook simultaneamente.

```txt
PUT /api/v1/products/{productId}/forms/{formId}/delivery
```

```ts
type FormDeliveryChannel = {
  type: "email" | "whatsapp" | "telegram" | "webhook";
  enabled: boolean;
  config: Record<string, string>; // email: {to}; whatsapp: {number}; telegram: {chatId, botToken}; webhook: {url, method}
};
type UpdateFormDeliveryRequest = { channels: FormDeliveryChannel[] };
```

Persistido em `FormDefinition.deliveryChannelsJson`. **Disparar de fato** a entrega (enviar o e-mail, chamar o webhook) é trabalho de uma etapa futura de integrações (fora do escopo desta etapa) — aqui só a configuração precisa existir, ser validada (ex.: `webhook` exige `url` válida; `telegram` exige `chatId` e `botToken` não vazios) e persistir corretamente.

### E. Padrão de qualidade e entrega (obrigatório)

> Resumo — detalhe completo em `00_padrao_qualidade_e_arquitetura.md`.

- **Java 25** / **Spring Boot 4.1.x**. Javadoc obrigatório na interface e em todo método de `FormDefinitionRepository`/`SubmissionRepository`. Mappers via MapStruct (`FormDefinitionMapper`, `SubmissionMapper`). 100% de cobertura nas classes funcionais, incluindo a validação de `FormDeliveryChannel` (webhook/telegram) e de `acceptedFileTypes`.
- Entregar em rodadas:
  1. `FormDefinition`, `Submission` (entities) + `FormDefinitionRepository`, `SubmissionRepository` + testes `@DataJpaTest`.
  2. `FormDefinitionMapper`, `SubmissionMapper` (MapStruct) + testes de mapper.
  3. `FormService`, `SubmissionService` (regras das Seções C/D) + testes com mocks — cada regra de validação (campo obrigatório, label duplicado, canal de entrega inválido, tipo de arquivo fora da lista) com teste do caminho feliz e da rejeição.
  4. `FormController`, `SubmissionController` (endpoints da Seção B/D) + testes `@WebMvcTest` + validação via `curl`.

## Critérios de aceite

- [ ] Criar/editar definição de formulário funciona.
- [ ] `field-types` retorna o catálogo fixo.
- [ ] Submissions podem ser listadas por formulário e no agregado do produto.
- [ ] Publicar formulário sem nenhum campo obrigatório é rejeitado.
- [ ] Detalhe de uma submission específica retorna `answersJson` completo.
- [ ] `PUT .../delivery` salva múltiplos canais simultâneos; `webhook` sem `url` válida e `telegram` sem `chatId`/`botToken` são rejeitados.
- [ ] Campo `Upload` com `acceptedFileTypes` rejeita arquivo fora da lista (ex.: enviar `.png` quando só `application/pdf` é aceito).
- [ ] Formulário/submission de produto fora do escopo do usuário retorna 404 (não 403).
- [ ] Produto com módulo `FORMS` desabilitado retorna 403 `MODULE_DISABLED` em qualquer endpoint desta etapa.
- [ ] `mvn clean verify` confirma 100% de cobertura nas classes elegíveis desta etapa (JaCoCo).
- [ ] `FormDefinitionRepository`/`SubmissionRepository` têm Javadoc na interface e em todo método.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/forms
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/forms/field-types
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/forms/submissions
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/backend/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio form e submission com builder e respostas"
```
