#!/usr/bin/env bash
#
# Aplica hardening de producao no realm Keycloak do Aegis.
# Uso:
#   KEYCLOAK_URL=https://auth.byop.dev \
#   KEYCLOAK_ADMIN=admin \
#   KEYCLOAK_ADMIN_PASSWORD=... \
#   SMTP_HOST=smtp.example.com \
#   SMTP_PORT=587 \
#   SMTP_USER=... \
#   SMTP_PASSWORD=... \
#   bash infra/keycloak/scripts/harden-realm-prod.sh

set -euo pipefail

REALM="${KEYCLOAK_REALM:-aegis}"
WEB_CLIENT_ID="${KEYCLOAK_WEB_CLIENT_ID:-aegis-web}"
KCADM="${KCADM:-/opt/keycloak/bin/kcadm.sh}"

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Erro: variavel obrigatoria ausente: $name" >&2
    exit 1
  fi
}

for name in KEYCLOAK_URL KEYCLOAK_ADMIN KEYCLOAK_ADMIN_PASSWORD SMTP_HOST SMTP_PORT SMTP_USER SMTP_PASSWORD; do
  require_env "$name"
done

echo "==> Autenticando no Keycloak em $KEYCLOAK_URL..."
"$KCADM" config credentials \
  --server "$KEYCLOAK_URL" \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$KEYCLOAK_ADMIN_PASSWORD"

echo "==> Aplicando timeouts de token..."
"$KCADM" update "realms/$REALM" \
  -s accessTokenLifespan=300 \
  -s accessTokenLifespanForImplicitFlow=300 \
  -s ssoSessionIdleTimeout=1800 \
  -s ssoSessionMaxLifespan=28800 \
  -s offlineSessionIdleTimeout=604800 \
  -s offlineSessionMaxLifespan=2592000 \
  -s actionTokenGeneratedByUserLifespan=900 \
  -s refreshTokenMaxReuse=0

echo "==> Ativando brute force protection..."
"$KCADM" update "realms/$REALM" \
  -s bruteForceProtected=true \
  -s permanentLockout=false \
  -s maxFailureWaitSeconds=900 \
  -s minimumQuickLoginWaitSeconds=60 \
  -s waitIncrementSeconds=60 \
  -s quickLoginCheckMilliSeconds=1000 \
  -s maxDeltaTimeSeconds=43200 \
  -s failureFactor=5

echo "==> Aplicando password policy..."
"$KCADM" update "realms/$REALM" \
  -s 'passwordPolicy=length(10) and upperCase(1) and lowerCase(1) and digits(1) and notUsername(undefined) and passwordHistory(5)'

if [ "${DISABLE_AEGIS_WEB_DIRECT_GRANTS:-false}" = "true" ]; then
  echo "==> Desabilitando Direct Access Grants no cliente publico..."
  CLIENT_UUID=$("$KCADM" get clients -r "$REALM" -q "clientId=$WEB_CLIENT_ID" --fields id,clientId | python3 -c 'import json,sys; clients=json.load(sys.stdin); print(clients[0]["id"] if clients else "")')
  if [ -z "$CLIENT_UUID" ]; then
    echo "Erro: cliente Keycloak nao encontrado: $WEB_CLIENT_ID" >&2
    exit 1
  fi
  "$KCADM" update "clients/$CLIENT_UUID" -r "$REALM" \
    -s directAccessGrantsEnabled=false \
    -s standardFlowEnabled=true \
    -s implicitFlowEnabled=false \
    -s publicClient=true
else
  echo "==> Mantendo Direct Access Grants do $WEB_CLIENT_ID: AuthService.login ainda usa password grant."
fi

echo "==> Configurando SMTP do realm..."
"$KCADM" update "realms/$REALM" \
  -s "smtpServer.host=$SMTP_HOST" \
  -s "smtpServer.port=$SMTP_PORT" \
  -s "smtpServer.from=${SMTP_FROM:-noreply@byop.dev}" \
  -s "smtpServer.fromDisplayName=${SMTP_FROM_DISPLAY_NAME:-Aegis PMS}" \
  -s "smtpServer.replyTo=${SMTP_REPLY_TO:-suporte@byop.dev}" \
  -s "smtpServer.auth=${SMTP_AUTH:-true}" \
  -s "smtpServer.user=$SMTP_USER" \
  -s "smtpServer.password=$SMTP_PASSWORD" \
  -s "smtpServer.starttls=${SMTP_STARTTLS:-true}" \
  -s "smtpServer.ssl=${SMTP_SSL:-false}"

echo "==> Hardening concluido com sucesso."
