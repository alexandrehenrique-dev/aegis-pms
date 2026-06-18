# O que é o Aegis

O Aegis é a plataforma administrativa central do ecossistema BYOP. Ele é uma plataforma multi-produto, multi-tenant, modular e orientada a contratos, criada para administrar produtos digitais do BYOP e de futuros clientes.

Definição oficial: o Aegis não gerencia páginas como entidade central; ele gerencia produtos digitais. Páginas são apenas uma forma de materializar um produto.

O Aegis administra conteúdo, capacidades, contratos, usuários, assets, fluxos editoriais, permissões, integrações e dados de operação. Ele não renderiza páginas públicas. Frontends externos atuam como renderizadores de contratos JSON, enquanto o Aegis permanece como fonte de verdade.

Fonte utilizada para esta visão: `docs/AEGIS_BKP_RECUPERADO.md`, que consolida o Documento Mestre V1, ADRs e anexos de documentação.

# Problema que resolve

O Aegis resolve o risco de cada produto digital do BYOP nascer como uma solução isolada, com JSONs próprios, componentes específicos, regras hardcoded e contratos improvisados.

Problemas explicitamente identificados na documentação:

- duplicação de regras;
- acoplamento entre frontend e conteúdo;
- contratos frágeis;
- dificuldade de manutenção e evolução;
- falta de governança editorial;
- falta de versionamento;
- falta de permissões por produto;
- falta de histórico;
- dificuldade de escalar para múltiplos clientes;
- risco de virar um Frankenstein arquitetural.

O Aegis nasce para impedir essa fragmentação arquitetural, mantendo uma base comum para produtos digitais distintos.

# Público-alvo

Público-alvo primário:

- administradores BYOP;
- donos de produtos digitais;
- equipes internas responsáveis por conteúdo, operação, UX, infraestrutura e desenvolvimento;
- agentes do ecossistema BYOP, incluindo Aegis, Eirene, Daedalus, Zeus, Athena e Loki.

Público-alvo operacional:

- usuários com membership em tenants;
- donos, administradores, managers, editores e viewers de produtos;
- operadores que precisam gerenciar conteúdo, assets, formulários, SEO, analytics, permissões e auditoria.

Público-alvo futuro:

- clientes externos, caso o Aegis evolua para uma plataforma comercial SaaS.

Lacuna: a documentação não define personas finais com nomes, jornadas detalhadas, nível de maturidade técnica, prioridades por perfil ou critérios comerciais de adoção.

# Casos de uso

Casos de uso centrais:

- cadastrar produtos digitais;
- definir donos de produtos;
- selecionar categoria de produto;
- habilitar features por produto;
- montar menus e módulos a partir das features habilitadas;
- permitir que donos editem conteúdos sem alterar código;
- expor contratos JSON para frontends externos;
- publicar conteúdo para produtos públicos;
- gerir tenants, memberships, roles e convites;
- gerir páginas, seções, blocos, content types, assets, forms, SEO, navegação, revisões, auditoria e analytics.

Produtos fonte usados para modelagem:

- CMSS: site institucional com páginas, navegação, hero, seções, galeria, eventos, timeline, contato, SEO e versionamento;
- Maestro Beton: site comercial com serviços, agenda, repertório, vídeos, galeria, depoimentos, orçamento, WhatsApp e leads;
- Conecta Talentos: portal de RH com empresa, serviços, vagas, banco de talentos, candidaturas, upload de currículo, leads corporativos, blog e pipeline futuro;
- Alexandre Dev: portfólio com projetos, experiências, skills, artigos, downloads, currículo, cases, SEO e analytics;
- Loki: biblioteca autoral com manifestos, poemas, reflexões, trechos, livros, playlists, referências musicais, downloads, anonimato autoral e analytics literário;
- WikiDev: wiki técnica com categorias, tópicos, artigos, busca, relacionados, link preview, comentários, sugestões, bug reports, contribuidores e Telegram.

# Objetivos do produto

Objetivos oficiais:

- ser a fonte de verdade administrativa dos produtos digitais do BYOP;
- permitir gestão multi-produto e multi-tenant;
- tornar conteúdo público editável pelo Aegis, não hardcoded nos frontends;
- desacoplar frontends da estrutura interna do banco;
- entregar contratos JSON estáveis e versionados;
- sustentar governança editorial, revisão, publicação, rollback e auditoria;
- permitir que features governem capacidades reais por produto;
- manter isolamento lógico forte por tenant;
- nascer SaaS Ready, sem implementar billing no MVP;
- crescer sem virar Frankenstein arquitetural.

