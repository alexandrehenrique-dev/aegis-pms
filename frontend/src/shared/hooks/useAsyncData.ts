import { useEffect, useState } from "react";

type AsyncState<T> = { data: T | null; loading: boolean; error: boolean };

/**
 * Carrega dados de um service (hoje sempre resolvendo sobre mocks, depois
 * sobre chamadas reais) e expõe o trio loading/error/data que as
 * páginas usam para decidir entre skeleton, PartialErrorWidget ou conteúdo.
 * `deps` segue a mesma semântica de useEffect — uma chamada nova é disparada
 * quando algum item mudar.
 */
export function useAsyncData<T>(loader: () => Promise<T>, deps: ReadonlyArray<unknown> = []): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({ data: null, loading: true, error: false });

  useEffect(() => {
    let active = true;
    setState({ data: null, loading: true, error: false });
    loader()
      .then((data) => { if (active) setState({ data, loading: false, error: false }); })
      .catch(() => { if (active) setState({ data: null, loading: false, error: true }); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}
