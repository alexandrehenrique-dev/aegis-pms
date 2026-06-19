# 🛡️ AEGIS PMS — SCREEN INVENTORY
## Artefato 003 — Catálogo Completo de Telas, Estados, Permissões e Responsividade

> Projeto: **Aegis PMS — Product Management System**  
> Ecossistema: **BYOP — Build Your Own Path**  
> Responsável visual/conceitual: **Eirene**  
> Base estratégica: **Aegis PMS — UX Roadmap Master**  
> Natureza deste artefato: **inventário de telas para UX, Figma, prototipação e implementação futura**  
> Status: **Versão 1.0 — Inventário expandido antes do Figma**

---

# 1. PROPÓSITO DO DOCUMENTO

Este documento existe para transformar o mapa de experiência do Aegis PMS em um inventário operacional de telas.

O objetivo não é desenhar a interface final ainda.

O objetivo é responder, com precisão:

- quais telas existem;
- quais telas pertencem ao MVP;
- quais telas são futuras;
- quais estados cada tela precisa possuir;
- quais permissões impactam cada tela;
- quais componentes principais cada tela exige;
- como cada tela deve se comportar em desktop, tablet e mobile;
- quais telas precisam de variações por tenant, produto, módulo, papel ou status;
- quais telas são críticas para o Figma Make;
- quais telas devem ser prototipadas primeiro.

Aegis PMS não deve ser tratado como um CMS tradicional.

Aegis PMS é uma plataforma para administrar produtos digitais.

A hierarquia mental central é:

```txt
Login
→ Tenant
→ Produto
→ Módulos
→ Conteúdo
→ Operação
```

Portanto, este Screen Inventory não organiza apenas páginas.

Ele organiza o cockpit operacional de uma plataforma multi-tenant, modular, product-first, contract-first e preparada para crescer.

---

# 2. PRINCÍPIO CENTRAL DO INVENTÁRIO

Toda tela do Aegis PMS deve responder claramente:

1. **Onde estou?**
2. **Qual tenant está ativo?**
3. **Qual produto está ativo?**
4. **Qual módulo está ativo?**
5. **O que posso fazer aqui?**
6. **O que aconteceu recentemente?**
7. **O que exige minha atenção agora?**
8. **Tenho permissão para executar esta ação?**

Se uma tela não responde isso, ela está incompleta.

---

# 3. CLASSIFICAÇÃO DAS TELAS

As telas serão classificadas em cinco níveis:

## 3.1 Core MVP

Telas obrigatórias para o Aegis existir como plataforma mínima.

Sem elas, o produto não possui fluxo administrativo suficiente.

## 3.2 MVP Expandido

Telas desejáveis ainda na primeira versão funcional, mas que podem entrar após o core.

## 3.3 Pós-MVP

Telas importantes para maturidade da plataforma, mas não bloqueiam a primeira validação.

## 3.4 Enterprise / Futuro

Telas ligadas a escalabilidade, automação, governança avançada, integrações complexas e multi-produto maduro.

## 3.5 Experimental / BYOP

Telas que diferenciam o Aegis como ecossistema próprio: graph, IA futura, memória operacional, relações entre produtos, conhecimento e jornadas.

---

# 4. PERSONAS E IMPACTO NAS TELAS

## 4.1 Aegis Super Admin

Controla toda a plataforma.

Pode:

- criar tenants;
- criar produtos;
- gerenciar usuários;
- habilitar módulos;
- visualizar auditoria;
- administrar integrações;
- acessar configurações globais;
- operar qualquer tenant/produto.

Impacto de UX:

- precisa de visão global;
- precisa alternar tenant rapidamente;
- precisa acessar auditoria e sistema;
- precisa enxergar saúde operacional.

---

## 4.2 Tenant Admin

Controla uma organização.

Pode:

- convidar usuários;
- criar produtos;
- habilitar módulos;
- aprovar conteúdo;
- administrar permissões dentro do tenant;
- visualizar analytics dos produtos do tenant.

Impacto de UX:

- precisa de clareza sobre o tenant ativo;
- precisa gerenciar equipe e produtos;
- não deve ver controles globais de sistema.

---

## 4.3 Product Manager

Responsável por um produto digital.

Pode:

- administrar conteúdo;
- configurar módulos;
- visualizar analytics;
- acompanhar formulários;
- organizar assets;
- acompanhar roadmap futuro;
- aprovar ou solicitar revisão conforme permissões.

Impacto de UX:

- precisa de dashboard por produto;
- precisa de navegação modular;
- precisa enxergar pendências e métricas.

---

## 4.4 Editor

Produz conteúdo.

Pode:

- criar conteúdo;
- editar conteúdo;
- enviar para aprovação;
- usar assets;
- visualizar histórico do próprio conteúdo.

Impacto de UX:

- precisa de editor claro;
- precisa de estados editoriais simples;
- não deve ver configurações sensíveis;
- precisa de feedback de permissões.

---

## 4.5 Viewer

Somente leitura.

Pode:

- visualizar dashboards permitidos;
- consultar conteúdo;
- consultar assets;
- consultar analytics se permitido.

Impacto de UX:

- botões de ação devem sumir ou aparecer desabilitados com explicação;
- telas devem priorizar leitura;
- não deve haver ações destrutivas.

---

# 5. VISÃO GERAL DO INVENTÁRIO

Este inventário propõe aproximadamente:

```txt
Core MVP:                 34 telas
MVP Expandido:            24 telas
Pós-MVP:                  22 telas
Enterprise/Futuro:        18 telas
Experimental/BYOP:        12 telas
Total estimado:          110 telas/variações
```

