package br.com.byop.aegis.form.api;

import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormResponseReadModelServiceTest {

    private static final UUID FORM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private FormDefinitionRepository formDefinitionRepository;

    @Test
    void shouldRegisterResponseWhenFormExists() {
        FormDefinition form = form();
        Instant receivedAt = Instant.parse("2026-06-29T12:00:00Z");
        when(formDefinitionRepository.findById(FORM_ID)).thenReturn(Optional.of(form));

        service().registerResponse(FORM_ID, receivedAt);

        assertThat(form.getResponseCount()).isEqualTo(1L);
        assertThat(form.getLastActivityAt().toInstant()).isEqualTo(receivedAt);
        verify(formDefinitionRepository).save(form);
    }

    @Test
    void shouldIgnoreMissingForm() {
        when(formDefinitionRepository.findById(FORM_ID)).thenReturn(Optional.empty());

        service().registerResponse(FORM_ID, Instant.parse("2026-06-29T12:00:00Z"));

        verify(formDefinitionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private FormResponseReadModelService service() {
        return new FormResponseReadModelService(formDefinitionRepository);
    }

    private FormDefinition form() {
        return new FormDefinition(new FormDefinition.Creation(
                TENANT_ID, PRODUCT_ID, "Contato", "lead",
                "[{\"label\":\"Email\",\"type\":\"Email\",\"required\":true}]", "[]"
        ));
    }
}
