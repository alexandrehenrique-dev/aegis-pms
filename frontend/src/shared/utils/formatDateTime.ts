/** Formata um valor de `<input type="datetime-local">` (ex.: "2026-07-12T16:00") para exibição em pt-BR. */
export function formatDateTime(value: string): string {
  if (!value) return "data a definir";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}
