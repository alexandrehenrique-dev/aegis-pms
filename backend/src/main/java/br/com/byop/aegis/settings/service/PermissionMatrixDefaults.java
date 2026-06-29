package br.com.byop.aegis.settings.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogo de fabrica da matriz de permissoes, usado por
 * {@code restore-defaults}. As chaves de permissao e o resultado por papel
 * sao copiados literalmente de {@code frontend/src/core/permissions/roles.ts}
 * ({@code roleVisibleNav}/{@code roleBlockedRoutePrefixes}) — nunca uma
 * nomenclatura nova (ADR-0014, etapa 17 do backend). Qualquer papel novo no
 * frontend exige atualizar esta classe na mesma revisao.
 */
final class PermissionMatrixDefaults {

    static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    static final String ROLE_PRODUCT_MANAGER = "PRODUCT_MANAGER";
    static final String ROLE_EDITOR = "EDITOR";
    static final String ROLE_VIEWER = "VIEWER";

    private static final String DASHBOARD = "/dashboard";
    private static final String PRODUCTS = "/products";
    private static final String SETTINGS = "/settings";
    private static final String AUDIT = "/audit";
    private static final String CONTENT = "/content";
    private static final String PAGES = "/pages";
    private static final String ASSETS = "/assets";
    private static final String FORMS = "/forms";
    private static final String ANALYTICS = "/analytics";
    private static final String KNOWLEDGE = "/knowledge";
    private static final String SETTINGS_SECURITY = "/settings/security";
    private static final String SETTINGS_TENANT = "/settings/tenant";
    private static final String USERS = "/users";
    private static final String SETTINGS_PERMISSIONS = "/settings/permissions";
    private static final String SETTINGS_ROLES = "/settings/roles";
    private static final String SETTINGS_ACCESS_PREVIEW = "/settings/access-preview";
    private static final String PRODUCTS_NEW = "/products/new";
    private static final String CONTENT_PUBLISH = "/content/*/publish";
    private static final String CONTENT_EDITOR = "/content/*/editor";
    private static final String PAGES_EDITOR = "/pages/*/editor";
    private static final String FORMS_NEW = "/forms/new";
    private static final String ASSETS_UPLOAD = "/assets/upload";
    private static final String ASSETS_METADATA = "/assets/*/metadata";
    private static final String ASSETS_TAGS = "/assets/*/tags";
    private static final String CONTENT_WORKFLOW = "/content/*/workflow";
    private static final String KNOWLEDGE_GRAPH = "/knowledge/graph";
    private static final String KNOWLEDGE_RELATIONSHIPS = "/knowledge/relationships";
    private static final String KNOWLEDGE_ENTITIES = "/knowledge/entities";
    private static final String KNOWLEDGE_SEARCH = "/knowledge/search";
    private static final String KNOWLEDGE_ORPHANS = "/knowledge/orphans";
    private static final String KNOWLEDGE_INSIGHTS = "/knowledge/insights";

    static final List<String> CANONICAL_ROLES = List.of(
            ROLE_SUPER_ADMIN, ROLE_TENANT_ADMIN, ROLE_PRODUCT_MANAGER, ROLE_EDITOR, ROLE_VIEWER
    );

    static final List<String> PERMISSION_KEYS = List.of(
            DASHBOARD, PRODUCTS, SETTINGS, AUDIT, CONTENT, PAGES, ASSETS, FORMS,
            ANALYTICS, KNOWLEDGE, SETTINGS_SECURITY, SETTINGS_TENANT, USERS,
            SETTINGS_PERMISSIONS, SETTINGS_ROLES, SETTINGS_ACCESS_PREVIEW, PRODUCTS_NEW,
            CONTENT_PUBLISH, CONTENT_EDITOR, PAGES_EDITOR, FORMS_NEW, ASSETS_UPLOAD,
            ASSETS_METADATA, ASSETS_TAGS, CONTENT_WORKFLOW, KNOWLEDGE_GRAPH,
            KNOWLEDGE_RELATIONSHIPS, KNOWLEDGE_ENTITIES, KNOWLEDGE_SEARCH, KNOWLEDGE_ORPHANS,
            KNOWLEDGE_INSIGHTS
    );

    private static final Map<String, Set<String>> ALLOWED_KEYS_BY_ROLE = Map.of(
            ROLE_SUPER_ADMIN, Set.of(
                    DASHBOARD, PRODUCTS, SETTINGS, AUDIT, SETTINGS_SECURITY, SETTINGS_TENANT,
                    USERS, SETTINGS_PERMISSIONS, SETTINGS_ROLES, SETTINGS_ACCESS_PREVIEW, PRODUCTS_NEW
            ),
            ROLE_TENANT_ADMIN, Set.of(
                    DASHBOARD, PRODUCTS, SETTINGS, AUDIT, CONTENT, PAGES, ASSETS, FORMS,
                    ANALYTICS, KNOWLEDGE, SETTINGS_TENANT, USERS, SETTINGS_PERMISSIONS,
                    SETTINGS_ROLES, SETTINGS_ACCESS_PREVIEW, PRODUCTS_NEW, CONTENT_PUBLISH,
                    CONTENT_EDITOR, PAGES_EDITOR, FORMS_NEW, ASSETS_UPLOAD, ASSETS_METADATA,
                    ASSETS_TAGS, CONTENT_WORKFLOW, KNOWLEDGE_GRAPH, KNOWLEDGE_RELATIONSHIPS,
                    KNOWLEDGE_ENTITIES, KNOWLEDGE_SEARCH, KNOWLEDGE_ORPHANS, KNOWLEDGE_INSIGHTS
            ),
            ROLE_PRODUCT_MANAGER, Set.of(
                    DASHBOARD, PRODUCTS, SETTINGS, CONTENT, PAGES, ASSETS, FORMS, ANALYTICS,
                    KNOWLEDGE, CONTENT_EDITOR, PAGES_EDITOR, FORMS_NEW, ASSETS_UPLOAD,
                    ASSETS_METADATA, ASSETS_TAGS, CONTENT_WORKFLOW, KNOWLEDGE_GRAPH,
                    KNOWLEDGE_RELATIONSHIPS, KNOWLEDGE_ENTITIES, KNOWLEDGE_SEARCH, KNOWLEDGE_ORPHANS,
                    KNOWLEDGE_INSIGHTS
            ),
            ROLE_EDITOR, Set.of(
                    DASHBOARD, PRODUCTS, CONTENT, PAGES, ASSETS, FORMS, ANALYTICS,
                    CONTENT_EDITOR, PAGES_EDITOR, FORMS_NEW, ASSETS_UPLOAD, ASSETS_METADATA,
                    ASSETS_TAGS, CONTENT_WORKFLOW, CONTENT_PUBLISH
            ),
            ROLE_VIEWER, Set.of(
                    DASHBOARD, PRODUCTS, CONTENT, PAGES, ANALYTICS, CONTENT_PUBLISH
            )
    );

    private PermissionMatrixDefaults() {
    }

    static boolean isAllowedByDefault(String role, String permissionKey) {
        return ALLOWED_KEYS_BY_ROLE.getOrDefault(role, Set.of()).contains(permissionKey);
    }
}
