/**
 * Validações inline de formulário (Sprint 19) — espelham em formato/
 * obrigatoriedade as anotações Jakarta Validation que o backend aplica
 * (ADR-0019). Regras de negócio (e-mail duplicado, slug único) continuam
 * sendo responsabilidade do backend; o frontend só trata a mensagem de erro
 * retornada por ele.
 */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SLUG_PATTERN = /^[a-z0-9-]+$/;

export function emailError(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) return "E-mail é obrigatório.";
  if (!EMAIL_PATTERN.test(trimmed)) return "Informe um e-mail válido.";
  return undefined;
}

export function textLengthError(value: string, min: number, max: number, fieldLabel: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) return `${fieldLabel} é obrigatório.`;
  if (trimmed.length < min || trimmed.length > max) return `${fieldLabel} deve ter entre ${min} e ${max} caracteres.`;
  return undefined;
}

export function slugError(value: string, min = 3, max = 50): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) return "Identificador é obrigatório.";
  if (!SLUG_PATTERN.test(trimmed)) return "Use apenas letras minúsculas, números e hífens.";
  if (trimmed.length < min || trimmed.length > max) return `Deve ter entre ${min} e ${max} caracteres.`;
  return undefined;
}
