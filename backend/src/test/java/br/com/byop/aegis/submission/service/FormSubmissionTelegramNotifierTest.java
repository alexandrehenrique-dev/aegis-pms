package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.form.api.FormReference;
import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.domain.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FormSubmissionTelegramNotifierTest {

    @Test
    void shouldSendExternalFormSubmissionToConfiguredTelegramChannel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FormSubmissionTelegramNotifier notifier = new FormSubmissionTelegramNotifier(builder.build(), new ObjectMapper());
        FormReference form = form("""
                [{"type":"telegram","enabled":true,"config":{"chatId":"chat-1","botToken":"product-token"}}]
                """);

        server.expect(once(), requestTo("https://api.telegram.org/botproduct-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nova submissao de formulario")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ana@example.com")))
                .andRespond(withSuccess());

        notifier.notify(form, submission());

        server.verify();
    }

    @Test
    void shouldIgnoreInvalidDeliveryJsonAndNotPropagateTelegramFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FormSubmissionTelegramNotifier notifier = new FormSubmissionTelegramNotifier(builder.build(), new ObjectMapper());
        FormReference form = form("""
                [{"type":"telegram","enabled":true,"config":{"chatId":"chat-1","botToken":"product-token"}}]
                """);

        server.expect(once(), requestTo("https://api.telegram.org/botproduct-token/sendMessage"))
                .andRespond(withServerError());

        assertThatCode(() -> notifier.notify(form, submission())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("not-json"), submission())).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void shouldSkipWhenDeliveryChannelIsNotSendableTelegram() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FormSubmissionTelegramNotifier notifier = new FormSubmissionTelegramNotifier(builder.build(), new ObjectMapper());

        server.expect(never(), requestTo("https://api.telegram.org/botproduct-token/sendMessage"));

        assertThatCode(() -> notifier.notify(form(null), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("[]"), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("""
                [{"type":"email","enabled":true},{"type":"telegram","enabled":false}]
                """), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("""
                [{"type":"telegram","enabled":true}]
                """), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("""
                [{"type":"telegram","enabled":true,"config":{"chatId":"chat-1"}}]
                """), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("""
                [{"type":"telegram","enabled":true,"config":{"botToken":"product-token"}}]
                """), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("""
                {"type":"telegram"}
                """), submissionWithBlankOptionalFields())).doesNotThrowAnyException();
        assertThatCode(() -> notifier.notify(form("""
                ["telegram"]
                """), submissionWithBlankOptionalFields())).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void shouldUseDashForBlankSubmissionFieldsInTelegramMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FormSubmissionTelegramNotifier notifier = new FormSubmissionTelegramNotifier(builder.build(), new ObjectMapper());
        FormReference form = form("""
                [{"type":"telegram","enabled":true,"config":{"chatId":"chat-1","botToken":"product-token"}}]
                """);

        server.expect(once(), requestTo("https://api.telegram.org/botproduct-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nome: -")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email: -")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Origem: -")))
                .andRespond(withSuccess());

        notifier.notify(form, submissionWithBlankOptionalFields());

        server.verify();
    }

    private FormReference form(String deliveryChannelsJson) {
        return new FormReference(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                true,
                "[]",
                deliveryChannelsJson
        );
    }

    private Submission submission() {
        Submission submission = new Submission(new Submission.Creation(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                OffsetDateTime.parse("2026-07-03T10:00:00Z"),
                "Ana",
                "ana@example.com",
                "site",
                SubmissionStatus.NEW,
                null,
                null,
                "{}"
        ));
        ReflectionTestUtils.setField(submission, "id", UUID.fromString("44444444-4444-4444-4444-444444444444"));
        return submission;
    }

    private Submission submissionWithBlankOptionalFields() {
        Submission submission = new Submission(new Submission.Creation(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                OffsetDateTime.parse("2026-07-03T10:00:00Z"),
                "",
                "",
                " ",
                SubmissionStatus.NEW,
                null,
                null,
                "{}"
        ));
        ReflectionTestUtils.setField(submission, "id", UUID.fromString("55555555-5555-5555-5555-555555555555"));
        return submission;
    }
}
