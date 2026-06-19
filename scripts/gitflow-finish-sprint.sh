#!/usr/bin/env bash
#
# gitflow-finish-sprint.sh
#
# Finaliza uma branch de sprint no fluxo gitflow do projeto:
#   1. Atualiza develop.
#   2. Faz merge --no-ff da branch da sprint em develop.
#   3. Envia develop atualizada para o remoto.
#   4. Apaga a branch da sprint SOMENTE no remoto (mantém localmente).
#
# Uso:
#   ./scripts/gitflow-finish-sprint.sh sprint/09-servicos-fluxo-super-admin
#
# Requisitos: já deve existir um remote chamado "origin" e a branch
# informada já deve ter sido enviada ao remoto (git push -u origin <branch>).

set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Uso: $0 <nome-da-branch>" >&2
  echo "Exemplo: $0 sprint/09-servicos-fluxo-super-admin" >&2
  exit 1
fi

BRANCH="$1"
REMOTE="origin"
BASE_BRANCH="develop"

if ! git rev-parse --verify "$BRANCH" >/dev/null 2>&1; then
  echo "Erro: branch local '$BRANCH' não encontrada." >&2
  exit 1
fi

echo "==> Atualizando ${BASE_BRANCH}..."
git checkout "$BASE_BRANCH"
git pull "$REMOTE" "$BASE_BRANCH"

echo "==> Fazendo merge --no-ff de ${BRANCH} em ${BASE_BRANCH}..."
git merge --no-ff "$BRANCH" -m "merge: finaliza ${BRANCH} em ${BASE_BRANCH}"

echo "==> Enviando ${BASE_BRANCH} para ${REMOTE}..."
git push "$REMOTE" "$BASE_BRANCH"

echo "==> Apagando ${BRANCH} somente no remoto (mantendo localmente)..."
git push "$REMOTE" --delete "$BRANCH"

echo "==> Concluído. ${BRANCH} foi mesclada em ${BASE_BRANCH}, removida do remoto, e continua disponível localmente."
