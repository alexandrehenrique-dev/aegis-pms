import type { ProductStatus } from "../../../shared/types";

// `id` espelha o id usado em `core/auth/mocks/users.ts` (mockProductsByUser,
// tenant t1) — sem isto, `enableModule(name, productId)` (Tarefa C.2) nunca
// encontra o produto certo neste store quando chamado a partir de
// `effectiveProduct?.id` (AuthContext), já que os dois mocks são stores
// independentes que só coincidiam pelo `name`.
export const products = [
  {
    id: "p1", name: "Maestro Beton", type: "Site Institucional", status: "Ativo" as ProductStatus,
    modules: 6, last: "Formulário recebeu 3 respostas", score: "94%",
    modulesList: ["Páginas", "Conteúdo", "Assets", "Forms", "SEO", "Analytics"],
  },
  {
    id: "p2", name: "Aion Logbook", type: "Jogo / Experimento", status: "Pendente" as ProductStatus,
    modules: 4, last: "Conteúdo enviado para revisão", score: "71%",
    modulesList: ["Conteúdo", "Assets", "SEO", "Analytics"],
  },
  {
    id: "p3", name: "Eirene UI", type: "Design System", status: "Ativo" as ProductStatus,
    modules: 5, last: "Assets do produto atualizados", score: "88%",
    modulesList: ["Páginas", "Conteúdo", "Assets", "SEO", "Analytics"],
  },
  {
    id: "p4", name: "Genesis", type: "Produto SaaS", status: "Ativo" as ProductStatus,
    modules: 7, last: "SEO global revisado", score: "91%",
    modulesList: ["Conteúdo", "Assets", "Forms", "Analytics", "SEO", "Workflow", "Knowledge Graph"],
  },
  {
    id: "p5", name: "WikiDev", type: "Knowledge Base", status: "Arquivado" as ProductStatus,
    modules: 3, last: "Nova página publicada", score: "62%",
    modulesList: ["Conteúdo", "Knowledge Graph", "SEO"],
  },
  {
    id: "p6", name: "Conecta Talentos", type: "Portal", status: "Sem módulos" as ProductStatus,
    modules: 0, last: "Produto criado há 1 dia", score: "—",
    modulesList: [],
  },
  {
    id: "p11", name: "CMSS", type: "Site Institucional", status: "Ativo" as ProductStatus,
    modules: 5, last: "Página Quem Somos atualizada", score: "85%",
    modulesList: ["Páginas", "Conteúdo", "Assets", "SEO", "Analytics"],
  },
  {
    id: "p12", name: "Alexandre Dev", type: "Portfolio", status: "Ativo" as ProductStatus,
    modules: 4, last: "Novo projeto publicado (Eirene UI)", score: "90%",
    modulesList: ["Portfolio", "Páginas", "SEO", "Analytics"],
  },
  {
    id: "p13", name: "Loki", type: "Biblioteca Filosófica", status: "Ativo" as ProductStatus,
    modules: 4, last: "Novo manifesto publicado", score: "77%",
    modulesList: ["Conteúdo", "Knowledge Graph", "SEO", "Analytics"],
  },
];