Nem todas precisam ser desenhadas imediatamente.

Mas todas devem estar previstas para evitar que a arquitetura visual nasça pequena demais.

---

# 6. MAPA DE MÓDULOS DO INVENTÁRIO

```txt
00. Área Pública / Autenticação
01. Seleção de Contexto
02. Dashboard Global
03. Tenants
04. Usuários e Permissões
05. Produtos
06. Catálogo de Módulos
07. CMS / Conteúdo
08. Páginas e Blocos
09. Workflow Editorial
10. Versionamento
11. Assets
12. Formulários
13. Submissions
14. Analytics
15. SEO
16. Knowledge Graph
17. Auditoria
18. Configurações
19. Notificações
20. Operação / Sistema
21. Estados Globais
22. Responsividade
```

---

# 7. PADRÃO DE DESCRIÇÃO DE TELA

Cada tela será descrita com:

- **ID**
- **Nome**
- **Status**
- **Prioridade**
- **Persona principal**
- **Objetivo**
- **Quando aparece**
- **Componentes principais**
- **Ações primárias**
- **Ações secundárias**
- **Estados obrigatórios**
- **Permissões**
- **Desktop**
- **Tablet**
- **Mobile**
- **Observações UX**

---

# 8. 00 — ÁREA PÚBLICA / AUTENTICAÇÃO

---

## 00.01 — Login

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Todas  
**Objetivo:** permitir acesso seguro à plataforma.

### Quando aparece

Primeira tela para usuários não autenticados.

### Componentes principais

- logo Aegis;
- campo e-mail/usuário;
- campo senha;
- botão entrar;
- link "Esqueci minha senha";
- link para primeiro acesso, se aplicável;
- mensagem de erro;
- loading state;
- aviso de ambiente, se local/staging;
- possível identidade visual BYOP.

### Ações primárias

- entrar.

### Ações secundárias

- recuperar senha;
- acessar convite;
- trocar idioma, futuramente.

### Estados obrigatórios

- vazio;
- preenchido;
- carregando;
- credenciais inválidas;
- conta bloqueada;
- sessão expirada;
- erro de servidor;
- sucesso/redirecionamento.

### Permissões

Pública.

### Desktop

Layout centralizado, com painel visual lateral opcional.

### Tablet

Layout centralizado, reduzindo ilustração ou painel lateral.

### Mobile

Formulário full-width, sem excesso visual.

### Observações UX

Login deve transmitir segurança e autoridade. Não deve parecer tela genérica de admin template.

---

## 00.02 — Recuperar Senha — Solicitação

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Todas  
**Objetivo:** solicitar recuperação de acesso.

### Componentes principais

- campo e-mail;
- botão enviar;
- link voltar ao login;
- mensagem explicativa.

### Estados obrigatórios

- e-mail inválido;
- envio em andamento;
- e-mail enviado;
- erro;
- rate limit, futuro.

### Permissões

Pública.

### Mobile

Fluxo simples, sem fricção.

---

## 00.03 — Recuperar Senha — Token / Código

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Todas  
**Objetivo:** validar token ou link de recuperação.

### Componentes principais

- campo token/código;
- nova senha;
- confirmar senha;
- botão redefinir.

### Estados obrigatórios

- token inválido;
- token expirado;
- senhas divergentes;
- senha fraca;
- sucesso.

---

## 00.04 — Primeiro Acesso

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Usuário convidado  
**Objetivo:** finalizar ativação da conta.

### Componentes principais

- boas-vindas;
- dados do convite;
- definição de senha;
- aceite de termos;
- seleção de tenant padrão, se houver mais de um.

### Estados obrigatórios

- convite válido;
- convite expirado;
- convite já usado;
- senha inválida;
- sucesso.

### Observações UX

Deve parecer entrada em uma plataforma séria, não cadastro comum.

---

## 00.05 — Convite Inválido

**Status:** Core MVP  
**Prioridade:** Média  
**Persona principal:** Usuário convidado  
**Objetivo:** explicar problema com convite.

### Estados

- expirado;
- revogado;
- já aceito;
- usuário sem permissão.

### Ações

- solicitar novo convite;
- voltar ao login.

---

# 9. 01 — SELEÇÃO DE CONTEXTO

---

## 01.01 — Seleção de Tenant

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Usuários com múltiplos tenants  
**Objetivo:** escolher organização ativa.

### Componentes principais

- lista de tenants;
- busca;
- cards com nome, slug, status;
- indicador de último acesso;
- botão entrar.

### Estados obrigatórios

- um tenant;
- múltiplos tenants;
- nenhum tenant;
- tenant suspenso;
- loading;
- erro.

### Permissões

Usuário autenticado.

### Observações UX

Se houver apenas um tenant, pode redirecionar automaticamente para o dashboard.

---

## 01.02 — Alternador de Tenant

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Super Admin, Tenant Admin  
**Objetivo:** trocar contexto sem sair da área autenticada.

### Componentes principais

- dropdown de tenants;
- busca rápida;
- status do tenant;
- tenant atual destacado.

### Estados

- troca em andamento;
- tenant indisponível;
- sem permissão.

### Observações UX

Este componente deve aparecer globalmente no header ou sidebar.

---

## 01.03 — Alternador de Produto

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Product Manager, Tenant Admin  
**Objetivo:** trocar produto ativo.

### Componentes principais

- lista de produtos do tenant;
- tipo do produto;
- status;
- favorito/recente;
- busca.

