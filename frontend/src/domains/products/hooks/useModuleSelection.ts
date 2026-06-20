import { useMemo, useState } from "react";
import { KNOWLEDGE_GRAPH_DEPENDENCY, PRODUCT_TYPE_MODULE_DEFAULTS, type ProductTypeKey } from "../../../core/products/moduleDefaults";

/**
 * Seleção de módulos por tipo de produto — compartilhada entre
 * `CreateProductForm` e `EditProductModal` para que editar um produto
 * tenha exatamente a mesma funcionalidade de adicionar/remover módulos que
 * criar um produto (mesmos defaults por tipo, mesma regra de dependência do
 * Knowledge Graph).
 */
export function useModuleSelection(initialType: ProductTypeKey, initialModules?: string[]) {
  const [type, setTypeState] = useState<ProductTypeKey>(initialType);
  const [selectedModules, setSelectedModules] = useState<Set<string>>(
    () => new Set(initialModules ?? (PRODUCT_TYPE_MODULE_DEFAULTS[initialType] ?? []).filter((m) => m.default).map((m) => m.key)),
  );

  const moduleOptions = useMemo(() => PRODUCT_TYPE_MODULE_DEFAULTS[type] ?? [], [type]);

  const setType = (next: string) => {
    const nextType = next as ProductTypeKey;
    setTypeState(nextType);
    setSelectedModules(new Set((PRODUCT_TYPE_MODULE_DEFAULTS[nextType] ?? []).filter((m) => m.default).map((m) => m.key)));
  };

  const toggleModule = (key: string) => {
    setSelectedModules((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
        if (key === KNOWLEDGE_GRAPH_DEPENDENCY) next.delete("Knowledge Graph");
      } else {
        next.add(key);
      }
      return next;
    });
  };

  return { type, setType, moduleOptions, selectedModules, toggleModule, selectedList: Array.from(selectedModules) };
}
