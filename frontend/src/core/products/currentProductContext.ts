/**
 * ID do produto efetivo da sessão (`useAuth().effectiveProduct.id`), espelhado
 * aqui pelo `AuthProvider` a cada troca — mesmo papel de
 * `setNotificationsCurrentUser` (core/notifications/services/notificationsService.ts):
 * permite que services de baixo nível (analytics, assets, knowledge graph)
 * montem `/products/{id}/...` sem precisar receber `productId` como argumento
 * em toda chamada, já que hoje eles são invocados sem nenhum contexto de
 * produto explícito nos call sites (telas ainda não passam esse parâmetro).
 */
let currentProductId: string | null = null;
let currentProductSlug: string | null = null;

export function setCurrentProductId(productId: string | null) {
  currentProductId = productId;
}

export function setCurrentProductSlug(productSlug: string | null) {
  currentProductSlug = productSlug;
}

/** Lança se chamado em modo api sem produto efetivo selecionado — nenhuma chamada `/products/{id}/...` é válida sem isso. */
export function requireCurrentProductId(): string {
  if (!currentProductId) throw new Error("Nenhum produto efetivo selecionado — chamada de API dependente de produto fora de contexto.");
  return currentProductId;
}

/** Slug do produto efetivo para stores mockados que ainda indexam por slug. */
export function currentProductSlugOrId(): string | null {
  return currentProductSlug ?? currentProductId;
}
