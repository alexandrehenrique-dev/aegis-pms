import { slugify } from "./slugify";

/**
 * Gera o slug de rota para qualquer produto a partir do nome.
 * Usado por ProductCard, ProductsList e roles.ts para montar
 * o path `/products/:slug`.
 * A função sempre retorna um valor não-vazio desde que `productName` seja
 * não-vazio — o fallback "produto" cobre o caso de nome vazio.
 */
export function getProductSlug(productName: string): string {
  return slugify(productName) || "produto";
}