Objetivo de experiência descrito:

```txt
Administrador BYOP
↓ cadastra Produto
↓ define Donos
↓ seleciona Categoria
↓ seleciona Funcionalidades (Features)
↓ Aegis habilita menus e módulos
↓ Dono acessa painel
↓ Dono edita conteúdos
↓ Frontend externo consome contrato JSON
↓ Produto público é atualizado
```

# Princípios arquiteturais

Princípios oficiais:

- Product First: Produto é a raiz operacional. Não site, não página, não cliente.
- Contract First: frontends consomem contratos, não entidades internas.
- CMS First: conteúdo público editável vive no Aegis; texto final não deve ser hardcoded no frontend.
- Multi-Tenant: isolamento lógico forte por tenant desde o início.
- Modular: núcleo estável, módulos habilitáveis por produto.
- SaaS Ready, não SaaS Enabled: reservar espaço arquitetural para billing, sem cobrança no MVP.
- REST First, GraphQL Future: REST é a API oficial do MVP; GraphQL é evolução futura.
- Segurança por padrão: autenticação, autorização, auditoria e isolamento tenant-aware.
- Evolução sem Frankenstein: crescer mantendo coerência arquitetural.

Decisões arquiteturais aceitas:

- ADR-0001: Product First.
- ADR-0002: Contract First.
- ADR-0003: CMS First.
- ADR-0004: Shared Database + Shared Schema + `tenantId` obrigatório.
- ADR-0005: Keycloak autentica; Aegis autoriza por tenant.
- ADR-0006: REST First.
- ADR-0007: GraphQL Future.
- ADR-0008: JSON Contracts.
- ADR-0009: SPA administrativa servida pelo Spring Boot na mesma origem.
- ADR-0010: PostgreSQL como banco inicial.

# Escopo MVP

O MVP cobre Fase 0, Fase 1 e Fase 2 do roadmap.

Fase 0 - Fundação:

- setup Spring Boot;
- Keycloak;
- PostgreSQL;
- Docker Compose;
- autenticação;
- tenants;
- memberships;
- produtos;
- usuários;
- roles;
- features;
- audit básico;
- SPA administrativa servida pelo backend.

Fase 1 - CMS Core:

- páginas;
- seções;
- blocos;
- assets;
- SEO;
- workflow editorial;
- contrato público;
- revisões por snapshot.

Fase 2 - Formulários e Analytics:

- form builder;
- submissions;
- Telegram como canal operacional;
- eventos de analytics;
- dashboard inicial.

Também pertencem ao MVP:

- API REST versionada em `/api/v1`;
- documentação via OpenAPI;
- contratos JSON canônicos;
- API pública apenas para conteúdo publicado;
- API administrativa tenant-scoped;
- PostgreSQL com Shared Schema;
- Flyway;
- Full Text Search nativo do PostgreSQL;
- cache inicial com Caffeine;
- editor com preview textual/estrutural, não preview visual completo;
- testes de isolamento multi-tenant desde o início.

# Escopo Pós-MVP

Pós-MVP corresponde às fases posteriores ao núcleo inicial, mas ainda antes da visão SaaS completa.

Fase 3 - Módulos reais:

- Institucional/CMSS;
- Maestro Beton;
- Alexandre Dev;
- Conecta Talentos.

Fase 4 - Biblioteca e Wiki:

- Loki;
- WikiDev;
- relações;
- comentários;
- sugestões;
- conhecimento conectado;
- busca.

Recursos também descritos como evolução pós-MVP:

- colaboração mais rica;
- comunidades e fóruns como evolução de comentários/discussões;
- publicação programada;
- preview visual completo do editor;
- busca dedicada ou semântica;
- Redis quando necessário;
- CDN ou host estático separado para a SPA quando houver necessidade de escala ou deploy independente.

Inconsistência ou tensão de escopo: o material bruto da sessão afirma que "o MVP nasce com comentários", mas o Documento Mestre consolidado coloca comentários, sugestões, Loki e WikiDev na Fase 4. Para esta visão oficial, comentários e colaboração devem ser tratados como Pós-MVP/Fase 4 até decisão contrária registrada.

# Escopo Futuro

Futuro estratégico:

- SaaS;
- planos;
- assinaturas;
- billing;
- invoices;
- payment provider;
- loja;
- LMS;
- CRM;
- comunidade;
- GraphQL;
- federation;
- API Gateway;
- motor de busca dedicado;
- busca semântica;
- banco por tenant ou schema por tenant para enterprise;
- realm Keycloak por tenant para enterprise;
- CDN separada para a SPA;
- login social;
- self-service onboarding de tenant.

