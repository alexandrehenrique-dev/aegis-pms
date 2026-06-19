# AEGIS PMS — SPRINT 04
## ASSETS MODULE

Continue a partir das Sprints 01, 02 e 03.

Preserve exatamente:

- App Shell
- Header Global
- Sidebar
- Tenant Switcher
- Product Switcher
- Breadcrumb
- Global Search
- Notification Center
- Identidade visual white-first
- Hierarquia Plataforma → Tenant → Produto → Módulo → Recurso
- Dashboard Global
- Dashboard de Produto
- Módulo de Conteúdo

Não recrie a fundação.
Não recrie dashboards.
Não recrie editor de conteúdo.
Não mude o estilo visual.
Não gere Forms, Analytics, Knowledge Graph, Users, Settings ou Audit nesta sprint.

---

# OBJETIVO DA SPRINT

Gerar o módulo de Assets do Aegis PMS.

Esta sprint deve criar:

1. Biblioteca de Assets
2. Upload de Asset
3. Preview / Detalhe do Asset
4. Edição de Metadados
5. Sistema de Tags
6. Uso do Asset no Produto
7. Asset Picker reutilizável
8. Estados de processamento, erro, vazio e permissão

O módulo deve provar que o Aegis não armazena arquivos soltos.

Ele administra ativos digitais vinculados a produtos, conteúdos, SEO, formulários e conhecimento.

---

# CONTEXTO BASE

Tenant ativo:

BYOP

Produto ativo:

Maestro Beton

Módulo ativo:

Assets

Breadcrumb base:

Plataforma → BYOP → Maestro Beton → Assets

---

# REGRA CENTRAL

Toda tela de assets deve responder:

- Onde estou?
- Qual tenant está ativo?
- Qual produto está ativo?
- Qual asset estou gerenciando?
- Onde este asset é usado?
- Qual é seu status?
- O que posso fazer agora?

---

# TELA 01 — BIBLIOTECA DE ASSETS

Contexto:

Plataforma → BYOP → Maestro Beton → Assets → Biblioteca

Criar biblioteca visual de assets.

Elementos obrigatórios:

- Page Header com título "Assets"
- descrição: "Gerencie imagens, vídeos, documentos e arquivos vinculados a este produto."
- badge do produto ativo
- CTA principal: "Upload de asset"
- CTA secundário: "Organizar tags"
- busca por nome, tag ou tipo
- filtros
- alternância Grid / Lista

Filtros obrigatórios:

- Tipo: imagem, vídeo, áudio, documento, PDF
- Status: ativo, em processamento, arquivado, com erro
- Uso: em uso, não utilizado, usado em SEO, usado em página
- Tag
- Data de upload

Assets mockados:

- hero-maestro-beton.jpg
- galeria-casamento-01.jpg
- galeria-corporativo-02.jpg
- depoimento-video.mp4
- release-institucional.pdf
- logo-maestro.svg
- og-home.jpg
- sax-performance-audio.mp3

Grid card deve mostrar:

- thumbnail
- tipo
- nome
- tamanho
- status
- tags
- onde é usado
- menu contextual

Lista deve mostrar:

- nome
- tipo
- tamanho
- status
- tags
- uso
- data
- ações

Estados obrigatórios:

- biblioteca preenchida
- biblioteca vazia
- busca sem resultado
- loading skeleton
- erro parcial
- sem permissão
- asset processando
- upload em andamento

Mobile:

Grid vira lista de cards compactos.
Filtros viram drawer.
Ações ficam em menu contextual.

---

# TELA 02 — UPLOAD DE ASSET

Contexto:

Plataforma → BYOP → Maestro Beton → Assets → Upload

Criar tela ou drawer grande de upload.

Elementos obrigatórios:

- zona drag and drop
- botão selecionar arquivos
- lista de arquivos selecionados
- progresso individual
- progresso geral
- validação de tipo
- validação de tamanho
- preview quando possível
- campos iniciais de metadados

Campos:

- Nome amigável
- Alt text
- Legenda
- Tags
- Crédito
- Pasta lógica / grupo
- Visibilidade
- Produto vinculado

Tipos permitidos:

- JPG
- PNG
- WEBP
- SVG
- MP4
- MP3
- PDF

Estados:

- vazio
- arrastando arquivo
- arquivos selecionados
- upload em progresso
- upload concluído
- upload parcial
- erro de tipo
- erro de tamanho
- sem permissão

Microcopy:

- "Arraste arquivos para cá ou selecione do seu dispositivo."
- "Use textos alternativos para melhorar acessibilidade e SEO."
- "Arquivos enviados ficam vinculados ao produto ativo."

---

# TELA 03 — DETALHE / PREVIEW DO ASSET

Contexto:

Plataforma → BYOP → Maestro Beton → Assets → hero-maestro-beton.jpg

