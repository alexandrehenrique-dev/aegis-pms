import { Badge } from "../../../shared/components/Primitives";
import { KNOWLEDGE_GRAPH_DEPENDENCIES, type ModuleOption } from "../../../core/products/moduleDefaults";

/** Checkboxes de módulos por tipo de produto — usado idêntico em CreateProductForm e EditProductModal (ver useModuleSelection). */
export function ModuleCheckboxList({ options, selected, onToggle }: { options: ModuleOption[]; selected: Set<string>; onToggle: (key: string) => void }) {
  const missingKnowledgeGraphDependencies = KNOWLEDGE_GRAPH_DEPENDENCIES.filter((dependency) => !selected.has(dependency));
  const knowledgeGraphBlocked = missingKnowledgeGraphDependencies.length > 0;
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((m) => {
        const isKnowledgeGraph = m.key === "Knowledge Graph";
        const disabled = m.comingSoon || (isKnowledgeGraph && knowledgeGraphBlocked);
        const checked = selected.has(m.key) && !m.comingSoon;
        return (
          <label
            key={m.key}
            title={isKnowledgeGraph && knowledgeGraphBlocked ? `Exige os módulos ${missingKnowledgeGraphDependencies.join(" e ")} habilitados` : m.comingSoon ? "Ainda não implementado no backend" : undefined}
            className={`flex items-center gap-2 rounded-full border border-border px-3 py-1.5 text-sm ${disabled ? "opacity-50" : ""}`}
          >
            <input type="checkbox" checked={checked} disabled={disabled} onChange={() => onToggle(m.key)} aria-label={`Módulo ${m.key}`} />
            {m.key}
            {m.comingSoon && <Badge tone="amber">em breve</Badge>}
          </label>
        );
      })}
    </div>
  );
}
