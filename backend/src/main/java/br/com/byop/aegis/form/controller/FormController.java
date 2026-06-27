package br.com.byop.aegis.form.controller;

import br.com.byop.aegis.form.contract.CreateFormDefinitionRequest;
import br.com.byop.aegis.form.contract.UpdateFormDefinitionRequest;
import br.com.byop.aegis.form.contract.UpdateFormDeliveryRequest;
import br.com.byop.aegis.form.dto.FormDetail;
import br.com.byop.aegis.form.dto.FormSummary;
import br.com.byop.aegis.form.service.FormService;
import br.com.byop.aegis.product.api.ModuleKey;
import br.com.byop.aegis.product.api.ProductAccessPort;
import br.com.byop.aegis.product.api.RequireModule;
import br.com.byop.aegis.security.AuthenticatedUser;
import br.com.byop.aegis.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequireModule(ModuleKey.FORMS)
public class FormController {

    private final FormService formService;
    private final ProductAccessPort productAccessPort;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public FormController(FormService formService, ProductAccessPort productAccessPort,
                          AuthenticatedUserProvider authenticatedUserProvider) {
        this.formService = formService;
        this.productAccessPort = productAccessPort;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping("/api/v1/products/{productId}/forms")
    public List<FormSummary> listForms(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.listForms(productId);
    }

    @PostMapping("/api/v1/products/{productId}/forms")
    @ResponseStatus(HttpStatus.CREATED)
    public FormDetail createForm(@PathVariable("productId") UUID productId,
                                 @Valid @RequestBody CreateFormDefinitionRequest request,
                                 Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.createForm(productId, request);
    }

    @GetMapping("/api/v1/products/{productId}/forms/{formId}")
    public FormDetail getForm(@PathVariable("productId") UUID productId,
                              @PathVariable("formId") UUID formId,
                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.getForm(productId, formId);
    }

    @PutMapping("/api/v1/products/{productId}/forms/{formId}")
    public FormDetail updateForm(@PathVariable("productId") UUID productId,
                                 @PathVariable("formId") UUID formId,
                                 @Valid @RequestBody UpdateFormDefinitionRequest request,
                                 Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.updateForm(productId, formId, request);
    }

    @GetMapping("/api/v1/products/{productId}/forms/field-types")
    public List<String> listFieldTypes(@PathVariable("productId") UUID productId, Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.listFieldTypes(productId);
    }

    @PostMapping("/api/v1/products/{productId}/forms/{formId}/publish")
    public FormDetail publish(@PathVariable("productId") UUID productId,
                              @PathVariable("formId") UUID formId,
                              Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.publish(productId, formId);
    }

    @PutMapping("/api/v1/products/{productId}/forms/{formId}/delivery")
    public FormDetail updateDelivery(@PathVariable("productId") UUID productId,
                                     @PathVariable("formId") UUID formId,
                                     @Valid @RequestBody UpdateFormDeliveryRequest request,
                                     Authentication authentication) {
        assertProductAccess(authentication, productId);
        return formService.updateDelivery(productId, formId, request);
    }

    private AuthenticatedUser assertProductAccess(Authentication authentication, UUID productId) {
        AuthenticatedUser caller = authenticatedUserProvider.from(authentication);
        productAccessPort.assertAccessible(productId, caller);
        return caller;
    }
}
