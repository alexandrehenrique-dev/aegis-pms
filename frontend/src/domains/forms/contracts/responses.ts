export type FormSummary = {
  name: string; type: string; status: string; responses: string;
  conversion: string; lastActivity: string; publication: string;
};

export type SubmissionSummary = {
  date: string; name: string; email: string; source: string;
  status: string; owner: string; score: string;
};

export type ListFormsResponse = FormSummary[];
export type ListSubmissionsResponse = SubmissionSummary[];
export type ListFieldTypesResponse = string[];
