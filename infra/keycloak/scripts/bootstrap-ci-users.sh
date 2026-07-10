#!/usr/bin/env bash
#
# bootstrap-ci-users.sh — cria os usuários de teste da stack EFÊMERA de CI.
#
# Executado pelo serviço one-shot `keycloak-init-ci` (infra/docker-compose.ci.yml)
# dentro da imagem oficial do Keycloak (que já traz kcadm.sh, mas não traz
# curl/jq — por isso tudo aqui usa apenas bash + kcadm).
#
# O realm canônico (infra/keycloak/realm/aegis-realm.json) é importado pelo
# `start-dev --import-realm` e NÃO contém usuários. Este script complementa a
# importação criando o usuário operacional que as coleções Bruno usam
# (defaults de bruno/collection.bru), com a realm role AEGIS_SUPER_ADMIN.
#
# Idempotente: pode rodar mais de uma vez sem erro.
# JAMAIS usar em produção — credenciais descartáveis de CI.

set -euo pipefail

KCADM=/opt/keycloak/bin/kcadm.sh

KC_URL="${KC_URL:?KC_URL obrigatório}"
ADMIN_USER="${KEYCLOAK_ADMIN:?KEYCLOAK_ADMIN obrigatório}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD obrigatório}"
REALM="${KEYCLOAK_REALM:-aegis}"
TEST_USER="${CI_TEST_USERNAME:-aegis}"
TEST_PASS="${CI_TEST_PASSWORD:?CI_TEST_PASSWORD obrigatório}"
TEST_EMAIL="${CI_TEST_EMAIL:-aegis-ci@example.com}"
TEST_ROLE="${CI_TEST_REALM_ROLE:-AEGIS_SUPER_ADMIN}"

echo "==> Aguardando Keycloak em ${KC_URL} aceitar login admin..."
for i in $(seq 1 60); do
  if "$KCADM" config credentials --server "$KC_URL" --realm master \
      --user "$ADMIN_USER" --password "$ADMIN_PASS" >/dev/null 2>&1; then
    echo "==> Keycloak pronto (tentativa ${i})."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "ERRO: Keycloak não ficou pronto a tempo." >&2
    exit 1
  fi
  sleep 5
done

echo "==> Aguardando realm '${REALM}' (import automático)..."
for i in $(seq 1 30); do
  if "$KCADM" get "realms/${REALM}" >/dev/null 2>&1; then
    echo "==> Realm '${REALM}' disponível."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "ERRO: realm '${REALM}' não foi importado." >&2
    exit 1
  fi
  sleep 2
done

echo "==> Garantindo usuário de teste '${TEST_USER}'..."
if ! "$KCADM" create users -r "$REALM" \
    -s "username=${TEST_USER}" \
    -s "email=${TEST_EMAIL}" \
    -s "firstName=Aegis" \
    -s "lastName=CI" \
    -s enabled=true \
    -s emailVerified=true 2>/dev/null; then
  echo "    (usuário já existe — seguindo em frente)"
fi

echo "==> Definindo senha (não temporária)..."
"$KCADM" set-password -r "$REALM" --username "$TEST_USER" --new-password "$TEST_PASS"

echo "==> Atribuindo realm role '${TEST_ROLE}'..."
"$KCADM" add-roles -r "$REALM" --uusername "$TEST_USER" --rolename "$TEST_ROLE"

echo "==> Bootstrap de usuários de CI concluído com sucesso."
