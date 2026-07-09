package br.com.byop.aegis.dashboard.service;

import br.com.byop.aegis.asset.api.AssetAnalyticsResponse;
import br.com.byop.aegis.asset.api.AssetAnalyticsService;
import br.com.byop.aegis.content.api.ContentAnalyticsResponse;
import br.com.byop.aegis.content.api.ContentAnalyticsService;
import br.com.byop.aegis.dashboard.dto.DashboardSummaryResponse;
import br.com.byop.aegis.product.api.ProductAccessScope;
import br.com.byop.aegis.product.api.ProductUserAccessService;
import br.com.byop.aegis.product.api.ProductVisibilityService;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.submission.api.SubmissionAnalyticsResponse;
import br.com.byop.aegis.submission.api.SubmissionAnalyticsService;
import br.com.byop.aegis.tenant.api.TenantUserAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agregador puro do hub do tenant — nenhuma tabela/cache propria (etapa 17).
 * Reaproveita exatamente as mesmas APIs publicas ja consumidas pela etapa 14
 * (analytics): {@code content.api}, {@code asset.api} e
 * {@code submission.api}. O escopo de produtos visiveis ao chamador segue a
 * mesma regra de {@code ProductService#listProducts} (ADR-0019), via
 * {@link ProductVisibilityService} — SUPER_ADMIN agrega globalmente
 * (decisao explicita da propria etapa), os demais papeis agregam apenas os
 * produtos que conseguem ver.
 */
@Slf4j
@Service
public class DashboardService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String CONVERSION_RATE_UNAVAILABLE = "0%";

    private final ProductVisibilityService productVisibilityService;
    private final ProductUserAccessService productUserAccessService;
    private final TenantUserAccessService tenantUserAccessService;
    private final ContentAnalyticsService contentAnalyticsService;
    private final AssetAnalyticsService assetAnalyticsService;
    private final SubmissionAnalyticsService submissionAnalyticsService;

    public DashboardService(ProductVisibilityService productVisibilityService,
                            ProductUserAccessService productUserAccessService,
                            TenantUserAccessService tenantUserAccessService,
                            ContentAnalyticsService contentAnalyticsService,
                            AssetAnalyticsService assetAnalyticsService,
                            SubmissionAnalyticsService submissionAnalyticsService) {
        this.productVisibilityService = productVisibilityService;
        this.productUserAccessService = productUserAccessService;
        this.tenantUserAccessService = tenantUserAccessService;
        this.contentAnalyticsService = contentAnalyticsService;
        this.assetAnalyticsService = assetAnalyticsService;
        this.submissionAnalyticsService = submissionAnalyticsService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(AuthenticatedUser caller) {
        log.debug("getSummary: caller='{}'", caller.subject());
        List<ProductAccessScope> scopes = productVisibilityService.listVisibleProducts(caller);
        List<UUID> productIds = scopes.stream().map(ProductAccessScope::productId).toList();

        long activeProducts = scopes.stream().filter(scope -> STATUS_ACTIVE.equals(scope.status())).count();
        long archivedProducts = scopes.stream().filter(scope -> STATUS_ARCHIVED.equals(scope.status())).count();

        long pendingContent = 0;
        long pendingContentNeedingReview = 0;
        long criticalApprovals = 0;
        long formsReceived = 0;
        long formsReceivedToday = 0;
        long recentAssets = 0;

        for (UUID productId : productIds) {
            ContentAnalyticsResponse content = contentAnalyticsService.summarize(productId);
            pendingContent += content.drafts();
            pendingContentNeedingReview += content.pendingReview();
            criticalApprovals += content.stalePendingReview();

            SubmissionAnalyticsResponse submissions = submissionAnalyticsService.summarize(productId);
            formsReceived += submissions.total();
            formsReceivedToday += submissionAnalyticsService.countToday(productId);

            AssetAnalyticsResponse assets = assetAnalyticsService.summarize(productId);
            recentAssets += assets.recent();
        }

        ActiveProductUsers activeProductUsers = countActiveProductUsers(scopes);
        long activeUsers = activeProductUsers.assignedUsers();
        long productManagers = activeProductUsers.productManagers();

        return new DashboardSummaryResponse(
                activeProducts, archivedProducts, pendingContent, pendingContentNeedingReview,
                pendingContentNeedingReview, criticalApprovals, formsReceived, formsReceivedToday,
                recentAssets, activeUsers, productManagers, CONVERSION_RATE_UNAVAILABLE
        );
    }

    private ActiveProductUsers countActiveProductUsers(List<ProductAccessScope> scopes) {
        Map<UUID, List<UUID>> productIdsByTenant = scopes.stream()
                .collect(Collectors.groupingBy(ProductAccessScope::tenantId,
                        Collectors.mapping(ProductAccessScope::productId, Collectors.toList())));
        Set<String> activeAssignedUsers = new HashSet<>();
        Set<String> activeProductManagers = new HashSet<>();
        productIdsByTenant.forEach((tenantId, tenantProductIds) -> {
            Set<String> activeTenantSubjects = Set.copyOf(tenantUserAccessService.listActiveUserSubjects(tenantId));
            productUserAccessService.listDistinctAssignedUserSubjects(tenantProductIds)
                    .stream()
                    .filter(activeTenantSubjects::contains)
                    .forEach(activeAssignedUsers::add);
            productUserAccessService.listDistinctProductManagerSubjects(tenantProductIds)
                    .stream()
                    .filter(activeTenantSubjects::contains)
                    .forEach(activeProductManagers::add);
        });
        return new ActiveProductUsers(activeAssignedUsers.size(), activeProductManagers.size());
    }

    private record ActiveProductUsers(long assignedUsers, long productManagers) {
    }
}
