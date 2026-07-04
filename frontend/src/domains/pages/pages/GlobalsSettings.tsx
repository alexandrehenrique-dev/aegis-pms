import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { Button, Card, Field, PageHeader, SkeletonLines } from "../../../shared/components/Primitives";
import { MediaField } from "../../../shared/components/MediaField";
import { ItemsCrudEditor } from "../components/ItemsCrudEditor";
import { useCurrentProduct } from "../../../core/products/useCurrentProduct";
import { useAsyncData } from "../../../shared/hooks/useAsyncData";
import { globalsService } from "../services/globalsService";
import { toast } from "../../../core/notifications/toast";
import type { NavLink, ProductGlobals, SocialLink } from "../contracts/globals";

/**
 * Navbar, footer e redes sociais (ADR-0013, Sprint 13, Tarefa G.3) — editados
 * uma única vez por produto aqui, nunca mais como bloco de uma página
 * específica. Toda página renderiza isto automaticamente (ver `GlobalChrome`).
 */
export function GlobalsSettings() {
  const { product } = useCurrentProduct();
  const productSlug = product ? product.id : "p1";
  const { data: loaded, loading } = useAsyncData(() => globalsService.getGlobals(productSlug), [productSlug]);
  const [globals, setGlobals] = useState<ProductGlobals | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => setGlobals(loaded ?? null), [loaded]);

  if (loading || !globals) return <SkeletonLines />;

  const handleSave = async () => {
    setSaving(true);
    try {
      await globalsService.updateGlobals(productSlug, globals);
      toast.success("Entidades globais atualizadas.", { description: "Navbar, footer e redes sociais refletem em todas as páginas." });
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Navbar, footer e redes sociais" module="Páginas" desc="Configuração única por produto — reflete automaticamente em toda página." badge={product?.name ?? "Produto"}>
        <Button primary onClick={handleSave} disabled={saving}>{saving && <Loader2 size={15} className="animate-spin" />}{saving ? "Salvando..." : "Salvar alterações"}</Button>
      </PageHeader>

      <div className="grid gap-4">
        <Card>
          <h2 className="mb-3 text-lg font-semibold">Navbar</h2>
          <MediaField
            label="Logo"
            value={globals.navbar.logoAssetId ?? ""}
            typeFilter="imagem"
            onChange={(logoAssetId) => setGlobals({ ...globals, navbar: { ...globals.navbar, logoAssetId } })}
          />
          <div className="mt-3">
            <p className="mb-2 text-sm font-medium">Links do menu ({globals.navbar.links.length})</p>
            <ItemsCrudEditor
              items={globals.navbar.links}
              newItem={{ label: "", href: "" } as NavLink}
              onChange={(links) => setGlobals({ ...globals, navbar: { ...globals.navbar, links: links as unknown as NavLink[] } })}
            />
          </div>
        </Card>

        <Card>
          <h2 className="mb-3 text-lg font-semibold">Footer</h2>
          <Field label="Endereço" value={globals.footer.addressText ?? ""} onChange={(addressText) => setGlobals({ ...globals, footer: { ...globals.footer, addressText } })} />
          <div className="mt-3">
            <p className="mb-2 text-sm font-medium">Links do rodapé ({globals.footer.links.length})</p>
            <ItemsCrudEditor
              items={globals.footer.links}
              newItem={{ label: "", href: "" } as NavLink}
              onChange={(links) => setGlobals({ ...globals, footer: { ...globals.footer, links: links as unknown as NavLink[] } })}
            />
          </div>
        </Card>

        <Card>
          <h2 className="mb-3 text-lg font-semibold">Redes sociais</h2>
          <ItemsCrudEditor
            items={globals.socialLinks}
            newItem={{ platform: "", href: "" } as SocialLink}
            onChange={(socialLinks) => setGlobals({ ...globals, socialLinks: socialLinks as unknown as SocialLink[] })}
          />
        </Card>

        <Card>
          <h2 className="mb-3 text-lg font-semibold">WhatsApp flutuante</h2>
          <label className="mb-3 flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={globals.floatingWhatsapp?.enabled ?? false}
              onChange={(e) => setGlobals({ ...globals, floatingWhatsapp: { enabled: e.target.checked, number: globals.floatingWhatsapp?.number ?? "", message: globals.floatingWhatsapp?.message ?? "" } })}
              className="accent-primary"
            />
            Exibir botão flutuante de WhatsApp em todas as páginas
          </label>
          {globals.floatingWhatsapp?.enabled && (
            <div className="grid gap-3 md:grid-cols-2">
              <Field label="Número" value={globals.floatingWhatsapp.number} onChange={(number) => setGlobals({ ...globals, floatingWhatsapp: { ...globals.floatingWhatsapp!, number } })} />
              <Field label="Mensagem inicial" value={globals.floatingWhatsapp.message} onChange={(message) => setGlobals({ ...globals, floatingWhatsapp: { ...globals.floatingWhatsapp!, message } })} />
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
