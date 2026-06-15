# docs/modules — Documentação por Módulo

## Propósito

Documentação detalhada de cada **módulo** do Aegis. O Aegis é composto por **núcleo + módulos**: o núcleo existe para todos os produtos; os módulos são habilitados conforme a categoria e as features de cada produto.

## Conteúdo esperado

Um arquivo por módulo, descrevendo domínio, entidades, features expostas, contratos, dependências e owner.

Módulos previstos:

- `core` — produtos, tenants, memberships, features, auditoria.
- `identity` — integração Keycloak, roles, permissões contextuais.
- `content` — pages, sections, blocks, content types, traduções, revisões.
- `assets` — asset manager.
- `forms` — form builder, submissions.
- `seo` — SEO engine.
- `analytics` — eventos e dashboards.
- `modules/rh` — JobPosting, CandidateSubmission, Lead, CompanyProfile.
- `modules/music` — serviços, agenda, repertório, eventos.
- `modules/portfolio` — projetos, experiências, skills, downloads.
- `modules/library` — manifestos, poemas, reflexões, livros, playlists.
- `modules/wiki` — categorias, tópicos, artigos, relações, busca.

## Regras de governança de módulos

- Criar módulo novo quando há domínio claro, owner, contratos, dados próprios e responsabilidades distintas.
- Reutilizar módulo existente quando o comportamento é variação do domínio atual.
- Promover para core quando vários produtos dependem e a regra é transversal.
- Core não cresce por conveniência.
