package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.asset.api.AssetReference;
import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.form.api.FormReference;
import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.submission.command.SubmitFormCommand;
import br.com.byop.aegis.submission.domain.Submission;
import br.com.byop.aegis.submission.domain.SubmissionStatus;
import br.com.byop.aegis.submission.dto.SubmissionDetail;
import br.com.byop.aegis.submission.dto.SubmissionSummary;
import br.com.byop.aegis.submission.exception.InvalidSubmissionException;
import br.com.byop.aegis.submission.exception.SubmissionNotFoundException;
import br.com.byop.aegis.submission.mapper.SubmissionMapper;
import br.com.byop.aegis.submission.repository.SubmissionRepository;
import br.com.byop.aegis.submission.api.SubmissionReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FORM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SUBMISSION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ASSET_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private FormReferenceService formReferenceService;

    @Mock
    private AssetReferenceService assetReferenceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditService auditService;

    @Mock
    private FormSubmissionTelegramNotifier telegramNotifier;

    private SubmissionService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(submissionRepository, submissionMapper, formReferenceService,
                assetReferenceService, new ObjectMapper(), eventPublisher, auditService, telegramNotifier);
    }

    @Test
    void shouldListSubmissionsForForm() {
        Submission submission = submission();
        SubmissionSummary summary = new SubmissionSummary(SUBMISSION_ID, "date", "Ana", "ana@example.com", "site", "new", "—", "—");
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.findAllByFormIdOrderByDateDesc(FORM_ID)).thenReturn(List.of(submission));
        when(submissionMapper.toSummary(submission)).thenReturn(summary);

        assertThat(service.listSubmissions(PRODUCT_ID, FORM_ID)).containsExactly(summary);
    }

    @Test
    void shouldListProductSubmissions() {
        Submission submission = submission();
        SubmissionSummary summary = new SubmissionSummary(SUBMISSION_ID, "date", "Ana", "ana@example.com", "site", "new", "—", "—");
        when(formReferenceService.listFormIds(PRODUCT_ID)).thenReturn(List.of(FORM_ID));
        when(submissionRepository.findAllByFormIdInOrderByDateDesc(List.of(FORM_ID))).thenReturn(List.of(submission));
        when(submissionMapper.toSummary(submission)).thenReturn(summary);

        assertThat(service.listProductSubmissions(PRODUCT_ID)).containsExactly(summary);
    }

    @Test
    void shouldReturnEmptyProductSubmissionsWhenProductHasNoForms() {
        when(formReferenceService.listFormIds(PRODUCT_ID)).thenReturn(List.of());

        assertThat(service.listProductSubmissions(PRODUCT_ID)).isEmpty();
    }

    @Test
    void shouldGetSubmissionDetailWithFullAnswersJson() {
        Submission submission = submission();
        SubmissionDetail detail = new SubmissionDetail(SUBMISSION_ID, FORM_ID, submission.getDate(), "Ana", "ana@example.com",
                "site", "new", "—", null, Map.of("Email", "ana@example.com"), null);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.findByFormIdAndId(FORM_ID, SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(submissionMapper.toDetail(eq(submission), any())).thenReturn(detail);

        assertThat(service.getSubmission(PRODUCT_ID, FORM_ID, SUBMISSION_ID)).isEqualTo(detail);
    }

    @Test
    void shouldRejectGetWhenSubmissionDoesNotBelongToForm() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.findByFormIdAndId(FORM_ID, SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubmission(PRODUCT_ID, FORM_ID, SUBMISSION_ID))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    @Test
    void shouldSubmitToPublishedFormAndPersistAssetIdInAnswersJson() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, uploadFieldsJson(List.of("application/pdf"))));
        when(assetReferenceService.getRequiredReference(ASSET_ID))
                .thenReturn(new AssetReference(ASSET_ID, PRODUCT_ID, "application/pdf", "document"));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(submissionMapper.toDetail(any(), any())).thenReturn(new SubmissionDetail(null, FORM_ID, null, "Ana",
                "ana@example.com", "site", "new", "—", null, Map.of("Curriculo", ASSET_ID.toString()), null));
        SubmitFormCommand command = command(Map.of("Email", "ana@example.com", "Curriculo", ASSET_ID.toString()));

        service.submit(PRODUCT_ID, FORM_ID, command);

        ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(captor.capture());
        assertThat(captor.getValue().getAnswersJson()).contains(ASSET_ID.toString());
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).recordEvent(auditCaptor.capture());
        AuditRecordCommand audit = auditCaptor.getValue();
        assertThat(audit.tenantId()).isEqualTo(TENANT_ID);
        assertThat(audit.productId()).isEqualTo(PRODUCT_ID);
        assertThat(audit.actorSubject()).isEqualTo("system");
        assertThat(audit.action()).isEqualTo("FORM_SUBMISSION_RECEIVED");
        assertThat(audit.targetId()).isEqualTo(FORM_ID.toString());
        verify(telegramNotifier).notify(any(FormReference.class), any(Submission.class));
        verify(eventPublisher).publishEvent(any(SubmissionReceivedEvent.class));
    }

    @Test
    void shouldSubmitUploadWhenFormUsesAcceptedFormatLabels() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, "[{\"label\":\"Curriculo\",\"type\":\"Upload\",\"required\":true,"
                        + "\"acceptedFormats\":[\"PDF\",\"Imagem\",\"DOCX\",\"ZIP\"]}]"));
        when(assetReferenceService.getRequiredReference(ASSET_ID))
                .thenReturn(new AssetReference(ASSET_ID, PRODUCT_ID, "image/png", "image"));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmitFormCommand command = command(Map.of("Curriculo", ASSET_ID.toString()));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, command.answers(), null);
        when(submissionMapper.toDetail(any(), eq(command.answers()))).thenReturn(detail);

        assertThat(service.submit(PRODUCT_ID, FORM_ID, command)).isEqualTo(detail);
    }

    @Test
    void shouldSubmitWhenAnswersAreNull() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, optionalFieldsJson()));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, null, null);
        when(submissionMapper.toDetail(any(), eq(null))).thenReturn(detail);

        assertThat(service.submit(PRODUCT_ID, FORM_ID, command(null))).isEqualTo(detail);
    }

    @Test
    void shouldIgnoreMissingUploadAnswer() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, optionalUploadFieldsJson(List.of("application/pdf"))));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, Map.of(), null);
        when(submissionMapper.toDetail(any(), eq(Map.of()))).thenReturn(detail);

        assertThat(service.submit(PRODUCT_ID, FORM_ID, command(Map.of()))).isEqualTo(detail);
    }

    @Test
    void shouldIgnoreFieldsWithoutUploadType() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, "[{\"label\":null,\"required\":true}]"));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, Map.of("Email", "ana@example.com"), null);
        when(submissionMapper.toDetail(any(), eq(Map.of("Email", "ana@example.com")))).thenReturn(detail);

        assertThat(service.submit(PRODUCT_ID, FORM_ID, command(Map.of("Email", "ana@example.com")))).isEqualTo(detail);
    }

    @Test
    void shouldSubmitWhenFormFieldsDoNotDeserializeAsList() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doReturn("not-a-list").when(objectMapper).readValue(any(String.class), eq(List.class));
        doReturn("{}").when(objectMapper).writeValueAsString(any());
        SubmissionService defensiveService = new SubmissionService(submissionRepository, submissionMapper,
                formReferenceService, assetReferenceService, objectMapper, eventPublisher, auditService, telegramNotifier);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmitFormCommand command = command(Map.of("Email", "ana@example.com"));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, command.answers(), null);
        when(submissionMapper.toDetail(any(), eq(command.answers()))).thenReturn(detail);

        assertThat(defensiveService.submit(PRODUCT_ID, FORM_ID, command)).isEqualTo(detail);
    }

    @Test
    void shouldSubmitWhenFormFieldsListDoesNotContainMaps() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doReturn(List.of("not-a-map")).when(objectMapper).readValue(any(String.class), eq(List.class));
        doReturn("{}").when(objectMapper).writeValueAsString(any());
        SubmissionService defensiveService = new SubmissionService(submissionRepository, submissionMapper,
                formReferenceService, assetReferenceService, objectMapper, eventPublisher, auditService, telegramNotifier);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmitFormCommand command = command(Map.of("Email", "ana@example.com"));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, command.answers(), null);
        when(submissionMapper.toDetail(any(), eq(command.answers()))).thenReturn(detail);

        assertThat(defensiveService.submit(PRODUCT_ID, FORM_ID, command)).isEqualTo(detail);
    }

    @Test
    void shouldRejectSubmissionForDraftForm() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(false, basicFieldsJson()));
        SubmitFormCommand command = command(Map.of());

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.FORM_NOT_PUBLISHED_ERROR);
    }

    @Test
    void shouldRejectSubmissionMissingRequiredField() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        SubmitFormCommand command = command(Map.of("Email", " "));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.MISSING_REQUIRED_FIELD_ERROR);
    }

    @ParameterizedTest
    @MethodSource("missingRequiredAnswers")
    void shouldRejectSubmissionWhenRequiredAnswerIsMissing(Map<String, Object> answers) {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        SubmitFormCommand command = command(answers);

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.MISSING_REQUIRED_FIELD_ERROR);
    }

    @ParameterizedTest
    @MethodSource("presentRequiredAnswers")
    void shouldAcceptNonBlankRequiredAnswerShapes(Map<String, Object> answers) {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubmissionDetail detail = new SubmissionDetail(null, FORM_ID, null, "Ana", "ana@example.com",
                "site", "new", "—", null, answers, null);
        when(submissionMapper.toDetail(any(), eq(answers))).thenReturn(detail);

        assertThat(service.submit(PRODUCT_ID, FORM_ID, command(answers))).isEqualTo(detail);
    }

    @Test
    void shouldRejectUploadSubmissionWithoutAcceptedFileTypes() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, uploadFieldsJson(List.of())));
        SubmitFormCommand command = command(Map.of("Curriculo", ASSET_ID.toString()));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.UPLOAD_ACCEPTED_TYPES_ERROR);
    }

    @Test
    void shouldRejectUploadSubmissionWithInvalidAcceptedFileTypesShape() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, "[{\"label\":\"Curriculo\",\"type\":\"Upload\",\"required\":true,"
                        + "\"acceptedFileTypes\":\"application/pdf\"}]"));
        SubmitFormCommand command = command(Map.of("Curriculo", ASSET_ID.toString()));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.UPLOAD_ACCEPTED_TYPES_ERROR);
    }

    @Test
    void shouldRejectUploadSubmissionWithNonStringAcceptedFileType() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, "[{\"label\":\"Curriculo\",\"type\":\"Upload\",\"required\":true,"
                        + "\"acceptedFileTypes\":[123]}]"));
        SubmitFormCommand command = command(Map.of("Curriculo", ASSET_ID.toString()));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.UPLOAD_ACCEPTED_TYPES_ERROR);
    }

    @Test
    void shouldRejectUploadAssetFromAnotherProduct() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, uploadFieldsJson(List.of("application/pdf"))));
        when(assetReferenceService.getRequiredReference(ASSET_ID))
                .thenReturn(new AssetReference(ASSET_ID, OTHER_PRODUCT_ID, "application/pdf", "document"));
        SubmitFormCommand command = command(Map.of("Curriculo", ASSET_ID.toString()));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.INVALID_UPLOAD_ASSET_ERROR);
    }

    @Test
    void shouldRejectUploadAssetWithMimeOutsideAcceptedList() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, uploadFieldsJson(List.of("application/pdf"))));
        when(assetReferenceService.getRequiredReference(ASSET_ID))
                .thenReturn(new AssetReference(ASSET_ID, PRODUCT_ID, "image/png", "image"));
        SubmitFormCommand command = command(Map.of("Curriculo", ASSET_ID.toString()));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.INVALID_UPLOAD_MIME_ERROR);
    }

    @Test
    void shouldRejectUploadAnswerThatIsNotUuid() {
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID))
                .thenReturn(form(true, uploadFieldsJson(List.of("application/pdf"))));
        SubmitFormCommand command = command(Map.of("Curriculo", "not-uuid"));

        assertThatThrownBy(() -> service.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(InvalidSubmissionException.class)
                .hasMessage(SubmissionService.INVALID_UPLOAD_ASSET_ERROR);
    }

    @Test
    void shouldFailWhenSubmissionAnswersCannotBeSerialized() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(String.class), eq(List.class))).thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));
        SubmissionService brokenService = new SubmissionService(submissionRepository, submissionMapper,
                formReferenceService, assetReferenceService, objectMapper, eventPublisher, auditService, telegramNotifier);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        SubmitFormCommand command = command(Map.of("Email", "ana@example.com"));

        assertThatThrownBy(() -> brokenService.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to serialize submission answers");
    }

    @Test
    void shouldFailWhenFormFieldsCannotBeReadForSubmission() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(String.class), eq(List.class))).thenThrow(mock(JacksonException.class));
        SubmissionService brokenService = new SubmissionService(submissionRepository, submissionMapper,
                formReferenceService, assetReferenceService, objectMapper, eventPublisher, auditService, telegramNotifier);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        SubmitFormCommand command = command(Map.of("Email", "ana@example.com"));

        assertThatThrownBy(() -> brokenService.submit(PRODUCT_ID, FORM_ID, command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to deserialize form fields");
    }

    @Test
    void shouldFailWhenSubmissionAnswersCannotBeRead() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(String.class), eq(Map.class))).thenThrow(mock(JacksonException.class));
        SubmissionService brokenService = new SubmissionService(submissionRepository, submissionMapper,
                formReferenceService, assetReferenceService, objectMapper, eventPublisher, auditService, telegramNotifier);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.findByFormIdAndId(FORM_ID, SUBMISSION_ID)).thenReturn(Optional.of(submission()));

        assertThatThrownBy(() -> brokenService.getSubmission(PRODUCT_ID, FORM_ID, SUBMISSION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to deserialize submission answers");
    }

    @Test
    void shouldReturnEmptyAnswersWhenSubmissionAnswersDoNotDeserializeAsMap() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doReturn("not-a-map").when(objectMapper).readValue(any(String.class), eq(Map.class));
        SubmissionService defensiveService = new SubmissionService(submissionRepository, submissionMapper,
                formReferenceService, assetReferenceService, objectMapper, eventPublisher, auditService, telegramNotifier);
        Submission submission = submission();
        SubmissionDetail detail = new SubmissionDetail(SUBMISSION_ID, FORM_ID, submission.getDate(), "Ana",
                "ana@example.com", "site", "new", "—", null, Map.of(), null);
        when(formReferenceService.getRequiredReference(PRODUCT_ID, FORM_ID)).thenReturn(form(true, basicFieldsJson()));
        when(submissionRepository.findByFormIdAndId(FORM_ID, SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(submissionMapper.toDetail(submission, Map.of())).thenReturn(detail);

        assertThat(defensiveService.getSubmission(PRODUCT_ID, FORM_ID, SUBMISSION_ID)).isEqualTo(detail);
    }

    private FormReference form(boolean published, String fieldsJson) {
        return new FormReference(FORM_ID, TENANT_ID, PRODUCT_ID, published, fieldsJson, "[]");
    }

    private Submission submission() {
        Submission submission = new Submission(new Submission.Creation(FORM_ID, OffsetDateTime.parse("2026-06-27T10:00:00Z"),
                "Ana", "ana@example.com", "site", SubmissionStatus.NEW, null, null, "{\"Email\":\"ana@example.com\"}"));
        org.springframework.test.util.ReflectionTestUtils.setField(submission, "id", SUBMISSION_ID);
        return submission;
    }

    private SubmitFormCommand command(Map<String, Object> answers) {
        return new SubmitFormCommand("Ana", "ana@example.com", "site", null, null, answers);
    }

    private String basicFieldsJson() {
        return "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]";
    }

    private String optionalFieldsJson() {
        return "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":false}]";
    }

    private String uploadFieldsJson(List<String> acceptedFileTypes) {
        String values = acceptedFileTypes.stream()
                .map(value -> "\"" + value + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return "[{\"label\":\"Curriculo\",\"type\":\"Upload\",\"required\":true,\"acceptedFileTypes\":[" + values + "]}]";
    }

    private String optionalUploadFieldsJson(List<String> acceptedFileTypes) {
        String values = acceptedFileTypes.stream()
                .map(value -> "\"" + value + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        return "[{\"label\":\"Curriculo\",\"type\":\"Upload\",\"required\":false,\"acceptedFileTypes\":[" + values + "]}]";
    }

    private static Stream<Arguments> missingRequiredAnswers() {
        return Stream.of(
                Arguments.of((Map<String, Object>) null),
                Arguments.of(Map.of()),
                Arguments.of(Map.of("Email", List.of()))
        );
    }

    private static Stream<Arguments> presentRequiredAnswers() {
        return Stream.of(
                Arguments.of(Map.of("Email", List.of("ana@example.com"))),
                Arguments.of(Map.of("Email", 42))
        );
    }
}
