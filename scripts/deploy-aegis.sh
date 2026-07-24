#!/usr/bin/env bash
#
# Build e deploy idempotente do Aegis PMS no Genesis Lab.
#
# Configuração canônica:
#   AEGIS_DEPLOY_BRANCH=develop
#
# IMAGE_TAG deriva da branch por padrão. Para uma tag explícita, exporte
# IMAGE_TAG antes da execução. O arquivo de ambiente nunca é carregado como
# shell script e seu conteúdo não é exibido.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

AEGIS_DEPLOY_BRANCH="${AEGIS_DEPLOY_BRANCH:-develop}"
IMAGE_NAME="${IMAGE_NAME:-aegis-pms}"
IMAGE_TAG="${IMAGE_TAG:-$AEGIS_DEPLOY_BRANCH}"
CONTAINER_NAME="${AEGIS_CONTAINER_NAME:-aegis-pms-prod}"
COMPOSE_FILE="${AEGIS_COMPOSE_FILE:-$REPOSITORY_DIR/infra/docker-compose.prod.yml}"
DOCKERFILE="${AEGIS_DOCKERFILE:-$REPOSITORY_DIR/backend/Dockerfile}"
AEGIS_ENV_FILE="${AEGIS_ENV_FILE:-/opt/genesis-lab/services/aegis-pms/secrets/.env.${AEGIS_DEPLOY_BRANCH}}"

VITE_API_MODE="${VITE_API_MODE:-api}"
VITE_API_BASE_URL="${VITE_API_BASE_URL:-/api/v1}"
VITE_KEYCLOAK_URL="${VITE_KEYCLOAK_URL:-https://auth.buildyourownpath.io}"
VITE_KEYCLOAK_REALM="${VITE_KEYCLOAK_REALM:-aegis-pms}"
VITE_KEYCLOAK_CLIENT_ID="${VITE_KEYCLOAK_CLIENT_ID:-aegis-web}"

ROLLBACK_TAG="rollback-${AEGIS_DEPLOY_BRANCH}"
DEPLOYMENT_STARTED=false
PREVIOUS_IMAGE_ID=""
TEMP_DIR="$(mktemp -d)"

log() {
  printf '==> %s\n' "$*"
}

fail() {
  printf 'Erro: %s\n' "$*" >&2
  if [ "${DEPLOYMENT_STARTED:-false}" = true ]; then
    rollback_on_error 1
  fi
  exit 1
}

cleanup() {
  rm -r "$TEMP_DIR"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "dependência obrigatória não encontrada: $1"
}

read_env_value() {
  local key="$1"
  awk -v key="$key" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      if ((substr(value, 1, 1) == "\"" && substr(value, length(value), 1) == "\"") ||
          (substr(value, 1, 1) == "\047" && substr(value, length(value), 1) == "\047")) {
        value = substr(value, 2, length(value) - 2)
      }
      print value
      exit
    }
  ' "$AEGIS_ENV_FILE"
}

wait_for_container_health() {
  local attempt
  local state

  for attempt in $(seq 1 60); do
    state="$(docker inspect "$CONTAINER_NAME" \
      --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
      2>/dev/null || true)"

    if [ "$state" = "healthy" ]; then
      log "Container saudável."
      return 0
    fi

    if [ "$state" = "unhealthy" ] || [ "$state" = "exited" ] || [ "$state" = "dead" ]; then
      printf 'Container entrou no estado %s.\n' "$state" >&2
      return 1
    fi

    sleep 5
  done

  printf 'Timeout aguardando o healthcheck do container.\n' >&2
  return 1
}

rollback_on_error() {
  local exit_code="${1:-$?}"
  trap - ERR

  printf 'Deploy interrompido com status %s.\n' "$exit_code" >&2
  docker logs --tail 100 "$CONTAINER_NAME" 2>/dev/null || true

  if [ "$DEPLOYMENT_STARTED" = true ] && [ -n "$PREVIOUS_IMAGE_ID" ]; then
    log "Restaurando a imagem anterior ${IMAGE_NAME}:${ROLLBACK_TAG}..."
    if IMAGE_TAG="$ROLLBACK_TAG" docker compose \
      -f "$COMPOSE_FILE" \
      --env-file "$AEGIS_ENV_FILE" \
      up -d --force-recreate aegis-backend \
      && wait_for_container_health; then
      printf 'Rollback concluído com a imagem anterior.\n' >&2
    else
      printf 'Rollback automático falhou; intervenção manual necessária.\n' >&2
    fi
  else
    printf 'Não há imagem anterior elegível para rollback automático.\n' >&2
  fi

  exit "$exit_code"
}

