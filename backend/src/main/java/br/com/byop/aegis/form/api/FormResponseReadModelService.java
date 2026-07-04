package br.com.byop.aegis.form.api;

import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
public class FormResponseReadModelService {

    private final FormDefinitionRepository formDefinitionRepository;

    public FormResponseReadModelService(FormDefinitionRepository formDefinitionRepository) {
        this.formDefinitionRepository = formDefinitionRepository;
    }

    @Transactional
    public void registerResponse(UUID formId, Instant receivedAt) {
        log.debug("registerResponse: formId='{}', receivedAt='{}'", formId, receivedAt);
        formDefinitionRepository.findById(formId).ifPresent(form -> {
            OffsetDateTime activityAt = OffsetDateTime.ofInstant(receivedAt, ZoneOffset.UTC);
            form.registerResponse(activityAt);
            formDefinitionRepository.save(form);
            log.info("registerResponse: resposta registrada para formId='{}'", formId);
        });
    }
}
