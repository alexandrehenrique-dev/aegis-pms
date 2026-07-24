#!/usr/bin/env bash
#
# Executa o hardening do Keycloak com o CLI oficial em container efemero.
# Quando KEYCLOAK_RESOLVE_IP estiver definido no arquivo operacional, adiciona
# um override DNS apenas ao container, preservando hostname, SNI e TLS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

AEGIS_DEPLOY_BRANCH="${AEGIS_DEPLOY_BRANCH:-develop}"
AEGIS_ENV_FILE="${AEGIS_ENV_FILE:-/opt/genesis-lab/services/aegis-pms/secrets/.env.${AEGIS_DEPLOY_BRANCH}}"
KEYCLOAK_CLI_IMAGE="${KEYCLOAK_CLI_IMAGE:-quay.io/keycloak/keycloak:26.0}"
DOCKER_COMMAND="${DOCKER_COMMAND:-docker}"

fail() {
  printf 'Erro: %s\n' "$*" >&2
  exit 1
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
  done < "$AEGIS_ENV_FILE"
}

validate_ipv4() {
  local address="$1"
  local octet
  local -a octets

  IFS='.' read -r -a octets <<< "$address"
  [ "${#octets[@]}" -eq 4 ] || return 1
  for octet in "${octets[@]}"; do
    [[ "$octet" =~ ^[0-9]{1,3}$ ]] || return 1
    [ "$octet" -le 255 ] || return 1
  done
}

[ -f "$AEGIS_ENV_FILE" ] || fail "arquivo de ambiente não encontrado: $AEGIS_ENV_FILE"
command -v "$DOCKER_COMMAND" >/dev/null 2>&1 \
  || fail "Docker não encontrado: $DOCKER_COMMAND"

KEYCLOAK_URL="${KEYCLOAK_URL:-$(read_env_value KEYCLOAK_URL)}"
KEYCLOAK_RESOLVE_IP="${KEYCLOAK_RESOLVE_IP:-$(read_env_value KEYCLOAK_RESOLVE_IP)}"

case "$KEYCLOAK_URL" in
  http://*|https://*)
    ;;
  *)
    fail "KEYCLOAK_URL deve usar http:// ou https://."
    ;;
esac

KEYCLOAK_AUTHORITY="${KEYCLOAK_URL#*://}"
KEYCLOAK_AUTHORITY="${KEYCLOAK_AUTHORITY%%/*}"
KEYCLOAK_HOSTNAME="${KEYCLOAK_AUTHORITY%%:*}"
[[ "$KEYCLOAK_HOSTNAME" =~ ^[a-zA-Z0-9.-]+$ ]] \
  || fail "hostname inválido em KEYCLOAK_URL."

DOCKER_ARGS=(
  run
  --rm
)

if [ -n "$KEYCLOAK_RESOLVE_IP" ]; then
  validate_ipv4 "$KEYCLOAK_RESOLVE_IP" \
    || fail "KEYCLOAK_RESOLVE_IP deve ser um endereço IPv4 válido."
  DOCKER_ARGS+=(--add-host "${KEYCLOAK_HOSTNAME}:${KEYCLOAK_RESOLVE_IP}")
  printf '==> Rota privada do Keycloak: %s -> %s\n' \
    "$KEYCLOAK_HOSTNAME" "$KEYCLOAK_RESOLVE_IP"
fi

DOCKER_ARGS+=(
  --volume "$REPOSITORY_DIR:/workspace:ro"
  --volume "$AEGIS_ENV_FILE:/run/secrets/aegis.env:ro"
  --workdir /workspace
  --entrypoint /bin/bash
  "$KEYCLOAK_CLI_IMAGE"
  infra/keycloak/scripts/harden-realm-prod.sh
  --env-file /run/secrets/aegis.env
)

"$DOCKER_COMMAND" "${DOCKER_ARGS[@]}"
