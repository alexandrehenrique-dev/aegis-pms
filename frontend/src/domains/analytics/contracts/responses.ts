export type AnalyticsKpi = { label: string; value: string; comparison: string; note: string; tone: string };
export type HealthSignal = { label: string; status: string; score: string; tone: string };
export type ChannelRow = { name: string; visits: string; conversion: string; trend: string };
export type TrendCardResponse = { type: string; text: string; metric: string; severity: string; reviewed: boolean };
export type AnalyticsReport = { name: string; description: string; period: string; format: string; status: string; lastGenerated: string };

export type ListKpisResponse = AnalyticsKpi[];
export type ListHealthResponse = HealthSignal[];
export type ListChannelsResponse = ChannelRow[];
export type ListTrendsResponse = TrendCardResponse[];
export type ListReportsResponse = AnalyticsReport[];
