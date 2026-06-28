import { dashboardSummary } from "../mocks/dashboard.mocks";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { DashboardSummaryResponse } from "../contracts/responses";

export const dashboardService = {
  async getSummary(): Promise<DashboardSummaryResponse> {
    if (IS_API_MODE) return apiClient.get<DashboardSummaryResponse>("/dashboard/summary");
    return dashboardSummary;
  },
};
