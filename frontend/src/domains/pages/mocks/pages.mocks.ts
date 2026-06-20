import type { Page } from "../contracts/responses";

/**
 * Mocks fiéis aos contratos de negócio reais analisados na Sprint 11
 * (`docs/sprints/11_modelo_paginas_blocos_knowledge_graph_e_mocks_produtos.md`,
 * Tarefa E.2). Só os produtos com o módulo "Páginas" no conjunto padrão
 * (Site Institucional, Portal, Portfolio — ver `core/products/moduleDefaults.ts`)
 * têm `pages` populado aqui; Knowledge Base e Library/Books/Music são
 * predominantemente `content`/`knowledge` (ver mocks daqueles domínios).
 */
export const pagesByProduct: Record<string, Page[]> = {
  "maestro-beton": [
    {
      id: "mb-home",
      productSlug: "maestro-beton",
      slug: "home",
      title: "Home",
      locale: "pt-BR",
      status: "published",
      version: 18,
      seo: { title: "Maestro Beton | Experiências", description: "Conheça experiências e apresentações do Maestro Beton." },
      sections: [
        {
          id: "mb-home-s1",
          type: "hero",
          label: "Hero",
          order: 0,
          content: {
            title: "Maestro Beton: experiências que conectam pessoas",
            subtitle: "Eventos, cultura e encontros em uma plataforma institucional governada.",
            image: { src: "hero-maestro-beton.jpg", alt: "Maestro Beton em apresentação ao vivo" },
            ctaPrimary: { label: "Solicitar orçamento", href: "#contato" },
            ctaSecondary: { label: "Ver apresentações", href: "#agenda" },
          },
          settings: { alignment: "center", visibility: "visible" },
        },
        {
          id: "mb-home-s2",
          type: "card-list",
          label: "Serviços",
          order: 1,
          content: {
            title: "Serviços",
            items: [
              { title: "Casamentos", desc: "Repertório personalizado para a cerimônia e a festa.", icon: "heart" },
              { title: "Eventos corporativos", desc: "Trilha sonora para confraternizações e lançamentos.", icon: "briefcase" },
              { title: "Shows", desc: "Apresentações autorais e covers para o público geral.", icon: "mic" },
            ],
          },
        },
        {
          id: "mb-home-s3",
          type: "gallery",
          label: "Galeria",
          order: 2,
          content: {
            items: [
              { src: "galeria-1.jpg", alt: "Apresentação em casamento ao ar livre" },
              { src: "galeria-2.jpg", alt: "Show em evento corporativo" },
              { src: "galeria-3.jpg", alt: "Maestro Beton ao piano" },
            ],
          },
        },
        {
          id: "mb-home-s4",
          type: "event-list",
          label: "Agenda",
          order: 3,
          content: { title: "Agenda" },
          source: { type: "contentType", contentType: "evento", filter: { status: "published", upcoming: true } },
        },
        {
          id: "mb-home-s5",
          type: "contact",
          label: "Contato / Orçamento",
          order: 4,
          content: {
            title: "Solicitar orçamento",
            fields: [
              { name: "tipoEvento", label: "Tipo de evento", type: "select", options: ["Casamento", "Evento corporativo", "Show"], required: true },
              { name: "data", label: "Data do evento", type: "date", required: true },
              { name: "cidade", label: "Cidade", type: "text", required: true },
              { name: "convidados", label: "Número de convidados", type: "number", required: false },
            ],
          },
        },
      ],
    },
  ],

  "conecta-talentos": [
    {
      id: "ct-home",
      productSlug: "conecta-talentos",
      slug: "home",
      title: "Home",
      locale: "pt-BR",
      status: "published",
      version: 4,
      seo: { title: "Conecta Talentos | Recrutamento com propósito", description: "Conectando pessoas e empresas com propósito." },
      sections: [
        {
          id: "ct-home-s1",
          type: "hero",
          label: "Hero",
          order: 0,
          content: {
            title: "Conectando Pessoas e Empresas com Propósito",
            subtitle: "Consultoria de RH e recrutamento focada em encaixe cultural, não só técnico.",
            ctaPrimary: { label: "Ver vagas abertas", href: "#vagas" },
          },
        },
        {
          id: "ct-home-s2",
          type: "text",
          label: "Quem Somos",
          order: 1,
          content: {
            title: "Quem Somos",
            body: "Há mais de 10 anos conectamos talentos a empresas que valorizam propósito, diversidade e crescimento de carreira.",
          },
        },
        {
          id: "ct-home-s3",
          type: "card-list",
          label: "Vagas abertas",
          order: 2,
          // JobPosting ainda não é entidade de backend (Sprint 02, etapa 23 — registrado, não implementado).
          // Mock no formato já definido para a futura entidade, para a UI já consumir o shape certo.
          content: {
            title: "Vagas abertas",
            items: [
              {
                id: "job-1", title: "Analista de RH Pleno", city: "São Paulo, SP", modality: "Híbrido", type: "CLT",
                salaryRange: "R$ 4.500 - R$ 6.000", requirements: ["Graduação em RH/Psicologia", "2+ anos em recrutamento"],
                benefits: ["VR", "Plano de saúde", "Home office flexível"], status: "published",
              },
              {
                id: "job-2", title: "Desenvolvedor(a) Backend Java", city: "Remoto", modality: "Remoto", type: "PJ",
                salaryRange: "R$ 9.000 - R$ 13.000", requirements: ["Spring Boot", "PostgreSQL"],
                benefits: ["Horário flexível", "Auxílio equipamento"], status: "published",
              },
              {
                id: "job-3", title: "Coordenador(a) Comercial", city: "Belo Horizonte, MG", modality: "Presencial", type: "CLT",
                salaryRange: "R$ 7.000 + comissão", requirements: ["5+ anos em gestão comercial"],
                benefits: ["VR", "VA", "Plano de saúde"], status: "published",
              },
              {
                id: "job-4", title: "Estágio em Marketing Digital", city: "São Paulo, SP", modality: "Híbrido", type: "Estágio",
                salaryRange: "R$ 1.800", requirements: ["Cursando Marketing/Comunicação"],
                benefits: ["VR", "Auxílio transporte"], status: "draft",
              },
            ],
          },
        },
        {
          id: "ct-home-s4",
          type: "card-list",
          label: "Blog",
          order: 3,
          content: { title: "Blog" },
          source: { type: "contentType", contentType: "artigo", filter: { status: "published", limit: 3 } },
        },
      ],
    },
  ],

  cmss: [
    {
      id: "cmss-home",
      productSlug: "cmss",
      slug: "home",
      title: "Home",
      locale: "pt-BR",
      status: "published",
      version: 6,
      seo: { title: "CMSS | Corporação Musical São Sebastião", description: "Tradição musical e formação cultural há gerações." },
      sections: [
        {
          id: "cmss-home-s1",
          type: "hero",
          label: "Hero",
          order: 0,
          content: {
            title: "Corporação Musical São Sebastião",
            subtitle: "Tradição, formação musical e comunidade desde fundação da banda.",
            image: { src: "hero-cmss.jpg", alt: "Banda da Corporação Musical São Sebastião em apresentação" },
            ctaPrimary: { label: "Conheça nossa história", href: "/historia" },
            ctaSecondary: { label: "Apoie a CMSS", href: "/apoie" },
          },
        },
        {
          id: "cmss-home-s2",
          type: "feature-grid",
          label: "Pilares",
          order: 1,
          content: {
            items: [
              { title: "Formação musical", desc: "Aulas gratuitas de instrumentos para a comunidade." },
              { title: "Apresentações", desc: "Eventos cívicos, religiosos e culturais ao longo do ano." },
              { title: "Preservação cultural", desc: "Memória viva da música de banda na cidade." },
            ],
          },
        },
        {
          id: "cmss-home-s3",
          type: "event-list",
          label: "Próximos eventos",
          order: 2,
          content: { title: "Próxima apresentação" },
          source: { type: "contentType", contentType: "evento", filter: { status: "published", upcoming: true, limit: 3 } },
        },
        {
          id: "cmss-home-s4",
          type: "cta-section",
          label: "CTA Apoie",
          order: 3,
          content: { title: "Ajude a manter essa tradição viva", ctaPrimary: { label: "Quero apoiar", href: "/apoie" } },
        },
      ],
    },
    {
      id: "cmss-quem-somos",
      productSlug: "cmss",
      slug: "quem-somos",
      title: "Quem Somos",
      locale: "pt-BR",
      status: "published",
      version: 3,
      seo: { title: "Quem Somos | CMSS" },
      sections: [
        {
          id: "cmss-qs-s1",
          type: "image-text",
          label: "Apresentação",
          order: 0,
          content: {
            title: "Quem Somos",
            body: "A Corporação Musical São Sebastião reúne músicos voluntários e estudantes em formação, mantendo viva a tradição das bandas de coreto.",
            image: { src: "cmss-grupo.jpg", alt: "Grupo de músicos da CMSS reunido" },
            imagePosition: "right",
          },
        },
        {
          id: "cmss-qs-s2",
          type: "two-column",
          label: "Missão e Valores",
          order: 1,
          content: {
            left: [{ type: "text", content: { title: "Missão", body: "Democratizar o acesso à formação musical de qualidade." } }],
            right: [{ type: "text", content: { title: "Valores", body: "Tradição, comunidade, disciplina e generosidade." } }],
          },
        },
      ],
    },
    {
      id: "cmss-historia",
      productSlug: "cmss",
      slug: "historia",
      title: "História",
      locale: "pt-BR",
      status: "published",
      version: 2,
      seo: { title: "História | CMSS" },
      sections: [
        {
          id: "cmss-hist-s1",
          type: "timeline",
          label: "Linha do tempo",
          order: 0,
          content: {
            items: [
              { date: "1950", title: "Fundação", desc: "Criação da banda por músicos locais." },
              { date: "1978", title: "Primeira sede própria", desc: "Inauguração da sede atual da corporação." },
              { date: "2005", title: "Escola de música", desc: "Início das aulas gratuitas para a comunidade." },
              { date: "2023", title: "75 anos", desc: "Celebração de 75 anos de atividade ininterrupta." },
            ],
          },
        },
      ],
    },
    {
      id: "cmss-agenda",
      productSlug: "cmss",
      slug: "agenda",
      title: "Agenda",
      locale: "pt-BR",
      status: "published",
      version: 4,
      seo: { title: "Agenda | CMSS" },
      sections: [
        {
          id: "cmss-agenda-s1",
          type: "event-list",
          label: "Agenda completa",
          order: 0,
          content: { title: "Agenda de apresentações" },
          source: { type: "contentType", contentType: "evento" },
        },
      ],
    },
    {
      id: "cmss-apoie",
      productSlug: "cmss",
      slug: "apoie",
      title: "Apoie",
      locale: "pt-BR",
      status: "published",
      version: 2,
      seo: { title: "Apoie a CMSS" },
      sections: [
        {
          id: "cmss-apoie-s1",
          type: "rich-text",
          label: "Como apoiar",
          order: 0,
          content: { title: "Como apoiar", body: "<p>A CMSS depende de doações e patrocínio para manter instrumentos, uniformes e a escola de música gratuita.</p>" },
        },
        {
          id: "cmss-apoie-s2",
          type: "faq",
          label: "Perguntas frequentes",
          order: 1,
          content: {
            items: [
              { q: "Posso doar instrumentos usados?", a: "Sim, aceitamos doação de instrumentos em qualquer estado de conservação." },
              { q: "Existe certificado de incentivo fiscal?", a: "Estamos formalizando parceria com lei de incentivo cultural municipal." },
            ],
          },
        },
      ],
    },
    {
      id: "cmss-contato",
      productSlug: "cmss",
      slug: "contato",
      title: "Contato",
      locale: "pt-BR",
      status: "published",
      version: 3,
      seo: { title: "Contato | CMSS" },
      sections: [
        {
          id: "cmss-contato-s1",
          type: "contact",
          label: "Fale com a CMSS",
          order: 0,
          content: {
            title: "Fale com a CMSS",
            fields: [
              { name: "nome", label: "Nome", type: "text", required: true },
              { name: "email", label: "E-mail", type: "email", required: true },
              { name: "mensagem", label: "Mensagem", type: "textarea", required: true },
            ],
          },
        },
        {
          id: "cmss-contato-s2",
          type: "footer",
          label: "Rodapé",
          order: 1,
          content: { address: "Praça Central, s/n — São Sebastião", social: ["instagram", "facebook"] },
        },
      ],
    },
  ],

  "alexandre-dev": [
    {
      id: "ad-home",
      productSlug: "alexandre-dev",
      slug: "home",
      title: "Home",
      locale: "pt-BR",
      status: "published",
      version: 9,
      seo: { title: "Alexandre Silva | Desenvolvedor", description: "Portfólio de projetos, experiências e habilidades técnicas." },
      sections: [
        {
          id: "ad-home-s1",
          type: "hero",
          label: "Hero",
          order: 0,
          content: {
            title: "Alexandre Silva",
            subtitle: "Desenvolvedor full-stack — produtos web, automação e arquitetura de dados.",
            metrics: [
              { label: "Projetos", value: "12" },
              { label: "Anos de experiência", value: "10" },
            ],
            ctaPrimary: { label: "Ver projetos", href: "#projetos" },
          },
        },
        {
          id: "ad-home-s2",
          type: "card-list",
          label: "Projetos",
          order: 1,
          content: {
            title: "Projetos",
            items: [
              { title: "Aion Logbook", desc: "Diário operacional com IA para registro e busca de decisões técnicas.", tags: ["TypeScript", "React"] },
              { title: "Aegis CMS", desc: "Plataforma multi-tenant de gestão de produtos digitais (este projeto).", tags: ["React", "Spring Boot"] },
              { title: "Eirene UI", desc: "Design system e biblioteca de componentes reutilizáveis.", tags: ["TypeScript", "Tailwind"] },
              { title: "Pomodoro App", desc: "Aplicativo de produtividade com técnica Pomodoro.", tags: ["React", "PWA"] },
              { title: "Genesis Lab", desc: "Laboratório de experimentos com geração de produtos via IA.", tags: ["Node.js", "IA"] },
            ],
          },
        },
        {
          id: "ad-home-s3",
          type: "feature-grid",
          label: "Skills",
          order: 2,
          content: {
            title: "Skills",
            items: [
              { title: "Frontend", desc: "React, TypeScript, Tailwind, design systems." },
              { title: "Backend", desc: "Java/Spring Boot, Node.js, PostgreSQL." },
              { title: "Plataforma", desc: "Docker, CI/CD, arquitetura multi-tenant." },
            ],
          },
        },
        {
          id: "ad-home-s4",
          type: "timeline",
          label: "Experiências",
          order: 3,
          content: {
            items: [
              { date: "2024 — atual", title: "Founder, BYOP", desc: "Construção da plataforma Aegis e produtos relacionados." },
              { date: "2019 — 2024", title: "Engenheiro de Software Sênior", desc: "Liderança técnica em produtos de dados e backend." },
              { date: "2015 — 2019", title: "Desenvolvedor Full-stack", desc: "Produtos web para clientes de diversos setores." },
            ],
          },
        },
      ],
    },
  ],
};