### Estados

- nenhum produto;
- múltiplos produtos;
- produto arquivado;
- produto sem módulos habilitados.

---

# 10. 02 — DASHBOARD GLOBAL

---

## 02.01 — Dashboard Global

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Super Admin, Tenant Admin  
**Objetivo:** apresentar visão operacional do tenant/plataforma.

### Componentes principais

- cards de produtos ativos;
- conteúdos pendentes;
- aprovações;
- formulários recebidos;
- assets recentes;
- atividades recentes;
- usuários ativos;
- atalhos rápidos;
- seletor de período;
- alertas operacionais.

### Ações primárias

- criar produto;
- revisar aprovações;
- ver submissions;
- acessar auditoria.

### Estados obrigatórios

- sem produtos;
- com produtos;
- sem atividade recente;
- loading skeleton;
- erro parcial em widgets;
- sem permissão para alguns widgets.

### Permissões

- Super Admin: visão plataforma.
- Tenant Admin: visão tenant.
- Product Manager: visão produtos autorizados.
- Editor: visão editorial limitada.
- Viewer: visão leitura.

### Desktop

Grid modular 12 colunas.

### Tablet

Cards em duas colunas.

### Mobile

Cards empilhados, priorizando alertas e ações pendentes.

### Observações UX

Esta tela deve parecer um centro de comando, não uma home burocrática.

---

## 02.02 — Dashboard Global — Estado Vazio

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Tenant Admin  
**Objetivo:** orientar primeiro uso.

### Cenários

- sem tenant configurado;
- sem produto criado;
- sem módulo habilitado.

### Componentes

- mensagem clara;
- CTA criar produto;
- checklist inicial;
- link para documentação.

---

## 02.03 — Dashboard Global — Sem Permissões

**Status:** Core MVP  
**Prioridade:** Média  
**Persona principal:** Viewer/Editor  
**Objetivo:** explicar acesso limitado.

### Componentes

- mensagem de leitura;
- módulos disponíveis;
- contato com administrador.

---

# 11. 03 — TENANTS

---

## 03.01 — Lista de Tenants

**Status:** Core MVP  
**Prioridade:** Crítica para Super Admin  
**Persona principal:** Super Admin  
**Objetivo:** gerenciar organizações.

### Componentes principais

- tabela de tenants;
- busca;
- filtros por status;
- colunas: nome, slug, usuários, produtos, status, última atividade;
- ações de linha;
- botão criar tenant.

### Estados obrigatórios

- lista vazia;
- loading;
- erro;
- sem permissão;
- tenant arquivado;
- busca sem resultado.

### Desktop

Tabela densa com ações.

### Mobile

Cards por tenant.

---

## 03.02 — Criar Tenant

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Super Admin  
**Objetivo:** criar organização.

### Campos

- nome;
- slug;
- descrição;
- status inicial;
- idioma padrão;
- plano/tipo, futuro.

### Estados

- slug disponível;
- slug indisponível;
- validação;
- salvando;
- sucesso.

---

## 03.03 — Detalhe do Tenant

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Super Admin, Tenant Admin  
**Objetivo:** visão central da organização.

### Abas

- Geral;
- Usuários;
- Produtos;
- Módulos;
- Auditoria;
- Configurações.

### Componentes

- resumo do tenant;
- status;
- contadores;
- atividade recente;
- ações rápidas.

---

## 03.04 — Editar Tenant

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Super Admin, Tenant Admin  
**Objetivo:** alterar dados gerais do tenant.

### Campos

- nome;
- slug;
- descrição;
- status;
- branding básico;
- idioma padrão.

### Estados

- alterações não salvas;
- validação;
- salvando;
- erro;
- sucesso.

---

## 03.05 — Arquivar Tenant

**Status:** MVP Expandido  
**Prioridade:** Média  
**Persona principal:** Super Admin  
**Objetivo:** desativar tenant sem excluir dados.

### Componentes

- modal de confirmação;
- impacto da ação;
- campo de confirmação textual;
- motivo.

### Observações

Ação sensível. Deve gerar auditoria.

---

# 12. 04 — USUÁRIOS E PERMISSÕES

---

## 04.01 — Lista de Usuários

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Tenant Admin, Super Admin  
**Objetivo:** gerenciar usuários.

### Componentes

- tabela;
- filtros por tenant, papel, status;
- busca;
- botão convidar;
- ações de linha.

### Colunas

- nome;
- e-mail;
- papel;
- tenant;
- produtos associados;
- status;
- último acesso.

### Estados

- sem usuários;
- busca sem resultado;
- usuário pendente;
- usuário bloqueado;
- loading;
- erro.

---

## 04.02 — Convidar Usuário

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Tenant Admin  
**Objetivo:** enviar convite.

### Campos

- nome;
- e-mail;
- papel;
- tenant;
- produtos permitidos;
- mensagem opcional.

### Estados

- e-mail inválido;
- usuário já existe;
- convite enviado;
- convite pendente;
- erro.

---

## 04.03 — Detalhe do Usuário

**Status:** MVP Expandido  
**Prioridade:** Média  
**Persona principal:** Tenant Admin  
**Objetivo:** visualizar perfil e permissões.

### Abas

- Perfil;
- Permissões;
- Produtos;
- Atividade;
- Segurança.

---

## 04.04 — Editar Permissões do Usuário

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Tenant Admin, Super Admin  
**Objetivo:** alterar papéis e acesso.

### Componentes

- matriz de permissões;
- seleção de produtos;
- seleção de módulos;
- resumo de impacto.

