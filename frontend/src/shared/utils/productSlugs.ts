// Maestro Beton é o único produto seed com módulos > 0 e rota de detalhe
// dedicada nesta sprint — os demais produtos mock não têm uma página própria
// ainda. Usado tanto por domains/products (abrir um produto a partir da
// lista) quanto por core/auth (redirecionar editor/viewer direto para o
// produto já selecionado no login — ver core/permissions/roles.ts).
const productSlugs: Record<string, string> = { "Maestro Beton": "maestro-beton" };

export function getProductSlug(productName: string): string | undefined {
  return productSlugs[productName];
}
