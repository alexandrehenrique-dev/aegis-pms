export type DashboardSummaryResponse = {
  activeProducts: number;
  archivedProducts: number;
  pendingContent: number;
  pendingContentNeedingReview: number;
  openApprovals: number;
  criticalApprovals: number;
  formsReceived: number;
  formsReceivedToday: number;
  recentAssets: number;
  activeUsers: number;
  productManagers: number;
  conversionRate: string;
};