### Estados

- sem permissão para editar;
- alterações não salvas;
- conflito de papel;
- sucesso.

---

## 04.05 — Revogar Acesso

**Status:** MVP Expandido  
**Prioridade:** Média  
**Persona principal:** Tenant Admin  
**Objetivo:** remover acesso do usuário.

### Componentes

- modal;
- impacto;
- motivo;
- confirmação.

---

# 13. 05 — PRODUTOS

---

## 05.01 — Lista de Produtos

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Tenant Admin, Product Manager  
**Objetivo:** listar produtos digitais do tenant.

### Componentes

- cards ou tabela;
- busca;
- filtro por tipo;
- filtro por status;
- botão criar produto;
- visual de módulos ativos.

### Exemplos de produtos

- Maestro Beton;
- Conecta Talentos;
- Alexandre Dev;
- Loki;
- WikiDev;
- CMSS;
- Eirene UI;
- Genesis;
- Aion Logbook.

### Estados

- nenhum produto;
- produto ativo;
- produto arquivado;
- sem permissão;
- loading.

### Desktop

Cards grandes com status e módulos.

### Mobile

Cards empilhados com CTA claro.

---

## 05.02 — Criar Produto

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Tenant Admin  
**Objetivo:** criar produto digital.

### Campos

- nome;
- slug;
- tipo;
- idioma padrão;
- descrição;
- tenant;
- template inicial;
- módulos iniciais.

### Tipos sugeridos

- site institucional;
- site pessoal/profissional;
- CMS;
- landing page;
- documentação;
- produto SaaS;
- knowledge base;
- app interno;
- design system;
- outro.

### Estados

- slug disponível;
- slug indisponível;
- validação;
- salvando;
- sucesso;
- erro.

---

## 05.03 — Detalhe do Produto

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Product Manager  
**Objetivo:** centro operacional do produto.

### Abas principais

- Visão Geral;
- Módulos;
- Conteúdo;
- Assets;
- Forms;
- Analytics;
- Graph;
- Configurações.

### Componentes

- status do produto;
- módulos habilitados;
- últimos eventos;
- pendências;
- links rápidos;
- preview, quando aplicável.

### Observações UX

Esta é uma das telas mais importantes do Aegis.

Se esta tela for acertada, o restante da plataforma se encaixa.

---

## 05.04 — Editar Produto

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager, Tenant Admin  
**Objetivo:** alterar dados do produto.

### Campos

- nome;
- slug;
- tipo;
- idioma padrão;
- descrição;
- status;
- branding;
- domínio, futuro.

---

## 05.05 — Produto — Estado Sem Módulos

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager  
**Objetivo:** orientar habilitação inicial.

### Componentes

- mensagem;
- CTA catálogo de módulos;
- sugestão de módulos por tipo de produto.

---

## 05.06 — Produto — Preview Público

**Status:** MVP Expandido  
**Prioridade:** Média  
**Persona principal:** Product Manager, Editor  
**Objetivo:** visualizar produto gerado ou site publicado.

### Componentes

- iframe/preview;
- links;
- ambiente;
- status de publicação.

---

# 14. 06 — CATÁLOGO DE MÓDULOS

---

## 06.01 — Catálogo de Módulos

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Tenant Admin, Product Manager  
**Objetivo:** habilitar/desabilitar capacidades do produto.

### Componentes

- grid estilo marketplace;
- busca;
- filtros por categoria;
- cards de módulo;
- status instalado/habilitado;
- dependências;
- CTA habilitar.

### Módulos sugeridos

- CMS;
- Assets;
- Forms;
- Analytics;
- SEO;
- Knowledge Graph;
- Workflow;
- Versionamento;
- Deploy;
- Commerce futuro;
- Members futuro;
- Courses futuro.

### Estados

- nenhum módulo;
- módulo habilitado;
- módulo desabilitado;
- módulo com dependência faltando;
- sem permissão;
- loading.

---

## 06.02 — Detalhe do Módulo

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** explicar módulo antes de habilitar.

### Conteúdo

- descrição;
- funcionalidades;
- dependências;
- permissões;
- impacto;
- status.

---

## 06.03 — Habilitar Módulo

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** confirmar ativação.

### Componentes

- modal de confirmação;
- dependências;
- impacto;
- permissões necessárias.

---

## 06.04 — Desabilitar Módulo

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** desativar módulo sem perda indevida de dados.

### Componentes

- alerta de impacto;
- opção manter dados;
- motivo;
- confirmação.

---

# 15. 07 — CMS / CONTENT

---

## 07.01 — Dashboard Editorial

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Product Manager, Editor  
**Objetivo:** resumir saúde editorial do produto.

### Widgets

- rascunhos;
- em revisão;
- publicados;
- arquivados;
- conteúdos recentes;
- aprovações pendentes;
- autores ativos;
- idiomas.

### Estados

- sem conteúdo;
- com pendências;
- sem permissão;
- loading;
- erro parcial.

---

## 07.02 — Lista de Conteúdos

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Editor  
**Objetivo:** gerenciar conteúdos.

### Colunas

- título;
- tipo;
- autor;
- idioma;
- status;
- atualização;
- publicação;
- produto.

### Filtros

- status;
- tipo;
- idioma;
- autor;
- data;
- busca.

### Ações

- criar;
- editar;
- enviar para revisão;
- arquivar;
- duplicar.

---

## 07.03 — Criar Conteúdo

**Status:** Core MVP  
**Prioridade:** Crítica  
**Objetivo:** iniciar novo conteúdo.

