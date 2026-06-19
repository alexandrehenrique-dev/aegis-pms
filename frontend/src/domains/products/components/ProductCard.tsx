import { useNavigate } from "react-router";
import { ExternalLink, MoreHorizontal } from "lucide-react";
import { Badge, Button, Card } from "../../../shared/components/Primitives";
import { ProductStatusBadge } from "../../../shared/components/ProductStatusBadge";
import { products } from "../mocks/products.mocks";

// Maestro Beton is the only seeded product with modules > 0 and a dedicated
// detail route in this sprint; every other seeded product routes to /products
// (list) since they have no individual mock detail page yet.
const productSlugs: Record<string, string> = { "Maestro Beton": "maestro-beton" };

export function ProductCard({ p }: { p: typeof products[number] }) {
  const navigate = useNavigate();
  const slug = productSlugs[p.name];
  const handleOpen = () => {
    if (!p.modules) { navigate("/products/maestro-beton?empty=1"); return; }
    navigate(slug ? `/products/${slug}` : "/products");
  };
  return (
    <Card>
      <div className="flex items-start justify-between gap-3">
        <div><h3 className="font-semibold">{p.name}</h3><p className="text-sm text-muted-foreground">{p.type}</p></div>
        <button className="rounded-lg p-1 hover:bg-muted"><MoreHorizontal size={18} /></button>
      </div>
      <div className="mt-4 flex flex-wrap gap-2"><ProductStatusBadge status={p.status} /><Badge tone="blue">{p.modules} módulos</Badge></div>
      <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
        <div className="rounded-lg bg-muted p-3"><p className="text-xs text-muted-foreground">Saúde</p><p className="font-semibold">{p.score}</p></div>
        <div className="rounded-lg bg-muted p-3"><p className="text-xs text-muted-foreground">Atividade</p><p className="truncate font-semibold">Hoje</p></div>
      </div>
      <p className="mt-4 text-sm text-muted-foreground">{p.last}</p>
      <Button primary onClick={handleOpen}><ExternalLink size={15} />Abrir produto</Button>
    </Card>
  );
}