trap cleanup EXIT
trap rollback_on_error ERR

case "$AEGIS_DEPLOY_BRANCH" in
  develop|release)
    ;;
  *)
    fail "AEGIS_DEPLOY_BRANCH deve ser 'develop' ou 'release'."
    ;;
esac

case "$IMAGE_TAG" in
  *[!a-zA-Z0-9_.-]*|'')
    fail "IMAGE_TAG contém caracteres inválidos."
    ;;
esac

[ -d "$REPOSITORY_DIR/.git" ] || fail "repositório Git não encontrado em $REPOSITORY_DIR"
[ -f "$DOCKERFILE" ] || fail "Dockerfile não encontrado em $DOCKERFILE"
[ -f "$COMPOSE_FILE" ] || fail "Compose não encontrado em $COMPOSE_FILE"
[ -f "$AEGIS_ENV_FILE" ] || fail "arquivo de ambiente não encontrado em $AEGIS_ENV_FILE"

for dependency in git docker curl awk grep sed seq; do
  require_command "$dependency"
done

docker info >/dev/null 2>&1 || fail "Docker não está acessível para o usuário atual."
docker compose version >/dev/null 2>&1 || fail "Docker Compose não está disponível."

cd "$REPOSITORY_DIR"

[ -z "$(git status --porcelain)" ] || fail "há alterações locais; o deploy foi abortado sem sobrescrevê-las."
git remote get-url origin >/dev/null 2>&1 || fail "remote origin não configurado."

log "Atualizando referências remotas..."
git fetch --prune origin "$AEGIS_DEPLOY_BRANCH"

CURRENT_BRANCH="$(git branch --show-current)"
if [ "$CURRENT_BRANCH" != "$AEGIS_DEPLOY_BRANCH" ]; then
  if git show-ref --verify --quiet "refs/heads/$AEGIS_DEPLOY_BRANCH"; then
    git switch "$AEGIS_DEPLOY_BRANCH"
  else
    git switch --track -c "$AEGIS_DEPLOY_BRANCH" "origin/$AEGIS_DEPLOY_BRANCH"
  fi
fi

read -r AHEAD_COUNT BEHIND_COUNT < <(
  git rev-list --left-right --count "HEAD...origin/$AEGIS_DEPLOY_BRANCH"
)

if [ "$AHEAD_COUNT" -gt 0 ] && [ "$BEHIND_COUNT" -gt 0 ]; then
  fail "a branch local divergiu de origin/$AEGIS_DEPLOY_BRANCH."
fi

if [ "$AHEAD_COUNT" -gt 0 ]; then
  fail "a branch local possui commits ainda não enviados; faça o push antes do deploy."
fi

if [ "$BEHIND_COUNT" -gt 0 ]; then
  log "Aplicando atualização fast-forward de origin/$AEGIS_DEPLOY_BRANCH..."
  git pull --ff-only origin "$AEGIS_DEPLOY_BRANCH"
fi

[ -z "$(git status --porcelain)" ] || fail "o repositório deixou de estar limpo após a atualização."

PUBLIC_URL="${AEGIS_PUBLIC_URL:-$(read_env_value AEGIS_APP_BASE_URL)}"
PUBLIC_URL="${PUBLIC_URL:-https://aegis.byop.dev}"
CORS_ORIGINS="${AEGIS_CORS_TEST_ORIGINS:-$(read_env_value AEGIS_APP_CORS_ALLOWED_ORIGINS)}"
CORS_ORIGINS="${CORS_ORIGINS:-https://aegis.byop.dev}"
CORS_TEST_ORIGIN="$(printf '%s' "${CORS_ORIGINS%%,*}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

log "Validando configuração do Compose..."
IMAGE_TAG="$IMAGE_TAG" docker compose \
  -f "$COMPOSE_FILE" \
  --env-file "$AEGIS_ENV_FILE" \
  config --quiet

PREVIOUS_IMAGE_ID="$(docker inspect "$CONTAINER_NAME" --format '{{.Image}}' 2>/dev/null || true)"
if [ -n "$PREVIOUS_IMAGE_ID" ]; then
  docker image tag "$PREVIOUS_IMAGE_ID" "${IMAGE_NAME}:${ROLLBACK_TAG}"
  log "Imagem atual preservada como ${IMAGE_NAME}:${ROLLBACK_TAG}."
fi

