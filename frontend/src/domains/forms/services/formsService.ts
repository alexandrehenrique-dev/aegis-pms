import { forms, submissionsData, fieldTypes } from "../mocks/forms.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { FormSummary, ListFieldTypesResponse, ListFormsResponse, ListSubmissionsResponse, SubmissionSummary } from "../contracts/responses";

const formsStore: FormSummary[] = forms.map(([name, type, status, responses, conversion, lastActivity, publication]) => ({
  name, type, status, responses, conversion, lastActivity, publication,
}));

const submissionsStore: SubmissionSummary[] = submissionsData.map(([date, name, email, source, status, owner, score]) => ({
  date, name, email, source, status, owner, score,
}));

export const formsService = {
  async listForms(): Promise<ListFormsResponse> {
    return formsStore;
  },
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