### Campos

- título;
- tipo;
- idioma;
- produto;
- template;
- slug;
- descrição.

---

## 07.04 — Editor de Conteúdo

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Editor  
**Objetivo:** editar conteúdo estruturado.

### Layout

Sidebar esquerda:

- estrutura;
- blocos;
- árvore de seções.

Centro:

- editor;
- canvas;
- campos principais.

Direita:

- propriedades;
- status;
- SEO;
- publicação;
- auditoria rápida.

### Estados

- rascunho;
- editando;
- salvando;
- salvo;
- erro;
- conflito de edição;
- sem permissão;
- conteúdo bloqueado.

### Observações UX

Editor deve parecer cockpit de conteúdo, não textarea gigante.

---

## 07.05 — Editor JSON / Contrato

**Status:** MVP Expandido  
**Prioridade:** Alta  
**Persona principal:** Product Manager, dev/admin  
**Objetivo:** visualizar e validar contrato JSON.

### Componentes

- editor JSON;
- validação;
- schema;
- preview;
- diff;
- copiar/exportar.

### Estados

- JSON válido;
- JSON inválido;
- schema incompatível;
- alterações não salvas.

---

## 07.06 — Preview de Conteúdo

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** visualizar conteúdo antes de publicar.

### Componentes

- preview desktop;
- preview tablet;
- preview mobile;
- status;
- data de publicação;
- CTA enviar revisão/publicar.

---

## 07.07 — Conteúdo Sem Permissão

**Status:** Core MVP  
**Prioridade:** Média  
**Objetivo:** informar restrição sem quebrar fluxo.

---

# 16. 08 — PÁGINAS E BLOCOS

---

## 08.01 — Lista de Páginas

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Editor, Product Manager  
**Objetivo:** gerenciar páginas do produto.

### Exemplos

- Home;
- Sobre;
- Serviços;
- Agenda;
- Contato.

### Colunas

- página;
- slug;
- idioma;
- status;
- última atualização;
- publicação.

---

## 08.02 — Editor de Página

**Status:** Core MVP  
**Prioridade:** Crítica  
**Objetivo:** editar página por blocos.

### Tabs

- PT;
- EN;
- ES.

### Blocos

- hero;
- sections;
- SEO;
- CTA;
- galeria;
- vídeos;
- depoimentos.

### Estados

- bloco publicado;
- bloco em rascunho;
- bloco em revisão;
- bloco com erro;
- bloco sem tradução.

---

## 08.03 — Biblioteca de Blocos

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** selecionar bloco reutilizável.

### Componentes

- lista de templates;
- categorias;
- preview;
- inserir bloco.

---

## 08.04 — Configuração de SEO por Página

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** configurar metadados da página.

### Campos

- title;
- description;
- keywords;
- og image;
- canonical;
- robots, futuro.

---

## 08.05 — Preview Responsivo da Página

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** conferir resultado em múltiplos formatos.

### Modos

- mobile;
- tablet;
- desktop.

---

# 17. 09 — WORKFLOW EDITORIAL

---

## 09.01 — Workflow de Aprovação

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Editor, Product Manager  
**Objetivo:** mover conteúdo entre estados.

### Estados

```txt
Draft
→ In Review
→ Published
→ Archived
```

### Componentes

- status atual;
- próximas ações;
- comentários;
- histórico;
- responsáveis.

---

## 09.02 — Enviar para Revisão

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Editor  
**Objetivo:** solicitar aprovação.

### Campos

- mensagem;
- revisores;
- checklist;
- mudanças recentes.

---

## 09.03 — Revisar Conteúdo

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager, Tenant Admin  
**Objetivo:** aprovar ou solicitar ajuste.

### Ações

- aprovar;
- solicitar alterações;
- comentar;
- rejeitar;
- publicar.

---

## 09.04 — Histórico do Workflow

**Status:** Core MVP  
**Prioridade:** Média  
**Objetivo:** timeline completa.

### Eventos

- criado;
- editado;
- enviado;
- aprovado;
- publicado;
- arquivado;
- restaurado.

---

# 18. 10 — VERSIONAMENTO

---

## 10.01 — Histórico de Versões

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager, Editor  
**Objetivo:** listar versões.

### Colunas

- versão;
- autor;
- data;
- status;
- comentário;
- ação.

---

## 10.02 — Comparar Versões

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** diff lado a lado.

### Componentes

- versão A;
- versão B;
- diff visual;
- diff JSON;
- highlights;
- restaurar.

---

## 10.03 — Restaurar Versão

**Status:** Core MVP  
**Prioridade:** Média  
**Objetivo:** recuperar conteúdo anterior.

### Componentes

- confirmação;
- impacto;
- motivo;
- auditoria.

---

# 19. 11 — ASSETS

---

## 11.01 — Biblioteca de Assets

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Editor, Product Manager  
**Objetivo:** gerenciar arquivos.

### Layout

Grid principal.

### Filtros

- imagem;
- vídeo;
- PDF;
- áudio;
- tag;
- produto;
- uso.

### Estados

- vazio;
- upload em andamento;
- erro de upload;
- sem permissão;
- arquivo processando.

---

## 11.02 — Upload de Asset

**Status:** Core MVP  
**Prioridade:** Crítica  
**Objetivo:** enviar arquivos.

### Componentes

- drag and drop;
- seleção manual;
- progresso;
- validação de tipo;
- validação de tamanho;
- metadados iniciais.

---

## 11.03 — Detalhe do Asset

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** visualizar arquivo e metadados.

