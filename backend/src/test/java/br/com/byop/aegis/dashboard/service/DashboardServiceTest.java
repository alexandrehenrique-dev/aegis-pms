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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final UUID FIRST_PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ProductVisibilityService productVisibilityService;

    @Mock
    private ProductUserAccessService productUserAccessService;

    @Mock
    private ContentAnalyticsService contentAnalyticsService;

    @Mock
    private AssetAnalyticsService assetAnalyticsService;

    @Mock
    private SubmissionAnalyticsService submissionAnalyticsService;

    @InjectMocks
    private DashboardService service;

    @Test
    void shouldAggregateAcrossVisibleProducts() {
        AuthenticatedUser caller = new AuthenticatedUser("user-1", "user@aegis.app", "user", "User", Set.of("ROLE_TENANT_ADMIN"));
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of(
                new ProductAccessScope(FIRST_PRODUCT_ID, TENANT_ID, "ACTIVE"),
                new ProductAccessScope(SECOND_PRODUCT_ID, TENANT_ID, "ARCHIVED")
        ));
        when(contentAnalyticsService.summarize(FIRST_PRODUCT_ID))
                .thenReturn(new ContentAnalyticsResponse(8, 5, 2, 1, 1, null));
        when(contentAnalyticsService.summarize(SECOND_PRODUCT_ID))
                .thenReturn(new ContentAnalyticsResponse(3, 2, 1, 0, 0, null));
        when(submissionAnalyticsService.summarize(FIRST_PRODUCT_ID))
                .thenReturn(new SubmissionAnalyticsResponse(10, 4, null));
        when(submissionAnalyticsService.summarize(SECOND_PRODUCT_ID))
                .thenReturn(new SubmissionAnalyticsResponse(2, 0, null));
        when(submissionAnalyticsService.countToday(FIRST_PRODUCT_ID)).thenReturn(3L);
        when(submissionAnalyticsService.countToday(SECOND_PRODUCT_ID)).thenReturn(0L);
        when(assetAnalyticsService.summarize(FIRST_PRODUCT_ID))
                .thenReturn(new AssetAnalyticsResponse(9, 3, 2, null));
        when(assetAnalyticsService.summarize(SECOND_PRODUCT_ID))
                .thenReturn(new AssetAnalyticsResponse(1, 0, 0, null));
        when(productUserAccessService.countDistinctAssignedUsers(List.of(FIRST_PRODUCT_ID, SECOND_PRODUCT_ID)))
                .thenReturn(7L);
        when(productUserAccessService.countDistinctProductManagers(List.of(FIRST_PRODUCT_ID, SECOND_PRODUCT_ID)))
                .thenReturn(2L);

        DashboardSummaryResponse summary = service.getSummary(caller);

        assertThat(summary.activeProducts()).isEqualTo(1);
        assertThat(summary.archivedProducts()).isEqualTo(1);
        assertThat(summary.pendingContent()).isEqualTo(1);
        assertThat(summary.pendingContentNeedingReview()).isEqualTo(3);
        assertThat(summary.openApprovals()).isEqualTo(3);
        assertThat(summary.criticalApprovals()).isEqualTo(1);
        assertThat(summary.formsReceived()).isEqualTo(12);
        assertThat(summary.formsReceivedToday()).isEqualTo(3);
        assertThat(summary.recentAssets()).isEqualTo(3);
        assertThat(summary.activeUsers()).isEqualTo(7);
        assertThat(summary.productManagers()).isEqualTo(2);
        assertThat(summary.conversionRate()).isEqualTo("0%");
    }

    @Test
    void shouldReturnZeroedSummaryWhenCallerHasNoVisibleProducts() {
        AuthenticatedUser caller = new AuthenticatedUser("user-1", "user@aegis.app", "user", "User", Set.of("ROLE_VIEWER"));
        when(productVisibilityService.listVisibleProducts(caller)).thenReturn(List.of());
        when(productUserAccessService.countDistinctAssignedUsers(List.of())).thenReturn(0L);
        when(productUserAccessService.countDistinctProductManagers(List.of())).thenReturn(0L);

        DashboardSummaryResponse summary = service.getSummary(caller);

        assertThat(summary.activeProducts()).isZero();
        assertThat(summary.archivedProducts()).isZero();
        assertThat(summary.pendingContent()).isZero();
        assertThat(summary.formsReceived()).isZero();
        assertThat(summary.activeUsers()).isZero();
    }
}
