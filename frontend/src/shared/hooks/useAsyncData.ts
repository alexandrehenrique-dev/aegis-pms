import { useEffect, useState } from "react";

type AsyncState<T> = { data: T | null; loading: boolean; error: boolean; rawError: unknown };

/**
 * Carrega dados de um service (hoje sempre resolvendo sobre mocks, depois
 * sobre chamadas reais) e expõe o trio loading/error/data que as
 * páginas usam para decidir entre skeleton, PartialErrorWidget ou conteúdo.
 * `deps` segue a mesma semântica de useEffect — uma chamada nova é disparada
 * quando algum item mudar. `rawError` (K.1, BUG-SPRINT-05) carrega o erro
 * original (ex.: `ApiError` com `code`) para telas que precisam de mensagens
 * contextuais em vez do `PartialErrorWidget` genérico.
 */
export function useAsyncData<T>(loader: () => Promise<T>, deps: ReadonlyArray<unknown> = []): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({ data: null, loading: true, error: false, rawError: null });

  useEffect(() => {
    let active = true;
    setState({ data: null, loading: true, error: false, rawError: null });
    loader()
      .then((data) => { if (active) setState({ data, loading: false, error: false, rawError: null }); })
      .catch((err) => { if (active) setState({ data: null, loading: false, error: true, rawError: err }); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}