### Campos

- preview;
- nome;
- alt text;
- legenda;
- tags;
- crédito;
- tamanho;
- dimensões;
- uso no sistema.

---

## 11.04 — Editar Metadados do Asset

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** melhorar SEO, acessibilidade e organização.

### Campos

- nome;
- alt;
- caption;
- tags;
- crédito;
- visibilidade.

---

## 11.05 — Uso do Asset

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** mostrar onde o asset está sendo usado.

### Exemplo

- Home hero;
- Galeria;
- SEO OG image;
- produto X.

---

# 20. 12 — FORMULÁRIOS

---

## 12.01 — Lista de Formulários

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager  
**Objetivo:** gerenciar formulários do produto.

### Exemplos

- contato;
- orçamento;
- candidatura;
- inscrição.

### Colunas

- nome;
- slug;
- respostas;
- status;
- última resposta;
- produto.

---

## 12.02 — Criar Formulário

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** criar formulário configurável.

### MVP

Campos fixos inicialmente.

### Futuro

Builder visual.

---

## 12.03 — Builder de Formulário MVP

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** configurar campos básicos.

### Campos

- texto;
- e-mail;
- telefone;
- select;
- textarea;
- date;
- checkbox.

---

## 12.04 — Detalhe do Formulário

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** acompanhar configuração e respostas.

### Abas

- Configuração;
- Campos;
- Respostas;
- Integrações.

---

# 21. 13 — SUBMISSIONS

---

## 13.01 — Lista de Submissions

**Status:** Core MVP  
**Prioridade:** Crítica  
**Persona principal:** Product Manager  
**Objetivo:** visualizar respostas recebidas.

### Filtros

- produto;
- formulário;
- período;
- status;
- origem.

### Ações

- abrir;
- exportar;
- arquivar;
- marcar resolvido.

---

## 13.02 — Detalhe da Submission

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** ler resposta completa.

### Componentes

- dados enviados;
- metadados;
- origem;
- status;
- notas internas;
- histórico.

---

## 13.03 — Exportar Submissions

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** exportar CSV/JSON.

### Estados

- exportando;
- sucesso;
- erro;
- sem permissão.

---

# 22. 14 — ANALYTICS

---

## 14.01 — Dashboard Analytics

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager  
**Objetivo:** acompanhar performance do produto.

### Widgets

- visitas;
- conversão;
- páginas populares;
- formulários;
- origem de tráfego;
- período.

### Estados

- sem dados;
- loading;
- erro;
- dados parciais;
- sem permissão.

---

## 14.02 — Analytics de Conteúdo

**Status:** Pós-MVP  
**Prioridade:** Média  
**Objetivo:** avaliar conteúdo.

### Métricas

- visualizações;
- engajamento;
- publicações;
- revisões;
- traduções pendentes.

---

## 14.03 — Analytics de Formulários

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** acompanhar conversões.

### Métricas

- submissões;
- taxa de conversão;
- origem;
- período;
- status.

---

## 14.04 — Analytics de Produto

**Status:** Pós-MVP  
**Prioridade:** Média  
**Objetivo:** consolidar visão por produto.

---

# 23. 15 — SEO

---

## 15.01 — Configuração SEO Global

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager  
**Objetivo:** configurar SEO do produto.

### Campos

- title;
- description;
- keywords;
- default OG image;
- base URL;
- locale.

---

## 15.02 — Preview Google

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** simular resultado de busca.

---

## 15.03 — Preview Social

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** simular compartilhamento.

---

## 15.04 — Checklist SEO

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** orientar melhorias.

### Itens

- title presente;
- description presente;
- imagem OG;
- slug amigável;
- alt text;
- idioma.

---

# 24. 16 — KNOWLEDGE GRAPH

---

## 16.01 — Lista de Nodes

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager, Super Admin  
**Objetivo:** listar entidades do graph.

### Colunas

- nome;
- tipo;
- produto;
- relações;
- status;
- atualização.

---

## 16.02 — Criar Node

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** criar entidade manualmente.

### Campos

- tipo;
- nome;
- descrição;
- metadados;
- produto;
- tags.

---

## 16.03 — Detalhe do Node

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** visualizar entidade e relações.

### Abas

- Geral;
- Relações;
- Metadados;
- Conteúdos vinculados;
- Histórico.

---

## 16.04 — Lista de Edges

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** listar relações.

### Colunas

- origem;
- destino;
- tipo;
- peso;
- produto;
- atualização.

---

## 16.05 — Criar Edge

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** criar relação manual.

### Campos

- origem;
- destino;
- tipo;
- metadados.

---

## 16.06 — Visualização Graph

**Status:** Core MVP  
**Prioridade:** Alta  
**Objetivo:** canvas interativo do grafo.

### Componentes

- canvas;
- filtros;
- zoom;
- busca;
- painel lateral de node;
- painel lateral de edge;
- legenda.

### Estados

- graph vazio;
- muitos nodes;
- erro de renderização;
- loading;
- sem permissão.

---

## 16.07 — Graph por Produto

**Status:** Pós-MVP  
**Prioridade:** Média  
**Objetivo:** filtrar relações de um produto.

---

## 16.08 — Graph Global

**Status:** Experimental / BYOP  
**Prioridade:** Média  
**Objetivo:** visualizar relações entre produtos do ecossistema.

### Exemplo

- Maestro Beton → Site;
- Site → Formulário;
- Formulário → Lead;
- Lead → Produto;
- Produto → Tenant.

---

# 25. 17 — AUDITORIA

---

