import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader, SelectLike } from "../../../shared/components/Primitives";
import { UnsavedChangesBanner } from "../../../shared/components/Banners";
import { toast } from "../../../core/notifications/toast";
import { productsService } from "../../products/services/productsService";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { PRODUCT_TYPES } from "../../../core/products/moduleDefaults";
import type { ProductStatus } from "../../../shared/types";

const STATUSES: ProductStatus[] = ["Ativo", "Pendente", "Arquivado", "Sem módulos"];
const LOCALES = ["pt-BR", "en-US", "es-ES", "fr-FR", "de-DE"];

export function ProductSettings() {
  const navigate = useNavigate();
  const { product } = useCurrentProduct();

  const [name, setName] = useState(product?.name ?? "");
  const [type, setType] = useState(product?.type ?? "");
  const [status, setStatus] = useState<ProductStatus>(product?.status ?? "Ativo");
  const [locale, setLocale] = useState("pt-BR");
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);

  // Sincroniza quando o produto efetivo muda (ex.: troca de produto no switcher).
  // Guarda por id em vez de depender do objeto inteiro para não resetar os campos
  // em edição a cada atualização otimista de `product`.
  const syncedProductId = useRef<string | undefined>(undefined);
  useEffect(() => {
    if (!product || product.id === syncedProductId.current) return;
    syncedProductId.current = product.id;
    setName(product.name);
    setType(product.type);
    setStatus(product.status);
    setDirty(false);
  }, [product]);

  const markDirty = <T,>(setter: (v: T) => void) => (v: T) => { setter(v); setDirty(true); };

  const handleSave = async () => {
    if (!product) return;
    setSaving(true);
    try {
      await productsService.update(product.id!, {
        name,
        type,
        status,
        modules: product.modulesList ?? [],
      });
      setDirty(false);
      toast.success("Produto atualizado!", { description: `${name} foi salvo com sucesso.` });
    } catch {
      toast.error("Erro ao salvar produto", { description: "Verifique sua conexão e tente novamente." });
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (!product) return navigate(-1);
    setName(product.name);
    setType(product.type);
    setStatus(product.status);
    setDirty(false);
    navigate(-1);
  };

  if (!product) {
    return (
      <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
        Nenhum produto selecionado. Selecione um produto para editar as configurações.
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title="Configurações do produto"
        module="Configurações"
        desc={`Identidade e ciclo de vida de "${product.name}".`}
        badge="Produto"
      >
        <Button onClick={handleCancel}>Cancelar</Button>
        <Button primary onClick={handleSave} disabled={saving || !dirty}>
          {saving && <Loader2 size={15} className="animate-spin" />}
          {saving ? "Salvando..." : "Salvar alterações"}
        </Button>
      </PageHeader>

      {dirty && <UnsavedChangesBanner />}

      <div className="grid gap-4">
        {/* Dados gerais */}
        <Card>
          <h2 className="mb-4 text-lg font-semibold">Dados gerais</h2>
          <div className="grid gap-4 md:grid-cols-2">
            <Field
              label="Nome do produto"
              value={name}
              onChange={markDirty(setName)}
            />
            <Field
              label="Identificador (slug)"
              value={product.key ?? ""}
              locked
            />
            <SelectLike
              label="Tipo"
              value={type}
              options={PRODUCT_TYPES}
              onChange={markDirty(setType)}
            />
            <SelectLike
              label="Idioma padrão"
              value={locale}
              options={LOCALES}
              onChange={markDirty(setLocale)}
            />
          </div>
        </Card>

        {/* Status */}
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Status do produto</h2>
          <p className="mb-3 text-sm text-muted-foreground">
            Produtos <strong>Arquivados</strong> ficam inacessíveis para editores e visualizadores.
            Produtos <strong>Sem módulos</strong> aparecem bloqueados na seleção de produto.
          </p>
          <div className="flex flex-wrap gap-2">
            {STATUSES.map((s) => (
              <button
                key={s}
                onClick={() => markDirty(setStatus)(s)}
                className={`rounded-xl border px-4 py-2 text-sm transition ${
                  status === s
                    ? "border-primary bg-primary/5 font-medium text-primary"
                    : "border-border bg-card text-muted-foreground hover:bg-muted"
                }`}
              >
                {s}
              </button>
            ))}
          </div>
        </Card>

        {/* Módulos */}
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Módulos habilitados</h2>
          <p className="mb-3 text-sm text-muted-foreground">
            Para alterar os módulos deste produto, acesse a aba{" "}
            <button
              className="text-primary underline underline-offset-2"
              onClick={() => navigate("/products/" + (product.id ?? product.name) + "/modules")}
            >
              Módulos
            </button>
            .
          </p>
          {(product.modulesList ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground italic">Nenhum módulo habilitado.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {(product.modulesList ?? []).map((m) => (
                <span key={m} className="rounded-full border border-border bg-muted px-3 py-1 text-sm">
                  {m}
                </span>
              ))}
            </div>
          )}
        </Card>

        {/* Zona de perigo */}
        <Card className="border-destructive/30">
          <h2 className="mb-1 text-lg font-semibold text-destructive">Zona de perigo</h2>
          <p className="mb-3 text-sm text-muted-foreground">
            Arquivar o produto bloqueia o acesso para toda a equipe. Excluir inicia o fluxo de
            exportação e é irreversível.
          </p>
          <div className="flex gap-2">
            <Button
              onClick={() => markDirty(setStatus)("Arquivado")}
              className="border-amber-300 text-amber-700 hover:bg-amber-50"
              disabled={status === "Arquivado"}
            >
              Arquivar produto
            </Button>
          </div>
        </Card>
      </div>
    </>
  );
}
