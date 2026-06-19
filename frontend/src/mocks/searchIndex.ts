// Cross-cutting global search (⌘K) index. Originally `searchIndex` in App.tsx,
// pointing at `Screen` values; now points at real route paths.
export type SearchEntry = { cat: string; label: string; sub: string; path: string };

export const searchIndex: SearchEntry[] = [
  { cat: "Produtos", label: "Maestro Beton", sub: "Site Institucional · Ativo", path: "/products/maestro-beton" },
  { cat: "Produtos", label: "Aion Logbook", sub: "Jogo / Experimento · Pendente", path: "/products" },
  { cat: "Produtos", label: "Eirene UI", sub: "Design System · Ativo", path: "/products" },
  { cat: "Produtos", label: "Genesis", sub: "Produto SaaS · Ativo", path: "/products" },
  { cat: "Conteúdo", label: "Página Home", sub: "Published · PT-BR", path: "/content/home/editor" },
  { cat: "Conteúdo", label: "Sobre o Maestro", sub: "In Review · PT-BR", path: "/content/sobre/editor" },
  { cat: "Conteúdo", label: "Galeria", sub: "In Review · PT-BR", path: "/content" },
  { cat: "Conteúdo", label: "Depoimentos", sub: "Published · ES-ES", path: "/content" },
  { cat: "Assets", label: "hero-maestro-beton.jpg", sub: "Imagem · 2.4 MB", path: "/assets/hero-maestro-beton" },
  { cat: "Assets", label: "galeria-casamento-01.jpg", sub: "Imagem · 3.1 MB", path: "/assets/galeria-casamento-01" },
  { cat: "Assets", label: "release-institucional.pdf", sub: "PDF · 1.2 MB", path: "/assets/release-institucional" },
  { cat: "Formulários", label: "Contato Comercial", sub: "248 respostas · Publicado", path: "/forms" },
  { cat: "Formulários", label: "Orçamento Maestro", sub: "93 respostas · Publicado", path: "/forms" },
  { cat: "Formulários", label: "RSVP Evento Junho", sub: "0 respostas · Rascunho", path: "/forms/new" },
  { cat: "Entidades KG", label: "Produto Maestro Beton", sub: "Raiz · ativo", path: "/knowledge/entities/produto" },
  { cat: "Entidades KG", label: "Página Home", sub: "Página · published", path: "/knowledge/entities/pg-home" },
  { cat: "Configurações", label: "Usuários", sub: "Gestão de equipe", path: "/users" },
  { cat: "Configurações", label: "Permissões", sub: "Matriz de acesso", path: "/settings/permissions" },
  { cat: "Configurações", label: "Auditoria", sub: "Timeline de eventos", path: "/audit" },
];
