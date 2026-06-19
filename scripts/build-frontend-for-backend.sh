#!/usr/bin/env bash
#
# build-frontend-for-backend.sh
#
# Builda o frontend (React + Vite) e copia o resultado para dentro do backend
# Spring Boot, para que ele sirva a SPA na mesma origem (ADR-0009 + ADR-0011,
# ver docs/sprints/sprint-02-fundacao-backend-gpt/09_build_frontend_react_no_backend.md).
#
# Uso:
#   ./scripts/build-frontend-for-backend.sh
#   (ou, de dentro de frontend/: npm run build:backend)
#
# Seguro de rodar mesmo antes do backend existir: cria o diretório de destino
# se ele ainda não existir.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

FRONTEND_DIR="$ROOT_DIR/frontend"
FRONTEND_DIST="$FRONTEND_DIR/dist"
BACKEND_STATIC_DIR="$ROOT_DIR/backend/src/main/resources/static"

if [ ! -d "$FRONTEND_DIR" ]; then
  echo "Erro: diretório do frontend não encontrado em $FRONTEND_DIR" >&2
  exit 1
fi

echo "==> Instalando dependências do frontend (se necessário)..."
(cd "$FRONTEND_DIR" && npm install)

echo "==> Buildando o frontend (vite build)..."
(cd "$FRONTEND_DIR" && npm run build)

if [ ! -d "$FRONTEND_DIST" ]; then
  echo "Erro: build não gerou $FRONTEND_DIST" >&2
  exit 1
fi

echo "==> Preparando diretório estático do backend em $BACKEND_STATIC_DIR..."
mkdir -p "$BACKEND_STATIC_DIR"

echo "==> Limpando build anterior do backend (se houver)..."
rm -rf "${BACKEND_STATIC_DIR:?}"/*

echo "==> Copiando frontend/dist -> backend/src/main/resources/static..."
cp -R "$FRONTEND_DIST"/. "$BACKEND_STATIC_DIR"/

echo "==> Concluído. O backend (quando existir e estiver rodando) servirá a SPA a partir de $BACKEND_STATIC_DIR."
