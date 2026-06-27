package br.com.byop.aegis.form.api;

import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.domain.FormStatus;
import br.com.byop.aegis.form.exception.FormNotFoundException;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FormReferenceService {

    private final FormDefinitionRepository formRepository;

    public FormReferenceService(FormDefinitionRepository formRepository) {
        this.formRepository = formRepository;
    }

    @Transactional(readOnly = true)
    public FormReference getRequiredReference(UUID productId, UUID formId) {
        FormDefinition form = formRepository.findByProductIdAndId(productId, formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
        return toReference(form);
    }

    @Transactional(readOnly = true)
    public List<UUID> listFormIds(UUID productId) {
        return formRepository.findAllByProductId(productId).stream()
                .map(FormDefinition::getId)
                .toList();
    }

    private FormReference toReference(FormDefinition form) {
        return new FormReference(form.getId(), form.getTenantId(), form.getProductId(),
                form.getStatus() == FormStatus.PUBLISHED, form.getFieldsJson());
    }
}
