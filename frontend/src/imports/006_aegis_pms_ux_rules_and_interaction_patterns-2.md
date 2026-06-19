# 006 — AEGIS PMS
# UX RULES & INTERACTION PATTERNS
## Versão 1.0

---

# PROPÓSITO

Este documento define as regras universais de experiência do usuário do Aegis PMS.

Seu objetivo é garantir consistência entre:

- UX
- UI
- Frontend
- Backend
- Produto
- Documentação

Toda tela futura do Aegis deve obedecer estas regras.

---

# PRINCÍPIOS FUNDAMENTAIS

## Regra 01 — Contexto Nunca Pode Ser Perdido

O usuário deve sempre saber:

Tenant Atual
→ Produto Atual
→ Módulo Atual
→ Recurso Atual

Obrigatório:

- Header contextual
- Breadcrumbs
- Título da página
- Identificação do produto

Nunca utilizar telas órfãs.

---

## Regra 02 — Navegação Deve Ser Previsível

O mesmo comportamento deve produzir o mesmo resultado.

Exemplos:

- Botão "Criar" sempre cria.
- Botão "Salvar" sempre salva.
- Botão "Cancelar" sempre retorna.

Não reinventar padrões.

---

## Regra 03 — O Sistema Deve Explicar o Que Está Acontecendo

Nenhuma ação pode parecer mágica.

Sempre informar:

- carregando
- salvando
- processando
- sincronizando
- publicando
- concluído
- erro

---

# NAVEGAÇÃO

## Sidebar

Estrutura principal:

Dashboard
Conteúdo
Assets
Forms
Analytics
Knowledge Graph
Configurações

Regras:

- item ativo destacado
- item pai expandido
- suporte a colapso
- persistência da preferência

---

## Breadcrumbs

Obrigatórios em páginas internas.

Exemplo:

BYOP
→ Maestro Beton
→ Conteúdo
→ Artigos
→ Editar

Nunca ocultar o caminho.

---

## Busca Global

Disponível em qualquer tela.

Atalho:

CTRL + K
CMD + K

Busca:

- produtos
- conteúdos
- assets
- formulários
- usuários

---

# FEEDBACK VISUAL

## Loading

Todo carregamento superior a 300ms deve exibir feedback.

Utilizar:

- Skeleton
- Spinner
- Progress

Nunca exibir tela vazia.

---

## Success

Toda operação concluída deve informar:

- o que aconteceu
- quando aconteceu
- qual entidade foi afetada

Exemplo:

"Artigo publicado com sucesso."

---

## Error

Toda falha deve informar:

- problema
- consequência
- possível solução

Ruim:

"Erro desconhecido"

Bom:

"Não foi possível publicar o conteúdo. Verifique os campos obrigatórios."

---

## Warning

Utilizar para:

- operações irreversíveis
- riscos
- inconsistências

Nunca utilizar warning para sucesso.

---

# TOASTS

## Posições

- top-right
- top-left
- bottom-right
- bottom-left

Padrão:

top-right

---

## Comportamento

Sucesso:
- desaparece automaticamente

Informação:
- desaparece automaticamente

Warning:
- permanece mais tempo

Erro:
- permanece até interação

---

## Conteúdo

Ícone
Título
Mensagem
Ação opcional
Fechar

---

# MODAIS

## Quando Utilizar

Somente quando:

- interromper fluxo
- exigir confirmação
- exigir foco

---

## Quando NÃO Utilizar

Não utilizar para:

- visualização simples
- navegação
- mensagens triviais

---

## Estrutura

Header

Body

Footer

---

## Ações

Primária

Secundária

Nunca mais de duas ações principais.

---

# FORMULÁRIOS

## Estrutura

Label

Campo

Ajuda

Validação

---

## Campos Obrigatórios

Identificação visual obrigatória.

Exemplo:

Nome *

---

## Validação

Preferencialmente inline.

Mostrar erro próximo ao campo.

Nunca apenas no topo.

---

## Salvamento

Draft automático sempre que possível.

---

# TABELAS

## Recursos Obrigatórios

Busca

Filtro

Ordenação

Paginação

Exportação

---

## Linhas

Hover

Seleção

Ações contextuais

---

## Estados

Loading

Empty

Error

Success

---

# EMPTY STATES

Toda listagem deve possuir estado vazio.

Estrutura:

Ícone

Título

Descrição

CTA

Exemplo:

"Nenhum artigo encontrado"

"Criar primeiro artigo"

---

# CONFIRMAÇÕES

## Obrigatórias

Excluir

Publicar

Arquivar

Alterar permissões

Remover usuários

---

## Estrutura

O que acontecerá

Impacto

Botão cancelar

Botão confirmar

---

# ACESSIBILIDADE

## Teclado

100% navegável por teclado.

---

## Foco

Todo elemento interativo deve possuir:

- foco visível
- contraste adequado

---

## Contraste

Seguir WCAG AA como mínimo.

---

## Ícones

Ícones nunca podem carregar significado sozinhos.

Sempre possuir:

- label
- tooltip
- texto contextual

---

# RESPONSIVIDADE

## Mobile First

Toda interface nasce para mobile.

Depois:

Tablet

Desktop

---

## Breakpoints

Mobile:
0–767

Tablet:
768–1279

Desktop:
1280+

---

## Scroll

Nunca permitir scroll horizontal.

---

## Tabelas

Transformar para:

- cards
ou
- scroll controlado

---

# PERMISSÕES

## Regra Universal

O usuário nunca deve ver ações que não pode executar.

Não apenas bloquear.

Ocultar.

---

## Auditoria

Toda ação crítica:

quem

quando

onde

o que mudou

---

# PADRÕES DE PERFORMANCE

## Feedback

Até 100ms:
sem feedback

100–300ms:
feedback opcional

300ms+:
feedback obrigatório

1000ms+:
skeleton recomendado

3000ms+:
mensagem contextual

---

# PADRÕES DE CONFIANÇA

O usuário deve sentir:

- controle
- previsibilidade
- segurança
- clareza

Nunca:

- surpresa
- medo
- dúvida operacional

---

# REGRA DE OURO

O Aegis PMS administra produtos.

Portanto toda interação deve responder rapidamente:

Onde estou?
O que estou administrando?
O que acabou de acontecer?
Qual é o próximo passo?

Se uma tela não responder essas quatro perguntas, ela deve ser redesenhada.
