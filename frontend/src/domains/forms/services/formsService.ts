import { forms, submissionsData, fieldTypes } from "../mocks/forms.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { pagesService } from "../../pages/services/pagesService";
import type { FormDelivery, FormField, FormSummary, ListFieldTypesResponse, ListFormsResponse, ListSubmissionsResponse, SubmissionSummary } from "../contracts/responses";

const formsStore: FormSummary[] = forms.map(([id, productSlug, name, type, status, responses, conversion, lastActivity, publication]) => ({
  id, productSlug, name, type, status, responses, conversion, lastActivity, publication,
}));

const submissionsStore: SubmissionSummary[] = submissionsData.map(([date, name, email, source, status, owner, score]) => ({
  date, name, email, source, status, owner, score,
}));

function makeField(type: string): FormField {
  return { id: `${type}-${Date.now()}`, type, label: type, placeholder: "", required: true, help: "", validation: "Nenhuma", mask: "—", defaultValue: "", condition: "Sempre visível" };
}

const DEFAULT_CONTATO_FIELDS: FormField[] = [
  { id: "nome", type: "Texto", label: "Nome", placeholder: "Seu nome", required: true, help: "", validation: "Nenhuma", mask: "—", defaultValue: "", condition: "Sempre visível" },
  { id: "email", type: "Email", label: "Email", placeholder: "seu@email.com", required: true, help: "Usaremos este email para responder sua solicitação.", validation: "Email válido", mask: "—", defaultValue: "", condition: "Sempre visível" },
  { id: "telefone", type: "Telefone", label: "Telefone", placeholder: "(11) 99999-9999", required: false, help: "", validation: "Telefone válido", mask: "—", defaultValue: "", condition: "Sempre visível" },
  { id: "mensagem", type: "Textarea", label: "Mensagem", placeholder: "", required: true, help: "", validation: "Nenhuma", mask: "—", defaultValue: "", condition: "Sempre visível" },
];

// Store em memória só para a sessão do navegador — ver nota equivalente em
// domains/products/services/productsService.ts. Campos de um formulário
// nunca tiveram persistência nenhuma antes da Tarefa K (FormBuilder sempre
// nascia com o mesmo array hardcoded, fosse qual fosse o formulário aberto).
const fieldsByFormId: Record<string, FormField[]> = {
  "form-contato-comercial": DEFAULT_CONTATO_FIELDS,
};

const deliveryByFormId: Record<string, FormDelivery> = {};

function emptyDelivery(): FormDelivery {
  return {
    channels: [
      { type: "email", config: { address: "" }, enabled: false },
      { type: "whatsapp", config: { number: "" }, enabled: false },
      { type: "telegram", config: { chatId: "", botToken: "" }, enabled: false },
      { type: "webhook", config: { url: "", method: "POST" }, enabled: false },
    ],
  };
}

export const formsService = {
  async listForms(productSlug?: string): Promise<ListFormsResponse> {
    return productSlug ? formsStore.filter((f) => f.productSlug === productSlug) : formsStore;
  },

  async getForm(id: string): Promise<FormSummary | undefined> {
    return formsStore.find((f) => f.id === id);
  },

  async getFormFields(id: string): Promise<FormField[]> {
    return fieldsByFormId[id] ?? [];
  },

  async saveFormFields(id: string, fields: FormField[]): Promise<void> {
    logApiCall("PUT", `/api/v1/products/{productId}/forms/${id}/fields`, { fields });
    fieldsByFormId[id] = fields;
  },

  makeField,

  async listSubmissions(): Promise<ListSubmissionsResponse> {
    return submissionsStore;
  },
  async listFieldTypes(): Promise<ListFieldTypesResponse> {
    return fieldTypes;
  },
  async markQualified(email: string): Promise<void> {
    const s = submissionsStore.find((x) => x.email === email);
    if (!s) return;
    logApiCall("PATCH", `/api/v1/products/{productId}/forms/submissions/${email}`, { status: "Qualificado" });
    s.status = "Qualificado";
  },
  async assignSubmissions(emails: string[], owner: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/forms/submissions/assign", { emails, owner });
    emails.forEach((email) => {
      const s = submissionsStore.find((x) => x.email === email);
      if (s) s.owner = owner;
    });
  },

  /**
   * Bloqueia a exclusão se algum bloco `contact`/`form` de alguma página do
   * produto referencia este `formId` (Sprint 12, Tarefa K.6) — evita deixar
   * uma página com referência quebrada.
   */
  async removeForm(id: string, productSlug: string): Promise<void> {
    const pages = await pagesService.listPages(productSlug);
    const usedIn = pages.find((p) => p.sections.some((s) => (s.type === "contact" || s.type === "form") && s.content.formId === id));
    if (usedIn) throw { status: 409, message: `Este formulário está em uso na página "${usedIn.title}".` };
    logApiCall("DELETE", `/api/v1/products/${productSlug}/forms/${id}`);
    const index = formsStore.findIndex((f) => f.id === id);
    if (index >= 0) formsStore.splice(index, 1);
    delete fieldsByFormId[id];
  },

  async getDelivery(formId: string): Promise<FormDelivery> {
    if (!deliveryByFormId[formId]) deliveryByFormId[formId] = emptyDelivery();
    return deliveryByFormId[formId];
  },

  async saveDelivery(formId: string, delivery: FormDelivery): Promise<void> {
    logApiCall("PUT", `/api/v1/products/{productId}/forms/${formId}/delivery`, delivery);
    deliveryByFormId[formId] = delivery;
  },

  async saveDraft(): Promise<void> {
    logApiCall("PUT", "/api/v1/products/{productId}/forms/{formId}");
  },
  async publish(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/forms/{formId}/publish");
  },
  async submitTest(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/forms/{formId}/test-submit");
  },
};
