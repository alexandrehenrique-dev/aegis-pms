import { kpis, health, channels } from "../mocks/analytics.mocks";
import { logApiCall } from "../../../shared/services/devLog";
import type { AnalyticsKpi, ChannelRow, HealthSignal, ListChannelsResponse, ListHealthResponse, ListKpisResponse } from "../contracts/responses";

const kpisStore: AnalyticsKpi[] = kpis.map(([label, value, comparison, note, tone]) => ({ label, value, comparison, note, tone }));
const healthStore: HealthSignal[] = health.map(([label, status, score, tone]) => ({ label, status, score, tone }));
const channelsStore: ChannelRow[] = channels.map(([name, visits, conversion, trend]) => ({ name, visits, conversion, trend }));

export const analyticsService = {
  async listKpis(): Promise<ListKpisResponse> {
    return kpisStore;
  },
  async listHealth(): Promise<ListHealthResponse> {
    return healthStore;
  },
  async listChannels(): Promise<ListChannelsResponse> {
    return channelsStore;
  },
  async generateReport(name: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/analytics/reports", { name });
  },
  async markTrendReviewed(label: string): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/analytics/trends/review", { label });
  },
  async generateActionPlan(): Promise<void> {
    logApiCall("POST", "/api/v1/products/{productId}/analytics/action-plan");
  },
};
