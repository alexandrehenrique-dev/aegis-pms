import { kpis, health, channels } from "../mocks/analytics.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import type { AnalyticsKpi, ChannelRow, HealthSignal, ListChannelsResponse, ListHealthResponse, ListKpisResponse } from "../contracts/responses";

const kpisStore: AnalyticsKpi[] = kpis.map(([label, value, comparison, note, tone]) => ({ label, value, comparison, note, tone }));
const healthStore: HealthSignal[] = health.map(([label, status, score, tone]) => ({ label, status, score, tone }));
const channelsStore: ChannelRow[] = channels.map(([name, visits, conversion, trend]) => ({ name, visits, conversion, trend }));

function warnMissingEndpoint(method: string, path: string) {
  if (!import.meta.env.PROD) console.warn(`analyticsService: backend ainda não expõe ${method} ${path}; mantendo ação em mock local.`);
}

export const analyticsService = {
  async listKpis(productId: string): Promise<ListKpisResponse> {
    if (IS_API_MODE) return apiClient.get<ListKpisResponse>(`/products/${productId}/analytics/kpis`);
    return kpisStore;
  },
  async listHealth(productId: string): Promise<ListHealthResponse> {
    if (IS_API_MODE) return apiClient.get<ListHealthResponse>(`/products/${productId}/analytics/health`);
    return healthStore;
  },
  async listChannels(productId: string): Promise<ListChannelsResponse> {
    if (IS_API_MODE) return apiClient.get<ListChannelsResponse>(`/products/${productId}/analytics/channels`);
    return channelsStore;
  },
  async generateReport(productId: string, name: string): Promise<void> {
    if (IS_API_MODE) warnMissingEndpoint("POST", `/api/v1/products/${productId}/analytics/reports`);
    logApiCall("POST", `/api/v1/products/${productId}/analytics/reports`, { name });
  },
  async markTrendReviewed(productId: string, label: string): Promise<void> {
    if (IS_API_MODE) warnMissingEndpoint("POST", `/api/v1/products/${productId}/analytics/trends/review`);
    logApiCall("POST", `/api/v1/products/${productId}/analytics/trends/review`, { label });
  },
  async generateActionPlan(productId: string): Promise<void> {
    if (IS_API_MODE) warnMissingEndpoint("POST", `/api/v1/products/${productId}/analytics/action-plan`);
    logApiCall("POST", `/api/v1/products/${productId}/analytics/action-plan`);
  },
};
