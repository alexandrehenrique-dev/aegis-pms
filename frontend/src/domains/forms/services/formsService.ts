import { forms, submissionsData, fieldTypes } from "../mocks/forms.mocks";
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
};
