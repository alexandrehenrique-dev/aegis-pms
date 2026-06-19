export type AnalyticsKpi = { label: string; value: string; comparison: string; note: string; tone: string };
export type HealthSignal = { label: string; status: string; score: string; tone: string };
export type ChannelRow = { name: string; visits: string; conversion: string; trend: string };

export type ListKpisResponse = AnalyticsKpi[];
export type ListHealthResponse = HealthSignal[];
export type ListChannelsResponse = ChannelRow[];
