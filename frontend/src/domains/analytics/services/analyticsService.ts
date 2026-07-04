import { kpis, health, channels } from "../mocks/analytics.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import { IS_API_MODE } from "../../../infra/apiMode";
import { apiClient } from "../../../shared/services/apiClient";
import { requireCurrentProductId } from "../../../core/products/currentProductContext";
import type { AnalyticsKpi, ChannelRow, HealthSignal, ListChannelsResponse, ListHealthResponse, ListKpisResponse } from "../contracts/responses";

const kpisStore: AnalyticsKpi[] = kpis.map(([label, value, comparison, note, tone]) => ({ label, value, comparison, note, tone }));
const healthStore: HealthSignal[] = health.map(([label, status, score, tone]) => ({ label, status, score, tone }));
const channelsStore: ChannelRow[] = channels.map(([name, visits, conversion, trend]) => ({ name, visits, conversion, trend }));

export const analyticsService = {
  async listKpis(): Promise<ListKpisResponse> {
    if (IS_API_MODE) return apiClient.get<ListKpisResponse>(`/products/${requireCurrentProductId()}/analytics/kpis`);
    return kpisStore;
  },
  async listHealth(): Promise<ListHealthResponse> {
    if (IS_API_MODE) return apiClient.get<ListHealthResponse>(`/products/${requireCurrentProductId()}/analytics/health`);
    return healthStore;
  },
  async listChannels(): Promise<ListChannelsResponse> {
    if (IS_API_MODE) return apiClient.get<ListChannelsResponse>(`/products/${requireCurrentProductId()}/analytics/channels`);
    return channelsStore;
  },
  async generateReport(name: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${requireCurrentProductId()}/analytics/reports`, { name });
    logApiCall("POST", "/api/v1/products/{productId}/analytics/reports", { name });
  },
  async markTrendReviewed(label: string): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${requireCurrentProductId()}/analytics/trends/review`, { label });
    logApiCall("POST", "/api/v1/products/{productId}/analytics/trends/review", { label });
  },
  async generateActionPlan(): Promise<void> {
    if (IS_API_MODE) return apiClient.post(`/products/${requireCurrentProductId()}/analytics/action-plan`);
    logApiCall("POST", "/api/v1/products/{productId}/analytics/action-plan");
  },
};
