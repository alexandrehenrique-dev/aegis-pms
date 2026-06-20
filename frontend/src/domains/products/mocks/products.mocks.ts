import type { ProductStatus } from "../../../shared/types";

export const products = [
  { name: "Maestro Beton", type: "Site Institucional", status: "Ativo" as ProductStatus, modules: 6, last: "Formulário recebeu 3 respostas", score: "94%" },
  { name: "Aion Logbook", type: "Jogo / Experimento", status: "Pendente" as ProductStatus, modules: 4, last: "Conteúdo enviado para revisão", score: "71%" },
  { name: "Eirene UI", type: "Design System", status: "Ativo" as ProductStatus, modules: 5, last: "Assets do produto atualizados", score: "88%" },
  { name: "Genesis", type: "Produto SaaS", status: "Ativo" as ProductStatus, modules: 7, last: "SEO global revisado", score: "91%" },
  { name: "WikiDev", type: "Knowledge Base", status: "Arquivado" as ProductStatus, modules: 3, last: "Nova página publicada", score: "62%" },
  { name: "Conecta Talentos", type: "Portal", status: "Sem módulos" as ProductStatus, modules: 0, last: "Produto criado há 1 dia", score: "—" },
  { name: "CMSS", type: "Site Institucional", status: "Ativo" as ProductStatus, modules: 5, last: "Página Quem Somos atualizada", score: "85%" },
  { name: "Alexandre Dev", type: "Portfolio", status: "Ativo" as ProductStatus, modules: 4, last: "Novo projeto publicado (Eirene UI)", score: "90%" },
  { name: "Loki", type: "Biblioteca Filosófica", status: "Ativo" as ProductStatus, modules: 4, last: "Novo manifesto publicado", score: "77%" },
];
