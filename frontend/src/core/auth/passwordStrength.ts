export function getPasswordStrength(pwd: string): "fraca" | "média" | "forte" {
  if (pwd.length < 8) return "fraca";
  if (pwd.length >= 12 && /\d/.test(pwd) && /[!@#$%^&*]/.test(pwd)) return "forte";
  if (pwd.length >= 8 && /\d/.test(pwd)) return "média";
  return "fraca";
}
