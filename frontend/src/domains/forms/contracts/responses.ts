export type FormSummary = {
  id: string; productSlug: string;
  name: string; type: string; status: string; responses: string;
  conversion: string; lastActivity: string; publication: string;
};

/** Campo de um formulário no `FormBuilder` — tipo é o nome da entrada da paleta (`Texto`, `Email`, etc.) escolhida ao adicionar. */
export type FormField = {
  id: string;
  type: string;
  label: string;
  placeholder: string;
  required: boolean;
  help: string;
  validation: string;
  mask: string;
  defaultValue: string;
  condition: string;
  /** Formatos aceitos quando `type === "Upload"` (Sprint 12, Tarefa K.7) — vazio = aceita qualquer formato. */
  acceptedFormats?: string[];
};

export type SubmissionSummary = {
  date: string; name: string; email: string; source: string;
  status: string; owner: string; score: string;
};

export type ListFormsResponse = FormSummary[];
export type ListSubmissionsResponse = SubmissionSummary[];
export type ListFieldTypesResponse = string[];
