package br.com.byop.aegis.tenant.api;

import java.util.UUID;

public record TenantReference(UUID tenantId, String name) {
}
