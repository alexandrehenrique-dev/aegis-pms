package br.com.byop.aegis.settings.dto;

import java.util.Map;

/**
 * Uma linha da matriz de permissoes: um papel canonico (ADR-0014) e o mapa
 * {@code permissionKey -> allowed} para todas as chaves conhecidas. Usado
 * como o shape de resposta de {@code GET .../roles}, {@code GET
 * .../permission-matrix} e {@code POST .../permission-matrix/preview} — as
 * tres telas do frontend consomem a mesma forma de dado (etapa 17).
 */
public record RoleMatrixEntry(
        String role,
        Map<String, Boolean> permissions
) {
}
