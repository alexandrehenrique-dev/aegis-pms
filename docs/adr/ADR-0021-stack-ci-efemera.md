# ADR-0021 — Stack efêmera de CI, isolada da infraestrutura de produção

## Status

ACCEPTED

## Contexto

Na Release 1, os testes de integração (coleções Bruno) rodavam no CI sobre o mesmo `docker-compose.yml` de desenvolvimento, no runner self-hosted do Genesis Lab. Isso funcionava, mas com duas fragilidades:

1. **Estado persistente invisível.** O realm `aegis` e os usuários de teste (`aegis` com `AEGIS_SUPER_ADMIN`) não estão no `aegis-realm.json` exportado — foram criados manualmente via `kcadm.sh` e sobreviviam nos volumes Docker do runner. O CI só passava porque esse estado residual existia. Qualquer runner novo (ou `docker volume prune`) quebraria o pipeline silenciosamente.

2. **Arquitetura futura híbrida.** A produção migrará para um modelo em que PostgreSQL (Aegis e Keycloak) e o próprio Keycloak vivem em uma máquina Oracle Cloud, enquanto Backend, Frontend, Proxy e Observabilidade permanecem no Genesis Lab. Nesse cenário, "subir a stack local" deixa de ser um espelho aceitável de produção — e apontar os testes para a infraestrutura real seria inaceitável: testes não determinísticos, dependentes de internet, perigosos, e capazes de poluir usuários e dados reais (o Bruno cria e apaga tenants, usuários no Keycloak e mensagens SMTP).

## Decisão

Cada ambiente passa a ter um compose de finalidade única:

| Ambiente | Arquivo | Característica |
|---|---|---|
| dev | `docker-compose.yml` (raiz) | volumes persistentes; nome default preservado para manter `docker compose up` |
| ci | `infra/docker-compose.ci.yml` | **totalmente efêmero**: tmpfs, sem volumes nomeados, credenciais descartáveis em `infra/.env.ci` (commitado) |
| prod | `infra/docker-compose.prod.yml` | somente backend; PostgreSQL e Keycloak externos (Oracle Cloud) |

A stack de CI sobe PostgreSQL Aegis, PostgreSQL Keycloak, Keycloak, MailHog e Backend — todos com sufixo `-ci`, em projeto compose próprio (`aegis-pms-ci`) e rede própria. O Keycloak importa o realm canônico automaticamente (`--import-realm`) e um serviço one-shot (`keycloak-init-ci`) cria os usuários de teste via `kcadm.sh`, eliminando a dependência de estado manual do runner. O workflow encerra sempre com `docker compose down -v` (`if: always()`) seguido de uma asserção de que nenhum container ou volume do projeto `aegis-pms-ci` permaneceu.

O Keycloak usado nos testes **jamais** poderá ser o Keycloak de produção: os testes autenticam via password grant com credenciais descartáveis, criam/excluem usuários pelo Admin API e o teardown apaga usuários e tenants por prefixo — operações que, contra o realm real, corromperiam dados de clientes.

## Consequências

Benefícios:

- **Isolamento**: nenhum teste alcança banco, realm ou usuários reais; portas de PostgreSQL nem são publicadas no host.
- **Repetibilidade**: cada run parte do mesmo estado zero (realm importado + usuários criados por script), sem depender de resíduos do runner.
- **Independência**: o pipeline funciona em qualquer runner com Docker, sem provisionamento manual prévio e sem internet além dos registries.
- **Paralelismo**: projeto compose e rede próprios abrem caminho para múltiplas execuções simultâneas (resta parametrizar as portas do host se necessário).
- **Segurança**: credenciais de CI são descartáveis e públicas por design; segredos reais nunca entram no job de testes.
- **Preparação para Oracle Cloud**: quando a persistência e a identidade migrarem, o CI não muda — ele já não depende da topologia de produção.

Custos aceitos:

- Subida um pouco mais lenta (import de realm + bootstrap de usuários a cada run).
- Duplicação controlada de definição de serviços entre dev e CI (mitigada pelos cabeçalhos cruzados nos composes).
- `infra/.env.ci` commitado exige disciplina: nenhum valor real pode entrar ali.
