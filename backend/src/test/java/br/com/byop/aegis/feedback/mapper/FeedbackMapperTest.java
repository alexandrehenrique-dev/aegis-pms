package br.com.byop.aegis.feedback.mapper;

import br.com.byop.aegis.feedback.domain.Feedback;
import br.com.byop.aegis.feedback.domain.FeedbackCategory;
import br.com.byop.aegis.feedback.domain.FeedbackPriority;
import br.com.byop.aegis.feedback.dto.FeedbackSummary;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackMapperTest {

    private static final UUID INTERNAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ASSET_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-03T10:00:00Z");

    private final FeedbackMapper mapper = Mappers.getMapper(FeedbackMapper.class);

    @Test
    void shouldMapEntityToSummaryUsingPublicIdAsPrimaryIdentifier() {
        Feedback feedback = feedback();

        FeedbackSummary summary = mapper.toSummary(feedback);

        assertThat(summary.id()).isEqualTo("AGS-0042");
        assertThat(summary.id()).isNotEqualTo(INTERNAL_ID.toString());
        assertThat(summary.category()).isEqualTo("Bug");
        assertThat(summary.priority()).isEqualTo("crítica");
        assertThat(summary.description()).isEqualTo("Falha no botao salvar.");
        assertThat(summary.status()).isEqualTo("aberto");
        assertThat(summary.createdBySubject()).isEqualTo("subject-1");
        assertThat(summary.tenantId()).isEqualTo(TENANT_ID);
        assertThat(summary.productId()).isEqualTo(PRODUCT_ID);
        assertThat(summary.createdAt()).isEqualTo(CREATED_AT);
        assertThat(summary.attachmentAssetId()).isEqualTo(ASSET_ID);
    }

    @Test
    void shouldReturnNullWhenFeedbackIsNull() {
        assertThat(mapper.toSummary(null)).isNull();
    }

    @Test
    void shouldExposeNullContractValues() {
        assertThat(mapper.toContractCategory(null)).isNull();
        assertThat(mapper.toContractPriority(null)).isNull();
        assertThat(mapper.toContractStatus(null)).isNull();
    }

    private Feedback feedback() {
        Feedback feedback = new Feedback(new Feedback.Creation(
                "AGS-0042",
                TENANT_ID,
                PRODUCT_ID,
                "subject-1",
                FeedbackCategory.BUG,
                FeedbackPriority.CRITICAL,
                "Falha no botao salvar.",
                "/settings",
                ASSET_ID
        ));
        ReflectionTestUtils.setField(feedback, "id", INTERNAL_ID);
        ReflectionTestUtils.setField(feedback, "createdAt", CREATED_AT);
        return feedback;
    }
}
