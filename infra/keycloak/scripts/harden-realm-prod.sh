#!/usr/bin/env bash
#
# Aplica hardening de producao no realm Keycloak do Aegis.
# Uso:
#   bash infra/keycloak/scripts/harden-realm-prod.sh \
#     --env-file /opt/genesis-lab/services/aegis-pms/secrets/.env.develop
#
# Ou com variaveis ja exportadas:
#   KEYCLOAK_URL=https://auth.buildyourownpath.io \
#   KEYCLOAK_ADMIN=admin \
#   KEYCLOAK_ADMIN_PASSWORD=... \
#   SMTP_HOST=smtp.example.com \
#   SMTP_PORT=587 \
#   SMTP_USER=... \
#   SMTP_PASSWORD=... \
#   bash infra/keycloak/scripts/harden-realm-prod.sh

set -euo pipefail

ENV_FILE=""

usage() {
  echo "Uso: $0 [--env-file CAMINHO]" >&2
}

read_env_value() {
  local name="$1"
  local line
  local value

  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      "$name="*)
        value="${line#*=}"
        value="${value#"${value%%[![:space:]]*}"}"
        value="${value%"${value##*[![:space:]]}"}"
        if [[ "$value" == \"*\" && "$value" == *\" ]] ||
           [[ "$value" == \'*\' && "$value" == *\' ]]; then
          value="${value:1:${#value}-2}"
        fi
        printf '%s\n' "$value"
        return
        ;;
    esac
  done < "$ENV_FILE"
}

load_env_value() {
  local name="$1"
  local value

  if [ -n "${!name:-}" ]; then
    return
  fi

  value="$(read_env_value "$name")"
  if [ -n "$value" ]; then
    printf -v "$name" '%s' "$value"
    export "$name"
  fi
}

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Erro: variavel obrigatoria ausente: $name" >&2
    exit 1
  fi
}

while [ $# -gt 0 ]; do
  case "$1" in
    --env-file)
      [ $# -ge 2 ] || {
        usage
        exit 1
      }
      ENV_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Erro: argumento desconhecido: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [ -n "$ENV_FILE" ]; then
  [ -f "$ENV_FILE" ] || {
    echo "Erro: arquivo de ambiente nao encontrado: $ENV_FILE" >&2
    exit 1
  }

  for name in \
    KEYCLOAK_URL KEYCLOAK_REALM KEYCLOAK_WEB_CLIENT_ID \
    KEYCLOAK_ADMIN KEYCLOAK_ADMIN_PASSWORD \
    SMTP_HOST SMTP_PORT SMTP_FROM SMTP_FROM_DISPLAY_NAME SMTP_REPLY_TO \
    SMTP_AUTH SMTP_USER SMTP_PASSWORD SMTP_STARTTLS SMTP_SSL \
    DISABLE_AEGIS_WEB_DIRECT_GRANTS; do
    load_env_value "$name"
  done
fi

REALM="${KEYCLOAK_REALM:-aegis-pms}"
WEB_CLIENT_ID="${KEYCLOAK_WEB_CLIENT_ID:-aegis-web}"
KCADM="${KCADM:-/opt/keycloak/bin/kcadm.sh}"

for name in KEYCLOAK_URL KEYCLOAK_ADMIN KEYCLOAK_ADMIN_PASSWORD SMTP_HOST SMTP_PORT SMTP_USER SMTP_PASSWORD; do
  require_env "$name"
done

[ -x "$KCADM" ] || {
  echo "Erro: kcadm nao encontrado ou sem permissao de execucao: $KCADM" >&2
  exit 1
}

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
