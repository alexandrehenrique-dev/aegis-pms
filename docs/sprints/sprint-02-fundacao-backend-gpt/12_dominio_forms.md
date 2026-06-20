# Etapa 12 — Domínio `form`/`submission` (formulários, builder, submissions)

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapa 06 concluída. Usa os packages `form` e `submission` já previstos na etapa 04.

## Contexto fixo

Telas `FormsDashboard`, `FormsList`, `FormBuilder`, `FormPreviewFrame`, `SubmissionTable`, `SubmissionDetails`, `BasicFormAnalytics`, `PublicationPanel` — hoje 100% mock. Payloads conforme `docs/trace/00_endpoints_esperados.md` (Seção B.3).

## Objetivo

CRUD de definição de formulário (campos, ordem, obrigatoriedade) e de submissions recebidas, com publicação.

## Tarefas

### A. Entidades

**FormDefinition**: `id`, `tenantId`, `productId`, `name`, `type`, `status`, `fieldsJson` (array de campos: label, tipo, obrigatório, placeholder, validação, condição de visibilidade — espelhando o painel de propriedades de `FormBuilder.tsx`), `publication`, `createdAt`, `updatedAt`.

**Submission**: `id`, `formId`, `date`, `name`, `email`, `source`, `status`, `ownerSubject`, `score`, `answersJson`, `createdAt`.

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

## Critérios de aceite

- [ ] Criar/editar definição de formulário funciona.
- [ ] `field-types` retorna o catálogo fixo.
- [ ] Submissions podem ser listadas por formulário e no agregado do produto.
- [ ] Publicar formulário sem nenhum campo obrigatório é rejeitado.
- [ ] Detalhe de uma submission específica retorna `answersJson` completo.

## Validação

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/forms
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/forms/field-types
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/<productId>/forms/submissions
```

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): dominio form e submission com builder e respostas"
```
