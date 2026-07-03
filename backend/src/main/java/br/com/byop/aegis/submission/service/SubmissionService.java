package br.com.byop.aegis.submission.service;

import br.com.byop.aegis.audit.api.AuditRecordCommand;
import br.com.byop.aegis.audit.api.AuditService;
import br.com.byop.aegis.asset.api.AssetReference;
import br.com.byop.aegis.asset.api.AssetReferenceService;
import br.com.byop.aegis.form.api.FormReference;
import br.com.byop.aegis.form.api.FormReferenceService;
import br.com.byop.aegis.form.api.FormFieldCatalog;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubmissionService {

    public static final String FORM_NOT_PUBLISHED_ERROR = "SUBMISSION_FORM_NOT_PUBLISHED";
    public static final String MISSING_REQUIRED_FIELD_ERROR = "MISSING_REQUIRED_FIELD";
    public static final String UPLOAD_ACCEPTED_TYPES_ERROR = "SUBMISSION_UPLOAD_ACCEPTED_FILE_TYPES_REQUIRED";
    public static final String INVALID_UPLOAD_ASSET_ERROR = "INVALID_SUBMISSION_UPLOAD_ASSET";
    public static final String INVALID_UPLOAD_MIME_ERROR = "INVALID_SUBMISSION_UPLOAD_MIME_TYPE";
    private static final String SYSTEM_ACTOR = "system";
    private static final String ACTION_FORM_SUBMISSION_RECEIVED = "FORM_SUBMISSION_RECEIVED";
    private static final String TARGET_TYPE_FORM = "Form";
    private static final String MODULE_FORM = "FORM";

    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;
    private final FormReferenceService formReferenceService;
    private final AssetReferenceService assetReferenceService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final FormSubmissionTelegramNotifier telegramNotifier;

    public SubmissionService(SubmissionRepository submissionRepository, SubmissionMapper submissionMapper,
                             FormReferenceService formReferenceService, AssetReferenceService assetReferenceService,
                             ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher,
                             AuditService auditService, FormSubmissionTelegramNotifier telegramNotifier) {
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.formReferenceService = formReferenceService;
        this.assetReferenceService = assetReferenceService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
        this.telegramNotifier = telegramNotifier;
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummary> listSubmissions(UUID productId, UUID formId) {
        FormReference form = formReferenceService.getRequiredReference(productId, formId);
        return submissionRepository.findAllByFormIdOrderByDateDesc(form.id()).stream()
                .map(submissionMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummary> listProductSubmissions(UUID productId) {
        List<UUID> formIds = formReferenceService.listFormIds(productId);
        if (formIds.isEmpty()) {
            return List.of();
        }
        return submissionRepository.findAllByFormIdInOrderByDateDesc(formIds).stream()
                .map(submissionMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionDetail getSubmission(UUID productId, UUID formId, UUID submissionId) {
        FormReference form = formReferenceService.getRequiredReference(productId, formId);
        Submission submission = submissionRepository.findByFormIdAndId(form.id(), submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        return submissionMapper.toDetail(submission, readMap(submission.getAnswersJson()));
    }

    @Transactional
    public SubmissionDetail submit(UUID productId, UUID formId, SubmitFormCommand command) {
        FormReference form = formReferenceService.getRequiredReference(productId, formId);
        if (!form.published()) {
            throw new InvalidSubmissionException(FORM_NOT_PUBLISHED_ERROR);
        }
        List<Map<String, Object>> fields = readList(form.fieldsJson());
        validateRequiredAnswers(fields, command.answers());
        validateUploadAnswers(productId, fields, command.answers());
        OffsetDateTime receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
        Submission submission = submissionRepository.save(new Submission(new Submission.Creation(
                form.id(), receivedAt, command.name(), command.email(), command.source(),
                SubmissionStatus.NEW, command.ownerSubject(), command.score(), writeJson(command.answers())
        )));
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("submissionId", submission.getId());
        auditPayload.put("source", command.source());
        auditService.recordEvent(new AuditRecordCommand(
                form.tenantId(),
                form.productId(),
                SYSTEM_ACTOR,
                ACTION_FORM_SUBMISSION_RECEIVED,
                TARGET_TYPE_FORM,
                form.id().toString(),
                form.id().toString(),
                MODULE_FORM,
                null,
                auditPayload
        ));
        telegramNotifier.notify(form, submission);
        eventPublisher.publishEvent(new SubmissionReceivedEvent(form.id(), form.productId(), receivedAt.toInstant()));
        return submissionMapper.toDetail(submission, command.answers());
    }

    private void validateRequiredAnswers(List<Map<String, Object>> fields, Map<String, Object> answers) {
        for (Map<String, Object> field : fields) {
            if (Boolean.TRUE.equals(field.get("required")) && isBlankAnswer(answers, stringValue(field.get("label")))) {
                throw new InvalidSubmissionException(MISSING_REQUIRED_FIELD_ERROR);
            }
        }
    }

    private boolean isBlankAnswer(Map<String, Object> answers, String label) {
        if (label.isBlank()) {
            return false;
        }
        if (answers == null) {
            return true;
        }
        Object answer = answers.get(label);
        if (answer == null) {
            return true;
        }
        if (answer instanceof String text) {
            return text.isBlank();
        }
        if (answer instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    private void validateUploadAnswers(UUID productId, List<Map<String, Object>> fields, Map<String, Object> answers) {
        if (answers == null) {
            return;
        }
        for (Map<String, Object> field : fields) {
            if (FormFieldCatalog.isUpload(stringValue(field.get("type")))) {
                validateUploadAnswer(productId, field, answers.get(stringValue(field.get("label"))));
            }
        }
    }

    private void validateUploadAnswer(UUID productId, Map<String, Object> field, Object answer) {
        if (answer == null) {
            return;
        }
        List<String> acceptedFileTypes = acceptedFileTypes(field);
        if (acceptedFileTypes.isEmpty()) {
            throw new InvalidSubmissionException(UPLOAD_ACCEPTED_TYPES_ERROR);
        }
        UUID assetId = parseAssetId(answer);
        AssetReference asset = assetReferenceService.getRequiredReference(assetId);
        if (!productId.equals(asset.productId())) {
            throw new InvalidSubmissionException(INVALID_UPLOAD_ASSET_ERROR);
        }
        if (!acceptedFileTypes.contains(asset.mimeType())) {
            throw new InvalidSubmissionException(INVALID_UPLOAD_MIME_ERROR);
        }
    }

    private UUID parseAssetId(Object answer) {
        try {
            return UUID.fromString(answer.toString());
        } catch (IllegalArgumentException _) {
            throw new InvalidSubmissionException(INVALID_UPLOAD_ASSET_ERROR);
        }
    }

    private List<String> acceptedFileTypes(Map<String, Object> field) {
        Object value = field.get("acceptedFileTypes");
        if (value instanceof List<?> list && list.stream().allMatch(item -> item instanceof String _)) {
            return list.stream()
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize submission answers", ex);
        }
    }

    private List<Map<String, Object>> readList(String json) {
        try {
            Object value = objectMapper.readValue(json, List.class);
            if (value instanceof List<?> list && list.stream().allMatch(item -> item instanceof Map<?, ?>)) {
                return list.stream()
                        .map(item -> toObjectMap((Map<?, ?>) item))
                        .toList();
            }
            return List.of();
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize form fields", ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            Object value = objectMapper.readValue(json, Map.class);
            if (value instanceof Map<?, ?> map) {
                return toObjectMap(map);
            }
            return Map.of();
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize submission answers", ex);
        }
    }

    private Map<String, Object> toObjectMap(Map<?, ?> map) {
        Map<String, Object> objectMap = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> objectMap.put(key.toString(), mapValue));
        return objectMap;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