## 17.01 — Timeline de Auditoria

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Super Admin, Tenant Admin  
**Objetivo:** visualizar eventos relevantes.

### Eventos

- login;
- criação;
- edição;
- publicação;
- exclusão lógica;
- convite;
- alteração de permissão;
- habilitação de módulo.

### Filtros

- usuário;
- tenant;
- produto;
- evento;
- período;
- módulo.

---

## 17.02 — Detalhe do Evento de Auditoria

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** inspecionar evento.

### Componentes

- ator;
- recurso;
- antes/depois;
- metadados;
- IP, futuro;
- user agent, futuro.

---

## 17.03 — Auditoria por Produto

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** restringir timeline ao produto ativo.

---

# 26. 18 — CONFIGURAÇÕES

---

## 18.01 — Configurações do Produto

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Product Manager  
**Objetivo:** configurar produto.

### Seções

- dados gerais;
- idioma padrão;
- branding;
- domínios;
- módulos;
- SEO;
- integrações.

---

## 18.02 — Configurações do Tenant

**Status:** Core MVP  
**Prioridade:** Alta  
**Persona principal:** Tenant Admin  
**Objetivo:** configurar organização.

### Seções

- dados gerais;
- usuários;
- segurança;
- permissões;
- branding do tenant;
- integrações.

---

## 18.03 — Configurações do Sistema

**Status:** Pós-MVP  
**Prioridade:** Alta para Super Admin  
**Persona principal:** Super Admin  
**Objetivo:** parâmetros globais.

### Seções

- tenants;
- limites;
- módulos globais;
- feature flags;
- integrações globais;
- manutenção.

---

## 18.04 — Integrações do Produto

**Status:** MVP Expandido  
**Prioridade:** Média  
**Objetivo:** gerenciar integrações.

### Exemplos

- WhatsApp;
- Instagram;
- YouTube;
- Google Maps;
- Webhook;
- Analytics provider.

---

## 18.05 — Domínios do Produto

**Status:** Pós-MVP  
**Prioridade:** Média  
**Objetivo:** associar domínios.

### Campos

- domínio;
- status DNS;
- SSL;
- ambiente;
- publicação.

---

# 27. 19 — NOTIFICAÇÕES

---

## 19.01 — Centro de Notificações

**Status:** Pós-MVP  
**Prioridade:** Média  
**Persona principal:** Todas  
**Objetivo:** centralizar alertas.

### Tipos

- aprovação pendente;
- erro integração;
- novo formulário;
- publicação;
- convite;
- falha de deploy futura.

---

## 19.02 — Preferências de Notificação

**Status:** Futuro  
**Prioridade:** Baixa/Média  
**Objetivo:** controlar canais.

### Canais futuros

- in-app;
- e-mail;
- Telegram;
- push.

---

# 28. 20 — OPERAÇÃO / SISTEMA

---

## 20.01 — Saúde do Sistema

**Status:** Pós-MVP  
**Prioridade:** Média  
**Persona principal:** Super Admin  
**Objetivo:** monitorar plataforma.

### Métricas

- API;
- banco;
- storage;
- filas futuras;
- integrações.

---

## 20.02 — Logs Operacionais

**Status:** Futuro  
**Prioridade:** Média  
**Objetivo:** consultar logs de operação.

---

## 20.03 — Feature Flags

**Status:** Futuro  
**Prioridade:** Média  
**Objetivo:** controlar funcionalidades.

---

## 20.04 — Ambientes

**Status:** Futuro  
**Prioridade:** Média  
**Objetivo:** separar staging/produção.

---

# 29. 21 — ESTADOS GLOBAIS OBRIGATÓRIOS

Toda tela do Aegis deve possuir variações previsíveis para estados.

---

## 21.01 — Loading

### Aplicar em

- dashboards;
- tabelas;
- cards;
- gráficos;
- editor;
- graph;
- assets.

### Diretriz visual

Skeleton elegante, sem spinner excessivo.

---

## 21.02 — Empty State

### Aplicar em

- sem produtos;
- sem conteúdos;
- sem assets;
- sem submissions;
- sem graph;
- sem usuários;
- sem analytics.

### Microcopy recomendada

- “Nenhum produto criado ainda.”
- “Este módulo ainda não possui conteúdo.”
- “Comece criando o primeiro item.”
- “Nada exige sua atenção agora.”

---

## 21.03 — Error State

### Aplicar em

- falha de API;
- permissão negada;
- recurso não encontrado;
- erro parcial.

### Diretriz visual

Erro deve ser claro, mas não dramático.

---

## 21.04 — Success State

### Aplicar em

- salvamento;
- publicação;
- convite enviado;
- upload concluído;
- módulo habilitado.

### Diretriz visual

Feedback curto, elegante, reversível quando possível.

---

## 21.05 — Permission State

### Variações

- sem acesso ao módulo;
- sem permissão de edição;
- sem permissão de exclusão;
- permissão expirada;
- papel insuficiente.

### Diretriz

Explicar o motivo e sugerir contato com administrador.

---

## 21.06 — Conflict State

### Aplicar em

- edição concorrente;
- conteúdo alterado por outro usuário;
- versão desatualizada;
- slug já usado.

---

## 21.07 — Draft / Review / Published / Archived

### Aplicar em

- conteúdo;
- páginas;
- blocos;
- produtos, se necessário.

---

## 21.08 — Destructive Confirmation

### Aplicar em

- arquivar;
- excluir logicamente;
- remover usuário;
- desabilitar módulo;
- restaurar versão.

---

