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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private static final String POSITIVE = "positivo";
    private static final String ATTENTION = "atenção";
    private static final String NEUTRAL = "neutro";
    private static final String HIGH = "alta";
    private static final String ASSETS_LABEL = "Assets";
    private static final String CONTENT_LABEL = "Conteúdo";
    private static final String FORMS_LABEL = "Forms";
    private static final String OPERATING_WELL_STATUS = "Operando bem";
    private static final String SIMULATED_TRAFFIC = "simulado";

    private final ContentAnalyticsService contentAnalyticsService;
    private final SubmissionAnalyticsService submissionAnalyticsService;
    private final AssetAnalyticsService assetAnalyticsService;

    public AnalyticsService(ContentAnalyticsService contentAnalyticsService,
                            SubmissionAnalyticsService submissionAnalyticsService,
                            AssetAnalyticsService assetAnalyticsService) {
        this.contentAnalyticsService = contentAnalyticsService;
        this.submissionAnalyticsService = submissionAnalyticsService;
        this.assetAnalyticsService = assetAnalyticsService;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsKpiResponse> listKpis(UUID productId) {
        ContentAnalyticsResponse content = contentAnalyticsService.summarize(productId);
        SubmissionAnalyticsResponse submissions = submissionAnalyticsService.summarize(productId);
        AssetAnalyticsResponse assets = assetAnalyticsService.summarize(productId);
        return List.of(
                publishedContentKpi(content),
                pendingContentKpi(content),
                submissionsKpi(submissions),
                recentAssetsKpi(assets)
        );
    }

    @Transactional(readOnly = true)
    public List<HealthSignalResponse> listHealth(UUID productId) {
        ContentAnalyticsResponse content = contentAnalyticsService.summarize(productId);
        SubmissionAnalyticsResponse submissions = submissionAnalyticsService.summarize(productId);
        AssetAnalyticsResponse assets = assetAnalyticsService.summarize(productId);
        HealthSignalResponse contentHealth = contentHealth(content);
        HealthSignalResponse formsHealth = formsHealth(submissions);
        HealthSignalResponse assetsHealth = assetsHealth(assets);
        HealthSignalResponse generalHealth = generalHealth(contentHealth, formsHealth, assetsHealth);
        return List.of(generalHealth, contentHealth, formsHealth, assetsHealth);
    }

    @Transactional(readOnly = true)
    public List<ChannelRowResponse> listChannels(UUID productId) {
        return List.of(
                new ChannelRowResponse("Direto", "0", "0%", SIMULATED_TRAFFIC),
                new ChannelRowResponse("Google", "0", "0%", SIMULATED_TRAFFIC),
                new ChannelRowResponse("Instagram", "0", "0%", SIMULATED_TRAFFIC),
                new ChannelRowResponse("WhatsApp", "0", "0%", SIMULATED_TRAFFIC),
                new ChannelRowResponse("Referral", "0", "0%", SIMULATED_TRAFFIC)
        );
    }

    @Transactional(readOnly = true)
    public List<TrendCardResponse> listTrends(UUID productId) {
        ContentAnalyticsResponse content = contentAnalyticsService.summarize(productId);
        SubmissionAnalyticsResponse submissions = submissionAnalyticsService.summarize(productId);
        AssetAnalyticsResponse assets = assetAnalyticsService.summarize(productId);
        return List.of(
                new TrendCardResponse("Crescimento", submissionsTrendText(submissions), signed(submissions.recent()), POSITIVE, false),
                new TrendCardResponse("Atenção", contentTrendText(content), metricDaysOrCount(content.stalePendingReview()), severity(content.stalePendingReview()), false),
                new TrendCardResponse("Oportunidade", assetTrendText(assets), assets.missingAltText() + " assets", severity(assets.missingAltText()), false),
                new TrendCardResponse("Estabilidade", "Canais aguardam instrumentação real de tráfego.", SIMULATED_TRAFFIC, NEUTRAL, false)
        );
    }

    @Transactional(readOnly = true)
    public List<AnalyticsReportResponse> listReports(UUID productId) {
        ContentAnalyticsResponse content = contentAnalyticsService.summarize(productId);
        SubmissionAnalyticsResponse submissions = submissionAnalyticsService.summarize(productId);
        AssetAnalyticsResponse assets = assetAnalyticsService.summarize(productId);
        return List.of(
                new AnalyticsReportResponse("Relatório mensal do produto", "Resumo executivo-operacional do produto.",
                        "mês atual", "PDF", reportStatus(content.total() + submissions.total() + assets.total()), "—"),
                new AnalyticsReportResponse("Performance de conteúdo", "Publicações, pendências e revisão editorial.",
                        "30 dias", "CSV", reportStatus(content.total()), "—"),
                new AnalyticsReportResponse("Conversões de formulários", "Submissions recebidas e atividade recente.",
                        "14 dias", "XLSX", reportStatus(submissions.total()), "—"),
                new AnalyticsReportResponse("Saúde do produto", "Pendências por módulo.",
                        "atual", "PDF", "pronto", "—"),
                new AnalyticsReportResponse("SEO e acessibilidade", "Alt text, metas e oportunidades.",
                        "30 dias", "PDF", reportStatus(assets.total()), "—")
        );
    }

    private AnalyticsKpiResponse publishedContentKpi(ContentAnalyticsResponse content) {
        String comparison = content.pendingReview() == 0 ? "sem pendências" : content.pendingReview() + " em revisão";
        String note = content.published() == 0 ? "Nenhum conteúdo publicado ainda." : "Conteúdo publicado a partir do workflow editorial.";
        return new AnalyticsKpiResponse("Conteúdos publicados", Long.toString(content.published()), comparison, note, tone(content.published()));
    }

    private AnalyticsKpiResponse pendingContentKpi(ContentAnalyticsResponse content) {
        long pending = content.pendingReview() + content.drafts();
        String comparison = content.stalePendingReview() == 0 ? "sem revisão vencida" : content.stalePendingReview() + " há mais de 30 dias";
        String note = pending == 0 ? "Não há conteúdo pendente." : "Conteúdo pendente considera drafts e itens em revisão.";
        return new AnalyticsKpiResponse("Conteúdos pendentes", Long.toString(pending), comparison, note, toneAttentionWhenPositive(pending));
    }

    private AnalyticsKpiResponse submissionsKpi(SubmissionAnalyticsResponse submissions) {
        String note = submissions.total() == 0 ? "Nenhuma submission recebida ainda." : "Submissions agregadas dos formulários do produto.";
        return new AnalyticsKpiResponse("Formulários recebidos", Long.toString(submissions.total()),
                submissions.recent() + " nos últimos 14 dias", note, tone(submissions.total()));
    }

    private AnalyticsKpiResponse recentAssetsKpi(AssetAnalyticsResponse assets) {
        String note = assets.total() == 0 ? "Nenhum asset cadastrado ainda." : "Assets recentes consideram uploads dos últimos 30 dias.";
        return new AnalyticsKpiResponse("Assets recentes", Long.toString(assets.recent()),
                assets.total() + " no total", note, tone(assets.recent()));
    }

    private HealthSignalResponse contentHealth(ContentAnalyticsResponse content) {
        if (content.total() == 0) {
            return new HealthSignalResponse(CONTENT_LABEL, "Sem conteúdo", "0", NEUTRAL);
        }
        if (content.stalePendingReview() > 0) {
            return new HealthSignalResponse(CONTENT_LABEL, "Revisão atrasada", "60", ATTENTION);
        }
        if (content.pendingReview() + content.drafts() > 0) {
            return new HealthSignalResponse(CONTENT_LABEL, "Pendências leves", "78", ATTENTION);
        }
        return new HealthSignalResponse(CONTENT_LABEL, OPERATING_WELL_STATUS, "95", POSITIVE);
    }

    private HealthSignalResponse formsHealth(SubmissionAnalyticsResponse submissions) {
        if (submissions.total() == 0) {
            return new HealthSignalResponse(FORMS_LABEL, "Sem submissions", "0", NEUTRAL);
        }
        if (submissions.recent() == 0) {
            return new HealthSignalResponse(FORMS_LABEL, "Sem atividade recente", "72", ATTENTION);
        }
        return new HealthSignalResponse(FORMS_LABEL, OPERATING_WELL_STATUS, "90", POSITIVE);
    }

    private HealthSignalResponse assetsHealth(AssetAnalyticsResponse assets) {
        if (assets.total() == 0) {
            return new HealthSignalResponse(ASSETS_LABEL, "Sem assets", "0", NEUTRAL);
        }
        if (assets.missingAltText() > 0) {
            return new HealthSignalResponse(ASSETS_LABEL, "Alt text pendente", "70", ATTENTION);
        }
        return new HealthSignalResponse(ASSETS_LABEL, OPERATING_WELL_STATUS, "92", POSITIVE);
    }

    private HealthSignalResponse generalHealth(HealthSignalResponse content, HealthSignalResponse forms,
                                               HealthSignalResponse assets) {
        int score = (parseScore(content.score()) + parseScore(forms.score()) + parseScore(assets.score())) / 3;
        String tone = generalHealthTone(score);
        String status = generalHealthStatus(score);
        return new HealthSignalResponse("Saúde geral", status, Integer.toString(score), tone);
    }

    private String generalHealthTone(int score) {
        if (score >= 85) {
            return POSITIVE;
        }
        if (score >= 60) {
            return ATTENTION;
        }
        return NEUTRAL;
    }

    private String generalHealthStatus(int score) {
        if (score >= 85) {
            return "Produto saudável";
        }
        if (score >= 60) {
            return "Requer atenção";
        }
        return "Dados insuficientes";
    }

    private String submissionsTrendText(SubmissionAnalyticsResponse submissions) {
        return submissions.recent() == 0
                ? "Formulários ainda não receberam submissions recentes."
                : "Formulários receberam submissions recentes.";
    }

    private String contentTrendText(ContentAnalyticsResponse content) {
        return content.stalePendingReview() == 0
                ? "Conteúdo editorial não possui revisão vencida."
                : "Conteúdo editorial possui revisão há mais de 30 dias.";
    }

    private String assetTrendText(AssetAnalyticsResponse assets) {
        return assets.missingAltText() == 0
                ? "Assets estão sem pendência de alt text."
                : "Assets sem alt text podem prejudicar SEO e acessibilidade.";
    }

    private String metricDaysOrCount(long value) {
        return value == 0 ? "0" : value + " pendentes";
    }

    private String severity(long value) {
        return value == 0 ? NEUTRAL : HIGH;
    }

    private String reportStatus(long baseCount) {
        return baseCount == 0 ? "sem dados suficientes" : "pronto";
    }

    private String tone(long value) {
        return value == 0 ? NEUTRAL : POSITIVE;
    }

    private String toneAttentionWhenPositive(long value) {
        return value == 0 ? POSITIVE : ATTENTION;
    }

    private String signed(long value) {
        return value == 0 ? "0" : "+" + value;
    }

    private int parseScore(String value) {
        return Integer.parseInt(value);
    }
}
