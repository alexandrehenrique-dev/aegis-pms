export type KGEntityType = "Tenant" | "Produto" | "Página" | "Asset" | "Formulário" | "Submission" | "Lead" | "Categoria" | "Tag" | "Autor" | "SEO";
export type KGNode = { id: string; label: string; type: KGEntityType; status: string; x: number; y: number; props: { k: string; v: string }[] };
export type KGEdge = { from: string; to: string; verb: string };

export const KG_W = 154;
export const KG_H = 52;

export const kgNodes: KGNode[] = [
  { id: "tenant", label: "BYOP", type: "Tenant", status: "ativo", x: 355, y: 20, props: [{ k: "Plano", v: "Pro" }, { k: "Produtos", v: "6" }, { k: "Usuários", v: "21" }] },
  { id: "produto", label: "Maestro Beton", type: "Produto", status: "ativo", x: 355, y: 118, props: [{ k: "Tipo", v: "Site Institucional" }, { k: "Módulos", v: "6" }, { k: "Saúde", v: "94%" }] },
  { id: "pg-home", label: "Página Home", type: "Página", status: "publicado", x: 55, y: 234, props: [{ k: "Idioma", v: "PT-BR" }, { k: "Versão", v: "v18" }, { k: "Autor", v: "Marina Costa" }] },
  { id: "asset", label: "hero-maestro.jpg", type: "Asset", status: "ativo", x: 235, y: 234, props: [{ k: "Tipo", v: "Imagem" }, { k: "Tamanho", v: "2.4 MB" }, { k: "Dimensões", v: "1920×1080" }] },
  { id: "form", label: "Form: Orçamento", type: "Formulário", status: "ativo", x: 450, y: 234, props: [{ k: "Respostas", v: "93" }, { k: "Conversão", v: "9.4%" }, { k: "Status", v: "Publicado" }] },
  { id: "seo", label: "SEO: Home", type: "SEO", status: "ativo", x: 650, y: 234, props: [{ k: "Title", v: "Maestro Beton" }, { k: "Score", v: "94/100" }, { k: "Indexada", v: "Sim" }] },
  { id: "autor", label: "Marina Costa", type: "Autor", status: "ativo", x: 40, y: 370, props: [{ k: "Papel", v: "Product Manager" }, { k: "Conteúdos", v: "12" }, { k: "Tenant", v: "BYOP" }] },
  { id: "cat", label: "Institucional", type: "Categoria", status: "ativo", x: 210, y: 370, props: [{ k: "Conteúdos", v: "4" }, { k: "Tags", v: "2" }, { k: "Produto", v: "Maestro Beton" }] },
  { id: "tag", label: "#hero", type: "Tag", status: "ativo", x: 390, y: 370, props: [{ k: "Assets", v: "3" }, { k: "Páginas", v: "1" }, { k: "Uso", v: "Header + SEO" }] },
  { id: "sub", label: "Submission #93", type: "Submission", status: "novo", x: 590, y: 370, props: [{ k: "Origem", v: "Página Home" }, { k: "Score", v: "87" }, { k: "Data", v: "Hoje 14:22" }] },
  { id: "lead", label: "Camila Rocha", type: "Lead", status: "qualificado", x: 590, y: 490, props: [{ k: "Email", v: "camila@studio.com" }, { k: "Status", v: "Qualificado" }, { k: "Fit", v: "Alto" }] },
];

export const kgEdges: KGEdge[] = [
  { from: "tenant", to: "produto", verb: "possui" },
  { from: "produto", to: "pg-home", verb: "possui" },
  { from: "produto", to: "asset", verb: "possui" },
  { from: "produto", to: "form", verb: "possui" },
  { from: "produto", to: "seo", verb: "possui" },
  { from: "pg-home", to: "asset", verb: "usa" },
  { from: "pg-home", to: "form", verb: "captura via" },
  { from: "pg-home", to: "autor", verb: "escrito por" },
  { from: "pg-home", to: "cat", verb: "pertence a" },
  { from: "asset", to: "tag", verb: "classificado por" },
  { from: "seo", to: "pg-home", verb: "descreve" },
  { from: "form", to: "sub", verb: "gera" },
  { from: "sub", to: "lead", verb: "cria" },
];

export const kgColor: Record<string, string> = {
  Tenant: "#7c3aed", Produto: "#7c3aed", Página: "#2563eb", Asset: "#0891b2",
  Formulário: "#059669", Submission: "#d97706", Lead: "#16a34a",
  Categoria: "#b45309", Tag: "#92400e", Autor: "#dc2626", SEO: "#6d28d9",
};
