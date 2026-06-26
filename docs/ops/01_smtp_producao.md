# Ops 01 — SMTP de produção (substituir MailHog)

> **Escopo:** configuração de infraestrutura, zero mudança de código no Aegis.
>
> O Aegis usa `JavaMailSender` do Spring Boot, que lê as variáveis de ambiente abaixo. Trocar de MailHog para produção é apenas uma substituição de valores no `.env` — não há código a alterar.

## Variáveis de ambiente SMTP do Aegis

```env
SMTP_HOST=
SMTP_PORT=
SMTP_USER=
SMTP_PASSWORD=
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=noreply@seudominio.com
SMTP_FROM_NAME=Aegis PMS
```

## Opções de provedor

### Opção 1 — Resend (recomendada para MVP)

**Por quê:** API moderna, 3.000 e-mails/mês gratuitos, dashboard de entrega em tempo real, domínio verificado em ~5 minutos, suporte a SMTP relay sem código adicional.

**Cadastro:** https://resend.com → criar conta → Settings → SMTP → gerar API key → verificar domínio (adicionar registros DNS MX/SPF/DKIM que eles fornecerão).

```env
SMTP_HOST=smtp.resend.com
SMTP_PORT=587
SMTP_USER=resend
SMTP_PASSWORD=re_xxxxxxxxxxxxxxxxxxxxxxxxxxxx   # API key gerada no dashboard
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=noreply@seudominio.com
SMTP_FROM_NAME=Aegis PMS
```

**Limites gratuitos:** 3.000/mês, 100/dia. Para o Aegis em fase inicial (convites + resets + notificações), confortável.

**Quando escalar:** plano Pro a partir de $20/mês para 50.000 e-mails.

---

### Opção 2 — Brevo (antigo Sendinblue)

**Por quê:** 300 e-mails/dia gratuitos, painel de análise detalhado, sem necessidade de cartão de crédito.

**Cadastro:** https://www.brevo.com → criar conta → Settings → SMTP & API → gerar chave SMTP.

```env
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USER=seu-email-de-cadastro@brevo.com
SMTP_PASSWORD=xsmtpsib-xxxxxxxxxxxxxxxxxxxxxxxxxxxx   # chave gerada no painel
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=noreply@seudominio.com
SMTP_FROM_NAME=Aegis PMS
```

**Limites gratuitos:** 300/dia (9.000/mês). Adequado para fases iniciais.

**Observação:** o `SMTP_USER` é o e-mail usado no cadastro, não uma chave separada.

---

### Opção 3 — Amazon SES

**Por quê:** escala infinita, $0,10 por 1.000 e-mails (praticamente zero custo em produção), sem limite de volume após verificação de domínio.

**Requisitos:** conta AWS com domínio verificado no SES + sair do sandbox (processo de aprovação de 24h).

**Setup:** AWS Console → SES → SMTP Settings → Create SMTP credentials → copiar `SMTP_USER` e `SMTP_PASSWORD` gerados.

```env
SMTP_HOST=email-smtp.us-east-1.amazonaws.com   # ou a região que você usa
SMTP_PORT=587
SMTP_USER=AKIAIOSFODNN7EXAMPLE                 # IAM SMTP username
SMTP_PASSWORD=BmxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxJ  # IAM SMTP password
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=noreply@seudominio.com
SMTP_FROM_NAME=Aegis PMS
```

**Atenção:** a senha SMTP do SES não é a senha da conta AWS — é gerada especificamente para SMTP nas configurações do SES.

---

## Checklist de troca de MailHog para produção

```
[ ] Escolher provedor (recomendado: Resend para MVP)
[ ] Criar conta e verificar domínio (DNS: SPF, DKIM, DMARC)
[ ] Gerar credenciais SMTP
[ ] Atualizar variáveis de ambiente no servidor de produção (.env ou secret manager)
[ ] NÃO subir as variáveis para o git — usar .env.production.local ou secrets do CI/CD
[ ] Remover (ou desabilitar no docker-compose.prod.yml) o serviço MailHog
[ ] Testar enviando um convite real via /api/v1/auth/invite/validate (etapa 28)
[ ] Verificar entrega no painel do provedor (Resend/Brevo/SES console)
[ ] Monitorar bounces e spam complaints no painel (limites de entrega se > 5% bounce)
```

## Registros DNS obrigatórios (todos os provedores)

Independente do provedor, configurar no DNS do domínio remetente:

```
# SPF — autoriza o provedor a enviar em seu nome
TXT  @  "v=spf1 include:provedor.com ~all"   # o provedor fornece o include correto

# DKIM — assinatura criptográfica (gerada no painel do provedor)
CNAME  resend._domainkey  resend._domainkey.seudominio.com.   # exemplo Resend

# DMARC — política de rejeição/quarentena
TXT  _dmarc  "v=DMARC1; p=quarantine; rua=mailto:dmarc@seudominio.com"
```

Sem SPF+DKIM, e-mails do Aegis irão para spam ou serão rejeitados pelo Gmail/Outlook.

## docker-compose — remover MailHog em produção

No `docker-compose.yml` de produção (ou arquivo `.prod.yml`), remover o serviço MailHog:

```yaml
# Remover em produção:
# mailhog:
#   image: mailhog/mailhog:v1.0.1
#   ports:
#     - "1025:1025"
#     - "8025:8025"
```

Em desenvolvimento, o MailHog permanece exatamente como está — sem nenhuma mudança.

## Decisão de arquitetura — sem sidecar separado

O usuário considerou criar uma aplicação/serviço separado só para e-mail. Isso não é necessário neste caso porque:

1. `JavaMailSender` do Spring Boot já é um cliente SMTP puro — conecta-se diretamente ao servidor do provedor
2. Os provedores acima (Resend, Brevo, SES) são servidores SMTP hospedados — não precisam de sidecar
3. A troca é 100% configuração de ambiente, sem código novo
4. Um sidecar adicionaria latência, um ponto de falha extra e complexidade de deploy sem ganho prático neste escopo

Se no futuro houver necessidade de fila de e-mails com retry automático, priorização, ou templates dinâmicos externos, um sidecar como [Postal](https://postalserver.io/) ou um worker separado faz sentido — mas não agora.