log "Construindo ${IMAGE_NAME}:${IMAGE_TAG} para a branch ${AEGIS_DEPLOY_BRANCH}..."
docker build \
  --no-cache \
  --build-arg "VITE_API_MODE=$VITE_API_MODE" \
  --build-arg "VITE_API_BASE_URL=$VITE_API_BASE_URL" \
  --build-arg "VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL" \
  --build-arg "VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM" \
  --build-arg "VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID" \
  -f "$DOCKERFILE" \
  -t "${IMAGE_NAME}:${IMAGE_TAG}" \
  .

BUILT_IMAGE_ID="$(docker image inspect "${IMAGE_NAME}:${IMAGE_TAG}" --format '{{.Id}}')"
[ -n "$BUILT_IMAGE_ID" ] || fail "o build não produziu uma imagem identificável."

log "Recriando o serviço aegis-backend..."
DEPLOYMENT_STARTED=true
IMAGE_TAG="$IMAGE_TAG" docker compose \
  -f "$COMPOSE_FILE" \
  --env-file "$AEGIS_ENV_FILE" \
  up -d --force-recreate aegis-backend

wait_for_container_health

RUNNING_IMAGE_ID="$(docker inspect "$CONTAINER_NAME" --format '{{.Image}}')"
[ "$RUNNING_IMAGE_ID" = "$BUILT_IMAGE_ID" ] \
  || fail "o container não está executando a imagem ${IMAGE_NAME}:${IMAGE_TAG} recém-gerada."
log "Imagem do container confirmada: $RUNNING_IMAGE_ID"

HEALTH_BODY="$TEMP_DIR/health-body"
HEALTH_STATUS="$(curl -sS -o "$HEALTH_BODY" -w '%{http_code}' "$PUBLIC_URL/actuator/health")"
case "$HEALTH_STATUS" in
  2??)
    ;;
  *)
    fail "healthcheck público retornou HTTP $HEALTH_STATUS."
    ;;
esac
grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "$HEALTH_BODY" \
  || fail "healthcheck público não retornou status UP."
log "Healthcheck público confirmado em $PUBLIC_URL/actuator/health."

PREFLIGHT_HEADERS="$TEMP_DIR/preflight-headers"
PREFLIGHT_BODY="$TEMP_DIR/preflight-body"
PREFLIGHT_STATUS="$(curl -sS \
  -D "$PREFLIGHT_HEADERS" \
  -o "$PREFLIGHT_BODY" \
  -w '%{http_code}' \
  -X OPTIONS \
  "$PUBLIC_URL/api/v1/auth/login" \
  -H "Origin: $CORS_TEST_ORIGIN" \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type')"

case "$PREFLIGHT_STATUS" in
  2??)
    ;;
  *)
    fail "preflight CORS retornou HTTP $PREFLIGHT_STATUS."
    ;;
esac

tr -d '\r' < "$PREFLIGHT_HEADERS" \
  | awk -F ': *' -v expected="$CORS_TEST_ORIGIN" '
      tolower($1) == "access-control-allow-origin" && $2 == expected { found = 1 }
      END { exit(found ? 0 : 1) }
    ' \
  || fail "preflight não devolveu access-control-allow-origin igual à origem configurada."

tr -d '\r' < "$PREFLIGHT_HEADERS" \
  | awk -F ': *' '
      tolower($1) == "access-control-allow-methods" && toupper($2) ~ /(^|, ?| )POST(,|$)/ { found = 1 }
      END { exit(found ? 0 : 1) }
    ' \
  || fail "preflight não autorizou o método POST."
log "Preflight CORS confirmado para $CORS_TEST_ORIGIN."

LOGIN_BODY="$TEMP_DIR/login-body"
LOGIN_STATUS="$(curl -sS \
  -o "$LOGIN_BODY" \
  -w '%{http_code}' \
  -X POST \
  "$PUBLIC_URL/api/v1/auth/login" \
  -H "Origin: $CORS_TEST_ORIGIN" \
  -H 'Content-Type: application/json' \
  --data '{"username":"invalid@example.invalid","password":"invalid"}')"

[ "$LOGIN_STATUS" != "403" ] || fail "login inválido ainda foi bloqueado com HTTP 403."
case "$LOGIN_STATUS" in
  2??)
    fail "credenciais propositalmente inválidas foram aceitas."
    ;;
esac
if grep -Fqi 'Invalid CORS request' "$LOGIN_BODY"; then
  fail "login inválido ainda foi bloqueado pela camada de CORS."
fi
log "Rota de login alcançada sem bloqueio CORS; retorno controlado HTTP $LOGIN_STATUS."

DEPLOYMENT_STARTED=false
log "Deploy concluído: branch=$AEGIS_DEPLOY_BRANCH image=${IMAGE_NAME}:${IMAGE_TAG} status=SUCCESS"
