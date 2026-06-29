package br.com.byop.aegis.form.api;

import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class FormResponseReadModelService {

    private final FormDefinitionRepository formDefinitionRepository;

    public FormResponseReadModelService(FormDefinitionRepository formDefinitionRepository) {
        this.formDefinitionRepository = formDefinitionRepository;
    }

    @Transactional
    public void registerResponse(UUID formId, Instant receivedAt) {
        formDefinitionRepository.findById(formId).ifPresent(form -> {
            OffsetDateTime activityAt = OffsetDateTime.ofInstant(receivedAt, ZoneOffset.UTC);
            form.registerResponse(activityAt);
            formDefinitionRepository.save(form);
        });
    }
}
