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

/**
 * Entrega de respostas (Sprint 13, Tarefa J) — antes, uma submissão só
 * gerava um toast interno, sem nenhum lugar para configurar "para onde
 * mandar". Cada canal expande seus próprios campos de configuração quando
 * marcado; mais de um pode estar habilitado simultaneamente. Persistido
 * junto com a definição do formulário (`PUT .../forms/{formId}/delivery`).
 */
export type DeliveryChannelType = "email" | "whatsapp" | "telegram" | "webhook";
export type DeliveryChannel = { type: DeliveryChannelType; config: Record<string, string>; enabled: boolean };
export type FormDelivery = { channels: DeliveryChannel[] };
