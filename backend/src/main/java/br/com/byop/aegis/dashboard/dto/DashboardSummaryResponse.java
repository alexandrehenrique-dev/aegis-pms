package br.com.byop.aegis.dashboard.dto;

/**
 * Resumo agregado do hub do tenant — {@code GET /api/v1/dashboard/summary}.
 * Nenhum campo e persistido; todos sao calculados em tempo real a partir das
 * APIs publicas de {@code product}, {@code content}, {@code asset} e
 * {@code submission} (etapa 17).
 */
public record DashboardSummaryResponse(
        long activeProducts,
        long archivedProducts,
        long pendingContent,
        long pendingContentNeedingReview,
        long openApprovals,
        long criticalApprovals,
        long formsReceived,
        long formsReceivedToday,
        long recentAssets,
        long activeUsers,
        long productManagers,
        String conversionRate
) {
}
