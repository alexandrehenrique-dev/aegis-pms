# Etapa 08 — Health, info e readiness

> Cole este arquivo inteiro numa conversa nova do GPT. Pré-requisito: etapas 04-07 concluídas.

## Contexto fixo

Aegis PMS precisa permitir validar rapidamente se aplicação, banco, Keycloak e o módulo de grafo estão funcionando — essencial para diagnosticar problemas em qualquer ambiente.

## Objetivo

Endpoints de health/status que cubram app, banco, Keycloak e Knowledge Graph.

## Tarefas

Usar Spring Actuator:

```txt
GET /actuator/health
GET /actuator/info
```

Criar endpoint customizado:

```txt
GET /api/v1/system/status
```

Deve informar: status da app, status do banco, se o issuer do Keycloak está configurado, status do módulo de grafo, profile ativo, versão de build (se disponível).

## Critérios de aceite

- [ ] `/actuator/health` público, responde `UP`.
- [ ] `/api/v1/system/status` exige autenticação e responde com os campos acima.
- [ ] Banco aparece como `UP` quando conectado.
- [ ] Falhas (ex.: banco fora do ar) produzem mensagem legível, não stack trace bruto.

## Validação

> **Entrega via collection Postman, não só curl** (ver `00_padrao_qualidade_e_arquitetura.md`, Seção 11). Os `curl` abaixo são a especificação exata de cada request — adicione-os à pasta desta etapa em `aegis-postman-collection.json` (collection cumulativa, autenticação via `{{token}}` herdado da pasta "Auth") e devolva o JSON completo atualizado para download.

```bash
curl http://localhost:8080/actuator/health
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/system/status
```

## Artefato de continuidade — `SPRINT-RESULTADO.md`

> Ver `00_padrao_qualidade_e_arquitetura.md`, Seção 12. Antes do commit, gere/atualize `docs/sprints/sprint-02-fundacao-backend-gpt/SPRINT-RESULTADO.md` (arquivo inteiro, nunca um diff) com a entrada desta etapa (template fixo da Seção 12.2): classes criadas, endpoints confirmados, qualquer decisão que esta etapa deixou a seu critério (registre a escolha real), e retrofits pendentes para etapas futuras. É o que a próxima conversa do GPT vai receber em vez da memória que ela não tem.

## Commit sugerido

```bash
git add backend/
git commit -m "feat(backend): endpoint de system status cobrindo app, banco, keycloak e grafo"
```
