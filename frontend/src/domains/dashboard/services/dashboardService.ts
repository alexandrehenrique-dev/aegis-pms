import { dashboardSummary } from "../mocks/dashboard.mocks";
import type { DashboardSummaryResponse } from "../contracts/responses";

export const dashboardService = {
  async getSummary(): Promise<DashboardSummaryResponse> {
    return dashboardSummary;
  },
};