Regra oficial: o MVP deve reservar espaço arquitetural para billing, mas não deve executar cobrança nem bloquear uso por inadimplência.

# O que NÃO é o Aegis

O Aegis não é:

- um CMS tradicional page-first;
- um renderizador de páginas públicas;
- o frontend público dos produtos;
- uma coleção de JSONs isolados por produto;
- um painel administrativo genérico com todos os módulos sempre expostos;
- uma plataforma SaaS com billing ativo no MVP;
- uma API GraphQL no MVP;
- um e-commerce, LMS, CRM ou comunidade no MVP;
- um sistema com banco/schema/realm separado por tenant no MVP;
- um substituto para frontends externos;
- um lugar onde Markdown define layout;
- um sistema onde o frontend governa conteúdo ou permissões;
- um sistema onde Keycloak decide autorização tenant-specific final.

# Roadmap de evolução

Roadmap oficial:

| Fase | Escopo | Classificação |
|---|---|---|
| Fase 0 - Fundação | Spring Boot, Keycloak, PostgreSQL, Docker Compose, auth, tenants, memberships, produtos, usuários, roles, features, audit básico, SPA servida pelo backend | MVP |
| Fase 1 - CMS Core | Páginas, seções, blocos, assets, SEO, workflow editorial, contrato público, revisões | MVP |
| Fase 2 - Formulários e Analytics | Form builder, submissions, Telegram, analytics events, dashboard inicial | MVP |
| Fase 3 - Módulos Reais | Institucional/CMSS, Maestro Beton, Alexandre Dev, Conecta Talentos | Pós-MVP |
| Fase 4 - Biblioteca e Wiki | Loki, WikiDev, relações, comentários, sugestões, conhecimento conectado, busca | Pós-MVP |
| Fase 5 - SaaS Futuro | Planos, assinaturas, loja, LMS, CRM, comunidade | Futuro |

Evoluções condicionadas a ADR:

- GraphQL;
- federation;
- API Gateway;
- banco por tenant;
- schema por tenant;
- realm por tenant;
- mudança de estratégia de deploy da SPA;
- motor de busca dedicado;
- alterações relevantes em autenticação, autorização, secrets, uploads, rate limit, backup ou tenant isolation.

# Glossário

Produto: unidade digital administrada pelo Aegis, como site institucional, blog, wiki, portal de RH, portfólio, biblioteca, landing page ou futuro e-commerce.

Tenant: fronteira primária de pertencimento, autorização, configuração, auditoria, conteúdo, assets, analytics e billing futuro.

Membership: contrato de acesso contextual de um usuário a um tenant.

Product Membership: vínculo granular de usuário a produto, limitado pela autoridade da role de tenant.

Feature: capacidade funcional controlável do Aegis para um produto.

Feature Catalog: catálogo global de capacidades possíveis.

Product Feature: relação produto-feature que define capacidade real habilitada.

Module: área do sistema onde capacidades vivem no código.

Category: tipo principal do produto, usado para orientar presets iniciais.

Page: unidade navegável de conteúdo.

Section: agrupamento estrutural dentro de uma página.

Block: unidade de composição com tipo e dados.

Content Type: definição de conteúdo estruturado com schema, fields, workflow, SEO, revisão, tradução, permissões, relações e analytics.

Asset: mídia ou arquivo gerenciado pelo Aegis.

Form Type: definição de formulário com schema, campos, validação, workflow, notificações, integrações, analytics, LGPD e permissões.

Revision: snapshot versionado do estado de um recurso editorial.

Publication: ato de apontar uma revisão aprovada como versão publicada.

Audit Log: registro imutável de ação relevante, indicando quem fez, quando, em qual contexto e com qual resultado.

Contract: fronteira JSON estável entre backend e frontend.

API Pública: API consumida por frontends externos, expondo apenas conteúdo publicado.

API Administrativa: API usada pelo painel administrativo, sempre tenant-scoped.

Eirene: SPA administrativa do Aegis.

Keycloak: provedor de identidade responsável por autenticação.

LGPD: conjunto de requisitos de privacidade, consentimento, retenção, exportação e anonimização de dados pessoais.

ADR: registro formal de decisão arquitetural.

SaaS Ready: arquitetura preparada para futura comercialização.

SaaS Enabled: cobrança, planos e billing efetivamente implementados. Não faz parte do MVP.

