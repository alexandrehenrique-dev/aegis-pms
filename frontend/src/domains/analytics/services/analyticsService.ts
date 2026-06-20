import { kpis, health, channels } from "../mocks/analytics.mocks";
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
  async generateReport(_name: string): Promise<void> {
    void _name;
  },
  async markTrendReviewed(_label: string): Promise<void> {
    void _label;
  },
  async generateActionPlan(): Promise<void> {},
};
