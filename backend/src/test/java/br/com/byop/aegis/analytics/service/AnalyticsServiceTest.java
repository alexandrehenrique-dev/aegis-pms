package br.com.byop.aegis.analytics.service;

import br.com.byop.aegis.analytics.dto.AnalyticsKpiResponse;
import br.com.byop.aegis.analytics.dto.AnalyticsReportResponse;
import br.com.byop.aegis.analytics.dto.ChannelRowResponse;
import br.com.byop.aegis.analytics.dto.HealthSignalResponse;
import br.com.byop.aegis.analytics.dto.TrendCardResponse;
import br.com.byop.aegis.asset.api.AssetAnalyticsResponse;
import br.com.byop.aegis.asset.api.AssetAnalyticsService;
import br.com.byop.aegis.content.api.ContentAnalyticsResponse;
import br.com.byop.aegis.content.api.ContentAnalyticsService;
import br.com.byop.aegis.submission.api.SubmissionAnalyticsResponse;
import br.com.byop.aegis.submission.api.SubmissionAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-27T12:00:00Z");
    private static final String SIMULATED_TRAFFIC = "simulado";

    private ContentAnalyticsService contentAnalyticsService;
    private SubmissionAnalyticsService submissionAnalyticsService;
    private AssetAnalyticsService assetAnalyticsService;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        contentAnalyticsService = mock(ContentAnalyticsService.class);
        submissionAnalyticsService = mock(SubmissionAnalyticsService.class);
        assetAnalyticsService = mock(AssetAnalyticsService.class);
        service = new AnalyticsService(contentAnalyticsService, submissionAnalyticsService, assetAnalyticsService);
    }

    @Test
    void shouldBuildKpisFromRealDomainSnapshots() {
        givenSnapshots(
                new ContentAnalyticsResponse(8, 5, 2, 1, 1, NOW),
                new SubmissionAnalyticsResponse(12, 4, NOW),
                new AssetAnalyticsResponse(9, 3, 2, NOW)
        );

        List<AnalyticsKpiResponse> kpis = service.listKpis(PRODUCT_ID);

        assertThat(kpis).containsExactly(
                new AnalyticsKpiResponse("Conteúdos publicados", "5", "2 em revisão",
                        "Conteúdo publicado a partir do workflow editorial.", "positivo"),
                new AnalyticsKpiResponse("Conteúdos pendentes", "3", "1 há mais de 30 dias",
                        "Conteúdo pendente considera drafts e itens em revisão.", "atenção"),
                new AnalyticsKpiResponse("Formulários recebidos", "12", "4 nos últimos 14 dias",
                        "Submissions agregadas dos formulários do produto.", "positivo"),
                new AnalyticsKpiResponse("Assets recentes", "3", "9 no total",
                        "Assets recentes consideram uploads dos últimos 30 dias.", "positivo")
        );
    }

    @Test
    void shouldBuildNeutralKpisForNewProductWithoutData() {
        givenSnapshots(
                new ContentAnalyticsResponse(0, 0, 0, 0, 0, null),
                new SubmissionAnalyticsResponse(0, 0, null),
                new AssetAnalyticsResponse(0, 0, 0, null)
        );

        List<AnalyticsKpiResponse> kpis = service.listKpis(PRODUCT_ID);

        assertThat(kpis).containsExactly(
                new AnalyticsKpiResponse("Conteúdos publicados", "0", "sem pendências",
                        "Nenhum conteúdo publicado ainda.", "neutro"),
                new AnalyticsKpiResponse("Conteúdos pendentes", "0", "sem revisão vencida",
                        "Não há conteúdo pendente.", "positivo"),
                new AnalyticsKpiResponse("Formulários recebidos", "0", "0 nos últimos 14 dias",
                        "Nenhuma submission recebida ainda.", "neutro"),
                new AnalyticsKpiResponse("Assets recentes", "0", "0 no total",
                        "Nenhum asset cadastrado ainda.", "neutro")
        );
    }

    @Test
    void shouldBuildPositiveHealthWhenAllDomainsAreHealthy() {
        givenSnapshots(
                new ContentAnalyticsResponse(3, 3, 0, 0, 0, NOW),
                new SubmissionAnalyticsResponse(5, 2, NOW),
                new AssetAnalyticsResponse(4, 2, 0, NOW)
        );

        List<HealthSignalResponse> health = service.listHealth(PRODUCT_ID);

        assertThat(health).containsExactly(
                new HealthSignalResponse("Saúde geral", "Produto saudável", "92", "positivo"),
                new HealthSignalResponse("Conteúdo", "Operando bem", "95", "positivo"),
                new HealthSignalResponse("Forms", "Operando bem", "90", "positivo"),
                new HealthSignalResponse("Assets", "Operando bem", "92", "positivo")
        );
    }

    @Test
    void shouldBuildAttentionHealthForStaleContentInactiveFormsAndAssetGaps() {
        givenSnapshots(
                new ContentAnalyticsResponse(4, 1, 2, 1, 1, NOW),
                new SubmissionAnalyticsResponse(8, 0, NOW.minusDays(20)),
                new AssetAnalyticsResponse(5, 1, 2, NOW)
        );

        List<HealthSignalResponse> health = service.listHealth(PRODUCT_ID);

        assertThat(health).containsExactly(
                new HealthSignalResponse("Saúde geral", "Requer atenção", "67", "atenção"),
                new HealthSignalResponse("Conteúdo", "Revisão atrasada", "60", "atenção"),
                new HealthSignalResponse("Forms", "Sem atividade recente", "72", "atenção"),
                new HealthSignalResponse("Assets", "Alt text pendente", "70", "atenção")
        );
    }

    @Test
    void shouldBuildPendingContentHealthWithoutStaleReview() {
        givenSnapshots(
                new ContentAnalyticsResponse(2, 1, 1, 0, 0, NOW),
                new SubmissionAnalyticsResponse(1, 1, NOW),
                new AssetAnalyticsResponse(1, 1, 0, NOW)
        );

        List<HealthSignalResponse> health = service.listHealth(PRODUCT_ID);

        assertThat(health.get(1)).isEqualTo(new HealthSignalResponse("Conteúdo", "Pendências leves", "78", "atenção"));
    }

    @Test
    void shouldBuildNeutralHealthForNewProduct() {
        givenSnapshots(
                new ContentAnalyticsResponse(0, 0, 0, 0, 0, null),
                new SubmissionAnalyticsResponse(0, 0, null),
                new AssetAnalyticsResponse(0, 0, 0, null)
        );

        List<HealthSignalResponse> health = service.listHealth(PRODUCT_ID);

        assertThat(health).containsExactly(
                new HealthSignalResponse("Saúde geral", "Dados insuficientes", "0", "neutro"),
                new HealthSignalResponse("Conteúdo", "Sem conteúdo", "0", "neutro"),
                new HealthSignalResponse("Forms", "Sem submissions", "0", "neutro"),
                new HealthSignalResponse("Assets", "Sem assets", "0", "neutro")
        );
    }

    @Test
    void shouldReturnStaticSimulatedChannels() {
        List<ChannelRowResponse> channels = service.listChannels(PRODUCT_ID);

        assertThat(channels)
                .hasSize(5)
                .allMatch(channel -> SIMULATED_TRAFFIC.equals(channel.trend()))
                .first()
                .isEqualTo(new ChannelRowResponse("Direto", "0", "0%", SIMULATED_TRAFFIC));
    }

    @Test
    void shouldBuildTrendsFromSnapshotsWithAttentionSignals() {
        givenSnapshots(
                new ContentAnalyticsResponse(4, 1, 2, 1, 2, NOW),
                new SubmissionAnalyticsResponse(8, 3, NOW),
                new AssetAnalyticsResponse(5, 1, 4, NOW)
        );

        List<TrendCardResponse> trends = service.listTrends(PRODUCT_ID);

        assertThat(trends).containsExactly(
                new TrendCardResponse("Crescimento", "Formulários receberam submissions recentes.", "+3", "positivo", false),
                new TrendCardResponse("Atenção", "Conteúdo editorial possui revisão há mais de 30 dias.", "2 pendentes", "alta", false),
                new TrendCardResponse("Oportunidade", "Assets sem alt text podem prejudicar SEO e acessibilidade.", "4 assets", "alta", false),
                new TrendCardResponse("Estabilidade", "Canais aguardam instrumentação real de tráfego.", "simulado", "neutro", false)
        );
    }

    @Test
    void shouldBuildNeutralTrendsWithoutAttentionSignals() {
        givenSnapshots(
                new ContentAnalyticsResponse(1, 1, 0, 0, 0, NOW),
                new SubmissionAnalyticsResponse(1, 0, NOW.minusDays(20)),
                new AssetAnalyticsResponse(1, 1, 0, NOW)
        );

        List<TrendCardResponse> trends = service.listTrends(PRODUCT_ID);

        assertThat(trends).containsExactly(
                new TrendCardResponse("Crescimento", "Formulários ainda não receberam submissions recentes.", "0", "positivo", false),
                new TrendCardResponse("Atenção", "Conteúdo editorial não possui revisão vencida.", "0", "neutro", false),
                new TrendCardResponse("Oportunidade", "Assets estão sem pendência de alt text.", "0 assets", "neutro", false),
                new TrendCardResponse("Estabilidade", "Canais aguardam instrumentação real de tráfego.", "simulado", "neutro", false)
        );
    }

    @Test
    void shouldBuildReportsWithEnoughData() {
        givenSnapshots(
                new ContentAnalyticsResponse(2, 2, 0, 0, 0, NOW),
                new SubmissionAnalyticsResponse(3, 1, NOW),
                new AssetAnalyticsResponse(4, 1, 0, NOW)
        );

        List<AnalyticsReportResponse> reports = service.listReports(PRODUCT_ID);

        assertThat(reports).containsExactly(
                new AnalyticsReportResponse("Relatório mensal do produto", "Resumo executivo-operacional do produto.",
                        "mês atual", "PDF", "pronto", "—"),
                new AnalyticsReportResponse("Performance de conteúdo", "Publicações, pendências e revisão editorial.",
                        "30 dias", "CSV", "pronto", "—"),
                new AnalyticsReportResponse("Conversões de formulários", "Submissions recebidas e atividade recente.",
                        "14 dias", "XLSX", "pronto", "—"),
                new AnalyticsReportResponse("Saúde do produto", "Pendências por módulo.",
                        "atual", "PDF", "pronto", "—"),
                new AnalyticsReportResponse("SEO e acessibilidade", "Alt text, metas e oportunidades.",
                        "30 dias", "PDF", "pronto", "—")
        );
    }

    @Test
    void shouldBuildReportsWithoutEnoughData() {
        givenSnapshots(
                new ContentAnalyticsResponse(0, 0, 0, 0, 0, null),
                new SubmissionAnalyticsResponse(0, 0, null),
                new AssetAnalyticsResponse(0, 0, 0, null)
        );

        List<AnalyticsReportResponse> reports = service.listReports(PRODUCT_ID);

        assertThat(reports)
                .extracting(AnalyticsReportResponse::status)
                .containsExactly("sem dados suficientes", "sem dados suficientes",
                        "sem dados suficientes", "pronto", "sem dados suficientes");
    }

    private void givenSnapshots(ContentAnalyticsResponse content, SubmissionAnalyticsResponse submissions,
                                AssetAnalyticsResponse assets) {
        when(contentAnalyticsService.summarize(PRODUCT_ID)).thenReturn(content);
        when(submissionAnalyticsService.summarize(PRODUCT_ID)).thenReturn(submissions);
        when(assetAnalyticsService.summarize(PRODUCT_ID)).thenReturn(assets);
    }
}