Criar página ou drawer de detalhe do asset.

Layout desktop:

Esquerda:

- preview grande do arquivo
- controles de zoom
- informações técnicas

Direita:

- metadados
- tags
- uso no sistema
- ações

Mostrar:

- nome do arquivo
- tipo
- tamanho
- dimensões / duração
- status
- data de upload
- enviado por
- checksum/id técnico
- URL pública futura
- texto alternativo
- legenda
- crédito
- tags

Ações:

- editar metadados
- substituir arquivo
- copiar referência
- baixar
- arquivar

Estados:

- imagem
- vídeo
- áudio
- PDF
- arquivo sem preview
- asset processando
- asset com erro
- sem permissão

---

# TELA 04 — EDIÇÃO DE METADADOS

Criar formulário de edição de metadados do asset.

Campos:

- Nome amigável
- Alt text
- Legenda
- Crédito
- Tags
- Pasta lógica
- Visibilidade
- SEO usage
- Observações internas

UX obrigatória:

- contador no alt text
- alerta se imagem usada em SEO não possuir alt text
- indicação de campos recomendados
- salvar
- cancelar
- alterações não salvas

Estados:

- válido
- inválido
- salvando
- salvo
- erro
- sem permissão

---

# TELA 05 — TAGS DE ASSETS

Criar área de organização de tags.

Elementos:

- lista de tags
- quantidade de assets por tag
- criar tag
- editar tag
- remover tag
- mesclar tags

Tags mockadas:

- casamento
- corporativo
- saxofone
- hero
- galeria
- depoimento
- seo
- institucional

Estados:

- sem tags
- tag em uso
- tag duplicada
- removendo tag
- sem permissão

---

# TELA 06 — USO DO ASSET NO SISTEMA

Criar tela ou aba "Uso".

Mostrar onde o asset está sendo usado.

Exemplos:

hero-maestro-beton.jpg usado em:

- Página Home → Bloco Hero
- Página Sobre → Galeria
- SEO → OG Image
- Preview público → Hero principal

Cada uso deve mostrar:

- recurso
- módulo
- localização
- status
- link para abrir recurso

Estados:

- asset usado
- asset não utilizado
- uso quebrado
- loading

UX importante:

Aegis deve deixar claro o impacto antes de arquivar ou substituir um asset usado.

---

# TELA 07 — ASSET PICKER

Criar componente reutilizável para seleção de assets.

Contexto:

Usado no Editor de Conteúdo, SEO, Forms e configurações futuras.

Estados:

- aberto
- busca
- filtros
- seleção única
- seleção múltipla
- preview lateral
- sem resultados
- upload rápido
- asset selecionado

Deve permitir:

- buscar asset existente
- filtrar por tipo
- selecionar
- fazer upload rápido
- confirmar seleção

---

# COMPONENTES NOVOS DA SPRINT

Criar ou refinar:

- AssetLibrary
- AssetGrid
- AssetList
- AssetCard
- AssetTypeIcon
- AssetStatusBadge
- AssetUploadZone
- UploadProgressItem
- AssetPreviewPanel
- AssetMetadataForm
- AssetTagManager
- AssetUsagePanel
- AssetPicker
- AssetPickerModal
- AssetProcessingState
- AssetBrokenUsageAlert
- FileValidationAlert

---

# REGRAS DE UX

Aplicar:

- contexto sempre visível
- breadcrumbs obrigatórios
- feedback em upload
- skeleton para biblioteca
- confirmação antes de arquivar asset usado
- erro recuperável
- ações ocultas sem permissão
- tags não podem parecer decoração; devem organizar operação

---

# RESPONSIVIDADE

Gerar:

Mobile 375px
Tablet 768px
Desktop 1440px

Desktop:

- biblioteca em grid/lista
- detalhe com preview à esquerda e metadados à direita
- upload em drawer/tela ampla
- asset picker com preview lateral

Tablet:

- grid em 2 colunas
- detalhe empilhado
- filtros em drawer

Mobile:

- cards compactos
- filtros em bottom sheet
- upload com lista vertical
- detalhe em seções
- asset picker full-screen

Nunca permitir scroll horizontal na página.

---

# QUALIDADE ESPERADA

O módulo deve parecer uma biblioteca profissional de ativos digitais.

Não deve parecer:

- pasta de arquivos
- galeria simples
- drive genérico
- input de upload básico

Deve transmitir:

"Estou administrando ativos digitais importantes de um produto."

---

# IMPORTANTE

Não gerar ainda:

- Forms Builder
- Submissions
- Analytics avançado
- Knowledge Graph Canvas
- Users
- Settings
- Audit completa

Esses módulos virão nas próximas sprints.

Nesta sprint, entregue apenas o módulo de Assets.