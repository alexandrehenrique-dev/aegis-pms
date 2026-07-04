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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
    private final ContentAnalyticsService contentAnalyticsService;
    private final AssetAnalyticsService assetAnalyticsService;
    private final SubmissionAnalyticsService submissionAnalyticsService;

    public DashboardService(ProductVisibilityService productVisibilityService,
                            ProductUserAccessService productUserAccessService,
                            ContentAnalyticsService contentAnalyticsService,
                            AssetAnalyticsService assetAnalyticsService,
                            SubmissionAnalyticsService submissionAnalyticsService) {
        this.productVisibilityService = productVisibilityService;
        this.productUserAccessService = productUserAccessService;
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

        long activeUsers = productUserAccessService.countDistinctAssignedUsers(productIds);
        long productManagers = productUserAccessService.countDistinctProductManagers(productIds);

        return new DashboardSummaryResponse(
                activeProducts, archivedProducts, pendingContent, pendingContentNeedingReview,
                pendingContentNeedingReview, criticalApprovals, formsReceived, formsReceivedToday,
                recentAssets, activeUsers, productManagers, CONVERSION_RATE_UNAVAILABLE
        );
    }
}
