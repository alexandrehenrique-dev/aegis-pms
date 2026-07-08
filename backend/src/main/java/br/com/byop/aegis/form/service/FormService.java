package br.com.byop.aegis.form.service;

import br.com.byop.aegis.form.contract.CreateFormDefinitionRequest;
import br.com.byop.aegis.form.contract.UpdateFormDefinitionRequest;
import br.com.byop.aegis.form.contract.UpdateFormDeliveryRequest;
import br.com.byop.aegis.form.domain.FormDefinition;
import br.com.byop.aegis.form.domain.FormFieldType;
import br.com.byop.aegis.form.dto.FormDetail;
import br.com.byop.aegis.form.dto.FormSummary;
import br.com.byop.aegis.form.exception.FormNotFoundException;
import br.com.byop.aegis.form.mapper.FormDefinitionMapper;
import br.com.byop.aegis.form.repository.FormDefinitionRepository;
import br.com.byop.aegis.product.api.ProductReference;
import br.com.byop.aegis.product.api.ProductReferenceService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class FormService {

    private final FormDefinitionRepository formRepository;
    private final FormDefinitionMapper formMapper;
    private final FormPublicationPolicy publicationPolicy;
    private final FormDeliveryPolicy deliveryPolicy;
    private final ProductReferenceService productReferenceService;
    private final ObjectMapper objectMapper;

    public FormService(FormDefinitionRepository formRepository, FormDefinitionMapper formMapper, FormPublicationPolicy publicationPolicy,
                       FormDeliveryPolicy deliveryPolicy, ProductReferenceService productReferenceService,
                       ObjectMapper objectMapper) {
        this.formRepository = formRepository;
        this.formMapper = formMapper;
        this.publicationPolicy = publicationPolicy;
        this.deliveryPolicy = deliveryPolicy;
        this.productReferenceService = productReferenceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FormSummary> listForms(UUID productId) {
        log.debug("listForms: productId='{}'", productId);
        return formRepository.findAllByProductId(productId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public FormDetail createForm(UUID productId, CreateFormDefinitionRequest request) {
        log.debug("createForm: productId='{}', name='{}'", productId, request.name());
        ProductReference product = productReferenceService.getRequiredReference(productId);
        FormDefinition form = formRepository.save(new FormDefinition(new FormDefinition.Creation(
                product.tenantId(), productId, request.name(), request.type(), writeJson(request.fields()), "[]"
        )));
        log.info("createForm: formulario criado id='{}', productId='{}'", form.getId(), productId);
        return toDetail(form, request.fields(), List.of());
    }

    @Transactional(readOnly = true)
    public FormDetail getForm(UUID productId, UUID formId) {
        log.debug("getForm: productId='{}', formId='{}'", productId, formId);
        return toDetail(findFormInProduct(productId, formId));
    }

    @Transactional
    public FormDetail updateForm(UUID productId, UUID formId, UpdateFormDefinitionRequest request) {
        log.debug("updateForm: productId='{}', formId='{}', name='{}'", productId, formId, request.name());
        FormDefinition form = findFormInProduct(productId, formId);
        form.applyDefinition(new FormDefinition.Edit(request.name(), request.type(), writeJson(request.fields())));
        formRepository.save(form);
        log.info("updateForm: formulario atualizado id='{}'", form.getId());
        return toDetail(form);
    }

    @Transactional(readOnly = true)
    public List<String> listFieldTypes(UUID productId) {
        log.debug("listFieldTypes: productId='{}'", productId);
        productReferenceService.getRequiredReference(productId);
        return FormFieldType.contractValues();
    }

    @Transactional
    public FormDetail publish(UUID productId, UUID formId) {
        log.debug("publish: productId='{}', formId='{}'", productId, formId);
        FormDefinition form = findFormInProduct(productId, formId);
        List<Map<String, Object>> fields = readList(form.getFieldsJson());
        publicationPolicy.assertPublishable(form.getName(), fields);
        form.publish(OffsetDateTime.now(ZoneOffset.UTC).toString());
        formRepository.save(form);
        log.info("publish: formulario publicado id='{}'", form.getId());
        return toDetail(form, fields, readList(form.getDeliveryChannelsJson()));
    }

    @Transactional
    public FormDetail updateDelivery(UUID productId, UUID formId, UpdateFormDeliveryRequest request) {
        log.debug("updateDelivery: productId='{}', formId='{}'", productId, formId);
        FormDefinition form = findFormInProduct(productId, formId);
        request.channels().forEach(deliveryPolicy::assertValid);
        form.updateDeliveryChannels(writeJson(request.channels()));
        formRepository.save(form);
        log.info("updateDelivery: canais de entrega atualizados formId='{}'", form.getId());
        return toDetail(form);
    }

    private FormDefinition findFormInProduct(UUID productId, UUID formId) {
        return formRepository.findByProductIdAndId(productId, formId)
                .orElseThrow(() -> new FormNotFoundException(formId));
    }

    private FormSummary toSummary(FormDefinition form) {
        return formMapper.toSummary(form);
    }

    private FormDetail toDetail(FormDefinition form) {
        return toDetail(form, readList(form.getFieldsJson()), readList(form.getDeliveryChannelsJson()));
    }

    private FormDetail toDetail(FormDefinition form, List<Map<String, Object>> fields, List<Map<String, Object>> deliveryChannels) {
        return formMapper.toDetail(form, fields, deliveryChannels);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize form JSON", ex);
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Object value = objectMapper.readValue(json, List.class);
            if (value instanceof List<?> list && list.stream().allMatch(item -> item instanceof Map<?, ?>)) {
                return list.stream()
                        .map(item -> toObjectMap((Map<?, ?>) item))
                        .toList();
            }
            return List.of();
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize form JSON", ex);
        }
    }

    private Map<String, Object> toObjectMap(Map<?, ?> map) {
        Map<String, Object> objectMap = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> objectMap.put(key.toString(), mapValue));
        return objectMap;
    }
}
