package br.com.byop.aegis.feedback.service;

import br.com.byop.aegis.feedback.domain.Feedback;
import br.com.byop.aegis.feedback.domain.FeedbackCategory;
import br.com.byop.aegis.feedback.domain.FeedbackPriority;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramFeedbackNotifierTest {

    @Test
    void shouldSendAegisFeedbackToGlobalTelegramWhenEnabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramFeedbackNotifier notifier = new TelegramFeedbackNotifier(builder.build(), true, "chat-1", "global-token");
        Feedback feedback = feedback();

        server.expect(once(), requestTo("https://api.telegram.org/botglobal-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGS-0001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/assets/66666666-6666-6666-6666-666666666666/download")))
                .andRespond(withSuccess());

        notifier.notify(feedback);

        server.verify();
    }

    @Test
    void shouldSkipAegisTelegramWhenDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramFeedbackNotifier notifier = new TelegramFeedbackNotifier(builder.build(), false, "chat-1", "global-token");

        server.expect(never(), requestTo("https://api.telegram.org/botglobal-token/sendMessage"));

        notifier.notify(feedback());

        server.verify();
    }

    @Test
    void shouldSkipAegisTelegramWhenGlobalConfigurationIsIncomplete() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramFeedbackNotifier missingChat = new TelegramFeedbackNotifier(builder.build(), true, " ", "global-token");
        TelegramFeedbackNotifier missingToken = new TelegramFeedbackNotifier(builder.build(), true, "chat-1", null);

        server.expect(never(), requestTo("https://api.telegram.org/botglobal-token/sendMessage"));

        missingChat.notify(feedback());
        missingToken.notify(feedback());

        server.verify();
    }

    @Test
    void shouldNotPropagateTelegramFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramFeedbackNotifier notifier = new TelegramFeedbackNotifier(builder.build(), true, "chat-1", "global-token");

        server.expect(once(), requestTo("https://api.telegram.org/botglobal-token/sendMessage"))
                .andRespond(withServerError());

        assertThatCode(() -> notifier.notify(feedback())).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void shouldSendAegisFeedbackWithoutAttachmentAndTrimLongDescription() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramFeedbackNotifier notifier = new TelegramFeedbackNotifier(builder.build(), true, "chat-1", "global-token");
        Feedback feedback = feedbackWithoutAttachment();

        server.expect(once(), requestTo("https://api.telegram.org/botglobal-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AGS-0002")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tela: -")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("...")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/download"))))
                .andRespond(withSuccess());

        notifier.notify(feedback);

        server.verify();
    }

    private Feedback feedback() {
        Feedback feedback = new Feedback(new Feedback.Creation(
                "AGS-0001",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "editor-subject",
                FeedbackCategory.BUG,
                FeedbackPriority.HIGH,
                "Botao salvar nao responde",
                "/settings",
                UUID.fromString("66666666-6666-6666-6666-666666666666")
        ));
        ReflectionTestUtils.setField(feedback, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return feedback;
    }

    private Feedback feedbackWithoutAttachment() {
        Feedback feedback = new Feedback(new Feedback.Creation(
                "AGS-0002",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                null,
                "editor-subject",
                FeedbackCategory.BUG,
                FeedbackPriority.HIGH,
                "a".repeat(250),
                null,
                null
        ));
        ReflectionTestUtils.setField(feedback, "id", UUID.fromString("77777777-7777-7777-7777-777777777777"));
        return feedback;
    }
}
