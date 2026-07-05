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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AnalyticsService {

    private static final String POSITIVE = "positivo";
    private static final String ATTENTION = "atenção";
    private static final String NEUTRAL = "neutro";
    private static final String HIGH = "alta";
    private static final String ASSETS_LABEL = "Assets";
    private static final String CONTENT_LABEL = "Conteúdo";
    private static final String FORMS_LABEL = "Forms";
    private static final String GENERAL_HEALTH_LABEL = "Saúde geral";
    private static final String OPERATING_WELL_STATUS = "Operando bem";
    private static final String SIMULATED_TRAFFIC = "simulado";
    private static final String ACTION_VIEW_SIGNALS = "Ver sinais";
    private static final String TARGET_OVERVIEW = "overview";
    private static final String TARGET_CONTENT = "content";
    private static final String TARGET_FORMS = "forms";
    private static final String TARGET_ASSETS = "assets";

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
        log.debug("listKpis: productId='{}'", productId);
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
        log.debug("listHealth: productId='{}'", productId);
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
        log.debug("listChannels: productId='{}'", productId);
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
        log.debug("listTrends: productId='{}'", productId);
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
        log.debug("listReports: productId='{}'", productId);
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
            return healthSignal(CONTENT_LABEL, "Sem conteúdo", "0", NEUTRAL,
                    "Nenhum item editorial encontrado para este produto.", "Criar conteúdo", TARGET_CONTENT);
        }
        if (content.stalePendingReview() > 0) {
            return healthSignal(CONTENT_LABEL, "Revisão atrasada", "60", ATTENTION,
                    content.stalePendingReview() + " item(ns) aguardam revisão há mais de 30 dias.",
                    "Resolver pendência", TARGET_CONTENT);
        }
        if (content.pendingReview() + content.drafts() > 0) {
            long pending = content.pendingReview() + content.drafts();
            return healthSignal(CONTENT_LABEL, "Pendências leves", "78", ATTENTION,
                    pending + " item(ns) ainda estão em rascunho ou revisão.", "Ver workflow", TARGET_CONTENT);
        }
        return healthSignal(CONTENT_LABEL, OPERATING_WELL_STATUS, "95", POSITIVE,
                content.published() + " conteúdo(s) publicados sem pendência operacional.", ACTION_VIEW_SIGNALS, TARGET_CONTENT);
    }

    private HealthSignalResponse formsHealth(SubmissionAnalyticsResponse submissions) {
        if (submissions.total() == 0) {
            return healthSignal(FORMS_LABEL, "Sem submissions", "0", NEUTRAL,
                    "Nenhuma submission real foi recebida para os formulários deste produto.",
                    "Ver submissions", TARGET_FORMS);
        }
        if (submissions.recent() == 0) {
            return healthSignal(FORMS_LABEL, "Sem atividade recente", "72", ATTENTION,
                    submissions.total() + " submission(ns) no total, mas nenhuma nos últimos 14 dias.",
                    "Investigar forms", TARGET_FORMS);
        }
        return healthSignal(FORMS_LABEL, OPERATING_WELL_STATUS, "90", POSITIVE,
                submissions.recent() + " submission(ns) recebidas nos últimos 14 dias.", ACTION_VIEW_SIGNALS, TARGET_FORMS);
    }

    private HealthSignalResponse assetsHealth(AssetAnalyticsResponse assets) {
        if (assets.total() == 0) {
            return healthSignal(ASSETS_LABEL, "Sem assets", "0", NEUTRAL,
                    "Nenhum asset real cadastrado para este produto.", "Enviar asset", TARGET_ASSETS);
        }
        if (assets.missingAltText() > 0) {
            return healthSignal(ASSETS_LABEL, "Alt text pendente", "70", ATTENTION,
                    assets.missingAltText() + " asset(s) precisam de alt text para acessibilidade e SEO.",
                    "Resolver pendência", TARGET_ASSETS);
        }
        return healthSignal(ASSETS_LABEL, OPERATING_WELL_STATUS, "92", POSITIVE,
                assets.total() + " asset(s) cadastrados sem pendência de alt text.", ACTION_VIEW_SIGNALS, TARGET_ASSETS);
    }

    private HealthSignalResponse generalHealth(HealthSignalResponse content, HealthSignalResponse forms,
                                               HealthSignalResponse assets) {
        int score = (parseScore(content.score()) + parseScore(forms.score()) + parseScore(assets.score())) / 3;
        String tone = generalHealthTone(score);
        String status = generalHealthStatus(score);
        String detail = "Média operacional calculada a partir de Conteúdo, Forms e Assets.";
        String action = ATTENTION.equals(tone) ? "Priorizar pendências" : ACTION_VIEW_SIGNALS;
        return healthSignal(GENERAL_HEALTH_LABEL, status, Integer.toString(score), tone, detail, action, TARGET_OVERVIEW);
    }

    private HealthSignalResponse healthSignal(String label, String status, String score, String tone,
                                              String detail, String actionLabel, String actionTarget) {
        return new HealthSignalResponse(label, status, score, tone, detail, actionLabel, actionTarget);
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