## Inconsistências identificadas

- Comentários aparecem no material bruto como algo que "nasce no MVP", mas o Documento Mestre consolidado os posiciona em Fase 4 junto com WikiDev, Loki e colaboração. Decisão adotada nesta visão: comentários ficam Pós-MVP até ADR ou ajuste oficial.
- A estrutura REST aparece em dois formatos: o Documento Mestre lista `/public/...` e rotas administrativas por recurso, enquanto o material bruto propõe `/api/v1/public`, `/api/v1/auth`, `/api/v1/admin` e `/api/v1/internal`. Falta decisão final de path convention.
- Há variação de nomenclatura em ADR-0009: "SPA servida pelo Spring Boot" e "SPA servido pelo Spring Boot". É só inconsistência textual, não arquitetural.
- O Documento Mestre afirma que os 10 ADRs foram formalizados, mas também lista decisões conceituais ainda a formalizar, como JSONB controlado, Markdown não é layout, Blocks como estrutura, Revisions por snapshot, Audit logs imutáveis, Telegram como canal, Flyway, Docker Compose inicial e cache evolutivo.

## Lacunas identificadas

- Personas e jornadas detalhadas não estão definidas.
- Critérios de sucesso do produto não estão definidos.
- Métricas de adoção, retenção ou operação não estão definidas.
- Matriz role x ação x feature ainda é conceitual.
- Sincronização Keycloak com base local não está especificada.
- Formato concreto dos tokens de convite não está definido.
- Estratégia runtime de validação de contratos JSON não está definida.
- Estratégia final de versionamento de contratos públicos não está definida.
- Política de invalidação de cache não está detalhada.
- Processamento de imagens não está definido.
- RPO/RTO numéricos de backup e DR não estão definidos.
- Stack concreta de observabilidade não está escolhida.
- Critérios para migrar de PostgreSQL FTS para motor dedicado não estão definidos.
- Modelo comercial de billing futuro não está especificado.

## Decisões pendentes

- Definir convenção final das rotas REST públicas, administrativas, auth e internal.
- Formalizar ADRs conceituais pendentes.
- Definir matriz de permissões efetivas.
- Definir sincronização entre Keycloak e usuários locais.
- Definir token, TTL, invalidação e segurança de convites.
- Definir biblioteca e ponto de validação JSON Schema.
- Definir versionamento de contratos públicos: path, header ou outro mecanismo.
- Definir política concreta de cache e invalidação.
- Definir estratégia de processamento e variações de imagem.
- Definir observabilidade: logs, métricas, traces, alertas e retenção.
- Definir RPO/RTO e rotina de restore testado.
- Decidir oficialmente se comentários são MVP ou Fase 4.

## Resumo executivo

O Aegis é a fundação administrativa do BYOP: uma plataforma multi-tenant, multi-produto, modular e contract-first para gerenciar produtos digitais. Ele administra conteúdo e capacidades; não renderiza páginas públicas. Frontends externos consomem contratos JSON versionados.

O MVP deve entregar a fundação técnica, CMS Core, formulários e analytics inicial. Recursos como módulos reais, biblioteca, wiki, colaboração e SaaS ficam separados como Pós-MVP ou Futuro. A arquitetura oficial privilegia Product First, CMS First, Contract First, REST First, tenant isolation, auditoria, governança por ADR e evolução sem Frankenstein.

## Perguntas abertas

- Comentários entram no MVP ou permanecem na Fase 4?
- Qual será a convenção final das rotas REST?
- Quais roles e ações mínimas compõem a matriz oficial de permissões?
- Como será feita a sincronização Keycloak ↔ base local?
- Qual estratégia concreta será usada para versionamento de contratos públicos?
- Quais métricas definem sucesso do MVP?
- Qual stack de observabilidade será adotada?
- Quais RPO/RTO serão assumidos no Genesis Lab e em produção?
- Quais critérios disparam Redis, motor de busca dedicado, CDN ou GraphQL?

## Próximas decisões necessárias

1. Resolver a divergência de escopo sobre comentários.
2. Fechar a estrutura oficial de rotas REST.
3. Formalizar ADRs conceituais pendentes.
4. Especificar a matriz de permissões efetivas.
5. Especificar contratos JSON prioritários do MVP.
6. Definir sincronização Keycloak com usuários locais.
7. Definir segurança e ciclo de vida de convites.
8. Definir política mínima de backup, restore, RPO e RTO.
9. Definir stack mínima de observabilidade.
10. Definir critérios objetivos para encerramento do MVP.