# 30. 22 — RESPONSIVIDADE

---

## 22.01 — Desktop

### Largura alvo

1440px.

### Layout

- sidebar fixa;
- header com contexto;
- área principal ampla;
- tabelas completas;
- painéis laterais;
- dashboards em grid.

---

## 22.02 — Tablet

### Largura alvo

768px–1023px.

### Layout

- sidebar colapsável;
- tabelas com colunas reduzidas;
- cards substituem grids complexos;
- painéis viram drawers.

---

## 22.03 — Mobile

### Largura alvo

390px–430px.

### Layout

- bottom navigation ou drawer;
- tabelas viram cards;
- ações críticas ficam em menus;
- filtros viram bottom sheets;
- editor complexo deve ser simplificado;
- graph pode virar lista por padrão.

### Observação

Aegis é uma plataforma administrativa, portanto mobile deve permitir consulta e ações rápidas, mas não necessariamente edição complexa total.

---

# 31. MATRIZ DE PRIORIDADE PARA FIGMA

---

## 31.1 Prototipar primeiro

1. Login
2. Dashboard Global
3. Lista de Produtos
4. Detalhe do Produto
5. Catálogo de Módulos
6. Dashboard Editorial
7. Lista de Conteúdos
8. Editor de Conteúdo
9. Biblioteca de Assets
10. Lista de Submissions
11. Analytics
12. Knowledge Graph
13. Configurações do Produto

---

## 31.2 Prototipar depois

1. Tenants
2. Usuários
3. Workflow
4. Versionamento
5. SEO
6. Auditoria
7. Forms Builder
8. Detalhe de Asset
9. Detalhe de Submission

---

## 31.3 Não prototipar no primeiro Figma

1. Feature Flags
2. Saúde do Sistema
3. Logs Operacionais
4. Domínios avançados
5. Notificações avançadas
6. Integrações complexas

---

# 32. INVENTÁRIO RESUMIDO POR STATUS

## Core MVP

1. Login
2. Recuperar senha
3. Primeiro acesso
4. Seleção de tenant
5. Alternador de tenant
6. Alternador de produto
7. Dashboard global
8. Lista tenants
9. Criar tenant
10. Detalhe tenant
11. Editar tenant
12. Lista usuários
13. Convidar usuário
14. Editar permissões
15. Lista produtos
16. Criar produto
17. Detalhe produto
18. Editar produto
19. Produto sem módulos
20. Catálogo módulos
21. Habilitar módulo
22. Dashboard editorial
23. Lista conteúdos
24. Criar conteúdo
25. Editor conteúdo
26. Preview conteúdo
27. Lista páginas
28. Editor página
29. SEO página
30. Preview responsivo
31. Workflow aprovação
32. Enviar revisão
33. Revisar conteúdo
34. Histórico workflow
35. Histórico versões
36. Comparar versões
37. Restaurar versão
38. Biblioteca assets
39. Upload asset
40. Detalhe asset
41. Editar metadados asset
42. Lista formulários
43. Detalhe formulário
44. Lista submissions
45. Detalhe submission
46. Dashboard analytics
47. SEO global
48. Preview Google
49. Preview Social
50. Lista nodes
51. Detalhe node
52. Lista edges
53. Visualização graph
54. Auditoria timeline
55. Configurações produto
56. Configurações tenant

Total Core MVP expandido realista: **56 telas/variações**.

---

## MVP Expandido

1. Arquivar tenant
2. Detalhe usuário
3. Revogar acesso
4. Preview público produto
5. Detalhe módulo
6. Desabilitar módulo
7. Editor JSON/Contrato
8. Biblioteca de blocos
9. Uso do asset
10. Criar formulário
11. Builder formulário MVP
12. Exportar submissions
13. Analytics de formulários
14. Checklist SEO
15. Criar node
16. Criar edge
17. Detalhe auditoria
18. Auditoria por produto
19. Integrações produto

Total MVP Expandido: **19 telas/variações**.

---

## Pós-MVP / Futuro

1. Analytics conteúdo
2. Analytics produto
3. Graph por produto
4. Configurações sistema
5. Domínios produto
6. Centro notificações
7. Preferências notificações
8. Saúde sistema
9. Logs operacionais
10. Feature flags
11. Ambientes
12. Graph global

Total Pós-MVP/Futuro: **12 telas/variações**.

---

# 33. CONCLUSÃO

O Aegis PMS não possui 27 telas.

As 27 telas iniciais são apenas a primeira camada visível.

Quando estados, permissões, detalhes, modais, responsividade e variações de contexto são considerados, o produto real se aproxima de:

```txt
56 telas no Core MVP expandido
19 telas no MVP Expandido
12 telas Pós-MVP/Futuro
87 telas/variações já identificadas
```

Esse número pode facilmente ultrapassar 100 quando forem adicionados:

- integrações avançadas;
- e-commerce;
- courses;
- members;
- automações;
- IA;
- deploy;
- ambientes;
- webhooks;
- templates;
- marketplace de módulos.

Portanto, a recomendação de Aegis estava correta:

> Antes de desenhar telas, é preciso inventariar o sistema.

A próxima etapa recomendada é:

# User Journeys

Com jornadas separadas para:

- Super Admin;
- Tenant Admin;
- Product Manager;
- Editor;
- Viewer.

Somente depois disso o Figma Make deve receber um prompt mestre.

---

# 34. FRASE GUIA

> Aegis não é um painel de administração.
> É o centro de comando onde produtos digitais são criados, operados, observados, publicados e lembrados.
