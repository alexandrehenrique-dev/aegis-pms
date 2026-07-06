import { forms, submissionsData, fieldTypes } from "../mocks/forms.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { pagesService } from "../../pages/services/pagesService";
import { slugify } from "../../../shared/utils/slugify";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { currentProductSlugOrId } from "../../../core/products/currentProductContext";
import type { FormDelivery, FormDetail, FormField, FormSummary, ListFieldTypesResponse, ListFormsResponse, ListSubmissionsResponse, SubmissionSummary } from "../contracts/responses";

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
  async listForms(productId: string): Promise<ListFormsResponse> {
    if (IS_API_MODE) return apiClient.get<ListFormsResponse>(`/products/${productId}/forms`);
    const productSlug = currentProductSlugOrId();
    if (!productSlug) return formsStore;
    return formsStore.filter((f) => f.productSlug === productSlug || f.productSlug === productId);
  },

  async getForm(productId: string, formId: string): Promise<FormDetail | FormSummary | undefined> {
    if (IS_API_MODE) return apiClient.get<FormDetail>(`/products/${productId}/forms/${formId}`);
    return formsStore.find((f) => f.id === formId);
  },

  /** Cria um formulário novo no store (bug fix Sprint 20, Tarefa D.1) — antes não existia, e o `FormBuilder` sempre abria com o mesmo formulário hardcoded de exemplo, fosse qual fosse o formulário recém-criado. */
  async createForm(productId: string, payload: { name: string; type: string }): Promise<FormSummary> {
    if (IS_API_MODE) return apiClient.post<FormSummary>(`/products/${productId}/forms`, { ...payload, fields: [] });
    logApiCall("POST", `/api/v1/products/${productId}/forms`, payload);
    const id = `form-${slugify(payload.name)}-${Date.now().toString(36)}`;
    const productSlug = currentProductSlugOrId() ?? productId;
    const created: FormSummary = {
      id, productSlug, name: payload.name, type: payload.type,
      status: "draft", responses: "0", conversion: "—", lastActivity: "agora", publication: "—",
    };
    formsStore.push(created);
    fieldsByFormId[id] = [...DEFAULT_CONTATO_FIELDS];
    return created;
  },

  async getFormFields(productId: string, formId: string): Promise<FormField[]> {
    if (IS_API_MODE) {
      const form = await apiClient.get<FormDetail>(`/products/${productId}/forms/${formId}`);
      return form.fields ?? [];
    }
    return fieldsByFormId[formId] ?? [];
  },

  async saveFormFields(productId: string, formId: string, fields: FormField[]): Promise<void> {
    if (IS_API_MODE) {
      const form = await apiClient.get<FormDetail>(`/products/${productId}/forms/${formId}`);
      await apiClient.put(`/products/${productId}/forms/${formId}`, { name: form.name, type: form.type, fields });
      return;
    }
    logApiCall("PUT", `/api/v1/products/${productId}/forms/${formId}`, { fields });
    fieldsByFormId[formId] = fields;
  },

  makeField,

  async listSubmissions(productId: string, formId: string): Promise<ListSubmissionsResponse> {
    if (IS_API_MODE) return apiClient.get<ListSubmissionsResponse>(`/products/${productId}/forms/${formId}/submissions`);
    return submissionsStore;
  },
  async listFieldTypes(): Promise<ListFieldTypesResponse> {
    // Catálogo estático — mesmo em modo api, retorna local por ora.
    return fieldTypes;
  },
  async markQualified(email: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/submissions/${email}/qualify`);
    const s = submissionsStore.find((x) => x.email === email);
    if (!s) return;
    logApiCall("PATCH", `/api/v1/submissions/${email}/qualify`, { status: "Qualificado" });
    s.status = "Qualificado";
  },
  async assignSubmissions(emails: string[], owner: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post("/submissions/assign", { emails, owner });
    logApiCall("POST", "/api/v1/submissions/assign", { emails, owner });
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
  async removeForm(productId: string, id: string): Promise<void> {
    if (IS_API_MODE) return apiClient.delete(`/products/${productId}/forms/${id}`);
    const productSlug = currentProductSlugOrId() ?? productId;
    const pages = await pagesService.listPages(productSlug);
    const usedIn = pages.find((p) => p.sections.some((s) => (s.type === "contact" || s.type === "form") && s.content.formId === id));
    if (usedIn) throw { status: 409, message: `Este formulário está em uso na página "${usedIn.title}".` };
    logApiCall("DELETE", `/api/v1/products/${productId}/forms/${id}`);
    const index = formsStore.findIndex((f) => f.id === id);
    if (index >= 0) formsStore.splice(index, 1);
    delete fieldsByFormId[id];
  },

  async getDelivery(productId: string, formId: string): Promise<FormDelivery> {
    if (IS_API_MODE) {
      const form = await apiClient.get<FormDetail>(`/products/${productId}/forms/${formId}`);
      return { channels: form.deliveryChannels ?? [] };
    }
    if (!deliveryByFormId[formId]) deliveryByFormId[formId] = emptyDelivery();
    return deliveryByFormId[formId];
  },

  async saveDelivery(productId: string, formId: string, delivery: FormDelivery): Promise<void> {
    if (IS_API_MODE) return apiClient.put(`/products/${productId}/forms/${formId}/delivery`, delivery);
    logApiCall("PUT", `/api/v1/products/${productId}/forms/${formId}/delivery`, delivery);
    deliveryByFormId[formId] = delivery;
  },

  /** Publicar/salvar rascunho deve mudar o `publication` no store (bug fix Sprint 20, Tarefa D.1) — antes só logava a chamada, sem refletir na coluna "Publicação" da lista. `publication` (não `status`) é o campo certo: `status` carrega nuances extras (ex.: "sem respostas") que um simples publicar/salvar rascunho não deve sobrescrever. */
  async saveDraft(productId: string, formId?: string): Promise<void> {
    if (IS_API_MODE && formId) {
      const form = await apiClient.get<FormDetail>(`/products/${productId}/forms/${formId}`);
      return apiClient.put(`/products/${productId}/forms/${formId}`, { name: form.name, type: form.type, fields: form.fields ?? [] });
    }
    logApiCall("PUT", `/api/v1/products/${productId}/forms/${formId ?? "novo-formulario"}`);
    if (formId) {
      const f = formsStore.find((x) => x.id === formId);
      if (f) f.publication = "Rascunho";
    }
  },
  async publish(productId: string, formId?: string): Promise<void> {
    if (IS_API_MODE && formId) return apiClient.post(`/products/${productId}/forms/${formId}/publish`);
    logApiCall("POST", `/api/v1/products/${productId}/forms/${formId ?? "novo-formulario"}/publish`);
    if (formId) {
      const f = formsStore.find((x) => x.id === formId);
      if (f) f.publication = "Publicado";
    }
  },
  /** `formId = "form-contato-comercial"` — `FormPreviewFrame` (tela de preview estático) ainda não recebe o form real por rota; esse é o único form com fixtures em `fieldsByFormId` hoje (ver topo do arquivo). */
  async submitTest(productId: string, formId = "form-contato-comercial"): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${productId}/forms/${formId}/test-submit`);
    logApiCall("POST", `/api/v1/products/${productId}/forms/${formId}/test-submit`);
  },
};
